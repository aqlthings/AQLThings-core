package fr.aquilon.minecraft.aquilonthings.modules.AQLMarkers;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.UpsertStatement;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A group of markers.
 * <p>
 *     This class is iterable over all the markers in this group.
 * </p>
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class MarkerGroup implements Iterable<IMarker>, JSONExportable {
    private int id;
    private String name;
    private String label;
    private Map<Integer, IMarker> markersByID;
    private Map<String, IMarker> markersByName;

    public MarkerGroup(int id, String name, String label) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.markersByID = new HashMap<>();
        this.markersByName = new HashMap<>();
    }

    public static MarkerGroup build(ResultSet rs) throws SQLException {
        return new MarkerGroup(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("label")
        );
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return label != null ? label : name;
    }

    /**
     * @return A set of markers in this group
     */
    public Set<IMarker> getMarkers() {
        return Collections.unmodifiableSet(new HashSet<>(markersByName.values()));
    }

    public IMarker getMarker(int id) {
        return markersByID.get(id);
    }

    public IMarker getMarker(String name) {
        return markersByName.get(name);
    }

    public boolean contains(IMarker marker) {
        return contains(marker.getName());
    }

    public boolean contains(String name) {
        return getMarker(name) != null;
    }

    public void add(IMarker marker) {
        if (contains(marker)) return;
        Objects.requireNonNull(marker);
        if (marker.getId() > 0) markersByID.put(marker.getId(), marker);
        markersByName.put(marker.getName(), marker);
        if (id != 0 && marker.getGroup() != this) marker.setGroup(this);
    }

    public void remove(IMarker marker) {
        Objects.requireNonNull(marker);
        if (marker.getId() > 0) markersByID.remove(marker.getId());
        markersByName.remove(marker.getName());
        if (id != 0 && marker.getGroup() == this) marker.setGroup(null);
    }

    public void updateMarkerInDB(IMarker marker, boolean present) throws SQLException {
        if (id <= 0) throw new IllegalStateException("Group is not persisted");
        if (marker.getId() <= 0) throw new IllegalStateException("Marker is not persisted");
        DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
        Connection conn = db.startTransaction(false);
        final String sql = "UPDATE "+AQLMarkers.DB_TABLE_MARKERS+" SET group_id = ? WHERE id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (present) stmt.setInt(1, id);
            else stmt.setNull(1, Types.INTEGER);
            stmt.setInt(2, marker.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(conn, e, sql);
            throw e;
        }
        db.endTransaction(conn);
    }

    @Override
    public Iterator<IMarker> iterator() {
        return getMarkers().iterator();
    }

    @Override
    public void forEach(Consumer<? super IMarker> action) {
        getMarkers().forEach(action);
    }

    @Override
    public Spliterator<IMarker> spliterator() {
        return getMarkers().spliterator();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("id", id > 0 ? id : JSONObject.NULL);
        res.put("name", name);
        res.put("displayName", label != null ? label : name);
        res.put("count", markersByName.size());
        return res;
    }

    public void saveToDB(Connection conn) throws SQLException {
        UpsertStatement stmt = UpsertStatement.build(conn, AQLMarkers.DB_TABLE_GROUPS, new String[]{"id"},
                "name", "label")
                .setParameter("id", id > 0 ? id : null) // Use auto-increment
                .setParameter("name", name)
                .setParameter("label", label);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            id = rs.getInt(1);
        }
    }

    public void deleteFromDB(Connection conn) throws SQLException {
        if (id <= 0) throw new IllegalStateException("Group is not persisted");
        final String sqlMarkers = "UPDATE "+AQLMarkers.DB_TABLE_MARKERS+" SET group_id = null WHERE group_id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sqlMarkers);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            DatabaseConnector.logException(e, sqlMarkers);
            throw e;
        }
        final String sqlGroup = "DELETE FROM "+AQLMarkers.DB_TABLE_GROUPS+" WHERE id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sqlGroup);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            DatabaseConnector.logException(e, sqlGroup);
            throw e;
        }
        forEach(m -> m.setGroup(null));
    }
}
