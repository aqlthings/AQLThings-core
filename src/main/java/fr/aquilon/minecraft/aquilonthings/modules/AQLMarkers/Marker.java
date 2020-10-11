package fr.aquilon.minecraft.aquilonthings.modules.AQLMarkers;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.utils.UpsertStatement;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A simple marker
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class Marker implements IMarker {
    public static final String DEFAULT_ICON = "";

    private int id;
    private Location pos;
    private String name;
    private String icon;
    private String label;
    private String desc;
    private String perm;
    private MarkerGroup group;

    public Marker(int id, Location pos, String name, String icon, String label, String desc, String perm, MarkerGroup group) {
        this.id = id;
        this.pos = pos;
        this.name = name;
        this.icon = icon;
        this.label = label;
        this.desc = desc;
        this.perm = perm;
        setGroup(group);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Location getPosition() {
        return pos;
    }

    public void setPosition(Location pos) {
        this.pos = Objects.requireNonNull(pos);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return label != null ? label : name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    @Override
    public String getPermission() {
        return perm;
    }

    public void setPermission(String perm) {
        this.perm = perm;
    }

    @Override
    public MarkerGroup getGroup() {
        return group;
    }

    @Override
    public void setGroup(MarkerGroup group) {
        this.group = group;
        if (group != null && !group.contains(this)) group.add(this);
    }

    public void saveToDB(Connection conn) throws SQLException {
        UpsertStatement stmt = UpsertStatement.build(conn, AQLMarkers.DB_TABLE_MARKERS, new String[]{"id"},
                "group_id", "world", "x", "y", "z", "yaw", "pitch", "name", "icon", "label", "description", "perm")
                .setParameter("id", id > 0 ? id : null) // Use auto-increment
                .setParameter("group_id", group != null ? group.getId() : null)
                .setParameter("world", pos.getWorld().getName())
                .setParameter("x", pos.getBlockX())
                .setParameter("y", pos.getBlockY())
                .setParameter("z", pos.getBlockZ())
                .setParameter("yaw", pos.getYaw())
                .setParameter("pitch", pos.getPitch())
                .setParameter("name", name)
                .setParameter("icon", icon)
                .setParameter("label", label)
                .setParameter("description", desc)
                .setParameter("perm", perm);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            id = rs.getInt(1);
        }
    }

    public static Marker build(ResultSet rs) throws SQLException {
        return new Marker(
                rs.getInt("id"),
                new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getInt("x")+.5,
                        rs.getInt("y"),
                        rs.getInt("z")+.5,
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                ),
                rs.getString("name"),
                rs.getString("icon"),
                rs.getString("label"),
                rs.getString("description"),
                rs.getString("perm"),
                null);
    }

    public void deleteFromDB(Connection conn) throws SQLException {
        if (id <= 0) throw new IllegalStateException("Marker is not persisted");
        final String sqlMarker = "DELETE FROM "+AQLMarkers.DB_TABLE_MARKERS+" WHERE id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sqlMarker);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            DatabaseConnector.logException(e, sqlMarker);
            throw e;
        }
    }
}
