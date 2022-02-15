package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIUser;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class MissingPermissionEx extends APIError {
    public MissingPermissionEx(APIUser user, String perm) {
        super(APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_PERM_MISSING, "Missing Permission");
        addData("user", user.toJSON());
        addData("permission", perm);
    }
}
