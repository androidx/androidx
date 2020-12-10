/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.customview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ExploreByTouchHelperTest {
    @Rule
    public final ActivityTestRule<ExploreByTouchHelperTestActivity> mActivityTestRule;

    private View mHost;

    public ExploreByTouchHelperTest() {
        mActivityTestRule = new ActivityTestRule<>(ExploreByTouchHelperTestActivity.class);
    }

    @Before
    public void setUp() {
        mHost = mActivityTestRule.getActivity().findViewById(R.id.host_view);
    }

    @Test
    @UiThreadTest
    public void testAssignBoundsInParent() {
        final TwoNestedViewHelper boundsInParentOnlyHelper = new ParentBoundsHelper(mHost);
        testBounds(boundsInParentOnlyHelper);
    }

    @Test
    @UiThreadTest
    public void testAssignBoundsInScreen() {
        final TwoNestedViewHelper boundsInScreenOnlyHelper = new ScreenBoundsHelper(mHost);
        testBounds(boundsInScreenOnlyHelper);
    }

    @Test
    @UiThreadTest
    public void testAssignBoundsInScreenAndParent() {
        final TwoNestedViewHelper boundsInScreenAndParentHelper =
                new ParentAndScreenBoundsHelper(mHost);
        testBounds(boundsInScreenAndParentHelper);
    }

    private void testBounds(TwoNestedViewHelper helper) {
        ViewCompat.setAccessibilityDelegate(mHost, helper);
        testBounds(helper, 0);
        testBounds(helper, 1);
        mHost.scrollTo(100, 50);
        testBounds(helper, 0);
        testBounds(helper, 1);
        mHost.scrollTo(0, 0);
        ViewCompat.setAccessibilityDelegate(mHost, null);
    }

    private void testBounds(TwoNestedViewHelper helper, int virtualViewId) {
        AccessibilityNodeInfoCompat node =
                helper.getAccessibilityNodeProvider(mHost).createAccessibilityNodeInfo(
                        virtualViewId);
        assertNotNull(node);

        VirtualItem item = helper.mVirtualItems[virtualViewId];
        final Rect nodeBoundsInParent = new Rect();
        node.getBoundsInParent(nodeBoundsInParent);
        assertEquals("Wrong bounds in parent", item.mBoundsInParent, nodeBoundsInParent);

        final Rect expectedNodeBoundsInScreen = getBoundsOnScreen(helper, virtualViewId,
                item.mParentId);
        final Rect nodeBoundsInScreen = new Rect();
        node.getBoundsInScreen(nodeBoundsInScreen);
        assertEquals("Wrong bounds in screen", expectedNodeBoundsInScreen,
                nodeBoundsInScreen);

        node.recycle();
    }

    private Rect getBoundsOnScreen(TwoNestedViewHelper helper, int virtualViewId,
            int virtualParentId) {
        final Rect boundsOnScreen = new Rect();
        boundsOnScreen.set(helper.mVirtualItems[virtualViewId].mBoundsInParent);
        if (virtualParentId != ExploreByTouchHelper.HOST_ID) {
            boundsOnScreen.offset(helper.mVirtualItems[virtualParentId].mBoundsInParent.left,
                    helper.mVirtualItems[virtualParentId].mBoundsInParent.top);
        }
        final int[] tempLocation = new int[2];
        mHost.getLocationOnScreen(tempLocation);
        boundsOnScreen.offset(tempLocation[0] - mHost.getScrollX(),
                tempLocation[1] - mHost.getScrollY());
        final Rect tempVisibleRect = new Rect();
        mHost.getLocalVisibleRect(tempVisibleRect);
        tempVisibleRect.offset(tempLocation[0] - mHost.getScrollX(),
                tempLocation[1] - mHost.getScrollY());
        boundsOnScreen.intersect(tempVisibleRect);
        return boundsOnScreen;
    }

    @Test
    @UiThreadTest
    public void testMoveFocusToNextVirtualId() {
        final ExploreByTouchHelper helper = new TwoNestedViewHelper(mHost);
        ViewCompat.setAccessibilityDelegate(mHost, helper);

        boolean moveFocusToId0 = helper.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
        assertEquals(0, helper.getKeyboardFocusedVirtualViewId());
        assertEquals(true, moveFocusToId0);

        boolean moveFocusToId1 = helper.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
        assertEquals(1, helper.getKeyboardFocusedVirtualViewId());
        assertEquals(true, moveFocusToId1);

        boolean moveFocusToInvalidId = helper.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
        assertEquals(ExploreByTouchHelper.INVALID_ID, helper.getKeyboardFocusedVirtualViewId());
        assertEquals(false, moveFocusToInvalidId);

        ViewCompat.setAccessibilityDelegate(mHost, null);
    }

    @Test
    @UiThreadTest
    public void testMoveFocusDirection() {
        final ExploreByTouchHelper helper = new TwoNestedViewHelper(mHost);
        ViewCompat.setAccessibilityDelegate(mHost, helper);
        helper.requestKeyboardFocusForVirtualView(0);

        boolean moveFocusUp = helper.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
        assertEquals(ExploreByTouchHelper.INVALID_ID, helper.getKeyboardFocusedVirtualViewId());
        assertEquals(false, moveFocusUp);

        boolean moveFocusDown = helper.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals(0, helper.getKeyboardFocusedVirtualViewId());
        assertEquals(true, moveFocusDown);

        ViewCompat.setAccessibilityDelegate(mHost, null);
    }

    /**
     * An extension of ExploreByTouchHelper that contains 2 nested virtual view
     * and specify {@link AccessibilityNodeInfoCompat#setBoundsInParent}.
     */
    private static class ParentBoundsHelper extends TwoNestedViewHelper {

        ParentBoundsHelper(View host) {
            super(host);
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            populateNodeForVirtualView(/* setBoundsFromParent= */true,
                    /* setBoundsFromScreen= */ false, virtualViewId, node);
        }
    }

    /**
     * An extension of ExploreByTouchHelper that contains 2 nested virtual view
     * and specify {@link AccessibilityNodeInfoCompat#setBoundsInScreen} by calling
     * {@link ExploreByTouchHelper#setBoundsInScreenFromBoundsInParent}.
     */
    private static class ScreenBoundsHelper extends TwoNestedViewHelper {

        ScreenBoundsHelper(View host) {
            super(host);
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            populateNodeForVirtualView(/* setBoundsFromParent= */false,
                    /* setBoundsFromScreen= */ true, virtualViewId, node);
        }
    }

    /**
     * An extension of ExploreByTouchHelper that contains 2 nested virtual view
     * and specify {@link AccessibilityNodeInfoCompat#setBoundsInParent}
     * and {@link AccessibilityNodeInfoCompat#setBoundsInScreen} by calling
     * {@link ExploreByTouchHelper#setBoundsInScreenFromBoundsInParent}.
     */
    private static class ParentAndScreenBoundsHelper extends TwoNestedViewHelper {

        ParentAndScreenBoundsHelper(View host) {
            super(host);
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            populateNodeForVirtualView(/* setBoundsFromParent= */true,
                    /* setBoundsFromScreen= */ true, virtualViewId, node);
        }
    }

    private static class VirtualItem {
        private int mParentId;
        private Rect mBoundsInParent;
        private String mText;

        VirtualItem(int parentId, String text, Rect boundsInParent) {
            this.mParentId = parentId;
            this.mBoundsInParent = boundsInParent;
            this.mText = text;
        }
    }

    /**
     * An extension of ExploreByTouchHelper that contains 2 nested virtual views.
     * Host view contains 1 child "bottom" and "bottom" contains one child
     * "nested-bottom-right".
     */
    private static class TwoNestedViewHelper extends ExploreByTouchHelper {
        private final View mHost;
        protected VirtualItem[] mVirtualItems = new VirtualItem[2];

        TwoNestedViewHelper(View host) {
            super(host);
            mHost = host;
            mVirtualItems[0] = new VirtualItem(ExploreByTouchHelper.HOST_ID, "bottom",
                    new Rect(0, mHost.getHeight() / 2,
                            mHost.getWidth(), mHost.getHeight()));
            mVirtualItems[1] = new VirtualItem(0, "nested-bottom-right",
                    new Rect(mHost.getWidth() / 2, 0,
                            mHost.getWidth(), mHost.getHeight() / 2));
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            if (x < mHost.getWidth() / 2 && y > mHost.getHeight() / 2) {
                return 0;
            } else if (x > mHost.getWidth() / 2 && y > mHost.getHeight() / 2) {
                return 1;
            }
            return -1;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            virtualViewIds.add(0);
            virtualViewIds.add(1);
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            populateNodeForVirtualView(/* setBoundsFromParent= */false,
                    /* setBoundsFromScreen= */ true, virtualViewId, node);
        }

        protected void populateNodeForVirtualView(boolean setBoundsFromParent,
                boolean setBoundsFromScreen, int virtualViewId,
                @NonNull AccessibilityNodeInfoCompat node) {
            if (virtualViewId <= mVirtualItems.length) {
                int index = virtualViewId;
                node.setContentDescription(mVirtualItems[index].mText);
                node.setParent(mHost, mVirtualItems[index].mParentId);
                if (setBoundsFromParent) {
                    node.setBoundsInParent(mVirtualItems[index].mBoundsInParent);
                }
                if (setBoundsFromScreen) {
                    setBoundsInScreenFromBoundsInParent(node, mVirtualItems[index].mBoundsInParent);
                }
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
                Bundle arguments) {
            return false;
        }
    }
}
