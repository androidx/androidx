/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;

import org.junit.Test;

@MediumTest
public class AutoTransitionTest extends BaseTest {

    private LinearLayout mRoot;
    private View mView0;
    private View mView1;

    @LargeTest
    @Test
    public void testLayoutBetweenFadeAndChangeBounds() throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRoot = (LinearLayout) rule.getActivity().getRoot();
                mView0 = new View(rule.getActivity());
                mView0.setBackgroundColor(Color.RED);
                mRoot.addView(mView0, new LinearLayout.LayoutParams(100, 100));
                mView1 = new View(rule.getActivity());
                mView1.setBackgroundColor(Color.BLUE);
                mRoot.addView(mView1, new LinearLayout.LayoutParams(100, 100));
            }
        });
        final LayoutCounter counter = new LayoutCounter();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertThat(mView1.getY(), is(100.f));
                assertThat(mView0.getVisibility(), is(View.VISIBLE));
                mView1.addOnLayoutChangeListener(counter);
            }
        });
        final SyncTransitionListener listener = new SyncTransitionListener(
                SyncTransitionListener.EVENT_END);
        final Transition transition = new AutoTransition();
        transition.setDuration(10);
        transition.addListener(listener);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, transition);
                // This makes view0 fade out and causes view1 to move upwards.
                mView0.setVisibility(View.GONE);
            }
        });
        assertThat("Timed out waiting for the TransitionListener",
                listener.await(), is(true));
        assertThat(mView1.getY(), is(0.f));
        assertThat(mView0.getVisibility(), is(View.GONE));
        counter.reset();
        listener.reset();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, transition);
                // Revert
                mView0.setVisibility(View.VISIBLE);
            }
        });
        assertThat("Timed out waiting for the TransitionListener",
                listener.await(), is(true));
        assertThat(mView1.getY(), is(100.f));
        assertThat(mView0.getVisibility(), is(View.VISIBLE));
    }

    private static class LayoutCounter implements View.OnLayoutChangeListener {

        private int mCalledCount;

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom) {
            mCalledCount++;
            // There should not be more than one layout request to view1.
            if (mCalledCount > 1) {
                fail("View layout happened too many times");
            }
        }

        void reset() {
            mCalledCount = 0;
        }

    }

}
