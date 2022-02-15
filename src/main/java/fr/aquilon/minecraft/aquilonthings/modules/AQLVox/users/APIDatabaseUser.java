package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions.APIPermissions;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * An AQLVox user stored in the database
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class APIDatabaseUser extends APIUser {
    private static final String HASH_METHOD = "PBKDF2WithHmacSHA512";
    private static final int HASH_SIZE = 512;
    private static final int SALT_SIZE = 512;
    private static final int PBKDF2_ITERATIONS = 10000;
    private final String passwordHash;
    private final String passwordSalt;
    private final UUID minecraftUUID;
    private final APIPermissions perms;

    public APIDatabaseUser(String name, String pass, String salt, UUID mcUUID) {
        super(name);
        this.passwordHash = pass;
        this.passwordSalt = salt;
        this.minecraftUUID = mcUUID;
        this.perms = APIPermissions.create();
    }

    public boolean checkPassword(String pass)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoPasswordException
    {
        if (passwordHash == null) throw new NoPasswordException("This user has no password");
        String hash = hashPassword(pass, passwordSalt);
        return passwordHash.equals(hash);
    }

    @Override
    public String getUniqueID() {
        return getName();
    }

    @Override
    public boolean isDefault() {
        return getName().equals("default");
    }

    @Override
    public APIPermissions getPermissions() {
        return perms;
    }

    @Override
    public UUID getPlayerUUID() {
        return minecraftUUID;
    }

    // --- Static ---

    public static String hashPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] passwordSalt = Utils.hexStringToBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, PBKDF2_ITERATIONS, HASH_SIZE);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_METHOD);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Utils.bytesToHexString(hash);
    }

    public static APIDatabaseUser findUser(String username) {
        DatabaseConnector db = AQLVox.instance.getDatabaseConnector();
        Connection conn = db.startTransaction();
        String sql = "SELECT * FROM aqlvox_dbusers WHERE user_id = ?";
        PreparedStatement stmt = db.prepare(conn, sql);
        String permsStr;
        APIDatabaseUser usr;
        try {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                db.endTransaction(conn);
                return null;
            }
            permsStr = rs.getString("permissions");
            usr = new APIDatabaseUser(
                    username,
                    rs.getString("passwd"),
                    rs.getString("pw_salt"),
                    Utils.getUUID(rs.getString("p_uuid"))
            );
        } catch (SQLException e) {
            db.endTransaction(conn, e, sql);
            return null;
        }
        List<String> permSet = Arrays.asList(permsStr.split("; ?"));
        List<String> defaultPerms = AQLVox.instance.getConfigArray("auth.default.permissions");
        usr.perms.addAllPerms(defaultPerms); // default permissions
        usr.perms.addAllPerms(permSet);
        return usr;
    }

    public static class NoPasswordException extends Exception {
        public NoPasswordException() {}

        public NoPasswordException(String message) {
            super(message);
        }
    }

}
