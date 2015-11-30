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
package android.support.v4.content;

import android.content.res.ColorStateList;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.ThemedYellowActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.test.R;
import android.support.v4.testutils.TestUtils;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.TypedValue;

public class ContextCompatTest extends ActivityInstrumentationTestCase2<ThemedYellowActivity> {
    private static final String TAG = "ResourcesCompatTest";

    public ContextCompatTest() {
        super("android.support.v4.content", ThemedYellowActivity.class);
    }

    @UiThreadTest
    @SmallTest
    public void testGetColor() throws Throwable {
        Context context = getActivity();

        assertEquals("Unthemed color load",
                ContextCompat.getColor(context, R.color.text_color), 0xFFFF8090);

        if (Build.VERSION.SDK_INT >= 23) {
            // The following test is only expected to pass on v23+ devices. The result of
            // calling theme-aware getColor() in pre-v23 is undefined.
            assertEquals("Themed yellow color load",
                    ContextCompat.getColor(context, R.color.simple_themed_selector),
                    0xFFF0B000);
        }
    }

    @UiThreadTest
    @SmallTest
    public void testGetColorStateList() throws Throwable {
        Context context = getActivity();

        ColorStateList unthemedColorStateList =
                ContextCompat.getColorStateList(context, R.color.complex_unthemed_selector);
        assertEquals("Unthemed color state list load: default",
                unthemedColorStateList.getDefaultColor(), 0xFF70A0C0);
        assertEquals("Unthemed color state list load: focused",
                unthemedColorStateList.getColorForState(
                        new int[] {android.R.attr.state_focused}, 0), 0xFF70B0F0);
        assertEquals("Unthemed color state list load: pressed",
                unthemedColorStateList.getColorForState(
                        new int[] {android.R.attr.state_pressed}, 0), 0xFF6080B0);

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getColorStateList() in pre-v23 is undefined.
            ColorStateList themedYellowColorStateList =
                    ContextCompat.getColorStateList(context, R.color.complex_themed_selector);
            assertEquals("Themed yellow color state list load: default",
                    themedYellowColorStateList.getDefaultColor(), 0xFFF0B000);
            assertEquals("Themed yellow color state list load: focused",
                    themedYellowColorStateList.getColorForState(
                            new int[]{android.R.attr.state_focused}, 0), 0xFFF0A020);
            assertEquals("Themed yellow color state list load: pressed",
                    themedYellowColorStateList.getColorForState(
                            new int[]{android.R.attr.state_pressed}, 0), 0xFFE0A040);
        }
    }

    @UiThreadTest
    @SmallTest
    public void testGetDrawable() throws Throwable {
        Context context = getActivity();

        Drawable unthemedDrawable =
                ContextCompat.getDrawable(context, R.drawable.test_drawable_red);
        TestUtils.assertAllPixelsOfColor("Unthemed drawable load",
                unthemedDrawable, context.getResources().getColor(R.color.test_red));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following test is only expected to pass on v23+ devices. The result of
            // calling theme-aware getDrawable() in pre-v23 is undefined.
            Drawable themedYellowDrawable =
                    ContextCompat.getDrawable(context, R.drawable.themed_drawable);
            TestUtils.assertAllPixelsOfColor("Themed yellow drawable load",
                    themedYellowDrawable, 0xFFF0B000);
        }
    }
}
