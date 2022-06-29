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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.VideoView;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import org.junit.Test;

import java.util.regex.Pattern;

public class BySelectorTests extends BaseTest {

    @Test
    public void testCopy() {
        launchTestActivity(MainActivity.class);

        // Base selector
        BySelector base = By.clazz(".TextView");

        // Select various TextView instances
        assertNotNull(mDevice.findObject(By.copy(base).text("Text View 1")));
        assertNotNull(mDevice.findObject(By.copy(base).text("Item1")));
        assertNotNull(mDevice.findObject(By.copy(base).text("Item3")));

        // Shouldn't be able to select an object that does not match the base
        assertNull(mDevice.findObject(By.copy(base).text("Accessible button")));
    }

    @Test
    public void testClazz() {
        launchTestActivity(BySelectorTestClazzActivity.class);

        // Single string combining package name and class name.
        assertNotNull(mDevice.findObject(By.clazz("android.widget.Button")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.CheckBox")));
        assertNull(mDevice.findObject(By.clazz("android.widget.NonExistentClass")));

        // Single string as partial class name, starting with a dot.
        // The package will be assumed as `android.widget`.
        assertNotNull(mDevice.findObject(By.clazz(".SeekBar")));
        assertNotNull(mDevice.findObject(By.clazz(".Switch")));
        assertNull(mDevice.findObject(By.clazz(".NonExistentClass")));

        // Two separate strings as package name and class name.
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "EditText")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "ProgressBar")));
        assertNull(mDevice.findObject(By.clazz("android.widget", "NonExistentClass")));

        // Class directly.
        assertNotNull(mDevice.findObject(By.clazz(RadioButton.class)));
        assertNotNull(mDevice.findObject(By.clazz(RatingBar.class)));
        assertNull(mDevice.findObject(By.clazz(VideoView.class)));

        // Pattern of the class name.
        assertNotNull(mDevice.findObject(By.clazz(Pattern.compile(".*TextView"))));
        assertNotNull(mDevice.findObject(By.clazz(Pattern.compile(".*get\\.C.*"))));
        assertNull(mDevice.findObject(By.clazz(Pattern.compile(".*TextView.+"))));
    }

    @Test
    public void testDescSetFromResource() {
        launchTestActivity(BySelectorTestDescActivity.class);

        // Content Description from resource
        assertNotNull(mDevice.findObject(By.desc("Content Description Set From Layout")));
    }

    @Test
    public void testDescSetAtRuntime() {
        launchTestActivity(BySelectorTestDescActivity.class);

        // Content Description set at runtime
        assertNotNull(mDevice.findObject(By.desc("Content Description Set At Runtime")));
    }

    @Test
    public void testDescNotFound() {
        launchTestActivity(BySelectorTestDescActivity.class);

        // No element has this content description
        assertNull(mDevice.findObject(By.desc("No element has this Content Description")));
    }

    // TODO(b/235841286): Implement these for desc():
    // 1. Patterns
    // 2. Runtime Widgets

    @Test
    public void testPackage() {
        launchTestActivity(MainActivity.class);

        // Full match with string argument
        assertNotNull(mDevice.findObject(By.pkg(TEST_APP)));
    }

    @Test
    public void testResUniqueId() {
        launchTestActivity(BySelectorTestResActivity.class);

        // Unique ID
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "unique_id")));
        assertNotNull(mDevice.findObject(By.res(TEST_APP + ":id/unique_id")));
    }

    @Test
    public void testResCommonId() {
        launchTestActivity(BySelectorTestResActivity.class);

        // Shared ID
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "shared_id")));
        assertNotNull(mDevice.findObject(By.res(TEST_APP + ":id/shared_id")));
        // 1. Make sure we can see all instances
        // 2. Differentiate between matches by other criteria
    }

    @Test
    public void testTextUnique() {
        launchTestActivity(BySelectorTestTextActivity.class);

        // Unique Text
        assertNotNull(mDevice.findObject(By.text("Unique Text")));
    }

    @Test
    public void testTextCommon() {
        launchTestActivity(BySelectorTestTextActivity.class);

        // Common Text
        assertNotNull(mDevice.findObject(By.text("Common Text")));
        assertEquals(2, mDevice.findObjects(By.text("Common Text")).size());
    }

    @Test
    public void testHasUniqueChild() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Find parent with unique child
        UiObject2 object = mDevice.findObject(By.hasChild(By.res(TEST_APP, "toplevel1_child1")));
        assertNotNull(object);
    }

    @Test
    public void testHasCommonChild() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Find parent(s) with common child
        assertNotNull(mDevice.findObject(By.pkg(TEST_APP).hasChild(By.clazz(".TextView"))));
        assertEquals(3,
                mDevice.findObjects(By.pkg(TEST_APP).hasChild(By.clazz(".TextView"))).size());
    }

    @Test
    public void testGetChildren() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        UiObject2 parent = mDevice.findObject(By.res(TEST_APP, "toplevel2"));
        assertEquals(2, parent.getChildren().size());
    }

    @Test
    public void testHasMultipleChildren() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Select parent with multiple hasChild selectors
        UiObject2 object = mDevice.findObject(By
                .hasChild(By.res(TEST_APP, "toplevel2_child1"))
                .hasChild(By.res(TEST_APP, "toplevel2_child2")));
        assertNotNull(object);
    }

    @Test
    public void testHasMultipleChildrenCollision() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Select parent with multiple hasChild selectors, but single child that matches both
        UiObject2 object = mDevice.findObject(By
                .hasChild(By.res(TEST_APP, "toplevel1_child1"))
                .hasChild(By.clazz(".TextView")));
        assertNotNull(object);
    }

    @Test
    public void testHasChildThatHasChild() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Select parent with child that has a child
        UiObject2 object = mDevice.findObject(
                By.hasChild(By.hasChild(By.res(TEST_APP, "toplevel3_container1_child1"))));
        assertNotNull(object);
    }

    @Test
    public void testHasDescendant() {
        launchTestActivity(BySelectorTestHasChildActivity.class);

        // Select a LinearLayout that has a unique descendant
        UiObject2 object = mDevice.findObject(By
                .clazz(".RelativeLayout")
                .hasDescendant(By.res(TEST_APP, "toplevel3_container1_child1")));
        assertNotNull(object);
    }
}
