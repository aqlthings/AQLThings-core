package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Billi on 03/02/2019.
 *
 * @author Billi
 */
public class PlaceRadius extends Place {
    private int detectionRadius;
    private Location pos;

    public PlaceRadius(int id, String name, Location loc, int detectionRadius) {
        super(id, name, loc!=null ? loc.getWorld().getName() : null);
        this.pos = loc;
        this.detectionRadius = detectionRadius;
    }

    public PlaceRadius(int id, String name, String world) {
        this(id, name, null, 0);
        setWorldName(world);
    }

    public int getDetectionRadius() {
        return detectionRadius;
    }

    public Location getPosition() {
        return pos;
    }

    public int getX() {
        return pos.getBlockX();
    }

    public int getY() {
        return pos.getBlockY();
    }

    public int getZ() {
        return pos.getBlockZ();
    }

    @Override
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(getWorldName())) return false;
        return loc.distance(pos) < getDetectionRadius();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = super.toJSON();
        res.put("position", JSONUtils.jsonLocation(pos));
        res.put("radius", detectionRadius);
        return res;
    }

    @Override
    public String getInfos() {
        return super.getInfos()+"\nPosition: "+getX()+" / "+getY()+" / "+getZ()+
                "\nRayon: "+detectionRadius;
    }

    @Override
    public String parseReplacePattern(String command, Player p) {
        String cmd = super.parseReplacePattern(command, p);
        double dist = p.getLocation().distance(pos);
        cmd.replaceAll("\\{Distance\\}", Double.toString(dist));
        return cmd;
    }

    @Override
    public String[] getFields() {
        return Field.names();
    }

    @Override
    public Object getField(String field) {
        Field f = Field.valueOf(field);
        switch (f) {
            case POS:
                return getX()+","+getY()+","+getZ();
            case RADIUS:
                return detectionRadius;
            default:
                throw new IllegalArgumentException("Unsupported field");
        }
    }

    @Override
    public void setField(String field, Object value) {
        Field f = Field.valueOf(field);
        switch (f) {
            case POS:
                pos = Place.getLocation(getWorldName(), value.toString());
                break;
            case RADIUS:
                detectionRadius = Integer.parseInt(value.toString());
                break;
            default:
                throw new IllegalArgumentException("Unsupported field");
        }
    }

    public enum Field {
        POS,
        RADIUS;

        public static String[] names() {
            List<Field> l = Arrays.asList(values());
            return l.stream().map(Field::name).toArray(String[]::new);
        }
    }
}
