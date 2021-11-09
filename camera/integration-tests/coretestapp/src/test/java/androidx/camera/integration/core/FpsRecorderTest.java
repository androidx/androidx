/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.annotation.NonNull;

import com.google.common.truth.StandardSubjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class FpsRecorderTest {

    private static final int TEST_FPS = 30;
    private static final long TEST_FPS_DURATION_NANOS = 1_000_000_000L / TEST_FPS;
    private static final long TIMESTAMP_BASE_NANOS = 1234567654321L;
    private static final double FPS_ALLOWED_ERROR = 1e-4;

    @Test
    public void fpsIsCalculated_withSingleSampleBuffer() {
        FpsRecorder fpsRecorder = new FpsRecorder(1);
        fpsRecorder.recordTimestamp(TIMESTAMP_BASE_NANOS);
        double fps = fpsRecorder.recordTimestamp(TIMESTAMP_BASE_NANOS + TEST_FPS_DURATION_NANOS);
        assertThat(fps).isWithin(FPS_ALLOWED_ERROR).of(TEST_FPS);
    }

    @Test
    public void fpsIsNaN_untilBufferSizePlusOneSamples() {
        double[] expected = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, TEST_FPS};
        double[] actual = new double[expected.length];

        FpsRecorder fpsRecorder = new FpsRecorder(expected.length - 1);
        long timestamp = TIMESTAMP_BASE_NANOS;
        for (int i = 0; i < expected.length; ++i) {
            actual[i] = fpsRecorder.recordTimestamp(timestamp);
            timestamp += TEST_FPS_DURATION_NANOS;
        }

        // Cannot compare with withTolerance() since NaN is not considered equal to itself in
        // that case
        for (int i = 0; i < actual.length; ++i) {
            if (Double.isFinite(actual[i])) {
                assertThat(actual[i]).isWithin(FPS_ALLOWED_ERROR).of(expected[i]);
            } else {
                assertThat(actual[i]).isEqualTo(expected[i]);
            }
        }

        assertElementsWithinTolerance_ignoringNaN(actual, expected);
    }

    @Test
    public void fpsIsNaN_afterReset() {
        double[] expected =
                {Double.NaN, Double.NaN, Double.NaN, TEST_FPS, Double.NaN, Double.NaN, Double.NaN,
                        TEST_FPS};
        double[] actual = new double[expected.length];

        FpsRecorder fpsRecorder = new FpsRecorder(expected.length / 2 - 1);
        long timestamp = TIMESTAMP_BASE_NANOS;
        for (int i = 0; i < expected.length; ++i) {
            if (i == expected.length / 2) {
                fpsRecorder.reset();
            }

            actual[i] = fpsRecorder.recordTimestamp(timestamp);
            timestamp += TEST_FPS_DURATION_NANOS;
        }

        assertElementsWithinTolerance_ignoringNaN(actual, expected);
    }

    @Test
    public void fpsIsCalculated_withLargeBuffer() {
        int bufferSize = 100;
        FpsRecorder fpsRecorder = new FpsRecorder(bufferSize);
        long timeStamp = TIMESTAMP_BASE_NANOS;
        double fpsOut = Double.NaN;
        for (int i = 0; i < bufferSize + 1; ++i) {
            fpsOut = fpsRecorder.recordTimestamp(timeStamp);
            timeStamp += TEST_FPS_DURATION_NANOS;
        }

        assertThat(fpsOut).isWithin(FPS_ALLOWED_ERROR).of(TEST_FPS);
    }

    @Test
    public void skippedFrame_dropsFps() {
        int bufferSize = 4;
        FpsRecorder fpsRecorder = new FpsRecorder(bufferSize);
        long timeStamp = TIMESTAMP_BASE_NANOS;
        double fpsOut = Double.NaN;
        for (int i = 0; i < bufferSize + 1; ++i) {
            fpsOut = fpsRecorder.recordTimestamp(timeStamp);
            timeStamp += TEST_FPS_DURATION_NANOS;
            if (i == 1) {
                // Simulate dropped frame. Add additional duration to timestamp.
                timeStamp += TEST_FPS_DURATION_NANOS;
            }
        }

        assertThat(fpsOut).isNotWithin(FPS_ALLOWED_ERROR).of(TEST_FPS);
        assertThat(fpsOut).isLessThan(TEST_FPS);
    }

    private void assertElementsWithinTolerance_ignoringNaN(@NonNull double[] actual,
            @NonNull double[] expected) {
        // Cannot compare with withTolerance() since NaN is not considered equal to itself in
        // that case
        for (int i = 0; i < actual.length; ++i) {
            StandardSubjectBuilder assertWithMessage = assertWithMessage(
                    "Assumption violation while "
                            + "comparing actual: [%s] to expected: [%s] at position %s",
                    Arrays.toString(actual),
                    Arrays.toString(expected),
                    Integer.toString(i));
            if (Double.isFinite(actual[i])) {
                assertWithMessage.that(actual[i]).isWithin(FPS_ALLOWED_ERROR).of(expected[i]);
            } else {
                assertWithMessage.that(actual[i]).isEqualTo(expected[i]);
            }
        }
    }
}
