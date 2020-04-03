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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.view.InflateException;

import androidx.core.animation.testapp.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AnimatorInflaterTest {

    private static final float EPSILON = 0.01f;

    @ClassRule
    public static AnimatorTestRule sAnimatorTestRule = new AnimatorTestRule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @UiThreadTest
    @Test
    public void testLoadAnimator() {
        Context context = ApplicationProvider.getApplicationContext();
        Animator anim = AnimatorInflater.loadAnimator(context, R.animator.animator_set_with_dimens);
        assertEquals(100, anim.getTotalDuration());

        DummyObject obj = new DummyObject();
        anim.setTarget(obj);
        anim.start();

        assertTrue(anim.isRunning());
        sAnimatorTestRule.advanceTimeBy(anim.getTotalDuration());
        assertFalse(anim.isRunning());

        float targetX = context.getResources().getDimension(R.dimen.test_animator_target_x);
        float targetY = context.getResources().getDimension(R.dimen.test_animator_target_y);

        assertEquals(targetX, obj.x, EPSILON);
        assertEquals(targetY, obj.y, EPSILON);
        assertEquals(2, obj.left);
    }


    @UiThreadTest
    @Test
    public void testLoadAnimatorAlongPath() {
        Context context = ApplicationProvider.getApplicationContext();
        Animator anim = AnimatorInflater.loadAnimator(context, R.animator.animator_along_path);
        assertTrue(anim.getInterpolator() instanceof LinearInterpolator);
        assertEquals(100, anim.getDuration());
        DummyObject obj = new DummyObject();
        anim.setTarget(obj);
        anim.start();

        // Check whether the animation is indeed running along the path.
        int inc = 2;
        for (int i = 0; i <= 100; i += inc) {
            float y = i <= 50 ? 0 : 100;
            assertEquals(i, obj.x, EPSILON);
            assertEquals(y, obj.y, EPSILON);
            sAnimatorTestRule.advanceTimeBy(inc);
        }
    }

    @Test
    public void pathInterpolator() {
        final Interpolator interpolator = AnimatorInflater.loadInterpolator(
                ApplicationProvider.getApplicationContext(),
                R.interpolator.path_interpolator
        );
        assertEquals(0.85f, interpolator.getInterpolation(0.5f), EPSILON);
    }

    @Test
    public void pathInterpolator_controlPoints() {
        final Interpolator interpolator = AnimatorInflater.loadInterpolator(
                ApplicationProvider.getApplicationContext(),
                R.interpolator.control_points_interpolator
        );
        assertEquals(0.89f, interpolator.getInterpolation(0.5f), EPSILON);
    }

    @Test
    public void pathInterpolator_singleControlPoint() {
        final Interpolator interpolator = AnimatorInflater.loadInterpolator(
                ApplicationProvider.getApplicationContext(),
                R.interpolator.single_control_point_interpolator
        );
        assertEquals(0.086f, interpolator.getInterpolation(0.5f), EPSILON);
    }

    @Test
    public void pathInterpolator_wrongControlPoint() {
        expectedException.expect(InflateException.class);
        expectedException.expectMessage("requires the controlY1 attribute");
        AnimatorInflater.loadInterpolator(ApplicationProvider.getApplicationContext(),
                R.interpolator.wrong_control_point_interpolator);
    }

    @Test
    public void pathInterpolator_wrongControlPoints() {
        expectedException.expect(InflateException.class);
        expectedException.expectMessage("requires both controlX2 and controlY2 for cubic Beziers");
        AnimatorInflater.loadInterpolator(ApplicationProvider.getApplicationContext(),
                R.interpolator.wrong_control_points_interpolator);
    }

    @Test
    public void pathInterpolator_wrongStartEnd() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("The Path must start at (0,0) and end at (1,1)");
        AnimatorInflater.loadInterpolator(ApplicationProvider.getApplicationContext(),
                R.interpolator.wrong_path_interpolator_1);
    }

    @Test
    public void pathInterpolator_loopBack() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("The Path cannot loop back on itself");
        AnimatorInflater.loadInterpolator(ApplicationProvider.getApplicationContext(),
                R.interpolator.wrong_path_interpolator_2);
    }

    @Test
    public void pathInterpolator_discontinuity() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(Build.VERSION.SDK_INT >= 26
                ? "The Path cannot have discontinuity in the X axis"
                // Older APIs don't detect discontinuity, but they report it as loop back.
                : "The Path cannot loop back on itself");
        AnimatorInflater.loadInterpolator(ApplicationProvider.getApplicationContext(),
                R.interpolator.wrong_path_interpolator_3);
    }

    static class DummyObject {

        public float x;
        public float y;
        public int left;

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getLeft() {
            return left;
        }
    }

}
