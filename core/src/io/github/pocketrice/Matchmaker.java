//package io.github.pocketrice;
//
//import java.util.*;
//
//public class Matchmaker {
//    Set<Match> matches;
//    Queue<Match> availableMatches;
//
//    public Matchmaker() {
//       super();
//    }
//
//    public Matchmaker(Match... ms) {
//        matches = new TreeSet<>();
//        availableMatches = new PriorityQueue<>();
//        addMatches(ms);
//    }
//
//    public Match findMatch(UUID mId) { // fix params? return?
//        for (Match m : matches) {
//            if (m.matchId.equals(mId)) return m;
//        }
//
//        return null;
//    }
//
//    public void addMatches(Match... ms) {
//        for (Match m : ms) {
//            if (matches.contains(m)) throw new IllegalArgumentException("Duplicate match found in matches!"); // sets can't have dupes!! I don't want dupes anyway...
//
//            if (!m.isFull) availableMatches.add(m); // max of 20 spectators
//            matches.add(m);
//        }
//    }
//
//    public void updateAvailableMatches() {
//        availableMatches.removeIf(m -> m.isFull);
//    }
//
//    public Match searchMatch() { // get first, least empty match (auto-sorted by treemap)
//        updateAvailableMatches();
//        return availableMatches.poll();
//    }
//
//    public void connectPlayers(Match match, Player... players) {
//        match.addPlayers(players);
//        // todo: amazon ce2 magic or smth
//    }
//
//    public void connectPlayers(Player... players) {
//        connectPlayers(searchMatch(), players);
//    }
//}
