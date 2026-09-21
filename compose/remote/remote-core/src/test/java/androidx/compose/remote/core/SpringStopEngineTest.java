/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.utilities.easing.SpringStopEngine;

import org.junit.Test;

/** Tests for {@link SpringStopEngine}. */
public class SpringStopEngineTest {

    @Test
    public void stiffSpring_remainsStableAcrossLargeDtSteps() {
        float stiffness = 1400f;
        float damping = (float) (2.0 * Math.sqrt(stiffness));
        SpringStopEngine spring =
                new SpringStopEngine(
                        new float[] {0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)});
        spring.setInitialValue(0f);
        spring.setTargetValue(1f);

        // A large frame delta, such as an 80ms hitch, must not take the spring out of range.
        float pos80ms = spring.get(0.08f);
        assertTrue("Expected finite position in [0, 1], got " + pos80ms, Float.isFinite(pos80ms));
        assertTrue("Expected pos80ms >= 0f, got " + pos80ms, pos80ms >= 0f);
        assertTrue("Expected pos80ms <= 1.01f, got " + pos80ms, pos80ms <= 1.01f);

        // Jumping seconds ahead settles cleanly on the target.
        float posSettled = spring.get(2.0f);
        assertEquals(1f, posSettled, 0.001f);
        assertTrue(spring.isStopped());
    }

    /** A spring must stay inside its travel range at every stiffness and refresh rate. */
    @Test
    public void springStaysInRangeAcrossRefreshRatesAndStiffnesses() {
        float[] frameDeltasSec = {0.008f, 0.016f, 0.033f, 0.066f, 0.100f};
        float[] stiffnesses = {50f, 400f, 1400f, 3000f};

        for (float stiffness : stiffnesses) {
            float damping = (float) (2.0 * Math.sqrt(stiffness));
            for (float frameDelta : frameDeltasSec) {
                SpringStopEngine spring =
                        new SpringStopEngine(
                                new float[] {
                                    0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)
                                });
                spring.setInitialValue(0f);
                spring.setTargetValue(1f);

                float time = 0f;
                for (int frame = 0; frame < 120; frame++) {
                    time += frameDelta;
                    float pos = spring.get(time);
                    String where =
                            "stiffness="
                                    + stiffness
                                    + " frameDelta="
                                    + (frameDelta * 1000f)
                                    + "ms frame="
                                    + frame
                                    + " pos="
                                    + pos;
                    assertTrue("Expected finite position, " + where, Float.isFinite(pos));
                    assertTrue("Expected pos >= -0.01f, " + where, pos >= -0.01f);
                    assertTrue("Expected pos <= 1.01f, " + where, pos <= 1.01f);
                }

                // Critically damped: settled on the target well before frame 120.
                assertEquals(
                        "stiffness=" + stiffness + " frameDelta=" + (frameDelta * 1000f) + "ms",
                        1f,
                        spring.get(time),
                        0.01f);
            }
        }
    }

    /** A critically damped spring approaches its target monotonically and stops there. */
    @Test
    public void softSpringSettlesWithoutOvershoot() {
        float stiffness = 50f;
        float damping = (float) (2.0 * Math.sqrt(stiffness));
        SpringStopEngine spring =
                new SpringStopEngine(
                        new float[] {0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)});
        spring.setInitialValue(0f);
        spring.setTargetValue(1f);

        float previous = 0f;
        float time = 0f;
        for (int frame = 0; frame < 120; frame++) {
            time += 0.016f;
            float pos = spring.get(time);
            assertTrue("Critically damped spring should not overshoot, got " + pos, pos <= 1.001f);
            assertTrue("Critically damped spring should not reverse, got " + pos, pos >= previous);
            previous = pos;
        }
        assertEquals(1f, spring.get(time), 0.001f);
        assertTrue(spring.isStopped());
    }

    /**
     * A spring is a function of elapsed time, not of how often it is sampled: stepped at 8ms or at
     * 100ms it must reach the same value at the same instant.
     */
    @Test
    public void springTrajectoryIsIndependentOfFrameRate() {
        float[] frameDeltasSec = {0.008f, 0.016f, 0.033f, 0.066f, 0.100f};
        float[] stiffnesses = {50f, 400f, 1400f, 3000f};
        float[] dampingRatios = {0.3f, 1.0f, 1.5f};
        float[] checkpointsSec = {0.05f, 0.1f, 0.25f, 0.5f, 1.0f};

        for (float stiffness : stiffnesses) {
            for (float dampingRatio : dampingRatios) {
                float damping = dampingRatio * (float) (2.0 * Math.sqrt(stiffness));
                float[] spring = {0f, stiffness, damping, 1e-5f, Float.intBitsToFloat(0)};

                float[][] traces = new float[frameDeltasSec.length][];
                for (int i = 0; i < frameDeltasSec.length; i++) {
                    traces[i] = sampleAtCheckpoints(spring, checkpointsSec, frameDeltasSec[i]);
                }

                for (int c = 0; c < checkpointsSec.length; c++) {
                    float min = Float.MAX_VALUE;
                    float max = -Float.MAX_VALUE;
                    for (float[] trace : traces) {
                        min = Math.min(min, trace[c]);
                        max = Math.max(max, trace[c]);
                    }
                    assertTrue(
                            "stiffness="
                                    + stiffness
                                    + " dampingRatio="
                                    + dampingRatio
                                    + " t="
                                    + checkpointsSec[c]
                                    + "s varied by "
                                    + (max - min)
                                    + " across frame rates ["
                                    + min
                                    + ", "
                                    + max
                                    + "]",
                            max - min < FRAME_RATE_TOLERANCE);
                }
            }
        }
    }

    /**
     * The integrator must track the exact solution of m*x'' + c*x' + k*x = 0, not merely stay in
     * range: an approximation that lags or damps too hard would pass the bounds checks above while
     * changing every animation on screen. Covers every spring the repo ships, under- to
     * over-damped.
     */
    @Test
    public void springMatchesClosedFormSolution() {
        // {stiffness, dampingRatio} per spring in the repo, plus an overdamped case.
        float[][] springs = {
            {1400f, 1.0f}, // Wear Material3 selection controls
            {50f, 1.0f}, // curved progress indicator, remoteSpring() default
            {30f, 0.91f}, // particle demos
            {3f, 0.87f}, // compass needle
            {3f, 0.29f}, // light sensor, strongly underdamped
            {1f, 0.5f}, // game ship position
            {400f, 1.5f}, // overdamped
        };
        float[] frameDeltasSec = {0.016f, 0.033f, 0.100f};

        for (float[] parameters : springs) {
            float stiffness = parameters[0];
            float dampingRatio = parameters[1];
            double omega = Math.sqrt(stiffness); // mass is 1
            float damping = dampingRatio * (float) (2.0 * omega);

            for (float frameDelta : frameDeltasSec) {
                SpringStopEngine spring =
                        new SpringStopEngine(
                                new float[] {
                                    0f, stiffness, damping, 1e-5f, Float.intBitsToFloat(0)
                                });
                spring.setInitialValue(0f);
                spring.setTargetValue(1f);

                float maxError = 0f;
                float worstTime = 0f;
                for (float time = frameDelta; time <= 3f; time += frameDelta) {
                    float error =
                            Math.abs(
                                    spring.get(time) - (float) unitStep(time, omega, dampingRatio));
                    if (error > maxError) {
                        maxError = error;
                        worstTime = time;
                    }
                }
                assertTrue(
                        "stiffness="
                                + stiffness
                                + " dampingRatio="
                                + dampingRatio
                                + " frameDelta="
                                + (frameDelta * 1000f)
                                + "ms deviated from the closed form by "
                                + maxError
                                + " at t="
                                + worstTime
                                + "s",
                        maxError < CLOSED_FORM_TOLERANCE);
            }
        }
    }

    /** Largest spread allowed between frame rates reading the same spring at the same instant. */
    private static final float FRAME_RATE_TOLERANCE = 0.001f;

    /** Largest deviation allowed from the exact solution, as a fraction of the travel. */
    private static final float CLOSED_FORM_TOLERANCE = 0.002f;

    /** Step at a fixed frame delta, landing exactly on each checkpoint so samples line up. */
    private static float[] sampleAtCheckpoints(
            float[] springParameters, float[] checkpointsSec, float frameDelta) {
        SpringStopEngine spring = new SpringStopEngine(springParameters.clone());
        spring.setInitialValue(0f);
        spring.setTargetValue(1f);

        float[] values = new float[checkpointsSec.length];
        float time = 0f;
        for (int i = 0; i < checkpointsSec.length; i++) {
            while (time + frameDelta < checkpointsSec[i]) {
                time += frameDelta;
                spring.get(time);
            }
            time = checkpointsSec[i];
            values[i] = spring.get(time);
        }
        return values;
    }

    /**
     * Exact unit step response of a mass-1 spring.
     *
     * @param time seconds since the step
     * @param omega undamped natural frequency, sqrt(stiffness / mass)
     * @param zeta damping ratio: below 1 oscillates, 1 is critical, above 1 is overdamped
     */
    private static double unitStep(double time, double omega, double zeta) {
        if (time <= 0) {
            return 0;
        }
        if (zeta < 1) {
            double damped = omega * Math.sqrt(1 - zeta * zeta);
            return 1
                    - Math.exp(-zeta * omega * time)
                            * (Math.cos(damped * time)
                                    + zeta / Math.sqrt(1 - zeta * zeta) * Math.sin(damped * time));
        }
        if (zeta == 1) {
            return 1 - Math.exp(-omega * time) * (1 + omega * time);
        }
        double root = Math.sqrt(zeta * zeta - 1);
        double slow = -omega * (zeta - root);
        double fast = -omega * (zeta + root);
        return 1 - (slow * Math.exp(fast * time) - fast * Math.exp(slow * time)) / (slow - fast);
    }
}
