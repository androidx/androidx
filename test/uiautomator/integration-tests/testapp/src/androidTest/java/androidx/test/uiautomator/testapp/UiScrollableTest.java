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

import android.widget.TextView;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class UiScrollableTest extends BaseTest {

    private int mDefaultMaxSearchSwipes = 0;

    private double mDefaultSwipeDeadZonePercentage = 0.0;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        mDefaultMaxSearchSwipes = new UiScrollable(new UiSelector()).getMaxSearchSwipes();
        mDefaultSwipeDeadZonePercentage =
                new UiScrollable(new UiSelector()).getSwipeDeadZonePercentage();
    }

    @After
    public void tearDown() {
        new UiScrollable(new UiSelector()).setMaxSearchSwipes(mDefaultMaxSearchSwipes);
        new UiScrollable(new UiSelector()).setSwipeDeadZonePercentage(
                mDefaultSwipeDeadZonePercentage);
    }

    @Test
    public void testGetChildByDescription() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target =
                relativeLayout.getChildByDescription(new UiSelector().className(TextView.class),
                        "This is the bottom");

        assertEquals("This is the bottom", target.getText());
        assertUiObjectNotFound(
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
        assertUiObjectNotFound(
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
        assertUiObjectNotFound(
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
        assertUiObjectNotFound(
                () -> relativeLayout.getChildByText(new UiSelector().className(TextView.class),
                        "This is the bottom", false));
    }

    @Test
    public void testScrollDescriptionIntoView() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/bottom_text"));

        assertFalse(target.exists());
        assertTrue(relativeLayout.scrollDescriptionIntoView("This is the bottom"));
        assertTrue(target.exists());
        assertFalse(relativeLayout.scrollDescriptionIntoView("This is non-existent"));
    }

    @Test
    public void testScrollIntoView_withUiObject() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/bottom_text"));
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
        UiSelector target = new UiSelector().resourceId(TEST_APP + ":id/bottom_text");
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
                new UiSelector().resourceId(TEST_APP + ":id/bottom_text"));

        assertTrue(relativeLayout.scrollIntoView(target));
        assertTrue(relativeLayout.ensureFullyVisible(target));
        assertUiObjectNotFound(
                () -> relativeLayout.ensureFullyVisible(
                        mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id/no_node"))));
    }

    @Test
    public void testScrollTextIntoView() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject target = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/bottom_text"));

        assertFalse(target.exists());
        assertTrue(relativeLayout.scrollTextIntoView("This is the bottom"));
        assertTrue(target.exists());
        assertFalse(relativeLayout.scrollTextIntoView("This is non-existent"));
    }

    @Test
    public void testSetMaxSearchSwipesAndGetMaxSearchSwipes() {
        UiScrollable scrollable = new UiScrollable(new UiSelector()).setMaxSearchSwipes(5);
        assertEquals(5, scrollable.getMaxSearchSwipes());
    }

    @Ignore // b/266965027
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
        assertUiObjectNotFound(noNode::flingForward);
    }

    @Test
    public void testScrollForward_vertical() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout")).setAsVerticalList();

        assertTrue(scrollRegion.scrollForward());
    }

    @Test
    public void testScrollForward_horizontal() throws Exception {
        launchTestActivity(HorizontalScrollTestActivity.class);

        UiScrollable scrollRegion = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"))
                .setAsHorizontalList();
        assertTrue(scrollRegion.scrollForward());
    }

    @Ignore // b/266965027
    @Test
    public void testFlingBackward() throws Exception {
        launchTestActivity(FlingTestActivity.class);

        UiScrollable flingRegion = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/fling_region")).setAsVerticalList();

        assertFalse(flingRegion.flingBackward());
        assertEquals("fling_up", flingRegion.getText());

        UiScrollable noNode = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/no_node"));
        assertUiObjectNotFound(noNode::flingBackward);
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

    @Test
    public void testScrollToBeginning_withSteps() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject topText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/top_text"));

        assertTrue(relativeLayout.scrollTextIntoView("This is the bottom"));
        assertFalse(topText.exists());
        assertTrue(relativeLayout.scrollToBeginning(20, 50));
        assertTrue(topText.exists());
    }

    @Test
    public void testScrollToBeginning_notEnoughSwipes_failed() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject topText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/top_text"));

        assertTrue(relativeLayout.scrollTextIntoView("This is the bottom"));
        assertFalse(topText.exists());
        assertFalse(relativeLayout.scrollToBeginning(1));
        assertFalse(topText.exists());
    }

    @Test
    public void testScrollToBeginning() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject topText = mDevice.findObject(new UiSelector().resourceId(TEST_APP + ":id"
                + "/top_text"));

        assertTrue(relativeLayout.scrollTextIntoView("This is the bottom"));
        assertFalse(topText.exists());
        assertTrue(relativeLayout.scrollToBeginning(20));
        assertTrue(topText.exists());
    }

    @Ignore // b/266965027
    @Test
    public void testFlingToBeginning() throws Exception {
        launchTestActivity(FlingTestActivity.class);

        UiScrollable flingRegion = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/fling_region")).setAsVerticalList();

        assertTrue(flingRegion.flingToBeginning(20));
        assertEquals("fling_up", flingRegion.getText());
    }

    @Test
    public void testScrollToEnd_withSteps() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject bottomText = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/bottom_text"));

        assertFalse(bottomText.exists());
        assertTrue(relativeLayout.scrollToEnd(20, 50));
        assertTrue(bottomText.exists());
    }

    @Test
    public void testScrollToEnd_notEnoughSwipes_failed() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject bottomText = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/bottom_text"));

        assertFalse(bottomText.exists());
        assertFalse(relativeLayout.scrollToEnd(1));
        assertFalse(bottomText.exists());
    }

    @Test
    public void testScrollToEnd() throws Exception {
        launchTestActivity(VerticalScrollTestActivity.class);

        UiScrollable relativeLayout = new UiScrollable(
                new UiSelector().resourceId(TEST_APP + ":id/relative_layout"));
        UiObject bottomText = mDevice.findObject(
                new UiSelector().resourceId(TEST_APP + ":id/bottom_text"));

        assertFalse(bottomText.exists());
        assertTrue(relativeLayout.scrollToEnd(20));
        assertTrue(bottomText.exists());
    }

    @Ignore // b/266965027
    @Test
    public void testFlingToEnd() throws Exception {
        launchTestActivity(FlingTestActivity.class);

        UiScrollable flingRegion = new UiScrollable(new UiSelector().resourceId(TEST_APP + ":id"
                + "/fling_region")).setAsVerticalList();

        assertTrue(flingRegion.flingToEnd(20));
        assertEquals("fling_down", flingRegion.getText());
    }

    @Test
    public void testSetSwipeDeadZonePercentageAndGetSwipeDeadZonePercentage() {
        UiScrollable scrollable =
                new UiScrollable(new UiSelector()).setSwipeDeadZonePercentage(0.2);
        assertEquals(0.2, scrollable.getSwipeDeadZonePercentage(), 0.01);
    }
}
