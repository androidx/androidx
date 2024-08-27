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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
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
    public void testSetCollectionInfoIsNullable() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setCollectionInfo(null);
    }

    @Test
    public void testSetCollectionItemInfoIsNullable() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setCollectionItemInfo(null);
    }

    @Test
    public void testSetCollectionItemInfoCompatBuilder_withDefaultValues() {
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfoCompat =
                new AccessibilityNodeInfoCompat.CollectionItemInfoCompat.Builder().build();

        assertThat(collectionItemInfoCompat.getColumnIndex()).isEqualTo(0);
        assertThat(collectionItemInfoCompat.getColumnSpan()).isEqualTo(0);
        assertThat(collectionItemInfoCompat.getColumnTitle()).isNull();

        assertThat(collectionItemInfoCompat.getRowIndex()).isEqualTo(0);
        assertThat(collectionItemInfoCompat.getRowSpan()).isEqualTo(0);
        assertThat(collectionItemInfoCompat.getRowTitle()).isNull();
        assertThat(collectionItemInfoCompat.isSelected()).isFalse();
        assertThat(collectionItemInfoCompat.isHeading()).isFalse();
    }

    @Test
    public void testSetCollectionInfoCompatBuilder_withRealValues() {
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfoCompat =
                new AccessibilityNodeInfoCompat.CollectionItemInfoCompat.Builder()
                        .setColumnIndex(2)
                        .setColumnSpan(1)
                        .setColumnTitle("Column title")
                        .setRowIndex(1)
                        .setRowSpan(2)
                        .setRowTitle("Row title")
                        .setSelected(true)
                        .setHeading(true)
                        .build();

        if (Build.VERSION.SDK_INT >= 33) {
            assertThat(collectionItemInfoCompat.getColumnTitle()).isEqualTo("Column title");
            assertThat(collectionItemInfoCompat.getRowTitle()).isEqualTo("Row title");
        }

        assertThat(collectionItemInfoCompat.isSelected()).isTrue();

        assertThat(collectionItemInfoCompat.getColumnIndex()).isEqualTo(2);
        assertThat(collectionItemInfoCompat.getColumnSpan()).isEqualTo(1);
        assertThat(collectionItemInfoCompat.getRowIndex()).isEqualTo(1);
        assertThat(collectionItemInfoCompat.getRowSpan()).isEqualTo(2);
        assertThat(collectionItemInfoCompat.isHeading()).isTrue();
    }

    @Test
    public void testRangeInfoCompatConstructor_always_returnsRangeInfoCompat() {
        AccessibilityNodeInfoCompat.RangeInfoCompat rangeInfoCompat =
                new AccessibilityNodeInfoCompat.RangeInfoCompat(
                        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT, 0, 100, 50);
        assertThat(rangeInfoCompat.getType()).isEqualTo(
                AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT);
        assertThat(rangeInfoCompat.getMin()).isEqualTo(0f);
        assertThat(rangeInfoCompat.getMax()).isEqualTo(100f);
        assertThat(rangeInfoCompat.getCurrent()).isEqualTo(50f);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void testGetChild_withPrefetchingStrategy_returnsChild() {
        AccessibilityNodeInfo accessibilityNodeInfo = mock(AccessibilityNodeInfo.class);
        AccessibilityNodeInfoCompat nodeCompat = AccessibilityNodeInfoCompat.wrap(
                accessibilityNodeInfo);
        nodeCompat.getChild(0, AccessibilityNodeInfoCompat.FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST);
        verify(accessibilityNodeInfo).getChild(0,
                AccessibilityNodeInfoCompat.FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void testGetChild_withPrefetchingStrategy_returnsParent() {
        AccessibilityNodeInfo accessibilityNodeInfo = mock(AccessibilityNodeInfo.class);
        AccessibilityNodeInfoCompat nodeCompat = AccessibilityNodeInfoCompat.wrap(
                accessibilityNodeInfo);
        nodeCompat.getParent(AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS);
        verify(accessibilityNodeInfo).getParent(
                AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS);
    }

    @Test
    public void testGetSetHintText() {
        final CharSequence hintText = "hint text";
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setHintText(hintText);
        assertThat(nodeCompat.getHintText()).isEqualTo(hintText);
    }

    @Test
    public void testGetSetPaneTitle() {
        final CharSequence paneTitle = "pane title";
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setPaneTitle(paneTitle);
        assertThat(nodeCompat.getPaneTitle()).isEqualTo(paneTitle);
    }

    @Test
    public void testGetSetTooltipText() {
        final CharSequence tooltipText = "tooltip";
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setTooltipText(tooltipText);
        assertThat(nodeCompat.getTooltipText()).isEqualTo(tooltipText);
    }

    @Test
    public void testGetSetShowingHintText() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setShowingHintText(true);
        assertThat(nodeCompat.isShowingHintText()).isTrue();
        nodeCompat.setShowingHintText(false);
        assertThat(nodeCompat.isShowingHintText()).isFalse();
    }

    @Test
    public void testGetSetScreenReaderFocusable() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setScreenReaderFocusable(true);
        assertThat(nodeCompat.isScreenReaderFocusable()).isTrue();
        nodeCompat.setScreenReaderFocusable(false);
        assertThat(nodeCompat.isScreenReaderFocusable()).isFalse();
    }

    @Test
    public void testGetSetMinDurationBetweenContentChanges() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setMinDurationBetweenContentChangesMillis(200L);
        assertThat(nodeCompat.getMinDurationBetweenContentChangesMillis()).isEqualTo(200L);
    }

    @Test
    public void testGetSetRequestInitialAccessibilityFocus() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setRequestInitialAccessibilityFocus(true);
        assertThat(nodeCompat.hasRequestInitialAccessibilityFocus()).isTrue();

    }

    @Test
    public void testGetSetContainerTitle() {
        final CharSequence containerTitle = "title";
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setContainerTitle(containerTitle);
        assertThat(nodeCompat.getContainerTitle()).isEqualTo(containerTitle);
    }

    @Test
    public void testGetBoundsInWindow() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        Rect bounds = new Rect(0, 0, 50, 50);
        nodeCompat.setBoundsInWindow(bounds);
        Rect outBounds = new Rect();
        nodeCompat.getBoundsInWindow(outBounds);
        assertThat(bounds).isEqualTo(outBounds);
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    public void testSetQueryFromAppProcessEnabled() {
        AccessibilityNodeInfo accessibilityNodeInfo = mock(AccessibilityNodeInfo.class);
        AccessibilityNodeInfoCompat nodeCompat = AccessibilityNodeInfoCompat.wrap(
                accessibilityNodeInfo);
        nodeCompat.setQueryFromAppProcessEnabled(null, true);
        verify(accessibilityNodeInfo).setQueryFromAppProcessEnabled(null, true);
    }

    @Test
    public void testisGranularScrollingSupported() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        assertThat(nodeCompat.isGranularScrollingSupported()).isFalse();
        nodeCompat.setGranularScrollingSupported(true);
        assertThat(nodeCompat.isGranularScrollingSupported()).isTrue();
    }

    @Test
    public void testGetSetHeading() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setHeading(true);
        assertThat(nodeCompat.isHeading()).isTrue();
        nodeCompat.setHeading(false);
        assertThat(nodeCompat.isHeading()).isFalse();
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfo =
                AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(0, 1, 0, 1, true);
        nodeCompat.setCollectionItemInfo(collectionItemInfo);
        assertThat(nodeCompat.isHeading()).isTrue();
    }

    @Test
    public void testGetSetTextEntryKey() {
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setTextEntryKey(true);
        assertThat(nodeCompat.isTextEntryKey()).isTrue();
        nodeCompat.setTextEntryKey(false);
        assertThat(nodeCompat.isTextEntryKey()).isFalse();
    }

    @Test
    public void testGetSetAccessibilityDataSensitive() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();

        accessibilityNodeInfoCompat.setAccessibilityDataSensitive(true);
        assertThat(accessibilityNodeInfoCompat.isAccessibilityDataSensitive()).isTrue();
        accessibilityNodeInfoCompat.setAccessibilityDataSensitive(false);
        assertThat(accessibilityNodeInfoCompat.isAccessibilityDataSensitive()).isFalse();
    }

    @Test
    public void testGetSetUniqueId() {
        final String uniqueId = "localUId";
        AccessibilityNodeInfoCompat nodeCompat = obtainedWrappedNodeCompat();
        nodeCompat.setUniqueId(uniqueId);
        assertThat(nodeCompat.getUniqueId()).isEqualTo(uniqueId);
    }

    @Test
    public void testAccessibilityActionsNotNull() {
        try {
            AccessibilityActionCompat actionCompat;
            actionCompat = AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN;
            assertThat(actionCompat.getId()).isEqualTo(
                    android.R.id.accessibilityActionShowOnScreen);
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION;
            assertThat(actionCompat.getId()).isEqualTo(
                    android.R.id.accessibilityActionScrollToPosition);
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_UP;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionScrollUp);
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_LEFT;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionScrollLeft);
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_DOWN;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionScrollDown);
            actionCompat = AccessibilityActionCompat.ACTION_SCROLL_RIGHT;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionScrollRight);
            actionCompat = AccessibilityActionCompat.ACTION_CONTEXT_CLICK;
            assertThat(actionCompat.getId()).isEqualTo(
                    android.R.id.accessibilityActionContextClick);
            actionCompat = AccessibilityActionCompat.ACTION_SET_PROGRESS;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionSetProgress);
            actionCompat = AccessibilityActionCompat.ACTION_MOVE_WINDOW;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionMoveWindow);
            actionCompat = AccessibilityActionCompat.ACTION_SHOW_TOOLTIP;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionShowTooltip);
            actionCompat = AccessibilityActionCompat.ACTION_HIDE_TOOLTIP;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionHideTooltip);
            actionCompat = AccessibilityActionCompat.ACTION_PRESS_AND_HOLD;
            assertThat(actionCompat.getId()).isEqualTo(
                    android.R.id.accessibilityActionPressAndHold);
            actionCompat = AccessibilityActionCompat.ACTION_IME_ENTER;
            assertThat(actionCompat.getId()).isEqualTo(android.R.id.accessibilityActionImeEnter);
        } catch (NullPointerException e) {
            Assert.fail("Expected no NullPointerException, but got: " + e.getMessage());
        }
    }

    @Test
    public void testAccessibilityActionToString() {
        AccessibilityActionCompat actionCompat;
        actionCompat = AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN;
        final String showOnScreen = "AccessibilityActionCompat: ACTION_SHOW_ON_SCREEN";
        assertThat(actionCompat.toString()).isEqualTo(showOnScreen);
        final String customAction = "CustomAction";
        actionCompat = new AccessibilityActionCompat(123123123, customAction);
        assertThat(actionCompat.toString()).isEqualTo("AccessibilityActionCompat: "
                + customAction);
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
            assertThat(touchDelegateInfoResult.getRegionCount()).isEqualTo(1);
            assertThat(touchDelegateInfoResult.getRegionAt(0)).isEqualTo(region);
            // getTargetForRegion return null, since we are not a11y service
            assertThat(touchDelegateInfoResult.getTargetForRegion(region)).isNull();
        } else {
            assertThat(touchDelegateInfoResult).isNull();
            assertThat(delegateInfo.getRegionCount()).isEqualTo(0);
            assertThat(delegateInfo.getRegionAt(0)).isNull();
            assertThat(delegateInfo.getTargetForRegion(region)).isNull();
        }
    }

    @Test
    public void testWrappedActionEqualsStaticAction() {
        // Static AccessibilityActionCompat
        AccessibilityActionCompat staticAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        // Wrapped AccessibilityAction
        AccessibilityActionCompat wrappedAction = new AccessibilityActionCompat(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        assertThat(staticAction.equals(wrappedAction)).isTrue();
        assertThat(staticAction.hashCode() == wrappedAction.hashCode()).isTrue();
    }

    @Test
    public void testActionIdAndLabelEqualsStaticAction() {
        AccessibilityActionCompat staticAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        // AccessibilityActionCompat defined by id and label
        AccessibilityActionCompat wrappedIdAndLabelAction = new AccessibilityActionCompat(
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, "label", null);
        assertThat(staticAction.equals(wrappedIdAndLabelAction)).isTrue();
        assertThat(staticAction.hashCode() == wrappedIdAndLabelAction.hashCode()).isTrue();
    }

    @Test
    public void testDifferentActionIdsNotEquals() {
        AccessibilityActionCompat staticLongClickAction =
                AccessibilityActionCompat.ACTION_LONG_CLICK;
        AccessibilityActionCompat staticClickAction = AccessibilityActionCompat.ACTION_CLICK;

        assertThat(staticLongClickAction.equals(staticClickAction)).isFalse();
        assertThat(staticLongClickAction.hashCode() == staticClickAction.hashCode())
                .isFalse();

        // AccessibilityActionCompat defined by id
        AccessibilityActionCompat wrappedIdLongClickAction = new AccessibilityActionCompat(
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, null, null);
        assertThat(wrappedIdLongClickAction.equals(staticClickAction)).isFalse();
        assertThat(wrappedIdLongClickAction.hashCode() == staticClickAction.hashCode())
                .isFalse();

        // Wrapped AccessibilityAction
        AccessibilityActionCompat wrappedLongClickAction = new AccessibilityActionCompat(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        assertThat(wrappedLongClickAction.equals(staticClickAction)).isFalse();
        assertThat(wrappedLongClickAction.hashCode() == staticClickAction.hashCode())
                .isFalse();
    }

    private AccessibilityNodeInfoCompat obtainedWrappedNodeCompat() {
        AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
        return AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo);
    }

    @SdkSuppress(minSdkVersion = 26)
    @SmallTest
    @Test
    public void testGetSetAvailableExtraData() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        final List<String> testData = Arrays.asList(new String[]{"A", "B"});

        accessibilityNodeInfoCompat.setAvailableExtraData(testData);
        assertThat(accessibilityNodeInfoCompat.getAvailableExtraData()).isEqualTo(testData);
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testGetExtraRenderingInfo() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        assertThat(
                accessibilityNodeInfoCompat.getExtraRenderingInfo()).isEqualTo(
                        accessibilityNodeInfoCompat.unwrap().getExtraRenderingInfo());
    }

    @SmallTest
    @Test
    public void testSetGetTextSelectable() {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = obtainedWrappedNodeCompat();
        accessibilityNodeInfoCompat.setTextSelectable(false);
        assertThat(accessibilityNodeInfoCompat.isTextSelectable()).isFalse();
        accessibilityNodeInfoCompat.setTextSelectable(true);
        assertThat(accessibilityNodeInfoCompat.isTextSelectable()).isTrue();
    }

    @SmallTest
    @Test
    public void testActionScrollInDirection() {
        AccessibilityActionCompat actionCompat =
                AccessibilityActionCompat.ACTION_SCROLL_IN_DIRECTION;
        assertThat(actionCompat.getId()).isEqualTo(
                android.R.id.accessibilityActionScrollInDirection);
        assertThat(actionCompat.toString()).isEqualTo("AccessibilityActionCompat: "
                + "ACTION_SCROLL_IN_DIRECTION");
    }
}
