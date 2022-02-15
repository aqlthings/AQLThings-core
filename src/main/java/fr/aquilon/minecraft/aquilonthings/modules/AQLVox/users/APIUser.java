package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions.APIPermissions;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public abstract class APIUser implements JSONExportable {
    private final String name;
    private Source source;

    public APIUser(String name) {
        this.name = name;
        this.source = null;
    }

    public Source getSource() {
        return source;
    }

    public boolean isFromToken() {
        return source == Source.TOKEN;
    }

    public boolean isFromApikey() {
        return source == Source.API_KEY;
    }

    /**
     * @return Whether the user allowed creating, listing and deleting API keys
     */
    public boolean canManageApikeys() {
        return !isDefault() && source.canManageApikeys();
    }

    /**
     * @return Whether the user allowed API tokens
     */
    public boolean canCreateToken() {
        return !isDefault() && source.canCreateToken();
    }

    public APIUser setSource(Source src) {
        this.source = src;
        return this;
    }

    public String toString() {
        return this.getClass().getSimpleName()+':'+getUniqueID();
    }

    public String getName() {
        return name;
    }

    public abstract APIPermissions getPermissions();

    public boolean hasPerm(String perm) {
        return getPermissions().hasPerm(perm);
    }

    public void check(APIRequest req) throws APIException {
        if (!getPermissions().hasPerm(req.getPermName()))
            throw new MissingPermissionEx(this, req.getPermName()); // Auth ok: Missing perm
    }

    public abstract boolean isDefault();

    public abstract String getUniqueID();

    public UUID getPlayerUUID() {
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name", getName());
        if (isDefault()) res.put("default", true);
        res.put("type", this.getClass().getSimpleName().substring(3));
        res.put("source", getSource()!=null ? getSource().name() : JSONObject.NULL);
        return res;
    }

    // --- Static ---

    public enum Source {
        USER_PASSWORD,
        FORUM_COOKIES, // APIForumUser only
        SIGNING_KEY, // APIStaticUser only
        API_KEY,
        TOKEN;

        public boolean canManageApikeys() {
            return ordinal() < Source.API_KEY.ordinal();
        }

        public boolean canCreateToken() {
            return ordinal() < Source.TOKEN.ordinal();
        }
    }

}
