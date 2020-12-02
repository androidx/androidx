/*
 * Copyright 2018 The Android Open Source Project
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

import android.graphics.Typeface;

import androidx.appcompat.test.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Provides tests specific to {@link AppCompatToggleButton} class.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class AppCompatToggleButtonTest
        extends AppCompatBaseViewTest<AppCompatToggleButtonActivity, AppCompatToggleButton> {
    public AppCompatToggleButtonTest() {
        super(AppCompatToggleButtonActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        // Button has default background set on it
        return true;
    }

    @Test
    public void testFontResources() {
        AppCompatToggleButton toggleButton = mContainer.findViewById(
                R.id.togglebutton_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, toggleButton.getTypeface());
    }
}
