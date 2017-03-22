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

package android.support.transition;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.filters.MediumTest;
import android.support.transition.test.R;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Test;

import java.util.List;

@MediumTest
public class TransitionInflaterTest extends BaseTest {

    @Test
    public void testInflationConstructors() throws Throwable {
        TransitionInflater inflater = TransitionInflater.from(rule.getActivity());
        // TODO: Add more Transition types
        Transition transition = inflater.inflateTransition(R.transition.transition_constructors);
        assertTrue(transition instanceof TransitionSet);
        TransitionSet set = (TransitionSet) transition;
        assertEquals(4, set.getTransitionCount());
    }

    @Test
    public void testInflation() {
        TransitionInflater inflater = TransitionInflater.from(rule.getActivity());
        // TODO: Add more Transition types
        verifyFadeProperties(inflater.inflateTransition(R.transition.fade));
        verifySlideProperties(inflater.inflateTransition(R.transition.slide));
        verifyExplodeProperties(inflater.inflateTransition(R.transition.explode));
        verifyChangeImageTransformProperties(
                inflater.inflateTransition(R.transition.change_image_transform));
        verifyChangeTransformProperties(inflater.inflateTransition(R.transition.change_transform));
        verifyChangeClipBoundsProperties(
                inflater.inflateTransition(R.transition.change_clip_bounds));
        verifyAutoTransitionProperties(inflater.inflateTransition(R.transition.auto_transition));
        verifyTransitionSetProperties(inflater.inflateTransition(R.transition.transition_set));
        verifyCustomTransitionProperties(
                inflater.inflateTransition(R.transition.custom_transition));
        verifyTargetIds(inflater.inflateTransition(R.transition.target_ids));
        verifyTargetNames(inflater.inflateTransition(R.transition.target_names));
        verifyTargetClass(inflater.inflateTransition(R.transition.target_classes));
    }

    // TODO: Add test for TransitionManager

    private void verifyFadeProperties(Transition transition) {
        assertTrue(transition instanceof Fade);
        Fade fade = (Fade) transition;
        assertEquals(Fade.OUT, fade.getMode());
    }

    private void verifySlideProperties(Transition transition) {
        assertTrue(transition instanceof Slide);
        Slide slide = (Slide) transition;
        assertEquals(Gravity.TOP, slide.getSlideEdge());
    }

    private void verifyExplodeProperties(Transition transition) {
        assertTrue(transition instanceof Explode);
        Visibility visibility = (Visibility) transition;
        assertEquals(Visibility.MODE_IN, visibility.getMode());
    }

    private void verifyChangeImageTransformProperties(Transition transition) {
        assertTrue(transition instanceof ChangeImageTransform);
    }

    private void verifyChangeTransformProperties(Transition transition) {
        assertTrue(transition instanceof ChangeTransform);
        ChangeTransform changeTransform = (ChangeTransform) transition;
        assertFalse(changeTransform.getReparent());
        assertFalse(changeTransform.getReparentWithOverlay());
    }

    private void verifyChangeClipBoundsProperties(Transition transition) {
        assertTrue(transition instanceof ChangeClipBounds);
    }

    private void verifyAutoTransitionProperties(Transition transition) {
        assertTrue(transition instanceof AutoTransition);
    }

    private void verifyTransitionSetProperties(Transition transition) {
        assertTrue(transition instanceof TransitionSet);
        TransitionSet set = (TransitionSet) transition;
        assertEquals(TransitionSet.ORDERING_SEQUENTIAL, set.getOrdering());
        assertEquals(2, set.getTransitionCount());
        assertTrue(set.getTransitionAt(0) instanceof ChangeBounds);
        assertTrue(set.getTransitionAt(1) instanceof Fade);
    }

    private void verifyCustomTransitionProperties(Transition transition) {
        assertTrue(transition instanceof CustomTransition);
    }

    private void verifyTargetIds(Transition transition) {
        List<Integer> targets = transition.getTargetIds();
        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertEquals(R.id.hello, (int) targets.get(0));
        assertEquals(R.id.world, (int) targets.get(1));
    }

    private void verifyTargetNames(Transition transition) {
        List<String> targets = transition.getTargetNames();
        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertEquals("hello", targets.get(0));
        assertEquals("world", targets.get(1));
    }

    private void verifyTargetClass(Transition transition) {
        List<Class> targets = transition.getTargetTypes();
        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertEquals(TextView.class, targets.get(0));
        assertEquals(ImageView.class, targets.get(1));
    }

    public static class CustomTransition extends Transition {
        public CustomTransition() {
            fail("Default constructor was not expected");
        }

        @SuppressWarnings("unused") // This constructor is used in XML
        public CustomTransition(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void captureStartValues(@NonNull TransitionValues transitionValues) {
        }

        @Override
        public void captureEndValues(@NonNull TransitionValues transitionValues) {
        }
    }

    public static class InflationFade extends Fade {
        public InflationFade(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    public static class InflationChangeBounds extends ChangeBounds {
        public InflationChangeBounds(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    public static class InflationTransitionSet extends TransitionSet {
        public InflationTransitionSet(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    public static class InflationAutoTransition extends AutoTransition {
        public InflationAutoTransition(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

}
