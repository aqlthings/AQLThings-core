package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters;

import com.google.common.hash.Hashing;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.CharacterNotFoundEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.MaxCharCountReachedEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions.MaxSkinCountReachedEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.AbstractCharacter;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharOrigins;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharRace;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Character;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterEdit;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterPlayer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CommonSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Skill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.SkillCategory;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.StaffNote;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.TempCharacter;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Billi on 03/03/2019.
 *
 * @author Billi
 */
public class CharacterDatabase {
    private AQLCharacters module;
    private DatabaseConnector db;

    public CharacterDatabase(AQLCharacters module) {
        this.module = module;
        this.db = AquilonThings.instance.getNewDatabaseConnector();
    }

    public AQLCharacters getModule() {
        return module;
    }

    public Character findCharacterByID(int charID) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, p_uuid, status, status_comment, category, " +
                "firstname, lastname, birth, age, height, weight, luck_points, " +
                "sex, race, origins, occupation, religion " +
                "FROM aqlcharacters_chars WHERE charid = ?";
        AbstractCharacter res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new Character(rs.getInt("charid"), rs.getString("p_uuid"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(rs.getString("category"))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setLuckPoints(rs.getInt("luck_points"))
                        .setOrigins(rs.getString("origins"))
                        .setOccupation(rs.getString("occupation"))
                        .setReligion(rs.getString("religion"))
                        .setHeight(rs.getFloat("height"))
                        .setWeight(rs.getFloat("weight"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return (Character) res;
    }

    public List<Character> findCharactersFromName(String charName) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, p_uuid, status, status_comment, category, " +
                "firstname, lastname, birth, age, height, weight, luck_points, " +
                "sex, race, origins, occupation, religion " +
                "FROM (SELECT *, CONCAT(firstname,' ',COALESCE(lastname,'')) AS `name` FROM aqlcharacters_chars) AS chars " +
                "WHERE LOWER(firstname) LIKE ? OR LOWER(lastname) LIKE ? OR LOWER(name) LIKE ? " +
                "LIMIT 50";
        List<Character> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, "%"+charName.toLowerCase()+"%");
            stmt.setString(2, "%"+charName.toLowerCase()+"%");
            stmt.setString(3, "%"+charName.toLowerCase()+"%");
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                res.add((Character) new Character(rs.getInt("charid"), rs.getString("p_uuid"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(rs.getString("category"))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setLuckPoints(rs.getInt("luck_points"))
                        .setOrigins(rs.getString("origins"))
                        .setOccupation(rs.getString("occupation"))
                        .setReligion(rs.getString("religion"))
                        .setHeight(rs.getFloat("height"))
                        .setWeight(rs.getFloat("weight"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<Character> findCharactersFromPartialUUID(String partialUUID) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, p_uuid, status, status_comment, category, " +
                "firstname, lastname, birth, age, height, weight, luck_points, " +
                "sex, race, origins, occupation, religion " +
                "FROM aqlcharacters_chars WHERE p_uuid LIKE ? " +
                "LIMIT 50";
        List<Character> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, partialUUID.toLowerCase()+"%");
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                res.add((Character) new Character(rs.getInt("charid"), rs.getString("p_uuid"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(rs.getString("category"))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setLuckPoints(rs.getInt("luck_points"))
                        .setOrigins(rs.getString("origins"))
                        .setOccupation(rs.getString("occupation"))
                        .setReligion(rs.getString("religion"))
                        .setHeight(rs.getFloat("height"))
                        .setWeight(rs.getFloat("weight"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public Character findPlayerCharacterFromName(String uuid, String charName) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, p_uuid, status, status_comment, category, " +
                "firstname, lastname, birth, age, height, weight, luck_points, " +
                "sex, race, origins, occupation, religion " +
                "FROM (SELECT *, CONCAT(firstname,' ',COALESCE(lastname,'')) AS `name` FROM aqlcharacters_chars) AS chars " +
                "WHERE p_uuid = ? AND LOWER(firstname) LIKE ? OR LOWER(lastname) LIKE ? OR LOWER(name) LIKE ? LIMIT 1";
        AbstractCharacter res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid);
            stmt.setString(2, "%"+charName.toLowerCase()+"%");
            stmt.setString(3, "%"+charName.toLowerCase()+"%");
            stmt.setString(4, "%"+charName.toLowerCase()+"%");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new Character(rs.getInt("charid"), rs.getString("p_uuid"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(rs.getString("category"))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setLuckPoints(rs.getInt("luck_points"))
                        .setOrigins(rs.getString("origins"))
                        .setOccupation(rs.getString("occupation"))
                        .setReligion(rs.getString("religion"))
                        .setHeight(rs.getFloat("height"))
                        .setWeight(rs.getFloat("weight"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return (Character) res;
    }

    public TempCharacter findTempCharacter(String pUUID) {
        Connection con = db.startTransaction();
        String sql = "SELECT p_uuid, name, height, weight, sex, race, skin " +
                "FROM aqlcharacters_temp_chars WHERE p_uuid = ?";
        AbstractCharacter res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, pUUID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new TempCharacter(rs.getString("p_uuid"))
                        .setName(rs.getString("name"))
                        .setSkin(rs.getString("skin"))
                        .setHeight(rs.getFloat("height"))
                        .setWeight(rs.getFloat("weight"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return (TempCharacter) res;
    }

    public CharacterPlayer findPlayer(String uuid) {
        Connection con = db.startTransaction();
        String sql = "SELECT p_uuid, c_charid, username, name, skin_name, common_skin, updated " +
                "FROM aqlcharacters_players WHERE p_uuid = ?";
        CharacterPlayer res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new CharacterPlayer(rs.getString("p_uuid"))
                        .setSelectedCharacter(rs.getInt("c_charid"))
                        .setUsername(rs.getString("username"))
                        .setName(rs.getString("name"))
                        .setSkinName(rs.getString("skin_name"))
                        .setCommonSkin(rs.getInt("common_skin"))
                        .setUpdated(rs.getTimestamp("updated"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterPlayer> findPlayersFromName(String name) {
        Connection con = db.startTransaction();
        String sql = "SELECT p_uuid, c_charid, username, name, skin_name, common_skin, updated " +
                "FROM aqlcharacters_players WHERE LOWER(username) LIKE ?";
        List<CharacterPlayer> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, '%'+name.toLowerCase()+'%');
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                res.add(new CharacterPlayer(rs.getString("p_uuid"))
                        .setSelectedCharacter(rs.getInt("c_charid"))
                        .setUsername(rs.getString("username"))
                        .setName(rs.getString("name"))
                        .setSkinName(rs.getString("skin_name"))
                        .setCommonSkin(rs.getInt("common_skin"))
                        .setUpdated(rs.getTimestamp("updated")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterPlayer> findPlayersFromCharacterName(String charName) {
        Connection con = db.startTransaction();
        String sql = "SELECT p.*, t.name AS temp_name, c.name AS char_name " +
                "FROM aqlcharacters_players AS p " +
                "LEFT JOIN (SELECT *, CONCAT(firstname,' ',COALESCE(lastname,'')) AS `name` FROM aqlcharacters_chars) AS c " +
                "ON c.charid = p.c_charid " +
                "LEFT JOIN aqlcharacters_temp_chars AS t " +
                "ON t.p_uuid = p.p_uuid " +
                "WHERE LOWER(p.name) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(t.name) LIKE ?";
        List<CharacterPlayer> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, '%'+charName.toLowerCase()+'%');
            stmt.setString(2, '%'+charName.toLowerCase()+'%');
            stmt.setString(3, '%'+charName.toLowerCase()+'%');
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                res.add(new CharacterPlayer(rs.getString("p_uuid"))
                        .setSelectedCharacter(rs.getInt("c_charid"))
                        .setUsername(rs.getString("username"))
                        .setName(rs.getString("name"))
                        .setSkinName(rs.getString("skin_name"))
                        .setCommonSkin(rs.getInt("common_skin"))
                        .setUpdated(rs.getTimestamp("updated")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean updatePlayer(Player player) {
        if (player==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, NULL, ?, NULL, NULL, NULL, ?) ON DUPLICATE KEY UPDATE " +
                "username = ?, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setString(2, player.getName());
            stmt.setString(4, player.getName());
            stmt.setTimestamp(3, updated);
            stmt.setTimestamp(5, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean setPlayerCharacter(Player player, Character character, String customName, CharacterSkin skin, CommonSkin commonSkin) {
        if (player==null || character==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "c_charid = ?, username = ?, name = ?, skin_name = ?, common_skin = ?, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setInt(2, character.getID());
            stmt.setInt(8, character.getID());
            stmt.setString(3, player.getName());
            stmt.setString(9, player.getName());
            stmt.setString(4, customName);
            stmt.setString(10, customName);
            stmt.setString(5, skin!=null ? skin.getName() : null);
            stmt.setString(11, skin!=null ? skin.getName() : null);
            if (commonSkin!=null) {
                stmt.setInt(6, commonSkin.getID());
                stmt.setInt(12, commonSkin.getID());
            } else {
                stmt.setNull(6, Types.INTEGER);
                stmt.setNull(12, Types.INTEGER);
            }
            stmt.setTimestamp(7, updated);
            stmt.setTimestamp(13, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean setPlayerCharacterTemp(Player player, String customName, CommonSkin commonSkin) {
        if (player==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, NULL, ?, ?, NULL, ?, ?) ON DUPLICATE KEY UPDATE " +
                "c_charid = NULL, username = ?, name = ?, skin_name = NULL, " +
                "common_skin = ?, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setString(2, player.getName());
            stmt.setString(6, player.getName());
            stmt.setString(3, customName);
            stmt.setString(7, customName);
            if (commonSkin!=null) {
                stmt.setInt(4, commonSkin.getID());
                stmt.setInt(8, commonSkin.getID());
            } else {
                stmt.setNull(4, Types.INTEGER);
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setTimestamp(5, updated);
            stmt.setTimestamp(9, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean setPlayerCustomName(Player player, String customName) {
        if (player==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, NULL, ?, ?, NULL, NULL, ?) ON DUPLICATE KEY UPDATE " +
                "username = ?, name = ?, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setString(2, player.getName());
            stmt.setString(5, player.getName());
            stmt.setString(3, customName);
            stmt.setString(6, customName);
            stmt.setTimestamp(4, updated);
            stmt.setTimestamp(7, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean unsetPlayerCharacter(Player player) {
        if (player==null) return false;
        Connection con = db.startTransaction(false);
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, NULL, ?, NULL, NULL, NULL, ?) ON DUPLICATE KEY UPDATE " +
                "c_charid = NULL, username = ?, name = NULL, skin_name = NULL, " +
                "common_skin = NULL, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setString(2, player.getName());
            stmt.setString(4, player.getName());
            stmt.setTimestamp(3, updated);
            stmt.setTimestamp(5, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        String sql = "DELETE FROM aqlcharacters_temp_chars WHERE p_uuid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean setPlayerSkin(Player player, CharacterSkin skin) {
        if (player==null || skin==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, updated) " +
                "VALUES (?, ?, ?, NULL, ?, ?) ON DUPLICATE KEY UPDATE " +
                "c_charid = ?, username = ?, skin_name = ?, updated = ?";
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setInt(2, skin.getCharacterID());
            stmt.setInt(6, skin.getCharacterID());
            stmt.setString(3, player.getName());
            stmt.setString(7, player.getName());
            stmt.setString(4, skin.getName());
            stmt.setString(8, skin.getName());
            stmt.setTimestamp(5, updated);
            stmt.setTimestamp(9, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean setPlayerCommonSkin(Player player, CommonSkin skin, boolean select) {
        if (player==null || skin==null) return false;
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_players " +
                "(p_uuid, c_charid, username, name, skin_name, common_skin, updated) " +
                "VALUES (?, NULL, ?, NULL, NULL, ?, ?) ON DUPLICATE KEY UPDATE " +
                "username = ?, common_skin = ?, updated = ?" +
                (select ? ", skin_name = NULL" : "");
        Timestamp updated = Timestamp.from(Instant.now());
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setString(1, uuid);
            stmt.setString(2, player.getName());
            stmt.setString(5, player.getName());
            stmt.setInt(3, skin.getID());
            stmt.setInt(6, skin.getID());
            stmt.setTimestamp(4, updated);
            stmt.setTimestamp(7, updated);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean fillCharacterDescriptions(Character pChar) throws CharacterNotFoundEx {
        Connection con = db.startTransaction();
        String sql = "SELECT physical_desc, story, details FROM aqlcharacters_chars WHERE charid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, pChar.getID());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                pChar
                        .setPhysicalDescription(rs.getString("physical_desc"))
                        .setStory(rs.getString("story"))
                        .setDetails(rs.getString("details"));
            } else throw new CharacterNotFoundEx();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public List<Character> findPlayerCharacters(String uuid) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, status, status_comment, firstname, lastname, birth, age, sex, race " +
                "FROM aqlcharacters_chars WHERE p_uuid = ?";
        ArrayList<Character> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                AbstractCharacter c = new Character(rs.getInt("charid"), uuid)
                        .setStatusComment(rs.getString("status_comment"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
                list.add((Character) c);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<Character> findCharactersWithStatus(String status) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, p_uuid, category, status, status_comment, firstname, lastname, " +
                "birth, age, sex, race FROM aqlcharacters_chars WHERE status = ?";
        ArrayList<Character> list = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                AbstractCharacter c = new Character(rs.getInt("charid"), rs.getString("p_uuid"))
                        .setCategory(rs.getString("category"))
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
                list.add((Character) c);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<Character> findSharedCharactersWithName(String category, String name) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, status, status_comment, firstname, lastname, birth, age, sex, race " +
                "FROM aqlcharacters_chars WHERE p_uuid IS NULL AND category = ? " +
                "AND LOWER(CONCAT(firstname,' ',COALESCE(lastname,''))) LIKE ?";
        ArrayList<Character> list = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            stmt.setString(2, "%"+name.toLowerCase()+"%");
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                AbstractCharacter c = new Character(rs.getInt("charid"), null)
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(category)
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
                list.add((Character) c);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<Character> findSharedCharacters(String category) {
        Connection con = db.startTransaction();
        String sql = "SELECT charid, status, status_comment, firstname, lastname, birth, age, sex, race " +
                "FROM aqlcharacters_chars WHERE p_uuid IS NULL AND category = ?";
        ArrayList<Character> list = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                AbstractCharacter c = new Character(rs.getInt("charid"), null)
                        .setStatus(Character.Status.find(rs.getString("status")))
                        .setStatusComment(rs.getString("status_comment"))
                        .setCategory(category)
                        .setFirstName(rs.getString("firstname"))
                        .setLastName(rs.getString("lastname"))
                        .setBirth(rs.getString("birth"))
                        .setAge(rs.getInt("age"))
                        .setSex(rs.getString("sex").charAt(0))
                        .setRace(rs.getString("race"));
                list.add((Character) c);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<String> findCharacterCategories() {
        Connection con = db.startTransaction();
        String sql = "SELECT category FROM aqlcharacters_chars WHERE category IS NOT NULL GROUP BY category";
        ArrayList<String> list;
        try {
            ResultSet rs = db.query(con, sql);
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<CharacterSkin> findCharacterSkins(int charID) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_skins WHERE c_charid = ? ORDER BY name ASC";
        ArrayList<CharacterSkin> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add((CharacterSkin) new CharacterSkin(charID, rs.getString("name"))
                        .setFile(rs.getString("file")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public CharacterSkin findCharacterSkin(int charID, String skinName) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_skins WHERE c_charid = ? AND name = ?";
        CharacterSkin res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, skinName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = (CharacterSkin) new CharacterSkin(charID, rs.getString("name"))
                        .setFile(rs.getString("file"));
            } else return null;
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean putCharacterSkin(int charID, UUID player, String name, BufferedImage img, boolean noLimit) throws MaxSkinCountReachedEx {
        String pName = player!=null?player.toString().replaceAll("-",""):"staff";
        String fileName = pName+"-"+
                Hashing.sha256().hashBytes(
                        (
                                pName
                                        + "-char"+charID
                                        + "-"+Utils.randomString(5)
                                        + "-"+name
                        ).getBytes()
                ).toString()
                +".png";
        String skinDir = AquilonThings.instance.getConfig().getString("AQLCharacters.skins.folder");
        if (skinDir==null || !(new File(skinDir)).isDirectory()) {
            AQLCharacters.LOGGER.mWarning("Skins directory undefined or not a directory (config: AQLCharacters.skins.folder)");
            return false;
        }
        File skinFile = new File(skinDir+(skinDir.endsWith(File.separator)?"":File.separator)+fileName);
        Connection con = db.startTransaction(false);
        if (con==null) return false;
        String querySQL = "SELECT p_uuid, COUNT(name) as count " +
                "FROM aqlcharacters_chars, aqlcharacters_char_skins " +
                "WHERE charid = c_charid AND charid = ?";
        PreparedStatement query = db.prepare(con, querySQL);
        String pUUID;
        int skinCount;
        try {
            query.setInt(1, charID);
            ResultSet queryRes = query.executeQuery();
            if (queryRes.next()) {
                pUUID = queryRes.getString("p_uuid");
                skinCount = queryRes.getInt("count");
            } else {
                db.endTransaction(con, false);
                return false;
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, querySQL);
            return false;
        }
        int maxSkinCount = AquilonThings.instance.getConfig().getInt("AQLCharacters.skins.max", 5);
        if (!noLimit && pUUID!=null && skinCount==maxSkinCount) {
            db.endTransaction(con, false);
            throw new MaxSkinCountReachedEx(maxSkinCount);
        }
        String sql = "INSERT INTO aqlcharacters_char_skins" +
                "(c_charid, name, file) " +
                "VALUES (?,?,?)";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, name);
            stmt.setString(3, fileName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        try {
            ImageIO.write(img, "png", skinFile);
        } catch (IOException e) {
            AQLCharacters.LOGGER.mWarning("Unable to save skin file "+skinFile.getAbsolutePath());
            db.endTransaction(con, false);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean putTempCharacterSkin(UUID player, String name, BufferedImage img) {
        String pUUID = player.toString().replaceAll("-","");
        String fileName = pUUID+"-"+
                Hashing.sha256().hashBytes(
                        (
                                pUUID
                                        + "-"+Utils.randomString(5)
                                        + "-"+name
                        ).getBytes()
                ).toString()
                +".png";
        String skinDir = AquilonThings.instance.getConfig().getString("AQLCharacters.skins.folder");
        if (skinDir==null || !(new File(skinDir)).isDirectory()) {
            AQLCharacters.LOGGER.mWarning("Skins directory undefined or not a directory (config: AQLCharacters.skins.folder)");
            return false;
        }
        File skinFile = new File(skinDir+(skinDir.endsWith(File.separator)?"":File.separator)+fileName);
        Connection con = db.startTransaction();
        if (con==null) return false;
        String sql = "UPDATE aqlcharacters_temp_chars " +
                "SET skin = ? WHERE p_uuid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, fileName);
            stmt.setString(2, pUUID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        try {
            ImageIO.write(img, "png", skinFile);
        } catch (IOException e) {
            AQLCharacters.LOGGER.mWarning("Unable to save skin file "+skinFile.getAbsolutePath());
            db.endTransaction(con, false);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean updateCharacterSkinName(int charID, String oldName, String newName) {
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlcharacters_char_skins SET name = ? WHERE c_charid = ? AND name = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, newName);
            stmt.setInt(2, charID);
            stmt.setString(3, oldName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean deleteCharacterSkin(CharacterSkin skin) {
        String skinDir = AquilonThings.instance.getConfig().getString("AQLCharacters.skins.folder");
        File skinFile = new File(skinDir, skin.getFile());
        if (!skinFile.isFile()) return false;
        Connection con = db.startTransaction();
        String sql = "DELETE FROM aqlcharacters_char_skins " +
                "WHERE c_charid = ? AND name = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, skin.getCharacterID());
            stmt.setString(2, skin.getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        if (!skinFile.delete()) {
            db.endTransaction(con, false);
            AQLCharacters.LOGGER.mInfo("Unable to delete skin file "+skinFile.getName());
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean deleteTempCharacterSkin(TempCharacter pChar) {
        if (pChar==null || pChar.getSkin()==null) return false;
        String skinDir = AquilonThings.instance.getConfig().getString("AQLCharacters.skins.folder");
        File skinFile = new File(skinDir, pChar.getSkin());
        if (!skinFile.isFile()) return false;
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlcharacters_temp_chars " +
                "SET skin = NULL " +
                "WHERE p_uuid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, pChar.getPlayerUUID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        if (!skinFile.delete()) {
            db.endTransaction(con, false);
            AQLCharacters.LOGGER.mInfo("Unable to delete skin file "+skinFile.getName());
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public List<UUID> findPlayersWithCharacterSkin(int charID, String name) {
        Connection con = db.startTransaction();
        String sql = "SELECT p_uuid FROM aqlcharacters_players WHERE c_charid = ? AND skin_name = ?";
        List<UUID> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, name);
            res = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                res.add(UUID.fromString(Utils.addUuidDashes(rs.getString("p_uuid"))));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<SkillCategory> findRequiredSkillCategories() {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skill_cat WHERE required = 1";
        ArrayList<SkillCategory> list;
        try {
            ResultSet rs = db.query(con, sql);
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new SkillCategory(rs.getString("skill_cat"))
                        .setLabel(rs.getString("label"))
                        .setRequired(rs.getBoolean("required"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public List<SkillCategory> findCharacterSkillCategories(int charID) {
        Connection con = db.startTransaction();
        String sql = "SELECT s.* FROM aqlcharacters_skill_cat AS s, aqlcharacters_char_skill_cat AS c " +
                "WHERE s.skill_cat = c.skill_cat AND c_charid = ?";
        ArrayList<SkillCategory> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new SkillCategory(rs.getString("skill_cat"))
                        .setLabel(rs.getString("label"))
                        .setRequired(rs.getBoolean("required")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public SkillCategory findCharacterSkillCategory(int charID, String category) {
        Connection con = db.startTransaction();
        String sql = "SELECT s.* FROM aqlcharacters_skill_cat AS s, aqlcharacters_char_skill_cat AS c " +
                "WHERE s.skill_cat = c.skill_cat AND c_charid = ? AND c.skill_cat = ?";
        SkillCategory res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                res = new SkillCategory(rs.getString("skill_cat"))
                        .setLabel(rs.getString("label"))
                        .setRequired(rs.getBoolean("required"));
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterSkill> findCharacterSkills(int charID) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_skills WHERE c_charid = ?";
        ArrayList<CharacterSkill> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CharacterSkill(rs.getInt("c_charid"), rs.getString("skill_cat"), rs.getString("skill_name"))
                        .setLevel(rs.getInt("lvl"))
                        .setComment(rs.getString("comment"))
                        .setCategoryUnlocked(true));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public CharacterSkill findCharacterSkillFromShorthand(int charID, String shorthand) {
        Connection con = db.startTransaction();
        String sql = "SELECT c.* FROM aqlcharacters_char_skills AS c, aqlcharacters_skills AS s " +
                "WHERE c.skill_cat = s.skill_cat AND c.skill_name = s.skill_name AND c_charid = ? AND shorthand = ?";
        CharacterSkill res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, shorthand);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                res = new CharacterSkill(rs.getInt("c_charid"),
                                rs.getString("skill_cat"), rs.getString("skill_name"))
                        .setLevel(rs.getInt("lvl"))
                        .setComment(rs.getString("comment"))
                        .setCategoryUnlocked(true);
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean unlockCharacterSkillCategory(int charID, String category) {
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_char_skill_cat VALUES (?, ?)";
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setInt(1, charID);
            stmt.setString(2, category);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean removeCharacterSkillCategory(int charID, String category) {
        Connection con = db.startTransaction();
        String sqlUpsert = "DELETE FROM aqlcharacters_char_skill_cat WHERE c_charid = ? AND skill_cat = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setInt(1, charID);
            stmt.setString(2, category);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean updateCharacterSkill(CharacterSkill skill) {
        Connection con = db.startTransaction();
        String sqlUpsert = "INSERT INTO aqlcharacters_char_skills " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "lvl = ?, comment = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sqlUpsert);
            stmt.setInt(1, skill.getCharID());
            stmt.setString(2, skill.getCategory());
            stmt.setString(3, skill.getName());
            stmt.setInt(4, skill.getLevel());
            stmt.setInt(6, skill.getLevel());
            stmt.setString(5, skill.getComment());
            stmt.setString(7, skill.getComment());
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlUpsert);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean deleteCharacterSkillsFromCategory(int charid, String category) {
        Connection con = db.startTransaction();
        String sql = "DELETE FROM aqlcharacters_char_skills WHERE c_charid = ? AND skill_cat = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charid);
            stmt.setString(2, category);
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public SkillCategory findSkillCategory(String category) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skill_cat WHERE skill_cat = ?";
        SkillCategory res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                res = new SkillCategory(rs.getString("skill_cat"))
                        .setLabel(rs.getString("label"))
                        .setRequired(rs.getBoolean("required"));
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<SkillCategory> findSkillCategories() {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skill_cat";
        ArrayList<SkillCategory> list;
        try {
            ResultSet rs = db.query(con, sql);
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new SkillCategory(rs.getString("skill_cat"))
                        .setLabel(rs.getString("label"))
                        .setRequired(rs.getBoolean("required"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public Map<String, List<Skill>> findSkills() {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skills";
        HashMap<String, List<Skill>> catList;
        try {
            ResultSet rs = db.query(con, sql);
            catList = new HashMap<>();
            while (rs.next()) {
                String cat = rs.getString("skill_cat");
                List<Skill> list = catList.computeIfAbsent(cat, k -> new ArrayList<>());
                list.add(new Skill(cat, rs.getString("skill_name"))
                        .setLabel(rs.getString("label"))
                        .setShorthand(rs.getString("shorthand"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return catList;
    }

    public Skill findSkillFromShorthand(String shorthand) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skills WHERE shorthand = ?";
        Skill res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, shorthand);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                res = new Skill(rs.getString("skill_cat"), rs.getString("skill_name"))
                        .setLabel(rs.getString("label"))
                        .setShorthand(rs.getString("shorthand"));             
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<Skill> findSkillsInCategory(String category) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skills WHERE skill_cat = ?";
        List<Skill> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Skill(category, rs.getString("skill_name"))
                        .setLabel(rs.getString("label"))
                        .setShorthand(rs.getString("shorthand"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public Skill findSkill(String category, String skill) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_skills WHERE skill_cat = ? AND skill_name = ?";
        Skill res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            stmt.setString(2, skill);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                res = new Skill(category, rs.getString("skill_name"))
                        .setLabel(rs.getString("label"))
                        .setShorthand(rs.getString("shorthand"));
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public Map<String,List<CharRace>> findRaces() {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_races";
        Map<String,List<CharRace>> list;
        try {
            ResultSet rs = db.query(con, sql);
            list = new HashMap<>();
            while (rs.next()) {
                String cat = rs.getString("category");
                if (list.get(cat)==null) list.put(cat, new ArrayList<>());
                list.get(cat).add(new CharRace(rs.getString("name"))
                        .setCategory(cat)
                        .setInfos(rs.getString("info"))
                        .setMinHeight(rs.getFloat("min_height"))
                        .setMaxHeight(rs.getFloat("max_height"))
                        .setMinWeight(rs.getFloat("min_weight"))
                        .setMaxWeight(rs.getFloat("max_weight"))
                        .setDefWeight(rs.getFloat("def_weight"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public CharRace findRaceFromName(String race) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_races WHERE name = ?";
        CharRace res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, race);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new CharRace(rs.getString("name"))
                        .setCategory(rs.getString("category"))
                        .setInfos(rs.getString("info"))
                        .setMinHeight(rs.getFloat("min_height"))
                        .setMaxHeight(rs.getFloat("max_height"))
                        .setMinWeight(rs.getFloat("min_weight"))
                        .setMaxWeight(rs.getFloat("max_weight"))
                        .setDefWeight(rs.getFloat("def_weight"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public int getPlayerCharacterCount(String uuid) {
        Connection con = db.startTransaction();
        int count;
        try {
            count = getPlayerCharacterCount(con, uuid);
        } catch (SQLException e) {
            db.endTransaction(con, e);
            return -1;
        }
        db.endTransaction(con);
        return count;
    }

    private int getPlayerCharacterCount(Connection con, String uuid) throws SQLException {
        String querySQL = "SELECT COUNT(charid) as count " +
                "FROM aqlcharacters_chars " +
                "WHERE p_uuid = ? AND status != 'archived'";
        PreparedStatement query = db.prepare(con, querySQL);
        query.setString(1, uuid);
        ResultSet queryRes = query.executeQuery();
        if (!queryRes.next()) {
            return -1;
        }
        return queryRes.getInt("count");
    }

    public boolean putCharacter(Character pChar, boolean noLimit) throws MaxCharCountReachedEx {
        Connection con;
        if (noLimit || pChar.getPlayerUUID()==null) {
            con = db.startTransaction();
        } else {
            con = db.startTransaction(false);
            if (con==null) return false;
            int charCount;
            try {
                charCount = getPlayerCharacterCount(con, pChar.getPlayerUUID());
            } catch (SQLException e) {
                db.endTransaction(con, e);
                return false;
            }
            if (charCount<0) return false;
            int maxCharCount = AquilonThings.instance.getConfig().getInt("AQLCharacters.chars.max", 3);
            if (charCount>=maxCharCount) {
                db.endTransaction(con, false);
                throw new MaxCharCountReachedEx(maxCharCount);
            }
        }
        String sql = "INSERT INTO aqlcharacters_chars" +
                "(p_uuid, status, category, firstname, lastname, sex, race, birth, age, height, origins, religion, occupation)" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement stmt = db.prepare(con, sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, pChar.getPlayerUUID());
            stmt.setString(2, pChar.setStatus(Character.Status.CREATED).getStatus().name().toLowerCase());
            stmt.setString(3, pChar.getCategory());
            stmt.setString(4, pChar.getFirstName());
            stmt.setString(5, pChar.getLastName());
            stmt.setString(6, pChar.getSex()+"");
            stmt.setString(7, pChar.getRace());
            stmt.setString(8, pChar.getBirth());
            if (pChar.getAge()!=0) stmt.setInt(9, pChar.getAge());
            else stmt.setNull(9, Types.INTEGER);
            if (pChar.getHeight()!=0) stmt.setFloat(10, pChar.getHeight());
            else stmt.setNull(10, Types.FLOAT);
            stmt.setString(11, pChar.getOrigins());
            stmt.setString(12, pChar.getReligion());
            stmt.setString(13, pChar.getOccupation());
            stmt.executeUpdate();
            ResultSet genID = stmt.getGeneratedKeys();
            genID.next();
            pChar.setId(genID.getInt(1));
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        AQLCharacters.LOGGER.mInfo("Character "+pChar.getID()+" created.");
        return true;
    }

    public boolean putTempCharacter(TempCharacter tChar) {
        Connection con = db.startTransaction();
        String sql = "INSERT INTO aqlcharacters_temp_chars" +
                "(p_uuid, name, sex, race, height, skin) " +
                "VALUES (?,?,?,?,?,NULL) ON DUPLICATE KEY " +
                "UPDATE name=?, sex=?, race=?, height=?, skin=NULL";
        try {
            PreparedStatement stmt = db.prepare(con, sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, tChar.getPlayerUUID());
            stmt.setString(2, tChar.getName());
            stmt.setString(6, tChar.getName());
            stmt.setString(3, tChar.getSex()+"");
            stmt.setString(7, tChar.getSex()+"");
            stmt.setString(4, tChar.getRace());
            stmt.setString(8, tChar.getRace());
            if (tChar.getHeight()!=0) {
                stmt.setFloat(5, tChar.getHeight());
                stmt.setFloat(9, tChar.getHeight());
            } else {
                stmt.setNull(5, Types.FLOAT);
                stmt.setNull(9, Types.FLOAT);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        AQLCharacters.LOGGER.mInfo("Temporary character created for player "+tChar.getPlayerUUID());
        return true;
    }

    public boolean deleteTempCharacter(String uuid) {
        TempCharacter character = findTempCharacter(uuid);
        return deleteTempCharacter(character);
    }

    public boolean deleteTempCharacter(TempCharacter character) {
        if (character == null) return true;
        if (character.getSkin()!=null && !deleteTempCharacterSkin(character))
            return false;
        Connection con = db.startTransaction();
        String sql = "DELETE FROM aqlcharacters_temp_chars " +
                "WHERE p_uuid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, character.getPlayerUUID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public List<CharOrigins> findOrigins() {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_origins";
        ArrayList<CharOrigins> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CharOrigins(rs.getString("name")).setWiki(rs.getString("wiki")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public boolean updateCharacter(Character newChar) {
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlcharacters_chars SET " +
                "status = ?, status_comment = ?, category = ?, firstname = ?, lastname = ?, sex = ?, race = ?, " +
                "age = ?, height = ?, birth = ?, origins = ?, occupation = ?, religion = ?, luck_points = ? " +
                "WHERE charid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, newChar.getStatus().name().toLowerCase());
            stmt.setString(2, newChar.getStatusComment());
            stmt.setString(3, newChar.getCategory());
            stmt.setString(4, newChar.getFirstName());
            stmt.setString(5, newChar.getLastName());
            stmt.setString(6, newChar.getSex()+"");
            stmt.setString(7, newChar.getRace());
            if (newChar.getAge()!=0) stmt.setInt(8, newChar.getAge());
            else stmt.setNull(8, Types.INTEGER);
            if (newChar.getHeight()!=0) stmt.setFloat(9, newChar.getHeight());
            else stmt.setNull(9, Types.FLOAT);
            stmt.setString(10, newChar.getBirth());
            stmt.setString(11, newChar.getOrigins());
            stmt.setString(12, newChar.getOccupation());
            stmt.setString(13, newChar.getReligion());
            stmt.setInt(14, newChar.getLuckPoints());
            stmt.setInt(15, newChar.getID());
            stmt.executeUpdate();
            AQLCharacters.LOGGER.mInfo("Character "+newChar.getID()+" updated.");
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean updateCharacterDescriptions(Character oldChar, Character newChar, int author) {
        if (oldChar.getID()!=newChar.getID()) return false;
        Connection con = db.startTransaction();
        String sqlDesc = "UPDATE aqlcharacters_chars " +
                "SET physical_desc = ?, story = ?, details = ? " +
                "WHERE charid = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sqlDesc);
            stmt.setString(1, newChar.getPhysicalDescription());
            stmt.setString(2, newChar.getStory());
            stmt.setString(3, newChar.getDetails());
            stmt.setInt(4, newChar.getID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, sqlDesc);
            return false;
        }
        db.endTransaction(con);
        Instant when = Instant.now();
        List<CharacterEdit> edits = new ArrayList<>();
        edits.add(CharacterEdit.Field.DESC_PHYSICAL.getCharacterEdit(oldChar, newChar, when, author));
        edits.add(CharacterEdit.Field.DESC_STORY.getCharacterEdit(oldChar, newChar, when, author));
        edits.add(CharacterEdit.Field.DESC_DETAILS.getCharacterEdit(oldChar, newChar, when, author));
        putCharacterEdits(edits);
        AQLCharacters.LOGGER.mInfo("Character "+newChar.getID()+" descriptions updated.");
        return true;
    }

    public CharacterEdit findCharacterFieldEdit(int charID, CharacterEdit.Field field, Instant when) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_edits WHERE charid = ? AND field = ? AND updated = ?";
        CharacterEdit res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, field.name());
            stmt.setTimestamp(3, Timestamp.from(when));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new CharacterEdit(charID,
                        CharacterEdit.Field.valueOf(rs.getString("field")),
                        rs.getTimestamp("updated").toInstant(),
                        rs.getInt("author")
                ).setStatus(rs.getString("status"))
                .setComment(rs.getString("comment"))
                .setDiff(rs.getString("diff"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        } catch (IllegalArgumentException e) {
            db.endTransaction(con);
            AQLCharacters.LOGGER.log(Level.WARNING, null, "Error while parsing diff (charID="+charID+"):", e);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterEdit> findLastCharacterFieldEdits(int charID, CharacterEdit.Field field, int limit, Instant from) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_edits WHERE charid = ? AND field = ? " +
                "AND updated < ? ORDER BY updated DESC LIMIT ?";
        List<CharacterEdit> res;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setString(2, field.name());
            stmt.setTimestamp(3, Timestamp.from(from != null ? from : Instant.now()));
            stmt.setInt(4, Math.min(limit, 500));
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                res.add(new CharacterEdit(charID,
                                CharacterEdit.Field.valueOf(rs.getString("field")),
                                rs.getTimestamp("updated").toInstant(),
                                rs.getInt("author")
                        ).setStatus(rs.getString("status"))
                        .setComment(rs.getString("comment"))
                        .setDiff(rs.getString("diff"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        } catch (IllegalArgumentException e) {
            db.endTransaction(con);
            AQLCharacters.LOGGER.log(Level.WARNING, null, "Error while parsing diff (charID="+charID+"):", e);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterEdit> findLastCharacterEdits(int charID, int limit, Instant from) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_edits WHERE updated < ? AND charid = ? "+
                "ORDER BY updated DESC LIMIT ?";
        List<CharacterEdit> res;
        Timestamp fromTs = Timestamp.from(from == null ? Instant.now() : from);
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setTimestamp(1, fromTs);
            stmt.setInt(2, charID);
            stmt.setInt(3, Math.min(limit, 500));
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                int cID = rs.getInt("charid");
                CharacterEdit edit = new CharacterEdit(cID,
                        CharacterEdit.Field.valueOf(rs.getString("field")),
                        rs.getTimestamp("updated").toInstant(),
                        rs.getInt("author")
                ).setStatus(rs.getString("status"))
                        .setComment(rs.getString("comment"));
                try {
                    edit.setDiff(rs.getString("diff"));
                } catch (IllegalArgumentException e) {
                    AQLCharacters.LOGGER.log(Level.WARNING, null, "Error while parsing diff (charID="+cID+"):", e);
                    continue;
                }
                res.add(edit);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public List<CharacterEdit> findLastEdits(String status, int limit, Instant from) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_edits WHERE updated < ?"+(status!=null?" AND status = ?":"")+
                " ORDER BY updated DESC LIMIT ?";
        List<CharacterEdit> res;
        Timestamp fromTs = Timestamp.from(from == null ? Instant.now() : from);
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            int id = 1;
            stmt.setTimestamp(id++, fromTs);
            if (status!=null) stmt.setString(id++, status);
            stmt.setInt(id, Math.min(limit, 500));
            ResultSet rs = stmt.executeQuery();
            res = new ArrayList<>();
            while (rs.next()) {
                int cID = rs.getInt("charid");
                CharacterEdit edit = new CharacterEdit(cID,
                        CharacterEdit.Field.valueOf(rs.getString("field")),
                        rs.getTimestamp("updated").toInstant(),
                        rs.getInt("author")
                ).setStatus(rs.getString("status"))
                .setComment(rs.getString("comment"));
                try {
                    edit.setDiff(rs.getString("diff"));
                } catch (IllegalArgumentException e) {
                    AQLCharacters.LOGGER.log(Level.WARNING, null, "Error while parsing diff (charID="+cID+"):", e);
                    continue;
                }
                res.add(edit);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean putCharacterEdits(List<CharacterEdit> edits) {
        Connection con = db.startTransaction();
        String sql = "INSERT INTO aqlcharacters_char_edits" +
                "(charid, field, updated, author, diff, status) " +
                "VALUES (?, ?, ?, ?, ?, 'new')";
        try {
            PreparedStatement stmt = db.prepare(con, sql);

            for (CharacterEdit edit : edits) {
                if (edit == null || edit.getDiff() == null) continue;
                stmt.setInt(1, edit.getCharID());
                stmt.setString(2, edit.getField().name());
                stmt.setTimestamp(3, Timestamp.from(edit.getUpdated()));
                stmt.setInt(4, edit.getAuthor());
                stmt.setString(5, edit.getDiff());
                stmt.addBatch();
            }

            stmt.executeBatch();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean updateCharacterEdit(CharacterEdit edit) {
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlcharacters_char_edits " +
                "SET status = ?, comment = ? " +
                "WHERE charid = ? AND field = ? AND updated = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, edit.getStatus());
            stmt.setString(2, edit.getComment());
            stmt.setInt(3, edit.getCharID());
            stmt.setString(4, edit.getField().name());
            stmt.setTimestamp(5, Timestamp.from(edit.getUpdated()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean fillCharacterStaffNotes(Character pChar) {
        List<StaffNote> res = findStaffNotes(pChar.getID());
        if (res==null) return false;
        pChar.setStaffNotes(res);
        return true;
    }

    public List<StaffNote> findStaffNotes(int charID) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_staff_notes " +
                "WHERE charid = ? ORDER BY created ASC";
        ArrayList<StaffNote> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(new StaffNote(rs.getInt("charid"), rs.getTimestamp("created").toInstant())
                        .setAuthor(rs.getInt("author"))
                        .setUpdated(rs.getTimestamp("updated").toInstant())
                        .setNote(rs.getString("note"))
                );
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public StaffNote findStaffNote(int charID, Instant noteCreationTime) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_char_staff_notes " +
                "WHERE charid = ? AND created = ?";
        StaffNote res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setTimestamp(2, Timestamp.from(noteCreationTime));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new StaffNote(rs.getInt("charid"), rs.getTimestamp("created").toInstant())
                        .setAuthor(rs.getInt("author"))
                        .setUpdated(rs.getTimestamp("updated").toInstant())
                        .setNote(rs.getString("note"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean putStaffNote(int charID, int author, String note) {
        Connection con = db.startTransaction();
        String sql = "INSERT INTO aqlcharacters_char_staff_notes " +
                "(charid, author, created, note) " +
                "VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setInt(2, author);
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setString(4, note);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean updateStaffNote(int charID, Instant noteCreationTime, int author, String note) {
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlcharacters_char_staff_notes " +
                "SET author = ?, note = ?, updated = ?" +
                "WHERE charid = ? AND created = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, author);
            stmt.setString(2, note);
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setInt(4, charID);
            stmt.setTimestamp(5, Timestamp.from(noteCreationTime));
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean deleteStaffNote(int charID, Instant noteCreationTime) {
        Connection con = db.startTransaction();
        String sql = "DELETE FROM aqlcharacters_char_staff_notes " +
                "WHERE charid = ? AND created = ?";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, charID);
            stmt.setTimestamp(2, Timestamp.from(noteCreationTime));
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public Map<String, Integer> findCommonSkinCategories() {
        Connection con = db.startTransaction();
        String sql = "SELECT category, COUNT(*) as count FROM aqlcharacters_common_skins GROUP BY category ORDER BY category ASC";
        HashMap<String, Integer> map;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            ResultSet rs = stmt.executeQuery();
            map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return map;
    }

    public List<CommonSkin> findCommonSkinsCategory(String category) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_common_skins WHERE category = ? ORDER BY name ASC";
        ArrayList<CommonSkin> list;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add((CommonSkin) new CommonSkin(rs.getInt("skin_id"), rs.getString("name"))
                        .setCategory(rs.getString("category"))
                        .setFile(rs.getString("file")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return list;
    }

    public CommonSkin findCommonSkin(int skinID) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_common_skins WHERE skin_id = ?";
        CommonSkin res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setInt(1, skinID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = (CommonSkin) new CommonSkin(skinID, rs.getString("name"))
                        .setCategory(rs.getString("category"))
                        .setFile(rs.getString("file"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public boolean putCommonSkin(String name, String category, BufferedImage img) {
        String fileName = "common-"+category+"-"+
                Hashing.sha256().hashBytes(
                        (
                                Utils.randomString(5) + "-" + name
                        ).getBytes()
                ).toString()
                +".png";
        String skinDir = AquilonThings.instance.getConfig().getString("AQLCharacters.skins.folder");
        if (skinDir==null || !(new File(skinDir)).isDirectory()) {
            AQLCharacters.LOGGER.mWarning("Skins directory undefined or not a directory (config: AQLCharacters.skins.folder)");
            return false;
        }
        File skinFile = new File(skinDir+(skinDir.endsWith(File.separator)?"":File.separator)+fileName);
        try {
            ImageIO.write(img, "png", skinFile);
        } catch (IOException e) {
            AQLCharacters.LOGGER.mWarning("Unable to save skin file "+skinFile.getAbsolutePath());
            return false;
        }
        Connection con = db.startTransaction();
        String sql = "INSERT INTO aqlcharacters_common_skins " +
                "(name, category, file) " +
                "VALUES (?,?,?)";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, fileName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    // TODO: return a list ?
    public CommonSkin findCommonSkinWithNames(String cat, String name) {
        Connection con = db.startTransaction();
        String sql = "SELECT * FROM aqlcharacters_common_skins WHERE category = ? AND name = ?";
        CommonSkin res = null;
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, cat);
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = (CommonSkin) new CommonSkin(rs.getInt("skin_id"), rs.getString("name"))
                        .setCategory(rs.getString("category"))
                        .setFile(rs.getString("file"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e,sql);
            return null;
        }
        db.endTransaction(con);
        return res;
    }
}
