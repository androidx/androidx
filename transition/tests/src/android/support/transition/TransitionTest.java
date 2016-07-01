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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.sameInstance;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.test.annotation.UiThreadTest;
import android.support.transition.test.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import org.junit.Test;

import java.util.List;

public class TransitionTest extends BaseTest {

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
    public void testTargets() {
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
    public void testListener() {
        Transition transition = new EmptyTransition();
        Transition.TransitionListener listener = new EmptyTransitionListener();
        assertThat(transition.addListener(listener), is(sameInstance(transition)));
        assertThat(transition.removeListener(listener), is(sameInstance(transition)));
    }

    public static class EmptyTransition extends Transition {

        public void captureEndValues(TransitionValues transitionValues) {
        }

        public void captureStartValues(TransitionValues transitionValues) {
        }

        public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                TransitionValues endValues) {
            return null;
        }

    }

    public static class EmptyTransitionListener implements Transition.TransitionListener {

        public void onTransitionStart(Transition transition) {
        }

        public void onTransitionEnd(Transition transition) {
        }

        public void onTransitionCancel(Transition transition) {
        }

        public void onTransitionPause(Transition transition) {
        }

        public void onTransitionResume(Transition transition) {
        }

    }

}
