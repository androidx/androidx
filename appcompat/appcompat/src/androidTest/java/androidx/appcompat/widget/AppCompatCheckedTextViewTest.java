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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;
import androidx.appcompat.test.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.CheckedTextViewCompat;
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
        assertTrue(checkMark instanceof AnimatedStateListDrawableCompat
                || checkMark instanceof AnimatedStateListDrawable);
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
}
