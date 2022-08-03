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

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

public class UiObjectTest extends BaseTest {

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

        assertThrows(noNode.getSelector().toString(), UiObjectNotFoundException.class,
                noNode::getChildCount);
    }

    /* TODO(b/241158642): Implement these tests, and the tests for exceptions of each tested method.

    public void testDragTo_destObjAndSteps() {}

    public void testDragTo_destXAndDestYAndSteps() {}

    public void testSwipeUp() {}

    public void testSwipeDown() {}

    public void testSwipeLeft() {}

    public void testSwipeRight() {}

    public void testClick() {}

    public void testClickAndWaitForNewWindow() {}

    public void testClickAndWaitForNewWindow_timeout() {}

    public void testClickTopLeft() {}

    public void testLongClickBottomRight() {}

    public void testClickBottomRight() {}

    public void testLongClick() {}

    public void testLongClickTopLeft() {}

    public void testGetText() {}

    public void testGetClassName() {}

    public void testGetContentDescription() {}

    public void testLegacySetText() {}

    public void testSetText() {}

    public void testClearTextField() {}

    public void testIsChecked() {}

    public void testIsSelected() {}

    public void testIsCheckable() {}

    public void testIsEnabled() {}

    public void testIsClickable() {}

    public void testIsFocused() {}

    public void testIsFocusable() {}

    public void testIsScrollable() {}

    public void testIsLongClickable() {}

    public void testGetPackageName() {}

    public void testGetVisibleBounds() {}

    public void testGetBounds() {}

    public void testWaitForExists() {}

    public void testWaitUntilGone() {}

    public void testExists() {}

    public void testPinchOut() {}

    public void testPinchIn() {}

    public void testPerformTwoPointerGesture() {}

    public void testPerformMultiPointerGesture() {}
    */
}
