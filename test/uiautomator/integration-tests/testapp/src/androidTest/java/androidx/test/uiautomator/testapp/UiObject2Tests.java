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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;

import androidx.test.filters.FlakyTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

import java.util.List;

public class UiObject2Tests extends BaseTest {

    @Test
    public void testClearTextField() {
        launchTestActivity(UiObject2TestClearTextActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        // Verify the text field has text before clear()
        assertEquals("sample_text", object.getText());
        object.clear();
        // Verify the text field does not have txt after clear()
        assertNull(object.getText());
    }

    @Test
    public void testClickButton() {
        launchTestActivity(UiObject2TestClickActivity.class);

        // Find the button and verify its initial state
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("Click Me!", button.getText());
        SystemClock.sleep(1000);

        // Click on the button and verify that the text has changed
        button.click();
        button.wait(Until.textEquals("I've been clicked!"), 10000);
        assertEquals("I've been clicked!", button.getText());
    }

    @Test
    public void testClickCheckBox() {
        launchTestActivity(UiObject2TestClickActivity.class);

        // Find the checkbox and verify its initial state
        UiObject2 checkbox = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertFalse(checkbox.isChecked());

        // Click on the checkbox and verify that it is now checked
        checkbox.click();
        checkbox.wait(Until.checked(true), 10000);
        assertTrue(checkbox.isChecked());
    }

    @Test
    public void testClickAndWaitForNewWindow() {
        launchTestActivity(UiObject2TestClickAndWaitActivity.class);

        // Click the button and wait for a new window
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "new_window_button"));
        button.clickAndWait(Until.newWindow(), 5000);
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
    public void testGetClassNameButton() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("android.widget.Button", object.getClassName());
    }

    @Test
    public void testGetClassNameCheckBox() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "check_box"));
        assertEquals("android.widget.CheckBox", object.getClassName());
    }

    @Test
    public void testGetClassNameEditText() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "edit_text"));
        assertEquals("android.widget.EditText", object.getClassName());
    }

    @Test
    public void testGetClassNameProgressBar() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "progress_bar"));
        assertEquals("android.widget.ProgressBar", object.getClassName());
    }

    @Test
    public void testGetClassNameRadioButton() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "radio_button"));
        assertEquals("android.widget.RadioButton", object.getClassName());
    }

    @Test
    public void testGetClassNameRatingBar() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "rating_bar"));
        assertEquals("android.widget.RatingBar", object.getClassName());
    }

    @Test
    public void testGetClassNameSeekBar() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "seek_bar"));
        assertEquals("android.widget.SeekBar", object.getClassName());
    }

    @Test
    public void testGetClassNameSwitch() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "switch_toggle"));
        assertEquals("android.widget.Switch", object.getClassName());
    }

    @Test
    public void testGetClassNameTextView() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "text_view"));
        assertEquals("android.widget.TextView", object.getClassName());
    }

    @Test
    public void testGetClassNameToggleButton() {
        launchTestActivity(UiObject2TestGetClassNameActivity.class);

        UiObject2 object = mDevice.findObject(By.res(TEST_APP, "toggle_button"));
        assertEquals("android.widget.ToggleButton", object.getClassName());
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
    public void testHasObject() {
        launchTestActivity(MainActivity.class);

        UiObject2 object = mDevice.findObject(By.pkg(TEST_APP));

        assertTrue(object.hasObject(By.text("Text View 1")));
        assertFalse(object.hasObject(By.text("")));
    }

    @Test
    public void testLongClickButton() {
        launchTestActivity(UiObject2TestLongClickActivity.class);

        // Find the button and verify its initial state
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertEquals("Long Click Me!", button.getText());

        // Click on the button and verify that the text has changed
        button.longClick();
        button.wait(Until.textEquals("I've been long clicked!"), 10000);
        assertEquals("I've been long clicked!", button.getText());
    }

    @Test
    public void testPinchIn100Percent() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        // Find the area to pinch
        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(1.0f, 100);
        scaleText.wait(Until.textNotEquals("1.0f"), 1000);
    }

    @Test
    public void testPinchIn75Percent() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        // Find the area to pinch
        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(.75f, 100);
        scaleText.wait(Until.textNotEquals("1.0f"), 1000);
    }

    @Test
    public void testPinchIn50Percent() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        // Find the area to pinch
        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(.5f, 100);
        scaleText.wait(Until.textNotEquals("1.0f"), 1000);
    }

    @Test
    public void testPinchIn25Percent() {
        launchTestActivity(UiObject2TestPinchActivity.class);

        // Find the area to pinch
        UiObject2 pinchArea = mDevice.findObject(By.res(TEST_APP, "pinch_area"));
        UiObject2 scaleText = pinchArea.findObject(By.res(TEST_APP, "scale_factor"));
        pinchArea.pinchClose(.25f, 100);
        scaleText.wait(Until.textNotEquals("1.0f"), 1000);
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
        scrollView.wait(Until.scrollable(true), 5000);
        while (scrollView.scroll(Direction.DOWN, 1.0f)) {
            // Continue until bottom.
        }

        // Make sure we're at the bottom
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "bottom_text")));
    }

    /* TODO(b/235841473): Implement these tests
    public void testSetText() {}

    public void testWaitForExists() {}

    public void testWaitForGone() {}
    */

    /* TODO(b/235841473): Implement more tests
    public void testDrag() {}

    public void testEquals() {}

    public void testFling() {}

    public void testIsCheckable() {}

    public void testIsChecked() {}

    public void testIsClickable() {}

    public void testIsEnabled() {}

    public void testIsFocusable() {}

    public void testIsFocused() {}

    public void testIsLongClickable() {}

    public void testIsScrollable() {}

    public void testIsSelected() {}
    */
}
