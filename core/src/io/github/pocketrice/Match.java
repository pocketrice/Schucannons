package io.github.pocketrice;

import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.Prysm.ForceConstant;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Match implements Comparable<Match> {

    Player currentPlayer, oppoPlayer;
    List<SpectatorPlayer> spectators;
    GameEnvironment gameEnv;
    GameState gameState;
    UUID matchId;

    @Getter
    boolean isEnded, isFull;
    @Getter
    int turnCount;
    @Getter
    long waitTime;

    public static int MAX_SPECTATORS = 5;

    public Match() {
        this(BotPlayer.FILLER, BotPlayer.FILLER, new ArrayList<>());
    }

    public Match(Player curr, Player oppo, List<SpectatorPlayer> specs) { // likely never to be used; matches shouldn't be pre-populated
        currentPlayer = curr;
        oppoPlayer = oppo;
        spectators = specs;
        gameEnv = new GameEnvironment(this);
        gameState = GameState.AWAIT;
        matchId = UUID.randomUUID();

        isEnded = false;
        isFull = (curr instanceof HumanPlayer && oppo instanceof HumanPlayer);
        turnCount = 0;
        waitTime = 0L;
    }

    public GameState determineState() {
        return (isEnded) ? GameState.ENDED : GameState.RUNNING; // todo: more state implementation OR remove
    }

    public void start() {
        // go
    }

    public Vector3 projMot(Vector3 projVec, Vector3 currLoc) {
        // ∆x = v0x*t
        // ∆y = v0y*t - 0.5gt^2
        // ∆z = v0z*t

        // for now, assume ∆y = 0.
        // 0 = v0y*t - 0.5gt^2
        // 0.5gt^2 = v0y*t
        // 0.5gt = v0y
        // t = v0y/0.5g
        float t = (float) (projVec.y / (0.5 * ForceConstant.EARTH_G.val()));
        float x = projVec.x * t;
        float z = projVec.z * t;

        return currLoc.add(x,0f,z);
    }

    public boolean isHit() {
        Vector3 projVec = currentPlayer.getProjVector();
        Vector3 currentLoc = currentPlayer.rb.getLocation();
        Vector3 projMot = projMot(projVec, currentLoc);

        return projMot.equals(oppoPlayer.rb.getLocation()) && gameEnv.isLegal(projMot);
    }


    public int playerCount() {
        return ((currentPlayer instanceof HumanPlayer) ? 1 : 0) + ((oppoPlayer instanceof HumanPlayer) ? 1 : 0) + spectators.size();
    }

    public void setPlayer(Player player, PlayerType type) {
        switch (type) {
            case CURRENT -> currentPlayer = player;
            case OPPONENT -> oppoPlayer = player;
            case SPECTATOR -> {
                if (spectators.size() > MAX_SPECTATORS) {
                    isFull = true;
                    refusePlayer(player);
                }
                else {
                    if (player instanceof BotPlayer)
                        refusePlayer(player);
                    else
                        spectators.add((SpectatorPlayer) player);
                }
            }
        }
    }

    public void addPlayers(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            PlayerType pType = PlayerType.get(Math.min(i,2));
            setPlayer(players[i], pType);
        }
    }

    public void refusePlayer(Player p) {
        System.out.println("Match full, cannot be joined"); // handle for player. This has to interact with gui and server.
    }

    public void runTurn() {
        // GAME LOGIC!
        currentPlayer.requestProjVector();
        Vector3 projVec = currentPlayer.getProjVector();

        gameEnv.animTurn();

        if (isHit()) oppoPlayer.deductPoint();

        sendData();
        if (isEnded)
            endMatch();
        else {
            if (!(oppoPlayer instanceof BotPlayer) || !((BotPlayer) oppoPlayer).isDummy()) { // dummy bots don't take a turn (CHECK THIS)
                Player temp = currentPlayer;
                currentPlayer = oppoPlayer;
                oppoPlayer = temp;
            }
        }
    }

    public void sendData() {
        System.out.println("Packet sent.");
    }

    public void endMatch() {
        System.out.println("Match ended.");
        isEnded = true;
    }

    @Override
    public int compareTo(Match other) {
        return Integer.compare(this.playerCount(), other.playerCount());
    }

    public void dispose() {

    }

    public enum PlayerType {
        INVALID(-1),
        CURRENT(0),
        OPPONENT(1),
        SPECTATOR(2);


        PlayerType(int i) {
            val = i;
        }

        static PlayerType get(int i) {
            switch (i) {
                case 0 -> {
                    return CURRENT;
                }
                case 1 -> {
                    return OPPONENT;
                }
                case 2 -> {
                    return SPECTATOR;
                }
                default -> {
                    return INVALID;
                }
            }
        }

        final int val;
    }

    public enum GameState {
        INVALID(-1),
        AWAIT(0),
        RUNNING(1),
        ENDED(2);

        GameState(int i) {
            val = i;
        }

        final int val;
    }
}
