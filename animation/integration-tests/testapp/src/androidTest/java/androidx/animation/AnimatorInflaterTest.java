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

package androidx.animation;

import android.content.Context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.animation.testapp.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AnimatorInflaterTest {

    private static final float EPSILON = 0.01f;

    @ClassRule
    public static AnimationTestRule sAnimationTestRule = new AnimationTestRule();

    @UiThreadTest
    @Test
    public void testLoadAnimator() {
        Context context = InstrumentationRegistry.getContext();
        Animator anim = AnimatorInflater.loadAnimator(context, R.animator.animator_set_with_dimens);
        assertEquals(100, anim.getTotalDuration());

        DummyObject obj = new DummyObject();
        anim.setTarget(obj);
        anim.start();

        assertTrue(anim.isRunning());
        sAnimationTestRule.advanceTimeBy(anim.getTotalDuration());
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
        Context context = InstrumentationRegistry.getContext();
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
            sAnimationTestRule.advanceTimeBy(inc);
        }
    }

    class DummyObject {

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
