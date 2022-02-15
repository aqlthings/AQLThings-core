package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions.APIPermissions;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.ForumGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Billi on 28/03/2018.
 *
 * @author Billi
 */
public class APIForumUser extends APIUser {
    private int forumUID;
    private UUID mcUUID;
    private ForumGroup[] groups;
    private int mainGroupID;
    private final APIPermissions perms;

    private APIForumUser(int forumUID, String userName) {
        super(userName);
        this.forumUID = forumUID;
        this.mcUUID = null;
        perms = APIPermissions.create();
    }

    @Override
    public UUID getPlayerUUID() {
        return mcUUID;
    }

    public void setPlayerUUID(UUID mcUUID) {
        this.mcUUID = mcUUID;
    }

    public int getUID() {
        return forumUID;
    }

    public void setUID(int uid) {
        this.forumUID = uid;
    }

    public ForumGroup[] getGroups() {
        return groups;
    }

    public ForumGroup getGroup(int id) {
        for (ForumGroup g : groups) {
            if (g.getID()==id) return g;
        }
        return null;
    }

    public List<Integer> getGroupIDs() {
        List<Integer> res = new ArrayList<>();
        for (ForumGroup g : groups) {
            res.add(g.getID());
        }
        return res;
    }

    public void setGroups(ForumGroup[] forumGroups) {
        this.groups = forumGroups;
    }

    public ForumGroup getMainGroup() {
        return getGroup(getMainGroupID());
    }

    public int getMainGroupID() {
        return mainGroupID;
    }

    public void setMainGroup(int mainGroup) {
        this.mainGroupID = mainGroup;
    }

    @Override
    public String getUniqueID() {
        return Integer.toString(getUID());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res =  super.toJSON();
        res.append("uid", getUID());
        res.append("group", getMainGroup().toJSON());
        JSONObject mc = null;
        UUID uuid = getPlayerUUID();
        if (uuid != null) {
            mc = new JSONObject();
            //mc.put("username", getMinecraftPseudo());
            mc.put("uuid", uuid.toString().replaceAll("-",""));
        }
        res.put("minecraft", mc != null ? mc : JSONObject.NULL);
        return res;
    }

    @Override
    public String toString() {
        return super.toString()+"["+getName()+"]";
    }

    @Override
    public void check(APIRequest req) throws APIException {
        super.check(req);
    }

    @Override
    public boolean isDefault() {
        // Forum users are never the default user
        return false;
    }

    @Override
    public APIPermissions getPermissions() {
        return perms;
    }

    // --- Static ---

    public static APIUser fromSession(int forumUID, String forumSession, String customGroups) {
        // TODO: cache users data
        JSONObject auth;
        String scriptURL =  AQLVox.instance.getConfig("auth.forum.authScript");
        try {
            URL url = new URL(scriptURL+"?uid="+forumUID+(forumSession!=null?"&sid="+forumSession:""));
            auth = Utils.httpGetJson(url);
        } catch (IOException e) {
            AQLVox.LOGGER.log(Level.WARNING, null, "APIForumUser.fromSession IOException: ",e);
            return null;
        } catch (JSONException e) {
            AQLVox.LOGGER.log(Level.WARNING, null, "APIForumUser.fromSession JSONException: ",e);
            return null;
        }
        if (auth==null || !auth.get("status").toString().equals("success")) {
            AquilonThings.LOGGER.warning(AquilonThings.LOG_PREFIX+"[AQLVox][Auth] APIForumUser.fromSession " +
                    "unable to get user data: "+(auth!=null?auth.toString():null));
            return null;
        }
        //AquilonThings.LOGGER.info(AquilonThings.LOG_PREFIX+"[AQLVox][Auth] DEBUG: "+auth.toJSONString());
        JSONObject uData = (JSONObject) auth.get("data");
        APIForumUser res = new APIForumUser(forumUID, uData.get("username").toString());
        Object mcUUID = uData.get("minecraftUUID");
        if (mcUUID != null) {
            String uuid = Utils.addUuidDashes(mcUUID.toString());
            if (uuid != null) res.setPlayerUUID(UUID.fromString(uuid));
        }
        // Load groups
        List<ForumGroup> gList = new ArrayList<>();
        JSONArray jsonGroups = (JSONArray) uData.get("groups");
        for (Object o: jsonGroups) {
            gList.add(ForumGroup.fromJSON(((JSONObject) o)));
        }
        res.setGroups(gList.toArray(new ForumGroup[0]));
        res.setMainGroup(Integer.parseInt(((JSONObject) uData.get("mainGroup")).get("id").toString()));
        // Load permissions
        loadUserPermissions(res);

        // If request sets custom groups (and is allowed to)
        if (customGroups != null && res.hasPerm("_setgroups")) {
            String[] custGroups = customGroups.split(";");
            gList.clear();
            for (String s : custGroups) {
                int gID;
                try {
                    gID = Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (gList.size()==0) res.setMainGroup(gID);
                gList.add(new ForumGroup(gID));
            }
            if (gList.size()>0) {
                res.setGroups(gList.toArray(new ForumGroup[0]));
                loadUserPermissions(res);
            }
        }
        return res;
    }

    public static APIUser fromUID(int forumUID, String customGroups) {
        return fromSession(forumUID, null, customGroups);
    }

    private static void loadUserPermissions(APIForumUser user) {
        APIPermissions perms = user.getPermissions();
        perms.clearAllPerms();
        List<String> defaultPerms = AQLVox.instance.getConfigArray("auth.forum.permissions.default");
        perms.addAllPerms(defaultPerms);
        for (Integer g: user.getGroupIDs()) {
            List<String> groupPerms = AQLVox.instance.getConfigArray("auth.forum.permissions."+g);
            perms.addAllPerms(groupPerms);
        }
    }
}
