/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
public class ChangeClipBoundsTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeClipBounds();
    }

    @SdkSuppress(minSdkVersion = 18)
    @Test
    public void testChangeClipBounds() throws Throwable {
        final View redSquare = spy(new View(rule.getActivity()));
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                redSquare.setBackgroundColor(Color.RED);
                mRoot.addView(redSquare, 100, 100);
            }
        });

        final Rect newClip = new Rect(40, 40, 60, 60);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, newClip);
            }
        });
        waitForStart();
        verify(redSquare, timeout(1000).atLeastOnce())
                .setClipBounds(argThat(isRectContaining(newClip)));
        waitForEnd();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Rect endRect = ViewCompat.getClipBounds(redSquare);
                assertNotNull(endRect);
                assertEquals(newClip, endRect);
            }
        });

        resetListener();
        reset(redSquare);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, null);
            }
        });
        waitForStart();
        verify(redSquare, timeout(1000).atLeastOnce())
                .setClipBounds(argThat(isRectContainedIn(newClip)));
        waitForEnd();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
            }
        });

    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingClipToNull() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            viewArr[0].setClipBounds(new Rect(0, 0, 50, 50));
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
        });
        final View view = viewArr[0];
        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(null);
        });

        final TransitionSeekController seekController = seekControllerArr[0];
        ChangeClipBounds returnTransition = new ChangeClipBounds();
        CountDownLatch latch = new CountDownLatch(1);
        returnTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                latch.countDown();
            }
        });

        rule.runOnUiThread(() -> {
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek past the always there transition before the clip transition
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 75, 75), view.getClipBounds());

            // Seek past the transition
            seekController.setCurrentPlayTimeMillis(800);
            assertNull(view.getClipBounds());

            // Seek back to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 75, 75), view.getClipBounds());

            // Seek before the transition:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            seekController.setCurrentPlayTimeMillis(450);
            TransitionManager.beginDelayedTransition(mRoot, returnTransition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        rule.runOnUiThread(() -> {
            // It should start from 75x75 and then transition in
            assertTrue(view.getClipBounds().width() <= 75);
            assertTrue(view.getClipBounds().height() <= 75);
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        rule.runOnUiThread(() -> {
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingClipFromNull() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
        });
        final View view = viewArr[0];
        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        final TransitionSeekController seekController = seekControllerArr[0];
        ChangeClipBounds returnTransition = new ChangeClipBounds();
        CountDownLatch latch = new CountDownLatch(1);
        returnTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                latch.countDown();
            }
        });

        rule.runOnUiThread(() -> {
            assertNull(view.getClipBounds());

            // Seek past the always there transition before the clip transition
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(new Rect(0, 0, 100, 100), view.getClipBounds());

            // Seek to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 75, 75), view.getClipBounds());

            // Seek past the transition
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek back to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 75, 75), view.getClipBounds());

            // Seek before the transition:
            seekController.setCurrentPlayTimeMillis(250);
            assertNull(view.getClipBounds());

            seekController.setCurrentPlayTimeMillis(450);
            TransitionManager.beginDelayedTransition(mRoot, returnTransition);
            view.setClipBounds(null);
        });

        rule.runOnUiThread(() -> {
            // It should start from 75x75 and then transition in
            assertTrue(view.getClipBounds().width() >= 75);
            assertTrue(view.getClipBounds().height() >= 75);
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        rule.runOnUiThread(() -> {
            assertNull(view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingClips() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
            viewArr[0].setClipBounds(new Rect(0, 0, 50, 50));
        });
        final View view = viewArr[0];
        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 80, 80));
        });

        final TransitionSeekController seekController = seekControllerArr[0];
        ChangeClipBounds returnTransition = new ChangeClipBounds();
        CountDownLatch latch = new CountDownLatch(1);
        returnTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                latch.countDown();
            }
        });

        rule.runOnUiThread(() -> {
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek past the always there transition before the clip transition
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 65, 65), view.getClipBounds());

            // Seek past the transition
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(new Rect(0, 0, 80, 80), view.getClipBounds());

            // Seek back to half through the transition
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(new Rect(0, 0, 65, 65), view.getClipBounds());

            // Seek before the transition:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            seekController.setCurrentPlayTimeMillis(450);
            TransitionManager.beginDelayedTransition(mRoot, returnTransition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        rule.runOnUiThread(() -> {
            // It should start from 75x75 and then transition in
            assertTrue(view.getClipBounds().width() <= 65);
            assertTrue(view.getClipBounds().height() <= 65);
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        rule.runOnUiThread(() -> {
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void changeClipBeforeStart() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
            viewArr[0].setClipBounds(new Rect(0, 0, 20, 50));
        });
        final View view = viewArr[0];
        rule.runOnUiThread(() -> {
            TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 80, 80));
        });

        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] =
                    TransitionManager.controlDelayedTransition(mRoot, new ChangeClipBounds());
            view.setClipBounds(new Rect(0, 0, 100, 100));
        });

        rule.runOnUiThread(() -> {
            TransitionSeekController seekController = seekControllerArr[0];
            // It should start from 20x50 and go to 100x100
            assertEquals(20, view.getClipBounds().width());
            assertEquals(50, view.getClipBounds().height());

            // half way through
            seekController.setCurrentPlayTimeMillis(150);
            assertEquals(60, view.getClipBounds().width());
            assertEquals(75, view.getClipBounds().height());

            // finish
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(100, view.getClipBounds().width());
            assertEquals(100, view.getClipBounds().height());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void testSeekInterruption() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // supported on U+
        }
        final View view = new View(rule.getActivity());

        rule.runOnUiThread(() -> {
            mRoot.addView(view, new ViewGroup.LayoutParams(100, 100));
        });

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        rule.runOnUiThread(() -> {
            assertNull(view.getClipBounds());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());

            // Seek back to the beginning
            seekControllerArr[0].setCurrentPlayTimeMillis(0);
            assertNull(view.getClipBounds());

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(50, 50, 100, 100));
        });

        rule.runOnUiThread(() -> {
            assertNull(view.getClipBounds());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(new Rect(50, 50, 100, 100), view.getClipBounds());

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(null);
        });

        rule.runOnUiThread(() -> {
            assertEquals(new Rect(50, 50, 100, 100), view.getClipBounds());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertNull(view.getClipBounds());

            // Seek to the middle
            seekControllerArr[0].setCurrentPlayTimeMillis(450);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        rule.runOnUiThread(() -> {
            assertEquals(new Rect(25, 25, 100, 100), view.getClipBounds());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void testSeekNoChange() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // supported on U+
        }
        final View view = new View(rule.getActivity());

        rule.runOnUiThread(() -> {
            mRoot.addView(view, new ViewGroup.LayoutParams(100, 100));
        });

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeClipBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        TransitionSeekController firstController = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] =
                    TransitionManager.controlDelayedTransition(mRoot, new ChangeClipBounds());
            view.setClipBounds(new Rect(0, 0, 50, 50));
        });

        rule.runOnUiThread(() -> {
            assertEquals(0, seekControllerArr[0].getDurationMillis());

            // Should only be controlled by the first transition
            firstController.setCurrentPlayTimeMillis(900);
            assertEquals(new Rect(0, 0, 50, 50), view.getClipBounds());
        });
    }

    private ArgumentMatcher<Rect> isRectContaining(final Rect rect) {
        return new ArgumentMatcher<Rect>() {
            @Override
            public boolean matches(Rect self) {
                return rect != null && self != null && self.contains(rect);
            }
        };
    }

    private ArgumentMatcher<Rect> isRectContainedIn(final Rect rect) {
        return new ArgumentMatcher<Rect>() {
            @Override
            public boolean matches(Rect self) {
                return rect != null && self != null && rect.contains(self);
            }
        };
    }

    @Test
    public void empty() {
        // Avoid "No tests found" on older devices
    }

}
