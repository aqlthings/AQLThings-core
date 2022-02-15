package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;

import java.util.HashMap;
import java.util.Map;

public class UserProviderRegistry {
    private static UserProviderRegistry instance;

    private final Map<String, UserProvider<?>> registry;

    private UserProviderRegistry() {
        registry = new HashMap<>();
    }

    public void register(String userType, UserProvider<?> provider) {
        if (registry.containsKey(userType))
            throw new IllegalStateException("Registry already has a provider for: "+userType);
        registry.put(userType, provider);
    }

    public APIUser getUser(String userType, String userID) throws APIError {
        UserProvider<?> provider = registry.get(userType);
        if (provider == null) throw new IllegalArgumentException("Unsupported user type: "+userType);
        return provider.getUser(userID);
    }

    public static UserProviderRegistry getInstance() {
        if (instance == null) instance = new UserProviderRegistry();
        return instance;
    }

    @FunctionalInterface
    public interface UserProvider<T extends APIUser> {
        T getUser(String userID) throws APIError;
    }
}
