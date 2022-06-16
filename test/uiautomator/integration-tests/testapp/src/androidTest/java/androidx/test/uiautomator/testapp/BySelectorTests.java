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
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class BySelectorTests {

    private static final String TAG = BySelectorTests.class.getSimpleName();

    private static final String TEST_APP = "androidx.test.uiautomator.testapp";
    private static final String ANDROID_WIDGET_PACKAGE = "android.widget";

    private UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    public void launchTestActivity(String activity) {
        // Launch the test app
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent()
                .setClassName(TEST_APP, String.format("%s.%s", TEST_APP, activity))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for activity to appear
        mDevice.wait(Until.hasObject(By.pkg(TEST_APP)), 10000);
    }

    @After
    public void tearDown() throws Exception {
        mDevice.pressHome();

        // Wait for the activity to disappear
        mDevice.wait(Until.gone(By.pkg(TEST_APP)), 5000);
    }

    @Test
    public void testCopy() {
        launchTestActivity("MainActivity");

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
    public void testClazzButton() {
        launchTestActivity("BySelectorTestClazzActivity");

        // Button
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "Button")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.Button")));
        assertNotNull(mDevice.findObject(By.clazz(".Button")));
        assertNotNull(mDevice.findObject(By.clazz(Button.class)));
    }

    @Test
    public void testClazzCheckBox() {
        launchTestActivity("BySelectorTestClazzActivity");

        // CheckBox
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "CheckBox")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.CheckBox")));
        assertNotNull(mDevice.findObject(By.clazz(".CheckBox")));
        assertNotNull(mDevice.findObject(By.clazz(CheckBox.class)));
    }

    @Test
    public void testClazzEditText() {
        launchTestActivity("BySelectorTestClazzActivity");

        // EditText
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "EditText")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.EditText")));
        assertNotNull(mDevice.findObject(By.clazz(".EditText")));
        assertNotNull(mDevice.findObject(By.clazz(EditText.class)));
    }

    @Test
    public void testClazzProgressBar() {
        launchTestActivity("BySelectorTestClazzActivity");

        // ProgressBar
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "ProgressBar")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.ProgressBar")));
        assertNotNull(mDevice.findObject(By.clazz(".ProgressBar")));
        assertNotNull(mDevice.findObject(By.clazz(ProgressBar.class)));
    }

    @Test
    public void testClazzRadioButton() {
        launchTestActivity("BySelectorTestClazzActivity");

        // RadioButton
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "RadioButton")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.RadioButton")));
        assertNotNull(mDevice.findObject(By.clazz(".RadioButton")));
        assertNotNull(mDevice.findObject(By.clazz(RadioButton.class)));
    }

    @Test
    public void testClazzRatingBar() {
        launchTestActivity("BySelectorTestClazzActivity");

        // RatingBar
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "RatingBar")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.RatingBar")));
        assertNotNull(mDevice.findObject(By.clazz(".RatingBar")));
        assertNotNull(mDevice.findObject(By.clazz(RatingBar.class)));
    }

    @Test
    public void testClazzSeekBar() {
        launchTestActivity("BySelectorTestClazzActivity");

        // SeekBar
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "SeekBar")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.SeekBar")));
        assertNotNull(mDevice.findObject(By.clazz(".SeekBar")));
        assertNotNull(mDevice.findObject(By.clazz(SeekBar.class)));
    }

    @Test
    public void testClazzSwitch() {
        launchTestActivity("BySelectorTestClazzActivity");

        // Switch
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "Switch")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.Switch")));
        assertNotNull(mDevice.findObject(By.clazz(".Switch")));
        assertNotNull(mDevice.findObject(By.clazz(Switch.class)));
    }

    @Test
    public void testClazzTextView() {
        launchTestActivity("BySelectorTestClazzActivity");

        // TextView
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "TextView")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.TextView")));
        assertNotNull(mDevice.findObject(By.clazz(".TextView")));
        assertNotNull(mDevice.findObject(By.clazz(TextView.class)));
    }

    @Test
    public void testClazzToggleButton() {
        launchTestActivity("BySelectorTestClazzActivity");

        // ToggleButton
        assertNotNull(mDevice.findObject(By.clazz("android.widget", "ToggleButton")));
        assertNotNull(mDevice.findObject(By.clazz("android.widget.ToggleButton")));
        assertNotNull(mDevice.findObject(By.clazz(".ToggleButton")));
        assertNotNull(mDevice.findObject(By.clazz(ToggleButton.class)));
    }

    @Test
    public void testClazzNotFound() {
        launchTestActivity("BySelectorTestClazzActivity");

        // Non-existent class
        assertNull(mDevice.findObject(By.clazz("android.widget", "NonExistentClass")));
        assertNull(mDevice.findObject(By.clazz("android.widget.NonExistentClass")));
        assertNull(mDevice.findObject(By.clazz(".NonExistentClass")));
    }

    @Test
    public void testClazzNull() {
        // clazz(String)
        try {
            mDevice.findObject(By.clazz((String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // clazz(String, String)
        try {
            mDevice.findObject(By.clazz((String) null, "foo"));
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            mDevice.findObject(By.clazz("foo", (String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // clazz(Class)
        try {
            mDevice.findObject(By.clazz((Class) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // clazz(Pattern)
        try {
            mDevice.findObject(By.clazz((Pattern) null));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    // TODO(b/235841286): Implement these for clazz():
    // 1. Custom class
    // 2. Patterns
    // 3. Runtime Widgets

    @Test
    public void testDescSetFromResource() {
        launchTestActivity("BySelectorTestDescActivity");

        // Content Description from resource
        assertNotNull(mDevice.findObject(By.desc("Content Description Set From Layout")));
    }

    @Test
    public void testDescSetAtRuntime() {
        launchTestActivity("BySelectorTestDescActivity");

        // Content Description set at runtime
        assertNotNull(mDevice.findObject(By.desc("Content Description Set At Runtime")));
    }

    @Test
    public void testDescNotFound() {
        launchTestActivity("BySelectorTestDescActivity");

        // No element has this content description
        assertNull(mDevice.findObject(By.desc("No element has this Content Description")));
    }

    @Test
    public void testDescNull() {
        // desc(String)
        try {
            mDevice.findObject(By.desc((String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // desc(Pattern)
        try {
            mDevice.findObject(By.desc((Pattern) null));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    // TODO(b/235841286): Implement these for desc():
    // 1. Patterns
    // 2. Runtime Widgets

    @Test
    public void testPackage() {
        launchTestActivity("MainActivity");

        // Full match with string argument
        assertNotNull(mDevice.findObject(By.pkg(TEST_APP)));
    }

    @Test
    public void testPkgNull() {
        // pkg(String)
        try {
            mDevice.findObject(By.pkg((String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // pkg(Pattern)
        try {
            mDevice.findObject(By.pkg((Pattern) null));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testResUniqueId() {
        launchTestActivity("BySelectorTestResActivity");

        // Unique ID
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "unique_id")));
        assertNotNull(mDevice.findObject(By.res(TEST_APP + ":id/unique_id")));
    }

    @Test
    public void testResCommonId() {
        launchTestActivity("BySelectorTestResActivity");

        // Shared ID
        assertNotNull(mDevice.findObject(By.res(TEST_APP, "shared_id")));
        assertNotNull(mDevice.findObject(By.res(TEST_APP + ":id/shared_id")));
        // 1. Make sure we can see all instances
        // 2. Differentiate between matches by other criteria
    }

    @Test
    public void testResNull() {
        // res(String)
        try {
            mDevice.findObject(By.res((String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // res(String, String)
        try {
            mDevice.findObject(By.res((String) null, "foo"));
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            mDevice.findObject(By.res("foo", (String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // res(Pattern)
        try {
            mDevice.findObject(By.res((Pattern) null));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testTextUnique() {
        launchTestActivity("BySelectorTestTextActivity");

        // Unique Text
        assertNotNull(mDevice.findObject(By.text("Unique Text")));
    }

    @Test
    public void testTextCommon() {
        launchTestActivity("BySelectorTestTextActivity");

        // Common Text
        assertNotNull(mDevice.findObject(By.text("Common Text")));
        assertEquals(2, mDevice.findObjects(By.text("Common Text")).size());
    }

    @Test
    public void testTextNull() {
        // text(String)
        try {
            mDevice.findObject(By.text((String) null));
            fail();
        } catch (NullPointerException expected) {
        }

        // text(Pattern)
        try {
            mDevice.findObject(By.text((Pattern) null));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testHasUniqueChild() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Find parent with unique child
        UiObject2 object = mDevice.findObject(By.hasChild(By.res(TEST_APP, "toplevel1_child1")));
        assertNotNull(object);
    }

    @Test
    public void testHasCommonChild() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Find parent(s) with common child
        assertNotNull(mDevice.findObject(By.pkg(TEST_APP).hasChild(By.clazz(".TextView"))));
        assertEquals(3,
                mDevice.findObjects(By.pkg(TEST_APP).hasChild(By.clazz(".TextView"))).size());
    }

    @Test
    public void testGetChildren() {
        launchTestActivity("BySelectorTestHasChildActivity");

        UiObject2 parent = mDevice.findObject(By.res(TEST_APP, "toplevel2"));
        assertEquals(2, parent.getChildren().size());
    }

    @Test
    public void testHasMultipleChildren() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Select parent with multiple hasChild selectors
        UiObject2 object = mDevice.findObject(By
                .hasChild(By.res(TEST_APP, "toplevel2_child1"))
                .hasChild(By.res(TEST_APP, "toplevel2_child2")));
        assertNotNull(object);
    }

    @Test
    public void testHasMultipleChildrenCollision() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Select parent with multiple hasChild selectors, but single child that matches both
        UiObject2 object = mDevice.findObject(By
                .hasChild(By.res(TEST_APP, "toplevel1_child1"))
                .hasChild(By.clazz(".TextView")));
        assertNotNull(object);
    }

    @Test
    public void testHasChildThatHasChild() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Select parent with child that has a child
        UiObject2 object = mDevice.findObject(
                By.hasChild(By.hasChild(By.res(TEST_APP, "toplevel3_container1_child1"))));
        assertNotNull(object);
    }

    @Test
    public void testHasDescendant() {
        launchTestActivity("BySelectorTestHasChildActivity");

        // Select a LinearLayout that has a unique descendant
        UiObject2 object = mDevice.findObject(By
                .clazz(".RelativeLayout")
                .hasDescendant(By.res(TEST_APP, "toplevel3_container1_child1")));
        assertNotNull(object);
    }
}
