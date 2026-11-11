/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.dynamicanimation.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.concurrent.TimeUnit;

/**
 * Robolectric tests verifying {@link AnimationHandler} and {@link DynamicAnimation} isolation
 * across tests when {@link SystemClock#uptimeMillis()} resets while {@link AnimationUtils}'s
 * thread-local animation state retains timestamps from earlier tests (b/584000171).
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {Config.TARGET_SDK})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AnimationHandlerRobolectricTest {

    private static final long FRAME_ADVANCE_MS = 16L;

    @After
    public void tearDown() {
        ShadowChoreographer.setPaused(false);
    }

    @Test
    public void test1_advanceClockAndPopulateAnimationUtilsState() {
        ShadowSystemClock.advanceBy(1, TimeUnit.HOURS);
        long animationTime = AnimationUtils.currentAnimationTimeMillis();
        assertEquals(SystemClock.uptimeMillis(), animationTime);
    }

    @Test
    public void test2_unlockedAnimationUtilsBetweenFrames_doesNotCrashWithScaleXNaN() {
        // SystemClock has been reset to ~100ms by Robolectric, while AnimationUtils's thread-local
        // lastReportedTimeMillis on the main thread still holds > 3,600,000ms from test1.
        View view = new View(ApplicationProvider.getApplicationContext());
        view.setScaleX(0.5f);
        SpringAnimation spring = new SpringAnimation(view, DynamicAnimation.SCALE_X, 1.0f);

        ShadowChoreographer.setPaused(true);
        spring.start();

        // Dispatch frame 1 inside Choreographer#doFrame.
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(FRAME_ADVANCE_MS, TimeUnit.MILLISECONDS);

        // An unlocked call to AnimationUtils.currentAnimationTimeMillis() between frames resets
        // AnimationUtils's lastReportedTimeMillis back down to the reset SystemClock (~115ms).
        long unlockedTime = AnimationUtils.currentAnimationTimeMillis();
        assertTrue(unlockedTime < 10_000L);

        // Dispatch frame 2 and let the spring finish; must not produce Float.NaN on View#setScaleX.
        ShadowChoreographer.setPaused(false);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(FRAME_ADVANCE_MS, TimeUnit.MILLISECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(spring.isRunning());
        assertEquals(1.0f, view.getScaleX(), 0.01f);
    }

    @Test
    public void test3_advanceClockAgainForStallCheck() {
        ShadowSystemClock.advanceBy(1, TimeUnit.HOURS);
        long animationTime = AnimationUtils.currentAnimationTimeMillis();
        assertEquals(SystemClock.uptimeMillis(), animationTime);
    }

    @Test
    public void test4_pausedChoreographerSpring_advancesWithoutStallingAtZeroDeltaT() {
        // Verify that a spring advances immediately on normal VSYNC frames instead of clamping
        // frame timestamps to the stale 1-hour AnimationUtils timestamp from test3.
        FloatValueHolder holder = new FloatValueHolder(0f);
        SpringAnimation spring = new SpringAnimation(holder, 1000f);

        ShadowChoreographer.setPaused(true);
        spring.start();

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(FRAME_ADVANCE_MS, TimeUnit.MILLISECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(FRAME_ADVANCE_MS, TimeUnit.MILLISECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(FRAME_ADVANCE_MS, TimeUnit.MILLISECONDS);

        assertTrue("Expected spring to advance past 0f, got " + holder.getValue(),
                holder.getValue() > 0f);
        spring.cancel();
    }

    @Test
    public void test5_doAnimationFrameWithRewoundTimestamp_reanchorsWithoutNaN() {
        View view = new View(ApplicationProvider.getApplicationContext());
        view.setScaleX(0.5f);
        SpringAnimation spring = new SpringAnimation(view, DynamicAnimation.SCALE_X, 1.0f);
        spring.mRunning = true;
        spring.mValue = 0.5f;

        assertFalse(spring.doAnimationFrame(3_600_000L));
        assertFalse(spring.doAnimationFrame(3_600_016L));
        float valueBeforeRewind = view.getScaleX();
        assertTrue(valueBeforeRewind > 0.5f);

        // Simulate a large backwards jump in frameTime; must clamp deltaT to 0 and re-anchor.
        assertFalse(spring.doAnimationFrame(100L));
        assertFalse(Float.isNaN(view.getScaleX()));
        assertEquals(valueBeforeRewind, view.getScaleX(), 0.001f);

        // Subsequent frame from the new timebase advances normally toward 1.0f.
        assertFalse(spring.doAnimationFrame(116L));
        assertTrue(view.getScaleX() > valueBeforeRewind);
        spring.mRunning = false;

        // Also verify FlingAnimation does not overflow to Infinity/NaN on a rewound frameTime.
        FloatValueHolder flingHolder = new FloatValueHolder(0f);
        FlingAnimation fling = new FlingAnimation(flingHolder).setStartVelocity(500f);
        fling.mRunning = true;
        fling.mValue = 0f;
        assertFalse(fling.doAnimationFrame(3_600_000L));
        assertFalse(fling.doAnimationFrame(3_600_016L));
        float flingBeforeRewind = flingHolder.getValue();
        assertFalse(fling.doAnimationFrame(100L));
        assertFalse(Float.isNaN(flingHolder.getValue()));
        assertFalse(Float.isInfinite(flingHolder.getValue()));
        assertEquals(flingBeforeRewind, flingHolder.getValue(), 0.001f);
        fling.mRunning = false;
    }
}
