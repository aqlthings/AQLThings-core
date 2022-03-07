package fr.aquilon.minecraft.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is meant as a way to store results of a query
 * and return an ordered list with the best result first.
 *
 * Created by Billi on 19/03/2019.
 * @author Billi
 */
public class ScoredResultList<T> {
    private final Map<T, Integer> results;

    public ScoredResultList() {
        results = new HashMap<>();
    }

    /**
     * Add a new result if not already present or
     * replace the previous score if the new one is higher.
     * @param result The result to add
     * @param score The score to check against
     * @return false if the result was already in the list with a higher score, true otherwise
     */
    public boolean addResult(T result, int score) {
        Integer prevScore = results.get(result);
        if (prevScore!=null && prevScore>score) return false;
        results.put(result, score);
        return true;
    }

    /**
     * Adds all results with the same score, see {@link #addResult}.
     * @param score The score to check against
     * @param results The results to add
     */
    public void addAllResults(int score, Iterable<T> results) {
        for (T t : results) addResult(t, score);
    }

    public int size() {
        return results.size();
    }

    public T[] getOrderedResults(T[] res) {
        if (res.length<size()) throw new IllegalArgumentException("Array isn't large enough");
        Map<T, Integer> map = new HashMap<>(results);
        while (map.size()>0) {
            int maxScore = 0;
            T bestResult = null;
            for (T c : map.keySet()) {
                int score = map.get(c);
                if (score<=maxScore) continue;
                maxScore = score;
                bestResult = c;
            }
            res[res.length-map.size()] = bestResult;
            map.remove(bestResult);
        }
        return res;
    }
}
