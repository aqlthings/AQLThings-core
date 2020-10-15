package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.CharacterNotFoundEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.MaxCharCountReachedEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.MaxSkinCountReachedEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.AbstractCharacter;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharOrigins;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharRace;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Character;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterEdit;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterPlayer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSearchResults;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CommonSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Skill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.SkillCategory;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.StaffNote;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.TempCharacter;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.RedirectionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * Created by Billi on 10/03/2018.
 * FIXME: validate character skin name (url args only support one word)
 *
 * @author Billi
 */
@SuppressWarnings("unchecked")
public class CharactersHttpAPI extends APIModule {
    public static final String MODULE_NAME = "characters";

    public static final String SUBERR_CHARACTER_NOT_FOUND = "101";
    public static final String SUBERR_CATEGORY_NOT_FOUND = "102";
    public static final String SUBERR_SKIN_NOT_FOUND = "103";
    public static final String SUBERR_STAFF_NOTE_NOT_FOUND = "104";
    public static final String SUBERR_NO_SUCH_FIELD = "105";
    public static final String SUBERR_INVALID_CHAR_NAME = "201";
    public static final String SUBERR_INVALID_CHAR_SEX = "202";
    public static final String SUBERR_INVALID_CHAR_RACE = "203";
    public static final String SUBERR_INVALID_CHAR_AGE = "204";
    public static final String SUBERR_EMPTY_REQUEST = "205";
    public static final String SUBERR_INVALID_COMMON_SKIN_CAT = "206";
    public static final String SUBERR_INVALID_SKIN_IMAGE = "207";
    public static final String SUBERR_INVALID_CHAR_HEIGHT = "208";
    public static final String SUBERR_INVALID_CHAR_LUCK_POINTS = "209";
    public static final String SUBERR_INVALID_CHAR_ORIGINS = "210";
    public static final String SUBERR_INVALID_CHAR_SKILL = "211";
    public static final String SUBERR_INVALID_CHAR_SKILL_LEVEL = "212";
    public static final String SUBERR_INVALID_CHAR_SKILL_COMMENT = "213";
    public static final String SUBERR_SKIN_SELECTED = "214";
    public static final String SUBERR_INVALID_SKIN_NAME = "215";
    public static final String SUBERR_INVALID_STATUS = "216";
    public static final String SUBERR_INVALID_CHAR_SKILL_CATEGORY = "217";
    public static final String SUBERR_NO_EDIT_FOUND = "218";
    public static final String SUBERR_INVALID_PLAYER = "301";
    public static final String SUBERR_FORBIDEN_USER_TYPE = "302";
    public static final String SUBERR_PLAYER_NOT_FOUND = "303";
    public static final String SUBERR_FORBIDEN_CATEGORY = "304";
    public static final String SUBERR_FORBIDEN_STATUS = "305";
    public static final String SUBERR_TOO_MANY_CHARS = "401";
    public static final String SUBERR_TOO_MANY_SKINS = "402";

    public static final String[] DEFAULT_CATEGORIES = {"staff", "nobles"};

    private final AQLCharacters m;
    private final CharacterDatabase charDB;
    private IWebsocket ws;

