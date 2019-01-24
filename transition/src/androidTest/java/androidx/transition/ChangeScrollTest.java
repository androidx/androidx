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
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.leq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.widget.TextView;

import androidx.test.filters.LargeTest;
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

        verify(view, timeout(1000).atLeastOnce()).setScrollX(and(gt(0), leq(150)));
        verify(view, timeout(1000).atLeastOnce()).setScrollY(and(gt(0), leq(300)));

        waitForEnd();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(150, view.getScrollX());
                assertEquals(300, view.getScrollY());
            }
        });
    }

}
