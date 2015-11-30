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
package android.support.v4.content.res;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.test.R;
import android.support.v4.testutils.TestUtils;
import android.support.v4.widget.TestActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.TypedValue;

public class ResourcesCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {
    private static final String TAG = "ResourcesCompatTest";

    public ResourcesCompatTest() {
        super("android.support.v4.content.res", TestActivity.class);
    }

    @UiThreadTest
    @SmallTest
    public void testGetColor() throws Throwable {
        Resources res = getActivity().getResources();
        assertEquals("Unthemed color load",
                ResourcesCompat.getColor(res, R.color.text_color, null),
                0xFFFF8090);

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getColor() in pre-v23 is undefined.
            Resources.Theme yellowTheme = res.newTheme();
            yellowTheme.applyStyle(R.style.YellowTheme, true);
            assertEquals("Themed yellow color load",
                    ResourcesCompat.getColor(res, R.color.simple_themed_selector, yellowTheme),
                    0xFFF0B000);

            Resources.Theme lilacTheme = res.newTheme();
            lilacTheme.applyStyle(R.style.LilacTheme, true);
            assertEquals("Themed lilac color load",
                    ResourcesCompat.getColor(res, R.color.simple_themed_selector, lilacTheme),
                    0xFFF080F0);
        }
    }

    @UiThreadTest
    @SmallTest
    public void testGetColorStateList() throws Throwable {
        Resources res = getActivity().getResources();

        ColorStateList unthemedColorStateList =
                ResourcesCompat.getColorStateList(res, R.color.complex_unthemed_selector, null);
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
            Resources.Theme yellowTheme = res.newTheme();
            yellowTheme.applyStyle(R.style.YellowTheme, true);
            ColorStateList themedYellowColorStateList =
                    ResourcesCompat.getColorStateList(res, R.color.complex_themed_selector,
                            yellowTheme);
            assertEquals("Themed yellow color state list load: default",
                    themedYellowColorStateList.getDefaultColor(), 0xFFF0B000);
            assertEquals("Themed yellow color state list load: focused",
                    themedYellowColorStateList.getColorForState(
                            new int[] {android.R.attr.state_focused}, 0), 0xFFF0A020);
            assertEquals("Themed yellow color state list load: pressed",
                    themedYellowColorStateList.getColorForState(
                            new int[] {android.R.attr.state_pressed}, 0), 0xFFE0A040);

            Resources.Theme lilacTheme = res.newTheme();
            lilacTheme.applyStyle(R.style.LilacTheme, true);
            ColorStateList themedLilacColorStateList =
                    ResourcesCompat.getColorStateList(res, R.color.complex_themed_selector,
                            lilacTheme);
            assertEquals("Themed lilac color state list load: default",
                    themedLilacColorStateList.getDefaultColor(), 0xFFF080F0);
            assertEquals("Themed lilac color state list load: focused",
                    themedLilacColorStateList.getColorForState(
                            new int[] {android.R.attr.state_focused}, 0), 0xFFF070D0);
            assertEquals("Themed lilac color state list load: pressed",
                    themedLilacColorStateList.getColorForState(
                            new int[] {android.R.attr.state_pressed}, 0), 0xFFE070A0);
        }
    }

    @UiThreadTest
    @SmallTest
    public void testGetDrawable() throws Throwable {
        Resources res = getActivity().getResources();

        Drawable unthemedDrawable =
                ResourcesCompat.getDrawable(res, R.drawable.test_drawable_red, null);
        TestUtils.assertAllPixelsOfColor("Unthemed drawable load",
                unthemedDrawable, res.getColor(R.color.test_red));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getDrawable() in pre-v23 is undefined.
            Resources.Theme yellowTheme = res.newTheme();
            yellowTheme.applyStyle(R.style.YellowTheme, true);
            Drawable themedYellowDrawable =
                    ResourcesCompat.getDrawable(res, R.drawable.themed_drawable, yellowTheme);
            TestUtils.assertAllPixelsOfColor("Themed yellow drawable load",
                    themedYellowDrawable, 0xFFF0B000);

            Resources.Theme lilacTheme = res.newTheme();
            lilacTheme.applyStyle(R.style.LilacTheme, true);
            Drawable themedLilacDrawable =
                    ResourcesCompat.getDrawable(res, R.drawable.themed_drawable, lilacTheme);
            TestUtils.assertAllPixelsOfColor("Themed lilac drawable load",
                    themedLilacDrawable, 0xFFF080F0);
        }
    }
}
