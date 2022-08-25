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
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.Switch;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

public class UiSelectorTests extends BaseTest {

    @Test
    public void testText() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().text("Sample text")).exists());
        assertFalse(mDevice.findObject(new UiSelector().text("Not text")).exists());
    }

    @Test
    public void testTextMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textMatches(".*text.*")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textMatches(".*nottext.*")).exists());
    }

    @Test
    public void testTextStartsWith() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textStartsWith("Text")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textStartsWith("NotText")).exists());
    }

    @Test
    public void testTextContains() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textContains("text")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textContains("not-text")).exists());
    }

    @Test
    public void testClassName_withString() {
        launchTestActivity(MainActivity.class);

        assertTrue(
                mDevice.findObject(new UiSelector().className("android.widget.Button")).exists());
        assertFalse(
                mDevice.findObject(new UiSelector().className("android.widget.Switch")).exists());
    }

    @Test
    public void testClassNameMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().classNameMatches(".*get\\.B.*")).exists());
        assertFalse(mDevice.findObject(new UiSelector().classNameMatches(".*Switch")).exists());
    }

    @Test
    public void testClassName_withClass() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().className(Button.class)).exists());
        assertFalse(mDevice.findObject(new UiSelector().className(Switch.class)).exists());
    }

    @Test
    public void testDescription() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().description("I'm accessible!")).exists());
        assertFalse(mDevice.findObject(new UiSelector().description("accessible")).exists());
    }

    @Test
    public void testDescriptionMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(
                mDevice.findObject(new UiSelector().descriptionMatches(".*accessible.*")).exists());
        assertFalse(
                mDevice.findObject(
                        new UiSelector().descriptionMatches(".*not_accessible.*")).exists());
    }

    @Test
    public void testDescriptionStartsWith() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().descriptionStartsWith("I'm")).exists());
        assertFalse(mDevice.findObject(new UiSelector().descriptionStartsWith("Im")).exists());
    }

    @Test
    public void testDescriptionContains() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().descriptionContains("acc")).exists());
        assertFalse(
                mDevice.findObject(new UiSelector().descriptionContains("abc")).exists());
    }

    @Test
    public void testResourcesId() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/example_id")).exists());
        assertFalse(
                mDevice.findObject(
                        new UiSelector().resourceId(TEST_APP + ":id/not_example_id")).exists());
    }

    @Test
    public void testResourcesIdMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceIdMatches(".*testapp:id/example.*")).exists());
        assertFalse(mDevice.findObject(
                new UiSelector().resourceIdMatches(".*testapp:id/not_example.*")).exists());
    }

    @Test
    public void testIndex() {
        launchTestActivity(ParentChildTestActivity.class);

        // `tree_N3` is the second child of its direct parent (1st in 0-based).
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N3").index(1)).exists());
        assertFalse(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N2").index(1)).exists());
    }

    @Test
    public void testInstance() throws Exception {
        launchTestActivity(ParentChildTestActivity.class);

        // `tree_N5` is the third instance (2nd in 0-based) of class `Button` ever rendered on
        // the screen.
        assertEquals("tree_N5",
                mDevice.findObject(new UiSelector().className(Button.class).instance(2)).getText());
    }

    @Test
    public void testEnabled() {
        launchTestActivity(IsEnabledTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/enabled_text_view").enabled(
                        true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/disabled_text_view").enabled(
                        false)).exists());
    }

    @Test
    public void testFocused() throws Exception {
        launchTestActivity(IsFocusedTestActivity.class);

        UiObject button = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/button"));

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/focusable_text_view").focused(
                        false)).exists());
        button.click();
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/focusable_text_view").focused(
                        true)).exists());
    }

    @Test
    public void testFocusable() {
        launchTestActivity(IsFocusedTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/focusable_text_view").focusable(
                        true)).exists());
        assertTrue(mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/non_focusable_text_view").focusable(false)).exists());
    }

    @Test
    public void testScrollable() {
        launchTestActivity(VerticalScrollTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/scroll_view").scrollable(
                        true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/top_text").scrollable(false)).exists());
    }

    @Test
    public void testSelected() throws Exception {
        launchTestActivity(IsSelectedTestActivity.class);

        UiObject selectedButton = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/selected_button"));

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/selected_target").selected(
                        false)).exists());
        selectedButton.click();
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/selected_target").selected(
                        true)).exists());
    }

    @Test
    public void testChecked() throws Exception {
        launchTestActivity(ClickTestActivity.class);

        UiObject checkBox = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box"));

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box").checked(false)).exists());
        checkBox.click();
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box").checked(true)).exists());
    }

    @Test
    public void testClickable() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().text("Accessible button").clickable(true)).exists());
        assertTrue(
                mDevice.findObject(new UiSelector().text("Sample text").clickable(false)).exists());
    }

    @Test
    public void testCheckable() {
        launchTestActivity(ClickTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box").checkable(true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button1").checkable(false)).exists());
    }

    @Test
    public void testLongClickable() {
        launchTestActivity(LongClickTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button").longClickable(true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/text_view").longClickable(
                        false)).exists());
    }

    /* TODO(b/242916007): Implement these tests, and the tests for exceptions of each tested method.

    public void testChildSelector() {}

    public void testFromParent() {}

    public void testPackageName() {}

    public void testPackageNameMatches() {}

    public void testToString() {}
    */
}
