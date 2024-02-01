/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.appcompat.widget;

import static android.os.Build.VERSION.SDK_INT;

import static androidx.appcompat.testutils.TestUtilsActions.setEnabled;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.CheckedTextViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Provides tests specific to {@link AppCompatCheckedTextView} class.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppCompatCheckedTextViewTest extends AppCompatBaseViewTest<
        AppCompatCheckedTextViewActivity, AppCompatCheckedTextView> {

    public AppCompatCheckedTextViewTest() {
        super(AppCompatCheckedTextViewActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        return true;
    }

    @Test
    public void testFontResources() {
        AppCompatCheckedTextView textView =
                mContainer.findViewById(R.id.checkedtextview_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testCheckMarkDefault_isNull() {
        // Given an ACCTV with the theme's check mark drawable
        final AppCompatCheckedTextView textView =
                mContainer.findViewById(R.id.checkedtextview_check_mark_default);
        final Drawable checkMark = CheckedTextViewCompat.getCheckMarkDrawable(textView);

        // Then this drawable should be null
        assertNull(checkMark);
    }

    @Test
    public void testCheckMarkBoth_isAnimated() {
        // Given an ACCTV which specifies both non-null android:checkMark and app:checkMarkCompat
        final AppCompatCheckedTextView checkedTextView =
                mContainer.findViewById(R.id.checkedtextview_check_mark_both);
        final Drawable checkMark = CheckedTextViewCompat.getCheckMarkDrawable(checkedTextView);

        // Then this drawable should be an animated-selector
        // i.e. compat version has precedence
        if (SDK_INT >= 21) {
            assertTrue(checkMark instanceof AnimatedStateListDrawableCompat
                    || checkMark instanceof AnimatedStateListDrawable);
        } else {
            assertTrue(checkMark instanceof AnimatedStateListDrawableCompat);
        }
    }

    /* Max SDK as we use this test to verify the fallback behavior in situations where the ASLD
       backport should not be used (e.g. building with AAPT1). */
    @SdkSuppress(maxSdkVersion = 20)
    @Test
    public void testCheckMarkPlatformOnly_isNotNull() {
        // Given an ACCTV which specifies a null app:checkMarkCompat and non-null android:checkMark
        final AppCompatCheckedTextView checkedTextView =
                mContainer.findViewById(R.id.checkedtextview_check_mark_platform);
        final Drawable checkMark = CheckedTextViewCompat.getCheckMarkDrawable(checkedTextView);

        // Then the drawable should be present
        assertNotNull(checkMark);
    }

    @Test
    public void testCheckMarkCompatOnly_isNotNull() {
        // Given an ACCTV which specifies a null android:checkMark and non-null app:checkMarkCompat
        final AppCompatCheckedTextView checkedTextView =
                mContainer.findViewById(R.id.checkedtextview_check_mark_compat);
        final Drawable checkMark = CheckedTextViewCompat.getCheckMarkDrawable(checkedTextView);

        // Then the drawable should be present
        assertNotNull(checkMark);
    }

    @UiThreadTest
    public void testSetCustomSelectionActionModeCallback() {
        final AppCompatCheckedTextView view = new AppCompatCheckedTextView(mActivity);
        final ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };

        // Default value is documented as null.
        assertNull(view.getCustomSelectionActionModeCallback());

        // Setter and getter should be symmetric.
        view.setCustomSelectionActionModeCallback(callback);
        assertEquals(callback, view.getCustomSelectionActionModeCallback());

        // Argument is nullable.
        view.setCustomSelectionActionModeCallback(null);
        assertNull(view.getCustomSelectionActionModeCallback());
    }

    @Test
    public void testCompoundDrawablesTint() {
        // Given an ACTV with a white drawableLeftCompat set and a #f0f drawableTint
        final AppCompatCheckedTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testCompoundDrawableRelativeTint() {
        // Given an ACTV with a white drawableStartCompat set and a #f0f drawableTint
        final AppCompatCheckedTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_relative_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = TextViewCompat.getCompoundDrawablesRelative(textView)[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testCompoundDrawablesTintList() {
        // Given an ACTV with a white drawableLeftCompat and a ColorStateList drawableTint set
        final AppCompatCheckedTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_list);
        final int defaultTint = ResourcesCompat.getColor(mResources, R.color.lilac_default, null);
        final int disabledTint = ResourcesCompat.getColor(mResources, R.color.lilac_disabled, null);

        // Then the initial drawable tint is applied
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                defaultTint,
                0,
                true);

        // When the view is disabled
        onView(withId(textView.getId())).perform(setEnabled(false));
        // Then the appropriate drawable tint is applied
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                disabledTint,
                0,
                true);
    }

    @Test
    public void testCompoundDrawablesTintMode() {
        // Given an ACTV with a red drawableLeft, a semi-transparent blue drawableTint
        // & drawableTintMode of src_over
        final AppCompatCheckedTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_mode);
        final int expected = ColorUtils.compositeColors(0x800000ff, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter returns the mode
        assertEquals(PorterDuff.Mode.SRC_OVER,
                TextViewCompat.getCompoundDrawableTintMode(textView));
    }

    @Test
    public void testSetCompoundDrawablesTintList() {
        // Given an ACTV with a compound drawable
        final AppCompatCheckedTextView textView = new AppCompatCheckedTextView(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // When a tint is set programmatically
        final int tint = 0xffa4c639;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(tintList, TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testSetCompoundDrawablesTintMode() {
        // Given an ACTV with a red compound drawable
        final AppCompatCheckedTextView textView = new AppCompatCheckedTextView(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.red_square), null, null, null);

        // When a semi-transparent blue tint is set programmatically with a mode of SRC_OVER
        final int tint = 0x800000ff;
        final PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);
        TextViewCompat.setCompoundDrawableTintMode(textView, mode);
        final int expected = ColorUtils.compositeColors(tint, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter should return the tint mode
        assertEquals(mode, TextViewCompat.getCompoundDrawableTintMode(textView));
    }


    @Test
    public void testCompoundDrawablesSetAfterTint() {
        // Given an ACTV with a magenta tint
        final AppCompatCheckedTextView textView = new AppCompatCheckedTextView(mActivity);
        final int tint = 0xffff00ff;
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(tint));

        // When a white compound drawable is set
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatCheckedTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
    }
}
