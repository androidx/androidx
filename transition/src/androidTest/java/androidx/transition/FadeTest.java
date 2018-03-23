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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.test.R;

import org.junit.Before;
import org.junit.Test;


@MediumTest
public class FadeTest extends BaseTest {

    private View mView;
    private ViewGroup mRoot;

    @UiThreadTest
    @Before
    public void setUp() {
        mRoot = rule.getActivity().getRoot();
        mView = new View(rule.getActivity());
        mRoot.addView(mView, new ViewGroup.LayoutParams(100, 100));
    }

    @Test
    public void testMode() {
        assertThat(Fade.IN, is(Visibility.MODE_IN));
        assertThat(Fade.OUT, is(Visibility.MODE_OUT));
        final Fade fade = new Fade();
        assertThat(fade.getMode(), is(Visibility.MODE_IN | Visibility.MODE_OUT));
        fade.setMode(Visibility.MODE_IN);
        assertThat(fade.getMode(), is(Visibility.MODE_IN));
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

    @Test
    public void testFadeOutThenIn() throws Throwable {
        // Fade out
        final Runnable interrupt = mock(Runnable.class);
        float[] valuesOut = new float[2];
        final InterruptibleFade fadeOut = new InterruptibleFade(Fade.MODE_OUT, interrupt,
                valuesOut);
        final Transition.TransitionListener listenerOut = mock(Transition.TransitionListener.class);
        fadeOut.addListener(listenerOut);
        changeVisibility(fadeOut, mRoot, mView, View.INVISIBLE);
        verify(listenerOut, timeout(3000)).onTransitionStart(any(Transition.class));

        // The view is in the middle of fading out
        verify(interrupt, timeout(3000)).run();

        // Fade in
        float[] valuesIn = new float[2];
        final InterruptibleFade fadeIn = new InterruptibleFade(Fade.MODE_IN, null, valuesIn);
        final Transition.TransitionListener listenerIn = mock(Transition.TransitionListener.class);
        fadeIn.addListener(listenerIn);
        changeVisibility(fadeIn, mRoot, mView, View.VISIBLE);
        verify(listenerOut, timeout(3000)).onTransitionPause(any(Transition.class));
        verify(listenerIn, timeout(3000)).onTransitionStart(any(Transition.class));
        assertThat(valuesOut[1], allOf(greaterThan(0f), lessThan(1f)));
        if (Build.VERSION.SDK_INT >= 19 && fadeOut.mInitialAlpha >= 0) {
            // These won't match on API levels 18 and below due to lack of Animator pause.
            assertEquals(valuesOut[1], valuesIn[0], 0.01f);
        }

        verify(listenerIn, timeout(3000)).onTransitionEnd(any(Transition.class));
        assertThat(mView.getVisibility(), is(View.VISIBLE));
        assertEquals(valuesIn[1], 1.f, 0.01f);
    }

    @Test
    public void testFadeInThenOut() throws Throwable {
        changeVisibility(null, mRoot, mView, View.INVISIBLE);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Fade in
        final Runnable interrupt = mock(Runnable.class);
        float[] valuesIn = new float[2];
        final InterruptibleFade fadeIn = new InterruptibleFade(Fade.MODE_IN, interrupt, valuesIn);
        final Transition.TransitionListener listenerIn = mock(Transition.TransitionListener.class);
        fadeIn.addListener(listenerIn);
        changeVisibility(fadeIn, mRoot, mView, View.VISIBLE);
        verify(listenerIn, timeout(3000)).onTransitionStart(any(Transition.class));

        // The view is in the middle of fading in
        verify(interrupt, timeout(3000)).run();

        // Fade out
        float[] valuesOut = new float[2];
        final InterruptibleFade fadeOut = new InterruptibleFade(Fade.MODE_OUT, null, valuesOut);
        final Transition.TransitionListener listenerOut = mock(Transition.TransitionListener.class);
        fadeOut.addListener(listenerOut);
        changeVisibility(fadeOut, mRoot, mView, View.INVISIBLE);
        verify(listenerIn, timeout(3000)).onTransitionPause(any(Transition.class));
        verify(listenerOut, timeout(3000)).onTransitionStart(any(Transition.class));
        assertThat(valuesIn[1], allOf(greaterThan(0f), lessThan(1f)));
        if (Build.VERSION.SDK_INT >= 19 && fadeIn.mInitialAlpha >= 0) {
            // These won't match on API levels 18 and below due to lack of Animator pause.
            assertEquals(valuesIn[1], valuesOut[0], 0.01f);
        }

        verify(listenerOut, timeout(3000)).onTransitionEnd(any(Transition.class));
        assertThat(mView.getVisibility(), is(View.INVISIBLE));
    }

    @Test
    public void testFadeWithAlpha() throws Throwable {
        // Set the view alpha to 0.5
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mView.setAlpha(0.5f);
            }
        });
        // Fade out
        final Fade fadeOut = new Fade(Fade.OUT);
        final Transition.TransitionListener listenerOut = mock(Transition.TransitionListener.class);
        fadeOut.addListener(listenerOut);
        changeVisibility(fadeOut, mRoot, mView, View.INVISIBLE);
        verify(listenerOut, timeout(3000)).onTransitionStart(any(Transition.class));
        verify(listenerOut, timeout(3000)).onTransitionEnd(any(Transition.class));
        // Fade in
        final Fade fadeIn = new Fade(Fade.IN);
        final Transition.TransitionListener listenerIn = mock(Transition.TransitionListener.class);
        fadeIn.addListener(listenerIn);
        changeVisibility(fadeIn, mRoot, mView, View.VISIBLE);
        verify(listenerIn, timeout(3000)).onTransitionStart(any(Transition.class));
        verify(listenerIn, timeout(3000)).onTransitionEnd(any(Transition.class));
        // Confirm that the view still has the original alpha value
        assertThat(mView.getVisibility(), is(View.VISIBLE));
        assertEquals(0.5f, mView.getAlpha(), 0.01f);
    }

    // After a transition, a transitioned view as part of a scene should not be removed
    @Test
    public void endVisibilityIsCorrect() throws Throwable {
        final TransitionActivity activity = rule.getActivity();
        final Scene[] scenes = new Scene[2];
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = activity.getLayoutInflater().inflate(R.layout.scene11, mRoot, false);

                scenes[0] = new Scene(mRoot, view);
                scenes[0].enter();
                scenes[1] = Scene.getSceneForLayout(mRoot, R.layout.scene12, activity);
            }
        });

        assertNotNull(activity.findViewById(R.id.redSquare));

        // We don't really care how short the duration is, so let's make it really short
        final Fade fade = new Fade();
        fade.setDuration(1);
        Transition.TransitionListener listener = mock(Transition.TransitionListener.class);
        fade.addListener(listener);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.go(scenes[1], fade);
            }
        });
        // should be much shorter than 1 second, but why worry about it?
        verify(listener, timeout(1000)).onTransitionEnd(any(Transition.class));

        assertNotNull(activity.findViewById(R.id.redSquare));

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.go(scenes[0], fade);
            }
        });
        verify(listener, timeout(1000)).onTransitionStart(any(Transition.class));
        assertNotNull(activity.findViewById(R.id.redSquare));
    }

    private void changeVisibility(final Fade fade, final ViewGroup container, final View target,
            final int visibility) throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fade != null) {
                    TransitionManager.beginDelayedTransition(container, fade);
                }
                target.setVisibility(visibility);
            }
        });
    }

    /**
     * A special version of {@link Fade} that runs a specified {@link Runnable} soon after the
     * target starts fading in or out.
     */
    private static class InterruptibleFade extends Fade {

        static final float ALPHA_THRESHOLD = 0.2f;

        float mInitialAlpha = -1;
        Runnable mMiddle;
        final float[] mAlphaValues;

        InterruptibleFade(int mode, Runnable middle, float[] alphaValues) {
            super(mode);
            mMiddle = middle;
            mAlphaValues = alphaValues;
        }

        @Nullable
        @Override
        public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                @Nullable final TransitionValues startValues,
                @Nullable final TransitionValues endValues) {
            final Animator animator = super.createAnimator(sceneRoot, startValues, endValues);
            if (animator instanceof ObjectAnimator) {
                ((ObjectAnimator) animator).addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                final float alpha = (float) animation.getAnimatedValue();
                                mAlphaValues[1] = alpha;
                                if (mInitialAlpha < 0) {
                                    mInitialAlpha = alpha;
                                    mAlphaValues[0] = mInitialAlpha;
                                } else if (Math.abs(alpha - mInitialAlpha) > ALPHA_THRESHOLD) {
                                    if (mMiddle != null) {
                                        mMiddle.run();
                                        mMiddle = null;
                                    }
                                }
                            }
                        });
            }
            return animator;
        }

    }

}
