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

package androidx.core.widget;

import static android.support.v4.testutils.LayoutDirectionActions.setLayoutDirection;
import static android.support.v4.testutils.TextViewActions.setCompoundDrawablesRelative;
import static android.support.v4.testutils.TextViewActions.setCompoundDrawablesRelativeWithIntrinsicBounds;
import static android.support.v4.testutils.TextViewActions.setMaxLines;
import static android.support.v4.testutils.TextViewActions.setMinLines;
import static android.support.v4.testutils.TextViewActions.setText;
import static android.support.v4.testutils.TextViewActions.setTextAppearance;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.testutils.TestUtils;
import android.text.Layout;
import android.text.TextDirectionHeuristics;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.test.R;
import androidx.core.text.PrecomputedTextCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@LargeTest
public class TextViewCompatTest extends BaseInstrumentationTestCase<TextViewTestActivity> {
    private static final String TAG = "TextViewCompatTest";

    private TextView mTextView;

    private class TestDrawable extends ColorDrawable {
        private int mWidth;
        private int mHeight;

        TestDrawable(@ColorInt int color, int width, int height) {
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
    public void testCompoundDrawablesRelativeRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(View.LAYOUT_DIRECTION_RTL));

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
        if (mTextView.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
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
    public void testCompoundDrawablesRelativeWithIntrinsicBoundsRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(View.LAYOUT_DIRECTION_RTL));

