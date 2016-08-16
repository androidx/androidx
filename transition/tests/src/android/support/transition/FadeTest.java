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

package android.support.transition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import android.animation.Animator;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;

@MediumTest
public class FadeTest extends BaseTest {

    private View mView;
    private ViewGroup mRoot;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRoot = rule.getActivity().getRoot();
                mView = new View(rule.getActivity());
                mRoot.addView(mView, new ViewGroup.LayoutParams(100, 100));
            }
        });
    }

    @Test
    @UiThreadTest
    public void testDisappear() {
        final Fade fade = new Fade();
        final TransitionValues startValues = new TransitionValues();
        startValues.view = mView;
        fade.captureStartValues(startValues);
        mView.setVisibility(View.INVISIBLE);
        final TransitionValues endValues = new TransitionValues();
        endValues.view = mView;
        fade.captureEndValues(endValues);
        Animator animator = fade.createAnimator(mRoot, startValues, endValues);
        assertThat(animator, is(notNullValue()));
    }

    @Test
    @UiThreadTest
    public void testAppear() {
        mView.setVisibility(View.INVISIBLE);
        final Fade fade = new Fade();
        final TransitionValues startValues = new TransitionValues();
        startValues.view = mView;
        fade.captureStartValues(startValues);
        mView.setVisibility(View.VISIBLE);
        final TransitionValues endValues = new TransitionValues();
        endValues.view = mView;
        fade.captureEndValues(endValues);
        Animator animator = fade.createAnimator(mRoot, startValues, endValues);
        assertThat(animator, is(notNullValue()));
    }

    @Test
    @UiThreadTest
    public void testNoChange() {
        final Fade fade = new Fade();
        final TransitionValues startValues = new TransitionValues();
        startValues.view = mView;
        fade.captureStartValues(startValues);
        final TransitionValues endValues = new TransitionValues();
        endValues.view = mView;
        fade.captureEndValues(endValues);
        Animator animator = fade.createAnimator(mRoot, startValues, endValues);
        // No visibility change; no animation should happen
        assertThat(animator, is(nullValue()));
    }

}
