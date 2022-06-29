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

import androidx.test.filters.FlakyTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

import java.util.List;

public class UiObject2Tests extends BaseTest {
    private static final int TIMEOUT_MS = 10_000;
    private static final int SPEED_MS = 100;

    @Test
    public void testClear() {
        launchTestActivity(UiObject2TestClearTextActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        // Verify the text field has text before clear()
        assertEquals("sample_text", object.getText());
        object.clear();
        // Verify the text field does not have txt after clear()
        assertNull(object.getText());
    }

    @Test
    public void testClick() {
        launchTestActivity(UiObject2TestClickActivity.class);

        // Find the button and verify its initial state
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("Click Me!", button.getText());

        // Click on the button and verify that the text has changed
        button.click();
        button.wait(Until.textEquals("I've been clicked!"), TIMEOUT_MS);
        assertEquals("I've been clicked!", button.getText());

        // Find the checkbox and verify its initial state
        UiObject2 checkbox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertFalse(checkbox.isChecked());

        // Click on the checkbox and verify that it is now checked
        checkbox.click();
        checkbox.wait(Until.checked(true), TIMEOUT_MS);
        assertTrue(checkbox.isChecked());
    }

    @Test
    public void testClickAndWait() {
        launchTestActivity(UiObject2TestClickAndWaitActivity.class);

        // Click the button and wait for a new window
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        assertTrue(button.clickAndWait(Until.newWindow(), TIMEOUT_MS));
    }

    @Test
    public void testEquals() {
        launchTestActivity(MainActivity.class);

        // Get the same textView object via different methods.
        UiObject2 textView1 = mDevice.findObject(By.res(TEST_APP, "example_id"));
        UiObject2 textView2 = mDevice.findObject(By.text("TextView with an id"));
        assertTrue(textView1.equals(textView2));
        UiObject2 linearLayout = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertFalse(textView1.equals(linearLayout));
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
        UiObject2 object1 = children.get(0);
        assertEquals("android.widget.TextView", object1.getClassName());
        UiObject2 object2 = children.get(1);
        assertEquals("android.widget.LinearLayout", object2.getClassName());

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "example_id"));
        assertTrue(object.getChildren().isEmpty());
    }

    @Test
    public void testGetClassName() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("android.widget.Button", button.getClassName());

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "text_view"));
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

        UiObject2 object = mDevice.findObject(By.text("Sample text"));
        assertEquals("Sample text", object.getText());

        UiObject2 nestedObject = mDevice.findObject(By.res(TEST_APP, "nested_elements"));
        assertNull(nestedObject.getText());
    }

    @Test
    public void testGetVisibleBounds() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.pkg(TEST_APP));
        Rect bounds = object.getVisibleBounds();
        int top = bounds.top;
        int bottom = bounds.bottom;
        int left = bounds.left;
        int right = bounds.right;
        int boundsHeight = bounds.height();
        int boundsWidth = bounds.width();
        int displayHeight = mDevice.getDisplayHeight();
        int displayWidth = mDevice.getDisplayWidth();
        // Test the lower bounds
        assertTrue(0 <= top);
        assertTrue(0 <= left);
        assertTrue(top < bottom);
        assertTrue(left < right);
        // Test the upper bounds
        assertTrue(boundsHeight < displayHeight);
        assertTrue(boundsWidth < displayWidth);
    }

    @Test
    public void testGetVisibleCenter() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.pkg(TEST_APP));
        Rect bounds = object.getVisibleBounds();
        Point center = object.getVisibleCenter();
        int top = bounds.top;
        int bottom = bounds.bottom;
        int left = bounds.left;
        int right = bounds.right;
        assertEquals((left + right) / 2, center.x);
        assertEquals((top + bottom) / 2, center.y);
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

        UiObject2 object = mDevice.findObject(By.pkg(TEST_APP));

        assertTrue(object.hasObject(By.text("Text View 1")));
        assertFalse(object.hasObject(By.text("")));
    }

    @Test
    public void testIsCheckable() {
        launchTestActivity(UiObject2TestClickActivity.class);

        // CheckBox objects are checkable by default.
        UiObject2 checkBox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertTrue(checkBox.isCheckable());
        // Button objects are not checkable by default.
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertFalse(button.isCheckable());
    }

    @Test
    public void testIsChecked() {
        launchTestActivity(UiObject2TestClickActivity.class);

        UiObject2 checkBox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertFalse(checkBox.isChecked());
        checkBox.click();
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
        launchTestActivity(UiObject2TestIsEnabledActivity.class);

        UiObject2 disabledObject = mDevice.findObject(By.res(TEST_APP, "disabled_text_view"));
        assertFalse(disabledObject.isEnabled());
        UiObject2 enabledObject = mDevice.findObject(By.res(TEST_APP, "enabled_text_view"));
        assertTrue(enabledObject.isEnabled());
    }

    @Test
    public void testIsFocusable() {
        launchTestActivity(UiObject2TestIsFocusedActivity.class);

        UiObject2 nonFocusableTextView = mDevice.findObject(By.res(TEST_APP,
                "non_focusable_text_view"));
        assertFalse(nonFocusableTextView.isFocusable());
        UiObject2 focusableTextView = mDevice.findObject(By.res(TEST_APP, "focusable_text_view"));
        assertTrue(focusableTextView.isFocusable());
    }

    @Test
    public void testIsFocused() {
        launchTestActivity(UiObject2TestIsFocusedActivity.class);

        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "focusable_text_view"));
        assertFalse(textView.isFocused());
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        button.click();
        assertTrue(textView.isFocused());
    }

    @Test
    public void testIsLongClickable() {
        launchTestActivity(UiObject2TestIsLongClickableActivity.class);

        UiObject2 longClickableButton = mDevice.findObject(By.res(TEST_APP,
                "long_clickable_button"));
        assertTrue(longClickableButton.isLongClickable());
        UiObject2 nonLongClickableButton = mDevice.findObject(By.res(TEST_APP,
                "non_long_clickable_button"));
        assertFalse(nonLongClickableButton.isLongClickable());
    }

    @Test
    public void testIsScrollable() {
        launchTestActivity(UiObject2TestVerticalScrollActivity.class);

        // ScrollView objects are scrollable by default.
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        assertTrue(scrollView.isScrollable());
        // TextView objects are not scrollable by default.
        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "top_text"));
        assertFalse(textView.isScrollable());
    }

    @Test
    public void testIsSelected() {
        launchTestActivity(UiObject2TestIsSelectedActivity.class);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "selected_button"));
        button.click();
        UiObject2 textView = mDevice.findObject(By.res(TEST_APP, "selected_target"));
        assertTrue(textView.isSelected());
    }

    @Test
    public void testLongClickButton() {
        launchTestActivity(UiObject2TestLongClickActivity.class);

        // Find the button and verify its initial state
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("Long Click Me!", button.getText());

        // Click on the button and verify that the text has changed
        button.longClick();
        button.wait(Until.textEquals("I've been long clicked!"), TIMEOUT_MS);
        assertEquals("I've been long clicked!", button.getText());
    }

    @Test
    public void testPinchClose() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                        + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testPinchClose_withSpeed() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(.75f, SPEED_MS);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testPinchOpen() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchOpen(.5f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale text to be greater than 1f after pinchOpen(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch > 1f);
    }

    @Test
    public void testPinchOpen_withSpeed() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchOpen(.25f, SPEED_MS);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale text to be greater than 1f after pinchOpen(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch > 1f);
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
        assertEquals("This object has already been recycled", e.getMessage());
    }

    @Test
    @FlakyTest
    public void testScrollDown() {
        launchTestActivity(UiObject2TestVerticalScrollActivity.class);

        // Make sure we're at the top
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "top_text")));

        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        Rect bounds = scrollView.getVisibleBounds();
        float distance = 50000 / (bounds.height() - 2 * 10);

        //scrollView.scroll(Direction.DOWN, 1.0f);
        //assertNull(mDevice.findObject(By.res(TEST_APP, "top_text")));
        //while (scrollView.scroll(Direction.DOWN, 1.0f)) {
        //}
        scrollView.scroll(Direction.DOWN, distance);
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "bottom_text")));
    }

    /* TODO(b/235841473): Fix this test
    public void testScrollDistance() {
        launchTestActivity(UiObject2TestVerticalScrollActivity.class);

        // Make sure we're at the top
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "top_text")));
        int MARGIN = 1;

        // Scroll to an element 5000px from the top
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        Rect bounds = scrollView.getVisibleBounds();
        float distance = 5000.0f / (float)(bounds.height() - 2*MARGIN);
        scrollView.scroll(Direction.DOWN, distance);
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "from_top_5000")));

        // Scroll to an element 10000px from the top
        scrollView.scroll(Direction.DOWN, distance);
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "from_top_10000")));

        // Scroll to an element 15000px from the top
        scrollView.scroll(Direction.DOWN, distance);
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "from_top_15000")));
    }
    */

    @Test
    @FlakyTest(bugId = 235841959)
    public void testScrollDownToEnd() {
        launchTestActivity(UiObject2TestVerticalScrollActivity.class);

        // Make sure we're at the top
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "top_text")));

        // Scroll as much as we can
        UiObject2 scrollView = mDevice.findObject(By.res(TEST_APP, "scroll_view"));
        scrollView.wait(Until.scrollable(true), TIMEOUT_MS);
        while (scrollView.scroll(Direction.DOWN, 1.0f)) {
            // Continue until bottom.
        }

        // Make sure we're at the bottom
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "bottom_text")));
    }

    @Test
    public void testSetGestureMargin() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));

        // Set the gesture's margins to a large number (greater than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds cannot form a rectangle and no action can be performed.
        pinchArea.setGestureMargin(1_000);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertEquals(String.format("Expected scale value to be equal to 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), 1f, scaleValueAfterPinch, 0f);

        // Set the gesture's margins to a small number (smaller than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds form a rectangle and action can be performed.
        pinchArea.setGestureMargin(1);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testSetGestureMargins() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));

        // Set the gesture's margins to large numbers (greater than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds cannot form a rectangle and no action can be performed.
        pinchArea.setGestureMargins(1, 1, 1_000, 1_000);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        float scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertEquals(String.format("Expected scale value to be equal to 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), 1f, scaleValueAfterPinch, 0f);

        // Set the gesture's margins to small numbers (smaller than the width or height of the UI
        // object's visible bounds).
        // The gesture's bounds form a rectangle and action can be performed.
        pinchArea.setGestureMargins(1, 1, 1, 1);
        pinchArea.pinchClose(1f);
        scaleText.wait(Until.textNotEquals("1.0f"), TIMEOUT_MS);
        scaleValueAfterPinch = Float.valueOf(scaleText.getText());
        assertTrue(String.format("Expected scale value to be less than 1f after pinchClose(), "
                + "but got [%f]", scaleValueAfterPinch), scaleValueAfterPinch < 1f);
    }

    @Test
    public void testSetText() {
        launchTestActivity(UiObject2TestClearTextActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        // Verify the text field has "sample_text" before setText()
        assertEquals("sample_text", object.getText());
        object.setText("new_text");
        // Verify the text field has "new_text" after setText()
        assertEquals("new_text", object.getText());
    }

    /* TODO(b/235841473): Implement these tests
    public void testDrag() {}

    public void testFling() {}

    public void testSwipe() {}

    public void testWaitForExists() {}

    public void testWaitForGone() {}
    */
}
