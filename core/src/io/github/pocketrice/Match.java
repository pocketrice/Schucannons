package io.github.pocketrice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.Prysm.ForceConstant;
import lombok.Getter;
import lombok.Setter;

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

    @Setter
    String matchName;
    @Getter
    boolean isFull;
    @Getter
    int turnCount;
    @Getter
    long waitTime;

    public static int MAX_SPECTATORS = 5;

    public Match() {
        this(null, null);
    }

    public Match(Player curr, Player oppo, SpectatorPlayer... specs) { // likely never to be used; matches shouldn't be pre-populated
        currentPlayer = curr;
        oppoPlayer = oppo;
        spectators = new ArrayList<>();
        spectators.addAll(List.of(specs));
        gameEnv = new GameEnvironment(this);
        gameState = GameState.AWAIT;
        matchId = UUID.randomUUID();
        matchName = "";

        isFull = false;
        turnCount = 0;
        waitTime = 0L;
    }


    public void updateState() { // force await if still await (should not be called during that phase anyway), or check for ended.
        if (gameState == GameState.AWAIT) return;
        if (turnCount > 10 || currentPlayer.health <= 0 || oppoPlayer.health <= 0) {
            gameState = GameState.ENDED;
        }
    }

    public void start() throws InterruptedException {
        gameState = GameState.RUNNING;
        System.out.println(ANSI_BLUE + "Match started.\n\n" + ANSI_RESET);
        render();
        while (turnCount < 10) {
            turnCount++;
            System.out.println("Turn " + turnCount + " started.");
            System.out.print(ANSI_PURPLE);
            runTurn();

            System.out.print("\n\n" + ANSI_YELLOW);
            runTurn();
            System.out.print(ANSI_RESET);
            System.out.println("◈ Turn " + turnCount + " ended.\n");
            System.out.println(currentPlayer + " : " + currentPlayer.health + "    -~-    " + oppoPlayer.health + " : " + oppoPlayer);
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
        return ((currentPlayer != null) ? 1 : 0) + ((oppoPlayer != null) ? 1 : 0) + spectators.size();
    }

    public void setPlayer(Player player, PlayerType type) {
        switch (type) {
            case CURRENT -> currentPlayer = player;
            case OPPONENT -> oppoPlayer = player;
            case SPECTATOR -> {
                if (spectators.size() >= MAX_SPECTATORS)
                    refusePlayer(player); // Refuse if AFTER full
                else {
                    if (player instanceof BotPlayer) refusePlayer(player);
                    else spectators.add(((HumanPlayer) player).convertSpec());
                }

                if (spectators.size() >= MAX_SPECTATORS) isFull = true;
            }
        }
    }

    public void addPlayers(Player... players) {
        for (Player p : players) {
            PlayerType pType = PlayerType.get(Math.min(playerCount(), 2));
            setPlayer(p, pType);
        }
    }

    public void refusePlayer(Player p) {
        p.isRefused = true;
        System.out.println("Match full, cannot be joined"); // handle for player. This has to interact with gui and server.

        // TODO: handle if refused (technically should not happen).
    }

    public boolean kickPlayer(Player p) {
        if (currentPlayer == p) {
            currentPlayer = null;
            return true;
        }
        else if (oppoPlayer == p) {
            oppoPlayer = null;
            return true;
        }
        else
            return spectators.remove((SpectatorPlayer) p);
    }

    public void runTurn() throws InterruptedException {
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
            System.out.println("Miss. You were off by " + truncate(oppoLoc.dst(projMot), 2) + "m.");

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

    public String getIdentifier() {
        return (matchName.isEmpty()) ? matchId.toString() : matchName;
    }

    public void endMatch() {
        System.out.println("❖ Match ended. Final score: " + currentPlayer + "'s " + currentPlayer.health + " / " + oppoPlayer + "'s " + oppoPlayer.health);
        kickPlayer(currentPlayer);
        kickPlayer(oppoPlayer);
        spectators.forEach(this::kickPlayer); // avoid list.clear as it's too raw
    }

    @Override
    public int compareTo(Match other) {
        return Integer.compare(this.playerCount(), other.playerCount());
    }

    public void render() {
        //Gdx.app.postRunnable(() -> {
            //System.out.println("MATCH RENDER");
           // gameEnv.render();
       // });
    }
    public void dispose() {
       // Gdx.app.postRunnable(() -> {
          //  gameEnv.dispose();
       // });
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

    @Override
    public String toString() {
        return (isFull ? ANSI_PURPLE + "FULL" : (currentPlayer == null || oppoPlayer == null) ? ANSI_GREEN + "OPEN" : ANSI_BLUE + "QUEUED") + " [" + playerCount() + "/2] " + "Match " + (matchName.isEmpty() ? matchId : matchName) + ANSI_RESET + " (" + (currentPlayer == null ? "NA" : currentPlayer) + ", " + (oppoPlayer == null ? "NA" : oppoPlayer) + (spectators.isEmpty() ? "" : ", " + spectators) + ")";
    }

    public boolean equals(Match other) {
        return this.toString().equals(other.toString()); // convenient way to include all comparable data
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
