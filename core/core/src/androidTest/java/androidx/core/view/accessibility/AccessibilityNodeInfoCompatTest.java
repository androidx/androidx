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

package androidx.core.view.accessibility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import android.graphics.Region;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.TouchDelegateInfoCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AccessibilityNodeInfoCompatTest {
    @Test
    public void testSetCollectionInfoIsNullable() throws Exception {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setCollectionInfo(null);
    }

    @Test
    public void testSetCollectionItemInfoIsNullable() throws Exception {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setCollectionItemInfo(null);
    }

    @Test
    public void testGetSetHintText() {
        final CharSequence hintText = (Build.VERSION.SDK_INT >= 19) ? "hint text" : null;
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setHintText(hintText);
        assertThat(nodeCompat.getHintText(), equalTo(hintText));
    }

    @Test
    public void testGetSetPaneTitle() {
        final CharSequence paneTitle = (Build.VERSION.SDK_INT >= 19) ? "pane title" : null;
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setPaneTitle(paneTitle);
        assertThat(nodeCompat.getPaneTitle(), equalTo(paneTitle));
    }

    @Test
    public void testGetSetTooltipText() {
        final CharSequence tooltipText = (Build.VERSION.SDK_INT >= 19) ? "tooltip" : null;
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setTooltipText(tooltipText);
        assertThat(nodeCompat.getTooltipText(), equalTo(tooltipText));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testGetSetShowingHintText() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setShowingHintText(true);
        assertThat(nodeCompat.isShowingHintText(), is(true));
        nodeCompat.setShowingHintText(false);
        assertThat(nodeCompat.isShowingHintText(), is(false));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testGetSetScreenReaderFocusable() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setScreenReaderFocusable(true);
        assertThat(nodeCompat.isScreenReaderFocusable(), is(true));
        nodeCompat.setScreenReaderFocusable(false);
        assertThat(nodeCompat.isScreenReaderFocusable(), is(false));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testGetSetHeading() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setHeading(true);
        assertThat(nodeCompat.isHeading(), is(true));
        nodeCompat.setHeading(false);
        assertThat(nodeCompat.isHeading(), is(false));
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfo =
                AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(0, 1, 0, 1, true);
        nodeCompat.setCollectionItemInfo(collectionItemInfo);
        assertThat(nodeCompat.isHeading(), is(true));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testGetSetTextEntryKey() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setTextEntryKey(true);
        assertThat(nodeCompat.isTextEntryKey(), is(true));
        nodeCompat.setTextEntryKey(false);
        assertThat(nodeCompat.isTextEntryKey(), is(false));
    }

    @Test
    public void testGetSetUniqueId() {
        final String uniqueId = (Build.VERSION.SDK_INT >= 19) ? "localUId" : null;
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setUniqueId(uniqueId);
        assertThat(nodeCompat.getUniqueId(), equalTo(uniqueId));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testAccessibilityActionsNotNull() {
        try {
            AccessibilityActionCompat actionCompat;
            actionCompat = AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionShowOnScreen)));
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionScrollToPosition)));
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_UP;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionScrollUp)));
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_LEFT;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionScrollLeft)));
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_DOWN;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionScrollDown)));
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_RIGHT;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionScrollRight)));
            actionCompat = AccessibilityActionCompat.ACTION_CONTEXT_CLICK;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionContextClick)));
            actionCompat = AccessibilityActionCompat.ACTION_SET_PROGRESS;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionSetProgress)));
            actionCompat = AccessibilityActionCompat.ACTION_MOVE_WINDOW;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionMoveWindow)));
            actionCompat = AccessibilityActionCompat.ACTION_SHOW_TOOLTIP;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionShowTooltip)));
            actionCompat = AccessibilityActionCompat.ACTION_HIDE_TOOLTIP;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionHideTooltip)));
            actionCompat = AccessibilityActionCompat.ACTION_PRESS_AND_HOLD;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionPressAndHold)));
            actionCompat = AccessibilityActionCompat.ACTION_IME_ENTER;
            assertThat(actionCompat.getId(),
                    is(getExpectedActionId(android.R.id.accessibilityActionImeEnter)));
        } catch (NullPointerException e) {
            Assert.fail("Expected no NullPointerException, but got: " + e.getMessage());
        }
    }

    @Test
    public void testTouchDelegateInfo() {
        final Map<Region, View> targetMap = new HashMap<>(1);
        final Region region = new Region(1, 1, 10, 10);
        targetMap.put(region, new View(InstrumentationRegistry.getContext()));
        final TouchDelegateInfoCompat delegateInfo = new TouchDelegateInfoCompat(targetMap);
        final AccessibilityNodeInfoCompat accessibilityNodeInfoCompat =
                obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setTouchDelegateInfo(delegateInfo);
        final TouchDelegateInfoCompat touchDelegateInfoResult =
                accessibilityNodeInfoCompat.getTouchDelegateInfo();
        if (Build.VERSION.SDK_INT >= 29) {
            assertThat(touchDelegateInfoResult.getRegionCount(), is(1));
            assertThat(touchDelegateInfoResult.getRegionAt(0), is(region));
            // getTargetForRegion return null, since we are not a11y service
            assertThat(touchDelegateInfoResult.getTargetForRegion(region), is(nullValue()));
        } else {
            assertThat(touchDelegateInfoResult, is(nullValue()));
            assertThat(delegateInfo.getRegionCount(), is(0));
            assertThat(delegateInfo.getRegionAt(0), is(nullValue()));
            assertThat(delegateInfo.getTargetForRegion(region), is(nullValue()));
        }
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void testWrappedActionEqualsStaticAction() {
        // Static AccessibilityActionCompat
        AccessibilityActionCompat staticAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        // Wrapped AccessibilityAction
        AccessibilityActionCompat wrappedAction = new AccessibilityActionCompat(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        assertThat(staticAction.equals(wrappedAction), equalTo(true));
        assertThat(staticAction.hashCode() == wrappedAction.hashCode(), equalTo(true));
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void testActionIdAndLabelEqualsStaticAction() {
        AccessibilityActionCompat staticAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        // AccessibilityActionCompat defined by id and label
        AccessibilityActionCompat wrappedIdAndLabelAction = new AccessibilityActionCompat(
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, "label", null);
        assertThat(staticAction.equals(wrappedIdAndLabelAction), equalTo(true));
        assertThat(staticAction.hashCode() == wrappedIdAndLabelAction.hashCode(), equalTo(true));
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void testDifferentActionIdsNotEquals() {
        AccessibilityActionCompat staticLongClickAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        AccessibilityActionCompat staticClickAction = AccessibilityActionCompat.ACTION_CLICK;

        assertThat(staticLongClickAction.equals(staticClickAction), equalTo(false));
        assertThat(staticLongClickAction.hashCode() == staticClickAction.hashCode(),
                equalTo(false));

        // AccessibilityActionCompat defined by id
        AccessibilityActionCompat wrappedIdLongClickAction = new AccessibilityActionCompat(
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, null, null);
        assertThat(wrappedIdLongClickAction.equals(staticClickAction), equalTo(false));
        assertThat(wrappedIdLongClickAction.hashCode() == staticClickAction.hashCode(),
                equalTo(false));

        // Wrapped AccessibilityAction
        AccessibilityActionCompat wrappedLongClickAction = new AccessibilityActionCompat(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        assertThat(wrappedLongClickAction.equals(staticClickAction), equalTo(false));
        assertThat(wrappedLongClickAction.hashCode() == staticClickAction.hashCode(),
                equalTo(false));
    }

    private AccessibilityNodeInfoCompat obtainedWrappedNodeCompat() {
        AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
        return AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo);
    }

    private int getExpectedActionId(int id) {
        return Build.VERSION.SDK_INT >= 21 ? id : 0;
    }

    @SdkSuppress(minSdkVersion = 26)
    @SmallTest
    @Test
    public void testGetSetAvailableExtraData() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        final List<String> testData = Arrays.asList(new String[]{"A", "B"});

        accessibilityNodeInfoCompat.setAvailableExtraData(testData);
        assertThat(accessibilityNodeInfoCompat.getAvailableExtraData(), equalTo(testData));
    }
}
