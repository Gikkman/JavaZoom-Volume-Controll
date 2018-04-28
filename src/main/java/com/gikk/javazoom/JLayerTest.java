package com.gikk.javazoom;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Scanner;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.*;

public class JLayerTest
{
    public static void main(String[] args)
    {
        SoundJLayer soundToPlay = new SoundJLayer("http://relay-ams.gameowls.com:80/chiptune.mp3");
        soundToPlay.play();
    }
}

class SoundJLayer extends PlaybackListener implements Runnable
{
    private final String url;
    private AdvancedPlayer player;
    private Thread playerThread;
    private AudioDevice device;
    private FloatControl volControl;

    public SoundJLayer(String url)
    {
        this.url = url;
    }

    public void play()
    {
        try
        {
            InputStream stream = new URL(url).openStream();

            JavaSoundAudioDevice a = new JavaSoundAudioDevice();

            this.device = FactoryRegistry.systemRegistry().createAudioDevice();
            this.player = new AdvancedPlayer( stream, device);

            this.player.setPlayBackListener(this);
            this.playerThread = new Thread(this, "AudioPlayerThread");
            this.playerThread.start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String line = "";
        try (Scanner scanner = new Scanner(System.in)) {
            while(!line.equals("q")){
                line = scanner.nextLine();

                if(line.matches("^(-?)(0|([1-9][0-9]*))(\\.[0-9]+)?$")) {
                    setVolume(Float.valueOf(line));
                }
            }
            this.player.stop();
        }
    }

    // PlaybackListener members
    @Override
    public void playbackStarted(PlaybackEvent playbackEvent)
    {
        System.out.println("playbackStarted()");
    }

    @Override
    public void playbackFinished(PlaybackEvent playbackEvent)
    {
        System.out.println("playbackEnded()");
    }

    public void setVolume(float gain){
        if(this.volControl == null) {
            Class<JavaSoundAudioDevice> clazz = JavaSoundAudioDevice.class;
            Field[] fields = clazz.getDeclaredFields();
            try{
                SourceDataLine source = null;
                for(Field field : fields) {
                    if("source".equals(field.getName())) {
                        field.setAccessible(true);
                        source = (SourceDataLine) field.get(this.device);
                        field.setAccessible(false);
                        this.volControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.volControl != null) {
            float newGain = Math.min(Math.max(gain, volControl.getMinimum()), volControl.getMaximum());
            System.out.println("Was: " + volControl.getValue() + " Will be: " + newGain);

            volControl.setValue(newGain);
        }
    }

    // Runnable members
    @Override
    public void run()
    {
        try
        {
            this.player.play();
        }
        catch (JavaLayerException ex)
        {
            ex.printStackTrace();
        }
    }
}