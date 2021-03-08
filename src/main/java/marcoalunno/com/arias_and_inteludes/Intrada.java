package marcoalunno.com.arias_and_inteludes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class Intrada extends AppCompatActivity {

    private Button startButton, pauseButton, stopButton;
    private SeekBar playAudioProgress, volumeSeekbar;
    private Handler audioProgressHandler;
    private Runnable runnable;
    private TextView timer, volumeCounter;
    private int currentPosition = 0;
    private int duration = 0; //total duration 1:40.385
    private int marginLeft;
    private float volume = 0.3f;
    private MediaPlayer mp = null;
    private ImageView intradaScore;
    private FrameLayout.LayoutParams params;
    private final int SCORE_OFFSET = 48; //default left margin for score
    private final int START_RIGHT_HAND_104 = 48544; //mms from beginning when live piano starts at quarter note = 104
    private final int START_RIGHT_HAND_120 = 44501; //mms from beginning when live piano starts at quarter note = 120
    //with the following variable, remember to update the interval in line 162
    private final int START_RIGHT_HAND_AGAIN_104 = 72559; //mms from beginning when live piano starts again at quarter note = 104
    private final int START_RIGHT_HAND_AGAIN_120 = 66492; //mms from beginning when live piano starts again at quarter note = 120

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intrada);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mp = MediaPlayer.create(getApplicationContext(), getResources().getIdentifier("intrada_120_loud" +
                "", "raw", getPackageName()));
        startButton = findViewById(R.id.play_audio_start_button);
        startButton.setEnabled(true);
        pauseButton = findViewById(R.id.play_audio_pause_button);
        pauseButton.setEnabled(false);
        stopButton = findViewById(R.id.play_audio_stop_button);
        stopButton.setEnabled(false);
        playAudioProgress = findViewById(R.id.play_audio_seekbar);
        duration = mp.getDuration();
        playAudioProgress.setMax(duration);
        volumeSeekbar = findViewById(R.id.volume_seekbar);
        timer = findViewById(R.id.timer_intrada);
        volumeCounter = findViewById(R.id.volume_counter);
        intradaScore = findViewById(R.id.intrada_score);
        params = (FrameLayout.LayoutParams)intradaScore.getLayoutParams();
        audioProgressHandler = new Handler();

        playAudioProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if (fromUser){
                    mp.seekTo(progress);
                    currentPosition = seekBar.getProgress();
                    timer.setText(convertTime(currentPosition));
                    if (mp.getCurrentPosition() > START_RIGHT_HAND_120 && mp.getCurrentPosition() < START_RIGHT_HAND_AGAIN_120){
                        marginLeft = -(progress-START_RIGHT_HAND_120) / 4;
                        params.setMargins(marginLeft+SCORE_OFFSET, 0, 0, 0);
                    } else if (mp.getCurrentPosition() >= START_RIGHT_HAND_AGAIN_120){
                        marginLeft = -(progress-START_RIGHT_HAND_AGAIN_120) / 4;
                        params.setMargins(marginLeft+SCORE_OFFSET, 0, 0, 0);
                    } else if (mp.getCurrentPosition() < START_RIGHT_HAND_120) {
                        params.setMargins(SCORE_OFFSET, 0, 0, 0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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

    public void startPlayer(View view) {
        mp.seekTo(currentPosition);
        mp.setVolume(volume,volume);
        mp.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        changeSeekBar();
    }

    public void pausePlayer(View view) {
        mp.pause();
        currentPosition = mp.getCurrentPosition();
        audioProgressHandler.removeCallbacks(runnable);
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
        resetMarginLeft();
        timer.setText(convertTime(currentPosition));
        audioProgressHandler.removeCallbacks(runnable);
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    public void resetMarginLeft(){
        marginLeft = SCORE_OFFSET;
        params.setMargins(marginLeft, 0, 0, 0);
    }

    public void changeSeekBar() {
        try {
            playAudioProgress.setProgress(mp.getCurrentPosition());
            timer.setText(convertTime(mp.getCurrentPosition()));
            if ((mp.getCurrentPosition() > START_RIGHT_HAND_120 && mp.getCurrentPosition() < 66000) || mp.getCurrentPosition() > 67000){
                params.setMargins(marginLeft, 0, 0, 0);
                marginLeft -= 5;
            } else {
                resetMarginLeft();
            }
            runnable = new Runnable() {
                @Override
                public void run() {
                    mp.setVolume(volume, volume);
                    changeSeekBar();

                }
            };
            audioProgressHandler.postDelayed(runnable, 1);
        } catch (NullPointerException ne) {}
    }

    public String convertTime(int d){
        String s = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(d),
                TimeUnit.MILLISECONDS.toSeconds(d)- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(d)));
        return s;
    }

    public void onBackPressed(){
        audioProgressHandler.removeCallbacks(runnable);
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
    }

    @Override
    public void onRestart(){
        super.onRestart();
        recreate();
    }
}
