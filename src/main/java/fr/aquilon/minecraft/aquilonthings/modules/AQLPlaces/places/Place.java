package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.Trigger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerFailedException;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Place implements JSONExportable {

	private int id;
	private String name;
	private String world;
	private ArrayList<Player> activePlayerList;
	private ArrayList<Trigger> triggers;

	public Place(int id, String name, String world) {
		this.id = id;
		this.name = name;
		this.activePlayerList = new ArrayList<>();
		this.triggers = new ArrayList<>();
		setWorldName(world);
	}

    public void setID(int id) {
        this.id = id;
    }

	public void setWorldName(String world) {
		this.world = world;
	}

	public int getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getWorldName() {
		return world;
	}

	public abstract boolean contains(Location loc);

	public PlaceTypeEnum getType() {
		return PlaceTypeEnum.fromClass(this.getClass());
	}

	/**
	 * Getter pour cette daube car maven c'est de la merde
	 * @return active player List
	 */
	public ArrayList<Player> getActivePlayersList(){
		return activePlayerList;
	}

	/**
	 * Ajouter un trigger.
	 * @param trigger
	 */
	public void addTrigger(Trigger trigger){
		triggers.add(trigger);
	}
	
	/**
	 * Supprimer un trigger.
	 * @param trigger
	 */
	public void removeTrigger(Trigger trigger){
		triggers.remove(trigger);
	}

    public void clearTriggers() {
	    triggers.clear();
    }

	public List<Trigger> getTriggers() {
		return Collections.unmodifiableList(triggers);
	}
	
	/**
	 * Retourne un trigger identifié par son nom.
	 * @param name Nom du trigger
	 * @return Objet trigger ou NULL si inconnu.
	 * @todo use a map with names
	 */
	public Trigger getTrigger(String name){
		for(Trigger trigger : triggers){
			if (trigger.getName().equals(name)){
				return trigger;
			}
		}
		return null;
	}
	
	/**
	 * Exécuter les triggers actifs
	 */
	public void trigger(AQLPlaces m, Player p, boolean goingIn) {
		for (Trigger trigger: triggers){
			if (!trigger.isEnabled()) continue;
			try {
				trigger.trigger(p, goingIn);
			} catch (TriggerFailedException e) {
				AQLPlaces.LOGGER.mWarning("Erreur d'exécution d'un trigger: "+e.trigger);
			}
		}
	}

    public String parseReplacePattern(String command, Player p) {
	    return command;
    }

	public boolean insertIntoDB(DatabaseConnector db, ModuleLogger log) {
		String sql = "INSERT INTO "+AQLPlaces.DB_PREFIX+"place_values VALUES (?,?,?)";
		Connection con = db.startTransaction();
		PreparedStatement stmt = db.prepare(con, sql);
		for (String f : getFields()) {
			try {
				stmt.setInt(1, id);
				stmt.setString(2, f);
				stmt.setString(3, getField(f).toString());
				stmt.addBatch();
			} catch (SQLException e) {
                log.mWarning("Unable to save field "+f+" for place "+id+"/"+name);
                db.logException(e, sql);
			}
		}
		try {
			stmt.executeBatch();
		} catch (SQLException e) {
			db.endTransaction(con, e, sql);
			return false;
		}
		db.endTransaction(con);
		return true;
	}

	public void retrieveParamsFromDB(DatabaseConnector db, ModuleLogger log){
		if (getFields().length<1) return;
		Connection con = db.startTransaction();
		String sql = "SELECT fieldname, fieldvalue FROM "+AQLPlaces.DB_PREFIX+"place_values " +
				"WHERE place_id = ? AND fieldname IN ('"+String.join("','", getFields())+"')";
		PreparedStatement stmt = db.prepare(con, sql);
		try {
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
                String field = rs.getString("fieldname");
                try {
                    setField(field, rs.getString("fieldvalue"));
                } catch (Exception e) {
                    log.mWarning(
                            "Unable to set field "+field+" on place "+id+"/"+name+" > " +
                            e.getClass().getName()+": "+e.getMessage()
                    );
                }
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, sql);
			return;
		}
		db.endTransaction(con);
	}

	public abstract String[] getFields();

	public abstract Object getField(String field);
	public abstract void setField(String field, Object value);

	@Override
	public JSONObject toJSON() {
		JSONObject res = new JSONObject();
		res.put("id", id);
        res.put("name", name);
		res.put("type", PlaceTypeEnum.fromClass(this.getClass()).getType());
        res.put("world", getWorldName());
        return res;
	}

    @Override
    public String toString() {
        return getType().name()+" "+getID()+"/"+getName();
    }

	public String getInfos() {
	    return "Type: "+getType().name()+"\nWorld: "+getWorldName();
	}

	// Static helpers
    public static Location getLocation(String worldName, String coords) {
        double[] p = new double[3];
        String[] posParts = coords.split(",");
        if (posParts.length!=3) throw new IllegalArgumentException("Malformed position found");
        for (int i = 0; i < posParts.length; i++) {
            p[i] = Double.parseDouble(posParts[i]);
        }
        World world = Bukkit.getWorld(worldName);
        if (world==null) throw new IllegalArgumentException("Unknown world");
        return new Location(world, p[0], p[1], p[2]);
    }
}
