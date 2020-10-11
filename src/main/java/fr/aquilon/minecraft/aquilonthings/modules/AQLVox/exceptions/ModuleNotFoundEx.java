package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class ModuleNotFoundEx extends APIError {
    public ModuleNotFoundEx(String module) {
        super(APIErrorEnum.ERROR_NOT_FOUND, SUBERR_MODULE_NOT_FOUND, "Module Not Found");
        addData("module", module);
    }
}
