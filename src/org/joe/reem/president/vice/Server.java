package org.joe.reem.president.vice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable
{
    private static ServerSocket server;
    private static JabberDatabase db;

    public static void main(String[] args) { new Server(); }

    public Server() { new Thread(this).start(); }

    public static boolean startServer()
    {
        try
        {
            server = new ServerSocket(44445); //initializing the ServerSocket
            server.setReuseAddress(true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void processClientRequests()
    {
        while (true)
        {
            Socket clientSocket = null;

            try { clientSocket = server.accept(); }
            catch (IOException e) { e.printStackTrace(); }

            ClientHandler clientH = new ClientHandler(clientSocket, db);
            clientH.start();
        }
    }

    @Override
    public void run()
    {
        if (startServer())
        {
            db = new JabberDatabase();
            db.resetDatabase(); //reset database
            processClientRequests();
        }
    }
}
