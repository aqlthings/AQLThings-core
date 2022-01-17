package fr.aquilon.minecraft.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A database statement to perform an Upsert (Update or Insert)
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class UpsertStatement {
    public final List<String> keys;
    public final List<String> params;
    public final String sql;
    public final PreparedStatement stmt;

    public UpsertStatement(String[] keys, String[] params, String sql, PreparedStatement stmt) {
        this.keys = Collections.unmodifiableList(Arrays.asList(keys));
        this.params = Collections.unmodifiableList(Arrays.asList(params));
        this.sql = sql;
        this.stmt = stmt;
        //AquilonThings.LOGGER.info("[Upsert] New upsert statement ("+String.join(", ", keys)+"; "+String.join(", ", params)+"):\n"+sql);
    }

    public static UpsertStatement build(Connection conn, String tableName, String[] keys, String... params) throws SQLException {
        String sql = buildSQL(tableName, keys, params);
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        return new UpsertStatement(keys, params, sql, stmt);
    }

    public static String buildSQL(String tableName, String[] keys, String... params) {
        if (keys.length < 1) throw new IllegalArgumentException("At least 1 key required");
        if (params.length < 1) throw new IllegalArgumentException("At least 1 parameter required");
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (").append(String.join(", ", keys)).append(", ");
        sql.append(String.join(", ", params)).append(") VALUES (");
        for (int i = 0; i < keys.length + params.length - 1; i++) {
            sql.append("?, ");
        }
        sql.append("?) ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < params.length - 1; i++) {
            sql.append(params[i]).append(" = ?, ");
        }
        sql.append(params[params.length - 1]).append(" = ?");
        return sql.toString();
    }

    public <T> UpsertStatement setParameter(String name, T value) throws SQLException {
        return setParameter(name, value, stmt::setObject);
    }

    public <T> UpsertStatement setParameter(String name, T value, StatementParameterSetter<T> parameterSetter) throws SQLException {
        int index = keys.indexOf(name);
        boolean isKey = true;
        if (index<0) {
            index = params.indexOf(name);
            if (index<0) throw new SQLException("No such parameter: "+name);
            isKey = false;
        }
        //AquilonThings.LOGGER.info("[Upsert] Set parameter "+(isKey ? '#' : '@')+name+": "+value);
        parameterSetter.accept(index+1+(isKey ? 0 : keys.size()), value);
        if (!isKey) // Only update if this not a key
            parameterSetter.accept(keys.size()+params.size()+index+1, value);
        return this;
    }

    /**
     * @see PreparedStatement#addBatch()
     * @throws SQLException on error
     */
    public void addBatch() throws SQLException {
        stmt.addBatch();
    }

    /**
     * @see PreparedStatement#executeBatch()
     * @throws SQLException on error
     * @return see {@link PreparedStatement#executeBatch()}
     */
    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    /**
     * @see PreparedStatement#clearParameters()
     * @throws SQLException on error
     */
    public void clearParameters() throws SQLException {
        stmt.clearParameters();
    }

    /**
     * @see PreparedStatement#executeUpdate()
     * @throws SQLException on error
     * @return see {@link PreparedStatement#executeUpdate()}
     */
    public int executeUpdate() throws SQLException {
        return stmt.executeUpdate();
    }

    /**
     * @see PreparedStatement#getGeneratedKeys()
     * @throws SQLException on error
     * @return see {@link PreparedStatement#getGeneratedKeys()}
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    /**
     * @see PreparedStatement#cancel()
     * @throws SQLException on error
     */
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    /**
     * @see PreparedStatement#close()
     * @throws SQLException on error
     */
    public void close() throws SQLException {
        stmt.close();
    }

    @FunctionalInterface
    public interface StatementParameterSetter<T> {
        void accept(int index, T value) throws SQLException;
    }
}
