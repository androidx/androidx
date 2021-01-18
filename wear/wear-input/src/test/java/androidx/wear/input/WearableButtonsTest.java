/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.RotateDrawable;
import android.provider.Settings;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.wear.input.testing.TestWearableButtonsProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowDrawable;

import java.util.HashMap;
import java.util.Map;

/** Unit tests for {@link WearableButtons}. */
@RunWith(WearInputTestRunner.class)
@DoNotInstrument
public class WearableButtonsTest {
    private final Point mScreenSize = new Point(480, 480);

    @Test
    public void testExactPoints_round() {
        // Positions were calculated using the following formula:
        // ScreenCoordinate.x = r*(1+cos(a))
        // ScreenCoordinate.y = r*(1-sin(a))
        // Angle = pi/8 * N, where N was the cardinal position
        // r = 240 because the screen is 480x480
        // Wolfram Alpha was helpful:  x=240*(1+cos(a)), y=240*(1-sin(a)), a=pi/8.0

        assertEquals(
                WearableButtons.LOC_EAST,
                WearableButtons.getLocationZone(true, mScreenSize, 480, 240));
        assertEquals(
                WearableButtons.LOC_ENE,
                WearableButtons.getLocationZone(true, mScreenSize, 461.731f, 148.156f));
        assertEquals(
                WearableButtons.LOC_NE,
                WearableButtons.getLocationZone(true, mScreenSize, 409.706f, 70.294f));
        assertEquals(
                WearableButtons.LOC_NNE,
                WearableButtons.getLocationZone(true, mScreenSize, 331.844f, 18.269f));
        assertEquals(
                WearableButtons.LOC_NORTH,
                WearableButtons.getLocationZone(true, mScreenSize, 240, 0));
        assertEquals(
                WearableButtons.LOC_NNW,
                WearableButtons.getLocationZone(true, mScreenSize, 148.156f, 18.269f));
        assertEquals(
                WearableButtons.LOC_NW,
                WearableButtons.getLocationZone(true, mScreenSize, 70.294f, 70.294f));
        assertEquals(
                WearableButtons.LOC_WNW,
                WearableButtons.getLocationZone(true, mScreenSize, 18.269f, 148.156f));
        assertEquals(
                WearableButtons.LOC_WEST,
                WearableButtons.getLocationZone(true, mScreenSize, 0, 240));
        assertEquals(
                WearableButtons.LOC_WSW,
                WearableButtons.getLocationZone(true, mScreenSize, 18.269f, 331.844f));
        assertEquals(
                WearableButtons.LOC_SW,
                WearableButtons.getLocationZone(true, mScreenSize, 70.294f, 409.706f));
        assertEquals(
                WearableButtons.LOC_SSW,
                WearableButtons.getLocationZone(true, mScreenSize, 148.156f, 461.731f));
        assertEquals(
                WearableButtons.LOC_SOUTH,
                WearableButtons.getLocationZone(true, mScreenSize, 240, 480));
        assertEquals(
                WearableButtons.LOC_SSE,
                WearableButtons.getLocationZone(true, mScreenSize, 331.844f, 461.731f));
        assertEquals(
                WearableButtons.LOC_SE,
                WearableButtons.getLocationZone(true, mScreenSize, 409.706f, 409.706f));
        assertEquals(
                WearableButtons.LOC_ESE,
                WearableButtons.getLocationZone(true, mScreenSize, 461.731f, 331.844f));
    }

    @Test
    public void testNonExactPoints_round() {
        // Positions were randomly determined based on #testExactPoints calculations
        // 1 degree in radians = Math.PI / 180;
        // Using formula from #testExactPoints, add and subtract X degrees from each exact point and
        // test edge cases
        // Wolfram Alpha helps again:  x = 240*(1+cos(a)), y=240*(1-sin(a)), a=(15.0*pi/8.0)+pi/180

        // Exact +1 degree
        assertEquals(
                WearableButtons.LOC_ESE,
                WearableButtons.getLocationZone(true, mScreenSize, 463.3f, 327.96f));
        // Exact -1 degree
        assertEquals(
                WearableButtons.LOC_ESE,
                WearableButtons.getLocationZone(true, mScreenSize, 460.094f, 335.7f));

        // Exact +5 degrees
        assertEquals(
                WearableButtons.LOC_WNW,
                WearableButtons.getLocationZone(true, mScreenSize, 16.7f, 152.04f));
        // Exact -5 degrees
        assertEquals(
                WearableButtons.LOC_WNW,
                WearableButtons.getLocationZone(true, mScreenSize, 19.906f, 144.3f));

        // Exactly between SE and ESE
        assertEquals(
                WearableButtons.LOC_ESE,
                WearableButtons.getLocationZone(true, mScreenSize, 439.553f, 373.337f));

        // Exactly between SSE and SE
        assertEquals(
                WearableButtons.LOC_SE,
                WearableButtons.getLocationZone(true, mScreenSize, 373.337f, 439.553f));
    }

