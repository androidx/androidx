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

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import android.support.test.filters.MediumTest;
import android.view.View;

import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class ChangeScrollTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeScroll();
    }

    @Test
    public void testChangeScroll() throws Throwable {
        enterScene(R.layout.scene5);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = rule.getActivity().findViewById(R.id.text);
                assertEquals(0, view.getScrollX());
                assertEquals(0, view.getScrollY());
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                view.scrollTo(150, 300);
            }
        });
        waitForStart();
        Thread.sleep(150);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = rule.getActivity().findViewById(R.id.text);
                final int scrollX = view.getScrollX();
                final int scrollY = view.getScrollY();
                assertThat(scrollX, is(both(greaterThan(0)).and(lessThan(150))));
                assertThat(scrollY, is(both(greaterThan(0)).and(lessThan(300))));
            }
        });
        waitForEnd();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = rule.getActivity().findViewById(R.id.text);
                assertEquals(150, view.getScrollX());
                assertEquals(300, view.getScrollY());
            }
        });
    }

}
