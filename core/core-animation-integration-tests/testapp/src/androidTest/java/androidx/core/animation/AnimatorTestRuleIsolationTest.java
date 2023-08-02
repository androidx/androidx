/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@UiThreadTest
public class AnimatorTestRuleIsolationTest {
    @Rule
    public AnimatorTestRule animatorTestRule = new AnimatorTestRule();

    private static boolean sDidRunActionA = false;
    private static boolean sDidRunActionB = false;
    private static String sRunningTest = "none";

    @Test
    public void testA() {
        sRunningTest = "A";
        sDidRunActionA = false;
        sDidRunActionB = false;
        Runnable actionA = () -> {
            sDidRunActionA = true;
            assertEquals(sRunningTest, "A");
        };
        startAnimator(100, actionA);
        startAnimator(101, actionA);
        animatorTestRule.advanceTimeBy(100);
        assertTrue(sDidRunActionA);
        assertFalse(sDidRunActionB);
    }

    @Test
    public void testB() {
        sRunningTest = "B";
        sDidRunActionA = false;
        sDidRunActionB = false;
        Runnable actionB = () -> {
            sDidRunActionB = true;
            assertEquals(sRunningTest, "B");
        };
        startAnimator(100, actionB);
        startAnimator(101, actionB);
        animatorTestRule.advanceTimeBy(100);
        assertTrue(sDidRunActionB);
        assertFalse(sDidRunActionA);
    }

    private void startAnimator(int duration, Runnable endAction) {
        ValueAnimator animator = ObjectAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                endAction.run();
            }
        });
        animator.start();
    }

}
