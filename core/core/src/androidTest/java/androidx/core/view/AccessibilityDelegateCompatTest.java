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

package androidx.core.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.core.view.accessibility.AccessibilityViewCommand.MoveAtGranularityArguments;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@MediumTest
@SdkSuppress(minSdkVersion = 18)
public class AccessibilityDelegateCompatTest extends
        BaseInstrumentationTestCase<ViewCompatActivity> {
    private static final int TIMEOUT_ASYNC_PROCESSING = 5000;

    private static UiAutomation sUiAutomation;

    private ViewGroup mView;
    private View mChildView;

    public AccessibilityDelegateCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() throws Throwable {
        Instrumentation instr = InstrumentationRegistry.getInstrumentation();
        sUiAutomation = instr.getUiAutomation();

        final Activity activity = mActivityTestRule.getActivity();
        mActivityTestRule.runOnUiThread(() -> {
            // Use a group, so it has a child
            mView = (ViewGroup) activity.findViewById(androidx.core.test.R.id.view).getParent();
            // On KitKat, some delegate methods aren't called for non-important views
            ViewCompat.setImportantForAccessibility(mView, View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        });
    }

    @Test
    public void testViewWithDelegate_reportsHasDelegate() throws Throwable {
        assertFalse(ViewCompat.hasAccessibilityDelegate(mView));
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertTrue(ViewCompat.hasAccessibilityDelegate(mView));
    }

    @Test
    public void testViewWithDelegateCompat_callsDelegateMethods() {
        final AccessibilityDelegateCompat mockCompat = mock(AccessibilityDelegateCompat.class);
        ViewCompat.setAccessibilityDelegate(mView, new BridgingDelegateCompat(mockCompat));
        assertMockBridgedAccessibilityDelegateCompatWorkingOnView(mockCompat);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testScreenReaderFocusable_propagatesToAccessibilityNodeInfo() {
        assertFalse(ViewCompat.isScreenReaderFocusable(mView));
        assertFalse(getNodeCompatForView(mView).isScreenReaderFocusable());

        ViewCompat.setScreenReaderFocusable(mView, true);

        assertTrue(ViewCompat.isScreenReaderFocusable(mView));
        assertTrue(getNodeCompatForView(mView).isScreenReaderFocusable());

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertTrue(getNodeCompatForView(mView).isScreenReaderFocusable());
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertTrue(getNodeCompatForView(mView).isScreenReaderFocusable());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityHeading_propagatesToAccessibilityNodeInfo() {
        assertFalse(ViewCompat.isAccessibilityHeading(mView));
        assertFalse(getNodeCompatForView(mView).isHeading());

        ViewCompat.setAccessibilityHeading(mView, true);

        assertTrue(ViewCompat.isAccessibilityHeading(mView));
        assertTrue(getNodeCompatForView(mView).isHeading());

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertTrue(getNodeCompatForView(mView).isHeading());
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertTrue(getNodeCompatForView(mView).isHeading());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_propagatesToAccessibilityNodeInfo() {
        assertNull(ViewCompat.getAccessibilityPaneTitle(mView));
        assertNull(getNodeCompatForView(mView).getPaneTitle());

        String title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mView, title);

        assertEquals(ViewCompat.getAccessibilityPaneTitle(mView), title);
        assertEquals(getNodeCompatForView(mView).getPaneTitle(), title);

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertEquals(getNodeCompatForView(mView).getPaneTitle(), title);
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertEquals(getNodeCompatForView(mView).getPaneTitle(), title);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 27)
    public void testAccessibilityPaneTitle_isntTrackedAsPaneWithoutTitle() throws Throwable {
        // This test isn't to test the propagation up, just that the event-sending behavior
        final AccessibilityDelegateCompat mockDelegate = mock(
                AccessibilityDelegateCompat.class);
        mActivityTestRule.runOnUiThread(() -> {
            ViewCompat.setAccessibilityLiveRegion(mView,
                    ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

            ViewCompat.setAccessibilityPaneTitle(mView, "Sample title");

            ViewCompat.setAccessibilityPaneTitle(mView, null);

            ViewCompat.setAccessibilityDelegate(mView, new BridgingDelegateCompat(mockDelegate));

            mView.setVisibility(View.VISIBLE);

            mView.getViewTreeObserver().dispatchOnGlobalLayout();

            ArgumentCaptor<AccessibilityEvent> argumentCaptor =
                    ArgumentCaptor.forClass(AccessibilityEvent.class);
            verify(mockDelegate, never()).sendAccessibilityEventUnchecked(
                    eq(mView), argumentCaptor.capture());
        });
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_isSentOnAppearance() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mView, title);
        mActivityTestRule.runOnUiThread(() -> {
            // Update the AccessibilityPaneVisibilityManager
            mView.setVisibility(View.INVISIBLE);
            mView.getViewTreeObserver().dispatchOnGlobalLayout();
        });

        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() -> {
                            mView.setVisibility(View.VISIBLE);
                            if (Build.VERSION.SDK_INT < 28) {
                                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                            }
                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isWindowStateChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    int isPaneTitle = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED);
                    // onInitializeA11yEvent was not called in 28, so package name was not set
                    boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                            || TextUtils.equals(event.getPackageName(), activity.getPackageName());
                    boolean hasTitleText = false;
                    if (event.getText().size() > 0) {
                        hasTitleText = event.getText().get(0).equals(title);
                    }
                    return isWindowStateChanged
                            && (isPaneTitle != 0)
                            && isFromThisPackage
                            && hasTitleText;
                },
                TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_parentVisible_isSentOnAppearance() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();
        mActivityTestRule.runOnUiThread(() -> {
            mChildView = activity.findViewById(androidx.core.test.R.id.view);
            // On KitKat, some delegate methods aren't called for non-important views
            ViewCompat.setImportantForAccessibility(mChildView,
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        });
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mChildView, title);
        assertEquals(mChildView.getVisibility(), View.VISIBLE);
        assertEquals(mChildView.getWindowVisibility(), View.VISIBLE);


        mActivityTestRule.runOnUiThread(() -> {
            // Update the AccessibilityPaneVisibilityManager
            mView.setVisibility(View.INVISIBLE);
            assertEquals(mChildView.isShown(), false);
        });


        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() -> {
                            if (Build.VERSION.SDK_INT < 28) {
                                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                                mView.setVisibility(View.VISIBLE);
                                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                            } else {
                                mView.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isWindowStateChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    int isPaneTitle = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED);
                    // onInitializeA11yEvent was not called in 28, so package name was not set
                    boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                            || TextUtils.equals(event.getPackageName(), activity.getPackageName());
                    boolean hasTitleText = false;
                    if (event.getText().size() > 0) {
                        hasTitleText = event.getText().get(0).equals(title);
                    }
                    return isWindowStateChanged
                            && (isPaneTitle != 0)
                            && isFromThisPackage
                            && hasTitleText;
                },
                TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_parentGone_isSentOnDisappearance() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();
        mActivityTestRule.runOnUiThread(() -> {
            mChildView = activity.findViewById(androidx.core.test.R.id.view);
            // On KitKat, some delegate methods aren't called for non-important views
            ViewCompat.setImportantForAccessibility(mChildView,
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        });
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mChildView, title);

        // Validity check
        assertEquals(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
                ViewCompat.getImportantForAccessibility(mChildView));
        assertEquals(mChildView.getVisibility(), View.VISIBLE);

        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() -> {
                            mView.setVisibility(View.INVISIBLE);
                            if (Build.VERSION.SDK_INT < 28) {
                                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                            }
                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isWindowStateChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    int isPaneTitle = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED);
                    // onInitializeA11yEvent was not called in 28, so package name was not set
                    boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                            || TextUtils.equals(event.getPackageName(), activity.getPackageName());
                    boolean hasTitleText = false;
                    if (event.getText().size() > 0) {
                        hasTitleText = event.getText().get(0).equals(title);
                    }
                    return isWindowStateChanged
                            && (isPaneTitle != 0)
                            && isFromThisPackage
                            && hasTitleText;
                },
                TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_isSentOnDisappearance() throws Throwable {
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mView, title);

        // Validity check
        assertEquals(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
                ViewCompat.getImportantForAccessibility(mView));

        final Activity activity = mActivityTestRule.getActivity();
        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() -> {
                            mView.setVisibility(View.INVISIBLE);
                            if (Build.VERSION.SDK_INT < 28) {
                                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                            }
                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isWindowStateChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    int isPaneTitle = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED);
                    // onInitializeA11yEvent was not called in 28, so package name was not set
                    boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                            || TextUtils.equals(event.getPackageName(), activity.getPackageName());
                    boolean hasTitleText = false;
                    if (event.getText().size() > 0) {
                        hasTitleText = event.getText().get(0).equals(title);
                    }
                    return isWindowStateChanged
                            && (isPaneTitle != 0)
                            && isFromThisPackage
                            && hasTitleText;
                },
                TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 25)
    public void testPerformSpanAction() {
        final ClickableSpan span1 = mock(ClickableSpan.class);
        final ClickableSpan span2 = mock(ClickableSpan.class);
        mView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                    AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                SpannableString clickableSpannedString =
                        new SpannableString("Spans the whole world");
                clickableSpannedString.setSpan(span1, 10, 13, 1);
                clickableSpannedString.setSpan(span2, 16, 18, 2);
                info.setText(clickableSpannedString);
            }
        });
        ViewCompat.enableAccessibleClickableSpanSupport(mView);
        AccessibilityNodeInfo nodeInfo = spy(AccessibilityNodeInfo.obtain());
        mView.onInitializeAccessibilityNodeInfo(nodeInfo);
        final Spanned text = (Spanned) AccessibilityNodeInfoCompat.wrap(nodeInfo).getText();
        final ClickableSpan[] spans =
                text.getSpans(0, text.length(), ClickableSpan.class);

        doReturn(true).when(nodeInfo).performAction(anyInt(), any(Bundle.class));

        spans[1].onClick(null);

        ArgumentCaptor<Integer> integerArgumentCaptor =
                ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Bundle> bundleArgumentCaptor =
                ArgumentCaptor.forClass(Bundle.class);
        verify(nodeInfo).performAction(
                integerArgumentCaptor.capture(), bundleArgumentCaptor.capture());
        Bundle args = bundleArgumentCaptor.getValue();
        int actionId = integerArgumentCaptor.getValue();

        //The service would end up calling the same thing ViewCompat calls
        ViewCompat.performAccessibilityAction(mView, actionId, args);
        verify(span2).onClick(mView);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testAccessibilityActionPropagatesToNodeInfo() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
        final CharSequence label = "Asad's action";
        final int id = ViewCompat.addAccessibilityAction(mView, label, action);
        assertTrue(nodeHasActionWithId(id, label));
        ViewCompat.removeAccessibilityAction(mView, id);
        assertFalse(nodeHasActionWithId(id, label));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testAddDuplicateAccessibilityAction() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
        final CharSequence label = "Asad's action";
        final int id = ViewCompat.addAccessibilityAction(mView, label, action);
        assertEquals(1, nodeActionCountsWithId(id, label));

        final int newId = ViewCompat.addAccessibilityAction(mView, label, action);
        assertEquals(1, nodeActionCountsWithId(id, label));
        assertEquals(id, newId);
    }

    private boolean nodeHasActionWithId(int id, CharSequence label) {
        return nodeActionCountsWithId(id, label) > 0;
    }

    private int nodeActionCountsWithId(int id, CharSequence label) {
        int count = 0;
        final List<AccessibilityActionCompat> actions = getNodeCompatForView(mView).getActionList();
        for (int i = 0; i < actions.size(); i++) {
            final AccessibilityActionCompat action = actions.get(i);
            if (action.getId() == id && TextUtils.equals(action.getLabel(), label)) {
                count++;
            }
        }
        return count;
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testAccessibilityActionPerformIsCalled() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
        final int id = ViewCompat.addAccessibilityAction(mView, "Asad's action", action);
        ViewCompat.performAccessibilityAction(mView, id, null);
        verify(action).perform(mView, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testAccessibilityActionIdsAreReusedIfActionIdIsRemoved() {
        int actionIdToBeRemoved = -1;
        for (int i = 0; i < 32; i++) {
            AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
            final int id = ViewCompat.addAccessibilityAction(mView,
                    "Test" + Integer.valueOf(i).toString(), action);
            ViewCompat.performAccessibilityAction(mView, id, null);
            verify(action).perform(mView, null);
            actionIdToBeRemoved = id;
        }
        ViewCompat.removeAccessibilityAction(mView, actionIdToBeRemoved);

        AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
        final int id = ViewCompat.addAccessibilityAction(mView, "Last test", action);
        ViewCompat.performAccessibilityAction(mView, id, null);
        verify(action).perform(mView, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testReplaceActionPerformIsCalled() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);

        ViewCompat.replaceAccessibilityAction(mView, AccessibilityActionCompat.ACTION_FOCUS,
                "Focus title", action);

        ViewCompat.performAccessibilityAction(mView,
                AccessibilityNodeInfoCompat.ACTION_FOCUS, null);
        verify(action).perform(mView, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testReplaceActionPerformIsCalledWithTwoReplacements() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);
        final AccessibilityViewCommand action2 = mock(AccessibilityViewCommand.class);

        ViewCompat.replaceAccessibilityAction(mView, AccessibilityActionCompat.ACTION_FOCUS,
                "Focus title", action);

        String expectedLabel = "Focus title 2";
        ViewCompat.replaceAccessibilityAction(mView, AccessibilityActionCompat.ACTION_FOCUS,
                expectedLabel, action2);

        ViewCompat.performAccessibilityAction(mView,
                AccessibilityNodeInfoCompat.ACTION_FOCUS, null);
        verify(action2).perform(mView, null);
        verify(action, never()).perform(mView, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testActionRemovedAfterNullReplacement() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);

        ViewCompat.replaceAccessibilityAction(mView, AccessibilityActionCompat.ACTION_FOCUS,
                "Focus title", action);

        ViewCompat.replaceAccessibilityAction(mView, AccessibilityActionCompat.ACTION_FOCUS,
                null, null);

        assertFalse(nodeHasActionWithId(AccessibilityNodeInfoCompat.ACTION_FOCUS, null));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testReplaceActionPerformIsCalledWithArguments() {
        final AccessibilityViewCommand action = mock(AccessibilityViewCommand.class);

        ViewCompat.replaceAccessibilityAction(mView,
                AccessibilityActionCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, "Move title",
                action);

        final Bundle bundle = new Bundle();
        final int granularity = AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER;
        bundle.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                granularity);
        final boolean extendSelection = true;
        bundle.putBoolean(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                extendSelection);
        ViewCompat.performAccessibilityAction(mView,
                AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, bundle);
        AccessibilityViewCommand.MoveAtGranularityArguments args =
                new AccessibilityViewCommand.MoveAtGranularityArguments();
        args.setBundle(bundle);

        final ArgumentCaptor<MoveAtGranularityArguments> argCaptor = ArgumentCaptor.forClass(
                MoveAtGranularityArguments.class);
        verify(action).perform(eq(mView), argCaptor.capture());
        assertEquals(granularity, argCaptor.getValue().getGranularity());
        assertEquals(extendSelection, argCaptor.getValue().getExtendSelection());
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testSetAccessibilityDelegate_viewAutoImportant_makesViewImportant() {
        ViewCompat.setImportantForAccessibility(mView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        assertThat(mView.getImportantForAccessibility()).isEqualTo(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertThat(mView.getImportantForAccessibility()).isEqualTo(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void testSetAccessibilityDelegate_viewUnimportant_doesNotMakeViewImportant() {
        ViewCompat.setImportantForAccessibility(mView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        Truth.assertThat(mView.getImportantForAccessibility()).isEqualTo(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        Truth.assertThat(mView.getImportantForAccessibility()).isEqualTo(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Test
    public void testAccessibilityDelegateStillWorksAfterCompatImplicitlyAdded() {
        View.AccessibilityDelegate mockDelegate = mock(View.AccessibilityDelegate.class);
        mView.setAccessibilityDelegate(mockDelegate);

        ViewCompat.setScreenReaderFocusable(mView, true);
        assertMockAccessibilityDelegateWorkingOnView(mockDelegate);
    }

    @FlakyTest(bugId = 206644987)
    @Test
    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 32) // API 33 fails 100% b/233396250
    public void testSetAccessibilityPaneTitle_sendsOutCorrectEvent() throws TimeoutException {
        final Activity activity = mActivityTestRule.getActivity();
        final CharSequence title = "Sample title";

        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() ->
                                ViewCompat.setAccessibilityPaneTitle(mView, title));
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isWindowStateChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    int isPaneTitle = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE);
                    // onInitializeA11yEvent is not called in 28 for panes, so the package name
                    // isn't set
                    boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                            || TextUtils.equals(event.getPackageName(),
                            activity.getPackageName());
                    boolean hasTitleText = false;
                    if (event.getText().size() > 0) {
                        hasTitleText = event.getText().get(0).equals(title);
                    }
                    return isWindowStateChanged
                            && (isPaneTitle != 0)
                            && isFromThisPackage
                            && hasTitleText;
                },
                TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testSetStateDescription_propagatesToAccessibilityNodeInfo_sendsOutCorrectEvent()
            throws TimeoutException {
        final Activity activity = mActivityTestRule.getActivity();
        final CharSequence state = "test";

        assertNull(ViewCompat.getStateDescription(mView));
        assertNull(getNodeCompatForView(mView).getStateDescription());

        sUiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityTestRule.runOnUiThread(() ->
                                ViewCompat.setStateDescription(mView, state));
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> {
                    boolean isContentChanged = event.getEventType()
                            == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                    int isStateDescription = (event.getContentChangeTypes()
                            & AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION);
                    boolean isFromThisPackage = TextUtils.equals(event.getPackageName(),
                            activity.getPackageName());
                    return isContentChanged && (isStateDescription != 0) && isFromThisPackage;
                },
                TIMEOUT_ASYNC_PROCESSING);

        assertEquals(state, ViewCompat.getStateDescription(mView));
        assertEquals(state, getNodeCompatForView(mView).getStateDescription());

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertEquals(state, getNodeCompatForView(mView).getStateDescription());
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertEquals(state, getNodeCompatForView(mView).getStateDescription());
    }

    private void assertMockAccessibilityDelegateWorkingOnView(
            View.AccessibilityDelegate mockDelegate) {
        final AccessibilityEvent event = AccessibilityEvent.obtain();

        mView.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        verify(mockDelegate).sendAccessibilityEvent(mView, AccessibilityEvent.TYPE_ANNOUNCEMENT);

        mView.sendAccessibilityEventUnchecked(event);
        verify(mockDelegate).sendAccessibilityEventUnchecked(mView, event);

        mView.dispatchPopulateAccessibilityEvent(event);
        verify(mockDelegate).dispatchPopulateAccessibilityEvent(mView, event);

        mView.onPopulateAccessibilityEvent(event);
        verify(mockDelegate).onPopulateAccessibilityEvent(mView, event);

        mView.onInitializeAccessibilityEvent(event);
        verify(mockDelegate).onInitializeAccessibilityEvent(mView, event);

        final AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        mView.onInitializeAccessibilityNodeInfo(info);
        verify(mockDelegate).onInitializeAccessibilityNodeInfo(mView, info);

        final View childView = mView.getChildAt(0);
        mView.requestSendAccessibilityEvent(childView, event);
        verify(mockDelegate).onRequestSendAccessibilityEvent(mView, childView, event);

        if (Build.VERSION.SDK_INT >= 16) {
            mView.getAccessibilityNodeProvider();
            verify(mockDelegate).getAccessibilityNodeProvider(mView);

            final Bundle bundle = new Bundle();
            mView.performAccessibilityAction(
                    AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
            verify(mockDelegate).performAccessibilityAction(
                    mView, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
        }
    }

    private void assertMockBridgedAccessibilityDelegateCompatWorkingOnView(
            AccessibilityDelegateCompat bridgedCompat) {
        final AccessibilityEvent event = AccessibilityEvent.obtain();

        mView.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        verify(bridgedCompat).sendAccessibilityEvent(mView, AccessibilityEvent.TYPE_ANNOUNCEMENT);

        mView.sendAccessibilityEventUnchecked(event);
        verify(bridgedCompat).sendAccessibilityEventUnchecked(mView, event);

        mView.dispatchPopulateAccessibilityEvent(event);
        verify(bridgedCompat).dispatchPopulateAccessibilityEvent(mView, event);

        mView.onPopulateAccessibilityEvent(event);
        verify(bridgedCompat).onPopulateAccessibilityEvent(mView, event);

        mView.onInitializeAccessibilityEvent(event);
        verify(bridgedCompat).onInitializeAccessibilityEvent(mView, event);

        final AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        mView.onInitializeAccessibilityNodeInfo(info);
        verify(bridgedCompat).onInitializeAccessibilityNodeInfo(eq(mView),
                any(AccessibilityNodeInfoCompat.class));

        final View childView = mView.getChildAt(0);
        mView.requestSendAccessibilityEvent(childView, event);
        verify(bridgedCompat).onRequestSendAccessibilityEvent(mView, childView, event);

        if (Build.VERSION.SDK_INT >= 16) {
            final AccessibilityNodeProviderCompat providerCompat =
                    new AccessibilityNodeProviderCompat() {
                        @Override
                        public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(
                                int virtualViewId) {
                            return AccessibilityNodeInfoCompat.wrap(info);
                        }
                    };
            when(bridgedCompat.getAccessibilityNodeProvider(mView)).thenReturn(providerCompat);
            AccessibilityNodeProvider provider = mView.getAccessibilityNodeProvider();
            assertEquals(info, provider.createAccessibilityNodeInfo(0));

            final Bundle bundle = new Bundle();
            mView.performAccessibilityAction(
                    AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
            verify(bridgedCompat).performAccessibilityAction(
                    mView, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
        }
    }

    private AccessibilityNodeInfoCompat getNodeCompatForView(View view) {
        final AccessibilityNodeInfo nodeInfo = AccessibilityNodeInfo.obtain();
        final AccessibilityNodeInfoCompat nodeCompat = AccessibilityNodeInfoCompat.wrap(nodeInfo);
        view.onInitializeAccessibilityNodeInfo(nodeInfo);
        return nodeCompat;
    }

    // Bridge to Mockito, since a mock won't get properly installed as a delegate on the view,
    // and a spy won't see the callbacks, which go to the real object.
    private static class BridgingDelegateCompat extends AccessibilityDelegateCompat {
        private final AccessibilityDelegateCompat mMockCompat;
        BridgingDelegateCompat(AccessibilityDelegateCompat mockCompat) {
            mMockCompat = mockCompat;
        }

        @Override
        public void sendAccessibilityEvent(@NonNull View host, int eventType) {
            mMockCompat.sendAccessibilityEvent(host, eventType);
        }

        @Override
        public void sendAccessibilityEventUnchecked(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            mMockCompat.sendAccessibilityEventUnchecked(host, event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(
                @NonNull View host, @NonNull AccessibilityEvent event) {
            return mMockCompat.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onPopulateAccessibilityEvent(
                @NonNull View host, @NonNull AccessibilityEvent event) {
            mMockCompat.onPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityEvent(
                @NonNull View host, @NonNull AccessibilityEvent event) {
            mMockCompat.onInitializeAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(
                @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            mMockCompat.onInitializeAccessibilityNodeInfo(host, info);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(@NonNull ViewGroup host, @NonNull View child,
                @NonNull AccessibilityEvent event) {
            return mMockCompat.onRequestSendAccessibilityEvent(host, child, event);
        }

        @Override
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(@NonNull View host) {
            return mMockCompat.getAccessibilityNodeProvider(host);
        }

        @Override
        public boolean performAccessibilityAction(
                @NonNull View host, int action, @Nullable Bundle args) {
            return mMockCompat.performAccessibilityAction(host, action, args);
        }
    }
}
