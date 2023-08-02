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

        assertEquals("UiSelector[TEXT=text]", new UiSelector().text("text").toString());
    }

    @Test
    public void testTextMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textMatches(".*text.*")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textMatches(".*nottext.*")).exists());

        assertEquals("UiSelector[TEXT_REGEX=.*text.*]",
                new UiSelector().textMatches(".*text.*").toString());
    }

    @Test
    public void testTextStartsWith() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textStartsWith("Text")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textStartsWith("NotText")).exists());

        assertEquals("UiSelector[START_TEXT=Text]",
                new UiSelector().textStartsWith("Text").toString());
    }

    @Test
    public void testTextContains() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().textContains("text")).exists());
        assertFalse(mDevice.findObject(new UiSelector().textContains("not-text")).exists());

        assertEquals("UiSelector[CONTAINS_TEXT=text]",
                new UiSelector().textContains("text").toString());
    }

    @Test
    public void testClassName_withString() {
        launchTestActivity(MainActivity.class);

        assertTrue(
                mDevice.findObject(new UiSelector().className("android.widget.Button")).exists());
        assertFalse(
                mDevice.findObject(new UiSelector().className("android.widget.Switch")).exists());

        assertEquals("UiSelector[CLASS=class]", new UiSelector().className("class").toString());
    }

    @Test
    public void testClassNameMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().classNameMatches(".*get\\.B.*")).exists());
        assertFalse(mDevice.findObject(new UiSelector().classNameMatches(".*Switch")).exists());

        assertEquals("UiSelector[CLASS_REGEX=.*]",
                new UiSelector().classNameMatches(".*").toString());
    }

    @Test
    public void testClassName_withClass() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().className(Button.class)).exists());
        assertFalse(mDevice.findObject(new UiSelector().className(Switch.class)).exists());

        assertEquals("UiSelector[CLASS=android.widget.Button]",
                new UiSelector().className(Button.class).toString());
    }

    @Test
    public void testDescription() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().description("I'm accessible!")).exists());
        assertFalse(mDevice.findObject(new UiSelector().description("accessible")).exists());

        assertEquals("UiSelector[DESCRIPTION=desc]",
                new UiSelector().description("desc").toString());
    }

    @Test
    public void testDescriptionMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(
                mDevice.findObject(new UiSelector().descriptionMatches(".*accessible.*")).exists());
        assertFalse(
                mDevice.findObject(
                        new UiSelector().descriptionMatches(".*not_accessible.*")).exists());

        assertEquals("UiSelector[DESCRIPTION_REGEX=.*]",
                new UiSelector().descriptionMatches(".*").toString());
    }

    @Test
    public void testDescriptionStartsWith() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().descriptionStartsWith("I'm")).exists());
        assertFalse(mDevice.findObject(new UiSelector().descriptionStartsWith("Im")).exists());

        assertEquals("UiSelector[START_DESCRIPTION=start]",
                new UiSelector().descriptionStartsWith("start").toString());
    }

    @Test
    public void testDescriptionContains() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().descriptionContains("acc")).exists());
        assertFalse(
                mDevice.findObject(new UiSelector().descriptionContains("abc")).exists());

        assertEquals("UiSelector[CONTAINS_DESCRIPTION=desc]",
                new UiSelector().descriptionContains("desc").toString());
    }

    @Test
    public void testResourcesId() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/example_id")).exists());
        assertFalse(
                mDevice.findObject(
                        new UiSelector().resourceId(TEST_APP + ":id/not_example_id")).exists());

        assertEquals("UiSelector[RESOURCE_ID=id]", new UiSelector().resourceId("id").toString());
    }

    @Test
    public void testResourcesIdMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceIdMatches(".*testapp:id/example.*")).exists());
        assertFalse(mDevice.findObject(
                new UiSelector().resourceIdMatches(".*testapp:id/not_example.*")).exists());

        assertEquals("UiSelector[RESOURCE_ID_REGEX=id.*]",
                new UiSelector().resourceIdMatches("id.*").toString());
    }

    @Test
    public void testIndex() {
        launchTestActivity(ParentChildTestActivity.class);

        // `tree_N3` is the second child of its direct parent (1st in 0-based).
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N3").index(1)).exists());
        assertFalse(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/tree_N2").index(1)).exists());

        assertEquals("UiSelector[INDEX=1]", new UiSelector().index(1).toString());
    }

    @Test
    public void testInstance() throws Exception {
        launchTestActivity(ParentChildTestActivity.class);

        // `tree_N5` is the third instance (2nd in 0-based) of class `Button` ever rendered on
        // the screen.
        assertEquals("tree_N5",
                mDevice.findObject(new UiSelector().className(Button.class).instance(2)).getText());

        assertEquals("UiSelector[INSTANCE=2]", new UiSelector().instance(2).toString());
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

        assertEquals("UiSelector[ENABLED=true]", new UiSelector().enabled(true).toString());
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

        assertEquals("UiSelector[FOCUSED=true]", new UiSelector().focused(true).toString());
    }

    @Test
    public void testFocusable() {
        launchTestActivity(IsFocusedTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/focusable_text_view").focusable(
                        true)).exists());
        assertTrue(mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/non_focusable_text_view").focusable(false)).exists());

        assertEquals("UiSelector[FOCUSABLE=true]", new UiSelector().focusable(true).toString());
    }

    @Test
    public void testScrollable() {
        launchTestActivity(VerticalScrollTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/scroll_view").scrollable(
                        true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/top_text").scrollable(false)).exists());

        assertEquals("UiSelector[SCROLLABLE=true]", new UiSelector().scrollable(true).toString());
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

        assertEquals("UiSelector[SELECTED=true]", new UiSelector().selected(true).toString());
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

        assertEquals("UiSelector[CHECKED=true]", new UiSelector().checked(true).toString());
    }

    @Test
    public void testClickable() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().text("Accessible button").clickable(true)).exists());
        assertTrue(
                mDevice.findObject(new UiSelector().text("Sample text").clickable(false)).exists());

        assertEquals("UiSelector[CLICKABLE=true]", new UiSelector().clickable(true).toString());
    }

    @Test
    public void testCheckable() {
        launchTestActivity(ClickTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/check_box").checkable(true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button1").checkable(false)).exists());

        assertEquals("UiSelector[CHECKABLE=true]", new UiSelector().checkable(true).toString());
    }

    @Test
    public void testLongClickable() {
        launchTestActivity(LongClickTestActivity.class);

        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/button").longClickable(true)).exists());
        assertTrue(mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/text_view").longClickable(
                        false)).exists());

        assertEquals("UiSelector[LONG_CLICKABLE=true]",
                new UiSelector().longClickable(true).toString());
    }

    @Test
    public void testChildSelector() {
        launchTestActivity(ParentChildTestActivity.class);

        UiSelector treeN2Selector = new UiSelector().resourceIdMatches(".*2.*");
        UiSelector treeN5Selector = new UiSelector().resourceIdMatches(".*5.*");
        UiSelector expectedTreeN3Selector = new UiSelector().resourceId(
                TEST_APP + ":id/tree_N3").childSelector(treeN5Selector);
        UiSelector notExpectedTreeN3Selector = new UiSelector().resourceId(
                TEST_APP + ":id/tree_N3").childSelector(treeN2Selector);

        assertTrue(mDevice.findObject(expectedTreeN3Selector).exists());
        assertFalse(mDevice.findObject(notExpectedTreeN3Selector).exists());

        assertEquals("UiSelector[CHILD=UiSelector[TEXT=text]]",
                new UiSelector().childSelector(new UiSelector().text("text")).toString());
    }

    @Test
    public void testFromParent() {
        launchTestActivity(ParentChildTestActivity.class);

        UiSelector treeN2Selector = new UiSelector().resourceIdMatches(".*2.*");
        UiSelector treeN4Selector = new UiSelector().resourceIdMatches(".*4.*");
        UiSelector expectedTreeN5Selector = new UiSelector().resourceId(
                TEST_APP + ":id/tree_N5").fromParent(treeN4Selector); // N5 is sibling of N4.
        UiSelector notExpectedTreeN5Selector = new UiSelector().resourceId(
                TEST_APP + ":id/tree_N5").fromParent(treeN2Selector);

        assertTrue(mDevice.findObject(expectedTreeN5Selector).exists());
        assertFalse(mDevice.findObject(notExpectedTreeN5Selector).exists());

        assertEquals("UiSelector[PARENT=UiSelector[TEXT=text]]",
                new UiSelector().fromParent(new UiSelector().text("text")).toString());
    }

    @Test
    public void testPackageName() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().packageName(TEST_APP)).exists());
        assertFalse(mDevice.findObject(new UiSelector().packageName(TEST_APP + "abc")).exists());

        assertEquals("UiSelector[PACKAGE_NAME=pack]",
                new UiSelector().packageName("pack").toString());
    }

    @Test
    public void testPackageNameMatches() {
        launchTestActivity(MainActivity.class);

        assertTrue(mDevice.findObject(new UiSelector().packageNameMatches(".*testapp.*")).exists());
        assertFalse(
                mDevice.findObject(new UiSelector().packageNameMatches(".*nottest.*")).exists());

        assertEquals("UiSelector[PACKAGE_NAME_REGEX=pack.*]",
                new UiSelector().packageNameMatches("pack.*").toString());
    }
}
