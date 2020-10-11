package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

/**
 * Created by Billi on 30/04/2017.
 *
 * @author Billi
 */
public class BadJsonBodyEx extends APIError {
    public BadJsonBodyEx(String message) {
        super(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_JSON, "Invalid request body, must be valid JSON.");
        addData("message", message);
    }
}
