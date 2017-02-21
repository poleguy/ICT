package com.phantompowerracing.ict;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.phantompowerracing.ict.UploadIntentService.startActionUpload;

/**
 * Created by poleguy on 2/6/2017.
 * http://stackoverflow.com/questions/1756296/android-writing-logs-to-text-file
 */
public class IctLog {
    String filename;
    IctLog(String newFilename) {
        filename = newFilename;
    }
    public void append(String text)
    {
        File logFile = new File(filename);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void clear()
    {
        File logFile = new File(filename);
        if (logFile.exists())
        {
            logFile.delete();
        }
    }

    /**
     *
     * @return yyyy-MM-dd HH:mm:ss.SSS000 format date as string
     */
    public static String timestamp(){
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
            String currentDateTime = dateFormat.format(new Date()); // Find todays date

            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public void upload(Context context, String[] filenames) {
        // upload the log to dropbox
        //DropboxUpload db = new DropboxUpload();
        //db.execute("");

//        class UploadAsyncTask extends AsyncTask<String, Void, String> {
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                Log.d("IctLog.upload","onPreExecute");
//            }
//
//            @Override
//            protected String doInBackground(String... filenames) {
//                // do above Server call here
//                try {
//                    Log.d("IctLog.upload","about to upload");
//                    GMailSender sender = new GMailSender("nick.dietz@gmail.com", "irrin.vallo");
//                    Log.d("IctLog.upload","got sender");
//                    sender.sendMail("ICT Log",
//                            "ICT Log",
//                            "nick.dietz@gmail.com",
//                            "nick.dietz@gmail.com",
//                            filenames
//                            );
//                    Log.d("IctLog.upload","sendMail complete");
//                } catch (Exception e) {
//                    Log.e("SendMail", e.getMessage(), e);
//                }
//
//                return "mail sent";
//            }
//
//            @Override
//            protected void onPostExecute(String message) {
//                //nothing to do
//                Log.d("IctLog.onPostExecute", message);
//            }
//        }
//        Log.d("IctLog.upload","creating task");
//        UploadAsyncTask job = new UploadAsyncTask();
//        Log.d("IctLog.upload","executing task");
//        job.execute(filenames[0],filenames[1]);
//        //job.execute();
//        Log.d("IctLog.upload","task complete");
        //Intent  uploadIntent = new Intent(this, UploadIntentService.class);
        //IntentService uploadIntentService = new UploadIntentService();
        //uploadIntentService.
        startActionUpload(context, filenames);

    }


}
