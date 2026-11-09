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
    long start = System.currentTimeMillis();
    long stage1Deadline = start + 1000;
    long stage2Deadline = start + 1600;

    var me = gameState.getCurrentTeam();
    List<Move> moves = gameState.getSensibleMoves();
    int base = Arrays.stream(gameState.getPointsForTeam(me)).sum();

    // 1) ходы с максимальным приростом очков
    // 1) Züge mit den Höhster Punkte Zahl
    int maxGain = -1;
    List<Move> best = new ArrayList<>();
    for (Move m : moves) {
      int gain = Arrays.stream(gameState.performMove(m).getPointsForTeam(me)).sum() - base;
      if (gain > maxGain) { maxGain = gain; best.clear(); }
      if (gain == maxGain) best.add(m);
      if (System.currentTimeMillis() > stage1Deadline) break;
    }

    // 2) среди них - тот, после которого у соперника меньше ходов
    // 2) aus allen zügen wehlt es die, die den Gegner Blockieren
    Collections.shuffle(best);
    Move choice = best.get(0);
    List<Move> best2 = new ArrayList<>();
    int minOpponentMoves = Integer.MAX_VALUE;
    for (Move m : best) {
      if (System.currentTimeMillis() > stage2Deadline) break;
      int opp = gameState.performMove(m).getSensibleMoves().size();
      if (opp < minOpponentMoves) { minOpponentMoves = opp; choice = m; }
    }

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
