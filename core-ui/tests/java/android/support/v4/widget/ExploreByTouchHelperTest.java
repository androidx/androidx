/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.coreui.test.R;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

@SmallTest
public class ExploreByTouchHelperTest extends BaseInstrumentationTestCase<ExploreByTouchHelperTestActivity> {
    private View mHost;

    public ExploreByTouchHelperTest() {
        super(ExploreByTouchHelperTestActivity.class);
    }

    @Before
    public void setUp() {
        // Accessibility delegates are only supported on API 14+.
        assumeTrue(Build.VERSION.SDK_INT >= 14);
        mHost = mActivityTestRule.getActivity().findViewById(R.id.host_view);
    }

    @Test
    @UiThreadTest
    public void testBoundsInScreen() {
        final ExploreByTouchHelper helper = new ParentBoundsHelper(mHost);
        ViewCompat.setAccessibilityDelegate(mHost, helper);

        final AccessibilityNodeInfoCompat node =
                helper.getAccessibilityNodeProvider(mHost).createAccessibilityNodeInfo(1);
        assertNotNull(node);

        final Rect hostBounds = new Rect();
        mHost.getLocalVisibleRect(hostBounds);
        assertFalse("Host has not been laid out", hostBounds.isEmpty());

        final Rect nodeBoundsInParent = new Rect();
        node.getBoundsInParent(nodeBoundsInParent);
        assertEquals("Wrong bounds in parent", hostBounds, nodeBoundsInParent);

        final Rect hostBoundsOnScreen = getBoundsOnScreen(mHost);
        final Rect nodeBoundsInScreen = new Rect();
        node.getBoundsInScreen(nodeBoundsInScreen);
        assertEquals("Wrong bounds in screen", hostBoundsOnScreen, nodeBoundsInScreen);

        final int scrollX = 100;
        final int scrollY = 50;
        mHost.scrollTo(scrollX, scrollY);

        // Generate a node for the new position.
        final AccessibilityNodeInfoCompat scrolledNode =
                helper.getAccessibilityNodeProvider(mHost).createAccessibilityNodeInfo(1);
        assertNotNull(scrolledNode);

        // Bounds in parent should not be affected by visibility.
        final Rect scrolledNodeBoundsInParent = new Rect();
        scrolledNode.getBoundsInParent(scrolledNodeBoundsInParent);
        assertEquals("Wrong bounds in parent after scrolling",
                hostBounds, scrolledNodeBoundsInParent);

        final Rect expectedBoundsInScreen = new Rect(hostBoundsOnScreen);
        expectedBoundsInScreen.offset(-scrollX, -scrollY);
        expectedBoundsInScreen.intersect(hostBoundsOnScreen);
        scrolledNode.getBoundsInScreen(nodeBoundsInScreen);
        assertEquals("Wrong bounds in screen after scrolling",
                expectedBoundsInScreen, nodeBoundsInScreen);

        ViewCompat.setAccessibilityDelegate(mHost, null);
    }

    private static Rect getBoundsOnScreen(View v) {
        final int[] tempLocation = new int[2];
        final Rect hostBoundsOnScreen = new Rect(0, 0, v.getWidth(), v.getHeight());
        v.getLocationOnScreen(tempLocation);
        hostBoundsOnScreen.offset(tempLocation[0], tempLocation[1]);
        return hostBoundsOnScreen;
    }

    /**
     * An extension of ExploreByTouchHelper that contains a single virtual view
     * whose bounds match the host view.
     */
    private static class ParentBoundsHelper extends ExploreByTouchHelper {
        private final View mHost;

        public ParentBoundsHelper(View host) {
            super(host);

            mHost = host;
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            return 1;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            virtualViewIds.add(1);
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            if (virtualViewId == 1) {
                node.setContentDescription("test");

                final Rect hostBounds = new Rect(0, 0, mHost.getWidth(), mHost.getHeight());
                node.setBoundsInParent(hostBounds);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            return false;
        }
    }
}
