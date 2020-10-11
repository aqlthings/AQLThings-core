package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.CharactersHttpAPI;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Billi on 11/03/2018.
 */
public class Character extends AbstractCharacter {
    public static final String PERM_CREATE_SELF_CHARACTER = CharactersHttpAPI.MODULE_NAME+".create.self";
    public static final String PERM_CREATE_SHARED_CHARACTER = CharactersHttpAPI.MODULE_NAME+".create.shared";
    public static final String PERM_VIEW_ALL_CHARACTERS = CharactersHttpAPI.MODULE_NAME+".view.any";
    public static final String PERM_EDIT_ALL_CHARACTERS = CharactersHttpAPI.MODULE_NAME+".edit.any";
    public static final String PERM_STAFF_FIELDS_VIEW = CharactersHttpAPI.MODULE_NAME+".fields.staff.view";
    public static final String PERM_STAFF_FIELDS_EDIT = CharactersHttpAPI.MODULE_NAME+".fields.staff.edit";
    public static final String PERM_UNLIMITED_CHARS = CharactersHttpAPI.MODULE_NAME+".limits.chars.none";
    public static final String PERM_UNLIMITED_SKINS = CharactersHttpAPI.MODULE_NAME+".limits.skins.none";
    public static final String PERM_UNLIMITED_SKILLS = CharactersHttpAPI.MODULE_NAME+".limits.skills.none";
    public static final String PERM_VIEW_HISTORY = CharactersHttpAPI.MODULE_NAME+".history.view";
    public static final String PERM_EDIT_HISTORY = CharactersHttpAPI.MODULE_NAME+".history.edit";
    public static final String PERM_VIEW_CHAR_CATEGORY = CharactersHttpAPI.MODULE_NAME+".category.{{CATEGORY}}.chars.view";
    public static final String PERM_USE_CHAR_CATEGORY = CharactersHttpAPI.MODULE_NAME+".category.{{CATEGORY}}.chars.use";
    public static final String PERM_VIEW_SKIN_CATEGORY = CharactersHttpAPI.MODULE_NAME+".category.{{CATEGORY}}.skins.view";
    public static final String PERM_USE_SKIN_CATEGORY = CharactersHttpAPI.MODULE_NAME+".category.{{CATEGORY}}.skins.use";
    public static final String PERM_USE_RACE_CATEGORY = CharactersHttpAPI.MODULE_NAME+".category.{{CATEGORY}}.races.use";

    private int id;
    private Status status;
    private String statusComment;
    private String category;
    private String firstName;
    private String lastName;
    private String birth;
    private int age;
    private int luckPoints;
    private String origins;
    private String religion;
    private String occupation;

    private String physicalDesc;
    private String story;
    private String details;
    private List<StaffNote> staffNotes;

    public Character(int id, String pUUID) {
        super(pUUID);
        this.id = id;
    }

    // --- Accessors ---

    public int getID() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public Character setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getStatusComment() {
        return statusComment;
    }

    public Character setStatusComment(String statusComment) {
        this.statusComment = statusComment;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Character setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Character setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Character setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    @Override
    public String getName() {
        return getFirstName()+(getLastName()!=null?" "+getLastName():"");
    }

    public String getBirth() {
        return birth;
    }

    public Character setBirth(String birth) {
        this.birth = birth;
        return this;
    }

    public int getAge() {
        return age;
    }

    public Character setAge(int age) {
        this.age = age;
        return this;
    }

    public int getLuckPoints() {
        return luckPoints;
    }

    public Character setLuckPoints(int luckPoints) {
        this.luckPoints = luckPoints;
        return this;
    }

    public String getOrigins() {
        return origins;
    }

    public Character setOrigins(String origins) {
        this.origins = origins;
        return this;
    }

    public String getReligion() {
        return religion;
    }

    public Character setReligion(String religion) {
        this.religion = religion;
        return this;
    }

    public String getOccupation() {
        return occupation;
    }

    public Character setOccupation(String occupation) {
        this.occupation = occupation;
        return this;
    }

    public String getPhysicalDescription() {
        return physicalDesc;
    }

    public Character setPhysicalDescription(String physicalDesc) {
        this.physicalDesc = physicalDesc;
        return this;
    }

    public String getStory() {
        return story;
    }

    public Character setStory(String story) {
        this.story = story;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public Character setDetails(String details) {
        this.details = details;
        return this;
    }

    public List<StaffNote> getStaffNotes() {
        return staffNotes;
    }

    public Character setStaffNotes(List<StaffNote> staffNotes) {
        this.staffNotes = staffNotes;
        return this;
    }
    // --- Methods ---

    public boolean isSharedCharacter() {
        return getPlayerUUID()==null;
    }

    public JSONObject toJSON(boolean details, boolean skipFilter) {
        JSONObject res = new JSONObject();
        res.put("id",getID());
        res.put("status",getStatus().name().toLowerCase());
        res.put("firstName",getFirstName());
        res.put("lastName",getLastName()); // Might be null
        res.put("name",getName());
        res.put("sex",getSex()=='M'?"male":"female");
        res.put("race",getRace());
        res.put("birth", getBirth());
        res.put("age",getAge());
        if (details) {
            res.put("luckPoints", getLuckPoints());
            res.put("height", getHeight());
            res.put("statusComment", getStatusComment());
            res.put("origins",getOrigins());
            res.put("religion",getReligion());
            res.put("occupation",getOccupation());
        }
        if (getPlayerUUID()==null) { // Shared character
            res.put("category",getCategory());
        } else if (!skipFilter) { // Player character
            res.put("player",getPlayerUUID());
            res.put("playerName",getPlayerName());
        }
        return res;
    }

    public boolean userCanView(APIUser u) {
        if (u.hasPerm(PERM_VIEW_ALL_CHARACTERS)) return true;
        if (isSharedCharacter()) {
            return u.hasPerm(PERM_VIEW_CHAR_CATEGORY.replace("{{CATEGORY}}",getCategory()));
        }
        return userIsOwner(u);
    }

    public boolean userCanUse(APIUser u) {
        if (u.hasPerm(PERM_EDIT_ALL_CHARACTERS)) return true;
        if (isSharedCharacter()) {
            return u.hasPerm(PERM_USE_CHAR_CATEGORY.replace("{{CATEGORY}}",getCategory()));
        }
        return userIsOwner(u);
    }

    public boolean userIsOwner(APIUser u) {
        if (!(u instanceof APIForumUser)) return false;
        return getPlayerUUIDObject().equals(((APIForumUser) u).getMinecraftUUID());
    }

    public enum Status {
        CREATED,
        PENDING,
        REJECTED,
        ACTIVATED,
        ARCHIVED;

        public static Status find(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
