/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.animation;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import android.graphics.Path;

import androidx.core.graphics.PathParser;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class InterpolatorTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void testCycleInterpolator() {
        for (int cycles = 0; cycles < 10; cycles++) {
            CycleInterpolator interpolator = new CycleInterpolator(cycles);
            for (float fraction = 0f; fraction <= 1f; fraction += 0.05f) {
                assertEquals(interpolator.getInterpolation(fraction),
                        Math.sin(2 * Math.PI * cycles * fraction), EPSILON);
            }
        }
    }

    @Test
    public void testPathInterpolator() {
        Interpolator interpolator = new PathInterpolator(0f, 0f, 0f, 1f);
        assertEquals(0.8892f, interpolator.getInterpolation(0.5f), 0.01f);
        assertEquals(0f, interpolator.getInterpolation(0f), 0.01f);
        assertEquals(1f, interpolator.getInterpolation(1f), 0.01f);

        interpolator = new PathInterpolator(1f, 0f);
        assertEquals(0.087f, interpolator.getInterpolation(0.5f), 0.01f);
        assertEquals(0f, interpolator.getInterpolation(0f), 0.01f);
        assertEquals(1f, interpolator.getInterpolation(1f), 0.01f);

        Path path = PathParser.createPathFromPathData(
                "M 0.0,0.0 c 0.08,0.0 0.04,1.0 0.2,0.8 l 0.6,0.1 L 1.0,1.0");
        interpolator = new PathInterpolator(path);
        assertEquals(0.85f, interpolator.getInterpolation(0.5f), 0.01f);
        assertEquals(0f, interpolator.getInterpolation(0f), 0.01f);
        assertEquals(1f, interpolator.getInterpolation(1f), 0.01f);
    }

    /**
     * Test that the interpolator curve accelerates first then decelerates.
     */
    @Test
    public void testAccelerateDecelerateInterpolator() {
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        // Accelerate
        assertTrue(getVelocity(interpolator, 0.01f) < getVelocity(interpolator, 0.5f));
        // Decelerate
        assertTrue(getVelocity(interpolator, 0.5f) > getVelocity(interpolator, 0.99f));

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testAccelerateInterpolator() {
        Interpolator interpolator = new AccelerateInterpolator();
        // Accelerate
        assertTrue(getVelocity(interpolator, 0.01f) < getVelocity(interpolator, 0.5f));
        // Accelerate still
        assertTrue(getVelocity(interpolator, 0.5f) < getVelocity(interpolator, 0.99f));

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testAnticipateInterpolator() {
        // The velocity should be first negative then positive
        Interpolator interpolator = new AnticipateInterpolator();

        // The interpolation should dip below 0 (i.e. anticipate) when fraction is in [0, 1]
        boolean didAnticipate = false;
        // At one point, the interpolation will go above 1 (i.e. overshoot)
        for (float fraction = 0.01f; fraction <= 1f; fraction += 0.01f) {
            if (interpolator.getInterpolation(fraction) < 0f) {
                didAnticipate = true;
                break;
            }
        }
        assertTrue(didAnticipate);

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testOvershootInterpolator() {
        Interpolator interpolator = new OvershootInterpolator();

        boolean didOvershoot = false;
        // At one point, the interpolation will go above 1 (i.e. overshoot)
        for (float fraction = 0.01f; fraction <= 1f; fraction += 0.01f) {
            if (interpolator.getInterpolation(fraction) > 1f) {
                didOvershoot = true;
                break;
            }
        }
        assertTrue(didOvershoot);

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testAnticipateOvershootInterpolator() {
        // When fraction = 0.5, output should be 0.5
        Interpolator interpolator = new AnticipateOvershootInterpolator();
        assertEquals(0.5f, interpolator.getInterpolation(0.5f));

        // When fraction < 0.5f, the interpolation will dip below 0 (i.e. anticipate)
        boolean didAnticipate = false;
        // At one point, the interpolation will go above 1 (i.e. overshoot)
        for (float fraction = 0.01f; fraction < 0.5f; fraction += 0.01f) {
            if (interpolator.getInterpolation(fraction) < 0f) {
                didAnticipate = true;
                break;
            }
        }
        assertTrue(didAnticipate);

        // When fraction > 0.5f, the interpolation will go above 1 (i.e. overshoot)
        boolean didOvershoot = false;
        // At one point, the interpolation will go above 1 (i.e. overshoot)
        for (float fraction = 0.5f; fraction <= 1f; fraction += 0.01f) {
            if (interpolator.getInterpolation(fraction) > 1f) {
                didOvershoot = true;
                break;
            }
        }
        assertTrue(didOvershoot);

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testBounceInterpolator() {
        Interpolator interpolator = new BounceInterpolator();
        // Interpolation should get to 1 a few times before fraction reaches 1
        int numBounce = 0;
        for (float fraction = 0.01f; fraction < 1f; fraction += 0.01f) {
            float interpolation = interpolator.getInterpolation(fraction);
            if (interpolation >= 0.98f && getVelocity(interpolator, fraction - 0.01f) > 0
                    && getVelocity(interpolator, fraction + 0.01f) < 0f) {
                numBounce++;
            }
        }
        assertTrue(numBounce >= 1);

        assertEquals(0f, interpolator.getInterpolation(0f), EPSILON);
        assertEquals(1f, interpolator.getInterpolation(1f), EPSILON);
    }

    @Test
    public void testLinearInterpolator() {
        Interpolator interpolator = new LinearInterpolator();
        for (float fraction = 0f; fraction <= 1f; fraction += 0.01f) {
            assertEquals(fraction, interpolator.getInterpolation(fraction), EPSILON);
        }
    }

    /**
     * Calculates the velocity (i.e. 2nd derivative) of the curve at the given fraction.
     */
    private static float getVelocity(Interpolator interpolator, float fraction) {
        float start;
        float end;
        if (fraction < 0.005f) {
            start = 0;
            end = 0.005f;
        } else if (fraction > 0.995f) {
            start = 0.995f;
            end = 1f;
        } else {
            start = fraction - 0.005f;
            end = fraction + 0.005f;
        }
        return (interpolator.getInterpolation(end) - interpolator.getInterpolation(start)) / 0.01f;
    }
}
