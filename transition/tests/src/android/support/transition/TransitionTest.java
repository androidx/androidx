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


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.transition.test.R;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

@MediumTest
public class TransitionTest extends BaseTest {

    private Scene[] mScenes = new Scene[2];

    @Before
    public void prepareScenes() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        mScenes[0] = Scene.getSceneForLayout(root, R.layout.scene0, activity);
        mScenes[1] = Scene.getSceneForLayout(root, R.layout.scene1, activity);
    }

    @Test
    public void testName() {
        Transition transition = new EmptyTransition();
        assertThat(transition.getName(),
                is(equalTo("android.support.transition.TransitionTest$EmptyTransition")));
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
        View container = LayoutInflater.from(activity).inflate(R.layout.scene0, root, false);
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
        List<Class> targetTypes = transition.getTargetTypes();
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

    private void showInitialScene() throws Throwable {
        SyncRunnable enter0 = new SyncRunnable();
        mScenes[0].setEnterAction(enter0);
        AutoTransition transition1 = new AutoTransition();
        transition1.setDuration(0);
        goToScene(mScenes[0], transition1);
        if (!enter0.await()) {
            fail("Timed out while waiting for scene change");
        }
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

        public void captureEndValues(@NonNull TransitionValues transitionValues) {
        }

        public void captureStartValues(@NonNull TransitionValues transitionValues) {
        }

        public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                @Nullable TransitionValues startValues,
                @Nullable TransitionValues endValues) {
            return null;
        }

    }

    public static class EmptyTransitionListener implements Transition.TransitionListener {

        public void onTransitionStart(@NonNull Transition transition) {
        }

        public void onTransitionEnd(@NonNull Transition transition) {
        }

        public void onTransitionCancel(@NonNull Transition transition) {
        }

        public void onTransitionPause(@NonNull Transition transition) {
        }

        public void onTransitionResume(@NonNull Transition transition) {
        }

    }

}
