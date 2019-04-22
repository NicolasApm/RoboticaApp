package com.michaeltroger.shapedetection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.michaeltroger.shapedetection.devices.BTCallback;
import com.michaeltroger.shapedetection.devices.BTUtil;
import com.michaeltroger.shapedetection.views.ConectarBluetoothView;
import com.michaeltroger.shapedetection.views.OverlayView;

/**
 * the main activity - entry to the application
 */
public class MainActivity extends Activity implements CvCameraViewListener2 {

    private String btAdd = "98:D3:81:FD:4E:DA";
    private String DataSend = "";
    Handler bluetoothIn;
    private BluetoothAdapter mBtAdapter;
    final int handlerState = 0;
    boolean flag = false;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String Inbit1 = "";
    private String Inbit2 = "";
    private String[] txts;
    // String para la direccion MAC

    /**
     * class name for debugging with logcat
     */
    private static final String TAG = MainActivity.class.getName();
    /**
     * the camera view
     */
    private CameraBridgeViewBase mOpenCvCameraView;
    /**
     * for displaying Toast info messages
     */
    private Toast toast;
    /**
     * responsible for displaying images on top of the camera picture
     */
    private OverlayView overlayView;
    /**
     * whether or not to log the memory usage per frame
     */
    private static final boolean LOG_MEM_USAGE = true;
    /**
     * detect only red objects
     */
    private static final boolean DETECT_RED_OBJECTS_ONLY = false;
    /**
     * the lower red HSV range (lower limit)
     */
    private static final Scalar HSV_LOW_RED1 = new Scalar(0, 100, 100);
    /**
     * the lower red HSV range (upper limit)
     */
    private static final Scalar HSV_LOW_RED2 = new Scalar(10, 255, 255);
    /**
     * the upper red HSV range (lower limit)
     */
    private static final Scalar HSV_HIGH_RED1 = new Scalar(160, 100, 100);
    /**
     * the upper red HSV range (upper limit)
     */
    private static final Scalar HSV_HIGH_RED2 = new Scalar(179, 255, 255);
    /**
     * definition of RGB red
     */
    private static final Scalar RGB_RED = new Scalar(255, 0, 0);
    /**
     * frame size width
     */
    private static final int FRAME_SIZE_WIDTH = 640;
    /**
     * frame size height
     */
    private static final int FRAME_SIZE_HEIGHT = 480;
    /**
     * whether or not to use a fixed frame size -> results usually in higher FPS
     * 640 x 480
     */
    private static final boolean FIXED_FRAME_SIZE = true;
    /**
     * whether or not to use the database to display
     * an image on top of the camera
     * when false the objects are labeled with writing
     */
    private static final boolean DISPLAY_IMAGES = false;
    /**
     * image thresholded to black and white
     */
    private Mat bw;
    /**
     * image converted to HSV
     */
    private Mat hsv;
    /**
     * the image thresholded for the lower HSV red range
     */
    private Mat lowerRedRange;
    /**
     * the image thresholded for the upper HSV red range
     */
    private Mat upperRedRange;
    /**
     * the downscaled image (for removing noise)
     */
    private Mat downscaled;
    /**
     * the upscaled image (for removing noise)
     */
    private Mat upscaled;
    /**
     * the image changed by findContours
     */
    private Mat contourImage;
    /**
     * the activity manager needed for getting the memory info
     * which is necessary for getting the memory usage
     */
    private ActivityManager activityManager;
    /**
     * responsible for getting memory information
     */
    private ActivityManager.MemoryInfo mi;
    /**
     * the found contour as hierarchy vector
     */
    private Mat hierarchyOutputVector;
    /**
     * approximated polygonal curve with specified precision
     */
    private MatOfPoint2f approxCurve;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    bw = new Mat();
                    hsv = new Mat();
                    lowerRedRange = new Mat();
                    upperRedRange = new Mat();
                    downscaled = new Mat();
                    upscaled = new Mat();
                    contourImage = new Mat();
                    hierarchyOutputVector = new Mat();
                    approxCurve = new MatOfPoint2f();
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        btAdapter = BluetoothAdapter.getDefaultAdapter();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_my);
        overlayView = (OverlayView) findViewById(R.id.overlay_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT);
        }
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mi = new ActivityManager.MemoryInfo();
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        VerificarEstadoBT();

        //Setea la direccion MAC
        //Log.d("direccio", address);
        BluetoothDevice device = btAdapter.getRemoteDevice("98:D3:81:FD:4E:DA");

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");

                    if (endOfLineIndex > 0) {
                        String dataInPrint;
                        dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        // IdBufferIn.setText("Dato: " + dataInPrint);//<-<- PARTE A MODIFICAR >->->
                        DataStringIN.delete(0, DataStringIN.length());
                        Log.d("DataIn", dataInPrint);
                        try {
                            txts = dataInPrint.split(",");
                            Inbit1 = txts[0];
                            Inbit2 = txts[1];
                            Log.d("Inbit1", Inbit1);
                            Log.d("Inbit2", Inbit2);
                            if (Inbit1.equals("Ok")) {
                                if (Inbit2.equals("Run")) {
                                    flag = false;
                                    Log.d("DataIn", dataInPrint);
                                }
                                if (Inbit2.equals("Stop")) {
                                    flag = true;
                                    Log.d("DataIn", dataInPrint);
                                }
                            } else {
                            }
                        } catch (Exception e) {
                            Log.e("TramaIn", "Trama no", e);
                        }
                    }
                }
            }
        };

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try {
            btSocket.connect();
        } catch (IOException e) {
            //Toast.makeText(getBaseContext(), "Error Conexion", Toast.LENGTH_LONG).show();
            try {
                btSocket.close();
                finish();
            } catch (IOException e2) {
            }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Toast.makeText(getBaseContext(), "PAUSADO", Toast.LENGTH_SHORT).show();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (toast != null)
            toast.cancel();

        try { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "Error Cerrando", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private void VerificarEstadoBT() {
        // Comprueba que el dispositivo tiene Bluetooth y que está encendido.
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {

            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Activado...");
            } else {
                //Solicita al usuario que active Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (LOG_MEM_USAGE) {
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.availMem / 1048576L; // 1024 x 1024
            Log.d(TAG, "available mem: " + availableMegs);
        }
        Mat gray = null;

        if (DETECT_RED_OBJECTS_ONLY) {
            gray = inputFrame.rgba();
        } else {
            gray = inputFrame.gray();
        }
        Mat dst = inputFrame.rgba();
        Imgproc.pyrDown(gray, downscaled, new Size(gray.cols() / 2, gray.rows() / 2));
        Imgproc.pyrUp(downscaled, upscaled, gray.size());

        if (DETECT_RED_OBJECTS_ONLY) {
            Imgproc.cvtColor(upscaled, hsv, Imgproc.COLOR_RGB2HSV);
            Core.inRange(hsv, HSV_LOW_RED1, HSV_LOW_RED2, lowerRedRange);
            Core.inRange(hsv, HSV_HIGH_RED1, HSV_HIGH_RED2, upperRedRange);
            Core.addWeighted(lowerRedRange, 1.0, upperRedRange, 1.0, 0.0, bw);
            Imgproc.Canny(bw, bw, 0, 255);
        } else {
            Imgproc.Canny(upscaled, bw, 0, 255);
        }

        Imgproc.dilate(bw, bw, new Mat(), new Point(-1, 1), 1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        contourImage = bw.clone();
        Imgproc.findContours(
                contourImage,
                contours,
                hierarchyOutputVector,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        for (MatOfPoint cnt : contours) {
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(
                    curve,
                    approxCurve,
                    0.02 * Imgproc.arcLength(curve, true),
                    true
            );

            int numberVertices = (int) approxCurve.total();
            double contourArea = Imgproc.contourArea(cnt);
            Log.d(TAG, "vertices:" + numberVertices);
            // ignore to small areas
            if (Math.abs(contourArea) < 100
                // || !Imgproc.isContourConvex(
            ) {
                continue;
            }
            // triangle detection
            if (numberVertices == 3) {
                if (DISPLAY_IMAGES) {
                    doSomethingWithContent("triangle");
                } else {
                    setLabel(dst, "TRI", cnt);
                }
                if (flag) {

                    DataSend = "1";
                    MyConexionBT.write(DataSend);
                    DataSend = "";
                }
            }

            // rectangle, pentagon and hexagon detection
            if (numberVertices >= 4 && numberVertices <= 6) {
                List<Double> cos = new ArrayList<>();
                for (int j = 2; j < numberVertices + 1; j++) {
                    cos.add(
                            angle(
                                    approxCurve.toArray()[j % numberVertices],
                                    approxCurve.toArray()[j - 2],
                                    approxCurve.toArray()[j - 1]
                            )
                    );
                }
                Collections.sort(cos);
                double mincos = cos.get(0);
                double maxcos = cos.get(cos.size() - 1);

                // rectangle detection
                if (numberVertices == 4
                        && mincos >= -0.1 && maxcos <= 0.3
                ) {
                    if (DISPLAY_IMAGES) {
                        doSomethingWithContent("rectangle");
                    } else {
                        setLabel(dst, "RECT", cnt);
                    }
                    if (flag) {
                        DataSend = "0";
                        MyConexionBT.write(DataSend);
                        DataSend = "";
                    }
                }
                // pentagon detection
                else if (numberVertices == 5
                        && mincos >= -0.34 && maxcos <= -0.27) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "PENTA", cnt);
                    }
                }
                // hexagon detection
                else if (numberVertices == 6
                        && mincos >= -0.55 && maxcos <= -0.45) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "HEXA", cnt);
                    }
                }
            }
            // circle detection
            else {
                Rect r = Imgproc.boundingRect(cnt);
                int radius = r.width / 2;

                if (Math.abs(
                        1 - (r.width / r.height)
                ) <= 0.2 &&
                        Math.abs(
                                1 - (
                                        contourArea / (Math.PI * radius * radius)
                                )
                        ) <= 0.2
                ) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "CIR", cnt);
                    }
                }
            }
        }
        Mat input = inputFrame.gray();
        Mat circles = new Mat();
        Imgproc.blur(input, input, new Size(7, 7), new Point(2, 2));
        Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100, 100, 90, 0, 1000);
        Log.i(TAG, String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));

        if (circles.cols() > 0) {
            if (flag) {
                DataSend = "2";
                MyConexionBT.write(DataSend);
                DataSend = "";
            }
        }
        circles.release();
        input.release();
        return dst;
    }

    /**
     * Helper function to find a cosine of angle between vectors
     * from pt0->pt1 and pt0->pt2
     */
    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt(
                (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10
        );
    }

    /**
     * display a label in the center of the given contur (in the given image)
     *
     * @param im      the image to which the label is applied
     * @param label   the label / text to display
     * @param contour the contour to which the label should apply
     */
    private void setLabel(Mat im, String label, MatOfPoint contour) {
        int fontface = Core.FONT_HERSHEY_SIMPLEX;
        double scale = 3;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];

        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect(contour);

        Point pt = new Point(
                r.x + ((r.width - text.width) / 2),
                r.y + ((r.height + text.height) / 2)
        );
        /*
        Imgproc.rectangle(
                im,
                new Point(r.x + 0, r.y + baseline[0]),
                new Point(r.x + text.width, r.y -text.height),
                new Scalar(255,255,255),
                -1
                );
        */
        Imgproc.putText(im, label, pt, fontface, scale, RGB_RED, thickness);
    }

    /**
     * makes an logcat/console output with the string detected
     * displays also a TOAST message and finally sends the command to the overlay
     *
     * @param content the content of the detected barcode
     */
    private void doSomethingWithContent(String content) {
        Log.d(TAG, "content: " + content); // for debugging in console

        final String command = content;

        Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
            public void run() {
                overlayView.changeCanvas(command);
            }
        });
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());

            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
