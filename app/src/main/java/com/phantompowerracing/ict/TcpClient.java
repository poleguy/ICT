package com.phantompowerracing.ict;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.util.Log;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
    private Context context;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(String ip_address, int port, OnMessageReceived listener, Context newContext) {
        mServerIp = ip_address;
        mServerPort = port;
        mMessageListener = listener;
        context = newContext;
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




            // before connecting to car, let's set up a route

            //http://stackoverflow.com/questions/2513713/how-to-use-3g-connection-in-android-application-instead-of-wi-fi/4756630#4756630
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null == cm) {
                Log.d("ICT", "ConnectivityManager is null, cannot try to force a mobile connection");
            }
            NetworkRequest.Builder request = new NetworkRequest.Builder();
            //request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            //request.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            //.setNetworkSpecifier("00:1a:70:ee:69:b6"); // 244Wesley
            //.setNetworkSpecifier("001a70ee69b6"); // 244Wesley
            //.setNetworkSpecifier("244Wesley_708-848-3835"); // 244Wesley
            //.setNetworkSpecifier("1a:fe:34:dc:e4:ac"); // ESP_DCE4AC
            final NetworkRequest nr = request.build();

            cm.requestNetwork(nr, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {


                    //WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
                    //WifiInfo info = wifiManager.getConnectionInfo ();
                    //String ssid  = info.getSSID();
                    //Log.d("IctNetwork","onAvailable" + ssid);
                    Log.d("TcpClient","onAvailable");
                    //if (ssid == "ESP_DCE4AC") {
                    //    ConnectivityManager.setProcessDefaultNetwork(network);
                    //ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback();
                    //cm.requestNetwork(nr, networkCallback );
                    //}
                    ConnectivityManager.setProcessDefaultNetwork(network);
                    //http://stackoverflow.com/questions/2513713/how-to-use-3g-connection-in-android-application-instead-of-wi-fi/4756630#4756630
                    //InetAddress ip = null;
                    //try {
                    //    ip = InetAddress.getByName("192.168.4.1");
                    //} catch (UnknownHostException e) {
                    //    e.printStackTrace();
                    //}
                    //int address = ByteBuffer.wrap(ip.getAddress()).getInt();
                    //cm.requestRouteToHost(ConnectivityManager.TYPE_WIFI, address);
                    //cm.bindProcessToNetwork(network);
                    //ConnectivityManager.bindProcessToNetwork();
                }
                @Override
                public void onLosing(Network network, int ms) {
                    Log.d("IctNetwork","onLosing");
                }
                @Override
                public void onLost(Network network) {
                    Log.d("IctNetwork","onLost");
                }
            });


//
//
//            //check if mobile connection is available and connected
//            NetworkInfo.State state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
//            Log.d("ICT", "TYPE_MOBILE_HIPRI network state: " + state);
//            if (0 == state.compareTo(NetworkInfo.State.CONNECTED) || 0 == state.compareTo(NetworkInfo.State.CONNECTING)) {
//            }
//
//            //activate mobile connection in addition to other connection already activated
//            //int resultInt = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_WIFI, "enableHIPRI");
//            //Log.d("ICT", "startUsingNetworkFeature for enableHIPRI result: " + resultInt);
//
//
//            //create a route for the specified address
//            int hostAddress = lookupHost(mServerIp);
//            if (-1 == hostAddress) {
//                Log.e("ICT", "Wrong host address transformation, result was -1");
//            }
//            //wait some time needed to connection manager for waking up
//            try {
//                for (int counter=0; counter<30; counter++) {
//                    NetworkInfo.State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
//                    if (0 == checkState.compareTo(NetworkInfo.State.CONNECTED))
//                        break;
//                    Thread.sleep(1000);
//                }
//            } catch (InterruptedException e) {
//                //nothing to do
//            }
//            boolean resultBool = connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_WIFI, hostAddress);
//            Log.d("ICT", "requestRouteToHost result: " + resultBool);
//            if (!resultBool)
//                Log.e("ICT", "Wrong requestRouteToHost result: expected true, but was false");
//
//            Network nw;
//
//            nw.getSocketFactory();


            boolean connected = false;
            //wait some time needed to connection manager for waking up
//            try {
//                for (int counter=0; counter<30; counter++) {
                    NetworkInfo.State checkState = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                    if (0 == checkState.compareTo(NetworkInfo.State.CONNECTED)) {
                        connected = true;
                        //break;
                    }
//                    Thread.sleep(1000);
//                }
//            } catch (InterruptedException e) {
//                //nothing to do
//            }

            if (connected == true) {
                //create a socket to make the connection with the server
                Socket socket = new Socket(serverAddr, mServerPort);

                try {

                    //sends the message to the server
                    mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    //receives the message which the server sends back
                    mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    // send login name
                    sendMessage(Constants.LOGIN_NAME + "Kazy");

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

    /**
     * Transform host name in int value used by {@link ConnectivityManager.requestRouteToHost}
     * method
     *
     * @param hostname
     * @return -1 if the host doesn't exists, elsewhere its translation
     * to an integer
     */
    private static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8 )
                |  (addrBytes[0] & 0xff);
        return addr;
    }

}