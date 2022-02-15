package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.json.JSONObject;

import java.time.Instant;

/**
 * Created by Billi on 19/03/2018.
 *
 * @author Billi
 */
public class StaffNote implements JSONExportable {
    private int charID;
    private String author;
    private Instant created;
    private Instant updated;
    private String note;

    public StaffNote(int charID, Instant created) {
        this.charID = charID;
        this.created = created;
    }

    // --- Accessors ---
    public int getCharID() {
        return charID;
    }

    public String getAuthor() {
        return author;
    }

    public StaffNote setAuthor(String author) {
        this.author = author;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public StaffNote setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public String getNote() {
        return note;
    }

    public StaffNote setNote(String note) {
        this.note = note;
        return this;
    }

    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("author", getAuthor()); // FIXME: return user object
        res.put("created", JSONUtils.jsonDate(getCreated()));
        res.put("updated", JSONUtils.jsonDate(getUpdated()));
        res.put("note", getNote());
        return res;
    }
}
