package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public interface APIPermissions {

    APIPermissions addAllPerms(Collection<String> perms);

    APIPermissions clearAllPerms();

    /**
     * If the permissions starts with '-' this is a negative permission
     * @param perm The permission string
     * @return this for command chaining
     */
    APIPermissions addPerm(String perm);

    APIPermissions removePerm(String perm);

    boolean hasPerm(String perm) throws InvalidPermissionException;

    Set<String> getPermList();

    // --- Static helpers ---

    static APIPermissions create() {
        return new MemoryPermissionHandler();
    }

    static String[] getPermHierarchy(String perm) {
        List<String> res = new ArrayList<>();
        res.add(perm);
        String curPerm = perm;
        while (curPerm.lastIndexOf('.')!=-1) {
            curPerm = curPerm.substring(0,curPerm.lastIndexOf('.'));
            if (!curPerm.isEmpty()) res.add(curPerm);
        }
        Collections.reverse(res);
        return res.toArray(new String[0]);
    }

    static String getPermRegex(String perm) throws InvalidPermissionException {
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

    // --- Exceptions ---

    class InvalidPermissionException extends RuntimeException {
        private final String perm;

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
