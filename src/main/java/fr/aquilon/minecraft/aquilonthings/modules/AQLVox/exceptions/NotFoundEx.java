package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class NotFoundEx extends APIError {
    private String uri;
    private String message;

    public NotFoundEx(String uri) {
        this(uri, null);
    }

    public NotFoundEx(String uri, String msg) {
        super(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_ROUTE_NOT_FOUND, "Not Found");
        this.uri = uri;
        this.message = msg;
        addData("uri", uri);
        addData("message", msg);
    }

    public String getURI() {
        return uri;
    }

    public String getDetails() {
        return message;
    }
}
