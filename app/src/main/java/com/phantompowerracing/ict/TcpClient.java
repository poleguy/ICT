package com.phantompowerracing.ict;

import android.util.Log;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Description
 *
 * @author Catalin Prata
 *         Date: 2/12/13
 */
public class TcpClient {

    //public static final String SERVER_IP = "192.168.1.81"; //target IP address
    public String mServerIp = "192.168.1.74";
    public int mServerPort = 23;
    //public static final int SERVER_PORT = 23; //9750
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(String ip_address, int port, OnMessageReceived listener) {
        mServerIp = ip_address;
        mServerPort = port;
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            mBufferOut.println(message);
            mBufferOut.flush();
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        // send mesage that we are closing the connection
        sendMessage(Constants.CLOSED_CONNECTION+"Kazy");

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(mServerIp);

            Log.e("TCP Client", String.format("C: Connecting... %s %d",mServerIp,mServerPort));

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, mServerPort);

            try {

                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // send login name
                sendMessage(Constants.LOGIN_NAME+"Kazy");

                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    try {
                        mServerMessage = mBufferIn.readLine();
                    } catch (SocketException e) {
                        // ignore
                        Log.e("TcpClient", "S: Handling Socket Closed in read");

                    }

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (ConnectException e) {
            // EHOSTUNREACH host unreachable(ConnectException e) {
            try {
                Thread.sleep(1000); // try again in a bit
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}