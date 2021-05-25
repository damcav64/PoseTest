package jp.daniel.posetest;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class ModelMetaData {
    public static String JSON_FILE = "__map.json";
    public static String MODEL_FILE = "__model.tflite";

    public static String[] labels;
    public static String modelVersion;
    public static boolean modelIsPose = true;

    public ModelMetaData() {
        urls = new Urls();
    }
    String id;
    class Urls {
        String tf;
        String tfjs;
        String tflite;
        String website;
        String labels;
    }
    Urls urls;

    public static ModelMetaData fromJasonString(String json) {
        try {
            JSONObject j = new JSONObject(json);
            return fromJasonObject(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    };

    private static ModelMetaData fromJasonObject(JSONObject j) {
        ModelMetaData d = null;
        try {
            d = new ModelMetaData();
            d.id = j.getString("id");
            j = j.getJSONObject("urls");
            d.urls.tf = j.getString("tf");
            d.urls.tfjs = j.getString("tfjs");
            d.urls.tflite = j.getString("tflite");
            d.urls.website = j.getString("website");
            d.urls.labels = j.getString("labels");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return d;
    }

    public static ModelMetaData[] arrayFromJasonString(String json) {
        ModelMetaData[] m = null;
        try {
            JSONArray j = new JSONArray(json);
            int l = j.length();
            m = new ModelMetaData[l];
            for (int i = 0; i < l; i++) {
                JSONObject o = j.getJSONObject(i);
                m[i] = fromJasonObject(o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return m;
    }

    public static void parseLabelsJson(String json) {
        try {
            JSONObject j = new JSONObject(json);
            JSONArray l = j.getJSONArray("labels");
            labels = new String[l.length()];
            for (int i=0; i<l.length(); i++) {
                labels[i] = l.getString(i);
            }
            if (j.has("model_type")){
                modelIsPose = j.getString("model_type").toLowerCase().equals("pose");
            } else {
                modelIsPose = false;
            }
            if (j.has("model_version")){
                modelVersion = j.getString("model_version");
            } else {
                modelVersion = "default";
            }

        }
        catch (Exception e) {
            // e.printStackTrace();
        }



    }
    public static String readJsonFile(Context c) {
        File mapFile = getJsonFilePath(c, false);
        String rawJson = "";
        FileInputStream fileInputStream = null;
        try {

            fileInputStream = new FileInputStream(mapFile);

            if (fileInputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                rawJson = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(c, "Can't load Json: " + e.toString(),
                    Toast.LENGTH_SHORT).show();
            return "";
        } catch (IOException e) {
            Toast.makeText(c, "Can't load Json: " + e.toString(),
                    Toast.LENGTH_SHORT).show();
            return "";
        } finally {}


        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rawJson;
    }

    public static File getModelFilePath(Context c, boolean temp) {
        File sdcard = c.getExternalFilesDir(null);
        return new File(sdcard, getModelFile(temp));
    }

    public static File getJsonFilePath(Context c, boolean temp) {
        File sdcard = c.getExternalFilesDir(null);
        return new File(sdcard, getJsonFile(temp));
    }

    public static String getModelFile(boolean temp) {
        return MODEL_FILE + (temp ? "_" : "");
    }

    public static String getJsonFile(boolean temp) {
        return JSON_FILE + (temp ? "_" : "");
    }
}