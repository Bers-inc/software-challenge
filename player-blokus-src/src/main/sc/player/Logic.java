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
 * Das Herz des Spielers:
 * Diese Logik bewertet alle verfügbaren gültigen Züge
 * und versucht, möglichst viele Punkte zu erzielen und
 * gleichzeitig die gegnerischen Möglichkeiten einzuschränken.
 *
 * Zusätzlich werden wichtige Informationen zum Spielverlauf
 * in der Konsole ausgegeben.
 */
public class Logic implements IGameHandler {

  private static final Logger log =
          LoggerFactory.getLogger(Logic.class);

  /** Speichert den aktuellen Spielstatus. */
  private GameState gameState;

  /**
   * Berechnet den nächsten Spielzug.
   *
   * Für die Berechnung stehen maximal zwei Sekunden zur Verfügung.
   * Die Zeit wird in zwei Abschnitte unterteilt:
   *
   * Phase 1:
   * Auswahl der Züge mit dem höchsten unmittelbaren Punktgewinn.
   *
   * Phase 2:
   * Bewertung dieser Züge hinsichtlich ihrer Auswirkungen
   * auf die Anzahl der gegnerischen Zugmöglichkeiten.
   *
   * @return Der ausgewählte gültige Spielzug.
   */
  @Override
  public Move calculateMove() {

    long start = System.currentTimeMillis();

    // Zeitgrenzen für die beiden Bewertungsphasen festlegen.
    long stage1Deadline = start + 1000;
    long stage2Deadline = start + 1600;

    // Das eigene Team bestimmen.
    var me = gameState.getCurrentTeam();

    // Alle aktuell möglichen gültigen Züge abrufen.
    List<Move> moves = gameState.getSensibleMoves();

    // Aktuellen Punktestand des eigenen Teams berechnen.
    int base = Arrays.stream(
            gameState.getPointsForTeam(me)
    ).sum();

    /*
     * PHASE 1:
     * Alle möglichen Züge auf ihren unmittelbaren
     * Punktgewinn überprüfen.
     *
     * Es werden nur Züge berücksichtigt, die den
     * höchsten bisher gefundenen Punktgewinn erzielen.
     */

    int maxGain = -1;

    // Hier werden die Züge mit dem höchsten Punktgewinn gesammelt.
    List<Move> best = new ArrayList<>();

    for (Move m : moves) {

      // Simulieren, wie sich der Punktestand nach dem Zug verändert.
      int gain = Arrays.stream(
              gameState.performMove(m).getPointsForTeam(me)
      ).sum() - base;

      // Prüfen, ob ein neuer maximaler Punktgewinn erreicht wurde.
      if (gain > maxGain) {
        maxGain = gain;
        best.clear();
      }

      // Alle Züge mit maximalem Punktgewinn speichern.
      if (gain == maxGain) {
        best.add(m);
      }

      // Berechnung abbrechen, wenn das Zeitlimit erreicht wurde.
      if (System.currentTimeMillis() > stage1Deadline) {
        break;
      }
    }

    /*
     * PHASE 2:
     * Die ausgewählten Züge werden hinsichtlich ihrer
     * Auswirkungen auf die gegnerischen Zugmöglichkeiten bewertet.
     *
     * Ziel ist es, einen Zug zu finden, der dem Gegner
     * möglichst wenige weitere Möglichkeiten lässt.
     */

    // Reihenfolge zufällig mischen, um Gleichstände aufzulösen.
    Collections.shuffle(best);

    // Zunächst den ersten Zug als beste Möglichkeit festlegen.
    Move choice = best.get(0);

    List<Move> best2 = new ArrayList<>();

    // Anfangswert für die Suche nach möglichst wenigen
    // gegnerischen Zugmöglichkeiten.
    int minOpponentMoves = Integer.MAX_VALUE;

    for (Move m : best) {

      // Prüfen, ob noch genügend Berechnungszeit vorhanden ist.
      if (System.currentTimeMillis() > stage2Deadline) {
        break;
      }

      // Zug simulieren und anschließend die möglichen
      // Züge des Gegners ermitteln.
      int opp = gameState.performMove(m)
              .getSensibleMoves()
              .size();

      // Den Zug mit den wenigsten gegnerischen Möglichkeiten auswählen.
      if (opp < minOpponentMoves) {
        minOpponentMoves = opp;
        choice = m;
      }
    }

    // Ausgewählten Zug und Berechnungsdauer protokollieren.
    log.info(
            "Sende Zug {} (+{} Punkte) nach {} ms.",
            choice,
            maxGain,
            System.currentTimeMillis() - start
    );

    return choice;
  }

  /**
   * Wird aufgerufen, sobald ein neuer Spielstatus verfügbar ist.
   *
   * Dabei wird der aktuelle Spielstatus gespeichert,
   * damit die Berechnung des nächsten Zuges darauf zugreifen kann.
   *
   * @param gameState Der neue Spielstatus.
   */
  @Override
  public void onUpdate(IGameState gameState) {

    this.gameState = (GameState) gameState;

    log.info(
            "Zug: {} | Am Zug: {}",
            gameState.getTurn(),
            gameState.getCurrentTeam()
    );
  }

  /**
   * Wird aufgerufen, sobald das Spiel beendet wurde.
   *
   * Das Spielergebnis wird in der Konsole ausgegeben.
   *
   * @param data Das Ergebnis der beendeten Partie.
   */
  public void onGameOver(GameResult data) {

    log.info("Das Spiel ist beendet. Ergebnis: {}", data);
  }

  /**
   * Wird aufgerufen, wenn der Server einen Fehler meldet.
   *
   * Ein solcher Fehler kann beispielsweise zum Abbruch
   * der aktuellen Partie führen.
   *
   * @param error Beschreibung des aufgetretenen Fehlers.
   */
  @Override
  public void onError(String error) {

    log.warn("Serverfehler: {}", error);
  }
}