/*
 * Copyright 2022 The Android Open Source Project
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.UiAutomation;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/** Integration tests for {@link androidx.test.uiautomator.UiDevice}. */
@LargeTest
public class UiDeviceTest extends BaseTest {

    private static final long TIMEOUT_MS = 5_000;
    private static final int GESTURE_MARGIN = 50;
    private static final String PACKAGE_NAME = "androidx.test.uiautomator.testapp";
    // Defined in 'AndroidManifest.xml'.
    private static final String APP_NAME = "UiAutomator Test App";

    @Rule
    public TemporaryFolder mTmpDir = new TemporaryFolder();

    @Test
    public void testHasObject() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.hasObject(By.text("Accessible button")));
        assertFalse(mDevice.hasObject(By.text("non-existent text")));
    }

    @Test
    public void testFindObjects() {
        launchTestActivity(MainActivity.class);

        // The package name in the `By` selector needs to be specified, otherwise
        // `mDevice.findObjects` will grab all the `TextView` in the display screen.
        // Note that the items in `ListView` will also be `TextView`. So there are 9 `TextView`s
        // in total.
        List<UiObject2> results = mDevice.findObjects(By.pkg(TEST_APP).clazz(TextView.class));
        assertEquals(9, results.size());

        Set<String> resultTexts = new HashSet<>();
        for (UiObject2 result : results) {
            resultTexts.add(result.getText());
        }
        assertTrue(resultTexts.contains("Third Level"));
        assertTrue(resultTexts.contains("Second Level"));
        assertTrue(resultTexts.contains("First Level"));
        assertTrue(resultTexts.contains("Sample text"));
        assertTrue(resultTexts.contains("Text View 1"));
        assertTrue(resultTexts.contains("TextView with an id"));
        assertTrue(resultTexts.contains("Item1"));
        assertTrue(resultTexts.contains("Item2"));
        assertTrue(resultTexts.contains("Item3"));

        assertEquals(0, mDevice.findObjects(By.text("non-existent text")).size());
    }

    @Test
    public void testPerformActionAndWait() {
        launchTestActivity(ClickAndWaitTestActivity.class);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        Point buttonCenter = button.getVisibleCenter();

        assertTrue(mDevice.performActionAndWait(() -> mDevice.click(buttonCenter.x, buttonCenter.y),
                Until.newWindow(), TIMEOUT_MS));
    }

    @Test
    public void testSetCompressedLayoutHeirarchy() { // NOTYPO: already-existing typo
        launchTestActivity(MainActivity.class);

        mDevice.setCompressedLayoutHeirarchy(true); // NOTYPO
        assertNull(mDevice.findObject(By.res(TEST_APP, "nested_elements")));

        mDevice.setCompressedLayoutHeirarchy(false); // NOTYPO
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "nested_elements")));
    }

    @Test
    public void testGetInstance() {
        assertEquals(mDevice, UiDevice.getInstance());
    }

    @Test
    public void testGetInstance_withInstrumentation() {
        assertEquals(mDevice, UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
    }

    @Test
    public void testPressMenu() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressMenu();
        assertEquals("keycode menu pressed", textView.getText());
    }

    @Test
    public void testPressBack() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressBack();
        assertEquals("keycode back pressed", textView.getText());
    }

    @Test
    public void testPressSearch() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressSearch();
        assertEquals("keycode search pressed", textView.getText());
    }

    @Test
    public void testPressDPadCenter() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDPadCenter();
        assertEquals("keycode dpad center pressed", textView.getText());
    }

    @Test
    public void testPressDPadDown() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDPadDown();
        assertEquals("keycode dpad down pressed", textView.getText());
    }

    @Test
    public void testPressDPadUp() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDPadUp();
        assertEquals("keycode dpad up pressed", textView.getText());
    }

    @Test
    public void testPressDPadLeft() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDPadLeft();
        assertEquals("keycode dpad left pressed", textView.getText());
    }

    @Test
    public void testPressDPadRight() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDPadRight();
        assertEquals("keycode dpad right pressed", textView.getText());
    }

    @Test
    public void testPressDelete() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressDelete();
        assertEquals("keycode delete pressed", textView.getText());
    }

    @Test
    public void testPressEnter() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressEnter();
        assertEquals("keycode enter pressed", textView.getText());
    }

    @Test
    public void testPressKeyCode() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressKeyCode(KeyEvent.KEYCODE_0);
        assertEquals("keycode 0 pressed", textView.getText());
    }

    @Test
    public void testPressKeyCode_withMetaState() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressKeyCode(KeyEvent.KEYCODE_Z,
                KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON);
        assertEquals("keycode Z pressed with meta shift left on", textView.getText());
    }

    @Test
    public void testPressRecentApps() throws Exception {
        launchTestActivity(KeycodeTestActivity.class);

        // No app name when the app is running.
        assertFalse(mDevice.wait(Until.hasObject(By.text(APP_NAME)), TIMEOUT_MS));

        mDevice.pressRecentApps();

        Pattern iconResIdPattern = Pattern.compile(".*launcher.*icon");
        // For API 28 and above, click on the app icon to make the name visible.
        if (mDevice.wait(Until.hasObject(By.res(iconResIdPattern)), TIMEOUT_MS)) {
            UiObject2 icon = mDevice.findObject(By.res(iconResIdPattern));
            icon.click();
        }

        // App name appears when on Recent screen.
        assertTrue(mDevice.wait(Until.hasObject(By.text(APP_NAME)), TIMEOUT_MS));
    }

    @Test
    public void testMultipleKeys() {
        launchTestActivity(KeycodeTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
        mDevice.pressKeyCodes(new int[]{KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B});
        assertEquals("keycode A and keycode B are pressed", textView.getText());
    }

    @Test
    public void testOpenNotification() {
        launchTestActivity(NotificationTestActivity.class);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "notification_button"));
        button.click();

        mDevice.openNotification();

        assertTrue(mDevice.wait(Until.hasObject(By.text("Test Notification")), TIMEOUT_MS));
    }

    @Test
    public void testOpenQuickSettings() {
        mDevice.openQuickSettings();

        assertTrue(mDevice.wait(Until.hasObject(By.res(Pattern.compile(".*quick_settings_panel"))),
                TIMEOUT_MS));
    }

    @Test
    public void testClick() {
        launchTestActivity(UiDeviceTestClickActivity.class);

        // Click a button in the middle of the activity.
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        mDevice.click(width / 2, height / 2);

        // Verify that the button was clicked.
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertNotNull(button);
        assertEquals("I've been clicked!", button.getText());
    }

    @Test
    public void testSwipe() {
        launchTestActivity(SwipeTestActivity.class);

        UiObject2 swipeRegion = mDevice.findObject(By.res(TEST_APP, "swipe_region"));

        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        mDevice.swipe(GESTURE_MARGIN, height / 2, width - GESTURE_MARGIN, height / 2, 10);

        assertTrue(swipeRegion.wait(Until.textEquals("swipe_right"), TIMEOUT_MS));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDrag() {
        launchTestActivity(DragTestActivity.class);

        UiObject2 dragButton = mDevice.findObject(By.res(TEST_APP, "drag_button"));
        UiObject2 dragDestination = mDevice.findObject(By.res(TEST_APP, "drag_destination"));

        Point start = dragButton.getVisibleCenter();
        Point end = dragDestination.getVisibleCenter();

        assertEquals("no_drag_yet", dragDestination.getText());
        mDevice.drag(start.x, start.y, end.x, end.y, 10);
        assertTrue(dragDestination.wait(Until.textEquals("drag_received"), TIMEOUT_MS));
    }

    @Test
    public void testSwipe_withPointArray() {
        launchTestActivity(SwipeTestActivity.class);

        UiObject2 swipeRegion = mDevice.findObject(By.res(TEST_APP, "swipe_region"));

        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();

        Point point1 = new Point(GESTURE_MARGIN, height / 2);
        Point point2 = new Point(width / 2, height / 2);
        Point point3 = new Point(width - GESTURE_MARGIN, height / 2);

        mDevice.swipe(new Point[]{point1, point2, point3}, 10);

        assertTrue(swipeRegion.wait(Until.textEquals("swipe_right"), TIMEOUT_MS));
    }

    @Test
    public void testGetCurrentPackageName() {
        launchTestActivity(KeycodeTestActivity.class);

        assertEquals(PACKAGE_NAME, mDevice.getCurrentPackageName());
    }

    @Test
    public void testSetOrientationLeft() throws Exception {
        launchTestActivity(KeycodeTestActivity.class);
        try {
            assertTrue(mDevice.isNaturalOrientation());
            assertEquals(UiAutomation.ROTATION_FREEZE_0, mDevice.getDisplayRotation());
            mDevice.setOrientationLeft();
            // Make the device wait for 1 sec for the rotation animation to finish.
            SystemClock.sleep(1_000);
            assertFalse(mDevice.isNaturalOrientation());
            assertEquals(UiAutomation.ROTATION_FREEZE_90, mDevice.getDisplayRotation());
            mDevice.setOrientationNatural();
            SystemClock.sleep(1_000);
            assertTrue(mDevice.isNaturalOrientation());
        } finally {
            mDevice.unfreezeRotation();
        }
    }

    @Test
    public void testSetOrientationRight() throws Exception {
        launchTestActivity(KeycodeTestActivity.class);
        try {
            assertTrue(mDevice.isNaturalOrientation());
            assertEquals(UiAutomation.ROTATION_FREEZE_0, mDevice.getDisplayRotation());
            mDevice.setOrientationRight();
            SystemClock.sleep(1_000);
            assertFalse(mDevice.isNaturalOrientation());
            assertEquals(UiAutomation.ROTATION_FREEZE_270, mDevice.getDisplayRotation());
            mDevice.setOrientationNatural();
            SystemClock.sleep(1_000);
            assertTrue(mDevice.isNaturalOrientation());
        } finally {
            mDevice.unfreezeRotation();
        }
    }

    @Test
    public void testIsScreenOn() throws Exception {
        launchTestActivity(MainActivity.class);

        mDevice.wakeUp();
        assertTrue(mDevice.isScreenOn());

        try {
            mDevice.sleep();
            assertFalse(mDevice.isScreenOn());
        } finally {
            mDevice.wakeUp();
            mDevice.pressMenu();
            assertTrue("Failed to wake up device and remove lockscreen",
                    mDevice.hasObject(By.pkg(TEST_APP)));
        }
    }

    @Test
    public void testDumpWindowHierarchy_withString() throws Exception {
        launchTestActivity(MainActivity.class);
        File outFile = mTmpDir.newFile();
        mDevice.dumpWindowHierarchy(outFile.getAbsolutePath());

        // Verify that a valid XML file was generated and that node attributes are correct.
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outFile);
        validateMainActivityXml(xml);
    }

    @Test
    public void testDumpWindowHierarchy_withFile() throws Exception {
        launchTestActivity(MainActivity.class);
        File outFile = mTmpDir.newFile();
        mDevice.dumpWindowHierarchy(outFile);

        // Verify that a valid XML file was generated and that node attributes are correct.
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outFile);
        validateMainActivityXml(xml);
    }

    @Test
    public void testDumpWindowHierarchy_withOutputStream() throws Exception {
        launchTestActivity(MainActivity.class);
        File outFile = mTmpDir.newFile();
        FileOutputStream outStream = new FileOutputStream(outFile);
        mDevice.dumpWindowHierarchy(outStream);

        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outFile);
        validateMainActivityXml(xml);
    }


    @FlakyTest(bugId = 259299647)
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void testWaitForWindowUpdate() {
        launchTestActivity(WaitTestActivity.class);

        // Returns false when the current window doesn't have the specified package name.
        assertFalse(mDevice.waitForWindowUpdate("non-existent package name", 1_000));

        UiObject2 text1 = mDevice.findObject(By.res(TEST_APP, "text_1"));

        // Returns true when change happens in the current window within the timeout.
        text1.click();
        assertTrue(mDevice.waitForWindowUpdate(PACKAGE_NAME, 5_000));

        // Returns false when no change happens in the current window within the timeout.
        text1.click();
        assertFalse(mDevice.waitForWindowUpdate(PACKAGE_NAME, 1_000));
    }

    @Test
    public void testGetLauncherPackageName() {
        assertTrue(mDevice.wait(Until.hasObject(By.pkg(mDevice.getLauncherPackageName())), 5_000));
    }

    private static void validateMainActivityXml(Document xml) throws Exception {
        Element element = (Element) XPathFactory.newInstance().newXPath()
                .compile("//hierarchy//*/node[@resource-id='" + TEST_APP + ":id/button']")
                .evaluate(xml, XPathConstants.NODE);
        assertNotNull(element);
        assertNotNull(Integer.valueOf(element.getAttribute("index")));
        assertEquals("Accessible button", element.getAttribute("text"));
        assertEquals(TEST_APP + ":id/button", element.getAttribute("resource-id"));
        assertEquals("android.widget.Button", element.getAttribute("class"));
        assertEquals(TEST_APP, element.getAttribute("package"));
        assertEquals("I'm accessible!", element.getAttribute("content-desc"));
        assertEquals("false", element.getAttribute("checkable"));
        assertEquals("false", element.getAttribute("checked"));
        assertEquals("true", element.getAttribute("clickable"));
        assertEquals("true", element.getAttribute("enabled"));
        assertEquals("true", element.getAttribute("focusable"));
        assertNotNull(element.getAttribute("focused"));
        assertEquals("false", element.getAttribute("scrollable"));
        assertEquals("false", element.getAttribute("long-clickable"));
        assertEquals("false", element.getAttribute("password"));
        assertEquals("false", element.getAttribute("selected"));
        assertEquals("true", element.getAttribute("visible-to-user"));
        assertNotNull(element.getAttribute("bounds"));
    }
}
