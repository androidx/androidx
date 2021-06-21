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

package androidx.core.content.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ColorStateListInflaterCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ColorStateListInflaterCompatTest {

    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(),
                R.style.ThemeOverlay_Core_ColorStateListInflaterCompat);
        mResources = mContext.getResources();
    }

    @Test
    public void testGetColorStateListWithThemedAttributes() throws Exception {
        final int colorForeground = getColorFromTheme(android.R.attr.colorForeground);
        @SuppressLint("ResourceType") final ColorStateList result =
                ColorStateListInflaterCompat.createFromXml(mResources,
                        mResources.getXml(R.color.color_state_list_themed_attrs),
                        mContext.getTheme());

        assertNotNull(result);

        // Now check the state colors

        // Disabled color should be colorForeground with 50% of its alpha
        final int expectedDisabled = Color.argb(128, Color.red(colorForeground),
                Color.green(colorForeground), Color.blue(colorForeground));
        assertEquals(expectedDisabled, getColorForDisabledState(result));

        // Default color should equal colorForeground
        assertEquals(colorForeground, result.getDefaultColor());
    }

    @Test
    public void testGetNestedColorStateList() throws Exception {
        final int textColorPrimary = getColorFromTheme(android.R.attr.textColorPrimary);
        @SuppressLint("ResourceType") final ColorStateList result =
                ColorStateListInflaterCompat.createFromXml(mResources,
                        mResources.getXml(R.color.color_state_list_secondary_text),
                        mContext.getTheme());
        assertNotNull(result);

        // Now check the state colors

        // Default color should be textColorPrimary with 54% (138) of its alpha
        final int expectedTextColorSecondary = Color.argb(138, Color.red(textColorPrimary),
                Color.green(textColorPrimary), Color.blue(textColorPrimary));
        assertEquals(expectedTextColorSecondary, result.getDefaultColor());
    }

    @ColorInt
    private int getColorFromTheme(@AttrRes int attrResId) {
        TypedArray a = TypedArrayUtils.obtainAttributes(mResources, mContext.getTheme(), null,
                new int[]{attrResId});
        final int colorForeground = a.getColor(0, 0);
        a.recycle();
        return colorForeground;
    }

    private int getColorForDisabledState(ColorStateList colorStateList) {
        return colorStateList.getColorForState(
                new int[]{-android.R.attr.state_enabled}, 0);
    }
}
