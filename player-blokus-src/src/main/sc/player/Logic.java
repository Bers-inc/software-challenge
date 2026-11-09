package sc.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.api.plugins.IGameState;
import sc.plugin2027.Move;
import sc.plugin2027.GameState;
import sc.shared.GameResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.List;

/**
 * Das Herz des Clients:
 * Eine simple Logik, die zufaellige gueltige Zuege macht.
 * <p>
 * Ausserdem werden zum Spielverlauf Konsolenausgaben gemacht.
 */
public class Logic implements IGameHandler {
  private static final Logger log = LoggerFactory.getLogger(Logic.class);

  /** Aktueller Spielstatus. */
  private GameState gameState;

  /** In dieser Methode habt ihr 2 Sekunden (berechnet etwas Puffer ein) Zeit,
   * um euren nächsten Zug zu planen. */
  @Override
  public Move calculateMove() {
    // Запоминаем время старта, чтобы уложиться в лимит 2 секунды на ход.
    // Wir speichern die Startzeit, um das Limit von 2 Sekunden pro Zug einzuhalten.
    long start = System.currentTimeMillis();
    // до этого момента перебираем ходы по очкам (этап 1)
    // bis zu diesem Punkt gehen wir die Züge nach Punkten durch (Phase 1)
    long stage1Deadline = start + 1000;
    // до этого момента анализируем ответы соперника (этап 2)
    // bis zu diesem Zeitpunkt analysieren wir die Antworten des Gegners (Phase 2)
    long stage2Deadline = start + 1600;

    // Моя команда (у неё две цвета: синий+красный или жёлтый+зелёный).
    // Mein Team (es hat zwei Farben: Blau + Rot oder Gelb + Grün).
    var me = gameState.getCurrentTeam();
    // Все допустимые ходы для текущего цвета.
    // Holle alle zulässigen Züge für die aktuelle Farbe.
    List<Move> moves = gameState.getSensibleMoves();
    // Текущий счёт моей команды. getPointsForTeam возвращает массив очков
    // (по цветам), поэтому суммируем его в одно число.
    // Aktueller Punktestand meines Teams. getPointsForTeam liefert ein Array von Punkten
    // (nach Farben) zurück; daher summieren wir diese zu einer einzigen Zahl.
    int base = Arrays.stream(gameState.getPointsForTeam(me)).sum();



    // ---------- ЭТАП 1: выбираем ходы с максимальным приростом очков ----------
    // Прирост очков = размер положенной фигуры (пентомино даёт 5, монимино 1 и т.д.).
    // Поэтому этап по сути ищет самые большие фигуры, которые можно положить.
    // ---------- Phase 1:  Züge mit den Höhster Punkte Zahl wehlen ----------
    // Punktzuwachs = Größe der platzierten Figur (Pentomino ergibt 5, Monomino 1 usw.).
    // Daher sucht dieser Schritt im Grunde nach den größten Figuren, die platziert werden können.
    int maxGain = -1; // лучший прирост, найденный на данный момент / bester Zuwachs, der bislang gefunden wurde
    List<Move> best = new ArrayList<>(); // все ходы с этим лучшим приростом / alle Züge mit diesem besten Zuwachs
    for (Move m : moves) {
      // performMove не меняет текущее состояние, а возвращает новое.
      // Считаем счёт после хода и вычитаем исходный.
      // performMove ändert den aktuellen Zustand nicht, sondern gibt einen neuen zurück.
      // Wir berechnen den Punktestand nach dem Zug und ziehen den ursprünglichen Wert ab.
      int gain = Arrays.stream(gameState.performMove(m).getPointsForTeam(me)).sum() - base;
      // Нашли ход лучше всех прежних: сбрасываем список и начинаем его заново.
      // Wir haben einen Zug gefunden, der besser ist als alle bisherigen: Wir verwerfen die Liste und beginnen von vorn.
      if (gain > maxGain) { maxGain = gain; best.clear(); }
      // Ход не хуже лучшего: запоминаем (в списке будут все равноценные ходы).
      // Ein ebenso guter Zug: Wir merken ihn uns (die Liste enthält alle gleichwertigen Züge).
      if (gain == maxGain) best.add(m);
      // Защита по времени: если вышли за 1 секунду, прекращаем перебор
      // и то, что уже нашли.
      // Zeitbasierter Schutz: Wenn 1 Sekunde verstrichen ist, beenden wir die Suche
      // und arbeiten mit dem weiter, was wir bereits gefunden haben.
      if (System.currentTimeMillis() > stage1Deadline) break;
    }

    // ---------- ЭТАП 2: среди лучших ходов выбираем тот, что мешает сопернику ----------
    // Перемешиваем, чтобы при равенстве не повторять один и тот же ход
    // и чтобы при нехватке времени смотреть случайную выборку, а не начало списка.
    // ---------- SCHRITT 2: Unter den besten Zügen wählen wir denjenigen aus, der den Gegner behindert ----------
    // Wir mischen die Züge, um bei Gleichstand nicht immer denselben Zug zu wählen
    // und um bei Zeitmangel eine zufällige Auswahl zu betrachten – statt immer nur den Anfang der Liste.
    Collections.shuffle(best);
    Move choice = best.get(0);  // запасной вариант, если времени на анализ не хватит / Ausweichoption, falls die Zeit für eine Analyse nicht ausreicht
    int minOpponentMoves = Integer.MAX_VALUE; // меньше всего ходов у соперника, найдено на данный момент / der Gegner hat die wenigsten Züge – bisher gefunden
    for (Move m : best) {
      // Защита по времени для второго этапа.
      // Zeitschutz für die zweite Stufe.
      if (System.currentTimeMillis() > stage2Deadline) break;
      // После моего хода очередь переходит к сопернику, поэтому getSensibleMoves()
      // на новом состоянии даёт возможные ходы соперника. Чем их меньше,
      // тем сильнее мы его стесняем.
      // Nach meinem Zug ist der Gegner am Zug; daher liefert getSensibleMoves()
      // für den neuen Zustand die möglichen Züge des Gegners. Je weniger es davon gibt,
      // desto stärker schränken wir ihn ein.
      int opp = gameState.performMove(m).getSensibleMoves().size();
      if (opp < minOpponentMoves) { minOpponentMoves = opp; choice = m; }
    }

    // Лог не трогать
    // Log nicht berühren
    log.info("Sende {} (+{} Punkte) nach {}ms.", choice, maxGain, System.currentTimeMillis() - start);
    return choice;
  }

  /** Ein neuer Spielstatus ist verfügbar, d.h. ein Zug wurde erfolgreich ausgeführt. */
  @Override
  public void onUpdate(IGameState gameState) {
    this.gameState = (GameState) gameState;
    log.info("Zug: {} Dran: {}", gameState.getTurn(), gameState.getCurrentTeam());
  }

  /** Wird aufgerufen, wenn das Spiel beendet ist. */
  public void onGameOver(GameResult data) {
    log.info("Das Spiel ist beendet, Ergebnis: {}", data,"Гиаганские потные яйца");
  }

  /** Wird aufgerufen, wenn der Server einen Fehler meldet.
   * Bedeutet auch den Abbruch des Spiels. */
  @Override
  public void onError(String error) {
    log.warn("Fehler: {}", error);
  }
}