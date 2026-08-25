package com.transcriber.workerservice.service;

import com.transcriber.workerservice.config.AudioCompressionProperties;
import com.transcriber.workerservice.exception.AudioCompressionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerAudioCompressionService {

    private static final long FFMPEG_TIMEOUT_MINUTES = 5;

    private final AudioCompressionProperties properties;

    public File compress(File rawFile) {
        double durationSeconds = readDurationSeconds(rawFile);
        int bitrateKbps = calculateBitrateKbps(durationSeconds);

        File outputFile = createTempOutputFile();
        runFfmpeg(rawFile, outputFile, bitrateKbps);

        int retries = 0;
        while (outputFile.length() > properties.getMaxOutputSizeBytes() && retries < 2) {
            int reducedBitrateKbps = Math.max(properties.getMinBitrateKbps(), bitrateKbps / 2);
            if (reducedBitrateKbps >= bitrateKbps) {
                break;
            }
            bitrateKbps = reducedBitrateKbps;
            log.info("Compressed file exceeds size limit ({} bytes), retrying with bitrate {}k",
                    outputFile.length(), bitrateKbps);
            runFfmpeg(rawFile, outputFile, bitrateKbps);
            retries++;
        }

        if (outputFile.length() > properties.getMaxOutputSizeBytes()) {
            throw new AudioCompressionException("Failed to compress audio below size limit after retries");
        }

        return outputFile;
    }

    private double readDurationSeconds(File rawFile) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                rawFile.getAbsolutePath()
        );

        try {
            Process process = processBuilder.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            boolean finished = process.waitFor(1, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new AudioCompressionException("ffprobe timed out");
            }
            if (process.exitValue() != 0) {
                throw new AudioCompressionException("Failed to read audio duration: " + stderr.trim());
            }
            if (stdout.isBlank()) {
                throw new AudioCompressionException("Failed to read audio duration");
            }
            return Double.parseDouble(stdout.trim());
        } catch (AudioCompressionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AudioCompressionException("Failed to read audio duration", ex);
        }
    }

    private int calculateBitrateKbps(double durationSeconds) {
        if (durationSeconds <= 0) {
            throw new AudioCompressionException("Failed to read audio duration");
        }

        long targetBits = properties.getMaxOutputSizeBytes() * 8;
        int calculatedBitrateKbps = (int) ((targetBits / durationSeconds / 1000) * properties.getBitrateSafetyFactor());
        return Math.max(
                properties.getMinBitrateKbps(),
                Math.min(properties.getMaxBitrateKbps(), calculatedBitrateKbps)
        );
    }

    private void runFfmpeg(File rawFile, File outputFile, int bitrateKbps) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", rawFile.getAbsolutePath(),
                "-vn",
                "-ac", "1",
                "-ar", String.valueOf(properties.getTargetSampleRateHz()),
                "-b:a", bitrateKbps + "k",
                "-f", "mp3",
                outputFile.getAbsolutePath()
        );

        try {
            Process process = processBuilder.start();
            String stderr = readStream(process.getErrorStream());
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new AudioCompressionException("ffmpeg timed out after " + FFMPEG_TIMEOUT_MINUTES + " minutes");
            }
            if (process.exitValue() != 0) {
                throw new AudioCompressionException(stderr.trim());
            }
        } catch (AudioCompressionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AudioCompressionException("ffmpeg compression failed", ex);
        }
    }

    private File createTempOutputFile() {
        try {
            return Files.createTempFile("audio-compressed-", ".mp3").toFile();
        } catch (Exception ex) {
            throw new AudioCompressionException("Failed to create temporary output file", ex);
        }
    }

    private String readStream(java.io.InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
