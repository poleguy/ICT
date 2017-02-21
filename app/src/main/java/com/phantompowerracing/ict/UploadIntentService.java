package com.phantompowerracing.ict;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

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

        //throw new UnsupportedOperationException("Not yet implemented");
        // do above Server call here
        try {
            Log.d("IctLog.upload","about to upload");
            GMailSender sender = new GMailSender("nick.dietz@gmail.com", "irrin.vallo");
            Log.d("IctLog.upload","got sender");
            sender.sendMail("ICT Log",
                    "ICT Log",
                    "nick.dietz@gmail.com",
                    "nick.dietz@gmail.com",
                    filenames
            );
            Log.d("IctLog.upload","sendMail complete");
        } catch (Exception e) {
            Log.e("SendMail", e.getMessage(), e);
        }
    }

}
