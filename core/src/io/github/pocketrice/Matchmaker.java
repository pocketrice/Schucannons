package io.github.pocketrice;

import com.github.javafaker.Faker;

import java.util.*;
import java.util.stream.Collectors;

public class Matchmaker {
    List<Match> matches;
    Queue<Match> availableMatches;

    public static final int AVAILABLE_MATCHES = 2, START_MATCHES = 6, MAX_MATCHES = 10;

    public Matchmaker() {
       Match[] startMatches = new Match[START_MATCHES];
       for (int i = 0; i < startMatches.length; i++) {
           startMatches[i] = new Match();
           startMatches[i].setMatchName(Faker.instance().food().spice().split(" ")[0] + " " + Faker.instance().hobbit().character().replaceAll("(The | Great )", "").split(" ")[0]); // insert commentary on blasé unfairness w/ titles and nobles, blah blah...
       }

       matches = new LinkedList<>();
       availableMatches = new PriorityQueue<>();
       addMatches(startMatches);
    }

    public Matchmaker(Match... ms) {
        matches = new LinkedList<>();
        availableMatches = new PriorityQueue<>();
        addMatches(ms);
    }

    public Match findMatch(String identifier) {
        for (Match m : matches) {
            if (m.matchId.toString().equals(identifier) || m.matchName.equals(identifier)) return m;
        }

        return null;
    }

    public void addMatches(Match... ms) { // Prefer over adding to set for safety
        for (Match m : ms) {
            assert matches.contains(m) : "Duplicate match found in matches!"; // don't allow dupes! set would've been better but had trouble w/ default match... sigh

            if (!m.isFull) availableMatches.add(m); // max of 20 spectators
            matches.add(m);
        }
    }

    public void updateAvailableMatches() {
        availableMatches.removeIf(m -> m.isFull);
        assert matches.size() <= MAX_MATCHES : "Too many matches! (" + matches.size() + " > " + MAX_MATCHES + ")";
        int numLacked = Math.max(0, AVAILABLE_MATCHES - availableMatches.size());

        for (int i = 0; i < numLacked; i++) addMatches(new Match());
    }

    public Match searchMatch() { // get first, least empty match (auto-sorted by treemap)
        updateAvailableMatches();
        return availableMatches.poll();
    }

    public Match connectPlayers(Match match, Player... players) {
        match.addPlayers(players);
        System.out.println("\uD83D\uDD17 Connected to match " + match.matchId);
        return match;
        // todo: amazon ce2 magic or smth
    }

    public void update() {
        updateAvailableMatches();
        for (Match m : matches) m.render();
    }

    public void dispose() {
        matches.forEach(Match::dispose);
    }

    public Match connectPlayers(Player... players) {
        return connectPlayers(searchMatch(), players);
    }

    @Override
    public String toString() {
        return availableMatches.stream().map(Match::toString).collect(Collectors.joining("\n"));
    }
}
