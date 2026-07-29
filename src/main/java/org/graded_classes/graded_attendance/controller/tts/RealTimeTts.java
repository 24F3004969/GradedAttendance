package org.graded_classes.graded_attendance.controller.tts;

import com.k2fsa.sherpa.onnx.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RealTimeTts {

    private static SourceDataLine line;
    OfflineTts tts;

    public void init() throws Exception {
        Path modelDir;

        Path devModelDir = Paths.get(
                System.getProperty("user.dir"),
                "models",
                "vits-piper-en_US-lessac-medium"
        );

        if (Files.exists(devModelDir)) {
            // Running from IntelliJ
            modelDir = devModelDir;
        } else {
            // Running from packaged app
            Path appDir = Paths.get(
                            RealTimeTts.class.getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI())
                    .getParent();

            modelDir = appDir
                    .resolve("models")
                    .resolve("vits-piper-en_US-lessac-medium");
        }

        System.out.println("Using model dir: " + modelDir);

        OfflineTtsVitsModelConfig vitsConfig =
                OfflineTtsVitsModelConfig.builder()
                        .setModel(modelDir.resolve("en_US-lessac-medium.onnx").toString())
                        .setTokens(modelDir.resolve("tokens.txt").toString())
                        .setDataDir(modelDir.resolve("espeak-ng-data").toString())
                        .build();
        OfflineTtsModelConfig modelConfig =
                OfflineTtsModelConfig.builder()
                        .setVits(vitsConfig)
                        .setNumThreads(Runtime.getRuntime().availableProcessors())
                        .setDebug(false)
                        .build();

        OfflineTtsConfig config =
                OfflineTtsConfig.builder()
                        .setModel(modelConfig)
                        .build();

        tts = new OfflineTts(config);
    }

    public void readAloud(String text) throws LineUnavailableException {
        boolean audioInitialized = false;
        GeneratedAudio audio =
                tts.generateWithCallback(
                        text,
                        0,
                        1.0f,
                        samples -> 1
                );

        if (!audioInitialized) {

            AudioFormat format =
                    new AudioFormat(
                            audio.getSampleRate(),
                            16,
                            1,
                            true,
                            false
                    );

            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            playAudio(audio);
        }

        line.drain();
        line.stop();
        line.close();

        tts.release();
    }

    private static void playAudio(GeneratedAudio audio) {

        float[] samples = audio.getSamples();

        byte[] pcm = new byte[samples.length * 2];

        int index = 0;

        for (float sample : samples) {

            short value = (short) (sample * 32767);

            pcm[index++] = (byte) (value & 0xff);
            pcm[index++] = (byte) ((value >> 8) & 0xff);
        }

        line.write(pcm, 0, pcm.length);
    }
}