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


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;
import androidx.transition.test.R;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

@MediumTest
public class TransitionTest extends BaseTest {

    private Scene[] mScenes = new Scene[2];
    private View[] mViews = new View[3];

    @Before
    public void prepareScenes() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        mScenes[0] = Scene.getSceneForLayout(root, R.layout.support_scene0, activity);
        mScenes[1] = Scene.getSceneForLayout(root, R.layout.support_scene1, activity);
    }

    @Test
    public void testName() {
        Transition transition = new EmptyTransition();
        assertThat(transition.getName(),
                is(equalTo("androidx.transition.TransitionTest$EmptyTransition")));
    }

    @Test
    public void testDuration() {
        Transition transition = new EmptyTransition();
        long duration = 12345;
        assertThat(transition.setDuration(duration), is(sameInstance(transition)));
        assertThat(transition.getDuration(), is(duration));
    }

    @Test
    public void testInterpolator() {
        Transition transition = new EmptyTransition();
        TimeInterpolator interpolator = new LinearInterpolator();
        assertThat(transition.setInterpolator(interpolator), is(sameInstance(transition)));
        assertThat(transition.getInterpolator(), is(interpolator));
    }

    @Test
    public void testStartDelay() {
        Transition transition = new EmptyTransition();
        long startDelay = 12345;
        assertThat(transition.setStartDelay(startDelay), is(sameInstance(transition)));
        assertThat(transition.getStartDelay(), is(startDelay));
    }

    @Test
    public void testTargetIds() {
        Transition transition = new EmptyTransition();
        assertThat(transition.addTarget(R.id.view0), is(sameInstance(transition)));
        assertThat(transition.addTarget(R.id.view1), is(sameInstance(transition)));
        List<Integer> targetIds = transition.getTargetIds();
        assertThat(targetIds.size(), is(2));
        assertThat(targetIds, hasItem(R.id.view0));
        assertThat(targetIds, hasItem(R.id.view1));
        assertThat(transition.removeTarget(R.id.view0), is(sameInstance(transition)));
        targetIds = transition.getTargetIds();
        assertThat(targetIds.size(), is(1));
        assertThat(targetIds, not(hasItem(R.id.view0)));
        assertThat(targetIds, hasItem(R.id.view1));
    }

    @Test
    @UiThreadTest
    public void testTargetView() {
        // Set up views
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        View container = LayoutInflater.from(activity)
                .inflate(R.layout.support_scene0, root, false);
        root.addView(container);
        View view0 = container.findViewById(R.id.view0);
        View view1 = container.findViewById(R.id.view1);
        // Test transition targets
        Transition transition = new EmptyTransition();
        assertThat(transition.addTarget(view0), is(sameInstance(transition)));
        assertThat(transition.addTarget(view1), is(sameInstance(transition)));
        List<View> targets = transition.getTargets();
        assertThat(targets.size(), is(2));
        assertThat(targets, hasItem(sameInstance(view0)));
        assertThat(targets, hasItem(sameInstance(view1)));
        assertThat(transition.removeTarget(view0), is(sameInstance(transition)));
        targets = transition.getTargets();
        assertThat(targets.size(), is(1));
        assertThat(targets, not(hasItem(sameInstance(view0))));
        assertThat(targets, hasItem(sameInstance(view1)));
    }

    @Test
    public void testTargetName() {
        Transition transition = new EmptyTransition();
        assertThat(transition.addTarget("a"), is(sameInstance(transition)));
        assertThat(transition.addTarget("b"), is(sameInstance(transition)));
        List<String> targetNames = transition.getTargetNames();
        assertNotNull(targetNames);
        assertThat(targetNames.size(), is(2));
        assertThat(targetNames, hasItem("a"));
        assertThat(targetNames, hasItem("b"));
        transition.removeTarget("a");
        assertThat(targetNames.size(), is(1));
        assertThat(targetNames, not(hasItem("a")));
        assertThat(targetNames, hasItem("b"));
    }

    @Test
    public void testTargetType() {
        Transition transition = new EmptyTransition();
        assertThat(transition.addTarget(Button.class), is(sameInstance(transition)));
        assertThat(transition.addTarget(ImageView.class), is(sameInstance(transition)));
        List<Class<?>> targetTypes = transition.getTargetTypes();
        assertNotNull(targetTypes);
        assertThat(targetTypes.size(), is(2));
        assertThat(targetTypes, hasItem(Button.class));
        assertThat(targetTypes, hasItem(ImageView.class));
        transition.removeTarget(Button.class);
        assertThat(targetTypes.size(), is(1));
        assertThat(targetTypes, not(hasItem(Button.class)));
        assertThat(targetTypes, hasItem(ImageView.class));
    }

    @Test
    public void testExcludeTargetId() throws Throwable {
        showInitialScene();
        Transition transition = new EmptyTransition();
        transition.addTarget(R.id.view0);
        transition.addTarget(R.id.view1);
        View view0 = rule.getActivity().findViewById(R.id.view0);
        View view1 = rule.getActivity().findViewById(R.id.view1);
        assertThat(transition.isValidTarget(view0), is(true));
        assertThat(transition.isValidTarget(view1), is(true));
        transition.excludeTarget(R.id.view0, true);
        assertThat(transition.isValidTarget(view0), is(false));
        assertThat(transition.isValidTarget(view1), is(true));
    }

    @Test
    public void testExcludeTargetView() throws Throwable {
        showInitialScene();
        Transition transition = new EmptyTransition();
        View view0 = rule.getActivity().findViewById(R.id.view0);
        View view1 = rule.getActivity().findViewById(R.id.view1);
        transition.addTarget(view0);
        transition.addTarget(view1);
        assertThat(transition.isValidTarget(view0), is(true));
        assertThat(transition.isValidTarget(view1), is(true));
        transition.excludeTarget(view0, true);
        assertThat(transition.isValidTarget(view0), is(false));
        assertThat(transition.isValidTarget(view1), is(true));
    }

    @Test
    public void testExcludeTargetName() throws Throwable {
        showInitialScene();
        Transition transition = new EmptyTransition();
        View view0 = rule.getActivity().findViewById(R.id.view0);
        View view1 = rule.getActivity().findViewById(R.id.view1);
        ViewCompat.setTransitionName(view0, "zero");
        ViewCompat.setTransitionName(view1, "one");
        transition.addTarget("zero");
        transition.addTarget("one");
        assertThat(transition.isValidTarget(view0), is(true));
        assertThat(transition.isValidTarget(view1), is(true));
        transition.excludeTarget("zero", true);
        assertThat(transition.isValidTarget(view0), is(false));
        assertThat(transition.isValidTarget(view1), is(true));
    }

    @Test
    public void testExcludeTargetType() throws Throwable {
        showInitialScene();
        Transition transition = new EmptyTransition();
        FrameLayout container = (FrameLayout) rule.getActivity().findViewById(R.id.container);
        View view0 = rule.getActivity().findViewById(R.id.view0);
        transition.addTarget(View.class);
        assertThat(transition.isValidTarget(container), is(true));
        assertThat(transition.isValidTarget(view0), is(true));
        transition.excludeTarget(FrameLayout.class, true);
        assertThat(transition.isValidTarget(container), is(false));
        assertThat(transition.isValidTarget(view0), is(true));
    }

    @Test
    public void testListener() {
        Transition transition = new EmptyTransition();
        Transition.TransitionListener listener = new EmptyTransitionListener();
        assertThat(transition.addListener(listener), is(sameInstance(transition)));
        assertThat(transition.removeListener(listener), is(sameInstance(transition)));
    }

    @Test
    public void testMatchOrder() throws Throwable {
        showInitialScene();
        final Transition transition = new ChangeBounds() {
            @Nullable
            @Override
            public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                    @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
                if (startValues != null && endValues != null) {
                    fail("Match by View ID should be prevented");
                }
                return super.createAnimator(sceneRoot, startValues, endValues);
            }
        };
        transition.setDuration(0);
        // This prevents matches between start and end scenes because they have different set of
        // View instances. They will be regarded as independent views even though they share the
        // same View IDs.
        transition.setMatchOrder(Transition.MATCH_INSTANCE);
        SyncRunnable enter1 = new SyncRunnable();
        mScenes[1].setEnterAction(enter1);
        goToScene(mScenes[1], transition);
        if (!enter1.await()) {
            fail("Timed out while waiting for scene change");
        }
    }

    @Test
    public void testExcludedTransitionAnimator() throws Throwable {
        showInitialScene();
        final Animator.AnimatorListener animatorListener = mock(Animator.AnimatorListener.class);
        final DummyTransition transition = new DummyTransition(animatorListener);
        final SyncTransitionListener transitionListener = new SyncTransitionListener(
                SyncTransitionListener.EVENT_END);
        transition.addListener(transitionListener);
        transition.excludeTarget(mViews[0], true);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(rule.getActivity().getRoot(), transition);
                mViews[0].setTranslationX(3.f);
            }
        });
        if (!transitionListener.await()) {
            fail("Timed out waiting for the TransitionListener");
        }
        verify(animatorListener, never()).onAnimationStart(any(Animator.class));
    }

    @Test
    public void testEpicenter() throws Throwable {
        final Transition transition = new EmptyTransition();
        final Transition.EpicenterCallback epicenterCallback = new Transition.EpicenterCallback() {
            private Rect mRect = new Rect();

            @Override
            public Rect onGetEpicenter(@NonNull Transition t) {
                assertThat(t, is(sameInstance(transition)));
                mRect.set(1, 2, 3, 4);
                return mRect;
            }
        };
        transition.setEpicenterCallback(epicenterCallback);
        assertThat(transition.getEpicenterCallback(),
                is(sameInstance(transition.getEpicenterCallback())));
        Rect rect = transition.getEpicenter();
        assertNotNull(rect);
        assertThat(rect.left, is(1));
        assertThat(rect.top, is(2));
        assertThat(rect.right, is(3));
        assertThat(rect.bottom, is(4));
    }

    @Test
    public void testSetPropagation() throws Throwable {
        final Transition transition = new EmptyTransition();
        assertThat(transition.getPropagation(), is(nullValue()));
        final TransitionPropagation propagation = new CircularPropagation();
        transition.setPropagation(propagation);
        assertThat(transition.getPropagation(), is(sameInstance(propagation)));
    }

    @Test
    public void testPropagationApplied() throws Throwable {
        showInitialScene();
        final Animator[] animators = new Animator[3];
        final Transition transition = new DummyTransition(null) {

            @Override
            public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                    TransitionValues startValues, TransitionValues endValues) {
                View view = startValues.view;
                int index = view == mViews[0] ? 0 : view == mViews[1] ? 1 : 2;
                animators[index] = super.createAnimator(sceneRoot, startValues, endValues);
                return animators[index];
            }
        };
        transition.setPropagation(new FirstViewTransitionPropagation());
        final SyncTransitionListener transitionListener = new SyncTransitionListener(
                SyncTransitionListener.EVENT_START);
        transition.addListener(transitionListener);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(rule.getActivity().getRoot(), transition);
                mViews[0].setTranslationX(3.f);
                mViews[1].setTranslationX(3.f);
                mViews[2].setTranslationX(3.f);
            }
        });
        if (!transitionListener.await()) {
            fail("Timed out waiting for the TransitionListener");
        }
        assertEquals(20, animators[0].getStartDelay());
        assertEquals(10, animators[1].getStartDelay());
        assertEquals(0, animators[2].getStartDelay());
    }

    @Test
    public void testIsTransitionRequired() throws Throwable {
        View fakeView = rule.getActivity().findViewById(R.id.view0);
        final EmptyTransition transition = new EmptyTransition();
        assertThat(transition.isTransitionRequired(null, null), is(false));
        final TransitionValues start = new TransitionValues(fakeView);
        final String propname = "android:transition:dummy";
        start.values.put(propname, 1);
        final TransitionValues end = new TransitionValues(fakeView);
        end.values.put(propname, 1);
        assertThat(transition.isTransitionRequired(start, end), is(false));
        end.values.put(propname, 2);
        assertThat(transition.isTransitionRequired(start, end), is(true));
    }

    private void showInitialScene() throws Throwable {
        SyncRunnable enter0 = new SyncRunnable();
        mScenes[0].setEnterAction(enter0);
        AutoTransition transition1 = new AutoTransition();
        transition1.setDuration(0);
        goToScene(mScenes[0], transition1);
        if (!enter0.await()) {
            fail("Timed out while waiting for scene change");
        }
        mViews[0] = rule.getActivity().findViewById(R.id.view0);
        mViews[1] = rule.getActivity().findViewById(R.id.view1);
        mViews[2] = rule.getActivity().findViewById(R.id.view2);
    }

    private void goToScene(final Scene scene, final Transition transition) throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.go(scene, transition);
            }
        });
    }

    public static class EmptyTransition extends Transition {

        @Override
        public void captureEndValues(@NonNull TransitionValues transitionValues) {
        }

        @Override
        public void captureStartValues(@NonNull TransitionValues transitionValues) {
        }

        @Override
        public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                @Nullable TransitionValues startValues,
                @Nullable TransitionValues endValues) {
            return null;
        }

    }

    public static class EmptyTransitionListener implements Transition.TransitionListener {

        @Override
        public void onTransitionStart(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
        }

    }

    /**
     * A dummy transition for monitoring use of its animator by the Transition framework.
     */
    private static class DummyTransition extends Transition {

        private final Animator.AnimatorListener mListener;

        DummyTransition(Animator.AnimatorListener listener) {
            mListener = listener;
        }

        @Override
        public void captureStartValues(@NonNull TransitionValues transitionValues) {
            captureValues(transitionValues);
        }

        @Override
        public void captureEndValues(@NonNull TransitionValues transitionValues) {
            captureValues(transitionValues);
        }

        private void captureValues(@NonNull TransitionValues transitionValues) {
            transitionValues.values.put("state", transitionValues.view.getTranslationX());
        }

        @Override
        public Animator createAnimator(@NonNull ViewGroup sceneRoot, TransitionValues startValues,
                TransitionValues endValues) {
            if (startValues == null || endValues == null) {
                return null;
            }
            final ObjectAnimator animator = ObjectAnimator
                    .ofFloat(startValues.view, "translationX", 1.f, 2.f);
            if (mListener != null) {
                animator.addListener(mListener);
            }
            return animator;
        }

    }

    /**
     * A propagation which adds 20 delay for the 1st view and 10 delay for the 2nd view
     */
    private class FirstViewTransitionPropagation extends TransitionPropagation {
        @Override
        public long getStartDelay(ViewGroup sceneRoot, Transition transition,
                TransitionValues startValues, TransitionValues endValues) {
            return startValues.view == mViews[0] ? 20 :
                    startValues.view == mViews[1] ? 10 : 0;
        }

        @Override
        public void captureValues(TransitionValues transitionValues) {
        }

        @Override
        public String[] getPropagationProperties() {
            return new String[0];
        }
    }
}
