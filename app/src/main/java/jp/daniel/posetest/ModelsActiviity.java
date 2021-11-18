package jp.daniel.posetest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModelsActiviity extends AppCompatActivity {

    private ListView listView;
    private DownloadManager dm;
    private RequestQueue queue;
    private StringRequest stringRequest;

    private ModelMetaData[] modelMetaData;
    private ModelMetaData modelMetaDataLatest;

    private Button settingsButton;

    private int filesDownloaded = 0;
    private int selectedIndex = 0;

  // private String API_KEY = "8MmWUcjMzd2VeehhIgnKE8wfm6HgbZPv7eu38u1t";
 //  private String URL_MODELS = "https://teachablemachine-api.cherry-ad.aidisco.sky.com/<project_id>/models";
 //  private String URL_LATEST = "https://teachablemachine-api.cherry-ad.aidisco.sky.com/<project_id>/models/latest";

    private String PROJECT_ID_PLACEHOLDER = "<project_id>";
    public String PROJECT_ID = "a7b5bafe-236d-4ee7-b80d-223dcd65c260";
    private String URL = "https://oke4v2jvik.execute-api.eu-west-1.amazonaws.com/test";
    private String URL_MODELS = "models";
    private String URL_LATEST = "models/latest";
    private String API_KEY = "76nm9pDfus5fgIlQTJFgZ8cQIVOAm1JnS9hZ2am8";

//https://teachablemachine-test.cherry-ad-dev.aidisco.sky.com/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models);


        Window w = getWindow();
        w.setTitle("Models");

        getFromPersistent();

        createListView();
        getListOfModels(true);

        // Get ListView object from xml
        settingsButton = (Button)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettings();
            }
        });
    }

    private void createListView() {
        listView = (ListView) findViewById(R.id.list);

        // Defined Array values to show in ListView


        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);

                selectedIndex = itemPosition;
                downloadFiles();
            }

        });
    }

    private void populateListView() {
        int l = modelMetaData.length;
        String[] values = new String[l];

        for (int i = 0; i < l; i++) {
            values[i] = modelMetaData[i].id;
            if (modelMetaDataLatest.id.equals(modelMetaData[i].id)) {
                values[i] += " (latest)";
            };
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
    }

    private void resetListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, new String[0]);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
    }

    private void getListOfModels(boolean aGetLatest) {
      //  String url="https://run.mocky.io/v3/117b9945-72c3-4e04-aece-f89e504eed3b";
        String url = URL + "/" + PROJECT_ID + "/" + (aGetLatest ? URL_LATEST : URL_MODELS);

        queue = Volley.newRequestQueue(this);
        //display the response on screen
        stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (aGetLatest) {
                    doLatestModelReceived(response);
                } else {
                    doModelListReceived(response);
                }
            }
        }, new Response.ErrorListener() {
            private static final String TAG = "Models activity download";
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error :" + error.toString());
                msg("Error :" + error.toString());
            }
        })
        {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("x-api-key", API_KEY);

                return params;
            }}
        ;

        queue.add(stringRequest);
    }

    private void doLatestModelReceived(String json) {
        modelMetaDataLatest = ModelMetaData.fromJasonString(json);
        getListOfModels(false);
    }

    private void doModelListReceived(String json) {
        modelMetaData = ModelMetaData.arrayFromJasonString(json);
        populateListView();
    }

    private void downloadFile(Context c, String url, String targetFileName) {

        try {
            dm = (DownloadManager) c.getSystemService(DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    //.setDestinationUri(Uri.parse(targetFileName.toString()))
                    .setDestinationInExternalFilesDir(c, null, targetFileName)
                    .setTitle(targetFileName).setDescription("download")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            dm.enqueue(request);

            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    doFileDownloaded();
                  //  long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    //    if (dm == reference) {
                    // Do something with downloaded file.
                    //   }

                }
            };
            registerReceiver(receiver, filter);

        } catch (Exception ex) {
            // just in case, it should never be called anyway
            Toast.makeText(getApplicationContext(), "Unable to save to the sd card", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    private void downloadFiles() {
        deleteFiles(true);
        filesDownloaded = 0;
        msg("Downloading model");
        downloadFile(this, modelMetaData[selectedIndex].urls.tflite,
                ModelMetaData.getModelFile(true));
    }

    public void doFileDownloaded() {
         if (++filesDownloaded == 1) {
            downloadFile(this, modelMetaData[selectedIndex].urls.labels,
                    ModelMetaData.getJsonFile(true));
        } else if (filesDownloaded == 2) {
            deleteFiles(false);
            renameTempFiles();
             Intent returnIntent = new Intent();
             returnIntent.putExtra("id", modelMetaData[selectedIndex].id);
             setResult(Activity.RESULT_OK,returnIntent);
            finish();

        } else {

             //why does this happen?
         }

    }

    private void renameTempFiles() {
        File f = ModelMetaData.getModelFilePath(this, true);
        if (f.exists()) {
            f.renameTo(ModelMetaData.getModelFilePath(this, false));
        }

        f = ModelMetaData.getJsonFilePath(this, true);
        if (f.exists()) {
            f.renameTo(ModelMetaData.getJsonFilePath(this, false));
        }
    }

    private void deleteFiles(boolean temp) {
        File f = ModelMetaData.getModelFilePath(this, temp);
        if (f.exists()) {
            f.delete();
        }

        f = ModelMetaData.getJsonFilePath(this, temp);
        if (f.exists()) {
            f.delete();
        }
    };

    private void startSettings() {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        settingsIntent.putExtra("URL", URL);
        settingsIntent.putExtra("PROJECT_ID", PROJECT_ID);
        settingsIntent.putExtra("API_KEY", API_KEY);
        startActivityForResult(settingsIntent, 1);
    }
    private void msg(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            URL = data.getStringExtra("URL");
            PROJECT_ID = data.getStringExtra("PROJECT_ID");
            API_KEY = data.getStringExtra("API_KEY");
            persist();
            resetListView();
            getListOfModels(true);
        }
    }

    private void persist() {
        SharedPreferences settings = getSharedPreferences("Test", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("URL", URL);
        edit.putString("PROJECT_ID", PROJECT_ID);
        edit.putString("API_KEY", API_KEY);
        edit.apply();
    }

    private void getFromPersistent() {
        SharedPreferences settings = getSharedPreferences("Test", Context.MODE_PRIVATE);
        String s1 = settings.getString("URL", "");
        if (!s1.equals("")) {
            String s2 = settings.getString("PROJECT_ID", "");
            if (!s2.equals("")) {
                String s3 = settings.getString("API_KEY", "");
                if (!s3.equals("")) {
                    URL = s1;
                    PROJECT_ID = s2;
                    API_KEY = s3;
                 }
            }
        }
    }
}