    public CharactersHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        m = AquilonThings.instance.getModuleData(AQLCharacters.class);
        if (m == null) throw new IllegalStateException("AQLCharacters module is not enabled !");
        charDB = m.getCharacterDB();
    }

    public static final String ROUTE_GET_CHARACTER = "getCharacter";
    public static final String ROUTE_POST_CREATE_CHARACTER = "createCharacter";
    public static final String ROUTE_PUT_UPDATE_CHARACTER = "updateCharacter";
    public static final String ROUTE_DELETE_CHARACTER = "deleteCharacter";
    public static final String ROUTE_GET_CHARACTER_DESC = "getCharacterDescriptions";
    public static final String ROUTE_PUT_CHARACTER_DESC = "updateCharacterDescriptions";
    public static final String ROUTE_GET_CHARACTER_SKINS = "getCharacterSkins";
    public static final String ROUTE_GET_CHARACTER_SKIN = "getCharacterSkin";
    public static final String ROUTE_POST_CHARACTER_SKIN = "addCharacterSkin";
    public static final String ROUTE_PUT_UPDATE_CHARACTER_SKIN = "updateCharacterSkin";
    public static final String ROUTE_DELETE_CHARACTER_SKIN = "deleteCharacterSkin";
    public static final String ROUTE_GET_CHARACTER_STAFF_NOTES = "getCharacterStaffNotes";
    public static final String ROUTE_POST_CREATE_CHARACTER_STAFF_NOTE = "addCharacterStaffNote";
    public static final String ROUTE_PUT_EDIT_CHARACTER_STAFF_NOTE = "updateCharacterStaffNote";
    public static final String ROUTE_DELETE_CHARACTER_STAFF_NOTE = "deleteCharacterStaffNote";
    public static final String ROUTE_GET_CHARACTER_DATA = "getCharacterData";
    public static final String ROUTE_GET_CHARACTER_PLAYER = "getCharacterPlayer";
    public static final String ROUTE_GET_PLAYER_CHARACTERS = "getPlayerCharacters";
    public static final String ROUTE_GET_TEMP_CHARACTER = "getTempCharacter";
    public static final String ROUTE_GET_TEMP_CHARACTER_SKIN = "getTempCharacterSkin";
    public static final String ROUTE_POST_TEMP_CHARACTER_SKIN = "setTempCharacterSkin";
    public static final String ROUTE_DELETE_TEMP_CHARACTER_SKIN = "deleteTempCharacterSkin";
    public static final String ROUTE_GET_CHARACTER_CATEGORIES = "getCharacterCategories";
    public static final String ROUTE_GET_SHARED_CHARACTERS = "getSharedCharacters";
    public static final String ROUTE_POST_SEARCH_CHARACTERS = "searchCharacters";
    public static final String ROUTE_GET_CHARACTERS_BY_STATUS = "searchCharactersWithStatus";
    public static final String ROUTE_GET_CHARACTER_SKILLS = "getCharacterSkills";
    public static final String ROUTE_GET_CHARACTER_SKILLS_CAT = "getCharacterSkillsCategories";
    public static final String ROUTE_PUT_UNLOCK_CHARACTER_SKILL_CAT = "unlockCharacterSkillCategory";
    public static final String ROUTE_DELETE_CHARACTER_SKILL_CAT = "deleteCharacterSkillCategory";
    public static final String ROUTE_GET_CHARACTER_SKILL = "getCharacterSkill";
    public static final String ROUTE_PUT_UPDATE_CHARACTER_SKILL = "updateCharacterSkill";
    public static final String ROUTE_GET_CHARACTER_FIELD_LAST_EDITS = "getCharacterFieldLastEdit";
    public static final String ROUTE_GET_CHARACTER_FIELD_EDIT = "getCharacterFieldEdit";
    public static final String ROUTE_PUT_CHARACTER_FIELD_EDIT = "updateCharacterFieldEdit";
    public static final String ROUTE_GET_CHARACTER_EDITS = "getCharacterEdits";
    public static final String ROUTE_GET_LAST_EDITS = "getLastEdits";
    public static final String ROUTE_GET_LAST_EDITS_WITH_STATUS = "getLastEditsWithStatus";
    public static final String ROUTE_GET_RACES = "getRaces";
    public static final String ROUTE_POST_NEW_RACE = "createRace";
    public static final String ROUTE_DELETE_RACE = "deleteRace";
    public static final String ROUTE_GET_ORIGINS = "getOrigins";
    public static final String ROUTE_POST_NEW_ORIGIN = "createOrigin";
    public static final String ROUTE_DELETE_ORIGIN = "deleteOrigin";
    public static final String ROUTE_GET_ALL_SKILLS = "getAllSkills";
    public static final String ROUTE_GET_SKILL_CATEGORIES = "getSkillCategories";
    public static final String ROUTE_GET_SKILLS = "getSkills";
    public static final String ROUTE_GET_COMMON_SKIN_CATEGORIES = "getCommonSkinCategories";
    public static final String ROUTE_GET_COMMON_SKINS_CATEGORY = "getCommonSkinsCategory";
    public static final String ROUTE_GET_COMMON_SKIN = "getCommonSkin";
    public static final String ROUTE_POST_SEARCH_COMMON_SKINS = "searchCommonSkins";
    public static final String ROUTE_POST_CREATE_COMMON_SKINS = "createCommonSkin";
    public static final String ROUTE_DELETE_COMMON_SKINS = "deleteCommonSkin";
    public static final String ROUTE_GET_CONTEXT = "getContext";

    @Override
    public void onReady() {
        registerRoute(ROUTE_GET_CONTEXT, NanoHTTPD.Method.GET, "/context", this::getContext);
        registerRoute(ROUTE_GET_ORIGINS, NanoHTTPD.Method.GET, "/context/origins", this::getOrigins);
        registerRoute(ROUTE_GET_RACES, NanoHTTPD.Method.GET, "/context/races", this::getRaces);
        registerRoute(ROUTE_GET_ALL_SKILLS, NanoHTTPD.Method.GET, "/context/skills", this::getSkills);
        registerRoute(ROUTE_GET_COMMON_SKIN_CATEGORIES, NanoHTTPD.Method.GET, "/common/skins/", this::getCommonSkinCategories);
        registerRoute(ROUTE_GET_COMMON_SKINS_CATEGORY, NanoHTTPD.Method.GET, "/common/skins/{string:category}", this::getCommonSkinsCategory); // Params: 0> Category
        registerRoute(ROUTE_GET_COMMON_SKIN, NanoHTTPD.Method.GET, "/common/skins/s/{int:skin-id}", this::getCommonSkin);
        registerRoute(ROUTE_POST_CREATE_COMMON_SKINS, NanoHTTPD.Method.POST, "/common/skins/{string:category}", this::createCommonSkin);
        //registerRoute(ROUTE_DELETE_COMMON_SKINS, NanoHTTPD.Method.DELETE, "/common/skins/s/{int:skin-id}", this::deleteCommonSkin);
        //registerRoute(ROUTE_POST_SEARCH_COMMON_SKINS, NanoHTTPD.Method.GET, "/common/skins", this::getCommonSkin);
        registerRoute(ROUTE_GET_CHARACTER_CATEGORIES, NanoHTTPD.Method.GET, "/common/chars", this::getCharacterCategories);
        registerRoute(ROUTE_GET_SHARED_CHARACTERS, NanoHTTPD.Method.GET, "/common/chars/{string:category}", this::getSharedCharacters);
        registerRoute(ROUTE_POST_CREATE_CHARACTER, NanoHTTPD.Method.POST, "/c", this::createCharacter);
        registerRoute(ROUTE_PUT_UPDATE_CHARACTER, NanoHTTPD.Method.PUT, "/c/{int:char-id}", this::updateCharacter);
        registerRoute(ROUTE_GET_CHARACTER, NanoHTTPD.Method.GET, "/c/{int:char-id}", this::getCharacter);
        //registerRoute(ROUTE_DELETE_CHARACTER, NanoHTTPD.Method.DELETE, "/c/{int:char-id}", this::deleteCharacter);
        registerRoute(ROUTE_GET_CHARACTER_DESC, NanoHTTPD.Method.GET, "/c/{int:char-id}/desc", this::getCharacterDescriptions);
        registerRoute(ROUTE_PUT_CHARACTER_DESC, NanoHTTPD.Method.PUT, "/c/{int:char-id}/desc", this::updateCharacterDescriptions);
        registerRoute(ROUTE_GET_CHARACTER_SKINS, NanoHTTPD.Method.GET, "/c/{int:char-id}/skins", this::getCharacterSkins);
        registerRoute(ROUTE_POST_CHARACTER_SKIN, NanoHTTPD.Method.POST, "/c/{int:char-id}/skins", this::addCharacterSkin);
        registerRoute(ROUTE_GET_CHARACTER_SKIN, NanoHTTPD.Method.GET, "/c/{int:char-id}/skins/{string:skin-name}", this::getCharacterSkin);
        registerRoute(ROUTE_PUT_UPDATE_CHARACTER_SKIN, NanoHTTPD.Method.PUT, "/c/{int:char-id}/skins/{string:skin-name}", this::updateCharacterSkin);
        registerRoute(ROUTE_DELETE_CHARACTER_SKIN, NanoHTTPD.Method.DELETE, "/c/{int:char-id}/skins/{string:skin-name}", this::deleteCharacterSkin);
        registerRoute(ROUTE_GET_CHARACTER_STAFF_NOTES, NanoHTTPD.Method.GET, "/c/{int:char-id}/staff-notes", this::getCharacterStaffNotes);
        registerRoute(ROUTE_POST_CREATE_CHARACTER_STAFF_NOTE, NanoHTTPD.Method.POST, "/c/{int:char-id}/staff-notes", this::addCharacterStaffNote);
        registerRoute(ROUTE_PUT_EDIT_CHARACTER_STAFF_NOTE, NanoHTTPD.Method.PUT, "/c/{int:char-id}/staff-notes/{long:time}", this::updateCharacterStaffNote);
        registerRoute(ROUTE_DELETE_CHARACTER_STAFF_NOTE, NanoHTTPD.Method.DELETE, "/c/{int:char-id}/staff-notes/{long:time}", this::deleteCharacterStaffNote);
        registerRoute(ROUTE_GET_CHARACTER_DATA, NanoHTTPD.Method.GET, "/c/{int:char-id}/data", this::getCharacterData);
        registerRoute(ROUTE_GET_CHARACTER_EDITS, NanoHTTPD.Method.GET, "/c/{int:char-id}/edits", this::getLastCharacterEdits);
        registerRoute(ROUTE_GET_CHARACTER_FIELD_LAST_EDITS, NanoHTTPD.Method.GET, "/c/{int:char-id}/edits/{string:field}", this::getLastCharacterFieldEdits);
        registerRoute(ROUTE_GET_CHARACTER_FIELD_EDIT, NanoHTTPD.Method.GET, "/c/{int:char-id}/edits/{string:field}/{long:time}", this::getCharacterFieldEdit);
        registerRoute(ROUTE_PUT_CHARACTER_FIELD_EDIT, NanoHTTPD.Method.PUT, "/c/{int:char-id}/edits/{string:field}/{long:time}", this::updateCharacterFieldEdit);
        registerRoute(ROUTE_GET_CHARACTER_SKILLS, NanoHTTPD.Method.GET, "/c/{int:char-id}/skills", this::getCharacterSkills);
        //registerRoute(ROUTE_GET_CHARACTER_SKILLS_CAT, NanoHTTPD.Method.GET, "/c/{int:char-id}/skills/{string:category}");
        registerRoute(ROUTE_PUT_UNLOCK_CHARACTER_SKILL_CAT, NanoHTTPD.Method.PUT, "/c/{int:char-id}/skills/{string:category}", this::unlockCharacterSkillCategory);
        //registerRoute(ROUTE_GET_CHARACTER_SKILL, NanoHTTPD.Method.GET, "/c/{int:char-id}/skills/{string:category}/{string:skill}");
        registerRoute(ROUTE_DELETE_CHARACTER_SKILL_CAT, NanoHTTPD.Method.DELETE, "/c/{int:char-id}/skills/{string:category}", this::deleteCharacterSkillCategory);
        registerRoute(ROUTE_PUT_UPDATE_CHARACTER_SKILL, NanoHTTPD.Method.PUT, "/c/{int:char-id}/skills/{string:category}/{string:skill}", this::updateCharacterSkill);
        registerRoute(ROUTE_GET_CHARACTER_PLAYER, NanoHTTPD.Method.GET, "/p/{uuid:player}", this::getCharacterPlayer);
        registerRoute(ROUTE_GET_PLAYER_CHARACTERS, NanoHTTPD.Method.GET, "/p/{uuid:player}/chars", this::getPlayerCharacters);
        registerRoute(ROUTE_GET_TEMP_CHARACTER, NanoHTTPD.Method.GET, "/p/{uuid:player}/temp-char", this::getTempCharacter);
        registerRoute(ROUTE_GET_TEMP_CHARACTER_SKIN, NanoHTTPD.Method.GET, "/p/{uuid:player}/temp-char/skin", this::getTempCharacterSkin);
        registerRoute(ROUTE_POST_TEMP_CHARACTER_SKIN, NanoHTTPD.Method.POST, "/p/{uuid:player}/temp-char/skin", this::setTempCharacterSkin);
        registerRoute(ROUTE_DELETE_TEMP_CHARACTER_SKIN, NanoHTTPD.Method.DELETE, "/p/{uuid:player}/temp-char/skin", this::deleteTempCharacterSkin);
        registerRoute(ROUTE_POST_SEARCH_CHARACTERS, NanoHTTPD.Method.POST, "/search/chars", this::searchChars);
        registerRoute(ROUTE_GET_CHARACTERS_BY_STATUS, NanoHTTPD.Method.GET, "/search/chars/status/{string:status}", this::searchCharactersByStatus);
        registerRoute(ROUTE_GET_LAST_EDITS, NanoHTTPD.Method.GET, "/search/chars/edits/", this::getLastEdits);
        registerRoute(ROUTE_GET_LAST_EDITS_WITH_STATUS, NanoHTTPD.Method.GET, "/search/chars/edits/{string:status}", this::getLastEdits);
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onPlayerCharacterChange(PlayerCharacterChangeEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".character", res);
    }

    @EventHandler
    public void onPlayerRace(PlayerRaceEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".race", res);
    }

    public JSONObject getCharacter(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        return pChar.toJSON(true, false);
    }

    public JSONObject getTempCharacter(APIRequest r) throws APIException {
        if (r.getUser().isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid;
        if (r.hasArg("player")) {
            uuid = r.getArg("player").getAsString();
        } else {
            if (!(r.getUser() instanceof APIForumUser))
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                        "Static user aren't allowed to update characters descriptions")
                        .addData("code", 1);
            APIForumUser user = (APIForumUser) r.getUser();
            if (user.getMinecraftPseudo()==null)
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                        "Player has no minecraft account linked")
                        .addData("code",2);
            if (user.getMinecraftUUID()==null)
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                        "Unable to find minecraft account")
                        .addData("code",3);
            uuid = user.getMinecraftUUID().toString().replaceAll("-","");
        }
        TempCharacter tChar = charDB.findTempCharacter(uuid);
        if (tChar==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No temporary character found for this user.");
        if (!tChar.userCanView(r.getUser()))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        return tChar.toJSON(true, true);
    }

    public JSONObject getCharacterData(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        try {
            charDB.fillCharacterDescriptions(pChar);
        } catch (CharacterNotFoundEx characterNotFoundEx) { // Should not be possible
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No such character.");
        }
        JSONObject res = pChar.toJSON(true, false);
        List<CharacterSkin> skins = charDB.findCharacterSkins(charID);
        JSONObject skinList = new JSONObject();
        for (CharacterSkin s : skins) {
            skinList.put(s.getName(), s.getFile());
        }
        res.put("skins", skinList);
        JSONObject desc = new JSONObject();
        desc.put("physicalDesc",pChar.getPhysicalDescription());
        desc.put("story",pChar.getStory());
        desc.put("details",pChar.getDetails());
        res.put("desc", desc);
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_VIEW)) {
            charDB.fillCharacterStaffNotes(pChar);
            res.put("staffNotes", JSONUtils.jsonArray(pChar.getStaffNotes()));
            res.put("weight", pChar.getWeight());
        }
        List<SkillCategory> unlockedSkillCats = charDB.findCharacterSkillCategories(charID);
        unlockedSkillCats.addAll(charDB.findRequiredSkillCategories());
        JSONObject skills = new JSONObject();
        for (SkillCategory cat : unlockedSkillCats) {
            skills.put(cat.getName(), new JSONObject());
        }
        List<CharacterSkill> skillList = charDB.findCharacterSkills(charID);
        for (CharacterSkill s: skillList) {
            //if (s.getLevel()==0) continue;
            JSONObject cat = (JSONObject) skills.get(s.getCategory());
            cat.put(s.getName(), s.toJSON());
        }
        res.put("skills", skills);
        return res;
    }

    public JSONObject createCharacter(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to create characters"
            ).addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        if (user.getMinecraftPseudo()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player has no minecraft account linked")
                    .addData("code",2);
        if (user.getMinecraftUUID()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Unable to find minecraft account")
                    .addData("code",3);

        JSONObject req = r.getJSONRequest();
        Character pChar = new Character(-1,null);
        // -- category / player
        if (req.get("category")!=null) {
            if (!user.hasPerm(Character.PERM_CREATE_SHARED_CHARACTER)) {
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                        "Not allowed to create shared characters")
                        .addData("code",4);
            }
            String category = req.get("category").toString(); // TODO: validate
            pChar.setCategory(category);
            if (!pChar.userCanUse(user))
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_CATEGORY,
                        "You are not allowed to use this category");
        } else {
            if (!user.hasPerm(Character.PERM_CREATE_SELF_CHARACTER)) {
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                        "Not allowed to create self characters")
                        .addData("code",5);
            }
            // TODO: check user is ForumUser to avoid NPE and ClassCastEx
            pChar.setPlayerUUID(((APIForumUser) user).getMinecraftUUID().toString().replaceAll("-",""));
        }
        // -- first name
        // TODO: validate input (special chars and length)
        if (req.has("firstName")) {
            pChar.setFirstName(req.get("firstName").toString()); // TODO: validate
        } else throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_NAME,
                "Missing or invalid character name.")
                .addData("code", 1)
                .addData("info", "Missing mandatory firstName.");
        // -- last name
        if (req.has("lastName"))
            pChar.setLastName(req.get("lastName").toString()); // TODO: validate
        if (!req.has("sex") || req.get("sex")==null) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SEX,
                    "Missing or invalid character sex.")
                    .addData("code", 1)
                    .addData("info", "Missing sex, either 'M' or 'F'.");
        }
        // -- sex
        try {
            pChar.setSex(req.get("sex").toString().toUpperCase().charAt(0));
        } catch (IllegalArgumentException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SEX,
                    "Missing or invalid character sex.")
                    .addData("code", 2)
                    .addData("info", "Invalid sex, can only by 'M' or 'F'.");
        }
        // -- race
        if (!req.has("race") || req.get("race")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_RACE,
                    "Missing or invalid character race.")
                    .addData("code", 1)
                    .addData("info", "Missing race.");
        CharRace cRace = charDB.findRaceFromName(req.get("race").toString());
        if (cRace==null || !cRace.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_RACE,
                    "Missing or invalid character race.")
                    .addData("code", 2)
                    .addData("info", "No such race.");
        pChar.setRace(cRace.getName());
        // -- height
        if (req.get("height")!=null) {
            float cHeight;
            try {
                cHeight = Float.parseFloat(req.get("height").toString());
            } catch (NumberFormatException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_HEIGHT,
                        "Invalid character height.")
                        .addData("code", 1)
                        .addData("info", "Decimal number expected.");
            }
            if (cHeight>cRace.getMaxHeight() || cHeight<cRace.getMinHeight()) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_HEIGHT,
                        "Invalid character height.")
                        .addData("code", 2)
                        .addData("info", "Height out of bounds for this race.")
                        .addData("min", cRace.getMinHeight())
                        .addData("max", cRace.getMaxHeight());
            }
            pChar.setHeight(cHeight);
        }

        // -- age
        if (req.get("age")!=null) {
            try {
                pChar.setAge(Integer.parseUnsignedInt(req.get("age").toString()));
            } catch (NumberFormatException e) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_AGE,
                        "Invalid character age.")
                        .addData("code", 2)
                        .addData("info", "Age is a number.");
            }
        }
        // -- birth
        if (req.get("birth")!=null) {
            pChar.setBirth(req.get("birth").toString()); // TODO: validate (format ?)
        }
        // -- origins
        if (req.get("origins")!=null) { // TODO: is null allowed ?
            List<CharOrigins> list = charDB.findOrigins();
            CharOrigins origins = null;
            for (CharOrigins o : list) {
                if (!o.getName().equals(req.get("origins").toString())) continue;
                origins = o;
                break;
            }
            if (origins==null)
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_ORIGINS,
                        "Invalid char origins")
                        .addData("list", JSONUtils.jsonArray(list));
            pChar.setOrigins(origins.getName());
        }
        // -- occupation
        if (req.has("occupation")) {
            pChar.setOccupation(req.get("occupation")!=null?req.get("occupation").toString():null); // TODO: validate (/!\ Null ?)
        }
        // -- religion
        if (req.has("religion")) {
            pChar.setReligion(req.get("religion")!=null?req.get("religion").toString():null); // TODO: validate (/!\ Null ?)
        }
        // == create
        try {
            if (!charDB.putCharacter(pChar, user.hasPerm(Character.PERM_UNLIMITED_CHARS)))
                throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                        "Error when registering character")
                        .addData("info","Unable to create character");
        } catch (MaxCharCountReachedEx maxEx) {
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_TOO_MANY_CHARS,
                        "Maximum number of characters reached")
                        .addData("max", maxEx.getMax());
        }
        List<CharacterEdit> edits = new ArrayList<>();
        edits.add(CharacterEdit.Field.STATUS.getCharacterEdit(pChar.getID(), Instant.now(), user.getUID(), null, Character.Status.CREATED));
        charDB.putCharacterEdits(edits);
        return pChar.toJSON(true, false);
    }

    public JSONObject updateCharacter(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to update characters"
            ).addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        if (user.getMinecraftPseudo()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player has no minecraft account linked")
                    .addData("code",2);
        if (user.getMinecraftUUID()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Unable to find minecraft account")
                    .addData("code",3);

        JSONObject req = r.getJSONRequest();
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);

        Instant now = Instant.now();
        List<CharacterEdit> edits = new ArrayList<>();

        // Set status
        if (req.get("status")!=null) {
            Character.Status oldStt = pChar.getStatus();
            Character.Status stt = Character.Status.find(req.get("status").toString());
            if (stt == null) {
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_STATUS,
                        "Invalid status")
                        .addData("code", 1)
                        .addData("list", JSONUtils.jsonArray(Character.Status.values()));
            }
            if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT)) {
                pChar.setStatus(stt);
            } else if (oldStt == Character.Status.ACTIVATED && stt == Character.Status.ARCHIVED) {
                pChar.setStatus(stt);
            } else if ((oldStt == Character.Status.CREATED || oldStt == Character.Status.REJECTED) && stt == Character.Status.PENDING) {
                // Player submits character, check skills
                List<String> requiredCategories = charDB.findRequiredSkillCategories().stream()
                        .map(SkillCategory::getName).collect(Collectors.toList());
                List<CharacterSkill> skills = charDB.findCharacterSkills(pChar.getID());
                int points = 7;
                for (CharacterSkill s : skills) {
                    if (s.getLevel()<1) continue;
                    if (s.getLevel()>2)
                        throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL,
                                "Skill level is over the limit")
                                .addData("code",1)
                                .addData("category",s.getCategory())
                                .addData("skill",s.getName());
                    points -= s.getLevel()*s.getLevel();
                    if (points < 0 && !user.hasPerm(Character.PERM_UNLIMITED_SKILLS))
                        throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL,
                                "Too much points used")
                                .addData("code",2)
                                .addData("max",7);
                    requiredCategories.remove(s.getCategory());
                }
                if (requiredCategories.size()>0)
                    throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL,
                            "Missing mandatory skill categories")
                            .addData("code",3)
                            .addData("list", new JSONArray(requiredCategories));
                List<String> unlockedCategories = charDB.findCharacterSkillCategories(charID).stream()
                        .map(SkillCategory::getName).collect(Collectors.toList());
                if (unlockedCategories.size()>3 && !user.hasPerm(Character.PERM_UNLIMITED_SKILLS))
                    throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                            "Too many skill categories unlocked")
                            .addData("code",4);
                // Everything is OK, set status
                pChar.setStatus(stt);
            } else {
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_STATUS,
                        "You are not allowed to set this status")
                        .addData("code", 2);
            }
            edits.add(CharacterEdit.Field.STATUS.getCharacterEdit(charID, now, user.getUID(), oldStt, pChar.getStatus()));
        }

        // TODO: validate input (special chars and length)
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("firstName")!=null)
            pChar.setFirstName(req.get("firstName").toString()); // Staff only, TODO: validate
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("lastName")!=null)
            pChar.setLastName(req.get("lastName").toString()); // Staff only, TODO: validate
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("statusComment")!=null) {
            String newComment = req.get("statusComment").toString();
            edits.add(CharacterEdit.Field.STATUS_COMMENT.getCharacterEdit(charID, now, user.getUID(), pChar.getStatusComment(), newComment));
            pChar.setStatusComment(newComment); // Staff only, TODO: validate
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("sex")!=null) try {
            pChar.setSex(req.get("sex").toString().toUpperCase().charAt(0)); // Staff only
        } catch (IllegalArgumentException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SEX,
                    "Invalid character sex.")
                    .addData("code", 2)
                    .addData("info", "Invalid sex, can only by 'M' or 'F'.");
        }
        CharRace cRace = null;
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("race")!=null) { // Staff only
            cRace = charDB.findRaceFromName(req.get("race").toString());
            if (cRace==null || !cRace.userCanUse(user))
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_RACE,
                        "Invalid character race.")
                        .addData("code", 2)
                        .addData("info", "No such race or not allowed.");
            pChar.setRace(cRace.getName());
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("birth")!=null) {
            pChar.setBirth(req.get("birth").toString()); // TODO: validate (format ?)
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("category")!=null) { // Staff only
            pChar.setCategory(req.get("category").toString()); // TODO: validate
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("origins")!=null) { // Staff only
            pChar.setOrigins(req.get("origins").toString()); // TODO: validate (get origins list)
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("height")!=null) { // Staff only
            float cHeight;
            try {
                cHeight = Float.parseFloat(req.get("height").toString());
            } catch (NumberFormatException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_HEIGHT,
                        "Invalid character height.")
                        .addData("code", 1)
                        .addData("info", "Decimal number expected.");
            }
            if (cRace==null)
                cRace = charDB.findRaceFromName(pChar.getRace());
            if (cHeight>cRace.getMaxHeight() || cHeight<cRace.getMinHeight()) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_HEIGHT,
                        "Invalid character height.")
                        .addData("code", 2)
                        .addData("info", "Height out of bounds for this race.")
                        .addData("min", cRace.getMinHeight())
                        .addData("max", cRace.getMaxHeight());
            }
            pChar.setHeight(cHeight);
        }
        if (user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT) && req.get("luckPoints")!=null) {
            try {
                pChar.setLuckPoints(Integer.parseUnsignedInt(req.get("luckPoints").toString()));
            } catch (NumberFormatException e) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_LUCK_POINTS,
                        "Invalid character luck points.")
                        .addData("code", 1)
                        .addData("info", "Luck points is a number.");
            }
        }
        if (req.get("age")!=null) {
            int newAge;
            try {
                newAge = Integer.parseUnsignedInt(req.get("age").toString());
            } catch (NumberFormatException e) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_AGE,
                        "Invalid character age.")
                        .addData("code", 2)
                        .addData("info", "Age is a number.");
            }
            edits.add(CharacterEdit.Field.AGE.getCharacterEdit(charID, now, user.getUID(), pChar.getAge(), newAge));
            pChar.setAge(newAge);
        }
        if (req.has("occupation")) {
            String newOccup = req.get("occupation")!=null ? req.get("occupation").toString() : null;
            edits.add(CharacterEdit.Field.OCCUPATION.getCharacterEdit(charID, now, user.getUID(), pChar.getOccupation(), newOccup));
            pChar.setOccupation(newOccup); // TODO: validate (/!\ Null ?)
        }
        if (req.has("religion")) {
            String newRelig = req.get("religion")!=null ? req.get("religion").toString() : null;
            edits.add(CharacterEdit.Field.RELIGION.getCharacterEdit(charID, now, user.getUID(), pChar.getReligion(), newRelig));
            pChar.setReligion(newRelig); // TODO: validate (/!\ Null ?)
        }
        if (!charDB.updateCharacter(pChar))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error when updating character");

        charDB.putCharacterEdits(edits);
        return pChar.toJSON(true, false);
    }

    public JSONObject getCharacterDescriptions(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        try {
            charDB.fillCharacterDescriptions(pChar);
        } catch (CharacterNotFoundEx characterNotFoundEx) { // Should not be possible
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No such character.");
        }
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("physicalDesc",pChar.getPhysicalDescription());
        res.put("story",pChar.getStory());
        res.put("details",pChar.getDetails());
        return res;
    }

    public JSONObject updateCharacterDescriptions(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to update characters descriptions"
            ).addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        JSONObject req = r.getJSONRequest();
        if (user.getMinecraftPseudo()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player has no minecraft account linked")
                    .addData("code",2);
        if (user.getMinecraftUUID()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Unable to find minecraft account")
                    .addData("code",3);
        int charID = r.getArg("char-id").getAsInt();
        Character oldChar = charDB.findCharacterByID(charID);
        if (oldChar==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No such character.");
        if (!oldChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        try {
            charDB.fillCharacterDescriptions(oldChar);
        } catch (CharacterNotFoundEx characterNotFoundEx) { // Should not be possible
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                    "No such character.");
        }
        Character newChar = new Character(oldChar.getID(), oldChar.getPlayerUUID());
        // TODO: validate input (special chars and length)
        newChar.setPhysicalDescription(
                req.get("physicalDesc")!=null ?
                        req.get("physicalDesc").toString() :
                        oldChar.getPhysicalDescription()
        );
        newChar.setStory(
                req.get("story")!=null ?
                        req.get("story").toString() :
                        oldChar.getStory()
        );
        newChar.setDetails(
                req.get("details")!=null ?
                        req.get("details").toString() :
                        oldChar.getDetails()
        );
        if (!charDB.updateCharacterDescriptions(oldChar, newChar, user.getUID())) {
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error when updating character");
        }
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", oldChar.getFirstName());
        jChar.put("lastName", oldChar.getLastName());
        res.put("character", jChar);
        res.put("physicalDesc",newChar.getPhysicalDescription());
        res.put("story",newChar.getStory());
        res.put("details",newChar.getDetails());
        return res;
    }

    public JSONObject getCharacterSkins(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        List<CharacterSkin> list = charDB.findCharacterSkins(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        JSONObject skinList = new JSONObject();
        for (CharacterSkin s : list) {
            skinList.put(s.getName(), s.getFile());
        }
        res.put("list", skinList);
        return res;
    }

    public JSONObject addCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        JSONObject req = r.getJSONRequest();
        if (req.get("skin")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_EMPTY_REQUEST,
                    "Empty request body");
        BufferedImage img;
        try {
            byte[] imgData = Base64.getDecoder().decode(req.get("skin").toString());
            InputStream imgDataStream = new ByteArrayInputStream(imgData);
            img = ImageIO.read(imgDataStream);
            imgDataStream.close();
        } catch (IllegalArgumentException | IOException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_IMAGE,
                    "Invalid image")
                    .addData("code", 3);
        }
        String skinName = req.get("name")!=null ? req.get("name").toString() : "CharacterSkin "+ Utils.randomString(8);
        if (!skinName.matches("^[\\p{L}0-9 \\\\-_()\\[\\]']{1,64}$"))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_NAME,
                    "Invalid skin name");
        try {
            if (!charDB.putCharacterSkin(charID, null,
                    skinName, img, user.hasPerm(Character.PERM_UNLIMITED_SKINS))) {
                throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                        "Error while saving character skin");
            }
        } catch (MaxSkinCountReachedEx maxEx) {
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_TOO_MANY_SKINS,
                    "Maximum number of skins per character reached")
                    .addData("max", maxEx.getMax());
        }
        List<CharacterSkin> list = charDB.findCharacterSkins(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        JSONObject skinList = new JSONObject();
        for (CharacterSkin s : list) {
            skinList.put(s.getName(), s.getFile());
        }
        res.put("list", skinList);
        return res;
    }

    public JSONObject getCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters").addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        String name = r.getArg("skin-name").getAsString();
        CharacterSkin skin = charDB.findCharacterSkin(charID, name);
        if (skin==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_SKIN_NOT_FOUND,
                    "No such skin for this character.");
        throw new RedirectionEx("/skins/"+skin.getFile()).addData("skin", skin.toJSON());
    }

    public JSONObject updateCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters").addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        String name = r.getArg("skin-name").getAsString();
        CharacterSkin skin = charDB.findCharacterSkin(charID, name);
        if (skin==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_SKIN_NOT_FOUND,
                    "No such skin for this character.");
        List<UUID> selectedList = charDB.findPlayersWithCharacterSkin(charID, name);
        if (selectedList.size()>0) throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_SKIN_SELECTED,
                "Cannot rename selected skin.");
        JSONObject req = r.getJSONRequest();
        if (req.get("name")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_NAME,
                    "Missing new skin name")
                    .addData("code",1);
        String skinName = req.get("name").toString();
        if (!skinName.matches("^[\\p{L}0-9 \\\\-_()\\[\\]']{1,64}$"))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_NAME,
                    "Invalid skin name")
                    .addData("code",2)
                    .addData("info","Name contains invalid characters, is too short or too long");
        if (!charDB.updateCharacterSkinName(skin.getCharacterID(), name, skinName))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error while saving skin name");
        skin.setName(skinName);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("skin", skin.toJSON(true));
        return res;
    }

    public JSONObject deleteCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters").addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        String name = r.getArg("skin-name").getAsString();
        List<UUID> selectedList = charDB.findPlayersWithCharacterSkin(charID, name);
        if (selectedList.size()>0) throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_SKIN_SELECTED,
                "Cannot delete selected skin.");
        List<CharacterSkin> list = charDB.findCharacterSkins(charID);
        Iterator<CharacterSkin> listIt = list.iterator();
        CharacterSkin skin = null;
        while (listIt.hasNext()) {
            CharacterSkin s = listIt.next();
            if (s.getName().equals(name)) {
                listIt.remove();
                skin = s;
                break;
            }
        }
        if (skin==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_SKIN_NOT_FOUND,
                    "No such skin for this character.");
        if (!charDB.deleteCharacterSkin(skin))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error while removing character skin");
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        JSONObject skinList = new JSONObject();
        for (CharacterSkin s : list) {
            skinList.put(s.getName(), s.getFile());
        }
        res.put("list", skinList);
        return res;
    }

    public JSONObject setTempCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid = r.getArg("player").getAsString();
        TempCharacter pChar = charDB.findTempCharacter(uuid);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No temporary character found for this player");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        JSONObject req = r.getJSONRequest();
        if (req.get("skin")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_EMPTY_REQUEST,
                    "Empty request body");
        BufferedImage img;
        try {
            byte[] imgData = Base64.getDecoder().decode(req.get("skin").toString());
            InputStream imgDataStream = new ByteArrayInputStream(imgData);
            img = ImageIO.read(imgDataStream);
            imgDataStream.close();
        } catch (IllegalArgumentException | IOException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_IMAGE,
                    "Invalid image")
                    .addData("code", 3);
        }
        String skinName = req.get("name")!=null ? req.get("name").toString() : "Skin "+ Utils.randomString(8);
        charDB.deleteTempCharacterSkin(pChar);
        if (!charDB.putTempCharacterSkin(pChar.getPlayerUUIDObject(), skinName, img)) {
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error while saving character skin");
        }
        Player player = Bukkit.getPlayer(UUID.fromString(Utils.addUuidDashes(uuid)));
        if (player!=null && player.isOnline())
            charDB.getModule().sendUpdatePackets(player);
        return getTempCharacter(r);
    }

    public JSONObject deleteTempCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid = r.getArg("player").getAsString();
        TempCharacter pChar = charDB.findTempCharacter(uuid);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No temporary character found for this player");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (!charDB.deleteTempCharacterSkin(pChar)) {
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error while removing character skin");
        }
        Player player = Bukkit.getPlayer(UUID.fromString(Utils.addUuidDashes(uuid)));
        if (player!=null && player.isOnline())
            charDB.getModule().sendUpdatePackets(player);
        return getTempCharacter(r);
    }

    public JSONObject getTempCharacterSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid = r.getArg("player").getAsString();
        TempCharacter pChar = charDB.findTempCharacter(uuid);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No temporary character found for this player");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        if (pChar.getSkin()==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_SKIN_NOT_FOUND,
                    "No skin for this character.");
        throw new RedirectionEx("/skins/"+pChar.getSkin());
    }

    public JSONObject getCharacterStaffNotes(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (!user.hasPerm(Character.PERM_STAFF_FIELDS_VIEW))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this information")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        List<StaffNote> list = charDB.findStaffNotes(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("staffNotes", JSONUtils.jsonArray(list));
        return res;
    }

    public JSONObject addCharacterStaffNote(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to edit staff notes")
                    .addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        if (!user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this information")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        JSONObject req = r.getJSONRequest();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (req.get("note")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_EMPTY_REQUEST,
                    "Empty request.");
        String note = req.get("note").toString();
        if (!charDB.putStaffNote(charID, user.getUID(), note))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to save note.");
        List<StaffNote> list = charDB.findStaffNotes(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("staffNotes", JSONUtils.jsonArray(list));
        return res;
    }

    public JSONObject updateCharacterStaffNote(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to edit staff notes")
                    .addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        if (!user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this information")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        JSONObject req = r.getJSONRequest();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (req.get("note")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_EMPTY_REQUEST,
                    "Empty request.");
        String note = req.get("note").toString();
        Instant noteTime = Instant.ofEpochMilli(r.getArg("time").getAsLong());
        if (charDB.findStaffNote(charID, noteTime)==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_STAFF_NOTE_NOT_FOUND,
                    "No such note.");
        if (!charDB.updateStaffNote(charID, noteTime, user.getUID(), note))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to edit note.");
        List<StaffNote> list = charDB.findStaffNotes(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("staffNotes", JSONUtils.jsonArray(list));
        return res;
    }

    public JSONObject deleteCharacterStaffNote(APIRequest r) throws APIException {
        if (!(r.getUser() instanceof APIForumUser))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_USER_TYPE,
                    "Static users aren't allowed to delete staff notes")
                    .addData("code", 1);
        APIForumUser user = (APIForumUser) r.getUser();
        if (!user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this information")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        Instant noteTime = Instant.ofEpochMilli(r.getArg("time").getAsLong());
        if (charDB.findStaffNote(charID, noteTime)==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_STAFF_NOTE_NOT_FOUND,
                    "No such note.");
        if (!charDB.deleteStaffNote(charID, noteTime))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to delete note.");
        List<StaffNote> list = charDB.findStaffNotes(charID);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        res.put("staffNotes", JSONUtils.jsonArray(list));
        return res;
    }

    public JSONObject getCharacterFieldEdit(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        if (!user.hasPerm(Character.PERM_VIEW_HISTORY))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to see history");
        CharacterEdit.Field field;
        try {
            field = CharacterEdit.Field.valueOf(r.getArg("field").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SUCH_FIELD,
                    "No field with this name");
        }
        Instant editTime = Instant.ofEpochMilli(r.getArg("time").getAsLong());
        CharacterEdit edit = charDB.findCharacterFieldEdit(charID, field, editTime);
        if (edit==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_EDIT_FOUND,
                    "No edit found for this field at this time");
        JSONObject res = edit.toJSON();
        res.put("character", pChar.toJSON(false, false));
        return res;
    }

    public JSONObject updateCharacterFieldEdit(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        JSONObject req = r.getJSONRequest();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        if (!user.hasPerm(Character.PERM_EDIT_HISTORY))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to edit history");
        CharacterEdit.Field field;
        try {
            field = CharacterEdit.Field.valueOf(r.getArg("field").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SUCH_FIELD,
                    "No field with this name");
        }
        Instant editTime = Instant.ofEpochMilli(r.getArg("time").getAsLong());
        CharacterEdit edit = charDB.findCharacterFieldEdit(charID, field, editTime);
        if (edit==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_EDIT_FOUND,
                    "No edit found for this field at this time");
        if (req.get("status") != null) {
            edit.setStatus(req.get("status").toString()); // TODO: validate
        }
        if (req.get("comment") != null) {
            edit.setComment(req.get("comment").toString()); // TODO: validate
        }
        if (!charDB.updateCharacterEdit(edit))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                "Error when updating edit status");
        JSONObject res = edit.toJSON();
        res.put("character", pChar.toJSON(false, false));
        return res;
    }

    public JSONObject getLastCharacterFieldEdits(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        if (!user.hasPerm(Character.PERM_VIEW_HISTORY))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to see history");
        CharacterEdit.Field field;
        try {
            field = CharacterEdit.Field.valueOf(r.getArg("field").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SUCH_FIELD,
                    "No field with this name");
        }
        // TODO: add arguments to choose start time (from) and limit
        List<CharacterEdit> edits = charDB.findLastCharacterFieldEdits(charID, field, 200, null);
        JSONObject res = new JSONObject();
        res.put("character", pChar.toJSON(false, false));
        res.put("edits", JSONUtils.jsonArray(edits));
        return res;
    }

    public JSONObject getLastCharacterEdits(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        if (!user.hasPerm(Character.PERM_VIEW_HISTORY))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to see history");
        // TODO: add arguments to choose start time (from) and limit
        List<CharacterEdit> edits = charDB.findLastCharacterEdits(charID, 200, null);
        JSONObject res = new JSONObject();
        res.put("character", pChar.toJSON(false, false));
        res.put("edits", JSONUtils.jsonArray(edits));
        return res;
    }

    public JSONObject getLastEdits(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        if (!user.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to see edits of other characters");
        if (!user.hasPerm(Character.PERM_VIEW_HISTORY))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_PERM_MISSING,
                    "Not allowed to see history");
        // TODO: add arguments to choose start time (from) and limit
        String status = null;
        if (r.hasArg("status"))
            status = r.getArg("status").getAsString();
        List<CharacterEdit> edits = charDB.findLastEdits(status, 200, null);
        JSONObject res = new JSONObject();
        JSONArray arr = new JSONArray();
        for (CharacterEdit edit : edits) {
            JSONObject e = edit.toJSON();
            Character sChar = charDB.findCharacterByID(edit.getCharID());
            if (sChar==null) continue; // Should not be possible
            e.put("character", sChar.toJSON(false,false));
            arr.put(e);
        }
        res.put("edits", arr);
        return res;
    }

    public JSONObject getCharacterSkills(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanView(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't access this character")
                    .addData("code", 1);
        JSONObject res = new JSONObject();
        JSONObject jChar = new JSONObject();
        jChar.put("id", charID);
        jChar.put("firstName", pChar.getFirstName());
        jChar.put("lastName", pChar.getLastName());
        res.put("character", jChar);
        List<SkillCategory> unlocked = charDB.findCharacterSkillCategories(charID);
        unlocked.addAll(charDB.findRequiredSkillCategories());
        JSONObject skills = new JSONObject();
        for (SkillCategory cat : unlocked) {
            skills.put(cat.getName(), new JSONObject());
        }
        List<CharacterSkill> list = charDB.findCharacterSkills(charID);
        for (CharacterSkill s: list) {
            //if (s.getLevel()==0) continue;
            JSONObject cat = (JSONObject) skills.get(s.getCategory());
            cat.put(s.getName(), s.toJSON());
        }
        res.put("skills", skills);
        return res;
    }

    public JSONObject updateCharacterSkill(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (pChar.getStatus() != Character.Status.CREATED && pChar.getStatus() != Character.Status.REJECTED &&
                !user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_STATUS,
                    "You are not allowed to edit characters in this state")
                    .addData("code", 1);
        Skill skill = charDB.findSkill(r.getArg("category").getAsString(), r.getArg("skill").getAsString());
        if (skill == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL,
                    "No such skill")
                    .addData("code",2);
        SkillCategory charSkillCat = charDB.findCharacterSkillCategory(charID, skill.getCategory());
        if (charSkillCat == null) {
            List<SkillCategory> requiredSkillCats = charDB.findRequiredSkillCategories();
            for (SkillCategory sCat : requiredSkillCats) {
                if (!sCat.getName().equals(skill.getCategory())) continue;
                charSkillCat = sCat;
                break;
            }
            if (charSkillCat == null)
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                        "This category isn't unlocked");
            else if (!charDB.unlockCharacterSkillCategory(charID, charSkillCat.getName()))
                throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                        "Unable to unlock character skill category");
        }
        JSONObject req = r.getJSONRequest();
        if (req.get("level") == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_LEVEL,
                    "Missing skill level")
                    .addData("code",3);
        Integer sLevel;
        try {
            sLevel = Integer.parseInt(req.get("level").toString());
            if (sLevel<0 || sLevel>5) throw new NumberFormatException("Invalid skill level");
        } catch (NumberFormatException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_LEVEL,
                    "Invalid skill level")
                    .addData("code",4)
                    .addData("min",0)
                    .addData("max",5);
        }
        if (req.get("comment") == null) // TODO: validate
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_COMMENT,
                    "Missing skill comment")
                    .addData("code",5);
        CharacterSkill cSkill = new CharacterSkill(pChar.getID(), skill.getCategory(), skill.getName())
                .setLevel(sLevel)
                .setComment(req.get("comment").toString())
                .setCategoryUnlocked(true);
        if (!charDB.updateCharacterSkill(cSkill))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to update character skill");
        return getCharacterSkills(r);
    }

    public JSONObject unlockCharacterSkillCategory(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (pChar.getStatus() != Character.Status.CREATED && pChar.getStatus() != Character.Status.REJECTED &&
                !user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_STATUS,
                    "You are not allowed to edit characters in this state")
                    .addData("code", 1);
        SkillCategory skillCat = charDB.findSkillCategory(r.getArg("category").getAsString());
        if (skillCat == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                    "No such skill category")
                    .addData("code",1);
        List<SkillCategory> charSkillCats = charDB.findCharacterSkillCategories(charID);
        if (charSkillCats.size()>3 && !user.hasPerm(Character.PERM_UNLIMITED_SKILLS))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                    "Too many categories unlocked")
                    .addData("max", 3)
                    .addData("code",2);
        for (SkillCategory sCat : charSkillCats) {
            if (!sCat.getName().equals(skillCat.getName())) continue;
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                    "This category is already unlocked")
                    .addData("code",3);
        }
        if (!charDB.unlockCharacterSkillCategory(charID, skillCat.getName()))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to unlock character skill category");
        return getCharacterSkills(r);
    }

    public JSONObject deleteCharacterSkillCategory(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        int charID = r.getArg("char-id").getAsInt();
        Character pChar = charDB.findCharacterByID(charID);
        if (pChar==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHARACTER_NOT_FOUND,
                "No such character.");
        if (!pChar.userCanUse(user))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                    "Player can't edit this character")
                    .addData("code", 1);
        if (pChar.getStatus() != Character.Status.CREATED && pChar.getStatus() != Character.Status.REJECTED &&
                !user.hasPerm(Character.PERM_STAFF_FIELDS_EDIT))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_STATUS,
                    "You are not allowed to edit characters in this state")
                    .addData("code", 1);
        SkillCategory skillCat = charDB.findSkillCategory(r.getArg("category").getAsString());
        if (skillCat == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                    "No such skill category")
                    .addData("code",1);
        SkillCategory charSkillCat = charDB.findCharacterSkillCategory(charID, skillCat.getName());
        if (charSkillCat == null) throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_CHAR_SKILL_CATEGORY,
                "This category isn't unlocked")
                .addData("code",2);
        if (!charDB.deleteCharacterSkillsFromCategory(charID, skillCat.getName()))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to delete character skill category")
                    .addData("code",1);
        if (!charDB.removeCharacterSkillCategory(charID, skillCat.getName()))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Unable to delete character skill category")
                    .addData("code",2);
        return getCharacterSkills(r);
    }

    public JSONObject getPlayerCharacters(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid = r.getArg("player").getAsString();
        List<Character> list = charDB.findPlayerCharacters(uuid);
        if (user instanceof APIForumUser) {
            APIForumUser userF = (APIForumUser) user;
            if (!user.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS) && !uuid.equals(userF.getMinecraftUUID().toString().replaceAll("-","")))
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                        "Not allowed to access this player")
                        .addData("code", 1);
        }
        JSONObject res = new JSONObject();
        res.put("uuid",uuid);
        JSONArray jList = new JSONArray();
        for (Character c : list) {
            jList.put(c.toJSON(false, true));
        }
        res.put("list", jList);
        CharacterPlayer player = charDB.findPlayer(uuid);
        res.put("selected", player!=null ? player.getSelectedCharacter() : JSONObject.NULL);
        TempCharacter tChar =  charDB.findTempCharacter(uuid);
        if (tChar!=null && tChar.userCanView(user))
            res.put("tempChar", tChar.toJSON(false, true));
        return res;
    }

    public JSONObject getCharacterPlayer(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String uuid = r.getArg("player").getAsString();
        CharacterPlayer player = charDB.findPlayer(uuid);
        if (user instanceof APIForumUser) {
            APIForumUser userF = (APIForumUser) user;
            if (!user.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS) && !uuid.equals(userF.getMinecraftUUID().toString().replaceAll("-","")))
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_INVALID_PLAYER,
                        "Not allowed to access this player")
                        .addData("code", 1);
        }
        if (player==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_PLAYER_NOT_FOUND,
                    "Player not found");
        JSONObject res =  player.toJSON();
        AbstractCharacter selectedChar;
        if (player.getSelectedCharacter() == 0) {
            selectedChar = charDB.findTempCharacter(uuid);
        } else {
            selectedChar = charDB.findCharacterByID(player.getSelectedCharacter());
            if (player.getSkinName()!=null) {
                CharacterSkin pSkin = charDB.findCharacterSkin(((Character) selectedChar).getID(), player.getSkinName());
                res.put("skin", pSkin.toJSON(true));
            }
        }
        if (selectedChar!=null) {
            res.put("selectedCharacter", selectedChar.toJSON(false, true));
            if (player.getName() == null)
                res.put("name", selectedChar.getName());
        }
        return res;
    }

    public JSONObject getCharacterCategories(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        Set<String> list = new HashSet<>(charDB.findCharacterCategories());
        list.addAll(Arrays.asList(DEFAULT_CATEGORIES));
        JSONArray cats = new JSONArray();
        for (String c : list) {
            if (!user.hasPerm(Character.PERM_VIEW_CHAR_CATEGORY.replace("{{CATEGORY}}",c))) continue;
            cats.put(c);
        }
        JSONObject res = new JSONObject();
        res.put("categories", cats);
        res.put("count", cats.length());
        return res;
    }

    public JSONObject getSharedCharacters(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String category = r.getArg("category").getAsString();
        if (!(
                user.hasPerm(Character.PERM_VIEW_CHAR_CATEGORY.replace("{{CATEGORY}}",category)) ||
                user.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS)
        )) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_CATEGORY,
                "Not allowed to access characters of this category")
                    .addData("code", 1);
        List<Character> list = charDB.findSharedCharacters(category);
        if (list==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CATEGORY_NOT_FOUND,
                "No such category.");
        JSONObject res = new JSONObject();
        res.put("category",category);
        JSONArray jList = new JSONArray();
        for (Character c : list) {
            jList.put(c.toJSON(false, true));
        }
        res.put("list", jList);
        return res;
    }

    public JSONObject getOrigins(APIRequest r) throws APIException {
        List<CharOrigins> list = charDB.findOrigins();
        if (list==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, APIError.SUBERR_INTERNAL_ERROR,
                "Couldn't find origins.");
        JSONObject res = new JSONObject();
        JSONArray jList = JSONUtils.jsonArray(list);
        res.put("list", jList);
        res.put("count", list.size());
        return res;
    }

    public JSONObject getRaces(APIRequest r) throws APIException {
        Map<String,List<CharRace>> list = charDB.findRaces();
        if (list==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, APIError.SUBERR_INTERNAL_ERROR,
                "Couldn't find races.");
        JSONObject res = new JSONObject();
        JSONObject jList = new JSONObject();
        int count = 0;
        for (String cat : list.keySet()) {
            if (!CharRace.userCanUse(r.getUser(), cat)) continue;
            JSONObject races = new JSONObject();
            for (CharRace race : list.get(cat)) {
                count++;
                races.put(race.getName(), race.toJSON());
            }
            jList.put(cat, races);
        }
        res.put("list", jList);
        res.put("categoryCount", jList.length());
        res.put("raceCount", count);
        return res;
    }

    public JSONObject getSkills(APIRequest r) throws APIException {
        List<SkillCategory> catList = charDB.findSkillCategories();
        Map<String, List<Skill>> skillList = charDB.findSkills();
        if (catList==null || skillList==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, APIError.SUBERR_INTERNAL_ERROR,
                    "Couldn't find skills.");
        JSONObject res = new JSONObject();
        JSONObject jCatList = new JSONObject();
        for (SkillCategory cat : catList) {
            JSONObject jCat = cat.toJSON();
            List<Skill> list = skillList.get(cat.getName());
            if (list==null) {
                jCat.append("skills", null);
                AQLCharacters.LOGGER.mWarning("Empty skill category : "+cat.getName());
            } else {
                JSONObject skills = new JSONObject();
                for (Skill skill : list) {
                    skills.put(skill.getName(), skill.toJSON());
                }
                jCat.put("skills", skills);
            }
            jCatList.put(cat.getName(), jCat);
        }
        res.put("categories", jCatList);
        res.put("count", jCatList.length());
        return res;
    }

    public JSONObject getContext(APIRequest r) throws APIException {
        JSONObject res = new JSONObject();
        res.put("origins", getOrigins(r).get("list"));
        res.put("races", getRaces(r).get("list"));
        res.put("skills", getSkills(r).get("categories"));
        res.put("categories", getCharacterCategories(r).get("categories"));
        int maxChars = 0;
        if (r.getUser().hasPerm(Character.PERM_CREATE_SELF_CHARACTER))
            maxChars = AquilonThings.instance.getConfig().getInt("AQLCharacters.chars.max", 3);
        res.put("maxChars", r.getUser().hasPerm(Character.PERM_UNLIMITED_CHARS) ? null : maxChars);
        int maxSkins = AquilonThings.instance.getConfig().getInt("AQLCharacters.skins.max", 5);
        res.put("maxSkins", r.getUser().hasPerm(Character.PERM_UNLIMITED_SKINS) ? null : maxSkins);
        return res;
    }

    public JSONObject getCommonSkinCategories(APIRequest r) {
        APIUser user = r.getUser();
        Map<String, Integer> map = charDB.findCommonSkinCategories();
        JSONObject res = new JSONObject();
        JSONObject list = new JSONObject();
        int total = 0;
        for (String cat : map.keySet()) {
            if (!user.hasPerm(Character.PERM_VIEW_SKIN_CATEGORY.replace("{{CATEGORY}}",cat))) continue;
            int count = map.get(cat);
            list.put(cat, count);
            total += count;
        }
        res.put("categories", list);
        res.put("total", total);
        return res;
    }

    public JSONObject getCommonSkinsCategory(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        String category = r.getArg("category").getAsString();
        if (!user.hasPerm(Character.PERM_VIEW_SKIN_CATEGORY.replace("{{CATEGORY}}",category)))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_CATEGORY,
                    "You are not allowed to see this category");
        List<CommonSkin> list = charDB.findCommonSkinsCategory(category);
        JSONObject res = new JSONObject();
        res.put("list", JSONUtils.jsonArray(list));
        res.put("count", list.size());
        return res;
    }

    public JSONObject getCommonSkin(APIRequest r) throws APIException {
        int skinID = r.getArg("skin-id").getAsInt();
        CommonSkin skin = charDB.findCommonSkin(skinID);
        if (skin==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_SKIN_NOT_FOUND,
                    "No such skin.");
        if (!r.getUser().hasPerm(Character.PERM_VIEW_SKIN_CATEGORY.replace("{{CATEGORY}}",skin.getCategory())))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_CATEGORY,
                    "You are not allowed to see this category");
        throw new RedirectionEx("/skins/"+skin.getFile()).addData("skin", skin.toJSON());
    }

    public JSONObject createCommonSkin(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        String category = r.getArg("category").getAsString();
        if (!user.hasPerm(Character.PERM_USE_SKIN_CATEGORY.replace("{{CATEGORY}}",category)))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDEN_CATEGORY,
                    "You are not allowed to see this category");
        if (!category.matches("^[^\";\\n]{3,64}$")) // TODO: Improve category name filter
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_COMMON_SKIN_CAT,
                    "Invalid category name");
        JSONObject req = r.getJSONRequest();
        if (req.get("skin")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_EMPTY_REQUEST,
                    "Empty request body");
        BufferedImage img;
        try {
            byte[] imgData = Base64.getDecoder().decode(req.get("skin").toString());
            InputStream imgDataStream = new ByteArrayInputStream(imgData);
            img = ImageIO.read(imgDataStream);
            imgDataStream.close();
        } catch (IllegalArgumentException | IOException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_IMAGE,
                    "Invalid image")
                    .addData("code", 3);
        }
        String skinName = req.get("name")!=null ? req.get("name").toString() : "CommonSkin "+ Utils.randomString(8);
        if (!skinName.matches("^[^\";\\n]{3,64}$")) // TODO: Improve skin name filter
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_SKIN_NAME,
                    "Invalid skin name");
        if (!charDB.putCommonSkin(skinName, category, img))
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Error while saving common skin");
        List<CommonSkin> list = charDB.findCommonSkinsCategory(category);
        JSONObject res = new JSONObject();
        res.put("list", JSONUtils.jsonArray(list));
        res.put("count", list.size());
        return res;
    }

    public JSONObject searchCharactersByStatus(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER,
                    "Default user isn't allowed to access characters")
                    .addData("code", 1);
        String status = r.getArg("status").getAsString();
        List<Character> list = charDB.findCharactersWithStatus(status);
        if (list==null) throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                "Couldn't fetch characters");
        JSONObject res = new JSONObject();
        JSONArray jList = new JSONArray();
        for (Character c : list) {
            if (!c.userCanView(user)) continue;
            jList.put(c.toJSON(false, false));
        }
        res.put("results", jList);
        res.put("count", jList.length());
        return res;
    }

    public JSONObject searchChars(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        JSONObject req = r.getJSONRequest();
        if (req.get("query")==null || req.get("query").toString().length()<2)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_EMPTY_REQUEST,
                    "Missing query or too short !");
        String query = req.get("query").toString().toLowerCase().replaceAll("%","[%]"); // TODO: validate
        CharacterSearchResults results = new CharacterSearchResults();

        if (user.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS)) {
            // Query is a player name
            List<CharacterPlayer> queryPlayers = charDB.findPlayersFromName(query);
            for (CharacterPlayer qP : queryPlayers) {
                int baseScore = qP.getUsername().equals(query) ? 500 :
                        qP.getUsername().equalsIgnoreCase(query) ? 250 :
                            qP.getUsername().toLowerCase().startsWith(query.toLowerCase()) ? 100 : 0;
                if (qP.getSelectedCharacter()!=0) {
                    results.addResult(baseScore+750, charDB.findCharacterByID(qP.getSelectedCharacter()));
                }
                List<Character> queryPlayerChars = charDB.findPlayerCharacters(qP.getUUID());
                results.addAllResults(baseScore+250, queryPlayerChars);
            }
        }

        // Query is the name of one of users chars
        if (user instanceof APIForumUser) {
            String uuid = ((APIForumUser) user).getMinecraftUUID().toString().replaceAll("-","");
            List<Character> pChars = charDB.findPlayerCharacters(uuid);
            for (Character c : pChars) {
                if (c.getName().toLowerCase().equals(query))
                    results.addResult(1500, c);
                if (c.getFirstName().toLowerCase().equals(query))
                    results.addResult(1000, c);
                if (c.getLastName()!=null && c.getLastName().toLowerCase().equals(query))
                    results.addResult(1000, c);
                else if (c.getName().toLowerCase().contains(query))
                    results.addResult(500, c);
            }
        }

        // Query is the name of a char
        List<Character> nameChars = charDB.findCharactersFromName(query);
        for (Character c : nameChars) {
            if (!c.userCanView(user)) continue;
            if (c.getName().toLowerCase().equals(query))
                results.addResult(500, c);
            if (c.getFirstName().toLowerCase().equals(query))
                results.addResult(200, c);
            if (c.getLastName()!=null && c.getLastName().toLowerCase().equals(query))
                results.addResult(200, c);
            else if (c.getName().toLowerCase().contains(query))
                results.addResult(100, c);
        }

        List<Character> uuidChars = charDB.findCharactersFromPartialUUID(query);
        for (Character c : uuidChars) {
            if (!c.userCanView(user)) continue;
            results.addResult(200, c);
        }

        JSONObject res = new JSONObject();
        res.put("results", results.getResultsArray());
        res.put("count", results.size());
        return res;
    }
}