package org.joe.reem.president.vice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;

/**
 * 
 * @author iankenny
 * The org.joe.reem.president.vice.JabberDatabase class. Formerly known as 'JabberServer'.
 */
public class JabberDatabase {

	private static final String dbcommand = "jdbc:postgresql://127.0.0.1:5432/postgres";
	private static final String db = "postgres";
	private static final String pw = "";

	private Connection conn;
	
	/**
	 * Returns the current database connection.
	 * @return the database connection.
	 */
	public Connection getConnection() { return conn; }

	/**
	 * This method returns a list of the userids of users following the user with the userid in the parameter.
	 * @param userid the userid of the user.
	 * @return a list of the userids of users following the user.
	 */
	public ArrayList<String> getFollowerUserIDs(int userid)
	{
		ArrayList<String> ret = new ArrayList<>();
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select userida from follows where useridb = ?");
			
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) { ret.add(rs.getObject("userida").toString()); }
			
		} catch (SQLException e) { e.printStackTrace(); }
		
		return ret;
	}

	/**
	 * This method returns a list of the userids of users being followed by the user in the parameter.
	 * @param userid The userid of the user.
	 * @return a list of the userids of users being followed by the user.
	 */
	public ArrayList<String> getFollowingUserIDs(int userid)
	{
		ArrayList<String> ret = new ArrayList<>();
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select useridb from follows where userida = ?");
			
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) { ret.add(rs.getObject("useridb").toString()); }

		} catch (SQLException e) { e.printStackTrace(); }
		
		return ret;
	}
	
	/**
	 * This method returns a list of pairs of users who follow each other. Each pair is only listed once.
	 * @return a list of 'mutual' follows, i.e. users who follow each other.
	 */
	public ArrayList<ArrayList<String>> getMutualFollowUserIDs()
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select f1.userida, f1.useridb from follows f1 inner join follows f2 on f1.userida = f2.useridb and f1.useridb = f2.userida");

			ResultSet rs = stmt.executeQuery();
			
			while (rs.next())
			{
				ArrayList<String> r = new ArrayList<>();
				r.add(rs.getObject("userida").toString()); 
				r.add(rs.getObject("useridb").toString()); 
				ret.add(r);
			}
		} catch (SQLException e) { e.printStackTrace(); }
		
		return removeDuplicates(ret);		
	}

	/**
	 * This method returns a list of the usernames of users not followed by the user with the userid in the parameter.
	 * @param userid the userid of the user.
	 * @return a list of the usernames of users not followed by the user.
	 */
	public ArrayList<ArrayList<String>> getUsersNotFollowed(int userid)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<>();

		try
		{
			PreparedStatement stmt = conn.prepareStatement("select username from jabberuser where userid not in (select useridb from follows where userida = ?)");
			
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();
	
			while (rs.next())
			{
				ArrayList<String> r = new ArrayList<>();
				r.add(rs.getObject("username").toString());
				ret.add(r);
			}
		} catch (SQLException e) { e.printStackTrace(); }
		
		return ret;
	}

	/**
	 * This method returns list of [username, jabtext] pairs of jabs liked by the user.
	 * @param userid the userid of the user.
	 * @return a list of [username, jabtext] pairs of the jabs liked by the user.
	 */
	public ArrayList<ArrayList<String>> getLikesOfUser(int userid)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<>();

		try
		{
			PreparedStatement stmt = conn.prepareStatement("select username, jabtext from jab natural join jabberuser where jabid in (select jabid from likes where userid = ?)");
		
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next())
			{
				ArrayList<String> r = new ArrayList<>();
				r.add(rs.getObject("username").toString()); 
				r.add(rs.getObject("jabtext").toString()); 
				ret.add(r);
			}
		} catch (SQLException e) { e.printStackTrace(); }
				
		return ret;
	}

	/**
	 * This method returns the timeline of the user as [username, jabtext] pairs.
	 * @param username the username of the user.
	 * @return the timeline of the user.
	 */
	@Deprecated
	public ArrayList<ArrayList<String>> getTimelineOfUser(String username)
	{
		int userid = this.getUserID(username);
		
		if (userid >= 0) return getTimelineOfUser(userid);

		return null;
	}

	/**
	 * This method returns the timeline of the user. Each jab in the returned list has the following
	 * data: [username, jabtext, jabid, number-of-likes].
	 * @param username the username of the user.
	 * @return the timeline of the user.
	 */
	public ArrayList<ArrayList<String>> getTimelineOfUserEx(String username)
	{
		int userid = this.getUserID(username);
		
		if (userid >= 0) return getTimelineOfUserEx(userid);
		
		return null;
	}

	/**
	 * This method returns the timeline of the user as [username, jabtext] pairs.
	 * @param userid the userid of the user.
	 * @return the timeline of the user.
	 */
	@Deprecated
	public ArrayList<ArrayList<String>> getTimelineOfUser(int userid)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<>();

		try
		{
			PreparedStatement stmt = conn.prepareStatement("select username, jabtext from jab natural join jabberuser where userid in (select useridb from follows where userida = ?)");
		
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next())
			{
				ArrayList<String> r = new ArrayList<>();
				r.add(rs.getObject("username").toString()); 
				r.add(rs.getObject("jabtext").toString()); 
				ret.add(r);
			}
		} catch (SQLException e) { e.printStackTrace(); }
						
		return ret;
	}

	/**
	 * This method returns the timeline of the user. Each jab in the returned list has the following
	 * data: [username, jabtext, jabid, number-of-likes].
	 * @param userid the userid of the user.
	 * @return the timeline of the user.
	 */
	public ArrayList<ArrayList<String>> getTimelineOfUserEx(int userid)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<>();

		try
		{
			PreparedStatement stmt = conn.prepareStatement("select username, jabtext, jabid, (select count(jabid) from likes A where A.jabid = B.jabid group by A.jabid) as jabcount from jab B natural join jabberuser where userid in (select useridb from follows where userida = ?)");
		
			stmt.setInt(1, userid);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next())
			{
				ArrayList<String> r = new ArrayList<>();
				r.add(rs.getObject("username").toString()); 
				r.add(rs.getObject("jabtext").toString()); 
				r.add(rs.getObject("jabid").toString());

				if (rs.getObject("jabcount") != null) r.add(rs.getObject("jabcount").toString());
				else r.add("0");

				ret.add(r);
			}
		} catch (SQLException e) { e.printStackTrace(); }
						
		return ret;
	}

	/**
	 * This method adds a jab from the user username.
	 * @param username the username of the user.
	 * @param jabtext the text of the jab.
	 */
	public void addJab(String username, String jabtext)
	{
		int userid = getUserID(username);
		
		int jabid = getNextJabID();
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("insert into jab (values (?,?,?));");
			
			stmt.setInt(1,  jabid);
			stmt.setInt(2,  userid);
			stmt.setString(3,  jabtext);
			
			stmt.executeUpdate();
			
		} catch (SQLException e) { e.printStackTrace(); }
	}
	
	/**
	 * This method adds a new user to the platform.
	 * @param username the username of the new user.
	 * @param emailadd the email address of the new user.
	 */
	public void addUser(String username, String emailadd)
	{
		int newid = getNextUserID();
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("insert into jabberuser (values(?,?,?))");
			
			stmt.setInt(1, newid);
			stmt.setString(2, username);
			stmt.setString(3, emailadd);
			
			stmt.executeUpdate();
			
			addFollower(newid, newid);
			
		} catch (SQLException e) { e.printStackTrace(); }
	}

	/**
	 * This method adds a new follows relationship between users: the user userida follows the user useridb.
	 * @param userida the user who is doing the following.
	 * @param useridb the user being followed.
	 */
	public void addFollower(int userida, int useridb)
	{
		try
		{
			PreparedStatement stmt = conn.prepareStatement("insert into follows (values(?,?))");
			
			stmt.setInt(1, userida);
			stmt.setInt(2, useridb);
			
			stmt.executeUpdate();
			
		} catch (SQLException e) { e.printStackTrace(); }
	}

	/**
	 * This method adds a new follows relationship between users: userida follows user username.
	 * @param userida the user who is doing the following.
	 * @param username the username of the user being followed.
	 */
	public void addFollower(int userida, String username)
	{
		int useridb = this.getUserID(username);
		
		if (useridb < 0) return;

		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("insert into follows (values(?,?))");
			
			stmt.setInt(1, userida);
			stmt.setInt(2, useridb);
			
			stmt.executeUpdate();
			
		} catch (SQLException e) { e.printStackTrace(); }
	}

	/**
	 * This method adds a new like: the user with user id userid likes the jab with jab id jabid.
	 * @param userid the user.
	 * @param jabid the jab.
	 */
	public void addLike(int userid, int jabid)
	{
		try
		{
			PreparedStatement stmt = conn.prepareStatement("insert into likes (values(?,?))");
			
			stmt.setInt(1, userid);
			stmt.setInt(2, jabid);
			
			stmt.executeUpdate();
			
		} catch (SQLException e) { e.printStackTrace(); }
	}
	
	/**
	 * This method returns a list of users with the most followers.
	 * @return a list of the users with the most followers.
	 */
	public ArrayList<String> getUsersWithMostFollowers()
	{
		ArrayList<String> ret = new ArrayList<>();
		
		String query = "select useridb from follows group by useridb having count (useridb) >= all (select count(useridb) from follows group by useridb order by count(useridb));";
	
		try
		{
			PreparedStatement pstmt = conn.prepareStatement(query);
			
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) { ret.add(rs.getObject("useridb").toString()); }
			
		} catch (SQLException e) { e.printStackTrace(); }
		
		return ret;
	}
	
	private int getNextJabID()
	{
		int maxid = -1;
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select max(jabid) from jab");
		
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) { maxid = rs.getInt(1); } // only one result.

		} catch (SQLException e) { e.printStackTrace(); }
		
		if (maxid < 0) return maxid;
		
		return maxid + 1;
	}

	/**
	 * A convenience method to get the userid of the user with user name username.
	 * @param username the username of the user.
	 * @return the userid of the user.
	 */
	public int getUserID(String username)
	{
		int ret = -1;
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select userid from jabberuser where username = ?");
			stmt.setString(1, username);
			
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) { ret = rs.getInt(1); } // only one result anyway.

		} catch (SQLException e) { e.printStackTrace(); }

		return ret;
	
	}


	private int getNextUserID()
	{
		
		String query = "select max(userid) from jabberuser";
		
		int maxid = -1;
		
		try (PreparedStatement stmt = conn.prepareStatement(query))
		{
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) { maxid = rs.getInt(1); }

		} catch (SQLException e) { e.printStackTrace(); }
		
		if (maxid < 0) return maxid;
		
		return maxid + 1;
	}
	
	private ArrayList<ArrayList<String>> removeDuplicates(ArrayList<ArrayList<String>> list)
	{
		
		for (ArrayList<String> l: list) { Collections.sort(l); }
		
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		
		ret.add(list.get(0));
		
		for (ArrayList<String> l: list)
		{
			if (!ret.contains(l)) ret.add(l);
		}
		
		return ret;
	}

	/**
	 * Default constructor. Connects to the database.
	 */
	public JabberDatabase() { connectToDatabase(); } //resetDatabase();

	
	/**
	 * Connects to the database. No need to call this as it is called by the default constuctor.
	 */
	public void connectToDatabase()
	{
		try { conn = DriverManager.getConnection(dbcommand,db,pw); }

		catch(Exception e) { e.printStackTrace(); }
	}

	/*
	 * Utility method to print an ArrayList of ArrayList<String>s to the console.
	 */
	private static void print2(ArrayList<ArrayList<String>> list) {
		
		for (ArrayList<String> s: list)
		{
			print1(s);
			System.out.println();
		}
	}
		
	/*
	 * Utility method to print an ArrayList to the console.
	 */
	private static void print1(ArrayList<String> list)
	{
		for (String s: list) { System.out.print(s + " "); }
	}

	/**
	 * Resets the database to the original data set. This method requires that the files
	 * jabberdef.sql and jabberdata.sql are in the working folder of the org.joe.reem.president.vice.JabberDatabase project
	 * (e.g. the root directory of the project). You don't have to use this method. It exists 
	 * in case you need to sort out the database if it has been messed up.
	 */
	public void resetDatabase()
	{
		dropTables();
		
		ArrayList<String> defs = loadSQL("jabberdef");
	
		ArrayList<String> data =  loadSQL("jabberdata");
		
		executeSQLUpdates(defs);
		executeSQLUpdates(data);
	}
	
	private void executeSQLUpdates(ArrayList<String> commands)
	{
		for (String query: commands)
		{
			try (PreparedStatement stmt = conn.prepareStatement(query)) { stmt.executeUpdate(); }

			catch (SQLException e) { e.printStackTrace(); }
		}
	}

	/*
	 * This method is to be used only by the resetDatabase() code.
	 * Do not use it yourself to load your own SQL.
	 */
	private ArrayList<String> loadSQL(String sqlfile)
	{
		ArrayList<String> commands = new ArrayList<>();
		
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(sqlfile + ".sql"));
			
			StringBuilder command = new StringBuilder();
			
			String line;
			
			while ((line = reader.readLine())!= null)
			{
				if (line.contains(";"))
				{
					command.append(line);
					command = new StringBuilder(command.toString().trim());
					commands.add(command.toString());
					command = new StringBuilder();
				}
				
				else
				{
					line = line.trim();
					command.append(line).append(" ");
				}
			}
			
			reader.close();
			
		} catch (IOException e) { e.printStackTrace(); }
		
		return commands;
	}

	private void dropTables()
	{
		String[] commands = {
				"drop table jabberuser cascade;",
				"drop table jab cascade;",
				"drop table follows cascade;",
				"drop table likes cascade;"};
		
		for (String query: commands)
		{
			try (PreparedStatement stmt = conn.prepareStatement(query)) { stmt.executeUpdate(); }

			catch (SQLException e) { e.printStackTrace(); }
		}
	}
}
