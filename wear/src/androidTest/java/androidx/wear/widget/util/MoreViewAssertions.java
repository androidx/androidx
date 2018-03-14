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

package androidx.wear.widget.util;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.util.HumanReadables;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MoreViewAssertions {

    public static ViewAssertion left(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View left: " + HumanReadables.describe(view), view.getLeft(), matcher);
            }
        };
    }

    public static ViewAssertion approximateTop(final Matcher<Double> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View top: " + HumanReadables.describe(view), ((double) view.getTop()),
                        matcher);
            }
        };
    }

    public static ViewAssertion top(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View top: " + HumanReadables.describe(view), view.getTop(), matcher);
            }
        };
    }

    public static ViewAssertion right(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View right: " + HumanReadables.describe(view), view.getRight(),
                        matcher);
            }
        };
    }

    public static ViewAssertion bottom(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View bottom: " + HumanReadables.describe(view), view.getBottom(),
                        matcher);
            }
        };
    }

    public static ViewAssertion approximateBottom(final Matcher<Double> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                assertThat("View bottom: " + HumanReadables.describe(view), ((double) view
                        .getBottom()), matcher);
            }
        };
    }

    /**
     * Returns a new ViewAssertion against a match of the view's left position, relative to the
     * left
     * edge of the containing window.
     *
     * @param matcher matcher for the left position
     */
    public static ViewAssertion screenLeft(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                int[] screenXy = {0, 0};
                view.getLocationInWindow(screenXy);
                assertThat("View screenLeft: " + HumanReadables.describe(view), screenXy[0],
                        matcher);
            }
        };
    }

    /**
     * Returns a new ViewAssertion against a match of the view's top position, relative to the top
     * edge of the containing window.
     *
     * @param matcher matcher for the top position
     */
    public static ViewAssertion screenTop(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                int[] screenXy = {0, 0};
                view.getLocationInWindow(screenXy);
                assertThat("View screenTop: " + HumanReadables.describe(view), screenXy[1],
                        matcher);
            }
        };
    }

    /**
     * Returns a new ViewAssertion against a match of the view's right position, relative to the
     * left
     * edge of the containing window.
     *
     * @param matcher matcher for the right position
     */
    public static ViewAssertion screenRight(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                int[] screenXy = {0, 0};
                view.getLocationInWindow(screenXy);
                assertThat("View screenRight: " + HumanReadables.describe(view),
                        screenXy[0] + view.getWidth(), matcher);
            }
        };
    }

    /**
     * Returns a new ViewAssertion against a match of the view's bottom position, relative to the
     * top
     * edge of the containing window.
     *
     * @param matcher matcher for the bottom position
     */
    public static ViewAssertion screenBottom(final Matcher<Integer> matcher) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewException) {
                int[] screenXy = {0, 0};
                view.getLocationInWindow(screenXy);
                assertThat("View screenBottom: " + HumanReadables.describe(view),
                        screenXy[1] + view.getHeight(), matcher);
            }
        };
    }

    public static Matcher<View> withTranslationX(final int xTranslation) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with x translation == " + xTranslation);
            }

            @Override
            public boolean matchesSafely(View view) {
                return view.getTranslationX() == xTranslation;
            }
        };
    }

    public static Matcher<RecyclerView> withPositiveVerticalScrollOffset() {
        return new TypeSafeMatcher<RecyclerView>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with positive y scroll offset");
            }

            @Override
            public boolean matchesSafely(RecyclerView view) {
                return view.computeVerticalScrollOffset() > 0;
            }
        };
    }

    public static Matcher<RecyclerView> withNoVerticalScrollOffset() {
        return new TypeSafeMatcher<RecyclerView>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with no y scroll offset");
            }

            @Override
            public boolean matchesSafely(RecyclerView view) {
                return view.computeVerticalScrollOffset() == 0;
            }
        };
    }
}
