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

import static androidx.transition.AtLeastOnceWithin.atLeastOnceWithin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.leq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.os.BuildCompat;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.transition.test.R;

import org.junit.Test;

@LargeTest
public class ChangeScrollTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeScroll();
    }

    @Test
    public void testChangeScroll() throws Throwable {
        final TextView view = spy(new TextView(rule.getActivity()));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRoot.addView(view, 100, 100);
                view.setText(R.string.longText);
            }
        });

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(0, view.getScrollX());
                assertEquals(0, view.getScrollY());
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                view.scrollTo(150, 300);
            }
        });
        waitForStart();

        verify(view, atLeastOnceWithin(1000)).setScrollX(and(gt(0), leq(150)));
        verify(view, atLeastOnceWithin(1000)).setScrollY(and(gt(0), leq(300)));

        waitForEnd();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(150, view.getScrollX());
                assertEquals(300, view.getScrollY());
            }
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingScroll() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeScroll());
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
            view.setScrollY(100);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            assertEquals(0, view.getScrollY());

            // Seek past the always there transition before the scroll
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(0, view.getScrollY());

            // Seek to half through the scroll
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(50, view.getScrollY());

            // Seek past the scroll
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(100, view.getScrollY());

            // Seek back to half through the scroll
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(50, view.getScrollY());

            // Seek before the scroll:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(0, view.getScrollY());

            seekController.setCurrentPlayTimeMillis(450);
            TransitionManager.beginDelayedTransition(mRoot, new ChangeScroll());
            view.setScrollY(0);
        });

        rule.runOnUiThread(() -> {
            // It should start from 50 and move to 0
            assertTrue(view.getScrollY() <= 50);
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingScrollBeforeStart() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeScroll());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
            viewArr[0].setScrollY(100);
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setScrollY(200);
        });

        rule.runOnUiThread(() -> {
            seekControllerArr[0] =
                    TransitionManager.controlDelayedTransition(mRoot, new ChangeScroll());
            view.setScrollY(0);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            // Should start from 100 and go to 0
            assertEquals(100, view.getScrollY());

            // Seek to half through the scroll
            seekController.setCurrentPlayTimeMillis(150);
            assertEquals(50, view.getScrollY());

            // Seek to the end
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(0, view.getScrollY());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void testSeekInterruption() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final View view = new View(rule.getActivity());

        rule.runOnUiThread(() -> {
            mRoot.addView(view, new ViewGroup.LayoutParams(100, 100));
        });

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeScroll());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setScrollY(100);
        });

        rule.runOnUiThread(() -> {
            assertEquals(0, view.getScrollY());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(100, view.getScrollY());

            // Seek back to the beginning
            seekControllerArr[0].setCurrentPlayTimeMillis(0);
            assertEquals(0, view.getScrollY());

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setScrollY(200);
        });

        rule.runOnUiThread(() -> {
            assertEquals(0, view.getScrollY());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(200, view.getScrollY());

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setScrollY(50);
        });

        rule.runOnUiThread(() -> {
            assertEquals(200, view.getScrollY());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(50, view.getScrollY());

            // Seek to the middle
            seekControllerArr[0].setCurrentPlayTimeMillis(450);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            view.setScrollY(500);
        });

        rule.runOnUiThread(() -> {
            assertEquals((200 + 50) / 2, view.getScrollY());

            // Seek to the end
            seekControllerArr[0].setCurrentPlayTimeMillis(900);
            assertEquals(500, view.getScrollY());
        });
    }
}
