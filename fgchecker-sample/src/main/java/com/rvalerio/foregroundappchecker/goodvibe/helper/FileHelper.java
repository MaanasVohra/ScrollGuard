package com.rvalerio.foregroundappchecker.goodvibe.helper;

import android.content.Context;
import android.util.Log;

import com.rvalerio.foregroundappchecker.goodvibe.main.Store;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Helper for file operations
 * Created by fnokeke on 5/29/17.
 */

public class FileHelper {
    private static final String TAG = "FileHelper";

    public static void prepareAllStorageFiles(Context context) {
        FileHelper.appendToFile(context, Store.FB_LOGS_CSV_FILENAME, "");
        FileHelper.appendToFile(context, Store.APP_LOGS_CSV_FILENAME, "");
        FileHelper.appendToFile(context, Store.SCREEN_LOGS_CSV_FILENAME, "");
    }

    public static void appendToFile(Context context, String filename, String data) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(filename, Context.MODE_APPEND);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "appendToFile: error" + e.toString());
            AlarmHelper.showInstantNotif(context, "appendToFile error", e.toString(), "", 5003);
        }
    }

    public static String readFromFile(Context context, String filename) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(filename);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
            AlarmHelper.showInstantNotif(context, "File not found error", e.toString(), "", 5113);
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
            AlarmHelper.showInstantNotif(context, "Cannot read file", e.toString(), "", 5223);
        }

        return ret;
    }

    public static void resetFile(Context context, String filename) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write("");
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "resetFile: error" + e.toString());
            AlarmHelper.showInstantNotif(context, "resetFile error", e.toString(), "", 5333);
        }
    }
}
