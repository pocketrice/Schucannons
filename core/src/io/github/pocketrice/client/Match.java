package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector3;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.github.pocketrice.shared.AnsiCode.*;

@Getter
public class Match implements Comparable<Match> {
    public static final int MAX_SPECTATORS = 5;

    Player currentPlayer, oppoPlayer;
    List<SpectatorPlayer> spectators;
    Vector3 cballPos;
    @Getter
    GameState state;
    @Getter
    PhaseType phase; // just to store match phase. Don't do calcs in here(?)
    UUID matchId;
    @Setter
    String matchName;
    @Setter
    Instant timestamp; // todo: needed? valid?

    boolean isFull;
    int turnCount;

    public Match() {
        this(null, null);
    }

    public Match(Player curr, Player oppo, SpectatorPlayer... specs) { // likely never to be used; matches shouldn't be pre-populated
        currentPlayer = curr;
        oppoPlayer = oppo;
        spectators = new ArrayList<>();
        spectators.addAll(List.of(specs));
        state = GameState.AWAIT;
        phase = PhaseType.NONE;
        matchId = UUID.randomUUID();
        matchName = "";

        isFull = false;
        turnCount = 0;
    }


    public void updateState() { // force a`wait if still await (should not be called during that phase anyway), or check for ended.
        if (state == GameState.AWAIT && currentPlayer != null && oppoPlayer != null && currentPlayer.isReady && oppoPlayer.isReady)
            state = GameState.READY; // note: matches aren't updating fast enough...
        else if (state != GameState.AWAIT && (turnCount > 10 || currentPlayer.health <= 0 || oppoPlayer.health <= 0))
            state = GameState.ENDED;
    }

    public void start() throws InterruptedException {
        state = GameState.READY;
        System.out.println(ANSI_BLUE + "Match started.\n\n" + ANSI_RESET);
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

    public Player getPlayer(UUID pid) {
        Player p = null;

        if (currentPlayer != null && currentPlayer.getPlayerId().equals(pid)) p = currentPlayer;
        else if (oppoPlayer != null && oppoPlayer.getPlayerId().equals(pid)) p = oppoPlayer;
        else {
            for (SpectatorPlayer sp : spectators) {
                if (sp.getPlayerId().equals(pid)) p = sp;
            }
        }

        return p;
    }

    public void addPlayers(Player... players) {
        for (Player p : players) {
            PlayerType pType = PlayerType.get(Math.min(playerCount(), 2));
            setPlayer(p, pType);
        }

        updateState();
    }

    public void refusePlayer(Player p) {
        p.isRefused = true;
        //System.out.println("Match full, cannot be joined"); // handle for player. This has to interact with gui and server.

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
//        Vector3 projLoc = truncVec(currentPlayer.rb.getLocation(), 2);
//        Vector3 oppoLoc = truncVec(oppoPlayer.rb.getLocation(), 2);
//        Vector3 projMot = Vector3.Zero; //truncVec(projMot(projVec, projLoc), 2); // FIXME
//
//        System.out.println(currentPlayer + " chose vector " + projVec + " from " + projLoc + ", landing at " + projMot + ".");
//        System.out.println("Opponent " + oppoPlayer + " sits at " + oppoPlayer.rb.getLocation() + ".");

        if (true) { //isHit()) { // FIXME
            oppoPlayer.deductHealth();
            System.out.println("Hit!");
        }
        else
         //   System.out.println("Miss. You were off by " + truncate(oppoLoc.dst(projMot), 2) + "m.");

        System.out.println("◇ Turn phase ended. Took " + truncate(msSinceEpoch() - timestamp, 2) + " ms.");
        updateState();
        if (state == GameState.ENDED)
            endMatch();
        else {
            if (!(oppoPlayer instanceof BotPlayer) || !((BotPlayer) oppoPlayer).isDummy()) { // dummy bots don't take a turn (CHECK THIS)
                Player temp = currentPlayer;
                currentPlayer = oppoPlayer;
                oppoPlayer = temp;
            }
        }
    }

    public String getIdentifier() {
        return (matchName.isEmpty()) ? matchId.toString() : matchName;
    }

    public Player[] players() {
        List<Player> players = new ArrayList<>();
        if (currentPlayer != null) players.add(currentPlayer);
        if (oppoPlayer != null) players.add(oppoPlayer);
        players.addAll(spectators);

        return players.toArray(new Player[0]);
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
        //return (isFull ? ANSI_PURPLE + "FULL" : (currentPlayer == null || oppoPlayer == null) ? ANSI_GREEN + "OPEN" : ANSI_BLUE + "QUEUED") + " [" + playerCount() + "/2] " + "Match " +  getIdentifier() + ANSI_RESET + " (" + (currentPlayer == null ? "NA" : currentPlayer) + ", " + (oppoPlayer == null ? "NA" : oppoPlayer) + (spectators.isEmpty() ? "" : ", " + spectators) + ")";
        return getMatchId() + "|" + getIdentifier() + "|[" + playerCount() + "/2]|" + (currentPlayer == null ? "NA" : currentPlayer) + ", " + (oppoPlayer == null ? "NA" : oppoPlayer) + (spectators.isEmpty() ? "" : ", " + spectators);
    }

    public boolean equals(Match other) {
        return this.toString().equals(other.toString()); // convenient way to include all comparable data
    }

    public boolean hasPlayer(UUID pid) {
        return Arrays.stream(players()).anyMatch(p -> p.getPlayerId().equals(pid));
    }

    public void advancePhase() {
        int newPhase = (phase.val + 1 > PhaseType.ENDED.val) ? 0 : phase.val + 1;
        System.out.println(phase + " -> " + newPhase);
        phase = PhaseType.get(newPhase);
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
        READY(1),
        RUNNING(2),
        ENDED(3);

        GameState(int i) {
            val = i;
        }

        static GameState get(int i) {
            switch (i) {
                case 0 -> {
                    return AWAIT;
                }
                case 1 -> {
                    return READY;
                }
                case 2 -> {
                    return RUNNING;
                }
                case 3 -> {
                    return ENDED;
                }
                default -> {
                    return INVALID;
                }
            }
        }

        public final int val;
    }

    public enum PhaseType { // bad order?
        INVALID(-1),
        NONE(0),
        MOVE(1),
        PROMPT(2),
        SIM(3),
        ENDED(4);


        PhaseType(int i) {
            val = i;
        }

        static PhaseType get(int i) {
            switch (i) {
                case 0 -> {
                    return NONE;
                }
                case 1 -> {
                    return MOVE;
                }
                case 2 -> {
                    return PROMPT;
                }
                case 3 -> {
                    return SIM;
                }
                case 4 -> {
                    return ENDED;
                }
                default -> {
                    return INVALID;
                }
            }
        }

        public final int val;
    }
}
