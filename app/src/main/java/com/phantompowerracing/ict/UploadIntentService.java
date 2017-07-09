package com.phantompowerracing.ict;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.text.TextUtils;
import android.util.Log;

import com.dropbox.core.util.StringUtil;

import java.net.InetAddress;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class UploadIntentService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPLOAD = "com.phantompowerracing.ict.action.UPLOAD";

    // TODO: Rename parameters
    private static final String EXTRA_FILENAMES = "com.phantompowerracing.ict.extra.FILENAMES";
    private static final String EXTRA_PARAM2 = "com.phantompowerracing.ict.extra.PARAM2";
    private static Context mContext;
    private static boolean mUploadPending = false;
    public UploadIntentService() {
        super("UploadIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionUpload(Context context, String[] filenames) {
        Intent intent = new Intent(context, UploadIntentService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_FILENAMES, filenames);
        mContext = context;
        mUploadPending = true;
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                final String[] filenames = intent.getStringArrayExtra(EXTRA_FILENAMES);
                handleActionUpload(filenames);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpload(String[] filenames) {
        final String[] mFilenames = filenames;
        //throw new UnsupportedOperationException("Not yet implemented");
        // do above Server call here

            // http://stackoverflow.com/questions/29835240/how-to-stay-connected-through-mobile-network-after-wifi-is-connected-on-android?noredirect=1&lq=1
            // http://stackoverflow.com/questions/25931334/send-request-over-mobile-data-when-wifi-is-on-android-l

            //ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
            //        Context.CONNECTIVITY_SERVICE);
            //NetworkRequest.Builder req = new NetworkRequest.Builder();
            //req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            //req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            //cm.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {

//                        @Override
//                        public void onAvailable(Network network) {
//                            Log.d("UploadIntentService", "onAvailable");
                            // If you want to use a raw socket...
                            //network.bindSocket(...);
                            // Or if you want a managed URL connection...
                            //URLConnection conn = network.openConnection(...);
//                            ConnectivityManager.setProcessDefaultNetwork(network);
                            try {
                                if (mUploadPending == true) {
                                    mUploadPending = false;
                                    Log.d("IctLog.upload", "about to upload");
                                    GMailSender sender = new GMailSender("nick.dietz@gmail.com", "irrin.vallo");
                                    Log.d("IctLog.upload", "got sender");
                                    sender.sendMail("ICT Log",
                                            "ICT Log",
                                            "nick.dietz@gmail.com",
                                            "nick.dietz@gmail.com",
                                            mFilenames
                                    );
                                    Log.d("IctLog.upload", "sendMail complete");
                                }
                            } catch (Exception e) {
                                Log.e("SendMail", e.getMessage(), e);
                            }
//                        }
                        // Be sure to override other options in NetworkCallback() too...

//                    });

    }

    // http://stackoverflow.com/questions/2513713/how-to-use-3g-connection-in-android-application-instead-of-wi-fi/4756630#4756630
    /**
     * Enable mobile connection for a specific address
     * @param context a Context (application or activity)
     * @param address the address to enable
     * @return true for success, else false
     */
    String TAG_LOG = "forceMobile";
    private boolean forceMobileConnectionForAddress(Context context, String address) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == connectivityManager) {
            Log.d(TAG_LOG, "ConnectivityManager is null, cannot try to force a mobile connection");
            return false;
        }

        //check if mobile connection is available and connected
        NetworkInfo.State state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
        Log.d(TAG_LOG, "TYPE_MOBILE_HIPRI network state: " + state);
        if (0 == state.compareTo(NetworkInfo.State.CONNECTED) || 0 == state.compareTo(NetworkInfo.State.CONNECTING)) {
            return true;
        }

        //activate mobile connection in addition to other connection already activated
        int resultInt = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
        Log.d(TAG_LOG, "startUsingNetworkFeature for enableHIPRI result: " + resultInt);

        //-1 means errors
        // 0 means already enabled
        // 1 means enabled
        // other values can be returned, because this method is vendor specific
        if (-1 == resultInt) {
            Log.e(TAG_LOG, "Wrong result of startUsingNetworkFeature, maybe problems");
            return false;
        }
        if (0 == resultInt) {
            Log.d(TAG_LOG, "No need to perform additional network settings");
            return true;
        }

        //find the host name to route
        String hostName = extractAddressFromUrl(address);

        Log.d(TAG_LOG, "Source address: " + address);
        //Log.d(TAG_LOG, "Destination host address to route: " + hostName);
        if (TextUtils.isEmpty(hostName)) hostName = address;

        //create a route for the specified address
        int hostAddress = lookupHost(hostName);
        if (-1 == hostAddress) {
            Log.e(TAG_LOG, "Wrong host address transformation, result was -1");
            return false;
        }
        //wait some time needed to connection manager for waking up
        try {
            for (int counter=0; counter<30; counter++) {
                NetworkInfo.State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
                if (0 == checkState.compareTo(NetworkInfo.State.CONNECTED))
                    break;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            //nothing to do
        }
        boolean resultBool = connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
        Log.d(TAG_LOG, "requestRouteToHost result: " + resultBool);
        if (!resultBool)
            Log.e(TAG_LOG, "Wrong requestRouteToHost result: expected true, but was false");

        return resultBool;
    }

    /**
     * This method extracts from address the hostname
     * @param url eg. http://some.where.com:8080/sync
     * @return some.where.com
     */
    public static String extractAddressFromUrl(String url) {
        String urlToProcess = null;

        //find protocol
        int protocolEndIndex = url.indexOf("://");
        if(protocolEndIndex>0) {
            urlToProcess = url.substring(protocolEndIndex + 3);
        } else {
            urlToProcess = url;
        }

        // If we have port number in the address we strip everything
        // after the port number
        int pos = urlToProcess.indexOf(':');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have resource location in the address then we strip
        // everything after the '/'
        pos = urlToProcess.indexOf('/');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have ? in the address then we strip
        // everything after the '?'
        pos = urlToProcess.indexOf('?');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }
        return urlToProcess;
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