        final Drawable drawableStart = new TestDrawable(0xFFFF0000, 30, 20);
        final Drawable drawableEnd = new TestDrawable(0xFF0000FF, 25, 45);
        final Drawable drawableBottom = new TestDrawable(0xFF00FF00, 15, 35);

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawableStart, null, drawableEnd, drawableBottom));

        // Check to see whether our text view is under RTL mode
        if (mTextView.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
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
    public void testCompoundDrawablesRelativeWithIntrinsicBoundsByIdRtl() throws Throwable {
        onView(withId(R.id.text_view)).perform(setLayoutDirection(View.LAYOUT_DIRECTION_RTL));

        onView(withId(R.id.text_view)).perform(setText(R.string.test_text_long));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.test_drawable_red, 0,
                R.drawable.test_drawable_green, R.drawable.test_drawable_blue));

        // Check to see whether our text view is under RTL mode
        if (mTextView.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
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
    public void testCompoundDrawablesRelativeGetterAndSetter() {
        final Drawable drawableStart = new TestDrawable(0xFFFF0000, 20, 20);
        final Drawable drawableTop = new TestDrawable(0xFFFFFF00, 20, 20);
        final Drawable drawableEnd = new TestDrawable(0xFF0000FF, 20, 20);
        final Drawable drawableBottom = new TestDrawable(0xFF00FF00, 20, 20);

        onView(withId(R.id.text_view)).perform(setLayoutDirection(View.LAYOUT_DIRECTION_RTL));
        onView(withId(R.id.text_view)).perform(setCompoundDrawablesRelative(drawableStart,
                drawableTop, drawableEnd, drawableBottom));

        // Check to see whether our text view is under RTL mode
        if (mTextView.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesRelative = TextViewCompat.getCompoundDrawablesRelative(mTextView);
        assertEquals(drawableStart, drawablesRelative[0]);
        assertEquals(drawableTop, drawablesRelative[1]);
        assertEquals(drawableEnd, drawablesRelative[2]);
        assertEquals(drawableBottom, drawablesRelative[3]);
    }

    @Test
    public void testSetCustomSelectionActionModeCallback_doesNotIgnoreTheGivenCallback() {
        // JB devices require the current thread to be prepared as a looper for this test.
        // The test causes the creation of an Editor object, which uses an UserDictionaryListener
        // that is handled on the main looper.
        Looper.prepare();

        final boolean[] callbackCalled = new boolean[4];
        TextViewCompat.setCustomSelectionActionModeCallback(mTextView, new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                callbackCalled[0] = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                callbackCalled[1] = true;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                callbackCalled[2] = true;
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                callbackCalled[3] = true;
            }
        });
        final Menu menu = new MenuBuilder(mTextView.getContext());
        final MenuItem item = menu.add("Option");
        mTextView.getCustomSelectionActionModeCallback().onCreateActionMode(null, menu);
        mTextView.getCustomSelectionActionModeCallback().onPrepareActionMode(null, menu);
        mTextView.getCustomSelectionActionModeCallback().onActionItemClicked(null, item);
        mTextView.getCustomSelectionActionModeCallback().onDestroyActionMode(null);
        for (boolean called : callbackCalled) {
            assertTrue(called);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26, maxSdkVersion =  27)
    @SuppressWarnings("deprecation")
    public void testSetCustomSelectionActionModeCallback_fixesBugInO() {
        // Create mock context and package manager for the text view.
        final PackageManager packageManagerMock = spy(mTextView.getContext().getPackageManager());
        final Context contextMock = spy(mTextView.getContext());
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        final TextView tvMock = spy(mTextView);
        // Set the new context on textViewMock by reflection, as TextView#getContext() is final.
        try {
            final Field contextField = View.class.getDeclaredField("mContext");
            contextField.setAccessible(true);
            contextField.set(tvMock, contextMock);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // We should be able to set mContext by reflection.
            assertTrue(false);
        }
        // Create fake activities able to handle the ACTION_PROCESS_TEXT intent.
        final ResolveInfo info1 = new ResolveInfo();
        info1.activityInfo = new ActivityInfo();
        info1.activityInfo.packageName = contextMock.getPackageName();
        info1.activityInfo.name = "Activity 1";
        info1.nonLocalizedLabel = "Option 3";
        final ResolveInfo info2 = new ResolveInfo();
        info2.activityInfo = new ActivityInfo();
        info2.activityInfo.packageName = contextMock.getPackageName();
        info2.activityInfo.name = "Activity 2";
        info2.nonLocalizedLabel = "Option 4";
        final ResolveInfo info3 = new ResolveInfo();
        info3.activityInfo = new ActivityInfo();
        info3.activityInfo.packageName = contextMock.getPackageName();
        info3.activityInfo.name = "Activity 3";
        info3.nonLocalizedLabel = "Option 5";
        final List<ResolveInfo> infos = Arrays.asList(info1, info2, info3);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Intent intent = invocation.getArgument(0);
                if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
                    return infos;
                }
                return invocation.callRealMethod();
            }
        }).when(packageManagerMock).queryIntentActivities((Intent) any(), anyInt());
        // Set a no op callback on the mocked text view, which should fix the SDK26 bug.
        TextViewCompat.setCustomSelectionActionModeCallback(tvMock, new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
        // Create a fake menu with two non process text items and two process text items.
        final Menu menu = new MenuBuilder(tvMock.getContext());
        menu.add(Menu.NONE, Menu.NONE, 1, "Option 1");
        menu.add(Menu.NONE, Menu.NONE, 2, "Option 2");
        menu.add(Menu.NONE, Menu.NONE, 100, "Option 3")
                .setIntent(new Intent(Intent.ACTION_PROCESS_TEXT));
        menu.add(Menu.NONE, Menu.NONE, 101, "Option 5")
                .setIntent(new Intent(Intent.ACTION_PROCESS_TEXT));
        // Run the callback and verify that the menu was updated. Its size should have increased
        // with 1, as now there are 3 process text options instead of 2 to be displayed.
        tvMock.getCustomSelectionActionModeCallback().onPrepareActionMode(null, menu);
        assertEquals(5, menu.size());
        for (int i = 0; i < menu.size(); ++i) {
            assertEquals("Option " + (i + 1), menu.getItem(i).getTitle());
        }
    }

    @UiThreadTest
    @Test
    public void testSetFirstBaselineToTopHeight() {
        mTextView.setText("This is some random text");
        final int padding = 100;
        mTextView.setPadding(padding, padding, padding, padding);

        final Paint.FontMetricsInt fontMetrics = mTextView.getPaint().getFontMetricsInt();
        final int fontMetricsTop = Math.max(
                Math.abs(fontMetrics.top), Math.abs(fontMetrics.ascent));

        int firstBaselineToTopHeight = fontMetricsTop + 10;
        TextViewCompat.setFirstBaselineToTopHeight(mTextView, firstBaselineToTopHeight);
        assertEquals(firstBaselineToTopHeight,
                TextViewCompat.getFirstBaselineToTopHeight(mTextView));
        assertNotEquals(padding, mTextView.getPaddingTop());

        firstBaselineToTopHeight = fontMetricsTop + 40;
        TextViewCompat.setFirstBaselineToTopHeight(mTextView, firstBaselineToTopHeight);
        assertEquals(firstBaselineToTopHeight,
                TextViewCompat.getFirstBaselineToTopHeight(mTextView));

        mTextView.setPadding(padding, padding, padding, padding);
        assertEquals(padding, mTextView.getPaddingTop());
    }

    @UiThreadTest
    @Test
    public void testSetFirstBaselineToTopHeight_tooSmall() {
        mTextView.setText("This is some random text");
        final int padding = 100;
        mTextView.setPadding(padding, padding, padding, padding);

        final Paint.FontMetricsInt fontMetrics = mTextView.getPaint().getFontMetricsInt();
        final int fontMetricsTop = Math.min(
                Math.abs(fontMetrics.top), Math.abs(fontMetrics.ascent));

        int firstBaselineToTopHeight = fontMetricsTop - 1;
        TextViewCompat.setFirstBaselineToTopHeight(mTextView, firstBaselineToTopHeight);
        assertNotEquals(firstBaselineToTopHeight,
                TextViewCompat.getFirstBaselineToTopHeight(mTextView));
        assertEquals(padding, mTextView.getPaddingTop());
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException.class)
    public void testSetFirstBaselineToTopHeight_negative() {
        TextViewCompat.setFirstBaselineToTopHeight(mTextView, -1);
    }

    @UiThreadTest
    @Test
    public void testSetLastBaselineToBottomHeight() {
        mTextView.setText("This is some random text");
        final int padding = 100;
        mTextView.setPadding(padding, padding, padding, padding);

        final Paint.FontMetricsInt fontMetrics = mTextView.getPaint().getFontMetricsInt();
        final int fontMetricsBottom = Math.max(
                Math.abs(fontMetrics.bottom), Math.abs(fontMetrics.descent));

        int lastBaselineToBottomHeight = fontMetricsBottom + 20;
        TextViewCompat.setLastBaselineToBottomHeight(mTextView, lastBaselineToBottomHeight);
        assertEquals(lastBaselineToBottomHeight,
                TextViewCompat.getLastBaselineToBottomHeight(mTextView));
        assertNotEquals(padding, mTextView.getPaddingBottom());

        lastBaselineToBottomHeight = fontMetricsBottom + 30;
        TextViewCompat.setLastBaselineToBottomHeight(mTextView, lastBaselineToBottomHeight);
        assertEquals(lastBaselineToBottomHeight,
                TextViewCompat.getLastBaselineToBottomHeight(mTextView));

        mTextView.setPadding(padding, padding, padding, padding);
        assertEquals(padding, mTextView.getPaddingBottom());
    }

    @UiThreadTest
    @Test
    public void testSetLastBaselineToBottomHeight_tooSmall() {
        mTextView.setText("This is some random text");
        final int padding = 100;
        mTextView.setPadding(padding, padding, padding, padding);

        final Paint.FontMetricsInt fontMetrics = mTextView.getPaint().getFontMetricsInt();
        final int fontMetricsBottom = Math.min(
                Math.abs(fontMetrics.bottom), Math.abs(fontMetrics.descent));

        int lastBaselineToBottomHeight = fontMetricsBottom - 1;
        TextViewCompat.setLastBaselineToBottomHeight(mTextView, lastBaselineToBottomHeight);
        assertNotEquals(lastBaselineToBottomHeight,
                TextViewCompat.getLastBaselineToBottomHeight(mTextView));
        assertEquals(padding, mTextView.getPaddingBottom());
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException.class)
    public void testSetLastBaselineToBottomHeight_negative() {
        TextViewCompat.setLastBaselineToBottomHeight(mTextView, -1);
    }

    @UiThreadTest
    @Test
    public void testSetLineHeight() {
        mTextView.setText("This is some random text");
        final float lineSpacingExtra = 50;
        final float lineSpacingMultiplier = 0.2f;
        mTextView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);

        TextViewCompat.setLineHeight(mTextView, 100);
        assertEquals(100, mTextView.getLineHeight());
        assertNotEquals(lineSpacingExtra, mTextView.getLineSpacingExtra(), 0);
        assertNotEquals(lineSpacingMultiplier, mTextView.getLineSpacingMultiplier(), 0);

        TextViewCompat.setLineHeight(mTextView, 200);
        assertEquals(200, mTextView.getLineHeight());

        mTextView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        assertEquals(lineSpacingExtra, mTextView.getLineSpacingExtra(), 0);
        assertEquals(lineSpacingMultiplier, mTextView.getLineSpacingMultiplier(), 0);
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException.class)
    public void testSetLineHeight_negative() {
        TextViewCompat.setLineHeight(mTextView, -1);
    }

    @UiThreadTest
    @Test
    public void testGetSetTextMetricsParams_API23() {
        PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotNull(params);

        // set different params to text view for checking setTextMetricsParams overwrite the params.
        mTextView.setBreakStrategy(
                params.getBreakStrategy() == Layout.BREAK_STRATEGY_SIMPLE
                        ? Layout.BREAK_STRATEGY_HIGH_QUALITY : Layout.BREAK_STRATEGY_SIMPLE
        );
        PrecomputedTextCompat.Params params2 = TextViewCompat.getTextMetricsParams(mTextView);
        TextViewCompat.setTextMetricsParams(mTextView, params);
        PrecomputedTextCompat.Params params3 = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotEquals(params.getBreakStrategy(), params2.getBreakStrategy());
        assertEquals(params.getBreakStrategy(), params3.getBreakStrategy());

        mTextView.setHyphenationFrequency(
                params.getHyphenationFrequency() == Layout.HYPHENATION_FREQUENCY_NONE
                        ? Layout.HYPHENATION_FREQUENCY_FULL : Layout.HYPHENATION_FREQUENCY_NONE
        );
        params2 = TextViewCompat.getTextMetricsParams(mTextView);
        TextViewCompat.setTextMetricsParams(mTextView, params);
        params3 = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotEquals(params.getHyphenationFrequency(), params2.getHyphenationFrequency());
        assertEquals(params.getHyphenationFrequency(), params3.getHyphenationFrequency());
    }

    @UiThreadTest
    @Test
    public void testSetGetTextMetricsParams_API18() {
        PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotNull(params);

        // set different params to text view for checking setTextMetricsParams overwrite the params.
        mTextView.setTextDirection(
                params.getTextDirection() == TextDirectionHeuristics.LTR
                        ? View.TEXT_DIRECTION_ANY_RTL : View.TEXT_DIRECTION_LTR
        );
        PrecomputedTextCompat.Params params2 = TextViewCompat.getTextMetricsParams(mTextView);
        TextViewCompat.setTextMetricsParams(mTextView, params);
        PrecomputedTextCompat.Params params3 = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotEquals(params.getTextDirection(), params2.getTextDirection());
        assertEquals(params.getTextDirection(), params3.getTextDirection());
    }

    @UiThreadTest
    @Test
    public void testSetGetTextMetricsParams() {
        PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotNull(params);

        // set different params to text view for checking setTextMetricsParams overwrite the params.
        mTextView.setTextScaleX(params.getTextPaint().getTextScaleX() * 2.0f + 1.0f);
        PrecomputedTextCompat.Params params2 = TextViewCompat.getTextMetricsParams(mTextView);
        TextViewCompat.setTextMetricsParams(mTextView, params);
        PrecomputedTextCompat.Params params3 = TextViewCompat.getTextMetricsParams(mTextView);
        assertNotEquals(params.getTextPaint().getTextScaleX(),
                params2.getTextPaint().getTextScaleX());
        assertEquals(params.getTextPaint().getTextScaleX(),
                params3.getTextPaint().getTextScaleX(), 0.0f);
    }

    @UiThreadTest
    @Test
    public void setPrecomputedText() {
        PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(mTextView);
        PrecomputedTextCompat precomptued = PrecomputedTextCompat.create("Hello, world", params);
        assertNotNull(precomptued);

        TextViewCompat.setPrecomputedText(mTextView, precomptued);
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException.class)
    public void setPrecomputedText_incompatible() {
        PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(mTextView);
        PrecomputedTextCompat precomptued = PrecomputedTextCompat.create("Hello, world", params);
        assertNotNull(precomptued);
        // set different params to text view for checking IlleaglArgumentException.
        mTextView.setTextScaleX(params.getTextPaint().getTextScaleX() * 2.0f + 1.0f);
        TextViewCompat.setPrecomputedText(mTextView, precomptued);
    }
}
