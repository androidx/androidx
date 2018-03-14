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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.view.View;

import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class ExplodeTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new Explode();
    }

    @Test
    public void testExplode() throws Throwable {
        enterScene(R.layout.scene10);
        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);
        final View greenSquare = rule.getActivity().findViewById(R.id.greenSquare);
        final View blueSquare = rule.getActivity().findViewById(R.id.blueSquare);
        final View yellowSquare = rule.getActivity().findViewById(R.id.yellowSquare);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                redSquare.setVisibility(View.INVISIBLE);
                greenSquare.setVisibility(View.INVISIBLE);
                blueSquare.setVisibility(View.INVISIBLE);
                yellowSquare.setVisibility(View.INVISIBLE);
            }
        });
        waitForStart();
        verify(mListener, never()).onTransitionEnd(any(Transition.class));
        assertEquals(View.VISIBLE, redSquare.getVisibility());
        assertEquals(View.VISIBLE, greenSquare.getVisibility());
        assertEquals(View.VISIBLE, blueSquare.getVisibility());
        assertEquals(View.VISIBLE, yellowSquare.getVisibility());
        float redStartX = redSquare.getTranslationX();
        float redStartY = redSquare.getTranslationY();

        SystemClock.sleep(100);
        verifyTranslation(redSquare, true, true);
        verifyTranslation(greenSquare, false, true);
        verifyTranslation(blueSquare, false, false);
        verifyTranslation(yellowSquare, true, false);
        assertThat(redStartX, is(greaterThan(redSquare.getTranslationX()))); // moving left
        assertThat(redStartY, is(greaterThan(redSquare.getTranslationY()))); // moving up
        waitForEnd();

        verifyNoTranslation(redSquare);
        verifyNoTranslation(greenSquare);
        verifyNoTranslation(blueSquare);
        verifyNoTranslation(yellowSquare);
        assertEquals(View.INVISIBLE, redSquare.getVisibility());
        assertEquals(View.INVISIBLE, greenSquare.getVisibility());
        assertEquals(View.INVISIBLE, blueSquare.getVisibility());
        assertEquals(View.INVISIBLE, yellowSquare.getVisibility());
    }

    @Test
    public void testImplode() throws Throwable {
        enterScene(R.layout.scene10);
        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);
        final View greenSquare = rule.getActivity().findViewById(R.id.greenSquare);
        final View blueSquare = rule.getActivity().findViewById(R.id.blueSquare);
        final View yellowSquare = rule.getActivity().findViewById(R.id.yellowSquare);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                redSquare.setVisibility(View.INVISIBLE);
                greenSquare.setVisibility(View.INVISIBLE);
                blueSquare.setVisibility(View.INVISIBLE);
                yellowSquare.setVisibility(View.INVISIBLE);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                redSquare.setVisibility(View.VISIBLE);
                greenSquare.setVisibility(View.VISIBLE);
                blueSquare.setVisibility(View.VISIBLE);
                yellowSquare.setVisibility(View.VISIBLE);
            }
        });
        waitForStart();

        assertEquals(View.VISIBLE, redSquare.getVisibility());
        assertEquals(View.VISIBLE, greenSquare.getVisibility());
        assertEquals(View.VISIBLE, blueSquare.getVisibility());
        assertEquals(View.VISIBLE, yellowSquare.getVisibility());
        float redStartX = redSquare.getTranslationX();
        float redStartY = redSquare.getTranslationY();

        SystemClock.sleep(100);
        verifyTranslation(redSquare, true, true);
        verifyTranslation(greenSquare, false, true);
        verifyTranslation(blueSquare, false, false);
        verifyTranslation(yellowSquare, true, false);
        assertThat(redStartX, is(lessThan(redSquare.getTranslationX()))); // moving right
        assertThat(redStartY, is(lessThan(redSquare.getTranslationY()))); // moving down
        waitForEnd();

        verifyNoTranslation(redSquare);
        verifyNoTranslation(greenSquare);
        verifyNoTranslation(blueSquare);
        verifyNoTranslation(yellowSquare);
        assertEquals(View.VISIBLE, redSquare.getVisibility());
        assertEquals(View.VISIBLE, greenSquare.getVisibility());
        assertEquals(View.VISIBLE, blueSquare.getVisibility());
        assertEquals(View.VISIBLE, yellowSquare.getVisibility());
    }

    private void verifyTranslation(View view, boolean goLeft, boolean goUp) {
        float translationX = view.getTranslationX();
        float translationY = view.getTranslationY();

        if (goLeft) {
            assertThat(translationX, is(lessThan(0.f)));
        } else {
            assertThat(translationX, is(greaterThan(0.f)));
        }

        if (goUp) {
            assertThat(translationY, is(lessThan(0.f)));
        } else {
            assertThat(translationY, is(greaterThan(0.f)));
        }
    }

    private void verifyNoTranslation(View view) {
        assertEquals(0f, view.getTranslationX(), 0.0f);
        assertEquals(0f, view.getTranslationY(), 0.0f);
    }

}
