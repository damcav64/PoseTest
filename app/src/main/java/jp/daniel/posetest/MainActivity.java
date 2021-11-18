package jp.daniel.posetest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.ProtoUtil;
import com.google.mediapipe.glutil.EglManager;

import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String BINARY_GRAPH_NAME = "pose_tracking_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks";
    private static final String YCMA_MODEL = "model.tflite";
    private static final String YCMA_MAP = "map.json";
    private String modelId = "default";
    private int textureName = 65;

    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;
    private static final boolean FLIP_FRAMES_VERTICALLY = true;


    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
    }

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private FrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;
    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    private Camera2Helper cameraHelper;

    private Interpreter tfLite;


    //new model
    int frameCount = -1;
    float [][][][] inputFrames;


    private boolean permissionsGranted = false;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutResId());

        ProtoUtil.registerTypeName(NormalizedLandmarkList.class, "mediapipe.NormalizedLandmarkList");

        int port = 8887; // 843 flash policy port
     //   WSServer wsServer = new WSServer(port);
      //  wsServer.start();

        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }

        verifyPermissions(this);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent activity2Intent = new Intent(getApplicationContext(), ModelsActiviity.class);
                startActivityForResult(activity2Intent, 1);
            }
        });

        eglManager = new EglManager(null);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                modelId = data.getStringExtra("id");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // Write your code if there's no result
            }
        }
    } //onActivityResult

    /*

    private static float[][] getPoseLandmarksArray(NormalizedLandmarkList poseLandmarks) {
        float[][] data = new float[1][66];
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            data[0][landmarkIndex] = landmark.getX();
            data[0][landmarkIndex + 33] = landmark.getY();
            landmarkIndex++;
        }
        return data;
    }

     */

    float [][][][] initInputFrames() {
        return new float[1][ModelMetaData.duration][33][ModelMetaData.modelIsPose ? 4 : 3];
    }

    private void getPoseLandmarksArrayFrames(NormalizedLandmarkList poseLandmarks, int frame, float[][][][] data) {
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            data[0][frame][landmarkIndex][0] = landmark.getX();
            data[0][frame][landmarkIndex][1] = landmark.getY();
            data[0][frame][landmarkIndex][2] = landmark.getZ();
            if (ModelMetaData.modelIsPose) {
                data[0][frame][landmarkIndex][3] = landmark.getVisibility();
            }
            landmarkIndex++;
        }
    }

    private float[][][] getPoseLandmarksArray2(NormalizedLandmarkList poseLandmarks) {
        float[][][]data = new float[1][33][ModelMetaData.modelIsPose ? 4 : 3];
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            data[0][landmarkIndex][0] = landmark.getX();
            data[0][landmarkIndex][1] = landmark.getY();
            data[0][landmarkIndex][2] = landmark.getZ();
            if (ModelMetaData.modelIsPose) {
                data[0][landmarkIndex][3] = landmark.getVisibility();
            }
            landmarkIndex++;
        }
        return data;
    }

    private static String getPoseLandmarksDebugString(NormalizedLandmarkList poseLandmarks) {
        String poseLandmarkStr = "Pose landmarks: " + poseLandmarks.getLandmarkCount() + "\n";
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            poseLandmarkStr +=
                    "\tLandmark ["
                            + landmarkIndex
                            + "]: ("
                            + landmark.getX()
                            + ", "
                            + landmark.getY()
                            + ", "
                            + landmark.getZ()
                            + ", "
                            + landmark.getVisibility()
                            + ", "
                            + landmark.getPresence()
                            + ")\n";
            ++landmarkIndex;
        }
        return poseLandmarkStr;
    }

    private static JSONObject getLabelJSON(float[][] output) {
        JSONObject labelObject = new JSONObject();
        try {
            labelObject.put("Y", output[0][0]);
            labelObject.put("M", output[0][1]);
            labelObject.put("C", output[0][2]);
            labelObject.put("A", output[0][3]);
            labelObject.put("None", output[0][4]);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return labelObject;
    }

    private static JSONObject createLandmarksJSON(JSONArray poseArray){
        JSONObject outputObject = new JSONObject();
        try {
            outputObject.put("type", "pose");
            outputObject.put("data", poseArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return outputObject;
    }

    private static JSONObject createConfidenceJSON(JSONObject labelObject){
        JSONObject outputObject = new JSONObject();
        try {
            outputObject.put("type", "event");
            outputObject.put("data", labelObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return outputObject;
    }

    private static JSONArray getPoseLandmarksJSON(NormalizedLandmarkList poseLandmarks) {
        JSONArray poseArray = new JSONArray();
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            JSONObject poseObject = new JSONObject();
            try {
                poseObject.put("x", landmark.getX());
                poseObject.put("y", landmark.getY());
                poseObject.put("z", landmark.getZ());
                poseObject.put("v", landmark.getVisibility());
                poseObject.put("p", landmark.getPresence());
                poseArray.put(poseObject);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return poseArray;
    }

    private void createConverter() {
        converter =
                new ExternalTextureConverter(
                        eglManager.getContext(), 2);
        converter.setFlipY(FLIP_FRAMES_VERTICALLY);
        converter.setConsumer(processor);
    }


    @Override
    protected void onResume() {
        super.onResume();
        go();
    }

    private void go() {

        if (permissionsGranted) {
            setFrameProcessor();
            createConverter();

            if (completeSetup()) {
                startCamera();
                msg(/*"id: " + modelId + */ "Type: "
                + (ModelMetaData.modelIsPose ? "pose" : "portrait"));
                createListView();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (converter != null) {
            converter.close();
        }

        if (previewDisplayView != null) {
            // Hide preview display until we re-open the camera again.
            previewDisplayView.setVisibility(View.GONE);
        }
    }

    protected int getContentViewLayoutResId() {
        return R.layout.activity_main;
    }

    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;
        previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return new Size(640, 480);
    }

    public void startCamera() {
        cameraHelper = new Camera2Helper(this,new CustomSurfaceTexture(textureName));

        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                });

        cameraHelper.startCamera(
                this, CameraHelper.CameraFacing.BACK, /*unusedSurfaceTexture=*/ null);
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }

    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private  MappedByteBuffer loadModelFile2(String modelFilename)
            throws IOException {
        File modelFile = ModelMetaData.getModelFilePath(this, false);
        FileInputStream fis = new FileInputStream(modelFile);
        FileChannel fileChannel = fis.getChannel();
        long declaredLength = modelFile.length();
        MappedByteBuffer bb = null;
        try {
            bb = fileChannel.map(FileChannel.MapMode.READ_ONLY,
                0, declaredLength);}
        catch (Exception e) {
            msg("Corrupt model");
        }
        return bb;
    }

    private void msg(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }





    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)
            && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                && (grantResults[2] == PackageManager.PERMISSION_GRANTED)){
            permissionsGranted = true;
            go();
        }
    }

    public boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int REQUEST_EXTERNAL_STORAGE = 1;
        List<String> requestedPermissions = new ArrayList<String>();

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED)  {
            requestedPermissions.add(Manifest.permission.CAMERA);
        }

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED) {
            requestedPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_DENIED) {
            requestedPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (requestedPermissions.size() > 0) {
            String[] rp = new String[requestedPermissions.size()];
            requestedPermissions.toArray(rp);
            ActivityCompat.requestPermissions(activity, rp, 666);
            return false;
        }
        else {
            permissionsGranted = true;
            return true;
        }
    }

    private void displayCaption(final int highestI, final float highest) {
        /*
        runOnUiThread(() -> {
            TextView v = (TextView)findViewById(R.id.caption1);
            if (highest < 0.5) {
                v.setText("");
                v = (TextView) findViewById(R.id.caption2);
                v.setText("");
            } else {
                v.setText(highestI == -1 ? "" : ModelMetaData.labels[highestI]);
                v = (TextView) findViewById(R.id.caption2);
                v.setText(highestI == -1 ? "" : String.valueOf(highest));
            }
        });*/
        runOnUiThread(() -> {
            listView.setItemChecked(highestI, true);
        });

    }

    private void setFrameProcessor() {


        try{
            displayCaption(-1, 0);
            sleep(2000);}
        catch (Exception e) {
        }

        processor =
                new FrameProcessor(
                        this,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        OUTPUT_VIDEO_STREAM_NAME);

        //take a nap while all the opengl threads sort themselves out


        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);

        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
                    Log.v(TAG, "Received pose landmarks packet.");
                    try {
                        if (true){//wsServer.getConnections().size() > 0) {
                            if (true) {
                                NormalizedLandmarkList poseLandmarks =
                                        PacketGetter.getProto(packet, NormalizedLandmarkList.class);
                                //wsServer.broadcast(createLandmarksJSON(getPoseLandmarksJSON(poseLandmarks)).toString());
                                // tflite extra
                                float[][] output = new float[1][ModelMetaData.labels.length];

                                if (ModelMetaData.duration == -1) {//old model
                                    float[][][] input = getPoseLandmarksArray2(poseLandmarks);
                                    tfLite.run(input, output);
                                } else {
                                    if (frameCount == -1 //very first time
                                            || ++frameCount == ModelMetaData.duration) {
                                        frameCount = 0;
                                        if (inputFrames != null) {
                                            tfLite.run(inputFrames, output);
                                        }
                                        inputFrames = initInputFrames();
                                    }
                                    getPoseLandmarksArrayFrames(poseLandmarks, frameCount, inputFrames);
                                }

                                float highest = 0;
                                int highestI = 0;
                                for (int i=0; i<ModelMetaData.labels.length; i++) {
                                    if (output[0][i] > highest) {
                                        highest = output[0][i];
                                        highestI = i;
                                    }
                                }

                                displayCaption(highestI, highest);

                                //     wsServer.broadcast(createConfidenceJSON(getLabelJSON(output)).toString());
                                //wsServer.broadcast("out: " + output);
                                //      Log.v(TAG, "Y " + output[0][0]);
                                //    Log.v(TAG, "M " + output[0][1]);
                                //   Log.v(TAG, "C " + output[0][2]);
                                //   Log.v(TAG, "A " + output[0][3]);
                                //   Log.v(TAG, "None " + output[0][4]);

//                            Log.v(
//                                    TAG,
//                                    "[TS:"
//                                            + packet.getTimestamp()
//                                            + "] "
//                                            + getPoseLandmarksDebugString(poseLandmarks));
                            }
                        }


                    } catch(InvalidProtocolBufferException exception){
                        Log.e(TAG, "Failed to get proto.", exception);
                    }
                });
    }

    private boolean getJson() {
        boolean theStuffIsOnTheSdCard = true;
        String jason = ModelMetaData.readJsonFile(this);
        if (jason.length() == 0) {
            jason = "{^labels^: [^Y^,^M^,^C^, ^A^, ^None^],  ^model_type^: ^pose^, ^model_version:^: ^default^}";
            jason = jason.replace('^', (char)34);
            theStuffIsOnTheSdCard = false;
        }
        ModelMetaData.parseLabelsJson(jason);
        return theStuffIsOnTheSdCard;
    }

    private boolean completeSetup() {

        boolean theStuffIsOnTheSdCard = getJson();
        // NNAPI
        Interpreter.Options options = (new Interpreter.Options());
        NnApiDelegate nnApiDelegate = null;
        // Initialize interpreter with NNAPI delegate for Android Pie or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            nnApiDelegate = new NnApiDelegate();
            options.addDelegate(nnApiDelegate);
        }

        // load YMCA tflite model
        Log.d(TAG, "loading tflite model: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        try {
            if (!theStuffIsOnTheSdCard) {
                tfLite = new Interpreter(loadModelFile(getAssets(), YCMA_MODEL));
            } else {
                tfLite = new Interpreter(loadModelFile2(ModelMetaData.MODEL_FILE));
            }

            Log.d(TAG, "onCreate: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            Log.d(TAG, tfLite.getInputTensor(0).toString());
            Log.d(TAG, tfLite.getOutputTensor(0).toString());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Problem with model: " + e.toString(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }

        previewDisplayView = new SurfaceView(this);
        setupPreviewDisplayView();

        AndroidAssetUtil.initializeNativeAssetManager(this);
        return true;
    }

//    private MappedByteBuffer loadModelFile() throws IOException {
//        String MODEL_ASSETS_PATH = "recog_model.tflite";
//        AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(MODEL_ASSETS_PATH) ;
//        FileInputStream fileInputStream = new FileInputStream( assetFileDescriptor.getFileDescriptor() ) ;
//        FileChannel fileChannel = fileInputStream.getChannel() ;
//        long startoffset = assetFileDescriptor.getStartOffset() ;
//        long declaredLength = assetFileDescriptor.getDeclaredLength() ;
//        return fileChannel.map( FileChannel.MapMode.READ_ONLY , startoffset , declaredLength ) ;
//    }

    private void createListView() {
        listView = (ListView) findViewById(R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setSelector(android.R.color.darker_gray);
        populateListView();
        // Defined Array values to show in ListView


        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

            }

        });
    }

    private void populateListView() {

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, ModelMetaData.labels);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
    }

}