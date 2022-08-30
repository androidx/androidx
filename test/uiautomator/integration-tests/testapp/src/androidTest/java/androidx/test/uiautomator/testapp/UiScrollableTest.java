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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.widget.TextView;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

public class UiScrollableTest extends BaseTest {
    @Test
    public void testGetChildByDescription() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByDescription(new UiSelector().className(TextView.class),
                        "This is the bottom");

        assertEquals("This is the bottom", target.getText());
        assertThrows(UiObjectNotFoundException.class,
                () -> relativeLayout.getChildByDescription(
                        new UiSelector().className(TextView.class),
                        "This is non-existent"));
    }

    @Test
    public void testGetChildByDescription_withoutScrollSearch() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByDescription(new UiSelector().className(TextView.class),
                        "This is the top", false);

        assertEquals("This is the top", target.getText());
        assertThrows(UiObjectNotFoundException.class,
                () -> relativeLayout.getChildByDescription(
                        new UiSelector().className(TextView.class), "This is the bottom",
                        false));
    }

    @Test
    public void testGetChildByInstance() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        // Find the first (0th in 0-based index) instance of `TextView` objects.
        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByInstance(new UiSelector().className(TextView.class), 0);

        // The search in `getChildByInstance` is performed without scrolling.
        assertEquals("This is the top", target.getText());
        assertFalse(relativeLayout.getChildByInstance(new UiSelector().className(TextView.class),
                1).exists());
    }

    @Test
    public void testGetChildByText() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByText(new UiSelector().className(TextView.class),
                        "This is the bottom");

        assertEquals("This is the bottom", target.getText());
        assertThrows(UiObjectNotFoundException.class,
                () -> relativeLayout.getChildByText(new UiSelector().className(TextView.class),
                        "This is non-existent"));
    }

    @Test
    public void testGetChildByText_withoutScrollSearch() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByText(new UiSelector().className(TextView.class),
                        "This is the top", false);

        assertEquals("This is the top", target.getText());
        assertThrows(UiObjectNotFoundException.class,
                () -> relativeLayout.getChildByText(new UiSelector().className(TextView.class),
                        "This is the bottom", false));
    }

    @Test
    public void testScrollDescriptionIntoView() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/from_top_15000"));

        assertFalse(target.exists());
        assertTrue(relativeLayout.scrollDescriptionIntoView("This is 15000px from the top"));
        assertTrue(target.exists());
        assertFalse(relativeLayout.scrollDescriptionIntoView("This is non-existent"));
    }

    @Test
    public void testScrollIntoView_withUiObject() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/from_top_15000"));
        UiObject nonExistentTarget = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/not_exist"));

        assertFalse(target.exists());
        assertTrue(relativeLayout.scrollIntoView(target));
        assertTrue(target.exists());
        assertFalse(relativeLayout.scrollIntoView(nonExistentTarget));
    }

    @Test
    public void testScrollIntoView_withUiSelector() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiSelector target = new UiSelector().resourceId(TEST_APP + ":id/from_top_15000");
        UiSelector nonExistentTarget = new UiSelector().resourceId(TEST_APP + ":id/not_exist");

        assertFalse(mDevice.findObject(target).exists());
        assertTrue(relativeLayout.scrollIntoView(target));
        assertTrue(mDevice.findObject(target).exists());
        assertFalse(relativeLayout.scrollIntoView(nonExistentTarget));
    }

    @Test
    public void testEnsureFullyVisible() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/from_top_15000"));

        assertTrue(relativeLayout.scrollIntoView(target));
        assertTrue(relativeLayout.ensureFullyVisible(target));
        assertThrows(UiObjectNotFoundException.class,
                () -> relativeLayout.ensureFullyVisible(
                        mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"))));
    }

    @Test
    public void testScrollTextIntoView() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/from_top_15000"));

        assertFalse(target.exists());
        assertTrue(relativeLayout.scrollTextIntoView("This is 15000px from the top"));
        assertTrue(target.exists());
        assertFalse(relativeLayout.scrollTextIntoView("This is non-existent"));
    }

    @Test
    public void testSetMaxSearchSwipesAndGetMaxSearchSwipes() {
        UiScrollable scrollable = new UiScrollable(new UiSelector()).setMaxSearchSwipes(5);
        assertEquals(5, scrollable.getMaxSearchSwipes());
    }

    @Test
    public void testFlingForward() throws Exception {
        launchTestActivity(FlingTestActivity.class);

        UiScrollable flingRegion = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/fling_region")).setAsVerticalList();

        // The `fling_region` won't move (thus return false), but it will receive the fling action.
        assertFalse(flingRegion.flingForward());
        assertEquals("fling_down", flingRegion.getText());

        // Assert throwing exception for `scrollForward(int)` and all its related methods.
        UiScrollable noNode = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/no_node"));
        assertThrows(UiObjectNotFoundException.class, noNode::flingForward);
    }

    @Test
    public void testScrollForward_vertical() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/swipe_region")).setAsVerticalList();

        assertFalse(scrollRegion.scrollForward());
        assertEquals("swipe_up", scrollRegion.getText());
    }

    @Test
    public void testScrollForward_horizontal() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/swipe_region")).setAsHorizontalList();

        assertFalse(scrollRegion.scrollForward(50));
        assertEquals("swipe_left", scrollRegion.getText());
    }

    @Test
    public void testFlingBackward() throws Exception {
        launchTestActivity(FlingTestActivity.class);

        UiScrollable flingRegion = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/fling_region")).setAsVerticalList();

        assertFalse(flingRegion.flingBackward());
        assertEquals("fling_up", flingRegion.getText());

        UiScrollable noNode = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/no_node"));
        assertThrows(UiObjectNotFoundException.class, noNode::flingBackward);
    }

    @Test
    public void testScrollBackward_vertical() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/swipe_region")).setAsVerticalList();

        assertFalse(scrollRegion.scrollBackward());
        assertEquals("swipe_down", scrollRegion.getText());
    }

    @Test
    public void testScrollBackward_horizontal() throws Exception {
        launchTestActivity(SwipeTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/swipe_region")).setAsHorizontalList();

        assertFalse(scrollRegion.scrollBackward(50));
        assertEquals("swipe_right", scrollRegion.getText());
    }

    /* TODO(b/243837077): Implement these tests, and the tests for exceptions of each tested method.

    public void testScrollToBeginning() {}

    public void testScrollToBeginning() {}

    public void testFlingToBeginning() {}

    public void testScrollToEnd() {}

    public void testScrollToEnd() {}

    public void testFlingToEnd() {}

    public void testGetSwipeDeadZonePercentage() {}

    public void testSetSwipeDeadZonePercentage() {}
     */
}