    @Test
    public void testEdgePoints_rect() {
        assertEquals(
                WearableButtons.LOC_LEFT_TOP,
                WearableButtons.getLocationZone(false, mScreenSize, 0, 160));
        assertEquals(
                WearableButtons.LOC_LEFT_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 0, 320));
        assertEquals(
                WearableButtons.LOC_LEFT_BOTTOM,
                WearableButtons.getLocationZone(false, mScreenSize, 0, 480));
        assertEquals(
                WearableButtons.LOC_RIGHT_TOP,
                WearableButtons.getLocationZone(false, mScreenSize, 480, 160));
        assertEquals(
                WearableButtons.LOC_RIGHT_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 480, 320));
        assertEquals(
                WearableButtons.LOC_RIGHT_BOTTOM,
                WearableButtons.getLocationZone(false, mScreenSize, 480, 480));
        assertEquals(
                WearableButtons.LOC_TOP_LEFT,
                WearableButtons.getLocationZone(false, mScreenSize, 160, 0));
        assertEquals(
                WearableButtons.LOC_TOP_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 320, 0));
        assertEquals(
                WearableButtons.LOC_TOP_RIGHT,
                WearableButtons.getLocationZone(false, mScreenSize, 479, 0));
        assertEquals(
                WearableButtons.LOC_BOTTOM_LEFT,
                WearableButtons.getLocationZone(false, mScreenSize, 160, 480));
        assertEquals(
                WearableButtons.LOC_BOTTOM_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 320, 480));
        assertEquals(
                WearableButtons.LOC_BOTTOM_RIGHT,
                WearableButtons.getLocationZone(false, mScreenSize, 479, 480));
    }

    @Test
    public void testNonEdgePoints_rect() {
        assertEquals(
                WearableButtons.LOC_LEFT_TOP,
                WearableButtons.getLocationZone(false, mScreenSize, 50, 100));
        assertEquals(
                WearableButtons.LOC_LEFT_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 80, 300));
        assertEquals(
                WearableButtons.LOC_LEFT_BOTTOM,
                WearableButtons.getLocationZone(false, mScreenSize, 100, 350));
        assertEquals(
                WearableButtons.LOC_RIGHT_TOP,
                WearableButtons.getLocationZone(false, mScreenSize, 460, 120));
        assertEquals(
                WearableButtons.LOC_RIGHT_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 450, 270));
        assertEquals(
                WearableButtons.LOC_RIGHT_BOTTOM,
                WearableButtons.getLocationZone(false, mScreenSize, 430, 400));
        assertEquals(
                WearableButtons.LOC_TOP_LEFT,
                WearableButtons.getLocationZone(false, mScreenSize, 130, 20));
        assertEquals(
                WearableButtons.LOC_TOP_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 280, 30));
        assertEquals(
                WearableButtons.LOC_TOP_RIGHT,
                WearableButtons.getLocationZone(false, mScreenSize, 429, 40));
        assertEquals(
                WearableButtons.LOC_BOTTOM_LEFT,
                WearableButtons.getLocationZone(false, mScreenSize, 140, 470));
        assertEquals(
                WearableButtons.LOC_BOTTOM_CENTER,
                WearableButtons.getLocationZone(false, mScreenSize, 290, 460));
        assertEquals(
                WearableButtons.LOC_BOTTOM_RIGHT,
                WearableButtons.getLocationZone(false, mScreenSize, 439, 450));
    }

    @Test
    public void testSimpleStrings() {
        assertEquals(
                R.string.buttons_round_bottom_right,
                WearableButtons.getFriendlyLocationZoneStringId(WearableButtons.LOC_ESE, 1));
        assertEquals(
                R.string.buttons_round_bottom_left_upper,
                WearableButtons.getFriendlyLocationZoneStringId(WearableButtons.LOC_ESE, 2));
        assertEquals(
                R.string.buttons_rect_bottom_left,
                WearableButtons.getFriendlyLocationZoneStringId(
                        WearableButtons.LOC_BOTTOM_LEFT, 1));
        assertEquals(
                R.string.buttons_rect_bottom_left,
                WearableButtons.getFriendlyLocationZoneStringId(
                        WearableButtons.LOC_BOTTOM_LEFT, 2));
    }

    @Test
    public void testSimpleIcons() {
        testRotateDrawable(WearableButtons.LOC_ESE, R.drawable.ic_cc_settings_button_e, 45);
        testRotateDrawable(WearableButtons.LOC_WNW, R.drawable.ic_cc_settings_button_e, -135);
        testRotateDrawable(
                WearableButtons.LOC_BOTTOM_LEFT, R.drawable.ic_cc_settings_button_bottom, 90);
        testRotateDrawable(
                WearableButtons.LOC_TOP_CENTER, R.drawable.ic_cc_settings_button_center, -90);
        testRotateDrawable(WearableButtons.LOC_TOP_LEFT, R.drawable.ic_cc_settings_button_top, -90);
    }

    @Test
    public void testGetButtonsRighty() {
        Map<Integer, TestWearableButtonsProvider.TestWearableButtonLocation> buttons =
                new HashMap<>();
        buttons.put(
                KeyEvent.KEYCODE_STEM_1,
                new TestWearableButtonsProvider.TestWearableButtonLocation(1, 2, 3, 4));

        TestWearableButtonsProvider provider = new TestWearableButtonsProvider(buttons);
        WearableButtons.setWearableButtonsProvider(provider);

        setLeftyModeEnabled(false);
        WearableButtons.ButtonInfo info =
                WearableButtons.getButtonInfo(
                        ApplicationProvider.getApplicationContext(), KeyEvent.KEYCODE_STEM_1);
        assertNotNull(info);
        assertEquals(1, info.getX(), 1.0e-7);
        assertEquals(2, info.getY(), 1.0e-7);
    }

    @Test
    public void testGetButtonsLefty() {
        setLeftyModeEnabled(true);
        Map<Integer, TestWearableButtonsProvider.TestWearableButtonLocation> buttons =
                new HashMap<>();
        buttons.put(
                KeyEvent.KEYCODE_STEM_1,
                new TestWearableButtonsProvider.TestWearableButtonLocation(1, 2, 3, 4));

        TestWearableButtonsProvider provider = new TestWearableButtonsProvider(buttons);
        WearableButtons.setWearableButtonsProvider(provider);
        WearableButtons.ButtonInfo info =
                WearableButtons.getButtonInfo(
                        ApplicationProvider.getApplicationContext(), KeyEvent.KEYCODE_STEM_1);
        assertNotNull(info);
        assertEquals(3, info.getX(), 1.0e-7);
        assertEquals(4, info.getY(), 1.0e-7);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetButtonsLeftyNoData() {
        Context context = ApplicationProvider.getApplicationContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Shadows.shadowOf(display).setWidth(480);
        Shadows.shadowOf(display).setHeight(480);

        setLeftyModeEnabled(true);
        Map<Integer, TestWearableButtonsProvider.TestWearableButtonLocation> buttons =
                new HashMap<>();
        buttons.put(
                KeyEvent.KEYCODE_STEM_1,
                new TestWearableButtonsProvider.TestWearableButtonLocation(1, 2));

        TestWearableButtonsProvider provider = new TestWearableButtonsProvider(buttons);
        WearableButtons.setWearableButtonsProvider(provider);
        WearableButtons.ButtonInfo info =
                WearableButtons.getButtonInfo(context, KeyEvent.KEYCODE_STEM_1);
        assertNotNull(info);
        assertEquals(479, info.getX(), 1.0e-7); // == 480 - 1
        assertEquals(478, info.getY(), 1.0e-7); // == 480 - 2
    }

    private void testRotateDrawable(
            int locationZone, int expectedDrawableId, int expectedDegreeRotation) {
        RotateDrawable rotateDrawable =
                WearableButtons.getButtonIconFromLocationZone(
                        ApplicationProvider.getApplicationContext(), locationZone);

        // We need Robolectric to pull out the underlying resource ID.
        ShadowDrawable rotateDrawableShadow = Shadows.shadowOf(rotateDrawable.getDrawable());

        assertEquals(rotateDrawableShadow.getCreatedFromResId(), expectedDrawableId);
        assertEquals(expectedDegreeRotation, rotateDrawable.getFromDegrees(), .001);
    }

    private void setLeftyModeEnabled(boolean enabled) {
        Settings.System.putInt(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.System.USER_ROTATION,
                enabled ? Surface.ROTATION_180 : Surface.ROTATION_0);
    }
}
