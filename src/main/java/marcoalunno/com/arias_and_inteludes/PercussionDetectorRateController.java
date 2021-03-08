package marcoalunno.com.arias_and_inteludes;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class PercussionDetectorRateController extends AppCompatActivity {

    private TextView note, frequency, rmsValue, thresholdCounter, sensitivityCounter, dbCounterLeft, dbCounterRight;
    private ToggleButton recordBtn;
    private boolean isRecording = false;
    private AudioDispatcher dispatcher = null;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private final int MY_PERMISSIONS_RECORD_AUDIO_IN_DEVICE = 2;
    private final int SAMPLE_RATE = 22050;
    private Thread dispatcherThread = null;
    private int threshold;
    private int sensitivity;
    private int dbLeft = -45;
    private int dbRight = -45;
    private float x1 = 0;
    private int progressLeftDb = 65;
    private int progressRightDb = 65;
    private int progressRateX = 0;
    private int progressRateY = 0;
    private SeekBar thresholdSeekbar, sensitivitySeekbar, dbLeftSeekbar, dbRightSeekbar;
    private float pitchInHz = 0;
    private float rms = 0;
    private SoundPool soundPool;
    private int soundId1;
    private Graph graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_percussion_detector_rate_controller);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new AndroidFFMPEGLocator(this);
        recordBtn = findViewById(R.id.recordBtn);
        graph = findViewById(R.id.canvas);
        rmsValue = findViewById(R.id.rms);
        note = findViewById(R.id.pitchText);
        frequency = findViewById(R.id.frequency);
        thresholdSeekbar = findViewById(R.id.threshold_seekBar);
        sensitivitySeekbar = findViewById(R.id.sensitivity_seekBar);
        dbLeftSeekbar = findViewById(R.id.dbleft);
        dbRightSeekbar = findViewById(R.id.dbright);
        thresholdCounter = findViewById(R.id.threshold_counter);
        sensitivityCounter = findViewById(R.id.sensitivity_counter);
        dbCounterLeft = findViewById(R.id.dbCounterLeft);
        dbCounterRight = findViewById(R.id.dbCounterRight);
        SharedPreferences settings = getSharedPreferences("settingsPercussion", MODE_PRIVATE);
        threshold = settings.getInt("thresholdValue", 10);
        sensitivity = settings.getInt("sensitivityValue", 65);
        thresholdSeekbar.setProgress(threshold);
        thresholdCounter.setText(Integer.toString(threshold));
        sensitivitySeekbar.setProgress(sensitivity);
        sensitivityCounter.setText(Integer.toString(sensitivity));

        thresholdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold = progress;
                thresholdCounter.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sensitivitySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress;
                sensitivityCounter.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        dbLeftSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressLeft, boolean fromUser) {
                dbLeft = progressLeft-110;
                graph.rateX = 0;
                graph.startY = (65 - progressLeft) * graph.getHeight() / 65;
                if (progressLeft >= progressRightDb) {
                    graph.rateX = graph.height;
                    graph.rateY = (65 - progressLeft) * graph.getHeight() / 65;
                } else if (progressLeft < progressRightDb) {
                    graph.rateY = (65 - progressRightDb) * graph.getHeight() / 65;
                }

                dbCounterLeft.setText(Integer.toString(dbLeft));
                progressLeftDb = progressLeft;
                progressRateX = 0;
                progressRateY = (int) (((450-graph.rateY) * 65) / 450);
                graph.reDraw();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        dbRightSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressRight, boolean fromUser) {
                dbRight = progressRight - 110;
                graph.rateX = graph.height;
                graph.endY = (65 - progressRight) * graph.getHeight() / 65;
                if (progressRight >= progressLeftDb) {
                    graph.rateX = 0;
                    graph.rateY = (65 - progressRight) * graph.getHeight() / 65;
                } else if (progressRight < progressLeftDb) {
                    graph.rateY = (65 - progressLeftDb) * graph.getHeight() / 65;
                }

                dbCounterRight.setText(Integer.toString(dbRight));
                progressRightDb = progressRight;
                progressRateX = 65;
                progressRateY = (int) (((450-graph.rateY) * 65) / 450);
                graph.reDraw();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        createSoundPool();
        requestAudioPermissions();
    }

    public void createSoundPool(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(32)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(32, AudioManager.STREAM_MUSIC, 0);
        }
        soundId1 = soundPool.load(this, R.raw.piano_ff_c3_loud, 1);
    }

    public void startRecording(View view) {
        if (!isRecording) {
            createSoundPool();
            record();
            isRecording = true;
        } else {
            stopRecord();
            isRecording = false;
        }
    }

    public void record() {
        thresholdSeekbar.setEnabled(false);
        sensitivitySeekbar.setEnabled(false);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                pitchInHz = result.getPitch();
                rms = (float) e.getdBSPL();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String freq = processPitch(pitchInHz);
                        note.setText(freq.substring(0, freq.indexOf(",")));
                        frequency.setText(String.format("%.3f", pitchInHz));
                        rmsValue.setText(String.format("%.3f", rms));
                    }
                });
            }
        };

        OnsetHandler oh = new OnsetHandler() {
            @Override
            public void handleOnset(double time, double salience) {
                //Get a, b, c terms of quadratic equation for Bezier curve knowing start point.y,
                //control point.y, end point.y and y coordinate of point on the curve (rms)
                int minDB = Math.min(dbLeft,dbRight);
                int maxDB = Math.max(dbLeft, dbRight);
                if (rms >= minDB && rms <= maxDB) {
                    float a = (minDB+110) - 2 * progressRateY + (maxDB+110);
                    float b = 2 * (progressRateY - (minDB+110));
                    float c = (minDB+110) - (110+rms);
                    //solve for t1
                    double t1 = (-b + Math.sqrt(Math.pow(b,2) - 4*a*c)) / (2*a);
                    //get point.x with t1
                    if (progressRateX == 0){
                        x1 = (float) (t1 * t1 * 65); //it would be : x1 = (float) (2 * (1 - t1) * t1 * progressRateX + t1 * t1 * 65); but progressRateX is always 0
                    } else if (progressRateX == 65) {
                        x1 = 65 - (float) (t1 * t1 * 65); //get the symmetrical from the progressRateX = 0;
                    }
                    float scaledX1 = (5 * x1) / 65;
                    float rate = (float) (0.25 * Math.pow(2, scaledX1));
                    soundPool.play(soundId1, 1, 1, 0, 0, rate); //rate varies from 0.25 (C1) to 8 (C6) with 1 = C3. Rate is a function of dB (rms)
                    //Log.i("coordinates", String.format("%d, %d, %d", minDB, progressRateY, maxDB));
                    //Log.i("dataBezier", String.format("%f, %f, %f, %f, %f, %d", rms, t1, x1, rate, scaledX1, progressRateX));
                } else {
                    Log.e("rate", "out of range");
                }
            }
        };

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, 1024, 0);
        AudioProcessor p1 = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, SAMPLE_RATE, 1024, pdh);
        AudioProcessor p2 = new PercussionOnsetDetector(SAMPLE_RATE, 1024, oh, sensitivity, threshold);
        dispatcher.addAudioProcessor(p1);
        dispatcher.addAudioProcessor(p2);
        dispatcherThread = new Thread(dispatcher, "Audio Dispatcher)");
        dispatcherThread.start();
        isRecording = true;
    }

    public void stopRecord(){
        dispatcherThread.interrupt();
        dispatcherThread = null;
        dispatcher.stop();
        rmsValue.setText("RMS");
        note.setText("pitch");
        frequency.setText("frequency");
        thresholdSeekbar.setEnabled(true);
        sensitivitySeekbar.setEnabled(true);
        //soundPool.stop(streamId);
        soundPool.release();
        soundPool = null;
    }

    static String processPitch(float pitchInHz) {
        String note = "";
        float freq = 0;
        for (int i = 0; i <= 8; i++ ) {
            if (pitchInHz >= 16.35 * Math.pow(2,i) && pitchInHz < 17.32 * Math.pow(2,i)) {
                //C
                note = "C" + i;
                freq = 16.35f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 17.32 * Math.pow(2,i) && pitchInHz < 18.35 * Math.pow(2,i)) {
                //C#
                note = "C#" + i;
                freq = 17.32f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 18.35 * Math.pow(2,i) && pitchInHz < 19.45 * Math.pow(2,i)) {
                //D
                note = "D" + i;
                freq = 18.35f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 19.45 * Math.pow(2,i) && pitchInHz < 20.60 * Math.pow(2,i)) {
                //D#
                note = "D#" + i;
                freq = 19.45f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 20.60 * Math.pow(2,i) && pitchInHz < 21.83 * Math.pow(2,i)) {
                //E
                note = "E" + i;
                freq = 20.60f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 21.83 * Math.pow(2,i) && pitchInHz < 23.12 * Math.pow(2,i)) {
                //F
                note = "F" + i;
                freq = 21.83f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 23.12 * Math.pow(2,i) && pitchInHz < 24.50 * Math.pow(2,i)) {
                //F#
                note = "F#" + i;
                freq = 23.12f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 24.50 * Math.pow(2,i) && pitchInHz < 25.96 * Math.pow(2,i)) {
                //G
                note = "G" + i;
                freq = 24.50f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 25.96 * Math.pow(2,i) && pitchInHz < 27.50 * Math.pow(2,i)) {
                //G#
                note = "G#" + i;
                freq = 25.96f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 27.50 * Math.pow(2,i) && pitchInHz < 29.14 * Math.pow(2,i)) {
                //A
                note = "A" + i;
                freq = 27.50f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 29.14 * Math.pow(2,i) && pitchInHz < 30.87 * Math.pow(2,i)) {
                //A#
                note = "A#" + i;
                freq = 29.14f * (float) Math.pow(2,i);
            } else if (pitchInHz >= 30.87 * Math.pow(2,i) && pitchInHz < 32.70 * Math.pow(2,i)) {
                //B
                note = "B"+ i;
                freq = 30.87f * (float) Math.pow(2,i);
            } else if (pitchInHz < 16.35 || pitchInHz >= 32.70 * Math.pow(2,8)){
                //no pitch
                note = "-";
                freq = 0;
            }
        }
        return String.format("%s,%.2f", note, freq);
    }

    public void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                //Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();
                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else {}
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == MY_PERMISSIONS_RECORD_AUDIO){

            } else if (requestCode == MY_PERMISSIONS_RECORD_AUDIO_IN_DEVICE){
                record();
            }
        } else {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        return;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (dispatcherThread != null){
            stopRecord();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("graph.startY", graph.startY);
        outState.putInt("graph.endY", graph.endY);
        outState.putFloat("graph.rateX", graph.rateX);
        outState.putFloat("graph.rateY", graph.rateY);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recordBtn.setChecked(isRecording);
        graph.startY = savedInstanceState.getInt("graph.startY");
        graph.endY = savedInstanceState.getInt("graph.endY");
        graph.rateX = savedInstanceState.getFloat("graph.rateX");
        graph.rateY = savedInstanceState.getFloat("graph.rateY");
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getSharedPreferences("settingsPercussion", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("thresholdValue", threshold);
        editor.putInt("sensitivityValue", sensitivity);
        editor.apply();
    }
}
