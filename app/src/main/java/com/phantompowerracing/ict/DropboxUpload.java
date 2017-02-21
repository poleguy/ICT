//package com.phantompowerracing.ict;
//
//import android.os.AsyncTask;
//
//import com.dropbox.core.DbxException;
//import com.dropbox.core.DbxRequestConfig;
//import com.dropbox.core.v2.DbxClientV2;
//import com.dropbox.core.v2.files.FileMetadata;
//import com.dropbox.core.v2.files.ListFolderResult;
//import com.dropbox.core.v2.files.Metadata;
//import com.dropbox.core.v2.users.FullAccount;
//
//import java.util.List;
//
//import java.io.FileInputStream;
//import java.io.InputStream;
//import java.io.IOException;
//
//public class DropboxUpload extends AsyncTask<String, Void, String> {
//    private static final String ACCESS_TOKEN = "gbtjBeVvrwAAAAAAAAAkKk30O8DEXYoy4dQbODqUWHuMgaf6XtGHAXq83SiDFcxb";
//    @Override
//    protected String doInBackground(String[] params) {
//        // do above Server call here
//        // upload the log to dropbox
//        try {
//            run();
//        } catch (DbxException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return "done";
//    }
//
//    @Override
//    protected void onPostExecute(String message) {
//        //process message
//    }
//    public static void run() throws DbxException, IOException {
//        // Create Dropbox client
//        //DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
//        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
//        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
//
//        // Get current account info
//        FullAccount account = client.users().getCurrentAccount();
//        System.out.println(account.getName().getDisplayName());
//
//        // Get files and folder metadata from Dropbox root directory
//        ListFolderResult result = client.files().listFolder("");
//        while (true) {
//            for (Metadata metadata : result.getEntries()) {
//                System.out.println(metadata.getPathLower());
//            }
//
//            if (!result.getHasMore()) {
//                break;
//            }
//
//            result = client.files().listFolderContinue(result.getCursor());
//        }
//
//        // Upload "test.txt" to Dropbox
//        try (InputStream in = new FileInputStream("test.txt")) {
//            FileMetadata metadata = client.files().uploadBuilder("/test.txt")
//                .uploadAndFinish(in);
//        }
//    }
//}