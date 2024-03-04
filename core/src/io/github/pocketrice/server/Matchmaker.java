package io.github.pocketrice.server;

import io.github.pocketrice.client.BotPlayer;
import io.github.pocketrice.client.Flavour;
import io.github.pocketrice.client.Flavour.FlavourType;
import io.github.pocketrice.client.Match;
import io.github.pocketrice.client.Player;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.pocketrice.shared.AnsiCode.ANSI_BLUE;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

public class Matchmaker {
    static final int AVAILABLE_MATCHES = 2, START_MATCHES = 6, MAX_MATCHES = 10;
    List<Match> matches;
    Queue<Match> availableMatches;



    public Matchmaker() {
       Match[] startMatches = new Match[START_MATCHES];
       for (int i = 0; i < startMatches.length; i++) {
           startMatches[i] = new Match();
           Random rng = new Random();

           if (rng.nextBoolean()) {
               double val = rng.nextDouble();
               for (int j = 0; j < Math.round(val * 3); j++)
                   startMatches[i].addPlayers(new BotPlayer());
           }

           startMatches[i].setMatchName(Flavour.random(FlavourType.HOBBIT, FlavourType.SPICE)); // insert obligatory commentary on blasé unfairness w/ titles and nobles, blah blah...
       }
       matches = new ArrayList<>();


       availableMatches = new PriorityQueue<>();
       addMatches(startMatches);

    }

    public Match findMatch(String identifier) {
        for (Match m : matches) {
            if (m.getMatchId().toString().equals(identifier) || m.getMatchName().equals(identifier)) return m;
        }
        return null;
    }

    public void addMatches(Match... ms) { // Prefer over adding to set for safety
        for (Match m : ms) {
            assert matches.contains(m) : "Duplicate match found in matches!"; // don't allow dupes! set would've been better but had trouble w/ default match... sigh

            if (!m.isFull()) availableMatches.add(m); // max of 20 spectators
            matches.add(m);
        }
    }

    public void updateAvailableMatches() {
        availableMatches.removeIf(Match::isFull);
        assert matches.size() <= MAX_MATCHES : "Too many matches! (" + matches.size() + " > " + MAX_MATCHES + ")";
        int numLacked = Math.max(0, AVAILABLE_MATCHES - availableMatches.size());

        for (int i = 0; i < numLacked; i++) addMatches(new Match());
    }

    public Match searchMatch() { // get first, least empty match (prioque'd)
        updateAvailableMatches();
        return availableMatches.poll();
    }

    public Match connectPlayers(Match match, Player... players) {
        match.addPlayers(players);
        System.out.println(ANSI_BLUE + "\uD83D\uDD17 Connected to match " + match.getMatchId() + (match.getMatchName().isEmpty() ? "" : " (" + match.getMatchName() + ")") + ANSI_RESET);
        return match;
        // todo: amazon ce2 magic or smth
    }

    public void update() {
        updateAvailableMatches();
    }

    public Match connectPlayers(Player... players) {
        return connectPlayers(searchMatch(), players);
    }

    @Override
    public String toString() {
        return matches.stream().map(Match::toString).collect(Collectors.joining("\n"));
    }
}
