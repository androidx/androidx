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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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

import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.core.view.accessibility.AccessibilityViewCommand.MoveAtGranularityArguments;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

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

    private static Instrumentation sInstrumentation;
    private static UiAutomation sUiAutomation;

    private ViewGroup mView;

    public AccessibilityDelegateCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        sInstrumentation = InstrumentationRegistry.getInstrumentation();
        sUiAutomation = sInstrumentation.getUiAutomation();
        final Activity activity = mActivityTestRule.getActivity();
        // Use a group, so it has a child
        mView = (ViewGroup) activity.findViewById(androidx.core.test.R.id.view).getParent();
        // On KitKat, some delegate methods aren't called for non-important views
        ViewCompat.setImportantForAccessibility(mView, View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Test
    public void testViewWithDelegate_reportsHasDelegate() {
        assertThat(ViewCompat.hasAccessibilityDelegate(mView), equalTo(false));
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertThat(ViewCompat.hasAccessibilityDelegate(mView), equalTo(true));
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
        assertThat(ViewCompat.isScreenReaderFocusable(mView), is(false));
        assertThat(getNodeCompatForView(mView).isScreenReaderFocusable(), is(false));

        ViewCompat.setScreenReaderFocusable(mView, true);

        assertThat(ViewCompat.isScreenReaderFocusable(mView), is(true));
        assertThat(getNodeCompatForView(mView).isScreenReaderFocusable(), is(true));

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertThat(getNodeCompatForView(mView).isScreenReaderFocusable(), is(true));
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertThat(getNodeCompatForView(mView).isScreenReaderFocusable(), is(true));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityHeading_propagatesToAccessibilityNodeInfo() {
        assertThat(ViewCompat.isAccessibilityHeading(mView), is(false));
        assertThat(getNodeCompatForView(mView).isHeading(), is(false));

        ViewCompat.setAccessibilityHeading(mView, true);

        assertThat(ViewCompat.isAccessibilityHeading(mView), is(true));
        assertThat(getNodeCompatForView(mView).isHeading(), is(true));

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertThat(getNodeCompatForView(mView).isHeading(), is(true));
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertThat(getNodeCompatForView(mView).isHeading(), is(true));
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
    @FlakyTest(bugId = 187190911)
    public void testAccessibilityPaneTitle_isntTrackedAsPaneWithoutTitle() {
        // This test isn't to test the propagation up, just that the event is sent correctly
        ViewCompat.setAccessibilityLiveRegion(mView,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

        ViewCompat.setAccessibilityPaneTitle(mView, "Sample title");

        ViewCompat.setAccessibilityPaneTitle(mView, null);

        final AccessibilityDelegateCompat mockDelegate = mock(
                AccessibilityDelegateCompat.class);
        ViewCompat.setAccessibilityDelegate(mView, new BridgingDelegateCompat(mockDelegate));

        mView.setVisibility(View.VISIBLE);

        mView.getViewTreeObserver().dispatchOnGlobalLayout();
        ArgumentCaptor<AccessibilityEvent> argumentCaptor =
                ArgumentCaptor.forClass(AccessibilityEvent.class);
        verify(mockDelegate, never()).sendAccessibilityEventUnchecked(
                eq(mView), argumentCaptor.capture());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_isSentOnAppearance() throws Throwable {
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mView, title);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the AccessibilityPaneVisibilityManager
                mView.setVisibility(View.INVISIBLE);
                mView.getViewTreeObserver().dispatchOnGlobalLayout();
                mView.setVisibility(View.VISIBLE);
            }
        });

        final AccessibilityDelegateCompat mockDelegate = mock(
                AccessibilityDelegateCompat.class);
        ViewCompat.setAccessibilityDelegate(mView, new BridgingDelegateCompat(mockDelegate));

        mView.getViewTreeObserver().dispatchOnGlobalLayout();

        ArgumentCaptor<AccessibilityEvent> argumentCaptor =
                ArgumentCaptor.forClass(AccessibilityEvent.class);
        if (Build.VERSION.SDK_INT < 28) {
            // Validity check
            assertThat(ViewCompat.getImportantForAccessibility(mView),
                    equalTo(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES));

            verify(mockDelegate).sendAccessibilityEventUnchecked(
                    eq(mView), argumentCaptor.capture());
            AccessibilityEvent event = argumentCaptor.getValue();
            assertThat(event.getText().get(0), equalTo(title));
            assertThat((event.getContentChangeTypes()
                    & AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_APPEARED),  not(0));
        } else {
            verify(mockDelegate, never()).sendAccessibilityEventUnchecked(
                    eq(mView), argumentCaptor.capture());
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testAccessibilityPaneTitle_isSentOnDisappearance() throws Throwable {
        final CharSequence title = "Sample title";
        ViewCompat.setAccessibilityPaneTitle(mView, title);

        // Validity check
        assertThat(ViewCompat.getImportantForAccessibility(mView),
                equalTo(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES));

        final Activity activity = mActivityTestRule.getActivity();
        sUiAutomation.executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    mActivityTestRule.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update the AccessibilityPaneVisibilityManager
                            mView.setVisibility(View.INVISIBLE);
                            mView.getViewTreeObserver().dispatchOnGlobalLayout();
                        }
                    });
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
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
            }
        }, TIMEOUT_ASYNC_PROCESSING);
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
        Bundle args = bundleArgumentCaptor.<Bundle>getValue();
        int actionId = integerArgumentCaptor.<Integer>getValue();

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
        assertThat(nodeActionCountsWithId(id, label), equalTo(1));

        final int newId = ViewCompat.addAccessibilityAction(mView, label, action);
        assertThat(nodeActionCountsWithId(id, label), equalTo(1));
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
        final AccessibilityViewCommand action =
                (AccessibilityViewCommand) mock(
                        AccessibilityViewCommand.class);

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
        assertTrue(argCaptor.getValue().getGranularity() == granularity);
        assertTrue(argCaptor.getValue().getExtendSelection() == extendSelection);
    }

    @Test
    public void testAccessiblityDelegateStillWorksAfterCompatImplicitlyAdded() {
        View.AccessibilityDelegate mockDelegate = mock(View.AccessibilityDelegate.class);
        mView.setAccessibilityDelegate(mockDelegate);

        ViewCompat.setScreenReaderFocusable(mView, true);
        assertMockAccessibilityDelegateWorkingOnView(mockDelegate);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testSetAccessibilityPaneTitle_sendsOutCorrectEvent() throws TimeoutException {
        final Activity activity = mActivityTestRule.getActivity();

        sUiAutomation.executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setAccessibilityPaneTitle(mView, "test");
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                boolean isWindowStateChanged = event.getEventType()
                        == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                int isPaneTitle = (event.getContentChangeTypes()
                        & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE);
                // onInitializeA11yEvent is not called in 28 for panes, so the package name
                // isn't set
                boolean isFromThisPackage = Build.VERSION.SDK_INT == 28
                        || TextUtils.equals(event.getPackageName(),
                        activity.getPackageName());
                boolean isFromThisSource =
                        event.getSource().equals(mView.createAccessibilityNodeInfo());
                return isWindowStateChanged && (isPaneTitle != 0) && isFromThisPackage
                        && isFromThisSource;
            }
        }, TIMEOUT_ASYNC_PROCESSING);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testSetStateDescription_propagatesToAccessibilityNodeInfo_sendsOutCorrectEvent()
            throws TimeoutException {
        final Activity activity = mActivityTestRule.getActivity();
        final CharSequence state = "test";

        assertThat(ViewCompat.getStateDescription(mView), is(nullValue()));
        assertThat(getNodeCompatForView(mView).getStateDescription(), is(nullValue()));

        sUiAutomation.executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setStateDescription(mView, state);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                boolean isContentChanged = event.getEventType()
                        == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                int isStateDescription = (event.getContentChangeTypes()
                        & AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION);
                boolean isFromThisPackage = TextUtils.equals(event.getPackageName(),
                        activity.getPackageName());
                return isContentChanged && (isStateDescription != 0) && isFromThisPackage;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        assertThat(ViewCompat.getStateDescription(mView), is(state));
        assertThat(getNodeCompatForView(mView).getStateDescription(), is(state));

        // The value should still propagate even if we attach and detach another delegate compat
        ViewCompat.setAccessibilityDelegate(mView, new AccessibilityDelegateCompat());
        assertThat(getNodeCompatForView(mView).getStateDescription(), is(state));
        ViewCompat.setAccessibilityDelegate(mView, null);
        assertThat(getNodeCompatForView(mView).getStateDescription(), is(state));
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
            assertThat(provider.createAccessibilityNodeInfo(0), equalTo(info));

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
        public void sendAccessibilityEvent(View host, int eventType) {
            mMockCompat.sendAccessibilityEvent(host, eventType);
        }

        @Override
        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
            mMockCompat.sendAccessibilityEventUnchecked(host, event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            return mMockCompat.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            mMockCompat.onPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            mMockCompat.onInitializeAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            mMockCompat.onInitializeAccessibilityNodeInfo(host, info);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                AccessibilityEvent event) {
            return mMockCompat.onRequestSendAccessibilityEvent(host, child, event);
        }

        @Override
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
            return mMockCompat.getAccessibilityNodeProvider(host);
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            return mMockCompat.performAccessibilityAction(host, action, args);
        }
    }
}
