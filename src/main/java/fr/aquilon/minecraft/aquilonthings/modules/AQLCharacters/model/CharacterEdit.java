package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIForumUser;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import name.fraser.neil.plaintext.diff_match_patch;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Billi on 28/05/2019.
 *
 * @author Billi
 */
public class CharacterEdit implements JSONExportable {
    private int charID;
    private Field field;
    private Instant updated;
    private int author;
    private String status;
    private String comment;
    private String diff;

    public CharacterEdit(int charID, Field field, Instant updated, int author) {
        this.charID = charID;
        this.field = field;
        this.updated = updated;
        this.author = author;
        this.status = "new";
    }

    public int getCharID() {
        return charID;
    }

    public Field getField() {
        return field;
    }

    public Instant getUpdated() {
        return updated;
    }

    public int getAuthor() {
        return author;
    }

    public String getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public String getDiff() {
        return diff;
    }

    public CharacterEdit setStatus(String status) {
        this.status = status;
        return this;
    }

    public CharacterEdit setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public CharacterEdit setDiff(String diff) {
        this.diff = diff;
        return this;
    }

    public CharacterEdit setDiff(Object oldValue, Object newValue) throws IllegalArgumentException {
        this.diff = field.getDiff(oldValue, newValue);
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("author", APIForumUser.fromUID(author, null).toJSON());
        res.put("updated", JSONUtils.jsonDate(updated));
        res.put("status", status);
        res.put("comment", comment);
        res.put("field", field.name().toLowerCase());
        res.put("diff", diff);
        return res;
    }

    /**
     * Wrapper around diff_match_patch library
     * @param oldStr The old text
     * @param newStr The new text
     * @return A list of row diffs
     */
    public static List<diff_match_patch.Diff> getTextDiff(String oldStr, String newStr) {
        if ((oldStr == null && newStr == null) || (oldStr != null && oldStr.equals(newStr))) return null;

        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(oldStr, newStr);
        dmp.diff_cleanupSemantic(diff);

        return diff;
    }

    public static JSONArray diffsToJSON(List<diff_match_patch.Diff> diffs) {
        if (diffs == null) return null;
        JSONArray res = new JSONArray();
        for (diff_match_patch.Diff d : diffs) res.put(diffToJSON(d));
        return res;
    }

    public static JSONObject diffToJSON(diff_match_patch.Diff diff) {
        JSONObject res = new JSONObject();
        res.put("type", diff.operation.name().toLowerCase());
        res.put("text", diff.text);
        return res;
    }

    private static JSONObject integerDiff(int a, int b) {
        if (a == b) return null;
        JSONObject res = new JSONObject();
        res.put("old", a);
        res.put("new", b);
        return res;
    }

    private static JSONObject stateDiff(Character.Status a, Character.Status b) {
        if (a == b) return null;
        JSONObject res = new JSONObject();
        res.put("old", a != null ? a.name().toLowerCase() : JSONObject.NULL);
        res.put("new", b != null ? b.name().toLowerCase() : JSONObject.NULL);
        return res;
    }

    private static JSONObject stringDiff(String a, String b) {
        if (a != null && a.equals(b)) return null;
        JSONObject res = new JSONObject();
        res.put("old", a);
        res.put("new", b);
        return res;
    }

    public enum Field {
        STATUS,
        STATUS_COMMENT,
        AGE,
        RELIGION,
        OCCUPATION,
        //SKILL, // TODO
        DESC_PHYSICAL,
        DESC_STORY,
        DESC_DETAILS;

        public CharacterEdit getCharacterEdit(Character oldChar, Character newChar, Instant updated, int author) {
            return new CharacterEdit(oldChar.getID(), this, updated, author)
                    .setDiff(getField(oldChar), getField(newChar));
        }

        public CharacterEdit getCharacterEdit(int charID, Instant updated, int author, Object oldValue, Object newValue) {
            return new CharacterEdit(charID, this, updated, author)
                    .setDiff(getDiff(oldValue, newValue));
        }

        public String getDiff(Object oldValue, Object newValue) {
            switch (this) {
                case STATUS:
                    JSONObject stateDiff = stateDiff((Character.Status) oldValue, (Character.Status) newValue);
                    return stateDiff != null ? stateDiff.toString() : null;
                case STATUS_COMMENT:
                    JSONObject stringDiff = stringDiff((String) oldValue, (String) newValue);
                    return stringDiff != null ? stringDiff.toString() : null;
                case AGE:
                    JSONObject intDiff = integerDiff((Integer) oldValue, (Integer) newValue);
                    return intDiff != null ? intDiff.toString() : null;
                case RELIGION:
                case OCCUPATION:
                case DESC_PHYSICAL:
                case DESC_STORY:
                case DESC_DETAILS:
                    JSONArray textDiff = diffsToJSON(getTextDiff((String) oldValue, (String) newValue));
                    return textDiff != null && textDiff.length() > 0 ? textDiff.toString() : null;
                default:
                    throw new UnsupportedOperationException("Field not supported");
            }
        }

        public Object getField(Character c) {
            switch (this) {
                case STATUS:
                    return c.getStatus();
                case STATUS_COMMENT:
                    return c.getStatusComment();
                case AGE:
                    return c.getAge();
                case RELIGION:
                    return c.getReligion();
                case OCCUPATION:
                    return c.getOccupation();
                case DESC_PHYSICAL:
                    return c.getPhysicalDescription();
                case DESC_STORY:
                    return c.getStory();
                case DESC_DETAILS:
                    return c.getDetails();
                default:
                    throw new UnsupportedOperationException("Field not supported");
            }
        }
    }
}
