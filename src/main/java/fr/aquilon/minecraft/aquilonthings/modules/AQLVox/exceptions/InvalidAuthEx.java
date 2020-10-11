package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class InvalidAuthEx extends APIError {
    public InvalidAuthEx() {
        super(APIError.APIErrorEnum.ERROR_INVALID_AUTH, SUBERR_INVALID_AUTH, "Invalid Authentication, user not found");
    }

    public InvalidAuthEx(String userName, String key) {
        super(APIError.APIErrorEnum.ERROR_INVALID_AUTH, SUBERR_INVALID_SIGNING, "Invalid Authentication, bad signing key");
        if (userName!=null) addData("userName", userName);
        if (key!=null) addData("key", key);
    }

    public InvalidAuthEx(String userName, APIUser user) {
        super(APIError.APIErrorEnum.ERROR_INVALID_AUTH, SUBERR_INVALID_AUTH, "Invalid Authentication");
        if (userName!=null) addData("userName", userName);
        if (user!=null) addData("user", user.toJSON());
    }
}
