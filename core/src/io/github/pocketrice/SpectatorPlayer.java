package io.github.pocketrice;

public class SpectatorPlayer extends HumanPlayer {

    public SpectatorPlayer(HumanPlayer hp) {
        super(hp.rb, hp.playerId, hp.playerName);
    }

    public HumanPlayer convertHuman() {
        return (HumanPlayer) this;
    }
    @Override
    public void deductHealth() {

    }

    @Override
    public void requestProjVector() {
    }

    @Override
    public String toString() {
        return "SPEC " + (playerName.isEmpty() ? playerId : playerName);
    }
}
