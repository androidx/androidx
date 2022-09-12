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

import android.graphics.Point;
import android.widget.TextView;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/** Integration tests for {@link androidx.test.uiautomator.UiDevice}. */
@LargeTest
public class UiDeviceTest extends BaseTest {

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
                Until.newWindow(), 10_000));
    }

    @Test
    public void testSetCompressedLayoutHeirarchy() { // NOTYPO: already-existing typo
        launchTestActivity(MainActivity.class);

        mDevice.setCompressedLayoutHeirarchy(true); // NOTYPO
        assertNull(mDevice.findObject(By.res(TEST_APP, "nested_elements")));

        mDevice.setCompressedLayoutHeirarchy(false); // NOTYPO
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "nested_elements")));
    }

    /* TODO(b/235841020): Implement these tests, and the tests for exceptions of each tested method.

    public void testGetInstance() {}

    public void testGetInstance_withInstrumentation() {}

    public void testGetDisplaySizeDp() {}

    public void testGetProductName() {}

    public void testGetLastTraversedText() {}

    public void testClearLastTraversedText() {}

    public void testPressMenu() {}

    public void testPressBack() {}

    public void testPressHome() {}

    public void testPressSearch() {}

    public void testPressDPadCenter() {}

    public void testPressDPadDown() {}

    public void testPressDPadUp() {}

    public void testPressDPadLeft() {}

    public void testPressDPadRight() {}

    public void testPressDelete() {}

    public void testPressEnter() {}

    public void testPressKeyCode() {}

    public void testPressKeyCode_withMetaState() {}

    public void testPressRecentApps() {}

    public void testOpenNotification() {}

    public void testOpenQuickSettings() {}

    public void testGetDisplayWidth() {}

    public void testGetDisplayHeight() {}
     */

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

    /* TODO(b/235841020): Implement these tests, and the tests for exceptions of each tested method.

    public void testSwipe() {}

    public void testDrag() {}

    public void testSwipe_withPointArray() {}

    public void testWaitForIdle() {}

    public void testWaitForIdle_withTimeout() {}

    public void testGetCurrentActivityName() {}

    public void testGetCurrentPackageName() {}

    public void testRegisterWatcher() {}

    public void testRemoveWatcher() {}

    public void testRunWatchers() {}

    public void testResetWatcherTriggers() {}

    public void testHasWatcherTriggered() {}

    public void testHasAnyWatcherTriggered() {}

    public void testIsNaturalOrientation() {}

    public void testGetDisplayRotation() {}

    public void testFreezeRotation() {}

    public void testUnfreezeRotation() {}

    public void testSetOrientationLeft() {}

    public void testSetOrientationRight() {}

    public void testSetOrientationNatural() {}

    public void testWakeUp() {}

    public void testIsScreenOn() {}

    public void testSleep() {}

    public void testDumpWindowHierarchy_withString() {}

    public void testDumpWindowHierarchy_withFile() {} // already added

    public void testDumpWindowHierarchy_withOutputStream() {}
    */

    @Test
    public void testDumpWindowHierarchy() throws Exception {
        launchTestActivity(MainActivity.class);
        File outFile = mTmpDir.newFile();
        mDevice.dumpWindowHierarchy(outFile);

        // Verify that a valid XML file was generated and that node attributes are correct.
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outFile);
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

    /* TODO(b/235841020): Implement these tests, and the tests for exceptions of each tested method.

    public void testWaitForWindowUpdate() {}

    public void testTakeScreenshot() {} // already added

    public void testTakeScreenshot_withScaleAndQuality() {} // already added

    public void testGetLauncherPackageName() {}

    public void testExecuteShellCommand() {} // already added
    */
}
