/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;

import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;
import androidx.appcompat.test.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Provides tests specific to {@link AppCompatCheckBox} class.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatCheckBoxTest extends AppCompatBaseViewTest<AppCompatCheckBoxActivity,
        AppCompatCheckBox> {

    public AppCompatCheckBoxTest() {
        super(AppCompatCheckBoxActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        return true;
    }

    @Test
    public void testFontResources() {
        AppCompatCheckBox checkBox = mContainer.findViewById(R.id.checkbox_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, checkBox.getTypeface());
    }

    @Test
    public void testDefaultButton_isAnimated() {
        // Given an ACCB with the theme's button drawable
        final AppCompatCheckBox checkBox = mContainer.findViewById(R.id.checkbox_button_compat);
        final Drawable button = CompoundButtonCompat.getButtonDrawable(checkBox);

        // Then this drawable should be an animated-selector
        assertTrue(button instanceof AnimatedStateListDrawableCompat
                || button instanceof AnimatedStateListDrawable);
    }

    /* Max SDK as we use this test to verify the fallback behavior in situations where the ASLD
       backport should not be used (e.g. building with AAPT1). */
    @SdkSuppress(maxSdkVersion = 20)
    @Test
    public void testNullCompatButton() {
        // Given an ACCB which specifies a null app:buttonCompat
        final AppCompatCheckBox checkBox = mContainer.findViewById(
                R.id.checkbox_null_button_compat);
        final Drawable button = CompoundButtonCompat.getButtonDrawable(checkBox);
        boolean isAnimated = button instanceof AnimatedStateListDrawableCompat;

        // Then the drawable should be present but not animated
        assertNotNull(button);
        assertFalse(isAnimated);
    }
}
