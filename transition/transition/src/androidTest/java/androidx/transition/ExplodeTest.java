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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.os.BuildCompat;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

@LargeTest
public class ExplodeTest extends BaseTransitionTest {

    private TranslationView mRedSquare;
    private TranslationView mGreenSquare;
    private TranslationView mBlueSquare;
    private TranslationView mYellowSquare;

    @Override
    Transition createTransition() {
        return new Explode();
    }

    @Before
    public void prepareViews() throws Throwable {
        final Context context = rule.getActivity();
        mRedSquare = new TranslationView(context);
        mGreenSquare = new TranslationView(context);
        mBlueSquare = new TranslationView(context);
        mYellowSquare = new TranslationView(context);
        rule.runOnUiThread(new Runnable() {
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
        assertThat(mRedSquare, hasVisibility(View.VISIBLE));
        assertThat(mGreenSquare, hasVisibility(View.VISIBLE));
        assertThat(mBlueSquare, hasVisibility(View.VISIBLE));
        assertThat(mYellowSquare, hasVisibility(View.VISIBLE));
        waitForEnd();

        assertThat(mRedSquare.getHistoricalTranslationX(), is(decreasing()));
        assertThat(mRedSquare.getHistoricalTranslationY(), is(decreasing()));

        assertThat(mGreenSquare.getHistoricalTranslationX(), is(increasing()));
        assertThat(mGreenSquare.getHistoricalTranslationY(), is(decreasing()));

        assertThat(mBlueSquare.getHistoricalTranslationX(), is(increasing()));
        assertThat(mBlueSquare.getHistoricalTranslationY(), is(increasing()));

        assertThat(mYellowSquare.getHistoricalTranslationX(), is(decreasing()));
        assertThat(mYellowSquare.getHistoricalTranslationY(), is(increasing()));

        assertThat(mRedSquare, allOf(hasVisibility(View.INVISIBLE), hasNoTranslations()));
        assertThat(mGreenSquare, allOf(hasVisibility(View.INVISIBLE), hasNoTranslations()));
        assertThat(mBlueSquare, allOf(hasVisibility(View.INVISIBLE), hasNoTranslations()));
        assertThat(mYellowSquare, allOf(hasVisibility(View.INVISIBLE), hasNoTranslations()));
    }

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
        assertThat(mRedSquare, hasVisibility(View.VISIBLE));
        assertThat(mGreenSquare, hasVisibility(View.VISIBLE));
        assertThat(mBlueSquare, hasVisibility(View.VISIBLE));
        assertThat(mYellowSquare, hasVisibility(View.VISIBLE));
        waitForEnd();

        assertThat(mRedSquare.getHistoricalTranslationX(), is(increasing()));
        assertThat(mRedSquare.getHistoricalTranslationY(), is(increasing()));

        assertThat(mGreenSquare.getHistoricalTranslationX(), is(decreasing()));
        assertThat(mGreenSquare.getHistoricalTranslationY(), is(increasing()));

        assertThat(mBlueSquare.getHistoricalTranslationX(), is(decreasing()));
        assertThat(mBlueSquare.getHistoricalTranslationY(), is(decreasing()));

        assertThat(mYellowSquare.getHistoricalTranslationX(), is(increasing()));
        assertThat(mYellowSquare.getHistoricalTranslationY(), is(decreasing()));

        assertThat(mRedSquare, allOf(hasVisibility(View.VISIBLE), hasNoTranslations()));
        assertThat(mGreenSquare, allOf(hasVisibility(View.VISIBLE), hasNoTranslations()));
        assertThat(mBlueSquare, allOf(hasVisibility(View.VISIBLE), hasNoTranslations()));
        assertThat(mYellowSquare, allOf(hasVisibility(View.VISIBLE), hasNoTranslations()));
    }

    @Test
    public void precondition() {
        assertThat(Arrays.asList(1f, 2f, 3f), is(increasing()));
        assertThat(Arrays.asList(3f, 2f, 1f), is(decreasing()));
        assertThat(Arrays.asList(1f, 9f, 3f), is(not(increasing())));
        assertThat(Arrays.asList(1f, 9f, 3f), is(not(decreasing())));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingExplode() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new Explode());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            this.mRedSquare.setVisibility(View.GONE);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        float[] translationValues = new float[2];

        rule.runOnUiThread(() -> {
            assertEquals(1f, ViewUtils.getTransitionAlpha(mRedSquare), 0f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);

            // Seek past the always there transition before the explode
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);

            // Seek half way:
            seekController.setCurrentPlayTimeMillis(450);
            assertNotEquals(0f, mRedSquare.getTranslationX(), 0.01f);
            assertNotEquals(0f, mRedSquare.getTranslationY(), 0.01f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            translationValues[0] = mRedSquare.getTranslationX();
            translationValues[1] = mRedSquare.getTranslationY();

            // Seek past the end
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);
            assertEquals(View.GONE, mRedSquare.getVisibility());

            // Seek before the explode:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);

            seekController.setCurrentPlayTimeMillis(450);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.VISIBLE);
        });

        rule.runOnUiThread(() -> {
            // It should start from half way values and decrease
            assertEquals(translationValues[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(translationValues[1], mRedSquare.getTranslationY(), 1f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            seekControllerArr[0].setCurrentPlayTimeMillis(300);

            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingImplode() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        Explode implode = new Explode();
        implode.setInterpolator(new LinearInterpolator());
        transition.addTransition(implode);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        rule.runOnUiThread(() -> {
            mRedSquare.setVisibility(View.GONE);
        });

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            mRedSquare.setVisibility(View.VISIBLE);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        float[] translationValues = new float[2];

        rule.runOnUiThread(() -> {
            assertEquals(1f, ViewUtils.getTransitionAlpha(mRedSquare), 0f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertNotEquals(0f, mRedSquare.getTranslationX(), 0.01f);
            assertNotEquals(0f, mRedSquare.getTranslationY(), 0.01f);

            float startX = mRedSquare.getTranslationX();
            float startY = mRedSquare.getTranslationY();

            // Seek past the always there transition before the explode
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(startX, mRedSquare.getTranslationX(), 0f);
            assertEquals(startY, mRedSquare.getTranslationY(), 0f);

            // Seek half way:
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(startX / 2f, mRedSquare.getTranslationX(), 0.01f);
            assertEquals(startY / 2f, mRedSquare.getTranslationY(), 0.01f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            translationValues[0] = mRedSquare.getTranslationX();
            translationValues[1] = mRedSquare.getTranslationY();

            // Seek past the end
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());

            // Seek before the explode:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());
            assertEquals(startX, mRedSquare.getTranslationX(), 0f);
            assertEquals(startY, mRedSquare.getTranslationY(), 0f);

            seekController.setCurrentPlayTimeMillis(450);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.GONE);
        });

        rule.runOnUiThread(() -> {
            // It should start from half way values and increase
            assertEquals(translationValues[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(translationValues[1], mRedSquare.getTranslationY(), 1f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());

            seekControllerArr[0].setCurrentPlayTimeMillis(300);
            assertEquals(View.GONE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingImplodeBeforeStart() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        Explode implode = new Explode();
        implode.setInterpolator(new LinearInterpolator());
        transition.addTransition(implode);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        rule.runOnUiThread(() -> {
            mRedSquare.setVisibility(View.GONE);
        });

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            mRedSquare.setVisibility(View.VISIBLE);
        });

        float[] translationValues = new float[2];

        rule.runOnUiThread(() -> {
            translationValues[0] = mRedSquare.getTranslationX();
            translationValues[1] = mRedSquare.getTranslationY();
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.GONE);
        });

        rule.runOnUiThread(() -> {
            // It should start from all the way out
            assertEquals(translationValues[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(translationValues[1], mRedSquare.getTranslationY(), 1f);
            assertEquals(View.VISIBLE, mRedSquare.getVisibility());

            seekControllerArr[0].setCurrentPlayTimeMillis(300);
            assertEquals(View.GONE, mRedSquare.getVisibility());
            assertEquals(0f, mRedSquare.getTranslationX(), 0f);
            assertEquals(0f, mRedSquare.getTranslationY(), 0f);
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekWithTranslation() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        rule.runOnUiThread(() -> {
            mRedSquare.setTranslationX(1f);
            mRedSquare.setTranslationY(5f);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.GONE);
        });

        final float[] interruptedTranslation = new float[2];

        rule.runOnUiThread(() -> {
            assertEquals(1f, mRedSquare.getTranslationX(), 0.01f);
            assertEquals(5f, mRedSquare.getTranslationY(), 0.01f);


            seekControllerArr[0].setCurrentPlayTimeMillis(150);
            interruptedTranslation[0] = mRedSquare.getTranslationX();
            interruptedTranslation[1] = mRedSquare.getTranslationY();

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.VISIBLE);
        });

        rule.runOnUiThread(() -> {
            // It should start from half way values and increase
            assertEquals(interruptedTranslation[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(interruptedTranslation[1], mRedSquare.getTranslationY(), 1f);

            // make sure it would go to the start value
            seekControllerArr[0].setCurrentPlayTimeMillis(300);
            assertEquals(1f, mRedSquare.getTranslationX(), 0.01f);
            assertEquals(5f, mRedSquare.getTranslationY(), 0.01f);

            // Now go back to the interrupted position again:
            seekControllerArr[0].setCurrentPlayTimeMillis(0);
            assertEquals(interruptedTranslation[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(interruptedTranslation[1], mRedSquare.getTranslationY(), 1f);

            // Send it back to GONE
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.GONE);
        });

        rule.runOnUiThread(() -> {
            assertEquals(interruptedTranslation[0], mRedSquare.getTranslationX(), 1f);
            assertEquals(interruptedTranslation[1], mRedSquare.getTranslationY(), 1f);

            // it should move away (toward the top-left)
            seekControllerArr[0].setCurrentPlayTimeMillis(299);
            assertTrue(mRedSquare.getTranslationX() < interruptedTranslation[0]);
            assertTrue(mRedSquare.getTranslationY() < interruptedTranslation[1]);

            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, new Explode());
            mRedSquare.setVisibility(View.VISIBLE);
        });

        rule.runOnUiThread(() -> {
            // It should end up at the initial translation
            seekControllerArr[0].setCurrentPlayTimeMillis(300);
            assertEquals(1f, mRedSquare.getTranslationX(), 0.01f);
            assertEquals(5f, mRedSquare.getTranslationY(), 0.01f);
        });
    }

    private Matcher<View> hasVisibility(final int visibility) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has visibility " + visibility);
            }

            @Override
            protected boolean matchesSafely(View item) {
                return item.getVisibility() == visibility;
            }
        };
    }

    private Matcher<View> hasNoTranslations() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has no translationX or Y");
            }

            @Override
            protected boolean matchesSafely(View item) {
                return item.getTranslationX() == 0f && item.getTranslationY() == 0f;
            }
        };
    }

    private ListOrderingMatcher increasing() {
        return new ListOrderingMatcher(false);
    }

    private ListOrderingMatcher decreasing() {
        return new ListOrderingMatcher(true);
    }

    private static class ListOrderingMatcher extends TypeSafeMatcher<List<Float>> {

        private final boolean mDesc;

        ListOrderingMatcher(boolean desc) {
            mDesc = desc;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("List is ordered");
        }

        @Override
        protected boolean matchesSafely(List<Float> item) {
            for (int i = 0, max = item.size() - 1; i < max; i++) {
                if (mDesc) {
                    if (item.get(i) < item.get(i + 1)) {
                        return false;
                    }
                } else {
                    if (item.get(i) > item.get(i + 1)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
