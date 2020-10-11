package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An action to execute when a specific event occurs on a place
 */
public abstract class Trigger {
    // TODO: implement JSONExportable
	private int id;
	private Place place;
	private String name;
	private boolean state;
	private ModuleLogger log;

	public Trigger(int id, Place p, String name, ModuleLogger log){
		this(id, p, name, true, log);
	}
	
	public Trigger(int id, Place p, String name, boolean state, ModuleLogger log){
		this.id = id;
		this.place = p;
		this.name = name;
		this.state = state;
		this.log = log;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public Place getPlace() {
		return place;
	}

	public void setPlace(Place place) {
		this.place = place;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public ModuleLogger getLogger() {
		return log;
	}

	@Override
	public String toString() {
		return getName()+" [id: "+getID()+"], place "+getPlace().getName();
	}

	public abstract boolean trigger(Player p, boolean goingIn) throws TriggerFailedException;

	public boolean insertIntoDB(DatabaseConnector db) {
		String sql = "INSERT INTO "+AQLPlaces.DB_PREFIX+"trigger_values VALUES (?,?,?)";
		Connection con = db.startTransaction();
		PreparedStatement stmt = db.prepare(con, sql);
		for (String f : getFields()) {
			try {
				stmt.setInt(1, id);
				stmt.setString(2, f);
				stmt.setString(3, getField(f).toString());
				stmt.addBatch();
			} catch (SQLException e) {
				log.mWarning("Unable to save field "+f+" for trigger "+id+"/"+name);
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

	public void retrieveParamsFromDB(DatabaseConnector db){
		if (getFields().length<1) return;
		Connection con = db.startTransaction();
		String sql = "SELECT fieldname, fieldvalue FROM "+AQLPlaces.DB_PREFIX+"trigger_values " +
				"WHERE trigger_id = ? AND fieldname IN ('"+String.join("','", getFields())+"')";
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
                            "Unable to set field "+field+" on trigger "+id+"/"+name+" > " +
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
	
	public void enable(){
		this.state = true;
	}
	public void disable(){
		this.state = false;
	}

	public abstract String[] getFields();

	public abstract Object getField(String field);
	public abstract void setField(String field, Object value);

	public TriggerTypeEnum getType() {
		return TriggerTypeEnum.fromClass(this.getClass());
	}

    public String getInfos() {
	    return "Type: "+getType().name();
	}
}
