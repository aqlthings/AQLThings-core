package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Billi on 27/03/2019.
 * @author Billi
 */
public class CharacterSearchResults {
    private Map<Integer, Integer> scores;
    private Map<Integer, Character> values;

    public CharacterSearchResults() {
        scores = new HashMap<>();
        values = new HashMap<>();
    }

    /**
     * Add a new result if not already present or
     * replace the previous score if the new one is higher.
     * @param score The score to check against
     * @param result The result to add
     * @return false if the result was already in the list with a higher score, true otherwise
     */
    public boolean addResult(int score, Character result) {
        Integer prevScore = scores.get(result.getID());
        if (prevScore!=null && prevScore>score) return false;
        scores.put(result.getID(), score);
        values.put(result.getID(), result);
        return true;
    }

    /**
     * Adds all results with the same score, see {@link #addResult}.
     * @param score The score to check against
     * @param results The results to add
     */
    public void addAllResults(int score, Character... results) {
        for (Character c : results) addResult(score, c);
    }

    /**
     * Adds all results with the same score, see {@link #addResult}.
     * @param score The score to check against
     * @param results The results to add
     */
    public void addAllResults(int score, Collection<Character> results) {
        for (Character c : results) addResult(score, c);
    }

    public int size() {
        return values.size();
    }

    public Character[] getOrderedResults(Character[] res) {
        if (res.length<size()) throw new IllegalArgumentException("Array isn't large enough");
        Map<Integer, Integer> map = new HashMap<>(scores);
        while (map.size()>0) {
            int maxScore = 0;
            int bestResultID = 0;
            for (int charID : map.keySet()) {
                int score = map.get(charID);
                if (score<=maxScore) continue;
                maxScore = score;
                bestResultID = charID;
            }
            res[res.length-map.size()] = values.get(bestResultID);
            map.remove(bestResultID);
        }
        return res;
    }

    public JSONArray getResultsArray() {
        JSONArray res = new JSONArray();
        Map<Integer, Integer> map = new HashMap<>(scores);
        while (map.size()>0) {
            int maxScore = 0;
            int bestResultID = 0;
            for (int charID : map.keySet()) {
                int score = map.get(charID);
                if (score<=maxScore) continue;
                maxScore = score;
                bestResultID = charID;
            }
            JSONObject result = values.get(bestResultID).toJSON(false,false);
            result.put("score",maxScore);
            res.put(result);
            map.remove(bestResultID);
        }
        return res;
    }
}
