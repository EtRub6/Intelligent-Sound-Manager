package com.ethanr.intelligentsoundmanager;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CPUFrequencyReader {
    private static final String TAG = "intelli_sound:" + CPUFrequencyReader.class.getSimpleName();
    private static final String CPU_FREQ_PATH = "/sys/devices/system/cpu/cpu";
    private static final String CPU_FREQ_FILE = "/cpufreq/scaling_cur_freq";

    /**
     * Returns the total number of logical CPU cores available on the system.
     * @return The number of logical cores.
     */
    public static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Reads the current CPU frequency for a specific core.
     *
     * @param core The core number (e.g., 0, 1, 2).
     * @return The current frequency in MHz, or -1 if the frequency cannot be read.
     */
    public static int getCpuFrequency(int core) {
        if (core < 0 || core >= getNumberOfCores()) {
            Log.e(TAG, "Invalid core number: " + core);
            return -1;
        }

        String filePath = CPU_FREQ_PATH + core + CPU_FREQ_FILE;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line != null) {
                // The frequency is in KHz, so we convert it to MHz by dividing by 1000.
                int frequencyKHz = Integer.parseInt(line.trim());
                int frequencyMHz = frequencyKHz / 1000;
                Log.d(TAG, "Core " + core + " frequency: " + frequencyMHz + " MHz");
                return frequencyMHz;
            }
        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, "Could not read frequency for core " + core + ": " + e.getMessage());
        }
        return -1;
    }

    /**
     * Reads the current CPU frequency for all available cores.
     * @return A map where the key is the core number and the value is the
     * current frequency in MHz. Returns an empty map if an error occurs.
     */
    public static Map<Integer, Integer> getCpuFrequencies() {
        Map<Integer, Integer> frequencies = new HashMap<>();
        int numberOfCores = getNumberOfCores();

        Log.d(TAG, "Number of cores detected: " + numberOfCores);

        for (int core = 0; core < numberOfCores; core++) {
            int frequency = getCpuFrequency(core);
            if (frequency != -1) {
                frequencies.put(core, frequency);
            }
        }
        return frequencies;
    }
}
