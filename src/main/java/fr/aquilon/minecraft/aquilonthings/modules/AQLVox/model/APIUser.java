package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public abstract class APIUser implements JSONExportable {
    private String name;
    private List<String> perms;
    private List<String> removedPerms;
    private Source source;

    public APIUser(String name) {
        this.name = name;
        this.perms = new ArrayList<>();
        this.removedPerms = new ArrayList<>();
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
     * Is the user allowed to create, list and delete Api keys
     */
    public boolean canManageApikeys() {
        return !isDefault() && source.canManageApikeys();
    }

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

    public APIUser addAllPerms(Collection<String> perms) {
        for (String p : perms)
            addPerm(p);
        return this;
    }

    public APIUser clearAllPerms() {
        this.perms.clear();
        this.removedPerms.clear();
        return this;
    }

    public APIUser addPerm(String perm) {
        if (perm.startsWith("-")) {
            if (this.perms.contains(perm.substring(1))) return this;
            this.removedPerms.add(perm.substring(1));
        } else {
            if (this.perms.contains(perm)) return this;
            this.perms.add(perm);
            removedPerms.remove(perm);
        }
        return this;
    }

    public APIUser removePerm(String perm) {
        this.perms.remove(perm);
        this.removedPerms.remove(perm);
        return this;
    }

    public boolean hasPerm(String perm) throws InvalidPermissionException {
        String[] hierarchy = getPermHierarchy(perm);
        for (String pHierarchy: hierarchy) {
            for (String p: perms) {
                if (p.equals("all")) return true;
                if (pHierarchy.matches(getPermRegex(p))) {
                    return !hasPermRemoved(perm);
                }
            }
        }
        return false;
    }

    public boolean hasPermRemoved(String perm) throws InvalidPermissionException {
        String[] hierarchy = getPermHierarchy(perm);
        for (String pHierarchy: hierarchy) {
            for (String p: removedPerms) {
                if (p.equals("all")) return true;
                if (pHierarchy.matches(getPermRegex(p))) return true;
            }
        }
        return false;
    }

    public static String getPermRegex(String perm) throws InvalidPermissionException {
        if (perm.equals("*")) return "\\w+(\\.\\w+)*";
        List<String> permParts = Arrays.asList(perm.split("\\."));
        Iterator<String> parts = permParts.iterator();
        List<String> regex = new ArrayList<>(permParts.size());
        while (parts.hasNext()) {
            String part = parts.next();
            if (part.equals("#")) regex.add("\\w+");
            else if (part.equals("*")) regex.add("(\\w+(\\.\\w+)*)?");
            else if (part.matches("\\w+")) regex.add(part);
            else throw new InvalidPermissionException(perm);
        }
        return String.join("\\.", regex);
    }

    public List<String> getPermList() {
        return new ArrayList<>(perms);
    }

    public void check(APIRequest req) throws APIException {
        if (!hasPerm(req.getPermName()))
            throw new MissingPermissionEx(this, req.getPermName()); // Auth ok: Missing perm
    }

    public abstract boolean isDefault();

    public abstract String getUniqueID();

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

    public static String[] getPermHierarchy(String perm) {
        List<String> res = new ArrayList<>();
        res.add(perm);
        String curPerm = perm;
        while (curPerm.lastIndexOf('.')!=-1) {
            curPerm = curPerm.substring(0,curPerm.lastIndexOf('.'));
            if (!curPerm.isEmpty()) res.add(curPerm);
        }
        Collections.reverse(res);
        return res.toArray(new String[res.size()]);
    }

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

    public static class InvalidPermissionException extends RuntimeException {
        private String perm;

        public InvalidPermissionException(String perm) {
            this(perm, null);
        }

        public InvalidPermissionException(String perm, Throwable cause) {
            super("Invalid permission: "+perm, cause);
            this.perm = perm;
        }

        public String getPerm() {
            return perm;
        }

        public APIError toAPIError(APIError.APIErrorEnum code, String subErr, String message) {
            return new APIError(code, subErr, message).addData("perm", perm);
        }
    }
}
