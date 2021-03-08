package marcoalunno.com.arias_and_inteludes;

/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -------------------------------------------------------------
 *
 * TarsosDSP is developed by Joren Six at IPEM, University Ghent
 *
 * -------------------------------------------------------------
 *
 *  Info: http://0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */


import java.util.Random;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class CustomSineGenerator implements AudioProcessor{

    private double gain;
    private double frequency;
    private double phase;
    private double tempFrequency;
    private double vibrato;
    private boolean countUp = true;

    public CustomSineGenerator(){}

    public CustomSineGenerator(double gain, double frequency, double vibrato){
        this.gain = gain;
        this.frequency = frequency;
        this.phase = 0;
        this.vibrato = vibrato;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] buffer = audioEvent.getFloatBuffer();
        double sampleRate = audioEvent.getSampleRate();
        double twoPiF = 2 * Math.PI * frequency;
        double time;
        for (int i = 0; i < buffer.length; i++) {
            time = i / sampleRate;
            buffer[i] += (float) (gain * Math.sin(twoPiF * time + phase));
        }
        phase = twoPiF * buffer.length / sampleRate + phase;

        if(countUp){
            double r = (new Random().nextInt(1)+1) * vibrato; //generate 1s or 2s
            frequency+=r;
        } else {
            double r = (new Random().nextInt(1)+1) * vibrato;
            frequency-=r;
        }
        if(frequency > tempFrequency+vibrato){
            countUp = false;
        }
        if(frequency < tempFrequency-vibrato){
            countUp = true;
        }
        return true;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public void setTempFrequency(double tempFrequency) {
        this.tempFrequency = tempFrequency;
    }

    public void setVibrato(double vibrato) {
        this.vibrato = vibrato;
    }

    @Override
    public void processingFinished() {
    }
}
