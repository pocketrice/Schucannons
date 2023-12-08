package io.github.pocketrice;

public class SpectatorPlayer extends Player {

    @Override
    public void deductHealth() {

    }

    @Override
    public void requestProjVector() {
    }

    @Override
    public String toString() {
        return "Spectator " + (playerName.isEmpty() ? playerId : playerName);
    }
}
