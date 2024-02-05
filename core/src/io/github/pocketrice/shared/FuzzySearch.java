package io.github.pocketrice.shared;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;

import java.util.*;
import java.util.stream.Collectors;

public class FuzzySearch {
    Set<String> results;
    LevenshteinDetailedDistance ld;
    int normThreshold, addThreshold; // expected: norm is rather low, add is somewhat higher
    boolean ignoreCase;

    public FuzzySearch(String... r) {
        this(5, 8, true, r);
    }

    public <E> FuzzySearch(Collection<E> col) {
        this(5, 8, true, convertToStrArr(col.toArray()));
    }

    public FuzzySearch(int normThr, int addThr, boolean ignoreCase, String... r) {
        results = Set.of(r);
        normThreshold = normThr;
        addThreshold = addThr;
        this.ignoreCase = ignoreCase;
        ld = LevenshteinDetailedDistance.getDefaultInstance();
    }

    public String[] getFuzzy(String query) { // touch dizzy...
        List<Pair<String, LevenshteinResults>> matches = new ArrayList<>();
        for (String res : results) {
            LevenshteinResults lr = (ignoreCase) ? ld.apply(query.toLowerCase(), res.toLowerCase()) : ld.apply(query, res);
            if (lr.getDistance() < normThreshold || lr.getInsertCount() < addThreshold && lr.getDeleteCount() + lr.getSubstituteCount() < 2) matches.add(Pair.of(res, lr));
        }
        if (matches.isEmpty()) return new String[]{ null }; // prefer array w/ null rather than empty array (for querying array)
        return matches.stream().sorted(Comparator.comparingInt(p -> p.getValue().getDistance())).map(Pair::getKey).toArray(String[]::new);
    }

    public String getHumanFuzzy(String query, boolean promptIfNone) { // Human readable results
        String[] strs = getFuzzy(query);
        return (strs[0] == null) ? (promptIfNone ? "No results found." : "") : "Did you mean " + Arrays.stream(strs).limit(2).collect(Collectors.joining(" or ")) + "?";
    }

    public static <T> String[] convertToStrArr(T[] arr) {
        String[] strs = new String[arr.length];

        for (int i = 0; i < arr.length; i++) {
            strs[i] = arr[i].toString();
        }

        return strs;
    }
}
