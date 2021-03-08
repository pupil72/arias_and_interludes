package marcoalunno.com.arias_and_inteludes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class Aria1 extends AppCompatActivity {

    private Button startButton, pauseButton, stopButton;
    private SeekBar playAudioProgress, volumeSeekbar;
    private Handler audioProgressHandler, staffScrollHandler;
    private Runnable runnableAudio, runnableStaff;
    private TextView timer, volumeCounter, countdown;
    private CountDownTimer countDownTimer;
    private int currentPosition = 0;
    private int duration = 0;
    private int marginTop = 0;
    private int scrollStep = 0;
    private int step = 0;
    private int nextStep = 0;
    private float volume;
    private final int NUMBER_OF_STAVES = 20;
    private int staffDuration = 0;
    private MediaPlayer mp;
    private ImageView aria1Score;
    private RelativeLayout aria1Layout;
    private FrameLayout.LayoutParams params;
    private Matrix matrix = new Matrix();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aria1);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mp = MediaPlayer.create(getApplicationContext(), getResources().getIdentifier("sonata_13_prepared_notes_only" +
                "", "raw", getPackageName()));
        startButton = findViewById(R.id.play_audio_start_button);
        startButton.setEnabled(true);
        pauseButton = findViewById(R.id.play_audio_pause_button);
        pauseButton.setEnabled(false);
        stopButton = findViewById(R.id.play_audio_stop_button);
        stopButton.setEnabled(false);
        playAudioProgress = findViewById(R.id.play_audio_seekbar);
        duration = mp.getDuration();
        staffDuration = duration / NUMBER_OF_STAVES;
        nextStep = staffDuration;
        playAudioProgress.setMax(duration);
        volumeSeekbar = findViewById(R.id.volume_seekbar);
        timer = findViewById(R.id.timer_intrada);
        volumeCounter = findViewById(R.id.volume_counter);
        countdown = findViewById(R.id.countdown);
        aria1Score = findViewById(R.id.aria1_score);
        aria1Layout = findViewById(R.id.aria1Layout);
        params = (FrameLayout.LayoutParams)aria1Score.getLayoutParams();
        audioProgressHandler = new Handler();
        staffScrollHandler = new Handler();
        SharedPreferences settings = getSharedPreferences("settingsAria1", MODE_PRIVATE);
        volume = settings.getFloat("volumeValue", 0.3f);
        volumeSeekbar.setProgress((int)(volume*10));
        volumeCounter.setText(Integer.toString((int)(volume*10)));

        //so, metronome here is half-note = 52, so a tick is every 1.153s (or 1153mms), and 3 ticks = 1153*3 = 3459
        countDownTimer = new CountDownTimer(3459, 100) {
            @Override
            public void onTick(long l) {
                countdown.setText(Long.toString((int)(l+1152)/1153));
            }

            @Override
            public void onFinish() {
                countdown.setText("");
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                //mp.setPlaybackParams(mp.getPlaybackParams().setPitch((float) Math.pow(2, 1.0/12.0))); //this transposes one semitone higher, 1.0 being the reference value (normal pitch)
                mp.start();
                changeSeekBar();
            }
        };

        runnableStaff = new Runnable() {
            @Override
            public void run() {
                if (scrollStep > -step) {
                    params.setMargins(0, marginTop + scrollStep, 0, 0);
                    scrollStep -= 15;
                    staffScrollHandler.postDelayed(runnableStaff, 10);
                } else {
                    staffScrollHandler.removeCallbacks(runnableStaff);
                    marginTop -= step;
                    scrollStep = 0;
                }
            }
        };

        playAudioProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if (fromUser){
                    mp.seekTo(progress);
                    currentPosition = seekBar.getProgress();
                    timer.setText(convertTime(currentPosition));
                    marginTop = -step * (currentPosition/staffDuration);
                    params.setMargins(0, marginTop, 0, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mp.getCurrentPosition() != 0) {
                    nextStep = staffDuration * (int) Math.ceil((double) mp.getCurrentPosition() / staffDuration);
                } else {
                    nextStep = staffDuration;
                }
            }
        });

        volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                volumeSeekbar.setProgress(progress);
                volumeCounter.setText(Integer.toString(progress));
                volume = progress/10.0f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        aria1Score.setImageResource(R.drawable.sonata_13_for_phone_screen_300_no_repeat);
        float scale = (float) aria1Layout.getWidth()/aria1Score.getDrawable().getIntrinsicWidth();
        int scaledHeight = (aria1Layout.getWidth()*aria1Score.getDrawable().getIntrinsicHeight()) / aria1Score.getDrawable().getIntrinsicWidth();
        step = scaledHeight / NUMBER_OF_STAVES;
        matrix.setScale(scale, scale);
        aria1Score.setImageMatrix(matrix);
    }

    public void startPlayer(View view) {
        mp.seekTo(currentPosition);
        mp.setVolume(volume,volume);
        startButton.setEnabled(false);
        countDownTimer.start();
    }

    public void pausePlayer(View view) {
        mp.pause();
        currentPosition = mp.getCurrentPosition();
        audioProgressHandler.removeCallbacks(runnableAudio);
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    public void stopPlayer(View view) {
        stop();
    }

    public void stop() {
        mp.pause();
        playAudioProgress.setProgress(0);
        currentPosition = 0;
        nextStep = staffDuration;
        marginTop = 0;
        scrollStep = 0;
        params.setMargins(marginTop, 0, 0, 0);
        timer.setText(convertTime(currentPosition));
        audioProgressHandler.removeCallbacks(runnableAudio);
        staffScrollHandler.removeCallbacks(runnableStaff);
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    public void changeSeekBar() {
        try {
            playAudioProgress.setProgress(mp.getCurrentPosition());
            timer.setText(convertTime(mp.getCurrentPosition()));
            if (mp.getCurrentPosition() > nextStep-2500 && nextStep < (staffDuration*NUMBER_OF_STAVES)){ //20 is the number of leaps toward the end of the score
                staffScrollHandler.post(runnableStaff);
                nextStep += staffDuration;
            }
            runnableAudio = new Runnable() {
                @Override
                public void run() {
                    mp.setVolume(volume, volume);
                    changeSeekBar();
                }
            };
            audioProgressHandler.postDelayed(runnableAudio, 1);
        } catch (NullPointerException ne) {}
    }

    public String convertTime(int d) {
        String s = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(d),
                TimeUnit.MILLISECONDS.toSeconds(d)- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(d)));
        return s;
    }

    public void onBackPressed() {
        audioProgressHandler.removeCallbacks(runnableAudio);
        staffScrollHandler.removeCallbacks(runnableStaff);
        mp.stop();
        mp.reset();
        mp.release();
        mp = null;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mp != null){
            stop();
        }
        countDownTimer.cancel();
        countdown.setText("");
        SharedPreferences settings = getSharedPreferences("settingsAria1", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("volumeValue", volume);
        editor.apply();
    }
}
