/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark.macro;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Collects jank metrics for all or a list of processes. */
class JankCollectionHelper {

    private static final String LOG_TAG = JankCollectionHelper.class.getSimpleName();

    // Prefix for all output metrics that come from the gfxinfo dump.
    @VisibleForTesting static final String GFXINFO_METRICS_PREFIX = "gfxinfo";
    // Shell dump commands to get and reset the tracked gfxinfo metrics.
    @VisibleForTesting static final String GFXINFO_COMMAND_GET = "dumpsys gfxinfo %s";
    @VisibleForTesting static final String GFXINFO_COMMAND_RESET = GFXINFO_COMMAND_GET + " reset";
    // Pattern matchers and enumerators to verify and pull gfxinfo metrics.
    // Example: "** Graphics info for pid 853 [com.google.android.leanbacklauncher] **"
    private static final String GFXINFO_OUTPUT_HEADER = "Graphics info for pid (\\d+) \\[(%s)\\]";
    // Note: use the [\\s\\S]* multi-line matcher to support String#matches(). Instead of splitting
    // the larger sections into more granular lines, we can match across all lines for simplicity.
    private static final String MULTILINE_MATCHER = "[\\s\\S]*%s[\\s\\S]*";

    public enum GfxInfoMetric {
        // Example: "Total frames rendered: 20391"
        TOTAL_FRAMES(
                Pattern.compile(".*Total frames rendered: (\\d+).*", Pattern.DOTALL),
                1,
                "total_frames"),
        // Example: "Janky frames: 785 (3.85%)"
        JANKY_FRAMES_COUNT(
                Pattern.compile(
                        ".*Janky frames: (\\d+) \\(([0-9]+[\\.]?[0-9]+)\\%\\).*", Pattern.DOTALL),
                1,
                "janky_frames_count"),
        // Example: "Janky frames: 785 (3.85%)"
        JANKY_FRAMES_PRCNT(
                Pattern.compile(
                        ".*Janky frames: (\\d+) \\(([0-9]+[\\.]?[0-9]+)\\%\\).*", Pattern.DOTALL),
                2,
                "janky_frames_percent"),
        // Example: "Janky frames (legacy): 785 (3.85%)"
        JANKY_FRAMES_LEGACY_COUNT(
                Pattern.compile(
                        ".*Janky frames \\(legacy\\): (\\d+) \\(([0-9]+[\\.]?[0-9]+)\\%\\).*",
                        Pattern.DOTALL),
                1,
                "janky_frames_legacy_count"),
        // Example: "Janky frames (legacy): 785 (3.85%)"
        JANKY_FRAMES_LEGACY_PRCNT(
                Pattern.compile(
                        ".*Janky frames \\(legacy\\): (\\d+) \\(([0-9]+[\\.]?[0-9]+)\\%\\).*",
                        Pattern.DOTALL),
                2,
                "janky_frames_legacy_percent"),
        // Example: "50th percentile: 9ms"
        FRAME_TIME_50TH(
                Pattern.compile(".*50th percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "frame_render_time_percentile_50"),
        // Example: "90th percentile: 9ms"
        FRAME_TIME_90TH(
                Pattern.compile(".*90th percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "frame_render_time_percentile_90"),
        // Example: "95th percentile: 9ms"
        FRAME_TIME_95TH(
                Pattern.compile(".*95th percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "frame_render_time_percentile_95"),
        // Example: "99th percentile: 9ms"
        FRAME_TIME_99TH(
                Pattern.compile(".*99th percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "frame_render_time_percentile_99"),
        // Example: "Number Missed Vsync: 0"
        NUM_MISSED_VSYNC(
                Pattern.compile(".*Number Missed Vsync: (\\d+).*", Pattern.DOTALL),
                1,
                "missed_vsync"),
        // Example: "Number High input latency: 0"
        NUM_HIGH_INPUT_LATENCY(
                Pattern.compile(".*Number High input latency: (\\d+).*", Pattern.DOTALL),
                1,
                "high_input_latency"),
        // Example: "Number Slow UI thread: 0"
        NUM_SLOW_UI_THREAD(
                Pattern.compile(".*Number Slow UI thread: (\\d+).*", Pattern.DOTALL),
                1,
                "slow_ui_thread"),
        // Example: "Number Slow bitmap uploads: 0"
        NUM_SLOW_BITMAP_UPLOADS(
                Pattern.compile(".*Number Slow bitmap uploads: (\\d+).*", Pattern.DOTALL),
                1,
                "slow_bmp_upload"),
        // Example: "Number Slow issue draw commands: 0"
        NUM_SLOW_DRAW(
                Pattern.compile(".*Number Slow issue draw commands: (\\d+).*", Pattern.DOTALL),
                1,
                "slow_issue_draw_cmds"),
        // Example: "Number Frame deadline missed: 0"
        NUM_FRAME_DEADLINE_MISSED(
                Pattern.compile(".*Number Frame deadline missed: (\\d+).*", Pattern.DOTALL),
                1,
                "deadline_missed"),
        // Number Frame deadline missed (legacy): 0
        NUM_FRAME_DEADLINE_MISSED_LEGACY(
                Pattern.compile(
                        ".*Number Frame deadline missed \\(legacy\\): (\\d+).*", Pattern.DOTALL),
                1,
                "deadline_missed_legacy"),
        // Example: "50th gpu percentile: 9ms"
        GPU_FRAME_TIME_50TH(
                Pattern.compile(".*50th gpu percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "gpu_frame_render_time_percentile_50"),
        // Example: "90th gpu percentile: 9ms"
        GPU_FRAME_TIME_90TH(
                Pattern.compile(".*90th gpu percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "gpu_frame_render_time_percentile_90"),
        // Example: "95th gpu percentile: 9ms"
        GPU_FRAME_TIME_95TH(
                Pattern.compile(".*95th gpu percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "gpu_frame_render_time_percentile_95"),
        // Example: "99th gpu percentile: 9ms"
        GPU_FRAME_TIME_99TH(
                Pattern.compile(".*99th gpu percentile: (\\d+)ms.*", Pattern.DOTALL),
                1,
                "gpu_frame_render_time_percentile_99");

        private final Pattern mPattern;
        private final int mGroupIndex;
        private final String mMetricId;

        GfxInfoMetric(Pattern pattern, int groupIndex, String metricId) {
            mPattern = pattern;
            mGroupIndex = groupIndex;
            mMetricId = metricId;
        }

        @Nullable
        public Double parse(@NonNull String lines) {
            Matcher matcher = mPattern.matcher(lines);
            if (matcher.matches()) {
                return Double.valueOf(matcher.group(mGroupIndex));
            } else {
                return null;
            }
        }

        @NonNull
        public String getMetricId() {
            return mMetricId;
        }
    }

    private final Set<String> mTrackedPackages = new HashSet<>();
    private UiDevice mDevice;

    /** Clear existing jank metrics, unless explicitly configured. */
    public boolean startCollecting() {
        if (mTrackedPackages.isEmpty()) {
            clearGfxInfo();
        } else {
            int exceptionCount = 0;
            Exception lastException = null;
            for (String pkg : mTrackedPackages) {
                try {
                    clearGfxInfo(pkg);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Encountered exception resetting gfxinfo.", e);
                    lastException = e;
                    exceptionCount++;
                }
            }
            // Throw exceptions after to not quit on a single failure.
            if (exceptionCount > 1) {
                throw new RuntimeException(
                        "Multiple exceptions were encountered resetting gfxinfo. Reporting the last"
                                + " one only; others are visible in logs.",
                        lastException);
            } else if (exceptionCount == 1) {
                throw new RuntimeException(
                        "Encountered exception resetting gfxinfo.", lastException);
            }
        }
        // No exceptions denotes success.
        return true;
    }

    /** Collect the {@code gfxinfo} metrics for tracked processes (or all, if unspecified). */
    @NonNull
    public Map<String, Double> getMetrics() {
        Map<String, Double> result = new HashMap<>();
        if (mTrackedPackages.isEmpty()) {
            result.putAll(getGfxInfoMetrics());
        } else {
            int exceptionCount = 0;
            Exception lastException = null;
            for (String pkg : mTrackedPackages) {
                try {
                    result.putAll(getGfxInfoMetrics(pkg));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Encountered exception getting gfxinfo.", e);
                    lastException = e;
                    exceptionCount++;
                }
            }
            // Throw exceptions after to ensure all failures are reported. The metrics will still
            // not be collected at this point, but it will possibly make the issue cause clearer.
            if (exceptionCount > 1) {
                throw new RuntimeException(
                        "Multiple exceptions were encountered getting gfxinfo. Reporting the last"
                                + " one only; others are visible in logs.",
                        lastException);
            } else if (exceptionCount == 1) {
                throw new RuntimeException("Encountered exception getting gfxinfo.", lastException);
            }
        }
        return result;
    }

    /** Do nothing, because nothing is needed to disable jank. */
    public boolean stopCollecting() {
        return true;
    }

    /** Add a package or list of packages to be tracked. */
    public void addTrackedPackages(@NonNull String... packages) {
        Collections.addAll(mTrackedPackages, packages);
    }

    /** Clear the {@code gfxinfo} for all packages. */
    @VisibleForTesting
    void clearGfxInfo() {
        // Not specifying a package will clear everything.
        clearGfxInfo("");
    }

    /** Clear the {@code gfxinfo} for the {@code pkg} specified. */
    @VisibleForTesting
    void clearGfxInfo(String pkg) {
        try {
            if (pkg.isEmpty()) {
                String command = String.format(GFXINFO_COMMAND_RESET, "--");
                String output = getDevice().executeShellCommand(command);
                // Success if any header (set by passing an empty-string) exists in the output.
                verifyMatches(output, getHeaderMatcher(""), "No package headers in output.");
                Log.v(LOG_TAG, "Cleared all gfxinfo.");
            } else {
                String command = String.format(GFXINFO_COMMAND_RESET, pkg);
                String output = getDevice().executeShellCommand(command);
                // Success if the specified package header exists in the output.
                verifyMatches(output, getHeaderMatcher(pkg), "No package header in output.");
                Log.v(LOG_TAG, String.format("Cleared %s gfxinfo.", pkg));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear gfxinfo.", e);
        }
    }

    /** Return a {@code Map<String, Double>} of {@code gfxinfo} metrics for all processes. */
    @VisibleForTesting
    Map<String, Double> getGfxInfoMetrics() {
        return getGfxInfoMetrics("");
    }

    /** Return a {@code Map<String, Double>} of {@code gfxinfo} metrics for {@code pkg}. */
    @VisibleForTesting
    @SuppressWarnings("StringSplitter")
    Map<String, Double> getGfxInfoMetrics(String pkg) {
        try {
            String command = String.format(GFXINFO_COMMAND_GET, pkg);
            String output = getDevice().executeShellCommand(command);
            verifyMatches(output, getHeaderMatcher(pkg), "Missing package header.");
            // Split each new section starting with two asterisks '**', and then query and append
            // all metrics. This method supports both single-package and multi-package outputs.
            String[] pkgMetricSections = output.split("\n\\*\\*");
            Map<String, Double> result = new HashMap<>();
            // Skip the 1st section, which contains only header information.
            for (int i = 1; i < pkgMetricSections.length; i++) {
                result.putAll(parseGfxInfoMetrics(pkgMetricSections[i]));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get gfxinfo.", e);
        }
    }

    /** Parse the {@code output} of {@code gfxinfo} to a {@code Map<String, Double>} of metrics. */
    private Map<String, Double> parseGfxInfoMetrics(String output) {
        Matcher header = Pattern.compile(getHeaderMatcher("")).matcher(output);
        if (!header.matches()) {
            throw new RuntimeException("Failed to parse package from gfxinfo output.");
        }
        // Package name is the only required field.
        String packageName = header.group(2);
        Log.v(LOG_TAG, String.format("Collecting metrics for: %s", packageName));
        // Parse each metric from the results via a common pattern.
        Map<String, Double> results = new HashMap<String, Double>();
        for (GfxInfoMetric metric : GfxInfoMetric.values()) {
            String metricKey =
                    constructKey(GFXINFO_METRICS_PREFIX, packageName, metric.getMetricId());
            // Find the metric or log that it's missing.
            Double value = metric.parse(output);
            if (value == null) {
                Log.d(LOG_TAG, String.format("Did not find %s from %s", metricKey, packageName));
            } else {
                results.put(metricKey, value);
            }
        }
        return results;
    }

    private String constructKey(@NonNull String ...tokens) {
        return TextUtils.join("_", tokens);
    }

    /**
     * Returns a matcher {@code String} for {@code pkg}'s {@code gfxinfo} headers.
     *
     * <p>Note: {@code pkg} may be empty.
     */
    private String getHeaderMatcher(String pkg) {
        return String.format(
                MULTILINE_MATCHER,
                String.format(GFXINFO_OUTPUT_HEADER, (pkg.isEmpty() ? ".*" : pkg)));
    }

    /** Verify the {@code output} matches {@code match}, or throw if not. */
    private void verifyMatches(String output, String match, String message, Object... args) {
        if (!output.matches(match)) {
            throw new IllegalStateException(String.format(message, args));
        }
    }

    /** Returns the {@link UiDevice} under test. */
    @NonNull
    @VisibleForTesting
    protected UiDevice getDevice() {
        if (mDevice == null) {
            mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        }
        return mDevice;
    }
}
