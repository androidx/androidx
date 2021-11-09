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
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.test.filters.LargeTest;
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
