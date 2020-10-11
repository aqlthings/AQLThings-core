package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class InternalServerErrorEx extends APIError {
    public InternalServerErrorEx(String message) {
        super(APIErrorEnum.ERROR_INTERNAL_ERROR, SUBERR_INTERNAL_ERROR, "Internal Server Error");
        addData("message", message);
    }
}
