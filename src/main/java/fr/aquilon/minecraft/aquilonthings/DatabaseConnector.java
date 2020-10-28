package fr.aquilon.minecraft.aquilonthings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnector {
	public static final String LOG_PREFIX = AquilonThings.LOG_PREFIX+"[BDD] ";
	public static final boolean DEBUG = false;

	private String address;
	private int port;
	private String user;
	private String password;
	private String base;
	private boolean secure;
	private static final Logger LOG = AquilonThings.LOGGER;

	/**
	 * Ce constructeur est utilisé par le framework pour définir un model de connexion.
	 * @param address
	 * @param port
	 * @param user
	 * @param password
	 * @param base
	 */
	public DatabaseConnector(String address, int port, String user,
                             String password, String base, boolean secure) {
		this.address = address;
		this.port = port;
		this.user = user;
		this.password = password;
		this.base = base;
		this.secure = secure;
	}

	/**
	 * Connection method
	 */
	private Connection getConnection() {
	    if (DEBUG) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            int stackElemID = 1;
            StackTraceElement stackE = trace[stackElemID];
            while (stackE.getClassName().equals(this.getClass().getName())) {
                stackElemID++;
                stackE = trace[stackElemID];
            }
            String stackData = stackE.getClassName().substring(stackE.getClassName().lastIndexOf(".")+1)+"."+stackE.getMethodName()+":"+stackE.getLineNumber();
            LOG.info(LOG_PREFIX+"Connexion de la base de données. > "+stackData);
        }

	    List<String> options = new ArrayList<>();
	    options.add("useUnicode=true");
	    options.add("characterEncoding=utf-8");
	    if (!secure) options.add("useSSL=false");
		String url = "jdbc:mysql://" + address + ":" + port + "/" + base + "?" + String.join("&", options);
		try {
            return DriverManager.getConnection(url, user, password);
		} catch (SQLException ex) {
			LOG.severe(LOG_PREFIX+"Problème de connexion à la base de donnée !");
			LOG.severe(LOG_PREFIX+ displayError(ex.getSQLState()));
			LOG.log(Level.SEVERE, LOG_PREFIX+ex.getMessage(), ex);
		}
		return null;
	}

	private static String displayError(String sqls) {
		if (sqls==null) return "Code [null], État indeterminé.";
		if(sqls.contains("08S01"))
			return "Code ["+sqls+"] Le serveur MySQL ne répond pas.";
		if(sqls.contains("28000"))
			return "Code ["+sqls+"] Identification impossible (user/password).";
		if(sqls.contains("42000"))
			return "Code ["+sqls+"] Accès refusé a la base de donnée.";
		if(sqls.contains("08004"))
			return "Code ["+sqls+"] Nombre de connexion maximum depassé.";
		
		return "Code ["+sqls+"] Code d'erreur inconnu, indications sur le manuel de reference de MySQL.";
	}

	public boolean validateConnection() {
		try {
			LOG.info(LOG_PREFIX+"Connexion BDD: "+getUser()+"@"+getAddress()+":"+getPort());
			Connection con = getConnection();
			ResultSet rs = query(con, "SELECT version()");
			if (rs.next()) {
				LOG.info(LOG_PREFIX+"Connexion reussie : MySQL "+rs.getString(1));
			}
			con.close();
			return true;
	
		} catch (Exception e) {
			return false;
			//LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * Permet d'envoyer une requête SQL à la base.
	 * Cette requête DOIT retourner des données.
	 *
	 * @param sql
	 * @return ResultSet
	 */
	public ResultSet query(Connection con, String sql) {
		if (con==null) {
			LOG.warning(LOG_PREFIX+"Erreur dans l'execution de la requete SQL: "+sql+"\nBDD non connectée !");
			return null;
		}
		try {
			Statement st = con.createStatement();
			return st.executeQuery(sql);
		} catch (SQLException ex) {
			logException(ex,sql);
			return null;
		}
	}


	/**
	 * Permet de preparer une requête SQL.
	 * @param sql SQL request
	 * @param options SQL request options
	 * @return Statement
	 */
	public PreparedStatement prepare(Connection con, String sql, int options) {
		if (con==null) {
			LOG.warning(LOG_PREFIX+"Erreur dans l'execution de la requete SQL: "+sql+"\nBDD non connectée !");
			return null;
		}
		try {
			return con.prepareStatement(sql, options);
		} catch (SQLException ex) {
			logException(ex,sql);
			return null;
		}
	}
	public PreparedStatement prepare(Connection con, String sql) {
		return prepare(con, sql, 0);
	}

	/**
	 * Permet d'envoyer une requête SQL à la base.
	 * Cette requête ne DOIT PAS retourner de donnée.
	 * @param sql
	 */
	public boolean updateQuery(Connection con, String sql){
        if (con==null) {
            LOG.warning(LOG_PREFIX+"Erreur dans l'execution de la requete SQL: "+sql+"\nBDD non connectée !");
            return false;
        }
		try {
            Statement st = con.createStatement();
            st.executeUpdate(sql);
		} catch (SQLException ex) {
			logException(ex,sql);
			return false;
		}
		return true;
	}

	public static void logException(SQLException ex) {
		logException(ex, null);
	}
	public static void logException(SQLException ex, String sql) {
		String pre = LOG_PREFIX;
		if (sql!=null) pre += "Erreur dans l'execution de la requete SQL: "+sql+"\n\t";
		else pre += "Erreur : ";
		LOG.warning(pre+ displayError(ex.getSQLState()));
		LOG.log(Level.WARNING, LOG_PREFIX+ex.getMessage(), ex);
	}

	public Connection startTransaction() {
		return startTransaction(true);
	}

	/**
	 * Connects to the database and set the according commit mode
	 * @param autoCommit Commit mode
	 * @return Whether the connexion was successfull or not
	 */
	public Connection startTransaction(boolean autoCommit) {
		Connection con;
        con = getConnection();
        try {
			con.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			logException(e);
            try {
                con.close();
            } catch (SQLException ignored) {}
            return null;
		}
		return con;
	}

	/**
	 * End transaction, print error, rollback if possible and disconnect
	 * @param ex Error
	 */
	public void endTransaction(Connection con, SQLException ex) {
		endTransaction(con, ex, null);
	}

	/**
	 * End transaction, print SQL error, rollback if possible and disconnect
	 * @param ex Error
	 */
	public void endTransaction(Connection con, SQLException ex, String sql) {
		logException(ex, sql);
		endTransaction(con, false);
	}

	/**
	 * End transaction, commits if necessary and disconnects
	 */
	public void endTransaction(Connection con) {
		endTransaction(con, true);
	}

	/**
	 * End transaction, commit or rollback and disconnect
	 * @param commit Commit or rollback
	 */
	public void endTransaction(Connection con, boolean commit) {
		try {
			if (!con.getAutoCommit()) {
				if (commit) con.commit();
				else con.rollback();
			}
		} catch (SQLException e) {
			logException(e);
		}
        try {
            con.close();
        } catch (SQLException ignored) {}
	}


	/*
	----- Getters et Setters -----
	*/
	
	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getBase() {
		return base;
	}
}
