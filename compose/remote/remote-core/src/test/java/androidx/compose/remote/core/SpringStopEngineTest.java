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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.operations.utilities.easing.SpringStopEngine;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Tests for {@link SpringStopEngine} and spring-backed {@link FloatExpression}. */
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

    @Test
    public void floatExpressionSpring_syncsTimeBeforeTargetChangeAfterIdlePeriod() {
        float stiffness = 1400f;
        float damping = (float) (2.0 * Math.sqrt(stiffness));
        int inputVarId = 1;
        int springVarId = 2;

        FloatExpression expr =
                new FloatExpression(
                        springVarId,
                        new float[] {Utils.asNan(inputVarId)},
                        new float[] {0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)});

        FakeRemoteContext context = new FakeRemoteContext();
        context.loadFloat(inputVarId, 0f);

        // Startup frame.
        context.setAnimationTime(0.016f);
        expr.updateVariables(context);
        expr.apply(context);
        assertEquals(0f, context.getFloat(springVarId), 0.001f);

        // Five seconds with no frames painted, then the input toggles.
        context.setAnimationTime(5.000f);
        context.loadFloat(inputVarId, 1f);
        expr.updateVariables(context);
        expr.apply(context);

        // The spring has only just been retargeted, so it is still at 0 and asks for frames.
        float valueAtToggle = context.getFloat(springVarId);
        assertEquals(0f, valueAtToggle, 0.01f);
        assertTrue(expr.isDirty());
        assertTrue(context.mRepaintRequested);

        // One 16ms frame later it is under way, not snapped to the target.
        context.mRepaintRequested = false;
        context.setAnimationTime(5.016f);
        expr.updateVariables(context);
        expr.apply(context);
        float valueFrame1 = context.getFloat(springVarId);
        assertTrue(
                "Expected 0f < valueFrame1 < 1f, got " + valueFrame1,
                valueFrame1 > 0.05f && valueFrame1 < 0.95f);
        assertTrue(expr.isDirty());
        assertTrue(context.mRepaintRequested);

        // Run on until settled.
        for (float t = 5.032f; t <= 5.500f; t += 0.016f) {
            context.setAnimationTime(t);
            expr.updateVariables(context);
            expr.apply(context);
        }
        assertEquals(1f, context.getFloat(springVarId), 0.01f);
    }

    /**
     * A spring retargeted every frame, such as a needle damping towards a live bearing, is already
     * stepped by apply(), so updateVariables() must not step it again: that would integrate the
     * frame against the previous target and cost a frame of latency. Driven with a 0.5Hz sweep, it
     * must track an undelayed reference rather than one stepped a frame behind.
     */
    @Test
    public void floatExpressionSpring_continuouslyRetargeted_doesNotLagByAFrame() {
        float stiffness = 50f;
        float damping = 0.3f * (float) (2.0 * Math.sqrt(stiffness));
        int inputVarId = 1;
        int springVarId = 2;
        float[] springSpec = {0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)};

        FloatExpression expr =
                new FloatExpression(
                        springVarId, new float[] {Utils.asNan(inputVarId)}, springSpec.clone());
        FakeRemoteContext context = new FakeRemoteContext();

        // Startup frame: everything begins settled at 0.
        context.loadFloat(inputVarId, 0f);
        context.setAnimationTime(0f);
        expr.updateVariables(context);
        expr.apply(context);

        SpringStopEngine undelayed = newSettledSpring(springSpec);
        SpringStopEngine delayedByAFrame = newSettledSpring(springSpec);

        float maxDeltaVsUndelayed = 0f;
        float maxDeltaVsDelayed = 0f;
        for (int frame = 1; frame <= 180; frame++) {
            float t = frame / 60f;
            float target = (float) Math.sin(2 * Math.PI * 0.5 * t);

            context.setAnimationTime(t);
            context.loadFloat(inputVarId, target);
            expr.updateVariables(context);
            expr.apply(context);
            float actual = context.getFloat(springVarId);

            // Retarget, then step the frame: the new target applies to this frame.
            undelayed.setTargetValue(target);
            float undelayedValue = undelayed.get(t);

            // Step the frame first, then retarget: the new target only applies next frame.
            delayedByAFrame.get(t);
            delayedByAFrame.setTargetValue(target);
            float delayedValue = delayedByAFrame.get(t);

            maxDeltaVsUndelayed = Math.max(maxDeltaVsUndelayed, Math.abs(actual - undelayedValue));
            maxDeltaVsDelayed = Math.max(maxDeltaVsDelayed, Math.abs(actual - delayedValue));
        }

        assertTrue(
                "Driven spring should track the undelayed reference, max delta was "
                        + maxDeltaVsUndelayed,
                maxDeltaVsUndelayed < 0.005f);
        assertTrue(
                "Driven spring should not match a one-frame-delayed reference, max delta was "
                        + maxDeltaVsDelayed,
                maxDeltaVsDelayed > 0.02f);
    }

    /**
     * A settled spring must stop asking for frames: once it reaches its target and the input stops
     * changing, apply() must neither request a repaint nor mark itself dirty.
     */
    @Test
    public void floatExpressionSpring_settled_stopsRequestingFrames() {
        float stiffness = 1400f;
        float damping = (float) (2.0 * Math.sqrt(stiffness));
        int inputVarId = 1;
        int springVarId = 2;

        FloatExpression expr =
                new FloatExpression(
                        springVarId,
                        new float[] {Utils.asNan(inputVarId)},
                        new float[] {0f, stiffness, damping, 0.001f, Float.intBitsToFloat(0)});

        FakeRemoteContext context = new FakeRemoteContext();
        context.loadFloat(inputVarId, 0f);
        context.setAnimationTime(0f);
        expr.updateVariables(context);
        expr.apply(context);

        // One second of frames after the input toggles: the spring animates.
        context.loadFloat(inputVarId, 1f);
        boolean requestedWhileMoving = false;
        for (int frame = 1; frame <= 60; frame++) {
            context.mRepaintRequested = false;
            expr.markNotDirty();
            context.setAnimationTime(frame / 60f);
            expr.updateVariables(context);
            expr.apply(context);
            requestedWhileMoving |= context.mRepaintRequested;
        }
        assertTrue("Expected repaints while the spring was moving", requestedWhileMoving);
        assertEquals(1f, context.getFloat(springVarId), 0.001f);

        // A further second with the input unchanged must be completely quiet.
        for (int frame = 61; frame <= 120; frame++) {
            float time = frame / 60f;
            context.mRepaintRequested = false;
            expr.markNotDirty();
            context.setAnimationTime(time);
            expr.updateVariables(context);
            expr.apply(context);
            assertFalse(
                    "Settled spring requested a repaint at t=" + time, context.mRepaintRequested);
            assertFalse("Settled spring marked itself dirty at t=" + time, expr.isDirty());
            assertEquals(1f, context.getFloat(springVarId), 0.0001f);
        }
    }

    /** A spring sitting settled at 0 with its clock synced to t = 0. */
    private static SpringStopEngine newSettledSpring(float[] springSpec) {
        SpringStopEngine spring = new SpringStopEngine(springSpec.clone());
        spring.setInitialValue(0f);
        spring.setTargetValue(0f);
        spring.get(0f);
        return spring;
    }

    private static class FakeRemoteContext extends RemoteContext {
        private final Map<Integer, Float> mFloats = new HashMap<>();
        boolean mRepaintRequested = false;

        FakeRemoteContext() {
            mRemoteComposeState =
                    new RemoteComposeState() {
                        @Override
                        public @Nullable float[] getFloats(int id) {
                            return null;
                        }
                    };
        }

        @Override
        public void needsRepaint() {
            mRepaintRequested = true;
        }

        @Override
        public void loadFloat(int id, float value) {
            mFloats.put(id, value);
        }

        @Override
        public float getFloat(int id) {
            return mFloats.getOrDefault(id, 0f);
        }

        @Override
        public @NonNull CollectionsAccess getCollectionsAccess() {
            return mRemoteComposeState;
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {}

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return null;
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {}

        @Override
        public void loadColor(int id, int color) {}

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {}

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {}

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {}

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {}

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {}

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {}

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {}

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {}

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {}

        @Override
        public void setNamedLong(@NonNull String name, long value) {}

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {}

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {}

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {}

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {}

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return null;
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {}

        @Override
        public void runNamedAction(int id, @Nullable Object value) {}

        @Override
        public void putObject(int id, @NonNull Object value) {}

        @Override
        public @Nullable Object getObject(int id) {
            return null;
        }

        @Override
        public void hapticEffect(int type) {}

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {}

        @Override
        public void loadText(int id, @NonNull String text) {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void overrideFloat(int id, float value) {}

        @Override
        public void loadInteger(int id, int value) {}

        @Override
        public void overrideInteger(int id, int value) {}

        @Override
        public void overrideText(int id, int valueId) {}

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {}

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {}

        @Override
        public int getInteger(int id) {
            return 0;
        }

        @Override
        public long getLong(int id) {
            return 0;
        }

        @Override
        public int getColor(int id) {
            return 0;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public @Nullable ShaderData getShader(int id) {
            return null;
        }

        @Override
        public void addClickArea(
                int id,
                int contentDescriptionId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {}
    }
}
