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
    private ObjectInputStream fromClient;

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
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                forClient = new ObjectOutputStream(clientSocket.getOutputStream());

                JabberMessage msg = (JabberMessage) fromClient.readObject();

                String ClientMsg = msg.getMessage();

                var message = new String[0];
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
                    case "signin" -> SignIn(info, message[2]);
                    case "register" -> RegisterUser(info, message[2], message[3]);
                    case "signout" -> SignOut();
                    case "timeline" -> Timeline();
                    case "users" -> UsersToFollow();
                    case "post" ->
                    {
                        var jabText = new StringBuilder(); //stringBuilder to store the jab text

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
            try
            {
                clientSocket.close();
                forClient.close(); //without this, EOF exception issues arise
                fromClient.close(); //without this, EOF exception issues arise
            }
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

        ArrayList<ArrayList<String>> likes = db.getLikesOfUser(userID);

        //check if the input jab has already been liked by the user
        boolean alreadyLiked = false;
        for (ArrayList<String> arr : likes)
        {
            if (arr.get(2).equals(jabIDasString))
            {
                alreadyLiked = true;
                break;
            }
        }

        if (alreadyLiked)
        {
            db.removeLike(userID, jabIDasInt); //remove like from the database
            forClient.writeObject(new JabberMessage("unposted")); //jab has already been liked
        }
        else
        {
            db.addLike(getUserID(), jabIDasInt); //add like to the database
            forClient.writeObject(new JabberMessage("posted"));
        }
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
     * Confirms user sign-out
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
    private void RegisterUser(final String username, final String eMail, final String password) throws IOException
    {
        if (db.getUsernames().contains(username)) forClient.writeObject(new JabberMessage("already-registered")); //username already exists
        else if (db.geteMails().contains(eMail)) forClient.writeObject(new JabberMessage("eMail-already-in-use")); //email already exists

        //new user
        else
        {
            db.addUser(username, eMail, password); //add user to the database
            forClient.writeObject(new JabberMessage("registered"));
        }
        forClient.flush();
    }

    /**
     * Logs the user in. The log in takes the form of initializing the global variables user and userID. If the username is invalid, the server doesn't log the client in
     * @param username the username to be checked in the database
     */
    private void SignIn(final String username, final String password) throws IOException
    {
        final int ID = db.getUserID(username); //get the userid from the database
        final String pass = db.getUserPassword(username); //get the password from the database

        if (ID == -1) forClient.writeObject(new JabberMessage("unknown-user"));
        else
        {
            if (!pass.trim().equals(password.trim())) forClient.writeObject(new JabberMessage("incorrect-pass"));
            else
            {
                //initialize global variables
                setUser(username);
                setUserID(ID);

                forClient.writeObject(new JabberMessage("signedin"));
            }
        }
        forClient.flush();
    }
}
