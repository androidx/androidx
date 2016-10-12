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


package android.support.v4.widget;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.compat.test.R;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.testutils.TestUtils;
import android.support.v4.view.ViewCompat;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.v4.testutils.LayoutDirectionActions.setLayoutDirection;
import static android.support.v4.testutils.TextViewActions.*;
import static org.junit.Assert.*;

public class TextViewCompatTest extends BaseInstrumentationTestCase<TextViewTestActivity> {
    private static final String TAG = "TextViewCompatTest";

    private TextView mTextView;

    private class TestDrawable extends ColorDrawable {
        private int mWidth;
        private int mHeight;

        public TestDrawable(@ColorInt int color, int width, int height) {
            super(color);
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }
    }

    public TextViewCompatTest() {
        super(TextViewTestActivity.class);
    }

    @Before
    public void setUp() {
        mTextView = (TextView) mActivityTestRule.getActivity().findViewById(R.id.text_view);
    }

    @Test
    @SmallTest
    public void testMaxLines() throws Throwable {
        final int maxLinesCount = 4;
        onView(withId(R.id.text_view)).perform(setMaxLines(maxLinesCount));

        assertEquals("Empty view: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_short));
        assertEquals("Short text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_medium));
        assertEquals("Medium text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        assertEquals("Long text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);
    }

    @Test
    @SmallTest
    public void testMinLines() throws Throwable {
        final int minLinesCount = 3;
        onView(withId(R.id.text_view)).perform(setMinLines(minLinesCount));

        assertEquals("Empty view: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_short));
        assertEquals("Short text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_medium));
        assertEquals("Medium text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        assertEquals("Long text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);
    }

    @Test
    @SmallTest
    public void testStyle() throws Throwable {
        onView(withId(R.id.text_view)).perform(setTextAppearance(R.style.TextMediumStyle));

        final Resources res = mActivityTestRule.getActivity().getResources();
        assertTrue("Styled text view: style",
                mTextView.getTypeface().isItalic() || (mTextView.getPaint().getTextSkewX() < 0));
        assertEquals("Styled text view: color", mTextView.getTextColors().getDefaultColor(),
                res.getColor(R.color.text_color));
        assertEquals("Styled text view: size", mTextView.getTextSize(),
                (float) res.getDimensionPixelSize(R.dimen.text_medium_size), 1.0f);
    }

    @Test
    @SmallTest
    public void testCompoundDrawablesRelative() throws Throwable {
        final Drawable drawableStart = new ColorDrawable(0xFFFF0000);
        drawableStart.setBounds(0, 0, 20, 20);
        final Drawable drawableTop = new ColorDrawable(0xFF00FF00);
        drawableTop.setBounds(0, 0, 30, 25);
        final Drawable drawableEnd = new ColorDrawable(0xFF0000FF);
        drawableEnd.setBounds(0, 0, 25, 20);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_medium));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelative(drawableStart,
                drawableTop, drawableEnd, null));

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();

        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableStart);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(), 20);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(), 20);

        assertEquals("Compound drawable: top", drawablesAbsolute[1], drawableTop);
        assertEquals("Compound drawable: top width",
                drawablesAbsolute[1].getBounds().width(), 30);
        assertEquals("Compound drawable: top height",
                drawablesAbsolute[1].getBounds().height(), 25);

        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableEnd);
        assertEquals("Compound drawable: right width",
                drawablesAbsolute[2].getBounds().width(), 25);
        assertEquals("Compound drawable: right height",
                drawablesAbsolute[2].getBounds().height(), 20);

        assertNull("Compound drawable: bottom", drawablesAbsolute[3]);
    }

    @Test
    @SmallTest
    public void testCompoundDrawablesRelativeRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        final Drawable drawableStart = new ColorDrawable(0xFFFF0000);
        drawableStart.setBounds(0, 0, 20, 20);
        final Drawable drawableTop = new ColorDrawable(0xFF00FF00);
        drawableTop.setBounds(0, 0, 30, 25);
        final Drawable drawableEnd = new ColorDrawable(0xFF0000FF);
        drawableEnd.setBounds(0, 0, 25, 20);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_medium));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelative(drawableStart,
                drawableTop, drawableEnd, null));

        // Check to see whether our text view is under RTL mode
        if (ViewCompat.getLayoutDirection(mTextView) != ViewCompat.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();

        // End drawable should be returned as left
        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableEnd);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(), 25);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(), 20);

        assertEquals("Compound drawable: top", drawablesAbsolute[1], drawableTop);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[1].getBounds().width(), 30);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[1].getBounds().height(), 25);

        // Start drawable should be returned as right
        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableStart);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[2].getBounds().width(), 20);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[2].getBounds().height(), 20);

        assertNull("Compound drawable: bottom", drawablesAbsolute[3]);
    }

    @Test
    @SmallTest
    public void testCompoundDrawablesRelativeWithIntrinsicBounds() throws Throwable {
        final Drawable drawableStart = new TestDrawable(0xFFFF0000, 30, 20);
        final Drawable drawableEnd = new TestDrawable(0xFF0000FF, 25, 45);
        final Drawable drawableBottom = new TestDrawable(0xFF00FF00, 15, 35);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawableStart, null, drawableEnd, drawableBottom));

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();

        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableStart);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(), 30);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(), 20);

        assertNull("Compound drawable: top", drawablesAbsolute[1]);

        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableEnd);
        assertEquals("Compound drawable: right width",
                drawablesAbsolute[2].getBounds().width(), 25);
        assertEquals("Compound drawable: right height",
                drawablesAbsolute[2].getBounds().height(), 45);

        assertEquals("Compound drawable: bottom", drawablesAbsolute[3], drawableBottom);
        assertEquals("Compound drawable: bottom width",
                drawablesAbsolute[3].getBounds().width(), 15);
        assertEquals("Compound drawable: bottom height",
                drawablesAbsolute[3].getBounds().height(), 35);
    }

    @Test
    @SmallTest
    public void testCompoundDrawablesRelativeWithIntrinsicBoundsRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        final Drawable drawableStart = new TestDrawable(0xFFFF0000, 30, 20);
        final Drawable drawableEnd = new TestDrawable(0xFF0000FF, 25, 45);
        final Drawable drawableBottom = new TestDrawable(0xFF00FF00, 15, 35);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawableStart, null, drawableEnd, drawableBottom));

        // Check to see whether our text view is under RTL mode
        if (ViewCompat.getLayoutDirection(mTextView) != ViewCompat.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();

        // End drawable should be returned as left
        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableEnd);
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(), 25);
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(), 45);

        assertNull("Compound drawable: top", drawablesAbsolute[1]);

        // Start drawable should be returned as right
        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableStart);
        assertEquals("Compound drawable: right width",
                drawablesAbsolute[2].getBounds().width(), 30);
        assertEquals("Compound drawable: right height",
                drawablesAbsolute[2].getBounds().height(), 20);

        assertEquals("Compound drawable: bottom", drawablesAbsolute[3], drawableBottom);
        assertEquals("Compound drawable: bottom width",
                drawablesAbsolute[3].getBounds().width(), 15);
        assertEquals("Compound drawable: bottom height",
                drawablesAbsolute[3].getBounds().height(), 35);
    }

    @Test
    @MediumTest
    public void testCompoundDrawablesRelativeWithIntrinsicBoundsById() throws Throwable {
        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.test_drawable_red, 0,
                R.drawable.test_drawable_green, R.drawable.test_drawable_blue));

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();
        final Resources res = mActivityTestRule.getActivity().getResources();

        // The entire left drawable should be the specific red color
        TestUtils.assertAllPixelsOfColor("Compound drawable: left color",
                drawablesAbsolute[0], res.getColor(R.color.test_red));
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_small_size));
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_medium_size));

        assertNull("Compound drawable: top", drawablesAbsolute[1]);

        // The entire right drawable should be the specific green color
        TestUtils.assertAllPixelsOfColor("Compound drawable: right color",
                drawablesAbsolute[2], res.getColor(R.color.test_green));
        assertEquals("Compound drawable: right width",
                drawablesAbsolute[2].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_medium_size));
        assertEquals("Compound drawable: right height",
                drawablesAbsolute[2].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_large_size));

        // The entire bottom drawable should be the specific blue color
        TestUtils.assertAllPixelsOfColor("Compound drawable: bottom color",
                drawablesAbsolute[3], res.getColor(R.color.test_blue));
        assertEquals("Compound drawable: bottom width",
                drawablesAbsolute[3].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_large_size));
        assertEquals("Compound drawable: bottom height",
                drawablesAbsolute[3].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_small_size));
    }

    @Test
    @MediumTest
    public void testCompoundDrawablesRelativeWithIntrinsicBoundsByIdRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.test_drawable_red, 0,
                R.drawable.test_drawable_green, R.drawable.test_drawable_blue));

        // Check to see whether our text view is under RTL mode
        if (ViewCompat.getLayoutDirection(mTextView) != ViewCompat.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();
        final Resources res = mActivityTestRule.getActivity().getResources();

        // The entire left / end drawable should be the specific green color
        TestUtils.assertAllPixelsOfColor("Compound drawable: left color",
                drawablesAbsolute[0], res.getColor(R.color.test_green));
        assertEquals("Compound drawable: left width",
                drawablesAbsolute[0].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_medium_size));
        assertEquals("Compound drawable: left height",
                drawablesAbsolute[0].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_large_size));

        assertNull("Compound drawable: top", drawablesAbsolute[1]);

        // The entire right drawable should be the specific red color
        TestUtils.assertAllPixelsOfColor("Compound drawable: right color",
                drawablesAbsolute[2], res.getColor(R.color.test_red));
        assertEquals("Compound drawable: right width",
                drawablesAbsolute[2].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_small_size));
        assertEquals("Compound drawable: right height",
                drawablesAbsolute[2].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_medium_size));

        // The entire bottom drawable should be the specific blue color
        TestUtils.assertAllPixelsOfColor("Compound drawable: bottom color",
                drawablesAbsolute[3], res.getColor(R.color.test_blue));
        assertEquals("Compound drawable: bottom width",
                drawablesAbsolute[3].getBounds().width(),
                res.getDimensionPixelSize(R.dimen.drawable_large_size));
        assertEquals("Compound drawable: bottom height",
                drawablesAbsolute[3].getBounds().height(),
                res.getDimensionPixelSize(R.dimen.drawable_small_size));
    }

    @Test
    @SmallTest
    public void testCompoundDrawablesRelativeGetterAndSetter() {
        final Drawable drawableStart = new TestDrawable(0xFFFF0000, 20, 20);
        final Drawable drawableTop = new TestDrawable(0xFFFFFF00, 20, 20);
        final Drawable drawableEnd = new TestDrawable(0xFF0000FF, 20, 20);
        final Drawable drawableBottom = new TestDrawable(0xFF00FF00, 20, 20);

        onView(withId(R.id.text_view)).perform(setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelative(drawableStart,
                drawableTop, drawableEnd, drawableBottom));

        // Check to see whether our text view is under RTL mode
        if (ViewCompat.getLayoutDirection(mTextView) != ViewCompat.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesRelative = TextViewCompat.getCompoundDrawablesRelative(mTextView);
        assertEquals(drawableStart, drawablesRelative[0]);
        assertEquals(drawableTop, drawablesRelative[1]);
        assertEquals(drawableEnd, drawablesRelative[2]);
        assertEquals(drawableBottom, drawablesRelative[3]);
    }
}
