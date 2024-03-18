package io.github.pocketrice.client;

public final class SpectatorPlayer extends HumanPlayer {

    public SpectatorPlayer(HumanPlayer hp) {
        super(hp.playerId, hp.playerName);
    }

    public HumanPlayer convertHuman() {
        return (HumanPlayer) this;
    }
    @Override
    public void deductHealth() {
        throw new IllegalCallerException("Spectators should not be active during the game!");
    }

    @Override
    public void requestProjVector() {
        throw new IllegalCallerException("Spectators should not be active during the game!");
    }

    @Override
    public String toString() {
        return "SPEC " + (playerName.isEmpty() ? playerId : playerName);
    }
}
