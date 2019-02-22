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
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.geq;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.leq;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

@LargeTest
public class ExplodeTest extends BaseTransitionTest {

    private View mRedSquare;
    private View mGreenSquare;
    private View mBlueSquare;
    private View mYellowSquare;

    @Override
    Transition createTransition() {
        return new Explode();
    }

    @Before
    public void prepareViews() {
        mRedSquare = spy(new View(rule.getActivity()));
        mGreenSquare = spy(new View(rule.getActivity()));
        mBlueSquare = spy(new View(rule.getActivity()));
        mYellowSquare = spy(new View(rule.getActivity()));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final FrameLayout frame = new FrameLayout(rule.getActivity());
                mRedSquare.setBackgroundColor(Color.RED);
                frame.addView(mRedSquare,
                        new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.TOP));
                mGreenSquare.setBackgroundColor(Color.GREEN);
                frame.addView(mGreenSquare,
                        new FrameLayout.LayoutParams(100, 100, Gravity.RIGHT | Gravity.TOP));
                mBlueSquare.setBackgroundColor(Color.BLUE);
                frame.addView(mBlueSquare,
                        new FrameLayout.LayoutParams(100, 100, Gravity.RIGHT | Gravity.BOTTOM));
                mYellowSquare.setBackgroundColor(Color.YELLOW);
                frame.addView(mYellowSquare,
                        new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.BOTTOM));
                mRoot.addView(frame,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
            }
        });
    }

    @Ignore("Temporarily disabled due to b/112005299")
    @Test
    public void testExplode() throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                mRedSquare.setVisibility(View.INVISIBLE);
                mGreenSquare.setVisibility(View.INVISIBLE);
                mBlueSquare.setVisibility(View.INVISIBLE);
                mYellowSquare.setVisibility(View.INVISIBLE);
            }
        });
        waitForStart();
        assertEquals(View.VISIBLE, mRedSquare.getVisibility());
        assertEquals(View.VISIBLE, mGreenSquare.getVisibility());
        assertEquals(View.VISIBLE, mBlueSquare.getVisibility());
        assertEquals(View.VISIBLE, mYellowSquare.getVisibility());

        verifyMovement(mRedSquare, Gravity.LEFT | Gravity.TOP, true);
        verifyMovement(mGreenSquare, Gravity.RIGHT | Gravity.TOP, true);
        verifyMovement(mBlueSquare, Gravity.RIGHT | Gravity.BOTTOM, true);
        verifyMovement(mYellowSquare, Gravity.LEFT | Gravity.BOTTOM, true);
        waitForEnd();

        verifyNoTranslation(mRedSquare);
        verifyNoTranslation(mGreenSquare);
        verifyNoTranslation(mBlueSquare);
        verifyNoTranslation(mYellowSquare);
        assertEquals(View.INVISIBLE, mRedSquare.getVisibility());
        assertEquals(View.INVISIBLE, mGreenSquare.getVisibility());
        assertEquals(View.INVISIBLE, mBlueSquare.getVisibility());
        assertEquals(View.INVISIBLE, mYellowSquare.getVisibility());
    }

    @Ignore("Temporarily disabled due to b/112005299")
    @Test
    public void testImplode() throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRedSquare.setVisibility(View.INVISIBLE);
                mGreenSquare.setVisibility(View.INVISIBLE);
                mBlueSquare.setVisibility(View.INVISIBLE);
                mYellowSquare.setVisibility(View.INVISIBLE);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                mRedSquare.setVisibility(View.VISIBLE);
                mGreenSquare.setVisibility(View.VISIBLE);
                mBlueSquare.setVisibility(View.VISIBLE);
                mYellowSquare.setVisibility(View.VISIBLE);
            }
        });
        waitForStart();

        assertEquals(View.VISIBLE, mRedSquare.getVisibility());
        assertEquals(View.VISIBLE, mGreenSquare.getVisibility());
        assertEquals(View.VISIBLE, mBlueSquare.getVisibility());
        assertEquals(View.VISIBLE, mYellowSquare.getVisibility());

        verifyMovement(mRedSquare, Gravity.LEFT | Gravity.TOP, false);
        verifyMovement(mGreenSquare, Gravity.RIGHT | Gravity.TOP, false);
        verifyMovement(mBlueSquare, Gravity.RIGHT | Gravity.BOTTOM, false);
        verifyMovement(mYellowSquare, Gravity.LEFT | Gravity.BOTTOM, false);
        waitForEnd();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verifyNoTranslation(mRedSquare);
        verifyNoTranslation(mGreenSquare);
        verifyNoTranslation(mBlueSquare);
        verifyNoTranslation(mYellowSquare);
        assertEquals(View.VISIBLE, mRedSquare.getVisibility());
        assertEquals(View.VISIBLE, mGreenSquare.getVisibility());
        assertEquals(View.VISIBLE, mBlueSquare.getVisibility());
        assertEquals(View.VISIBLE, mYellowSquare.getVisibility());
    }

    private void verifyMovement(View v, int direction, boolean movingOut) {
        final float startX = v.getTranslationX();
        final float startY = v.getTranslationY();
        final VerificationMode mode = timeout(1000).atLeastOnce();
        if ((direction & Gravity.LEFT) == Gravity.LEFT) {
            if (movingOut) {
                verify(v, mode).setTranslationX(and(lt(0f), lt(startX)));
            } else {
                verify(v, mode).setTranslationX(and(leq(0f), gt(startX)));
            }
        } else if ((direction & Gravity.RIGHT) == Gravity.RIGHT) {
            if (movingOut) {
                verify(v, mode).setTranslationX(and(gt(0f), gt(startX)));
            } else {
                verify(v, mode).setTranslationX(and(geq(0f), lt(startX)));
            }
        }
        if ((direction & Gravity.TOP) == Gravity.TOP) {
            if (movingOut) {
                verify(v, mode).setTranslationY(and(lt(0f), lt(startY)));
            } else {
                verify(v, mode).setTranslationY(and(leq(0f), gt(startY)));
            }
        } else if ((direction & Gravity.BOTTOM) == Gravity.BOTTOM) {
            if (movingOut) {
                verify(v, mode).setTranslationY(and(gt(0f), gt(startY)));
            } else {
                verify(v, mode).setTranslationY(and(geq(0f), lt(startY)));
            }
        }
    }

    private void verifyNoTranslation(View view) {
        assertEquals(0f, view.getTranslationX(), 0.0f);
        assertEquals(0f, view.getTranslationY(), 0.0f);
    }

}
