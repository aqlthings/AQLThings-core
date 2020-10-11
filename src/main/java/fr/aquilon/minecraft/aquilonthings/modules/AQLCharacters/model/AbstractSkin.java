package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 11/03/2018.
 *
 * @author Billi
 */
public abstract class AbstractSkin implements JSONExportable {
    private String name;
    private String file;

    public AbstractSkin(String name) {
        this.name = name;
    }

    // --- Accessors ---

    public String getName() {
        return name;
    }

    public AbstractSkin setName(String name) {
        this.name = name;
        return this;
    }

    public String getFile() {
        return file;
    }

    /**
     * @return The filename without the ending {@code .png}
     */
    public String getFileName() {
        if (!file.endsWith(".png")) return file;
        return file.substring(0, file.length()-4);
    }

    public AbstractSkin setFile(String file) {
        this.file = file;
        return this;
    }

    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name",getName());
        res.put("file",getFile());
        return res;
    }

    public abstract String getLabel();
}
