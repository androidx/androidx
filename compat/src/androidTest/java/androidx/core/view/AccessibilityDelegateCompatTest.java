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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AccessibilityDelegateCompatTest extends
        BaseInstrumentationTestCase<ViewCompatActivity> {

    private ViewGroup mView;

    public AccessibilityDelegateCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        // Use a group, so it has a child
        mView = (ViewGroup) activity.findViewById(androidx.core.test.R.id.view).getParent();
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
        final AccessibilityEvent event = AccessibilityEvent.obtain();

        ViewCompat.setAccessibilityDelegate(mView, new BridgingDelegateCompat(mockCompat));

        mView.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        verify(mockCompat).sendAccessibilityEvent(mView, AccessibilityEvent.TYPE_ANNOUNCEMENT);

        mView.sendAccessibilityEventUnchecked(event);
        verify(mockCompat).sendAccessibilityEventUnchecked(mView, event);

        mView.dispatchPopulateAccessibilityEvent(event);
        verify(mockCompat).dispatchPopulateAccessibilityEvent(mView, event);

        mView.onPopulateAccessibilityEvent(event);
        verify(mockCompat).onPopulateAccessibilityEvent(mView, event);

        mView.onInitializeAccessibilityEvent(event);
        verify(mockCompat).onInitializeAccessibilityEvent(mView, event);

        final AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        mView.onInitializeAccessibilityNodeInfo(info);
        verify(mockCompat).onInitializeAccessibilityNodeInfo(eq(mView),
                any(AccessibilityNodeInfoCompat.class));

        final View childView = mView.getChildAt(0);
        mView.requestSendAccessibilityEvent(childView, event);
        verify(mockCompat).onRequestSendAccessibilityEvent(mView, childView, event);

        if (Build.VERSION.SDK_INT >= 16) {
            final AccessibilityNodeProviderCompat providerCompat =
                    new AccessibilityNodeProviderCompat() {
                        @Override
                        public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(
                                int virtualViewId) {
                            return AccessibilityNodeInfoCompat.wrap(info);
                        }
                    };
            when(mockCompat.getAccessibilityNodeProvider(mView)).thenReturn(providerCompat);
            AccessibilityNodeProvider provider = mView.getAccessibilityNodeProvider();
            assertThat(provider.createAccessibilityNodeInfo(0), equalTo(info));

            final Bundle bundle = new Bundle();
            mView.performAccessibilityAction(
                    AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
            verify(mockCompat).performAccessibilityAction(
                    mView, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, bundle);
        }
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
