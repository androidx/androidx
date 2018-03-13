/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.testutils;

import android.database.sqlite.SQLiteCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.core.view.TintableBackgroundView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

public class TestUtilsMatchers {
    /**
     * Returns a matcher that matches <code>ImageView</code>s which have drawable flat-filled
     * with the specific color.
     */
    public static Matcher drawable(@ColorInt final int color) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {
            private String failedComparisonDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("with drawable of color: ");

                description.appendText(failedComparisonDescription);
            }

            @Override
            public boolean matchesSafely(final ImageView view) {
                Drawable drawable = view.getDrawable();
                if (drawable == null) {
                    return false;
                }

                // One option is to check if we have a ColorDrawable and then call getColor
                // but that API is v11+. Instead, we call our helper method that checks whether
                // all pixels in a Drawable are of the same specified color.
                try {
                    TestUtils.assertAllPixelsOfColor("", drawable, view.getWidth(),
                            view.getHeight(), true, color, 0, true);
                    // If we are here, the color comparison has passed.
                    failedComparisonDescription = null;
                    return true;
                } catch (Throwable t) {
                    // If we are here, the color comparison has failed.
                    failedComparisonDescription = t.getMessage();
                    return false;
                }
            }
        };
    }

    /**
     * Returns a matcher that matches <code>View</code>s which have background flat-filled
     * with the specific color.
     */
    public static Matcher isBackground(@ColorInt final int color) {
        return isBackground(color, false);
    }

    /**
     * Returns a matcher that matches <code>View</code>s which have background flat-filled
     * with the specific color.
     */
    public static Matcher isBackground(@ColorInt final int color, final boolean onlyTestCenter) {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedComparisonDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("with background of color: ");

                description.appendText(failedComparisonDescription);
            }

            @Override
            public boolean matchesSafely(final View view) {
                Drawable drawable = view.getBackground();
                if (drawable == null) {
                    return false;
                }
                try {
                    if (onlyTestCenter) {
                        TestUtils.assertCenterPixelOfColor("", drawable, view.getWidth(),
                                view.getHeight(), false, color, 0, true);
                    } else {
                        TestUtils.assertAllPixelsOfColor("", drawable, view.getWidth(),
                                view.getHeight(), false, color, 0, true);
                    }
                    // If we are here, the color comparison has passed.
                    failedComparisonDescription = null;
                    return true;
                } catch (Throwable t) {
                    // If we are here, the color comparison has failed.
                    failedComparisonDescription = t.getMessage();
                    return false;
                }
            }
        };
    }

    /**
     * Returns a matcher that matches <code>View</code>s whose combined background starting
     * from the view and up its ancestor chain matches the specified color.
     */
    public static Matcher isCombinedBackground(@ColorInt final int color,
            final boolean onlyTestCenterPixel) {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedComparisonDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("with ascendant background of color: ");

                description.appendText(failedComparisonDescription);
            }

            @Override
            public boolean matchesSafely(View view) {
                // Create a bitmap with combined backgrounds of the view and its ancestors.
                Bitmap combinedBackgroundBitmap = TestUtils.getCombinedBackgroundBitmap(view);
                try {
                    if (onlyTestCenterPixel) {
                        TestUtils.assertCenterPixelOfColor("", combinedBackgroundBitmap,
                                color, 0, true);
                    } else {
                        TestUtils.assertAllPixelsOfColor("", combinedBackgroundBitmap,
                                combinedBackgroundBitmap.getWidth(),
                                combinedBackgroundBitmap.getHeight(), color, 0, true);
                    }
                    // If we are here, the color comparison has passed.
                    failedComparisonDescription = null;
                    return true;
                } catch (Throwable t) {
                    failedComparisonDescription = t.getMessage();
                    return false;
                } finally {
                    combinedBackgroundBitmap.recycle();
                }
            }
        };
    }

    /**
     * Returns a matcher that matches <code>CheckedTextView</code>s which are in checked state.
     */
    public static Matcher isCheckedTextView() {
        return new BoundedMatcher<View, CheckedTextView>(CheckedTextView.class) {
            private String failedDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("checked text view: ");

                description.appendText(failedDescription);
            }

            @Override
            public boolean matchesSafely(final CheckedTextView view) {
                if (view.isChecked()) {
                    return true;
                }

                failedDescription = "not checked";
                return false;
            }
        };
    }

    /**
     * Returns a matcher that matches <code>CheckedTextView</code>s which are in checked state.
     */
    public static Matcher isNonCheckedTextView() {
        return new BoundedMatcher<View, CheckedTextView>(CheckedTextView.class) {
            private String failedDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("non checked text view: ");

                description.appendText(failedDescription);
            }

            @Override
            public boolean matchesSafely(final CheckedTextView view) {
                if (!view.isChecked()) {
                    return true;
                }

                failedDescription = "checked";
                return false;
            }
        };
    }

    /**
     * Returns a matcher that matches data entry in <code>SQLiteCursor</code> that has
     * the specified text in the specified column.
     */
    public static Matcher<Object> withCursorItemContent(final String columnName,
            final String expectedText) {
        return new BoundedMatcher<Object, SQLiteCursor>(SQLiteCursor.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("doesn't match " + expectedText);
            }

            @Override
            protected boolean matchesSafely(SQLiteCursor cursor) {
                return TextUtils.equals(expectedText,
                        cursor.getString(cursor.getColumnIndex(columnName)));
            }
        };
    }

    /**
     * Returns a matcher that matches Views which implement TintableBackgroundView.
     */
    public static Matcher<View> isTintableBackgroundView() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is TintableBackgroundView");
            }

            @Override
            public boolean matchesSafely(View view) {
                return TintableBackgroundView.class.isAssignableFrom(view.getClass());
            }
        };
    }

    /**
     * Returns a matcher that matches lists of float values that fall into the specified range.
     */
    public static Matcher<List<Float>> inRange(final float from, final float to) {
        return new TypeSafeMatcher<List<Float>>() {
            private String mFailedDescription;

            @Override
            public void describeTo(Description description) {
                description.appendText(mFailedDescription);
            }

            @Override
            protected boolean matchesSafely(List<Float> item) {
                int itemCount = item.size();

                for (int i = 0; i < itemCount; i++) {
                    float curr = item.get(i);

                    if ((curr < from) || (curr > to)) {
                        mFailedDescription = "Value #" + i + ":" + curr + " should be between " +
                                from + " and " + to;
                        return false;
                    }
                }

                return true;
            }
        };
    }

    /**
     * Returns a matcher that matches lists of float values that are in ascending order.
     */
    public static Matcher<List<Float>> inAscendingOrder() {
        return new TypeSafeMatcher<List<Float>>() {
            private String mFailedDescription;

            @Override
            public void describeTo(Description description) {
                description.appendText(mFailedDescription);
            }

            @Override
            protected boolean matchesSafely(List<Float> item) {
                int itemCount = item.size();

                if (itemCount >= 2) {
                    for (int i = 0; i < itemCount - 1; i++) {
                        float curr = item.get(i);
                        float next = item.get(i + 1);

                        if (curr > next) {
                            mFailedDescription = "Values should increase between #" + i +
                                    ":" + curr + " and #" + (i + 1) + ":" + next;
                            return false;
                        }
                    }
                }

                return true;
            }
        };
    }

    /**
     * Returns a matcher that matches lists of float values that are in descending order.
     */
    public static Matcher<List<Float>> inDescendingOrder() {
        return new TypeSafeMatcher<List<Float>>() {
            private String mFailedDescription;

            @Override
            public void describeTo(Description description) {
                description.appendText(mFailedDescription);
            }

            @Override
            protected boolean matchesSafely(List<Float> item) {
                int itemCount = item.size();

                if (itemCount >= 2) {
                    for (int i = 0; i < itemCount - 1; i++) {
                        float curr = item.get(i);
                        float next = item.get(i + 1);

                        if (curr < next) {
                            mFailedDescription = "Values should decrease between #" + i +
                                    ":" + curr + " and #" + (i + 1) + ":" + next;
                            return false;
                        }
                    }
                }

                return true;
            }
        };
    }


    /**
     * Returns a matcher that matches {@link View}s based on the given child type.
     *
     * @param childMatcher the type of the child to match on
     */
    public static Matcher<ViewGroup> hasChild(final Matcher<View> childMatcher) {
        return new TypeSafeMatcher<ViewGroup>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has child: ");
                childMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(ViewGroup view) {
                final int childCount = view.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    if (childMatcher.matches(view.getChildAt(i))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

}
