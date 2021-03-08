package marcoalunno.com.arias_and_inteludes;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import be.tarsos.dsp.AudioGenerator;
import be.tarsos.dsp.FadeIn;
import be.tarsos.dsp.FadeOut;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.synthesis.AmplitudeLFO;

public class Theremin extends AppCompatActivity implements OnTouchListener, CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private ToggleButton         applyScreen;
    private SeekBar              gainSeekbar;
    private RelativeLayout       interScreenLayout;
    private LinearLayout         playerCommands;
    private boolean              isScreenApplied = false;
    private boolean              isFirstTime = true;
    private InterScreen          interScreen;
    private int                  cameraWidth = 0;
    private int                  columns = 0;
    private int                  gainDivisor;
    private AudioGenerator       generator;
    private Thread               generatorThread = null;
    private int                  tempFrequency = 0;
    private ArrayList<CustomSineGenerator> sine = new ArrayList<>();
    private int                  intSizeOut;
    private final int            SAMPLE_RATE = 44100;
    private final TarsosDSPAudioFormat OUTPUT_FORMAT = new TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private FadeOut              fadeOut;
    private Handler              handler;
    private Handler              interScreenHandler;
    private Mat                  touchedRegionRgba = null;
    private Mat                  touchedRegionHsv = null;
    private int                  mRgbaPerimeter = 0;
    private TextView             noteName;
    private TextView             gainDivisorCounter;
    private final int SINE_ARRAY_SIZE = 10;
    private final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback;

    public Theremin() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_theremin);
        interScreenLayout = findViewById(R.id.inter_screen_layout);
        playerCommands = findViewById(R.id.playerCommands);
        applyScreen = findViewById(R.id.apply_screen);
        gainSeekbar = findViewById(R.id.gain_seekbar);
        interScreen = findViewById(R.id.inter_screen);
        noteName = findViewById(R.id.noteName);
        gainDivisorCounter = findViewById(R.id.gain_divisor);
        SharedPreferences settings = getSharedPreferences("thereminSettings", MODE_PRIVATE);
        gainDivisor = settings.getInt("gain", -30);
        gainSeekbar.setProgress(gainDivisor);
        gainDivisorCounter.setText(Integer.toString(-gainDivisor));
        intSizeOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        handler = new Handler();
        interScreenHandler = new Handler();

        requestCameraPermission();

        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");
                        mOpenCvCameraView.enableView();
                        mOpenCvCameraView.setOnTouchListener(Theremin.this);
                        mOpenCvCameraView.setCameraIndex(1);
                    } break;

                    default: {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        gainSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                gainDivisor = progress+1;
                gainDivisorCounter.setText(Integer.toString(-progress-1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void applyScreen(View view){
        if (!isScreenApplied){
            interScreen.setBackgroundColor(Color.rgb(40,40,40));
            interScreen.colorCircle = Color.argb(150,255,255,255);
            interScreen.colorLines = Color.WHITE;
            isScreenApplied = true;
            applyScreen.setBackgroundColor(Color.argb(255, 64, 76, 155));
            playerCommands.setBackgroundColor(Color.argb(255, 64, 76, 155));
        } else {
            interScreen.setBackgroundColor(Color.TRANSPARENT);
            interScreen.colorCircle = Color.rgb(0,0,0);
            interScreen.colorLines = Color.TRANSPARENT;
            isScreenApplied = false;
            applyScreen.setBackgroundColor(Color.argb(125, 64, 76, 155));
            playerCommands.setBackgroundColor(Color.argb(125, 64, 76, 155));
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        cameraWidth = width;
        interScreen.requestLayout();
        FrameLayout.LayoutParams interScreenParams = (FrameLayout.LayoutParams) interScreenLayout.getLayoutParams();
        interScreenParams.width = width;
        interScreenParams.gravity = 1;
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        Scalar CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (!mIsColorSelected) {
            interScreen.clearCanvas();
            int cols = mRgba.cols();
            int rows = mRgba.rows();
            int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
            int x = cols - ((int)event.getX() - xOffset);
            int y = (int)event.getY() - yOffset;
            columns = cols;
            mRgbaPerimeter = (cols*2) + (rows*2);
            Log.i(TAG, "Touch image coordinates: (" + cols + ", " + rows + ")");
            if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

            Rect touchedRect = new Rect();

            touchedRect.x = (x>4) ? x-4 : 0;
            touchedRect.y = (y>4) ? y-4 : 0;

            touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
            touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

            touchedRegionRgba = mRgba.submat(touchedRect);

            touchedRegionHsv = new Mat();
            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

            // Calculate average color of touched region
            mBlobColorHsv = Core.sumElems(touchedRegionHsv);
            int pointCount = touchedRect.width*touchedRect.height;
            for (int i = 0; i < mBlobColorHsv.val.length; i++)
                mBlobColorHsv.val[i] /= pointCount;

            mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

            Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

            mDetector.setHsvColor(mBlobColorHsv);

            Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);

            applyScreen.setEnabled(true);
            mIsColorSelected = true;
            interScreen.isStopped = false;
        } else {
            clearAll();
            fadeOut.startFadeOut();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopGenerator();
                }
            }, 300);
            applyScreen.setBackgroundColor(Color.argb(125, 64, 76, 155));
            playerCommands.setBackgroundColor(Color.argb(125, 64, 76, 155));
        }
        return false;
    }

    public void makeGenerator(double gain, int frequency, double vibrato){
        fadeOut = new FadeOut(0.3);
        generator = new AudioGenerator(1024,0);
        generator.addAudioProcessor(new AmplitudeLFO(0, 0));
        for (int i = 0; i < SINE_ARRAY_SIZE; i++) {
            sine.add(new CustomSineGenerator(gain/Math.pow(2,i), frequency*(i+1), vibrato));
            generator.addAudioProcessor(sine.get(i));
            sine.get(i).setTempFrequency(tempFrequency);
        }
        generator.addAudioProcessor(new FadeIn(0.2));
        generator.addAudioProcessor(fadeOut);
        generator.addAudioProcessor(new AndroidAudioPlayer(OUTPUT_FORMAT, intSizeOut, AudioManager.STREAM_MUSIC));
        generatorThread = new Thread(generator, "Theremin thread)");
        generatorThread.start();
    }

    public void clearAll(){
        interScreenHandler.removeCallbacksAndMessages(null);
        mIsColorSelected = false;
        interScreen.clearCanvas();
        interScreen.setBackgroundColor(Color.TRANSPARENT);
        applyScreen.setEnabled(false);
        touchedRegionRgba.release();
        touchedRegionHsv.release();
        applyScreen.setChecked(false);
        isScreenApplied = false;
    }

    public void stopGenerator(){
        isFirstTime = true;
        generator.stop();
        generatorThread.interrupt();
        generatorThread = null;
        sine.clear();
        noteName.setText("");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            //Log.e(TAG, "Contours count: " + contours.size());
            //Imgproc.drawContours(mRgba, contours, -1, new Scalar(Color.BLUE), 3);
            for (MatOfPoint contour: contours) {
                Moments moments = Imgproc.moments(contour);
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                int perimeter = (int) Imgproc.arcLength(contour2f, true);
                int centroidX = (int) (moments.get_m10() / moments.get_m00());
                int centroidY = (int) (moments.get_m01() / moments.get_m00());
                double gain = ((double) centroidY / mRgba.height()) / (SINE_ARRAY_SIZE*gainDivisor); //for safety divide by at least twice sine.size()
                //int frequency = ((columns-centroidX) * (4186-40) / columns) + 40; //map values between 40Hz (lowest end of speaker's range) and 4186Hz (C8, highest end of piano keyboard)
                int frequency = (int) ((centroidX * (523.25-130.82) / columns) + 130.82); //map values between 261.64(C3) and 523.25(C5)
                double vibrato = (int) ((perimeter-50) * ((mRgbaPerimeter/3) / (mRgbaPerimeter-50.)));
                if (isFirstTime){
                    isFirstTime = false;
                    makeGenerator(gain, frequency, vibrato);
                    tempFrequency = frequency;
                }
                //if ((frequency < (tempFrequency - 100) )|| (frequency > (tempFrequency + 100))){
                if (frequency < (tempFrequency-(tempFrequency/8.)) || frequency > (tempFrequency+(tempFrequency/8.))) { //dividing by 8 means cycling through major seconds (8th partial)
                //if (frequency < (tempFrequency-(Math.round(Math.pow(2, 1.0/12.0))))  ||  frequency > (tempFrequency+(Math.round(Math.pow(2, 1.0/12.0))))){ //chromatic scale
                    for (int j = tempFrequency; j <= Math.abs(tempFrequency-frequency); j++) {
                        for (int i = 0; i < sine.size(); i++) {
                            sine.get(i).setFrequency(j * (i + 1));
                        }
                    }
                    tempFrequency = frequency;
                }
                for (int i = 0; i < sine.size(); i++) {
                    sine.get(i).setTempFrequency(tempFrequency*(i+1));
                    sine.get(i).setGain(gain);
                    sine.get(i).setVibrato(vibrato/3);
                }
                final int x = interScreen.getWidth() - ((centroidX * interScreen.getWidth()) / cameraWidth);
                final int y = centroidY;
                interScreenHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        noteName.setX(x+80);
                        noteName.setY(y-20);
                        String freq = PercussionDetectorRateController.processPitch(tempFrequency);
                        noteName.setText(freq.substring(0, freq.indexOf(",")));
                        //noteName.setText(PercussionDetectorRateController.processPitch(tempFrequency));
                        interScreen.reDraw(x, y);
                    }
                });
                //Imgproc.circle(mRgba, new Point(centroidX, centroidY), 10, new Scalar(0,0,0),20);
                //Imgproc.fillPoly(mRgba, Arrays.asList(contour), new Scalar(255,255,255)); //this fills the contour
            }
            //mRgba.setTo(new Scalar(133,133,133)); //change color of Mat image
            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);
            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return new Scalar(pointMatRgba.get(0, 0));
    }

    public void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        }
        else {}
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CAMERA_PERMISSION_REQUEST_CODE){}
        } else {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        return;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if(generatorThread != null) {
            clearAll();
            stopGenerator();
        }
        SharedPreferences settings = getSharedPreferences("thereminSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("gain", gainDivisor);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if(generatorThread != null) {
            generator.stop();
        }
    }
}