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
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.TextView;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

public class BySelectorTest extends BaseTest {

    @Test
    public void testCopy() {
        launchTestActivity(MainActivity.class);

        // Base selector
        BySelector base = By.clazz(".TextView");

        // Select various TextView instances
        assertTrue(mDevice.hasObject(By.copy(base).text("Text View 1")));
        assertTrue(mDevice.hasObject(By.copy(base).text("Item1")));
        assertTrue(mDevice.hasObject(By.copy(base).text("Item3")));

        // Shouldn't be able to select an object that does not match the base
        assertFalse(mDevice.hasObject(By.copy(base).text("Accessible button")));
    }

    @Test
    public void testClazz() {
        launchTestActivity(BySelectorTestActivity.class);

        // Single string combining package name and class name.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz("android.widget.Button")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz("android.widget.TextView")));

        // Single string as partial class name, starting with a dot.
        // The package will be assumed as `android.widget`.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(".Button")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(".TextView")));

        // Two separate strings as package name and class name.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz("android.widget", "Button")));
        assertFalse(
                mDevice.hasObject(By.res(TEST_APP, "clazz").clazz("android.widget", "TextView")));

        // Class directly.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(Button.class)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(TextView.class)));

        // Pattern of the class name.
        assertTrue(
                mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(Pattern.compile(".*get\\.B.*"))));
        assertFalse(
                mDevice.hasObject(By.res(TEST_APP, "clazz").clazz(Pattern.compile(".*TextView"))));
    }

    @Test
    public void testDesc() {
        launchTestActivity(BySelectorTestActivity.class);

        // String as the exact content description.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "desc_family").desc("The is\nthe desc.")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "desc_family").desc("desc")));

        // Pattern of the content description.
        assertTrue(mDevice.hasObject(
                By.res(TEST_APP, "desc_family").desc(Pattern.compile(".*desc.*", Pattern.DOTALL))));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "desc_family").desc(
                Pattern.compile(".*not_desc.*", Pattern.DOTALL))));
    }

    @Test
    public void testDescContains() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "desc_family").descContains("desc")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "desc_family").descContains("not_desc")));
    }

    @Test
    public void testDescStartsWith() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "desc_family").descStartsWith("The")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "desc_family").descStartsWith("NotThe")));
    }

    @Test
    public void testDescEndsWith() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "desc_family").descEndsWith("desc.")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "desc_family").descEndsWith("not.")));
    }

    @Test
    public void testPkg() {
        launchTestActivity(MainActivity.class);

        // String as the complete full app name.
        assertTrue(mDevice.hasObject(By.pkg(TEST_APP)));
        assertFalse(mDevice.hasObject(By.pkg(TEST_APP + "_not")));

        // Pattern as the search pattern of the app name.
        assertTrue(mDevice.hasObject(By.pkg(Pattern.compile(".*testapp.*"))));
        assertFalse(mDevice.hasObject(By.pkg(Pattern.compile(".*not_testapp.*"))));
    }

    @Test
    public void testRes() {
        launchTestActivity(BySelectorTestActivity.class);

        // Single string as the resource name.
        assertTrue(mDevice.hasObject(By.res(TEST_APP + ":id/res")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP + ":id/not_res")));

        // Two strings as package name and ID name.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "res")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_res")));

        // Pattern of the resource name.
        assertTrue(mDevice.hasObject(By.res(Pattern.compile(".*testapp:id/res.*"))));
        assertFalse(mDevice.hasObject(By.res(Pattern.compile(".*testapp:id/not_res.*"))));
    }

    @Test
    public void testText() {
        launchTestActivity(BySelectorTestActivity.class);

        // Single string as the exact content of the text.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "text_family").text("This is\nthe text.")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "text_family").text("the text")));

        // Pattern of the text.
        assertTrue(mDevice.hasObject(
                By.res(TEST_APP, "text_family").text(Pattern.compile(".*text.*", Pattern.DOTALL))));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "text_family").text(
                Pattern.compile(".*nottext.*", Pattern.DOTALL))));
    }

    @Test
    public void testTextContains() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "text_family").textContains("text")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "text_family").textContains("not-text")));
    }

    @Test
    public void testTextStartsWith() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "text_family").textStartsWith("This")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "text_family").textStartsWith("NotThis")));
    }

    @Test
    public void testTextEndsWith() {
        launchTestActivity(BySelectorTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "text_family").textEndsWith("text.")));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "text_family").textEndsWith("not.")));
    }

    @Test
    public void testCheckable() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the checkable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "checkable").checkable(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_checkable").checkable(true)));

        // Find the uncheckable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_checkable").checkable(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "checkable").checkable(false)));
    }

    @Test
    public void testChecked() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the checked component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "checked").checked(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_checked").checked(true)));

        // Find the unchecked component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_checked").checked(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "checked").checked(false)));
    }

    @Test
    public void testClickable() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "clickable").clickable(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_clickable").clickable(true)));

        // Find the not clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_clickable").clickable(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "clickable").clickable(false)));
    }

    @Test
    public void testEnabled() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the enabled component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "enabled").enabled(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_enabled").enabled(true)));

        // Find the not enabled component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_enabled").enabled(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "enabled").enabled(false)));
    }

    @Test
    public void testFocusable() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "focusable").focusable(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_focusable").focusable(true)));

        // Find the not clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_focusable").focusable(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "focusable").focusable(false)));
    }

    @Test
    public void testFocused() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "focused").focused(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_focused").focused(true)));

        // Find the not clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_focused").focused(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "focused").focused(false)));
    }

    @Test
    public void testLongClickable() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the long clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "longClickable").longClickable(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_longClickable").longClickable(true)));

        // Find the not long clickable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_longClickable").longClickable(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "longClickable").longClickable(false)));
    }

    @Test
    public void testScrollable() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the scrollable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "scrollable").scrollable(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_scrollable").scrollable(true)));

        // Find the not scrollable component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_scrollable").scrollable(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "scrollable").scrollable(false)));
    }

    @Test
    public void testSelected() {
        launchTestActivity(BySelectorTestActivity.class);

        // Find the selected component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "selected").selected(true)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "not_selected").selected(true)));

        // Find the not selected component.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "not_selected").selected(false)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "selected").selected(false)));
    }

    @Test
    public void testDepth() {
        launchTestActivity(ParentChildTestActivity.class);

        // Depth of all nodes in the tree.
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N1").depth(5)));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N2").depth(6)));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N3").depth(6)));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N4").depth(7)));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N5").depth(7)));

        // Some random checks.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N1").depth(0)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N1").depth(1)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N2").depth(1)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N3").depth(1)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N4").depth(2)));
    }

    @Test
    public void testHasParent() {
        launchTestActivity(ParentChildTestActivity.class);
        BySelector n1 = By.res(TEST_APP, "tree_N1"); // grandparent
        BySelector n3 = By.res(TEST_APP, "tree_N3"); // parent
        BySelector n4 = By.res(TEST_APP, "tree_N4"); // sibling
        BySelector n5 = By.res(TEST_APP, "tree_N5"); // child

        // Can search by parent-child relationship.
        UiObject2 child = mDevice.findObject(By.copy(n5).hasParent(n3));
        assertEquals("tree_N5", child.getText());

        // Can find all children by parent-child relationship.
        List<UiObject2> descendants = mDevice.findObjects(By.hasParent(n3));
        assertEquals(2, descendants.size());

        // Parent selector can have a parent (grandparent search).
        UiObject2 grandchild = mDevice.findObject(By.copy(n5).hasParent(By.hasParent(n1)));
        assertEquals("tree_N5", grandchild.getText());

        // Parent selectors can have children (sibling search).
        UiObject2 sibling = mDevice.findObject(By.copy(n4).hasParent(By.hasChild(n5)));
        assertEquals("tree_N4", sibling.getText());

        // Parent must be a direct ancestor.
        assertFalse(mDevice.hasObject(By.copy(n5).hasParent(n1)));
        assertFalse(mDevice.hasObject(By.copy(n5).hasParent(n4)));
    }

    @Test
    public void testHasAncestor() {
        launchTestActivity(ParentChildTestActivity.class);
        BySelector n1 = By.res(TEST_APP, "tree_N1"); // grandparent
        BySelector n3 = By.res(TEST_APP, "tree_N3"); // parent
        BySelector n4 = By.res(TEST_APP, "tree_N4"); // sibling
        BySelector n5 = By.res(TEST_APP, "tree_N5"); // child

        // Can search by any ancestor or a subset of ancestors.
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(n1)));
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(n3)));
        assertFalse(mDevice.hasObject(By.copy(n5).hasAncestor(n4)));
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(n1, 2)));
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(n3, 1)));
        assertFalse(mDevice.hasObject(By.copy(n5).hasAncestor(n1, 1)));

        // Can find all descendants (N2, N3, N4, N5).
        List<UiObject2> descendants = mDevice.findObjects(By.hasAncestor(n1));
        assertEquals(4, descendants.size());

        // Ancestor selectors can have ancestors and descendants.
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(By.hasAncestor(n1))));
        assertFalse(mDevice.hasObject(By.copy(n5).hasAncestor(By.hasAncestor(n3))));
        assertTrue(mDevice.hasObject(By.copy(n5).hasAncestor(By.hasDescendant(n3))));
    }

    @Test
    public void testHasChild() {
        launchTestActivity(ParentChildTestActivity.class);

        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N3").hasChild(By.res(TEST_APP,
                "tree_N4"))));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N3").hasChild(By.res(TEST_APP,
                "tree_N2"))));

        // Child should be directly connected with its parent.
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N1").hasChild(By.res(TEST_APP,
                "tree_N5"))));
    }

    @Test
    public void testHasDescendant() {
        launchTestActivity(ParentChildTestActivity.class);

        // A BySelector as the possible descendant (child is also a descendant).
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N1").hasDescendant(By.res(TEST_APP,
                "tree_N5"))));
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N1").hasDescendant(By.res(TEST_APP,
                "tree_N3"))));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N2").hasDescendant(By.res(TEST_APP,
                "tree_N5"))));

        // A BySelector as the possible descendant, and an int as the max relative depth of the
        // search (the parent has a relative depth of 0).
        assertTrue(mDevice.hasObject(By.res(TEST_APP, "tree_N1").hasDescendant(By.res(TEST_APP,
                "tree_N5"), 2)));
        assertFalse(mDevice.hasObject(By.res(TEST_APP, "tree_N1").hasDescendant(By.res(TEST_APP,
                "tree_N5"), 1)));
    }
}
