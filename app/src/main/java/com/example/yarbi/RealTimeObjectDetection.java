package com.example.yarbi;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RealTimeObjectDetection {

    public static void tfliteRealtimeDetection(String modelPath, String labelPath, float minConf, AssetManager assetManager) {

        try {
            Log.d("ObjectDetection", "Start loading label map");
            List<String> labels = new ArrayList<>();
            InputStream labelsInput = assetManager.open(labelPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line.trim());
            }
            reader.close();
            Log.d("ObjectDetection", "Label map loaded successfully");

            Log.d("ObjectDetection", "Start loading model");
            ByteBuffer modelBuffer = loadModelFile(assetManager,modelPath);
            Interpreter interpreter = new Interpreter(modelBuffer);
            Log.d("ObjectDetection", "Model loaded successfully");
            int width = interpreter.getInputTensor(0).shape()[2];
            int height = interpreter.getInputTensor(0).shape()[1];
            boolean floatInput = interpreter.getInputTensor(0).dataType() == DataType.FLOAT32;
            Log.d("ObjectDetection", "Haaaaaa");
            double inputMean = 127.5;
            double inputStd = 127.5;
            VideoCapture capture = new VideoCapture(0); // Change the argument to 1 if you have multiple cameras
            if (!capture.isOpened()) {
                System.out.println("Camera not found or unable to access");
                Log.d("ObjectDetection", "No cam");
                return;
            }
            Mat frame = new Mat();
            while (capture.read(frame)) {
                if (frame.empty()) {
                    break;
                }

                Mat resizedFrame = new Mat();
                Imgproc.resize(frame, resizedFrame, new Size(width, height));
                Mat imageRGB = new Mat();
                Imgproc.cvtColor(resizedFrame, imageRGB, Imgproc.COLOR_BGR2RGB);

                // Normalize pixel values if using a floating model (non-quantized)
                if (floatInput) {
                    imageRGB.convertTo(imageRGB, CvType.CV_32F, 1.0 / inputStd, -inputMean / inputStd);
                }
                Object[] inputArray = {imageRGB};
                Map<Integer, Object> outputMap = new TreeMap<>();
                float[][][] boxes = new float[1][10][4];
                float[][] classes = new float[1][10];
                float[][] scores = new float[1][10];

                outputMap.put(1, boxes);
                outputMap.put(3, classes);
                outputMap.put(0, scores);

                interpreter.runForMultipleInputsOutputs(inputArray, outputMap);

                for (int i = 0; i < scores[0].length; i++) {
                    if (scores[0][i] > minConf) {
                        float ymin = boxes[0][i][0] * height;
                        float xmin = boxes[0][i][1] * width;
                        float ymax = boxes[0][i][2] * height;
                        float xmax = boxes[0][i][3] * width;

                        int yMin = Math.max(1, (int) ymin);
                        int xMin = Math.max(1, (int) xmin);
                        int yMax = Math.min((int) ymax, height);
                        int xMax = Math.min((int) xmax, width);

                        Imgproc.rectangle(resizedFrame, new Point(xMin, yMin), new Point(xMax, yMax), new Scalar(10, 255, 0), 2);

                        // Draw label
                        String objectName = labels.get((int) classes[0][i]);
                        String label = objectName + ": " + String.format("%.2f", scores[0][i]);
                        Imgproc.putText(resizedFrame, label, new Point(xMin, yMin - 7), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);
                    }
                }
                resizedFrame.release();
                imageRGB.release();
            }

            // Release the camera
            capture.release();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ObjectDetection", "Error loading files: " + e.getMessage());
            return;
        }

    }
    private static ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // use to get description of file
        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset =fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }


}
