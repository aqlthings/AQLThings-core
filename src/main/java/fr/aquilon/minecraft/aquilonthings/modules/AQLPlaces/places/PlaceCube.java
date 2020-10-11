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
public class PlaceCube extends Place {
    private Location pos1;
    private Location pos2;

    public PlaceCube(int id, String name, Location loc1, Location loc2) {
        super(id, name, loc1 != null ? loc1.getWorld().getName() : null);
        if (loc1 != null && loc2 != null && !loc1.getWorld().getUID().equals(loc2.getWorld().getUID()))
            throw new IllegalArgumentException("Pos 1 and pos 2 must be in the same world");
        this.pos1 = loc1;
        this.pos2 = loc2;
    }

    public PlaceCube(int id, String name, String world) {
        this(id, name, null, null);
        setWorldName(world);
    }

    public Location getPosition1() {
        return pos1;
    }

    public Location getPosition2() {
        return pos2;
    }

    public int[][] getCube() {
        return new int[][] {
                new int[] {pos1.getBlockX(), pos2.getBlockX()},
                new int[] {pos1.getBlockY(), pos2.getBlockY()},
                new int[] {pos1.getBlockZ(), pos2.getBlockZ()}
        };
    }

    @Override
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(getWorldName())) return false;
        int[] pos = new int[] {loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()};
        int[][] cube = getCube();
        for (int i=0; i<3; i++) {
            int min, max;
            min = Math.min(cube[i][0], cube[i][1]);
            max = Math.max(cube[i][0], cube[i][1]);
            if (pos[i] < min || pos[i] > max) return false;
        }
        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = super.toJSON();
        res.put("pos1", JSONUtils.jsonLocation(pos1));
        res.put("pos2", JSONUtils.jsonLocation(pos1));
        return res;
    }

    @Override
    public String getInfos() {
        return super.getInfos()+
                "\nPosition 1: "+pos1.getBlockX()+" / "+pos1.getBlockY()+" / "+pos1.getBlockZ()+
                "\nPosition 2: "+pos2.getBlockX()+" / "+pos2.getBlockY()+" / "+pos2.getBlockZ();
    }

    @Override
    public String parseReplacePattern(String command, Player p) {
        String cmd = super.parseReplacePattern(command, p);
        //double dist = ?; TODO: find distance from cube center
        //cmd.replaceAll("\\{Distance\\}", Double.toString(dist));
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
            case POS1:
                return pos1.getX()+","+pos1.getY()+","+pos1.getZ();
            case POS2:
                return pos2.getX()+","+pos2.getY()+","+pos2.getZ();
            default:
                throw new IllegalArgumentException("Unsupported field");
        }
    }

    @Override
    public void setField(String field, Object value) {
        Field f = Field.valueOf(field);
        switch (f) {
            case POS1:
                pos1 = Place.getLocation(getWorldName(), value.toString());
                break;
            case POS2:
                pos2 = Place.getLocation(getWorldName(), value.toString());
                break;
            default:
                throw new IllegalArgumentException("Unsupported field");
        }
    }



    public enum Field {
        POS1,
        POS2;

        public static String[] names() {
            List<Field> l = Arrays.asList(values());
            return l.stream().map(Field::name).toArray(String[]::new);
        }
    }
}
