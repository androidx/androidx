/*
 * Copyright 2025 The Android Open Source Project
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

import static android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiWindow;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@LargeTest
public class UiWindowTest extends BaseTest {
    private static final String APP_NAME = "UiAutomator Test App";
    private static final long TIMEOUT_MS = 5_000;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        launchTestActivityInMultiWindow(UiWindowTestActivity.class);
    }

    void launchTestActivityInMultiWindow(@NonNull Class<? extends Activity> activity) {
        launchTestActivity(
                activity,
                new Intent().setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
                null);
    }

    @Test
    public void testUiDevice_hasWindow() {
        assertTrue(
                "Test app window should exist",
                mDevice.hasWindow(By.Window.type(TYPE_APPLICATION).pkg(TEST_APP)));
        assertTrue(
                "At least one system window should exist",
                mDevice.hasWindow(By.Window.type(TYPE_SYSTEM)));
    }

    @Test
    public void testUiDevice_findWindow() {
        // Verify that the test app window is found and active using ByWindowSelector.
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull("Test app window should be active and non-null", window);
        assertEquals(TEST_APP, window.getPackageName());
        assertTrue(window.isActive());
    }

    @Test
    public void testUiDevice_findWindowByTitle() {
        String expectedTitle = APP_NAME;
        UiWindow window = mDevice.findWindow(By.Window.title(expectedTitle));
        assertNotNull(window);
        assertEquals(expectedTitle, window.getTitle());
    }

    @Test
    public void testUiDevice_findAppHeaderWindow() {
        assumeTrue("The app header window is present only in desktop mode", isDesktopWindowing());

        // Get the app window and its layer (z-order).
        UiWindow appWindow =
                mDevice.findWindow(By.Window.pkg(TEST_APP).type(TYPE_APPLICATION).active(true));
        assertNotNull(appWindow);
        int appWindowLayer = appWindow.getLayer();

        // Find the header window that is on top of the app window using layerAbove().
        UiWindow headerWindow =
                mDevice.findWindow(
                        By.Window
                                .pkg("com.android.systemui")
                                .type(TYPE_APPLICATION)
                                .layerAbove(appWindowLayer));
        assertNotNull("No header window found for app window: " + appWindow, headerWindow);
        assertTrue(
                "Header window should be above the app window's layer",
                headerWindow.getLayer() > appWindowLayer);
        assertTrue(
                "Should have a close button in header",
                headerWindow.hasObject(By.res("com.android.systemui", "close_window")));
    }

    @Test
    public void testUiDevice_closeNonActiveDesktopWindow() {
        assumeTrue("The app header window is present only in desktop mode", isDesktopWindowing());

        // Launch another test app window in desktop mode. Total 2 windows.
        launchTestActivityInMultiWindow(UiWindowTestActivity.class);
        List<UiWindow> appWindows = mDevice.findWindows(By.Window.pkg(TEST_APP));
        assertEquals(2, appWindows.size());

        // Find the non-active window that is the lowest z-order window and the last one in the
        // window stack.
        UiWindow nonActiveWindow = appWindows.get(1);
        assertNotNull(nonActiveWindow);
        assertFalse(nonActiveWindow.isActive());
        int nonActiveWindowId = nonActiveWindow.getId();

        List<UiWindow> windows =
                mDevice.findWindows(
                        By.Window
                                .pkg("com.android.systemui")
                                .type(TYPE_APPLICATION)
                                .layerAbove(nonActiveWindow.getLayer()));
        assertFalse(windows.isEmpty());
        UiWindow headerWindow = windows.get(windows.size() - 1);
        assertNotNull("No header window found for app window: " + nonActiveWindow, headerWindow);

        // Find the close button in the header window, click it to close the window.
        UiObject2 closeButton = headerWindow.findObject(
                By.res("com.android.systemui", "close_window"));
        assertNotNull(closeButton);
        assertTrue(closeButton.isClickable());
        closeButton.click();
        // Wait for the app window to no longer be present, identified by its unique window ID.
        assertTrue(
                "Window should no longer be present if the close button is clicked.",
                mDevice.wait(d -> !d.hasWindow(By.Window.id(nonActiveWindowId)), TIMEOUT_MS));
    }

    @Test
    public void testUiDevice_doesNotFindNonExistentWindow() {
        UiWindow window = mDevice.findWindow(By.Window.title("non existent title"));
        assertNull("Should not find a non-existent window", window);
    }

    @Test
    public void testUiDevice_doesNotFindNonExistentWindows() {
        List<UiWindow> windows = mDevice.findWindows(By.Window.title("non existent title"));
        assertNotNull("Should not return null list", windows);
        assertTrue("Should not find any non-existent windows", windows.isEmpty());
    }

    @Test
    public void testUiDevice_findWindows() {
        assumeTrue("Desktop mode required for multi-window", isDesktopWindowing());
        // Launch another test app window in desktop mode. Total 2 windows.
        launchTestActivityInMultiWindow(UiWindowTestActivity.class);

        // Verify that there are two windows of the test app now.
        List<UiWindow> windows = mDevice.findWindows(By.Window.pkg(TEST_APP));
        assertNotNull(windows);
        assertEquals(2, windows.size());
        for (UiWindow window : windows) {
            assertEquals(TEST_APP, window.getPackageName());
        }
    }

    @Test
    public void testUiWindow_equals() {
        UiWindow window1 = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        UiWindow window2 = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window1);
        assertNotNull(window2);
        assertEquals("Window should be equal to itself", window1, window1);
        assertEquals("Window should be equal to another window with same properties", window1,
                window2);

        UiWindow systemWindow = mDevice.findWindow(By.Window.type(TYPE_SYSTEM));
        if (systemWindow != null) {
            assertNotEquals(window1, systemWindow);
            assertNotEquals(window1.hashCode(), systemWindow.hashCode());
        }
    }

    @Test
    public void testUiWindow_hasObject() {
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window);
        assertTrue(
                "Button should be searchable within this window",
                window.hasObject(By.res(TEST_APP, "launch_task_window_button")));
    }

    @Test
    public void testUiWindow_findObject() {
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window);
        // Window-to-object search.
        UiObject2 button = window.findObject(By.res(TEST_APP, "launch_task_window_button"));
        assertNotNull("Button should be accessible within this window", button);
        assertEquals("launch_task_window", button.getText());
    }

    @Test
    public void testUiWindow_findObjects() {
        assumeTrue("Desktop mode required for multi-window", isDesktopWindowing());

        // Launch another test app window in desktop mode. Total 2 windows.
        launchTestActivityInMultiWindow(UiWindowTestActivity.class);

        // Window-to-object search.
        // Verify that two windows of the test app are now open, and each has two buttons.
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window);
        List<UiObject2> buttons = window.findObjects(By.clazz("android.widget.Button"));
        assertEquals("Should find two buttons in one window", 2, buttons.size());
    }

    @Test
    public void testUiWindow_getRootObjectAndPackageName() {
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window);
        UiObject2 root = window.getRootObject();
        assertNotNull(root);
        assertEquals(TEST_APP, root.getApplicationPackage());

        UiWindow systemWindow = mDevice.findWindow(By.Window.pkg("com.android.systemui"));
        if (systemWindow != null) {
            root = systemWindow.getRootObject();
            assertNotNull(root);
            assertEquals("com.android.systemui", root.getApplicationPackage());
        }
    }

    @Test(expected = StaleObjectException.class)
    public void testUiWindow_staleWindowException() {
        // Get a window reference.
        UiWindow window = mDevice.findWindow(By.Window.pkg(TEST_APP).active(true));
        assertNotNull(window);

        // Close the activity to make the window stale
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "close_task_window_button"));
        assertNotNull(button);
        button.click();
        mDevice.wait(d -> !d.hasWindow(By.Window.pkg(TEST_APP)), TIMEOUT_MS);

        // If the window is gone and stale, accessing it will throw a StaleObjectException.
        window.getRootObject();
        fail("Should not reach here. Expected exception was not caught when accessing stale "
                + "window");
    }
}
