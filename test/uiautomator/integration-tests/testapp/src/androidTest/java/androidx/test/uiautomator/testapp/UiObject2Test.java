/*
 * Copyright (C) 2014 The Android Open Source Project
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.EventCondition;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UiObject2Test extends BaseTest {
    private static final int TIMEOUT_MS = 10_000;
    private static final int SPEED_MS = 100;

    @Test
    public void testClear() {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        // Verify the text field has text before clear()
        assertEquals("sample_text", object.getText());
        object.clear();
        // Verify the text field does not have txt after clear()
        BySelector emptyText = By.res(TEST_APP, "edit_text").text("");
        assertTrue(mDevice.wait(Until.hasObject(emptyText), TIMEOUT_MS));
    }

    @Test
    public void testClick() {
        launchTestActivity(ClickTestActivity.class);

        // Short click with no parameter (`click()`).
        UiObject2 button1 = mDevice.findObject(By.res(TEST_APP, "button1"));
        assertEquals("text1", button1.getText());
        button1.click();
        button1.wait(Until.textEquals("text1_clicked"), TIMEOUT_MS);
        assertEquals("text1_clicked", button1.getText());
    }

    @Test
    public void testClick_point() {
        launchTestActivity(ClickTestActivity.class);

        // Short click with a point position as a parameter (`click(Point point)`).

        // Point inside the button.
        UiObject2 button2 = mDevice.findObject(By.res(TEST_APP, "button2"));
        assertEquals("text2", button2.getText());
        button2.click(getPointInsideBounds(button2));
        button2.wait(Until.textEquals("text2_clicked"), TIMEOUT_MS);
        assertEquals("text2_clicked", button2.getText());

        // Point outside the button.
        UiObject2 button3 = mDevice.findObject(By.res(TEST_APP, "button3"));
        assertEquals("text3", button3.getText());
        button3.click(getPointOutsideBounds(button3));
        button3.wait(Until.textEquals("text3_clicked"), TIMEOUT_MS);
        assertEquals("text3_clicked", button3.getText());
    }

    @Test
    public void testClick_duration() {
        launchTestActivity(ClickTestActivity.class);

        // Short click with a time duration as a parameter (`click(long duration)`).
        UiObject2 button4 = mDevice.findObject(By.res(TEST_APP, "button4"));
        assertEquals("text4", button4.getText());
        button4.click(50L);
        button4.wait(Until.textEquals("text4_clicked"), TIMEOUT_MS);
        assertEquals("text4_clicked", button4.getText());

        // Long click with a time duration as a parameter (`click(long duration)`).
        UiObject2 button5 = mDevice.findObject(By.res(TEST_APP, "button5"));
        assertEquals("text5", button5.getText());
        button5.click((long) (ViewConfiguration.getLongPressTimeout() * 1.5));
        button5.wait(Until.textEquals("text5_long_clicked"), TIMEOUT_MS);
        assertEquals("text5_long_clicked", button5.getText());
    }

    @Test
    public void testClick_pointAndDuration() {
        launchTestActivity(ClickTestActivity.class);

        // Short click with two parameters (`click(Point point, long duration)`).
        UiObject2 button6 = mDevice.findObject(By.res(TEST_APP, "button6"));
        assertEquals("text6", button6.getText());
        button6.click(getPointInsideBounds(button6), 50L);
        button6.wait(Until.textEquals("text6_clicked"), TIMEOUT_MS);
        assertEquals("text6_clicked", button6.getText());

        // Long click with two parameters (`click(Point point, long duration)`).
        UiObject2 button7 = mDevice.findObject(By.res(TEST_APP, "button7"));
        assertEquals("text7", button7.getText());
        button7.click(getPointInsideBounds(button7),
                (long) (ViewConfiguration.getLongPressTimeout() * 1.5));
        button7.wait(Until.textEquals("text7_long_clicked"), TIMEOUT_MS);
        assertEquals("text7_long_clicked", button7.getText());
    }

    @Test
    public void testClickAndWait_conditionAndTimeout() {
        launchTestActivity(ClickAndWaitTestActivity.class);

        // Click the button and wait for a new window
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        assertTrue(button.clickAndWait(Until.newWindow(), TIMEOUT_MS));
    }

    @Test
    public void testClickAndWait_pointAndConditionAndTimeout() {
        launchTestActivity(ClickAndWaitTestActivity.class);

        // Click point inside the button.
        UiObject2 button1 = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        assertTrue(button1.clickAndWait(getPointInsideBounds(button1), Until.newWindow(),
                TIMEOUT_MS));

        // Click point outside the button.
        UiObject2 button2 = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        assertTrue(button2.clickAndWait(getPointOutsideBounds(button2), Until.newWindow(),
                TIMEOUT_MS));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDrag_dest() {
        launchTestActivity(DragTestActivity.class);

        UiObject2 dragButton = mDevice.findObject(By.res(TEST_APP, "drag_button"));
        UiObject2 dragDestination = mDevice.findObject(By.res(TEST_APP, "drag_destination"));
        Point dest = dragDestination.getVisibleCenter();
        assertEquals("no_drag_yet", dragDestination.getText());
        dragButton.drag(dest);
        dragDestination.wait(Until.textEquals("drag_received"), TIMEOUT_MS);
        assertEquals("drag_received", dragDestination.getText());
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDrag_destAndSpeed() {
        launchTestActivity(DragTestActivity.class);

        UiObject2 dragButton = mDevice.findObject(By.res(TEST_APP, "drag_button"));
        UiObject2 dragDestination = mDevice.findObject(By.res(TEST_APP, "drag_destination"));
        Point dest = dragDestination.getVisibleCenter();
        assertEquals("no_drag_yet", dragDestination.getText());
        dragButton.drag(dest, 1000);
        dragDestination.wait(Until.textEquals("drag_received"), TIMEOUT_MS);
        assertEquals("drag_received", dragDestination.getText());
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testDrag_destAndSpeed_throwsIllegalArgumentException() {
        launchTestActivity(DragTestActivity.class);

        UiObject2 dragButton = mDevice.findObject(By.res(TEST_APP, "drag_button"));
        UiObject2 dragDestination = mDevice.findObject(By.res(TEST_APP, "drag_destination"));
        Point dest = dragDestination.getVisibleCenter();
        assertEquals("no_drag_yet", dragDestination.getText());
        assertThrows("Speed cannot be negative", IllegalArgumentException.class,
                () -> dragButton.drag(dest, -1000));
    }

    @Test
    public void testEquals() {
        launchTestActivity(MainActivity.class);

        // Get the same textView object via different methods.
        UiObject2 textView1 = mDevice.findObject(By.res(TEST_APP, "example_id"));
        UiObject2 textView2 = mDevice.findObject(By.text("TextView with an id"));
        assertEquals(textView1, textView2);
        UiObject2 linearLayout = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertNotEquals(textView1, linearLayout);
    }

    @Test
    public void testFindObject() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertNotNull(object.findObject(By.res(TEST_APP, "example_id")));
        assertNull(object.findObject(By.res(TEST_APP, "")));
    }

    @Test
    public void testFindObjects() {
        launchTestActivity(MainActivity.class);

        UiObject2 nestedObject = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        List<UiObject2> listWithMultipleObjectsFound = nestedObject.findObjects(By.res(""));
        assertEquals(5, listWithMultipleObjectsFound.size());

        List<UiObject2> listWithOneObjectFound = nestedObject.findObjects(By.text("First Level"));
        assertEquals(1, listWithOneObjectFound.size());
        assertEquals("First Level", listWithOneObjectFound.get(0).getText());

        List<UiObject2> listWithNoObjectsFound = nestedObject.findObjects(By.res(TEST_APP,
                "button"));
        assertTrue(listWithNoObjectsFound.isEmpty());
    }

    @Test
    public void testGetApplicationPackage() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.pkg(TEST_APP));
        assertEquals(TEST_APP, object.getApplicationPackage());
    }

    @Test
    public void testGetChildCount() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertEquals(0, object.getChildCount());

        UiObject2 nestedObject = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertEquals(2, nestedObject.getChildCount());
    }

    @Test
    public void testGetChildren() {
        launchTestActivity(MainActivity.class);

        UiObject2 nestedObject = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        List<UiObject2> children = nestedObject.getChildren();
        assertEquals(2, children.size());
        Set<String> childrenClassNames = new HashSet<>();
        childrenClassNames.add(children.get(0).getClassName());
        childrenClassNames.add(children.get(1).getClassName());
        assertTrue(childrenClassNames.contains("android.widget.TextView"));
        assertTrue(childrenClassNames.contains("android.widget.LinearLayout"));

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertTrue(object.getChildren().isEmpty());
    }

    @Test
    public void testGetClassName() {
        launchTestActivity(MainActivity.class);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("android.widget.Button", button.getClassName());

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertEquals("android.widget.TextView", textView.getClassName());
    }

    @Test
    public void testGetContentDescription() {
        launchTestActivity(MainActivity.class);

        UiObject2 buttonObject = mDevice.findObject(By.text("Accessible button"));
        assertEquals("I'm accessible!", buttonObject.getContentDescription());

        UiObject2 textViewObject = mDevice.findObject(By.text("Text View 1"));
        assertNull(textViewObject.getContentDescription());
    }

    @Test
    public void testGetParent() {
        launchTestActivity(MainActivity.class);

        UiObject2 objectAtDepth1 = mDevice.findObject(By.depth(1));
        UiObject2 objectAtDepth0 = objectAtDepth1.getParent();
        assertNotNull(objectAtDepth0);
        assertNull(objectAtDepth0.getParent());
    }

    @Test
    public void testGetResourceName() {
        launchTestActivity(MainActivity.class);

        UiObject2 textViewWithAnId = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertEquals("androidx.test.uiautomator.testapp:id/example_id",
                textViewWithAnId.getResourceName());

        UiObject2 textViewObjectWithoutAnId = mDevice.findObject(By.text("Text View 1"));
        assertNull(textViewObjectWithoutAnId.getResourceName());
    }

    @Test
    public void testGetText() {
        launchTestActivity(MainActivity.class);

        UiObject2 sampleTextObject = mDevice.findObject(By.text("Sample text"));
        assertEquals("Sample text", sampleTextObject.getText());

        UiObject2 nullTextObject = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertNull(nullTextObject.getText());
    }

    @Test
    public void testGetVisibleBounds() {
        launchTestActivity(VisibleBoundsTestActivity.class);

        UiObject2 partlyInvisibleRegion = mDevice.findObject(
                By.res(TEST_APP, "partly_invisible_region"));
        UiObject2 regionInsideScrollable = mDevice.findObject(
                By.res(TEST_APP, "region_inside_scrollable"));

        partlyInvisibleRegion.click();
        regionInsideScrollable.click();
        assertEquals(partlyInvisibleRegion.getText(),
                partlyInvisibleRegion.getVisibleBounds().toString());
        assertEquals(regionInsideScrollable.getText(),
                regionInsideScrollable.getVisibleBounds().toString());
    }

    @Test
    public void testGetVisibleCenter() {
        launchTestActivity(VisibleBoundsTestActivity.class);

        UiObject2 partlyInvisibleRegion = mDevice.findObject(
                By.res(TEST_APP, "partly_invisible_region"));

        partlyInvisibleRegion.click((long) (ViewConfiguration.getLongPressTimeout() * 1.5));
        assertEquals(partlyInvisibleRegion.getText(),
                partlyInvisibleRegion.getVisibleCenter().toString());
    }

    @Test
    public void testHashCode() {
        launchTestActivity(MainActivity.class);

        // Get the same textView object via different methods.
        // The same object should have the same hash code.
        UiObject2 textView1 = mDevice.findObject(By.res(TEST_APP, "example_id"));
        UiObject2 textView2 = mDevice.findObject(By.text("TextView with an id"));
        assertEquals(textView1.hashCode(), textView2.hashCode());

        // Different objects should have different hash codes.
        UiObject2 linearLayout = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertNotEquals(textView1.hashCode(), linearLayout.hashCode());
    }

    @Test
    public void testHasObject() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "example_id"));

        assertTrue(object.hasObject(By.text("TextView with an id")));
        assertFalse(object.hasObject(By.text("")));
    }

    @Test
    public void testIsCheckable() {
        launchTestActivity(ClickTestActivity.class);

        // CheckBox objects are checkable by default.
        UiObject2 checkBox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertTrue(checkBox.isCheckable());
        // Button objects are not checkable by default.
        UiObject2 button1 = mDevice.findObject(By.res(TEST_APP, "button1"));
        assertFalse(button1.isCheckable());
    }

    @Test
    public void testIsChecked() {
        launchTestActivity(ClickTestActivity.class);

        UiObject2 checkBox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertFalse(checkBox.isChecked());
        checkBox.click();
        checkBox.wait(Until.checked(true), TIMEOUT_MS);
        assertTrue(checkBox.isChecked());
    }

    @Test
    public void testIsClickable() {
        launchTestActivity(MainActivity.class);

        // TextView objects are not clickable by default.
        UiObject2 textView = mDevice.findObject(By.text("Sample text"));
        assertFalse(textView.isClickable());
        // Button objects are clickable by default.
        UiObject2 button = mDevice.findObject(By.text("Accessible button"));
        assertTrue(button.isClickable());
    }

    @Test
    public void testIsEnabled() {
        launchTestActivity(IsEnabledTestActivity.class);

        UiObject2 disabledObject = mDevice.findObject(By.res(TEST_APP, "disabled_text_view"));
        assertFalse(disabledObject.isEnabled());
        UiObject2 enabledObject = mDevice.findObject(By.res(TEST_APP, "enabled_text_view"));
        assertTrue(enabledObject.isEnabled());
    }

    @Test
    public void testIsFocusable() {
        launchTestActivity(IsFocusedTestActivity.class);

        UiObject2 nonFocusableTextView = mDevice.findObject(By.res(TEST_APP,
                "non_focusable_text_view"));
        assertFalse(nonFocusableTextView.isFocusable());
        UiObject2 focusableTextView = mDevice.findObject(By.res(TEST_APP, "focusable_text_view"));
        assertTrue(focusableTextView.isFocusable());
    }

    @Test
    public void testIsFocused() {
        launchTestActivity(IsFocusedTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "focusable_text_view"));
        assertFalse(textView.isFocused());
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        button.click();
        textView.wait(Until.focused(true), TIMEOUT_MS);
        assertTrue(textView.isFocused());
    }

    @Test
    public void testIsLongClickable() {
        launchTestActivity(IsLongClickableTestActivity.class);

        UiObject2 longClickableButton = mDevice.findObject(By.res(TEST_APP,
                "long_clickable_button"));
        assertTrue(longClickableButton.isLongClickable());
        UiObject2 nonLongClickableButton = mDevice.findObject(By.res(TEST_APP,
                "non_long_clickable_button"));
        assertFalse(nonLongClickableButton.isLongClickable());
    }

    @Test
    public void testIsScrollable() {
        launchTestActivity(VerticalScrollTestActivity.class);

        // ScrollView objects are scrollable by default.
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        assertTrue(scrollView.isScrollable());
        // TextView objects are not scrollable by default.
        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "top_text"));
        assertFalse(textView.isScrollable());
    }

    @Test
    public void testIsSelected() {
        launchTestActivity(IsSelectedTestActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "selected_target"));
        assertFalse(textView.isSelected());
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "selected_button"));
        button.click();
        textView.wait(Until.selected(true), TIMEOUT_MS);
        assertTrue(textView.isSelected());
    }

    @Test
    public void testLongClick() {
        launchTestActivity(LongClickTestActivity.class);

        // Find the button and verify its initial state
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("Long Click Me!", button.getText());

        // Click on the button and verify that the text has changed
        button.longClick();
        assertTrue(button.wait(Until.textEquals("I've been long clicked!"), TIMEOUT_MS));
    }

    @Test
    public void testPinchClose() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(0.75f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testPinchClose_withSpeed() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(.75f, SPEED_MS);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testPinchOpen() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchOpen(.5f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale text to be greater than 1f after pinchOpen(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch > 1f);
    }

    @Test
    public void testPinchOpen_withSpeed() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchOpen(.25f, SPEED_MS);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale text to be greater than 1f after pinchOpen(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch > 1f);
    }

    @Test
    public void testSwipe() {
        launchTestActivity(SwipeTestActivity.class);

        UiObject2 swipeRegion = mDevice.findObject(By.res(TEST_APP, "swipe_region"));

        swipeRegion.swipe(Direction.LEFT, 0.9f);
        assertTrue(swipeRegion.wait(Until.textEquals("swipe_left"), TIMEOUT_MS));

        swipeRegion.swipe(Direction.RIGHT, 1.0f);
        assertTrue(swipeRegion.wait(Until.textEquals("swipe_right"), TIMEOUT_MS));

        swipeRegion.swipe(Direction.UP, 0.9f, 1000);
        assertTrue(swipeRegion.wait(Until.textEquals("swipe_up"), TIMEOUT_MS));

        swipeRegion.swipe(Direction.DOWN, 1.0f, 1000);
        assertTrue(swipeRegion.wait(Until.textEquals("swipe_down"), TIMEOUT_MS));
    }

    @Test
    public void testSwipe_throwsIllegalArgumentException() {
        launchTestActivity(SwipeTestActivity.class);

        UiObject2 swipeRegion = mDevice.findObject(By.res(TEST_APP, "swipe_region"));

        assertThrows("Percent must be between 0.0f and 1.0f", IllegalArgumentException.class,
                () -> swipeRegion.swipe(Direction.UP, 10.0f));
        assertThrows("Percent must be between 0.0f and 1.0f", IllegalArgumentException.class,
                () -> swipeRegion.swipe(Direction.UP, -10.0f));
        assertThrows("Speed cannot be negative", IllegalArgumentException.class,
                () -> swipeRegion.swipe(Direction.UP, 1.0f, -10));
    }

    @Test
    public void testRecycle() {
        launchTestActivity(MainActivity.class);

        UiObject2 textView = mDevice.findObject(By.text("Sample text"));
        textView.recycle();
        // Attributes of a recycled object cannot be accessed.
        IllegalStateException e = assertThrows(
                "Expected testView.getText() to throw IllegalStateException, but it didn't.",
                IllegalStateException.class,
                () -> textView.getText()
        );
        assertEquals("This object has already been recycled.", e.getMessage());
    }

    @Test
    public void testScroll() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.

        // Scroll down to bottom where is two-screen-height distant from the top.
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));

        Rect bounds = scrollView.getVisibleBounds();
        float percent =
                (float) (mDevice.getDisplayHeight() * 2 / (bounds.height() - 100));
        scrollView.scroll(Direction.DOWN, percent);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testScroll_untilEnd() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.

        // Scroll until end (scroll method returns false).
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        while (scrollView.scroll(Direction.DOWN, 1.0f)) {
            // Continue until bottom.
        }
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testScrollUntil_conditionSatisfied() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));

        // Scroll until end
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        assertNotNull(scrollView.scrollUntil(Direction.DOWN,
                Until.findObject(By.res(TEST_APP, "bottom_text"))));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testScrollUntil_conditionNotSatisfied() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));

        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        // fail to find text that doesn't exist.
        assertNull(scrollView.scrollUntil(Direction.DOWN,
                Until.findObject(By.res(TEST_APP, "nonexistent_text"))));
        // We still scroll to the end.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testScrollUntil_eventConditionSatisfied() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));

        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        // Scroll for the event condition that occurs early before scrolling to the end.
        Integer result = scrollView.scrollUntil(Direction.DOWN,
                new EventCondition<Integer>() {
                    private Integer mResult = null;
                    @Override
                    public Integer getResult() {
                        return mResult;
                    }

                    @Override
                    public boolean accept(AccessibilityEvent event) {
                        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                            mResult = event.getEventType();
                            return true;
                        }
                        return false;
                    }
                });
        assertEquals(result, (Integer) AccessibilityEvent.TYPE_VIEW_SCROLLED);
        // We haven't scrolled to the end.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testScrollUntil_eventConditionNotSatisfied() {
        launchTestActivity(VerticalScrollTestActivity.class);
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "top_text"))); // Initially at top.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));

        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        // Scroll for the event condition that doesn't occur.
        Integer result = scrollView.scrollUntil(Direction.DOWN,
                new EventCondition<Integer>() {
                    private Integer mResult = null;
                    @Override
                    public Integer getResult() {
                        return mResult;
                    }

                    @Override
                    public boolean accept(AccessibilityEvent event) {
                        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                            mResult = event.getEventType();
                            return true;
                        }
                        return false;
                    }
                });
        assertNull(result);
        // We still scroll to the end when event condition never occurs.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "bottom_text")));
    }

    @Ignore // b/266617335
    @Test
    public void testFling_direction() {
        launchTestActivity(FlingTestActivity.class);

        UiObject2 flingRegion = mDevice.findObject(By.res(TEST_APP, "fling_region"));

        // No fling yet.
        assertEquals("no_fling", flingRegion.getText());

        while (flingRegion.fling(Direction.LEFT)) {
            // Continue until left bound.
        }
        assertTrue(flingRegion.wait(Until.textEquals("fling_left"), TIMEOUT_MS));
    }

    @Test
    public void testFling_directionAndSpeed() {
        launchTestActivity(FlingTestActivity.class);

        UiObject2 flingRegion = mDevice.findObject(By.res(TEST_APP, "fling_region"));

        // No fling yet.
        assertEquals("no_fling", flingRegion.getText());

        while (flingRegion.fling(Direction.UP, 5000)) {
            // Continue until up bound.
        }
        assertTrue(flingRegion.wait(Until.textEquals("fling_up"), TIMEOUT_MS));
    }

    @Test
    public void testFling_throwsIllegalArgumentException() {
        launchTestActivity(FlingTestActivity.class);

        UiObject2 flingRegion = mDevice.findObject(By.res(TEST_APP, "fling_region"));

        int speed =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledMinimumFlingVelocity()
                        / 2;

        assertThrows("Speed is less than the minimum fling velocity",
                IllegalArgumentException.class,
                () -> flingRegion.fling(Direction.DOWN, speed));
    }

    @Ignore // b/267208902
    @Test
    public void testSetGestureMargin() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));

        Rect pinchAreaBounds = pinchArea.getVisibleBounds();

        // Set the gesture's margins to a large number (greater than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds cannot form a rectangle and no action can be performed.
        pinchArea.setGestureMargin(Math.max(pinchAreaBounds.height(), pinchAreaBounds.width()) * 2);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertEquals(String.format("Expected scale value to be equal to 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), 1f, scaleValueAfterPinch, 0f);

        // Set the gesture's margins to a small number (smaller than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds form a rectangle and action can be performed.
        pinchArea.setGestureMargin(1);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Ignore // b/266617335
    @Test
    public void testSetGestureMargins() {
        launchTestActivity(PinchTestActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));

        Rect pinchAreaBounds = pinchArea.getVisibleBounds();

        // Set the gesture's margins to large numbers (greater than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds cannot form a rectangle and no action can be performed.
        pinchArea.setGestureMargins(1, 1, pinchAreaBounds.width() * 2,
                pinchAreaBounds.height() * 2);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertEquals(String.format("Expected scale value to be equal to 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), 1f, scaleValueAfterPinch, 0f);

        // Set the gesture's margins to small numbers (smaller than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds form a rectangle and action can be performed.
        pinchArea.setGestureMargins(1, 1, 1, 1);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        scaleValueAfterPinch = Float.parseFloat(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testSetText() {
        launchTestActivity(ClearTextTestActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        // Verify the text field has "sample_text" before setText()
        assertEquals("sample_text", object.getText());
        object.setText("new_text");
        // Verify the text field has "new_text" after setText()
        BySelector updatedText = By.res(TEST_APP, "edit_text").text("new_text");
        assertTrue(mDevice.wait(Until.hasObject(updatedText), TIMEOUT_MS));
    }

    /* Helper method to get a point inside the object. */
    private Point getPointInsideBounds(UiObject2 obj) {
        Rect objBounds = obj.getVisibleBounds();
        int pointX = objBounds.left + objBounds.width() / 3;
        int pointY = objBounds.top + objBounds.height() / 3;
        return new Point(pointX, pointY);
    }

    /* Helper method to get a point outside the object. */
    private Point getPointOutsideBounds(UiObject2 obj) {
        Rect objBounds = obj.getVisibleBounds();
        int pointX = objBounds.right + objBounds.width() / 3;
        int pointY = objBounds.bottom + objBounds.height() / 3;
        return new Point(pointX, pointY);
    }
}
