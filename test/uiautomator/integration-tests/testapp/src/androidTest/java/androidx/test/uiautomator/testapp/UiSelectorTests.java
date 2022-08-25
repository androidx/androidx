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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.Switch;

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

    /* TODO(b/242916007): Implement these tests, and the tests for exceptions of each tested method.

    public void testIndex() {}

    public void testInstance() {}

    public void testEnabled() {}

    public void testFocused() {}

    public void testFocusable() {}

    public void testScrollable() {}

    public void testSelected() {}

    public void testChecked() {}

    public void testClickable() {}

    public void testCheckable() {}

    public void testLongClickable() {}

    public void testChildSelector() {}

    public void testFromParent() {}

    public void testPackageName() {}

    public void testPackageNameMatches() {}

    public void testToString() {}
    */
}
