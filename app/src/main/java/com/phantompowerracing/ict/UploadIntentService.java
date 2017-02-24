package com.phantompowerracing.ict;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.net.URLConnection;

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

            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder req = new NetworkRequest.Builder();
            req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            cm.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {

                        @Override
                        public void onAvailable(Network network) {
                            Log.d("UploadIntentService", "onAvailable");
                            // If you want to use a raw socket...
                            //network.bindSocket(...);
                            // Or if you want a managed URL connection...
                            //URLConnection conn = network.openConnection(...);
                            ConnectivityManager.setProcessDefaultNetwork(network);
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
                        }
                        // Be sure to override other options in NetworkCallback() too...

                    });

    }

}
