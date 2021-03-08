package marcoalunno.com.arias_and_inteludes;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import static java.lang.Thread.sleep;

public class Aria3 extends AppCompatActivity {

    private TextView note, frequency;
    private boolean isRecording = false;
    private AudioDispatcher dispatcher = null;
    private Thread dispatcherThread;
    private SoundPool soundPool;
    private int soundId1, streamId;
    private float currentFrequency = 0;
    private AsyncTasks asyncTasks;
    private ExecutorService executorService;
    private static final int SAMPLE_RATE = 22050;
    private final int SOUND_DURATION = 5689; //this is the duration in mms of piano_impulse_response.ogg

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aria3);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        note = findViewById(R.id.pitchText);
        frequency = findViewById(R.id.frequency);
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-3); //supposedly 3 threads already occupy 3 cores: the UI thread, the async background thread and the dispatcher thread
        createSoundPool();
    }

    public void play(float rate){
        final float r = rate;
        /*
        Note: real sound duration after its rate is changed lasts between ~598mms and 91024mms
        Therefore:
        A4 = 440Hz (A above central C, 5689mms)
        A5 = 880Hz (2844.5mms)
        A6 = 1760Hz (1422.25mms ~= metronome 42)
        A7 = 3520 (711.125mms ~= metronome 84)
        */
        final int realDuration = (int) (SOUND_DURATION/r); //real sound duration after its rate is changed (~598mms <= realDuration <= 91024mms)
        final float rateStep = 0.2f*(realDuration/40)/realDuration; //this divides the rate increment in the for loop (below) into 40 parts according to the sound duration
        final int i = new Random().nextInt(3)-1; //this generates -1, 0  or +1 for glissando direction
        streamId = soundPool.play(soundId1, 1, 1, 1, 0, r);

        executorService.execute(new Runnable(){
            @Override
            public void run(){
                try {
                    if (i == -1) {
                        for (float f = r; f < r - 0.2; f -= rateStep) {
                            sleep(100);
                            if (soundPool != null) {
                                soundPool.setRate(streamId, f);
                            }
                        }
                    } else if (i == 1){
                        for (float f = r; f < r + 0.2; f += rateStep) {
                            sleep(100);
                            if (soundPool != null) {
                                soundPool.setRate(streamId, f);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void startPitchDetection(View view) {
        if (!isRecording) {
            createSoundPool();
            detectPitch();
            isRecording = true;
        } else {
            stopDetector();
            isRecording = false;
        }
    }

    public void detectPitch() {
        asyncTasks = new AsyncTasks(this);
        asyncTasks.execute();
    }

    private static class AsyncTasks extends AsyncTask<Void, String, String> {
        private WeakReference<Aria3> weakReference;

        AsyncTasks(Aria3 activity){
            weakReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... integers) {
            final Aria3 activity = weakReference.get();
            if (activity == null || activity.isFinishing()){
                return "No activity running";
            }

            PitchDetectionHandler pdh = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                    float pitchInHz = result.getPitch();
                    String data = PercussionDetectorRateController.processPitch(pitchInHz);
                    String freq = data.substring(data.indexOf(",")+1);
                    Float freqFloat = Float.parseFloat(freq);
                    String note = data.substring(0, data.indexOf(","));
                    if (freqFloat != activity.currentFrequency && !freq.equals("0.00")) {
                        publishProgress(note, freq);
                        activity.currentFrequency = freqFloat; //therefore, 27.5 <= currentFrequency <= 4186
                    }
                    try {
                        sleep(500);
                    } catch (InterruptedException ie){
                        ie.printStackTrace();
                    }
                }
            };
            activity.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, 1024, 0);
            AudioProcessor p1 = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.MPM, SAMPLE_RATE, 1024, pdh);
            activity.dispatcher.addAudioProcessor(p1);
            activity.dispatcherThread = new Thread(activity.dispatcher, "Audio Dispatcher)");
            activity.dispatcherThread.start();
            return "Finished!";
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Aria3 activity = weakReference.get();
            if (activity == null || activity.isFinishing()){
                return;
            }
            activity.note.setText(values[0]);
            activity.frequency.setText(values[1]);
            float rate = activity.currentFrequency/440.f; //therefore, 0.0625 <= rate <= 9.5136
            if (rate != 0) {
                activity.play(rate);
            }

        }
    }

    public void stopDetector(){
        asyncTasks.cancel(true);
        dispatcherThread.interrupt();
        dispatcherThread = null;
        dispatcher.stop();
        note.setText("pitch");
        frequency.setText("frequency");
        soundPool.stop(streamId);
        soundPool.release();
        soundPool = null;
        executorService.shutdown();
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
        soundId1 = soundPool.load(this, R.raw.piano_impulse_response, 1);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (dispatcherThread != null){
            stopDetector();
        }
    }
}
