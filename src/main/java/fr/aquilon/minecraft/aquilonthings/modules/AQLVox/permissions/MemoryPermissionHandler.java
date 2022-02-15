package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MemoryPermissionHandler implements APIPermissions {
    private final Set<String> perms;
    private final Set<String> deniedPerms;

    public MemoryPermissionHandler() {
        this.perms = new HashSet<>();
        this.deniedPerms = new HashSet<>();
    }

    @Override
    public APIPermissions addAllPerms(Collection<String> perms) {
        for (String p : perms)
            addPerm(p);
        return this;
    }

    @Override
    public APIPermissions clearAllPerms() {
        this.perms.clear();
        this.deniedPerms.clear();
        return this;
    }

    @Override
    public APIPermissions addPerm(String perm) {
        if (perm.startsWith("-")) {
            String p = perm.substring(1);
            this.deniedPerms.add(p);
            this.perms.remove(p);
        } else {
            this.perms.add(perm);
            deniedPerms.remove(perm);
        }
        return this;
    }

    @Override
    public APIPermissions removePerm(String perm) {
        this.perms.remove(perm);
        this.deniedPerms.remove(perm);
        return this;
    }

    @Override
    public boolean hasPerm(String perm) throws InvalidPermissionException {
        String[] hierarchy = APIPermissions.getPermHierarchy(perm);
        for (String pHierarchy: hierarchy) {
            for (String p: perms) {
                if (p.equals("all")) return true;
                if (pHierarchy.matches(APIPermissions.getPermRegex(p))) {
                    return !hasPermRemoved(perm);
                }
            }
        }
        return false;
    }

    public boolean hasPermRemoved(String perm) throws InvalidPermissionException {
        String[] hierarchy = APIPermissions.getPermHierarchy(perm);
        for (String pHierarchy: hierarchy) {
            for (String p: deniedPerms) {
                if (p.equals("all")) return true;
                if (pHierarchy.matches(APIPermissions.getPermRegex(p))) return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getPermList() {
        return Collections.unmodifiableSet(perms);
    }
}
