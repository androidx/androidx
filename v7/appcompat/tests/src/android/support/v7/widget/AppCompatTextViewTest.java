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
package android.support.v7.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.AppCompatTextViewActions;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Button;

import org.junit.Test;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to <code>AppCompatTextView</code> class.
 */
@SmallTest
public class AppCompatTextViewTest
        extends AppCompatBaseViewTest<AppCompatTextViewActivity, AppCompatTextView> {
    public AppCompatTextViewTest() {
        super(AppCompatTextViewActivity.class);
    }

    @Test
    public void testAllCaps() throws Throwable {
        final String text1 = mResources.getString(R.string.sample_text1);
        final String text2 = mResources.getString(R.string.sample_text2);

        final AppCompatTextView textView1 =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_caps1);
        final AppCompatTextView textView2 =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_caps2);

        // Note that TextView.getText() returns the original text. We are interested in
        // the transformed text that is set on the Layout object used to draw the final
        // (transformed) content.
        assertEquals("Text view starts in all caps on", text1.toUpperCase(),
                textView1.getLayout().getText());
        assertEquals("Text view starts in all caps off", text2,
                textView2.getLayout().getText());

        // Toggle all-caps mode on the two text views. Note that as with the core TextView,
        // setting a style with textAllCaps=false on a AppCompatTextView with all-caps on
        // will have no effect.
        onView(withId(R.id.text_view_caps1)).perform(
                AppCompatTextViewActions.setTextAppearance(R.style.TextStyleAllCapsOff));
        onView(withId(R.id.text_view_caps2)).perform(
                AppCompatTextViewActions.setTextAppearance(R.style.TextStyleAllCapsOn));

        assertEquals("Text view is still in all caps on", text1.toUpperCase(),
                textView1.getLayout().getText());
        assertEquals("Text view is in all caps on", text2.toUpperCase(),
                textView2.getLayout().getText());
    }

    @Test
    public void testAppCompatAllCapsFalseOnButton() throws Throwable {
        final String text = mResources.getString(R.string.sample_text2);
        final Button button = (Button) mContainer.findViewById(R.id.button_app_allcaps_false);

        assertEquals("Button is not in all caps", text, button.getLayout().getText());
    }
}
