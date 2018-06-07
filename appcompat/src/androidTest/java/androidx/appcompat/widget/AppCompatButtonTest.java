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
package androidx.appcompat.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static androidx.appcompat.testutils.TestUtilsActions.setTextAppearance;

import static org.junit.Assert.assertEquals;

import android.graphics.Typeface;
import android.support.test.filters.SmallTest;

import androidx.appcompat.test.R;
import androidx.core.content.res.ResourcesCompat;

import org.junit.Test;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatButton} class.
 */
@SmallTest
public class AppCompatButtonTest
        extends AppCompatBaseViewTest<AppCompatButtonActivity, AppCompatButton> {
    public AppCompatButtonTest() {
        super(AppCompatButtonActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        // Button has default background set on it
        return true;
    }

    @Test
    public void testAllCaps() {
        final String text1 = mResources.getString(R.string.sample_text1);
        final String text2 = mResources.getString(R.string.sample_text2);

        final AppCompatButton button1 =
                (AppCompatButton) mContainer.findViewById(R.id.button_caps1);
        final AppCompatButton button2 =
                (AppCompatButton) mContainer.findViewById(R.id.button_caps2);

        // Note that Button.getText() returns the original text. We are interested in
        // the transformed text that is set on the Layout object used to draw the final
        // (transformed) content.
        assertEquals("Button starts in all caps on", text1.toUpperCase(),
                button1.getLayout().getText().toString());
        assertEquals("Button starts in all caps off", text2,
                button2.getLayout().getText().toString());

        // Toggle all-caps mode on the two buttons
        onView(withId(R.id.button_caps1)).perform(
                setTextAppearance(R.style.TextStyleAllCapsOff));
        assertEquals("Button is now in all caps off", text1,
                button1.getLayout().getText().toString());

        onView(withId(R.id.button_caps2)).perform(
                setTextAppearance(R.style.TextStyleAllCapsOn));
        assertEquals("Button is now in all caps on", text2.toUpperCase(),
                button2.getLayout().getText().toString());
    }

    @Test
    public void testAppCompatAllCapsFalseOnButton() {
        final String text = mResources.getString(R.string.sample_text2);
        final AppCompatButton button =
                (AppCompatButton) mContainer.findViewById(R.id.button_app_allcaps_false);

        assertEquals("Button is not in all caps", text, button.getLayout().getText());
    }

    @Test
    public void testBackgroundTintListOnColoredButton() {
        testUntintedBackgroundTintingViewCompatAcrossStateChange(R.id.button_colored_untinted);
    }

    @Test
    public void testBackgroundTintListOnButton() {
        testUntintedBackgroundTintingViewCompatAcrossStateChange(R.id.button_untinted);
    }

    @Test
    public void testFontResources() {
        AppCompatButton button = mContainer.findViewById(R.id.button_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, button.getTypeface());
    }
}
