package jp.daniel.posetest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;

public class ModelsActiviity extends AppCompatActivity {

    private ListView listView;
    private DownloadManager dm;
    private RequestQueue queue;
    private StringRequest stringRequest;

    private ModelMetaData[] modelMetaData;

    private int filesDownloaded = 0;
    private int selectedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models);


        Window w = getWindow();
        w.setTitle("My title");

        createListView();
        getListOfModels();
        // Get ListView object from xml

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

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
                        .show();
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
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
    }

    private void getListOfModels() {
        String url="https://run.mocky.io/v3/117b9945-72c3-4e04-aece-f89e504eed3b";
        queue = Volley.newRequestQueue(this);
        //display the response on screen
        stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                doModelListReceived(response);
                Toast.makeText(getApplicationContext(), "Response :" + response.toString(), Toast.LENGTH_LONG).show();//display the response on screen

            }
        }, new Response.ErrorListener() {
            private static final String TAG = "Models activity download";
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i(TAG, "Error :" + error.toString());
            }
        });

        queue.add(stringRequest);
    }

    private void doModelItemReceived(String json) {
        ModelMetaData model = ModelMetaData.fromJasonString(json);
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
            Toast.makeText(getApplicationContext(), "Unable to save image", Toast.LENGTH_LONG).show();
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
            msg("Downloading json");
            downloadFile(this, modelMetaData[selectedIndex].urls.labels,
                    ModelMetaData.getJsonFile(true));
        } else if (filesDownloaded == 2) {
            deleteFiles(false);
            renameTempFiles();
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

    private void msg(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();

    }
}