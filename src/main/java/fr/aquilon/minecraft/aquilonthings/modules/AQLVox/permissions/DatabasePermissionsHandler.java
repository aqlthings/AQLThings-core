package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIDatabaseUser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DatabasePermissionsHandler implements APIPermissions {
    private final APIDatabaseUser user;
    private final DatabaseConnector db;
    private final Set<String> allowed;
    private final Set<String> denied;

    public DatabasePermissionsHandler(APIDatabaseUser user, DatabaseConnector db) {
        this.user = user;
        this.db = db;
        allowed = new HashSet<>();
        denied = new HashSet<>();
    }

    @Override
    public APIPermissions addAllPerms(Collection<String> perms) {
        for (String perm : perms) addPerm(perm); // FIXME: Single transaction or batch request
        return this;
    }

    @Override
    public APIPermissions clearAllPerms() {
        // FIXME
        return this;
    }

    /**
     * It is recommended to use {@link DatabasePermissionsHandler#addAllPerms(Collection)} to avoid multiple calls to database
     *
     * @param perm The permission string
     * @return this for command chaining
     */
    @Override
    public APIPermissions addPerm(String perm) {
        // FIXME
        return this;
    }

    @Override
    public APIPermissions removePerm(String perm) {
        // FIXME
        return this;
    }

    @Override
    public boolean hasPerm(String perm) throws InvalidPermissionException {
        // FIXME
        // Check cache
        return false;
    }

    @Override
    public Set<String> getPermList() {
        // FIXME
        return null;
    }
}
