package fr.aquilon.minecraft.utils;

/**
 * Created by Billi on 01/05/2017.
 *
 * @author Billi
 */
public class InvalidArgumentEx extends IllegalArgumentException {
    private int code;

    public InvalidArgumentEx(int code, String message) {
        super (message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
