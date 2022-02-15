package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users;

import com.google.common.hash.Hashing;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.InvalidAuthEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.permissions.APIPermissions;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import org.bukkit.configuration.ConfigurationSection;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by Billi on 28/03/2018.
 *
 * @author Billi
 */
public class APIStaticUser extends APIUser {
    private final String pass;
    private final APIPermissions perms;

    public APIStaticUser(String name, String pass) {
        super(name);
        this.pass = pass;
        this.perms = APIPermissions.create();
    }

    public boolean checkKey(String key, String method) {
        long time = (System.currentTimeMillis()/1000) >> 5;
        String expected = Hashing.sha256().hashString(getName()+method+pass+time, StandardCharsets.UTF_8).toString();
        return expected.equals(key);
    }

    @Override
    public APIPermissions getPermissions() {
        return perms;
    }

    @Override
    public void check(APIRequest req) throws APIException {
        String key = req.getSession().getHeaders().getOrDefault("x-api-key",null); // TODO: change header name, or use HTTP basic auth
        if (key==null) {
            List<String> keyUriArgs = req.getSession().getParameters().getOrDefault("key",null);
            if (keyUriArgs!=null) key = keyUriArgs.get(0);
        }
        if (getSource()==Source.SIGNING_KEY && !checkKey(key, req.getPermName())) {
            throw new InvalidAuthEx(getName(), key); // Invalid key
        }
        super.check(req);
    }

    @Override
    public String getUniqueID() {
        return getName();
    }

    @Override
    public boolean isDefault() {
        return getName().equals("default");
    }

    // --- Static ---

    public static APIStaticUser fromConfig(ConfigurationSection data) {
        String user = data.getName();
        APIStaticUser usr = new APIStaticUser(user, data.getString("password"));
        List<String> defaultPerms = AQLVox.instance.getConfigArray("auth.default.permissions");
        usr.getPermissions()
                .addAllPerms(defaultPerms) // default permissions
                .addAllPerms(data.getStringList("permissions"));
        return usr;
    }
}
