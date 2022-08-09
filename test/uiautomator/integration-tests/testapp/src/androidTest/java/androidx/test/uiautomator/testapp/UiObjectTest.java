/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;

import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

public class UiObjectTest extends BaseTest {
    private static final int TIMEOUT_MS = 10_000;

    @Test
    public void testGetChild() throws Exception {
        launchTestActivity(ParentChildTestActivity.class);

        UiObject treeN2 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/tree_N2"));
        UiObject treeN3 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/tree_N3"));

        assertFalse(
                treeN2.getChild(new UiSelector().resourceId(TEST_APP + ":id/tree_N4")).exists());
        assertTrue(treeN3.getChild(new UiSelector().resourceId(TEST_APP + ":id/tree_N4")).exists());
    }

    @Test
    public void testGetFromParent() throws Exception {
        launchTestActivity(ParentChildTestActivity.class);

        UiObject treeN4 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/tree_N4"));

        assertFalse(treeN4.getFromParent(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N2")).exists());
        assertTrue(treeN4.getFromParent(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N5")).exists());
    }

    @Test
    public void testGetChildCount() throws Exception {
        launchTestActivity(ParentChildTestActivity.class);

        UiObject treeN2 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/tree_N2"));
        UiObject treeN3 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/tree_N3"));

        assertEquals(0, treeN2.getChildCount());
        assertEquals(2, treeN3.getChildCount());
    }

    @Test
    public void testGetChildCount_throwsUiObjectNotFoundException() {
        launchTestActivity(ParentChildTestActivity.class);

        UiObject noNode = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"));

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::getChildCount);
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDragTo_destObjAndSteps() throws Exception {
        launchTestActivity(DragTestActivity.class);

        UiObject dragButton = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_button"));
        UiObject dragDestination = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_destination"));

        UiObject expectedDragDest = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_destination").text("drag_received"));

        assertEquals("no_drag_yet", dragDestination.getText());
        // Returning true from `dragTo` means that the drag action is performed successfully, not
        // necessarily the target is dragged to the desired destination.
        // The same applies to all the following tests.
        assertTrue(dragButton.dragTo(dragDestination, 40));
        assertTrue(expectedDragDest.waitForExists(TIMEOUT_MS));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDragTo_destXAndDestYAndSteps() throws Exception {
        launchTestActivity(DragTestActivity.class);

        UiObject dragButton = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_button"));
        UiObject dragDestination = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_destination"));

        UiObject expectedDragDest = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/drag_destination").text("drag_received"));
        Rect destBounds = dragDestination.getVisibleBounds();

        assertEquals("no_drag_yet", dragDestination.getText());
        assertTrue(dragButton.dragTo(destBounds.centerX(), destBounds.centerY(), 40));
        assertTrue(expectedDragDest.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testSwipeUp() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiObject swipeRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/swipe_region"));
        UiObject verySmallRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/very_small_region"));

        UiObject expectedSwipeRegion = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id"
                        + "/swipe_region").text("swipe_up"));

        assertFalse(verySmallRegion.swipeUp(100));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeUp(100));
        assertTrue(expectedSwipeRegion.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testSwipeDown() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiObject swipeRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/swipe_region"));
        UiObject verySmallRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/very_small_region"));

        UiObject expectedSwipeRegion = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id"
                        + "/swipe_region").text("swipe_down"));

        assertFalse(verySmallRegion.swipeDown(100));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeDown(100));
        assertTrue(expectedSwipeRegion.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testSwipeLeft() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiObject swipeRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/swipe_region"));
        UiObject verySmallRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/very_small_region"));

        UiObject expectedSwipeRegion = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id"
                        + "/swipe_region").text("swipe_left"));

        assertFalse(verySmallRegion.swipeLeft(100));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeLeft(100));
        assertTrue(expectedSwipeRegion.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testSwipeRight() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiObject swipeRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/swipe_region"));
        UiObject verySmallRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/very_small_region"));

        UiObject expectedSwipeRegion = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id"
                        + "/swipe_region").text("swipe_right"));

        assertFalse(verySmallRegion.swipeRight(100));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeRight(100));
        assertTrue(expectedSwipeRegion.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testClick() throws Exception {
        launchTestActivity(ClickTestActivity.class);

        UiObject button1 = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button1"));

        UiObject expectedButton1 = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button1").text("text1_clicked"));

        assertEquals("text1", button1.getText());
        assertTrue(button1.click());
        assertTrue(expectedButton1.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testClickAndWaitForNewWindow() throws Exception {
        launchTestActivity(ClickAndWaitTestActivity.class);

        UiObject newWindowsButton = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/new_window_button"));

        assertTrue(newWindowsButton.clickAndWaitForNewWindow());
    }

    @Test
    public void testClickAndWaitForNewWindow_timeout() throws Exception {
        launchTestActivity(ClickAndWaitTestActivity.class);

        UiObject newWindowsButton = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/new_window_button"));

        assertTrue(newWindowsButton.clickAndWaitForNewWindow(4_000));
    }

    @Test
    public void testClickTopLeft() throws Exception {
        launchTestActivity(ClickOnPositionTestActivity.class);

        UiObject clickRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/click_region"));

        assertEquals("click_region", clickRegion.getText());
        assertTrue(clickRegion.clickTopLeft());
        assertEquals("top_left_clicked", clickRegion.getText());
    }

    @Test
    public void testLongClickBottomRight() throws Exception {
        launchTestActivity(ClickOnPositionTestActivity.class);

        UiObject clickRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/click_region"));

        assertEquals("click_region", clickRegion.getText());
        assertTrue(clickRegion.longClickBottomRight());
        assertEquals("bottom_right_long_clicked", clickRegion.getText());
    }

    @Test
    public void testClickBottomRight() throws Exception {
        launchTestActivity(ClickOnPositionTestActivity.class);

        UiObject clickRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/click_region"));

        assertEquals("click_region", clickRegion.getText());
        assertTrue(clickRegion.clickBottomRight());
        assertEquals("bottom_right_clicked", clickRegion.getText());
    }

    @Test
    public void testLongClick() throws Exception {
        launchTestActivity(ClickOnPositionTestActivity.class);

        UiObject clickRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/click_region"));

        assertEquals("click_region", clickRegion.getText());
        assertTrue(clickRegion.longClick());
        assertTrue(clickRegion.getText().contains("long"));
    }

    @Test
    public void testLongClickTopLeft() throws Exception {
        launchTestActivity(ClickOnPositionTestActivity.class);

        UiObject clickRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/click_region"));

        assertEquals("click_region", clickRegion.getText());
        assertTrue(clickRegion.longClickTopLeft());
        assertEquals("top_left_long_clicked", clickRegion.getText());
    }

    @Test
    public void testClickFamily_throwsUiObjectNotFoundException() {
        launchTestActivity(ClickTestActivity.class);

        UiObject noNode = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"));

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::click);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::clickAndWaitForNewWindow);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::clickTopLeft);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::longClickBottomRight);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::clickBottomRight);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::longClick);

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::longClickTopLeft);
    }

    /* TODO(b/241158642): Implement these tests, and the tests for exceptions of each tested method.

    public void testGetText() {}

    public void testGetClassName() {}

    public void testGetContentDescription() {}

    public void testLegacySetText() {}

    public void testSetText() {}

    public void testClearTextField() {}

    public void testIsChecked() {}

    public void testIsSelected() {}

    public void testIsCheckable() {}

    public void testIsEnabled() {}

    public void testIsClickable() {}

    public void testIsFocused() {}

    public void testIsFocusable() {}

    public void testIsScrollable() {}

    public void testIsLongClickable() {}

    public void testGetPackageName() {}

    public void testGetVisibleBounds() {}

    public void testGetBounds() {}

    public void testWaitForExists() {}

    public void testWaitUntilGone() {}

    public void testExists() {}

    public void testPinchOut() {}

    public void testPinchIn() {}

    public void testPerformTwoPointerGesture() {}

    public void testPerformMultiPointerGesture() {}
    */
}
