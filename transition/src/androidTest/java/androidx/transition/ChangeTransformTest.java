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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.support.test.filters.MediumTest;
import android.view.View;

import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class ChangeTransformTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeTransform();
    }

    @Test
    public void testTranslation() throws Throwable {
        enterScene(R.layout.scene1);

        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                redSquare.setTranslationX(500);
                redSquare.setTranslationY(600);
            }
        });
        waitForStart();

        verify(mListener, never()).onTransitionEnd(any(Transition.class)); // still running
        // There is no way to validate the intermediate matrix because it uses
        // hidden properties of the View to execute.
        waitForEnd();
        assertEquals(500f, redSquare.getTranslationX(), 0.0f);
        assertEquals(600f, redSquare.getTranslationY(), 0.0f);
    }

    @Test
    public void testRotation() throws Throwable {
        enterScene(R.layout.scene1);

        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                redSquare.setRotation(45);
            }
        });
        waitForStart();

        verify(mListener, never()).onTransitionEnd(any(Transition.class)); // still running
        // There is no way to validate the intermediate matrix because it uses
        // hidden properties of the View to execute.
        waitForEnd();
        assertEquals(45f, redSquare.getRotation(), 0.0f);
    }

    @Test
    public void testScale() throws Throwable {
        enterScene(R.layout.scene1);

        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                redSquare.setScaleX(2f);
                redSquare.setScaleY(3f);
            }
        });
        waitForStart();

        verify(mListener, never()).onTransitionEnd(any(Transition.class)); // still running
        // There is no way to validate the intermediate matrix because it uses
        // hidden properties of the View to execute.
        waitForEnd();
        assertEquals(2f, redSquare.getScaleX(), 0.0f);
        assertEquals(3f, redSquare.getScaleY(), 0.0f);
    }

    @Test
    public void testReparent() throws Throwable {
        final ChangeTransform changeTransform = (ChangeTransform) mTransition;
        assertEquals(true, changeTransform.getReparent());
        enterScene(R.layout.scene5);
        startTransition(R.layout.scene9);
        verify(mListener, never()).onTransitionEnd(any(Transition.class)); // still running
        waitForEnd();

        resetListener();
        changeTransform.setReparent(false);
        assertEquals(false, changeTransform.getReparent());
        startTransition(R.layout.scene5);
        waitForEnd(); // no transition to run because reparent == false
    }

}
