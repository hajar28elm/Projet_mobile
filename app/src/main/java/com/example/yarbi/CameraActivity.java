package com.example.yarbi;
/*import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageClassification imageClassification;
    private AssetManager assetManager;
    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        String modelPath = "detect.tflite"; // Update with just the filename
        String labelPath = "labelmap.txt";
        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);
        assetManager = getAssets();
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);
        try {
            imageClassification = new ImageClassification(modelPath, labelPath, 0.1f, assetManager);
            Toast.makeText(this, "Model loaded succefully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "error loadning the model "+ e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView.setCameraPermissionGranted();  // <------ THIS!!!
                } else {
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();
        mRgba = imageClassification.recognizeImage(mRgba);
        return mRgba;

    }

}*/
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";

    private static final double FRAME_RATE = 0.5;
    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat hierarchy;
    List<MatOfPoint> contours;
    private long lastFrameTime = 0;
    TextView textViewInfo;

    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    //private objectDetectorClass objectDetectorClass;
    private Net net;
    OkHttpClient client = new OkHttpClient();


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV is loaded");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

       /* int MY_PERMISSIONS_REQUEST_CAMERA = 123;
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }*/

        setContentView(R.layout.activity_camera);
        textViewInfo = findViewById(R.id.textViewInfo);
        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);
        mOpenCvCameraView = findViewById(R.id.frame_Surface);
       // mOpenCvCameraView.setMaxFrameSize(3000, 3000);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);
         // Set your desired frame size
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            // OpenCV loaded successfully
            Toast.makeText(this, "OpenCV initialization is done", Toast.LENGTH_SHORT).show();
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            // OpenCV not loaded, try again
            Toast.makeText(this, "OpenCV is not loaded  try againg", Toast.LENGTH_SHORT).show();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat=new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        hierarchy=new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
        hierarchy.release();
}
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
       /* long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime < 2000 / FRAME_RATE) {
            return inputFrame.rgba();
        }*/
        mRgba=inputFrame.rgba();
        //Core.flip(mRgba, mRgba, 1); // Try different flip orientations
        byte[] frameBytes = convertMatToBytes(inputFrame.rgba());
        String jsonResponse = sendFrameToServer(frameBytes);
       // lastFrameTime = currentTime;

        if (jsonResponse != null) {
            Log.d("JSON Response", jsonResponse);

            try {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("results")) {
                    JSONArray detectedObjects = jsonObject.getJSONArray("results");
                    for (int i = 0; i < detectedObjects.length(); i++) {
                        JSONObject object = detectedObjects.getJSONObject(i);
                        String label = object.getString("label");

                        // Test si le label est égal à "koutoubia"
                      /* if ("koutoubia".equals(label)) {
                            //Afficher le TextView avec les informations
                            Log.d("Zmze", "maaabghit");
                           textViewInfo.setVisibility(View.VISIBLE);
                            // Mettre les informations souhaitées dans le TextView
                            textViewInfo.setText("Informations sur Koutoubia : [Vos informations ici]");
                            // Sortir de la boucle si nécessaire après avoir trouvé "koutoubia"
                            break;
                        } else {
                            // Cacher le TextView si le label n'est pas "koutoubia"
                            textViewInfo.setVisibility(View.GONE);
                        }*/
                        // Extract the bounding box coordinates
                        double ymin = object.getDouble("ymin");
                        double xmin = object.getDouble("xmin");
                        double ymax = object.getDouble("ymax");
                        double xmax = object.getDouble("xmax");

                       // mRgba=inputFrame.rgba();
                        contours=new ArrayList<MatOfPoint>();
                        hierarchy=new Mat();
                        Imgproc.Canny(mRgba,mIntermediateMat,80,100);
                        Imgproc.findContours(mIntermediateMat,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE,new Point(0,0));
                        hierarchy.release();

                        for(int contourIndex=0;contourIndex<contours.size();contourIndex++){
                            MatOfPoint2f approxCurve=new MatOfPoint2f();
                            MatOfPoint2f contour2f=new MatOfPoint2f(contours.get(contourIndex).toArray());
                            double approxDistance=Imgproc.arcLength(contour2f,true)*0.01;
                            Imgproc.approxPolyDP(contour2f,approxCurve,approxDistance,true);

                            MatOfPoint points=new MatOfPoint(approxCurve.toArray());
                            Rect rect=Imgproc.boundingRect(points);
                            double height=rect.height;
                            double width=rect.width;

                            if (height > 300 && width > 300) {
                                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 0), 3);
                                Imgproc.putText(mRgba, object.getString("label"), rect.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 255), 4);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return mRgba;
}

    private byte[] convertMatToBytes(Mat frame) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, matOfByte);
        return matOfByte.toArray();
    }
    private String sendFrameToServer(byte[] frameBytes) {
        AtomicReference<String> responseData = new AtomicReference<>("");
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("frame", "frame.jpg", RequestBody.create(MediaType.parse("image/jpeg"), frameBytes));

        RequestBody requestBody = multipartBuilder.build();
        Request request = new Request.Builder()
                .url("http://192.168.1.145:5001/detect_objects")
                .post(requestBody)
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Server down: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseBody = response.body().string();
                responseData.set(responseBody);
                Log.d("Response", responseBody);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return responseData.get();
}

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView.setCameraPermissionGranted();  // <------ THIS!!!
                } else {
                }
                return;
            }
        }
    }


}