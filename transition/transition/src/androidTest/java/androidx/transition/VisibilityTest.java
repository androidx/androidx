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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.testutils.AnimationDurationScaleRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

@MediumTest
public class VisibilityTest extends BaseTest {

    @Rule
    public final AnimationDurationScaleRule mAnimationDurationScaleRule =
            AnimationDurationScaleRule.createForAllTests(1f);

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
        final CustomVisibility visibility = new CustomVisibility();
        assertThat(visibility.getMode(), is(Visibility.MODE_IN | Visibility.MODE_OUT));
        visibility.setMode(Visibility.MODE_IN);
        assertThat(visibility.getMode(), is(Visibility.MODE_IN));
    }

    @Test
    @UiThreadTest
    public void testCustomVisibility() {
        final CustomVisibility visibility = new CustomVisibility();
        assertThat(visibility.getName(), is(equalTo(CustomVisibility.class.getName())));
        assertNotNull(visibility.getTransitionProperties());

        // Capture start values
        mView.setScaleX(0.5f);
        final TransitionValues startValues = new TransitionValues(mView);
        visibility.captureStartValues(startValues);
        assertThat((float) startValues.values.get(CustomVisibility.PROPNAME_SCALE_X), is(0.5f));

        // Hide the view and capture end values
        mView.setVisibility(View.GONE);
        final TransitionValues endValues = new TransitionValues(mView);
        visibility.captureEndValues(endValues);

        // This should invoke onDisappear, not onAppear
        ObjectAnimator animator = (ObjectAnimator) visibility
                .createAnimator(mRoot, startValues, endValues);
        assertNotNull(animator);
        assertThat(animator.getPropertyName(), is(equalTo("scaleX")));

        // Jump to the end of the animation
        animator.end();

        // This value confirms that onDisappear, not onAppear, was called
        assertThat((float) animator.getAnimatedValue(), is(0.25f));
    }

    @Test
    public void testApplyingTwoVisibility() throws Throwable {
        final View[] views = new View[2];
        // create fake transition and listener
        final TransitionSet set = new TransitionSet();
        set.addTransition(new Visibility() {
            @Override
            public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                    TransitionValues startValues, TransitionValues endValues) {
                views[0] = view;
                return ValueAnimator.ofFloat(0, 1);
            }
        });
        set.addTransition(new Visibility() {
            @Override
            public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                    TransitionValues startValues, TransitionValues endValues) {
                views[1] = view;
                return ValueAnimator.ofFloat(0, 1);
            }
        });
        Transition.TransitionListener listener = spy(new TransitionListenerAdapter());
        set.addListener(listener);

        // remove view
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, set);
                mRoot.removeView(mView);
            }
        });

        // wait for the transition to start
        verify(listener, timeout(3000)).onTransitionStart(any(Transition.class));

        // verify original view added to overlay
        assertNotNull(mView.getParent());
        // verify both animations using the same original view
        assertEquals(mView, views[0]);
        assertEquals(mView, views[1]);
    }

    @Test
    @UiThreadTest
    public void testCustomVisibility2() {
        final CustomVisibility2 visibility = new CustomVisibility2();
        final TransitionValues startValues = new TransitionValues(mView);
        visibility.captureStartValues(startValues);
        mView.setVisibility(View.GONE);
        final TransitionValues endValues = new TransitionValues(mView);
        visibility.captureEndValues(endValues);
        ObjectAnimator animator = (ObjectAnimator) visibility
                .createAnimator(mRoot, startValues, endValues);
        assertNotNull(animator);

        // Jump to the end of the animation
        animator.end();

        // This value confirms that onDisappear, not onAppear, was called
        assertThat((float) animator.getAnimatedValue(), is(0.25f));
    }

    @FlakyTest
    @Test
    public void testViewDetachedFromOverlayWhilePaused() throws Throwable {
        // create fake transition and listener
        final Visibility visibility = new Visibility() {
            @Override
            public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                    TransitionValues startValues, TransitionValues endValues) {
                return ValueAnimator.ofFloat(0, 1);
            }
        };
        Transition.TransitionListener listener = spy(new TransitionListenerAdapter());
        visibility.addListener(listener);

        // remove view
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, visibility);
                mRoot.removeView(mView);
            }
        });

        // wait for the transition to start
        verify(listener, timeout(3000)).onTransitionStart(any(Transition.class));

        // test pause/resume logic
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // The view attached to the overlay
                assertNotNull(mView.getParent());

                visibility.pause(mRoot);

                // The view detached from the overlay while paused
                assertNull(mView.getParent());

                visibility.resume(mRoot);

                // The view attached to the overlay after resume
                assertNotNull(mView.getParent());
            }
        });
    }

    /**
     * A custom {@link Visibility} with 5-arg onAppear/Disappear
     */
    public static class CustomVisibility extends Visibility {

        static final String PROPNAME_SCALE_X = "customVisibility:scaleX";

        private static String[] sTransitionProperties;

        @Nullable
        @Override
        public String[] getTransitionProperties() {
            if (sTransitionProperties == null) {
                String[] properties = super.getTransitionProperties();
                if (properties != null) {
                    sTransitionProperties = Arrays.copyOf(properties, properties.length + 1);
                } else {
                    sTransitionProperties = new String[1];
                }
                sTransitionProperties[sTransitionProperties.length - 1] = PROPNAME_SCALE_X;
            }
            return sTransitionProperties;
        }

        @Override
        public void captureStartValues(@NonNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            transitionValues.values.put(PROPNAME_SCALE_X, transitionValues.view.getScaleX());
        }

        @Override
        public Animator onAppear(@NonNull ViewGroup sceneRoot, TransitionValues startValues,
                int startVisibility, TransitionValues endValues, int endVisibility) {
            if (startValues == null) {
                return null;
            }
            float startScaleX = (float) startValues.values.get(PROPNAME_SCALE_X);
            return ObjectAnimator.ofFloat(startValues.view, "scaleX", startScaleX, 0.75f);
        }

        @Override
        public Animator onDisappear(@NonNull ViewGroup sceneRoot, TransitionValues startValues,
                int startVisibility, TransitionValues endValues, int endVisibility) {
            if (startValues == null) {
                return null;
            }
            float startScaleX = (float) startValues.values.get(PROPNAME_SCALE_X);
            return ObjectAnimator.ofFloat(startValues.view, "scaleX", startScaleX, 0.25f);
        }

    }

    /**
     * A custom {@link Visibility} with 4-arg onAppear/Disappear
     */
    public static class CustomVisibility2 extends Visibility {

        static final String PROPNAME_SCALE_X = "customVisibility:scaleX";

        @Override
        public void captureStartValues(@NonNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            transitionValues.values.put(PROPNAME_SCALE_X, transitionValues.view.getScaleX());
        }

        @Override
        public Animator onAppear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                TransitionValues startValues, TransitionValues endValues) {
            float startScaleX = startValues == null ? 0.25f :
                    (float) startValues.values.get(PROPNAME_SCALE_X);
            return ObjectAnimator.ofFloat(view, "scaleX", startScaleX, 0.75f);
        }

        @Override
        public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                TransitionValues startValues, TransitionValues endValues) {
            if (startValues == null) {
                return null;
            }
            float startScaleX = (float) startValues.values.get(PROPNAME_SCALE_X);
            return ObjectAnimator.ofFloat(view, "scaleX", startScaleX, 0.25f);
        }

    }

}
