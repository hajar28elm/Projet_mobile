package com.example.yarbi;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.checkerframework.checker.units.qual.A;
import org.opencv.android.Utils;
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
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
public class ImageClassification {
    private float minCon;
    InputStream labelsInput;
    Interpreter interpreter;
    int width ;
    int height ;
    boolean floatInput ;
    private List<String> labels;
    ImageClassification(String modelPath, String labelPath, float minConf, AssetManager assetManager) throws IOException{
        try {
            Log.d("ObjectDetection", "Start loading label map");
            labels = new ArrayList<>();
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
            interpreter = new Interpreter(modelBuffer);
            Log.d("ObjectDetection", "Model loaded successfully");
             width = interpreter.getInputTensor(0).shape()[2];
             height = interpreter.getInputTensor(0).shape()[1];
             floatInput = interpreter.getInputTensor(0).dataType() == DataType.FLOAT32;
            minCon = minConf;
            Log.d("ObjectDetection", "Haaaaaa");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ObjectDetection", "Error loading files: " + e.getMessage());
            return;
        }
    }
    public Mat recognizeImage(Mat frame) {
        Mat resizedFrame = new Mat();
        if (interpreter == null) {
            Log.e("ObjectDetection", "Interpreter is null");
            return frame; // Return the original frame if interpreter is not initialized
        }
        if (labels != null && !labels.isEmpty()) {
            // Your existing code that uses labels.get(int) goes here...
        } else {
            Log.e("ObjectDetection", "Labels list is null or empty");
        }
        try {
            double inputMean = 127.5;
            double inputStd = 127.5;
            Imgproc.resize(frame, resizedFrame, new Size(width, height));
            Mat imageRGB = new Mat();
            Imgproc.cvtColor(resizedFrame, imageRGB, Imgproc.COLOR_BGR2RGB);
            if (floatInput) {
                imageRGB.convertTo(imageRGB, CvType.CV_32F, 1.0 / inputStd, -inputMean / inputStd);
            }
            ByteBuffer inputBuffer = convertMatToByteBuffer(imageRGB);
            Object[] inputArray = {inputBuffer};
            Map<Integer, Object> outputMap = new TreeMap<>();
            float[][][] boxes = new float[1][10][4];
            float[][] classes = new float[1][10];
            float[][] scores = new float[1][10];
            outputMap.put(1, boxes);
            outputMap.put(3, classes);
            outputMap.put(0, scores);
            interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
            for (int i = 0; i < scores[0].length; i++) {
                Log.d("ObjectDetection", "in Ifffff");
                    float ymin = boxes[0][i][0] * height;
                    float xmin = boxes[0][i][1] * width;
                    float ymax = boxes[0][i][2] * height;
                    float xmax = boxes[0][i][3] * width;
                    int yMin = Math.max(1, (int) ymin);
                    int xMin = Math.max(1, (int) xmin);
                    int yMax = Math.min((int) ymax, height);
                    int xMax = Math.min((int) xmax, width);
                    Imgproc.rectangle(resizedFrame, new Point(xMin, yMin), new Point(xMax, yMax), new Scalar(10, 255, 0), 2);
                    String objectName = labels.get((int) classes[0][i]);
                    String label = objectName + ": " + String.format("%.2f", scores[0][i]);
                    Log.d("ObjectDetection", "lDakhkhl");
                    Imgproc.putText(resizedFrame, label, new Point(xMin, yMin - 7), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);
                    Log.d("ObjectDetection", label);
                resizedFrame.release();
                imageRGB.release();
            }
            } catch(Exception e){
                e.printStackTrace();
                Log.e("ObjectDetection", "Error recognizing image: " + e.getMessage());
            }
            return frame; // Modify this to return the processed frame with detections
        }

