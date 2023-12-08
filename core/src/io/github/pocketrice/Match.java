package io.github.pocketrice;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import io.github.pocketrice.Prysm.ForceConstant;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.github.pocketrice.AnsiCode.*;

public class Match implements Comparable<Match> {

    Player currentPlayer, oppoPlayer;
    List<SpectatorPlayer> spectators;
    GameEnvironment gameEnv;
    GameState gameState;
    UUID matchId;

    @Getter
    boolean isFull;
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

        isFull = (curr instanceof HumanPlayer && oppo instanceof HumanPlayer);
        turnCount = 0;
        waitTime = 0L;
    }

    public void updateState() { // force await if still await (should not be called during that phase anyway), or check for ended.
        if (gameState == GameState.AWAIT) return;
        if (turnCount > 10 || currentPlayer.health <= 0 || oppoPlayer.health <= 0) {
            gameState = GameState.ENDED;
        }
    }

    public void start() {
        gameState = GameState.RUNNING;
        System.out.println(ANSI_BLUE + "Match started.\n\n" + ANSI_RESET);
        while (turnCount < 10) {
            turnCount++;
            System.out.println("Turn " + turnCount + " started.");
            System.out.print(ANSI_PURPLE);
            runTurn();

            System.out.print("\n\n" + ANSI_YELLOW);
            runTurn();
            System.out.print(ANSI_RESET);
            System.out.println("◈ Turn " + turnCount + " ended.");
            System.out.println(currentPlayer + " - " + currentPlayer.health + " / " + oppoPlayer + " - " + oppoPlayer.health);
            System.out.println("\n\n\n\n");
        }
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

        return currLoc.cpy().add(x,0f,z);
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
        long timestamp = System.currentTimeMillis();
        Vector3 projVec = truncVec(currentPlayer.getProjVector(), 2);
        Vector3 projLoc = truncVec(currentPlayer.rb.getLocation(), 2);
        Vector3 oppoLoc = truncVec(oppoPlayer.rb.getLocation(), 2);
        Vector3 projMot = truncVec(projMot(projVec, projLoc), 2);

        System.out.println(currentPlayer + " chose vector " + projVec + " from " + projLoc + ", landing at " + projMot + ".");
        System.out.println("Opponent " + oppoPlayer + " sits at " + oppoPlayer.rb.getLocation() + ".");
        gameEnv.animTurn();

        if (isHit()) {
            oppoPlayer.deductHealth();
            System.out.println("Hit!");
        }
        else
            System.out.println("Miss. You were off by " + truncate(oppoLoc.dst(projMot), 2) + "u.");

        System.out.println("◇ Turn phase ended. Took " + truncate(msSinceEpoch() - timestamp, 2) + " ms.");
        updateState();
        sendData();
        if (gameState == GameState.ENDED)
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
        System.out.println("❖ Match ended. Final score: " + currentPlayer + "'s " + currentPlayer.health + " / " + oppoPlayer + "'s " + oppoPlayer.health);
    }

    @Override
    public int compareTo(Match other) {
        return Integer.compare(this.playerCount(), other.playerCount());
    }

    public void dispose() {
        gameEnv.dispose();
    }

    // Returns milliseconds since 0:00 1/1/1970 (Unix epoch).
    // @param N/A
    // @return milliseconds since Unix epoch (0:00 1/1/1970)
    public static long msSinceEpoch() { // unix epoch = 1/1/1970
        return System.currentTimeMillis();
    }

    public static float truncate(float value, int mantissa) // <+> APM
    {
        return (float) BigDecimal.valueOf(value).setScale(mantissa, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static Vector3 truncVec(Vector3 vec, int mantissa) {
        Vector3 copy = vec.cpy(); // Avoid modifying original vec.
        return vec.set(truncate(copy.x, mantissa), truncate(copy.y, mantissa), truncate(copy.z, mantissa));
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
