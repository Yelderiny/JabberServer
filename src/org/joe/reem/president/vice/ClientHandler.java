package org.joe.reem.president.vice;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler extends Thread
{
    private final JabberDatabase db;
    private final Socket clientSocket;
    private String user;
    private int userID;

    private ObjectOutputStream forClient;

    //accessors
    public String getUser() { return user; }
    public int getUserID() { return userID; }

    //mutators
    public void setUser(final String username) { user = username; }
    public void setUserID(final int ID) { userID = ID; }

    //constructor
    public ClientHandler(final Socket client, final JabberDatabase dbs)
    {
        clientSocket = client;
        db = dbs;
    }

    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                ObjectInputStream fromClient = new ObjectInputStream(clientSocket.getInputStream());
                forClient = new ObjectOutputStream(clientSocket.getOutputStream());

                JabberMessage msg = (JabberMessage) fromClient.readObject();

                String ClientMsg = msg.getMessage();

                String[] message = new String[0];
                final String command;
                final String info;

                //message is longer than one word
                if (ClientMsg.contains(" "))
                {
                    message = ClientMsg.split(" "); //split the client's massage into strings
                    command = message[0]; //save the command
                    info = message[1]; //save the information provided
                }
                else
                {
                    command = ClientMsg;
                    info = null;
                }

                //separate the command 'post' from the jab text
                switch (command)
                {
                    case "signin" -> SignIn(info);
                    case "register" -> RegisterUser(info);
                    case "signout" -> SignOut();
                    case "timeline" -> Timeline();
                    case "users" -> UsersToFollow();
                    case "post" ->
                    {
                        StringBuilder jabText = new StringBuilder(); //stringBuilder to store the jab text

                        for (int i = 1; i < message.length; i++) jabText.append(message[i]).append(" ");  //separate the command 'post' from the jab text and add spaces

                        PostJab(jabText.toString());
                    }
                    case "like" -> LikeJab(info);
                    case "follow" -> FollowUser(info);
                }
            }
        }
        catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
        finally
        {
            try { clientSocket.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    /**
     * Adds a follow relationship to the database, where the follower is the logged in user
     * @param username the user that will be followed
     */
    private void FollowUser(final String username) throws IOException
    {
        db.addFollower(getUserID(), username); //add the follow relationship to the data base

        forClient.writeObject(new JabberMessage("posted"));
        forClient.flush();
    }

    /**
     * Adds a like on the input jab by the logged in user
     * @param jabIDasString the jab ID which is to be liked given as a string rather than an int
     */
    private void LikeJab(final String jabIDasString) throws IOException
    {
        final int jabIDasInt = Integer.parseInt(jabIDasString); //change input ID from string to int

        db.addLike(getUserID(), jabIDasInt); //add like to the database
        forClient.writeObject(new JabberMessage("posted"));
        forClient.flush();

    }

    /**
     * Adds a jab to the database
     * @param jabtext the text of the jab to be added
     */
    private void PostJab(final String jabtext) throws IOException
    {
        db.addJab(getUser(), jabtext); //add jab to the database

        forClient.writeObject(new JabberMessage("posted"));
        forClient.flush();
    }

    /**
     * Provides an arraylist of users not followed by the logged in user
     */
    private void UsersToFollow() throws IOException
    {
        ArrayList<ArrayList<String>> usersNotFollowed = db.getUsersNotFollowed(getUserID()); //get the list of users not followed
        forClient.writeObject(new JabberMessage("users", usersNotFollowed));
        forClient.flush();
    }

    /**
     * Provides the timeline of the logged in user in String form in this order: [username, jabtext, jabid, number-of-likes]
     */
    private void Timeline() throws IOException
    {
        ArrayList<ArrayList<String>> timeline = db.getTimelineOfUserEx(getUser()); //get the timeline from the database
        forClient.writeObject(new JabberMessage("timeline", timeline));
        forClient.flush();
    }

    /**
     * not sure what so ever what this does
     */
    private void SignOut() throws IOException
    {
        forClient.writeObject(new JabberMessage("signedout"));
        forClient.flush();
    }

    /**
     * Adds the user's username to the database and logs them in. The log in takes the form of initializing the global variables user and userID
     * @param username the username to be added to the database
     */
    private void RegisterUser(final String username) throws IOException
    {
        //add user to the database
        String email = username + "@gmail.com"; //create user email
        db.addUser(username, email); //add user to the database

        //initialize global variables
        setUser(username);
        setUserID(db.getUserID(username));

        forClient.writeObject(new JabberMessage("signedin"));
        forClient.flush();
    }

    /**
     * Logs the user in. The log in takes the form of initializing the global variables user and userID. If the username is invalid, the server doesn't log the client in
     * @param username the username to be checked in the database
     */
    private void SignIn(final String username) throws IOException
    {
        final int ID = db.getUserID(username); //get the userid from the database

        if (ID == -1) forClient.writeObject(new JabberMessage("unknown-user"));
        else
        {
            //initialize global variables
            setUser(username);
            setUserID(ID);

            forClient.writeObject(new JabberMessage("signedin"));
        }
        forClient.flush();
    }
}
