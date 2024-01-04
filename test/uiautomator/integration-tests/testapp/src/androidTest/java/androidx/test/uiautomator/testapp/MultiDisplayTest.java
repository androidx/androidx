/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SdkSuppress(minSdkVersion = 30)
public class MultiDisplayTest extends BaseTest {
    @Before
    public void assumeMultiDisplay() {
        // Tests need to run with multiple displays.
        assumeTrue(getDisplayIds().size() > 1);
    }

    @Test
    public void testMultiDisplay_selector() {
        int secondaryDisplayId = getSecondaryDisplayId();
        launchTestActivity(MainActivity.class);
        launchTestActivityOnDisplay(IsFocusedTestActivity.class, secondaryDisplayId);

        // Found when display ID not specified.
        assertEquals(2, mDevice.findObjects(By.res(TEST_APP, "button")).size());

        // Not found with wrong display ID.
        assertFalse(mDevice.hasObject(
                By.res(TEST_APP, "nested_elements").displayId(secondaryDisplayId)));
        assertFalse(
                mDevice.hasObject(By.res(TEST_APP, "focusable_text_view").displayId(
                        Display.DEFAULT_DISPLAY)));

        // Found with correct display ID.
        assertTrue(
                mDevice.hasObject(
                        By.res(TEST_APP, "nested_elements").displayId(Display.DEFAULT_DISPLAY)));
        assertTrue(mDevice.hasObject(
                By.res(TEST_APP, "focusable_text_view").displayId(secondaryDisplayId)));
    }

    @Test
    public void testMultiDisplay_click() {
        int secondaryDisplayId = getSecondaryDisplayId();
        launchTestActivityOnDisplay(ClickTestActivity.class, secondaryDisplayId);

        UiObject2 button = mDevice.findObject(
                By.res(TEST_APP, "button1").displayId(secondaryDisplayId));
        button.click();
        assertEquals(secondaryDisplayId, button.getDisplayId());
        assertTrue(button.wait(Until.textEquals("text1_clicked"), TIMEOUT_MS));
    }

    @Test
    public void testMultiDisplay_treeRelationship() {
        int secondaryDisplayId = getSecondaryDisplayId();
        launchTestActivityOnDisplay(ParentChildTestActivity.class, secondaryDisplayId);

        // Different display IDs between parent and child.
        BySelector invalidChildSelector = By.res(TEST_APP, "tree_N3").displayId(
                Display.DEFAULT_DISPLAY);
        List<UiObject2> invalidParent =
                mDevice.findObjects(
                        By.hasChild(invalidChildSelector).displayId(secondaryDisplayId));
        assertEquals(0, invalidParent.size());

        // Same display ID between ancestor and descendant.
        BySelector validAncestorSelector =
                By.res(TEST_APP, "tree_N1").displayId(secondaryDisplayId);
        UiObject2 validDescendant =
                mDevice.findObject(By.hasAncestor(validAncestorSelector).displayId(
                        secondaryDisplayId).textContains("4"));
        assertEquals("tree_N4", validDescendant.getText());
    }

    @Test
    public void testMultiDisplay_displayMetrics() throws IOException {
        int secondaryDisplayId = getSecondaryDisplayId();

        try {
            int width = 800;
            int height = 400;
            mDevice.executeShellCommand(
                    String.format("wm size %dx%d -d %d", width, height, secondaryDisplayId));

            assertEquals(width, mDevice.getDisplayWidth(secondaryDisplayId));
            assertEquals(height, mDevice.getDisplayHeight(secondaryDisplayId));
        } finally {
            mDevice.executeShellCommand(
                    String.format("wm size reset -d %d", secondaryDisplayId));
        }
    }

    @Test
    public void testMultiDisplay_orientations() {
        int secondaryDisplayId = getSecondaryDisplayId();

        try {
            mDevice.setOrientationNatural(secondaryDisplayId);
            assertEquals(Surface.ROTATION_0, mDevice.getDisplayRotation(secondaryDisplayId));

            mDevice.setOrientationLeft(secondaryDisplayId);
            assertEquals(Surface.ROTATION_90, mDevice.getDisplayRotation(secondaryDisplayId));

            mDevice.setOrientationRight(secondaryDisplayId);
            assertEquals(Surface.ROTATION_270, mDevice.getDisplayRotation(secondaryDisplayId));

            mDevice.setOrientationPortrait(secondaryDisplayId);
            assertTrue(mDevice.getDisplayHeight(secondaryDisplayId) >= mDevice.getDisplayWidth(
                    secondaryDisplayId));

            mDevice.setOrientationLandscape(secondaryDisplayId);
            assertTrue(mDevice.getDisplayHeight(secondaryDisplayId) <= mDevice.getDisplayWidth(
                    secondaryDisplayId));
        } finally {
            mDevice.setOrientationNatural(secondaryDisplayId);
        }
    }

    // Helper to launch an activity on a specific display.
    private void launchTestActivityOnDisplay(@NonNull Class<? extends Activity> activity,
            int displayId) {
        launchTestActivity(activity, new Intent().setFlags(DEFAULT_FLAGS),
                ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle());
    }

    // Helper to get all the display IDs in the current testing environment.
    private static Set<Integer> getDisplayIds() {
        Context context = ApplicationProvider.getApplicationContext();
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        return Arrays.stream(displayManager.getDisplays()).map(Display::getDisplayId).collect(
                Collectors.toSet());
    }

    // Helper to get the ID of the first non-default display.
    private static int getSecondaryDisplayId() {
        return getDisplayIds().stream().filter(
                id -> id != Display.DEFAULT_DISPLAY).findFirst().orElse(-1);
    }
}
