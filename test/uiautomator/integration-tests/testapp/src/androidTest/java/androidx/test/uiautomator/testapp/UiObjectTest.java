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

import android.graphics.Point;
import android.graphics.Rect;

import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.UiObject;
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

        assertUiObjectNotFound(noNode::getChildCount);
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

        // Note that the `swipeRegion` will always show the swipe direction, even if the swipe
        // action does not happen inside `swipeRegion`.
        assertFalse(verySmallRegion.swipeUp(10));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeUp(10));
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

        assertFalse(verySmallRegion.swipeDown(10));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeDown(10));
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

        assertFalse(verySmallRegion.swipeLeft(10));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeLeft(10));
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

        assertFalse(verySmallRegion.swipeRight(10));
        assertEquals("no_swipe", swipeRegion.getText());
        assertTrue(swipeRegion.swipeRight(10));
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

        assertUiObjectNotFound(noNode::click);
        assertUiObjectNotFound(noNode::clickAndWaitForNewWindow);
        assertUiObjectNotFound(noNode::clickTopLeft);
        assertUiObjectNotFound(noNode::longClickBottomRight);
        assertUiObjectNotFound(noNode::clickBottomRight);
        assertUiObjectNotFound(noNode::longClick);
        assertUiObjectNotFound(noNode::longClickTopLeft);
    }

    @Test
    public void testGetText() throws Exception {
        launchTestActivity(MainActivity.class);

        UiObject sampleTextObject = mDevice.findObject(new UiSelector().text("Sample text"));
        UiObject nullTextObject = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/nested_elements"));

        assertEquals("Sample text", sampleTextObject.getText());
        assertEquals("", nullTextObject.getText());
    }

    @Test
    public void testGetClassName() throws Exception {
        launchTestActivity(MainActivity.class);

        UiObject button = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/button"));
        UiObject textView = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/example_id"));

        assertEquals("android.widget.Button", button.getClassName());
        assertEquals("android.widget.TextView", textView.getClassName());
    }

    @Test
    public void testGetContentDescription() throws Exception {
        launchTestActivity(MainActivity.class);

        UiObject button = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/button"));
        UiObject textView = mDevice.findObject(new UiSelector().text("Text View 1"));

        assertEquals("I'm accessible!", button.getContentDescription());
        assertEquals("", textView.getContentDescription());
    }

    @Test
    public void testLegacySetText() throws Exception {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject editText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/edit_text"));

        assertEquals("sample_text", editText.getText());
        editText.legacySetText("new_text");
        assertEquals("new_text", editText.getText());
    }

    @Test
    public void testSetText() throws Exception {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject editText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/edit_text"));

        assertEquals("sample_text", editText.getText());
        editText.setText("new_text");
        assertEquals("new_text", editText.getText());
    }

    @Test
    public void testClearTextField() throws Exception {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject editText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/edit_text"));

        assertEquals("sample_text", editText.getText());
        editText.clearTextField();
        assertEquals("", editText.getText());
    }

    @Test
    public void testTextFamily_throwsUiObjectNotFoundException() {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject noNode = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"));

        assertUiObjectNotFound(noNode::getText);
        assertUiObjectNotFound(noNode::getClassName);
        assertUiObjectNotFound(noNode::getContentDescription);
        assertUiObjectNotFound(() -> noNode.legacySetText("new_text"));
        assertUiObjectNotFound(() -> noNode.setText("new_text"));
        assertUiObjectNotFound(noNode::clearTextField);
        assertUiObjectNotFound(noNode::getPackageName);
    }

    @Test
    public void testIsChecked() throws Exception {
        launchTestActivity(ClickTestActivity.class);

        UiObject checkBox = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box"));

        assertFalse(checkBox.isChecked());
        checkBox.click();
        assertTrue(checkBox.isChecked());
    }

    @Test
    public void testIsSelected() throws Exception {
        launchTestActivity(IsSelectedTestActivity.class);

        UiObject selectedButton = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/selected_button"));
        UiObject selectedTarget = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/selected_target"));

        selectedButton.click();
        assertTrue(selectedTarget.isSelected());
    }

    @Test
    public void testIsCheckable() throws Exception {
        launchTestActivity(ClickTestActivity.class);

        UiObject checkBox = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box"));
        UiObject button1 = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button1"));

        assertTrue(checkBox.isCheckable());
        assertFalse(button1.isCheckable());
    }

    @Test
    public void testIsEnabled() throws Exception {
        launchTestActivity(IsEnabledTestActivity.class);

        UiObject disabledObject = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/disabled_text_view"));
        UiObject enabledObject = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/enabled_text_view"));

        assertFalse(disabledObject.isEnabled());
        assertTrue(enabledObject.isEnabled());
    }

    @Test
    public void testIsClickable() throws Exception {
        launchTestActivity(MainActivity.class);

        UiObject textView = mDevice.findObject(new UiSelector().text("Sample text"));
        UiObject button = mDevice.findObject(new UiSelector().text("Accessible button"));

        assertFalse(textView.isClickable());
        assertTrue(button.isClickable());
    }

    @Test
    public void testIsFocused() throws Exception {
        launchTestActivity(IsFocusedTestActivity.class);

        UiObject textView = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/focusable_text_view"));
        UiObject button = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/button"));

        assertFalse(textView.isFocused());
        button.click();
        assertTrue(textView.isFocused());
    }

    @Test
    public void testIsFocusable() throws Exception {
        launchTestActivity(IsFocusedTestActivity.class);

        UiObject nonFocusableTextView =
                mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                        + "/non_focusable_text_view"));
        UiObject focusableTextView = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/focusable_text_view"));

        assertFalse(nonFocusableTextView.isFocusable());
        assertTrue(focusableTextView.isFocusable());
    }

    @Test
    public void testIsScrollable() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiObject scrollView = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/scroll_view"));
        UiObject textView = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/top_text"));

        assertTrue(scrollView.isScrollable());
        assertFalse(textView.isScrollable());
    }

    @Test
    public void testIsLongClickable() throws Exception {
        launchTestActivity(IsLongClickableTestActivity.class);

        UiObject longClickableButton = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/long_clickable_button"));
        UiObject nonLongClickableButton = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/non_long_clickable_button"));

        assertTrue(longClickableButton.isLongClickable());
        assertFalse(nonLongClickableButton.isLongClickable());
    }

    @Test
    public void testAttributeCheckingMethods_throwsUiObjectNotFoundException() {
        launchTestActivity(ClickTestActivity.class);

        UiObject noNode = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"));

        assertUiObjectNotFound(noNode::isChecked);
        assertUiObjectNotFound(noNode::isSelected);
        assertUiObjectNotFound(noNode::isCheckable);
        assertUiObjectNotFound(noNode::isEnabled);
        assertUiObjectNotFound(noNode::isClickable);
        assertUiObjectNotFound(noNode::isFocused);
        assertUiObjectNotFound(noNode::isFocusable);
        assertUiObjectNotFound(noNode::isScrollable);
        assertUiObjectNotFound(noNode::isLongClickable);
    }

    @Test
    public void testGetPackageName() throws Exception {
        launchTestActivity(MainActivity.class);

        UiObject sampleTextObject = mDevice.findObject(new UiSelector().text("Sample text"));

        assertEquals(TEST_APP, sampleTextObject.getPackageName());
    }

    @Test
    public void testGetVisibleBounds() throws Exception {
        launchTestActivity(VisibleBoundsTestActivity.class);

        UiObject partlyInvisibleRegion =
                mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                        + "/partly_invisible_region"));
        UiObject regionInsideScrollable =
                mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                        + "/region_inside_scrollable"));

        partlyInvisibleRegion.click();
        regionInsideScrollable.click();
        assertEquals(partlyInvisibleRegion.getText(),
                partlyInvisibleRegion.getVisibleBounds().toString());
        assertEquals(regionInsideScrollable.getText(),
                regionInsideScrollable.getVisibleBounds().toString());
    }

    @Test
    @SdkSuppress(minSdkVersion = 23) // Bounds include invisible regions prior to API 23.
    public void testGetBounds() throws Exception {
        launchTestActivity(VisibleBoundsTestActivity.class);

        UiObject partlyInvisibleRegion =
                mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                        + "/partly_invisible_region"));

        partlyInvisibleRegion.click();
        assertEquals(partlyInvisibleRegion.getText(),
                partlyInvisibleRegion.getBounds().toString());
    }

    @Test
    public void testWaitForExists() throws Exception {
        launchTestActivity(WaitTestActivity.class);

        UiObject text1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/text_1"));
        assertEquals("text_1_not_changed", text1.getText());

        UiObject expectedText1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/text_1").text("text_1_changed"));

        text1.click();
        assertTrue(expectedText1.waitForExists(TIMEOUT_MS));
    }

    @Test
    public void testWaitForExist_timeout() throws Exception {
        launchTestActivity(WaitTestActivity.class);

        UiObject text1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/text_1"));
        assertEquals("text_1_not_changed", text1.getText());

        UiObject expectedText1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/text_1").text("text_1_changed"));

        assertFalse(expectedText1.waitForExists(1_000));
    }

    @Test
    public void testWaitUntilGone() throws Exception {
        launchTestActivity(WaitTestActivity.class);

        UiObject expectedText1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/text_1").text("text_1_not_changed"));
        assertTrue(expectedText1.exists());
        expectedText1.click();
        assertTrue(expectedText1.waitUntilGone(TIMEOUT_MS));
    }

    @Test
    public void testWaitUntilGone_timeout() {
        launchTestActivity(WaitTestActivity.class);

        UiObject expectedText1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/text_1").text("text_1_not_changed"));
        assertTrue(expectedText1.exists());
        assertFalse(expectedText1.waitUntilGone(1_000));
    }

    @Test
    public void testExists() {
        launchTestActivity(WaitTestActivity.class);

        UiObject text1 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/text_1"));
        UiObject text3 = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/text_3"));

        assertTrue(text1.exists());
        assertFalse(text3.exists());
    }

    @Test
    public void testPinchOut() throws Exception {
        launchTestActivity(PinchTestActivity.class);

        UiObject pinchArea = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/pinch_area"));
        UiObject scaleText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/scale_factor"));

        UiObject expectedScaleText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/scale_factor").text("1.0f"));

        assertTrue(pinchArea.pinchOut(0, 10));
        assertFalse(expectedScaleText.waitUntilGone(TIMEOUT_MS));
        assertTrue(pinchArea.pinchOut(100, 10));
        assertTrue(expectedScaleText.waitUntilGone(TIMEOUT_MS));
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format(
                "Expected scale text to be greater than 1f after pinchOut(), but got [%f]",
                scaleValueAfterPinch), scaleValueAfterPinch > 1f);
    }

    @Test
    public void testPinchIn() throws Exception {
        launchTestActivity(PinchTestActivity.class);

        UiObject pinchArea = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/pinch_area"));
        UiObject scaleText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/scale_factor"));

        UiObject expectedScaleText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/scale_factor").text("1.0f"));

        assertTrue(pinchArea.pinchIn(0, 10));
        assertFalse(expectedScaleText.waitUntilGone(TIMEOUT_MS));
        assertTrue(pinchArea.pinchIn(100, 10));
        assertTrue(expectedScaleText.waitUntilGone(TIMEOUT_MS));
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchIn(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testPinchFamily_throwsExceptions() {
        launchTestActivity(PinchTestActivity.class);

        UiObject noNode = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"));
        UiObject smallArea = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/small_area"));

        assertUiObjectNotFound(() -> noNode.pinchOut(100, 10));
        assertUiObjectNotFound(() -> noNode.pinchIn(100, 10));
        assertThrows(IllegalStateException.class, () -> smallArea.pinchOut(100, 10));
        assertThrows(IllegalStateException.class, () -> smallArea.pinchIn(100, 10));
        assertThrows(IllegalArgumentException.class, () -> smallArea.pinchOut(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> smallArea.pinchOut(101, 10));
        assertThrows(IllegalArgumentException.class, () -> smallArea.pinchIn(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> smallArea.pinchIn(101, 10));
    }

    @Test
    public void testPerformTwoPointerGesture_withZeroSteps() throws Exception {
        // Note that most part of `performTwoPointerGesture` (and `performMultiPointerGesture`)
        // has already been indirectly tested in other tests. This test only test the case when
        // the `step` parameter is set to zero.
        launchTestActivity(PointerGestureTestActivity.class);

        UiObject touchRegion = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/touch_region"));

        Rect visibleBounds = touchRegion.getVisibleBounds();
        Point startPoint1 = new Point(visibleBounds.left + 50, visibleBounds.top + 50);
        Point startPoint2 = new Point(visibleBounds.right - 50, visibleBounds.top + 50);
        Point endPoint1 = new Point(visibleBounds.left + 50, visibleBounds.bottom - 50);
        Point endPoint2 = new Point(visibleBounds.right - 50, visibleBounds.bottom - 50);

        assertTrue(touchRegion.performTwoPointerGesture(startPoint1, startPoint2, endPoint1,
                endPoint2, 0));
        assertEquals("2 touch(es) received", touchRegion.getText());
    }
}
