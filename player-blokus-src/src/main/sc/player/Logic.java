
package sc.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.api.plugins.IGameState;
import sc.plugin2027.Move;
import sc.plugin2027.GameState;
import sc.shared.GameResult;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Einfache Blokus-Strategie:
 * Wählt zufällig einen gültigen Zug aus.
 */
public class Logic implements IGameHandler {

  private static final Logger log = LoggerFactory.getLogger(Logic.class);

  /** Aktueller Spielstatus. */
  private GameState gameState;

  /**
   * Berechnet den nächsten Zug.
   */
  @Override
  public Move calculateMove() {

    long startTime = System.currentTimeMillis();

    log.info("Es wurde ein Zug von {} angefordert.",
            gameState.getCurrentTeam());

    // Alle gültigen Züge nach den Blokus-Regeln abrufen
    List<Move> possibleMoves = gameState.getSensibleMoves();

    // Prüfen, ob noch gültige Züge vorhanden sind
    if (possibleMoves == null || possibleMoves.isEmpty()) {
      log.warn("Keine gültigen Züge mehr vorhanden!");
      return null;
    }

    // Zufälligen gültigen Zug auswählen
    int randomIndex = ThreadLocalRandom.current()
            .nextInt(possibleMoves.size());

    Move move = possibleMoves.get(randomIndex);

    log.info("Aus {} möglichen Zügen wurde {} ausgewählt.",
            possibleMoves.size(), move);

    log.info("Sende Zug nach {}ms.",
            System.currentTimeMillis() - startTime);

    return move;
  }

  /**
   * Ein neuer Spielstatus ist verfügbar.
   */
  @Override
  public void onUpdate(IGameState gameState) {

    this.gameState = (GameState) gameState;

    log.info("Zug: {} | Dran: {}",
            gameState.getTurn(),
            gameState.getCurrentTeam());
  }

  /**
   * Das Spiel ist beendet.
   */
  public void onGameOver(GameResult data) {

    log.info("Das Spiel ist beendet, Ergebnis: {}", data);
  }

  /**
   * Der Server hat einen Fehler gemeldet.
   */
  @Override
  public void onError(String error) {

    log.warn("Fehler: {}", error);
  }
}