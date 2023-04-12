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

import static androidx.transition.AtLeastOnceWithin.atLeastOnceWithin;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;

import androidx.core.util.Pair;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.ArrayList;

@SmallTest
public class SlideEdgeTest extends BaseTransitionTest {

    private static final ArrayList<Pair<Integer, String>> SLIDE_EDGES = new ArrayList<>();

    static {
        SLIDE_EDGES.add(new Pair<>(Gravity.START, "START"));
        SLIDE_EDGES.add(new Pair<>(Gravity.END, "END"));
        SLIDE_EDGES.add(new Pair<>(Gravity.LEFT, "LEFT"));
        SLIDE_EDGES.add(new Pair<>(Gravity.TOP, "TOP"));
        SLIDE_EDGES.add(new Pair<>(Gravity.RIGHT, "RIGHT"));
        SLIDE_EDGES.add(new Pair<>(Gravity.BOTTOM, "BOTTOM"));
    }

    @Test
    public void testSetSide() {
        for (int i = 0, size = SLIDE_EDGES.size(); i < size; i++) {
            final Pair<Integer, String> pair = SLIDE_EDGES.get(i);
            int slideEdge = pair.first;
            String edgeName = pair.second;
            Slide slide = new Slide(slideEdge);
            assertEquals("Edge not set properly in constructor " + edgeName,
                    slideEdge, slide.getSlideEdge());

            slide = new Slide();
            slide.setSlideEdge(slideEdge);
            assertEquals("Edge not set properly with setter " + edgeName,
                    slideEdge, slide.getSlideEdge());
        }
    }

    @LargeTest
    @Test
    public void testSlideOut() throws Throwable {
        for (int i = 0, size = SLIDE_EDGES.size(); i < size; i++) {
            int slideEdge = SLIDE_EDGES.get(i).first;
            final Slide slide = new Slide(slideEdge);
            final Transition.TransitionListener listener =
                    spy(new TransitionListenerAdapter());
            slide.addListener(listener);

            final View redSquare = spy(new View(rule.getActivity()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    redSquare.setBackgroundColor(Color.RED);
                    mRoot.addView(redSquare, 100, 100);
                }
            });

            rule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TransitionManager.beginDelayedTransition(mRoot, slide);
                    redSquare.setVisibility(View.INVISIBLE);
                }
            });
            verify(listener, atLeastOnceWithin(3000)).onTransitionStart(any(Transition.class));
            assertEquals(View.VISIBLE, redSquare.getVisibility());

            float redStartX = redSquare.getTranslationX();
            float redStartY = redSquare.getTranslationY();

            switch (slideEdge) {
                case Gravity.LEFT:
                case Gravity.START:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationX(and(lt(0.f), lt(redStartX)));
                    verify(redSquare, never()).setTranslationY(not(eq(0f, 0.01f)));
                    break;
                case Gravity.RIGHT:
                case Gravity.END:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationX(and(gt(0.f), gt(redStartX)));
                    verify(redSquare, never()).setTranslationY(not(eq(0f, 0.01f)));
                    break;
                case Gravity.TOP:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationY(and(lt(0.f), lt(redStartY)));
                    verify(redSquare, never()).setTranslationX(not(eq(0f, 0.01f)));
                    break;
                case Gravity.BOTTOM:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationY(and(gt(0.f), gt(redStartY)));
                    verify(redSquare, never()).setTranslationX(not(eq(0f, 0.01f)));
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect slideEdge");
            }

            verify(listener, atLeastOnceWithin(1000)).onTransitionEnd(any(Transition.class));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            verifyNoTranslation(redSquare);
            assertEquals(View.INVISIBLE, redSquare.getVisibility());
        }
    }

    @LargeTest
    @Test
    public void testSlideIn() throws Throwable {
        for (int i = 0, size = SLIDE_EDGES.size(); i < size; i++) {
            final Pair<Integer, String> pair = SLIDE_EDGES.get(i);
            int slideEdge = pair.first;
            final Slide slide = new Slide(slideEdge);
            final Transition.TransitionListener listener =
                    spy(new TransitionListenerAdapter());
            slide.addListener(listener);


            final View redSquare = spy(new View(rule.getActivity()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    redSquare.setBackgroundColor(Color.RED);
                    mRoot.addView(redSquare, 100, 100);
                    redSquare.setVisibility(View.INVISIBLE);
                }
            });

            // now slide in
            rule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TransitionManager.beginDelayedTransition(mRoot, slide);
                    redSquare.setVisibility(View.VISIBLE);
                }
            });

            verify(listener, atLeastOnceWithin(3000)).onTransitionStart(any(Transition.class));
            assertEquals(View.VISIBLE, redSquare.getVisibility());

            final float redStartX = redSquare.getTranslationX();
            final float redStartY = redSquare.getTranslationY();

            switch (slideEdge) {
                case Gravity.LEFT:
                case Gravity.START:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationX(and(gt(redStartX), lt(0.f)));
                    verify(redSquare, never()).setTranslationY(not(eq(0f, 0.01f)));
                    break;
                case Gravity.RIGHT:
                case Gravity.END:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationX(and(gt(0.f), lt(redStartX)));
                    verify(redSquare, never()).setTranslationY(not(eq(0f, 0.01f)));
                    break;
                case Gravity.TOP:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationY(and(gt(redStartY), lt(0.f)));
                    verify(redSquare, never()).setTranslationX(not(eq(0f, 0.01f)));
                    break;
                case Gravity.BOTTOM:
                    verify(redSquare, atLeastOnceWithin(1000))
                            .setTranslationY(and(gt(0.f), lt(redStartY)));
                    verify(redSquare, never()).setTranslationX(not(eq(0f, 0.01f)));
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect slideEdge");
            }
            verify(listener, atLeastOnceWithin(1000)).onTransitionEnd(any(Transition.class));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            verifyNoTranslation(redSquare);
            assertEquals(View.VISIBLE, redSquare.getVisibility());
        }
    }

    private void verifyNoTranslation(View view) {
        assertEquals(0f, view.getTranslationX(), 0.01f);
        assertEquals(0f, view.getTranslationY(), 0.01f);
    }

}