   /* private List<String> loadLabelList(AssetManager assetManager, String labelPath) {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("label list ","gdhsdsds");
        } finally {
            // Ensure to close the reader in the finally block to release resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d("f finally ","sdsgdhsds");
                    e.printStackTrace(); // Print the stack trace
                }
            }
        }

        return labelList;
    }*/


    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset =fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
    // create new Mat function
   /* public Mat recognizeImage(Mat mat_image){

        try {
            Mat rotated_mat_image=new Mat();
            Mat a = mat_image.t();
            Core.flip(a, rotated_mat_image, 1);
            // Release mat
            a.release();

            // if you do not do this process you will get improper prediction, less no. of object
            // now convert it to bitmap
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rotated_mat_image, bitmap);
            // define height and width
            height = bitmap.getHeight();
            width = bitmap.getWidth();

            // scale the bitmap to input size of model
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

            // convert bitmap to bytebuffer as model input should be in it
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

            // defining output
            // 10: top 10 object detected
            // 4: there coordinate in image
            //  float[][][]result=new float[1][10][4];
            Object[] input = new Object[1];
            input[0] = byteBuffer;

            Map<Integer, Object> output_map = new TreeMap<>();
            // we are not going to use this method of output
            // instead we create treemap of three array (boxes,score,classes)

            float[][][] boxes = new float[1][10][4];
            float[][] scores = new float[1][10];
            float[][] classes = new float[1][10];


            output_map.put(1, boxes);
            output_map.put(3, classes);
            output_map.put(0, scores);

            interpreter.runForMultipleInputsOutputs(input, output_map);
            for (int i = 0; i < 10; i++) {
                float class_value = classes[0][i];
                float score_value = scores[0][i];

                if (score_value > 0.5) {
                    float[] box1 = boxes[0][i];

                    float top = box1[0] * height;
                    float left = box1[1] * width;
                    float bottom = box1[2] * height;
                    float right = box1[3] * width;

                    Log.d("id", labelList.get((int) class_value));
                    Imgproc.rectangle(rotated_mat_image, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0, 255), 2);
                    Imgproc.putText(rotated_mat_image, labelList.get((int) class_value), new Point(left, top), 3, 1, new Scalar(255, 0, 0, 255), 2);
                    break;
                }
            }


            Mat b = rotated_mat_image.t();
            Core.flip(b, mat_image, 0);
            b.release();
            // Now for second change go to CameraBridgeViewBase
            return mat_image;
        }catch(Exception e){

            return mat_image;
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        // some model input should be quant=0  for some quant=1
        // for this quant=0
        // Change quant=1
        // As we are scaling image from 0-255 to 0-1
        int quant=0;
        int size_images=INPUT_SIZE;
        if(quant==1){
            byteBuffer=ByteBuffer.allocateDirect(1*size_images*size_images*3);
        }
        else {
            byteBuffer=ByteBuffer.allocateDirect(4*1*size_images*size_images*3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_images*size_images];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel=0;

        // some error
        //now run
        for (int i=0;i<size_images;++i){
            for (int j=0;j<size_images;++j){
                final  int val=intValues[pixel++];
                if(quant==0){
                    byteBuffer.put((byte) ((val>>16)&0xFF));
                    byteBuffer.put((byte) ((val>>8)&0xFF));
                    byteBuffer.put((byte) (val&0xFF));
                }
                else {
                    byteBuffer.putFloat((((val >> 16) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val) & 0xFF))/255.0f);
                }
            }
        }
        return byteBuffer;
    }
    public void logTensorShapes() {
        // Get the number of input and output tensors
        int inputTensorCount = interpreter.getInputTensorCount();
        int outputTensorCount = interpreter.getOutputTensorCount();

        // Log details about the input tensors (shapes, etc.)
        for (int i = 0; i < inputTensorCount; i++) {
            int[] inputTensorShape = interpreter.getInputTensor(i).shape();
            System.out.println("Input Tensor " + i + " Shape: " + Arrays.toString(inputTensorShape));
        }

        // Log details about the output tensors (shapes, etc.)
        for (int i = 0; i < outputTensorCount; i++) {
            int[] outputTensorShape = interpreter.getOutputTensor(i).shape();
            System.out.println("Output Tensor " + i + " Shape: " + Arrays.toString(outputTensorShape));

}
}*/
    private ByteBuffer convertMatToByteBuffer(Mat frame) {
        int bytesPerChannel = 4; // 4 bytes per channel for FLOAT32 data type
        int byteSize = frame.rows() * frame.cols() * frame.channels() * bytesPerChannel;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteSize);
        byteBuffer.order(ByteOrder.nativeOrder());

        // Normalize and add pixel values to the ByteBuffer
        for (int row = 0; row < frame.rows(); row++) {
            for (int col = 0; col < frame.cols(); col++) {
                double[] pixel = frame.get(row, col);

                // Normalize pixel values for each channel (assuming RGB)
                float[] normalizedValues = new float[3]; // Assuming 3 channels for RGB image
                for (int channel = 0; channel < 3; channel++) {
                    normalizedValues[channel] = (float)((pixel[channel] / 255.0 - 0.5) / 0.5); // Normalize to [-1, 1] range
                }

                // Add normalized pixel values to the ByteBuffer
                for (float value : normalizedValues) {
                    byteBuffer.putFloat(value);
                }
            }
        }

        // Reset the position of the buffer to the beginning
        byteBuffer.rewind();
        return byteBuffer;
    }

}
