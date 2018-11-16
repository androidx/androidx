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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Build;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.util.SparseArray;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.R;
import androidx.core.view.accessibility.AccessibilityClickableSpanCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing {@link AccessibilityDelegate}.
 * <p>
 * <strong>Note:</strong> On platform versions prior to
 * {@link android.os.Build.VERSION_CODES#M API 23}, delegate methods on
 * views in the {@code android.widget.*} package are called <i>before</i>
 * host methods. This prevents certain properties such as class name from
 * being modified by overriding
 * {@link AccessibilityDelegateCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)},
 * as any changes will be overwritten by the host class.
 * <p>
 * Starting in {@link android.os.Build.VERSION_CODES#M API 23}, delegate
 * methods are called <i>after</i> host methods, which all properties to be
 * modified without being overwritten by the host class.
 */
public class AccessibilityDelegateCompat {

    static final class AccessibilityDelegateAdapter extends AccessibilityDelegate {
        final AccessibilityDelegateCompat mCompat;

        AccessibilityDelegateAdapter(AccessibilityDelegateCompat compat) {
            mCompat = compat;
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host,
                AccessibilityEvent event) {
            return mCompat.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            mCompat.onInitializeAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(
                View host, AccessibilityNodeInfo info) {
            AccessibilityNodeInfoCompat nodeInfoCompat = AccessibilityNodeInfoCompat.wrap(info);
            nodeInfoCompat.setScreenReaderFocusable(ViewCompat.isScreenReaderFocusable(host));
            nodeInfoCompat.setHeading(ViewCompat.isAccessibilityHeading(host));
            nodeInfoCompat.setPaneTitle(ViewCompat.getAccessibilityPaneTitle(host));
            mCompat.onInitializeAccessibilityNodeInfo(host, nodeInfoCompat);
            nodeInfoCompat.addSpansToExtras(info.getText(), host);
            List<AccessibilityActionCompat> actions = getActionList(host);
            for (int i = 0; i < actions.size(); i++) {
                nodeInfoCompat.addAction(actions.get(i));
            }
        }

        @Override
        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            mCompat.onPopulateAccessibilityEvent(host, event);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                AccessibilityEvent event) {
            return mCompat.onRequestSendAccessibilityEvent(host, child, event);
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            mCompat.sendAccessibilityEvent(host, eventType);
        }

        @Override
        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
            mCompat.sendAccessibilityEventUnchecked(host, event);
        }

        @Override
        @RequiresApi(16)
        public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
            AccessibilityNodeProviderCompat provider =
                    mCompat.getAccessibilityNodeProvider(host);
            return (provider != null)
                    ? (AccessibilityNodeProvider) provider.getProvider() : null;
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            return mCompat.performAccessibilityAction(host, action, args);
        }
    }

    private static final AccessibilityDelegate DEFAULT_DELEGATE = new AccessibilityDelegate();
    private final AccessibilityDelegate mOriginalDelegate;

    private final AccessibilityDelegate mBridge;

    /**
     * Creates a new instance.
     */
    public AccessibilityDelegateCompat() {
        this(DEFAULT_DELEGATE);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public AccessibilityDelegateCompat(AccessibilityDelegate originalDelegate) {
        mOriginalDelegate = originalDelegate;
        mBridge = new AccessibilityDelegateAdapter(this);
    }

    /**
     * @return The wrapped bridge implementation.
     */
    AccessibilityDelegate getBridge() {
        return mBridge;
    }

    /**
     * Sends an accessibility event of the given type. If accessibility is not
     * enabled this method has no effect.
     * <p>
     * The default implementation behaves as {@link View#sendAccessibilityEvent(int)
     * View#sendAccessibilityEvent(int)} for the case of no accessibility delegate
     * been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param eventType The type of the event to send.
     *
     * @see View#sendAccessibilityEvent(int) View#sendAccessibilityEvent(int)
     */
    public void sendAccessibilityEvent(View host, int eventType) {
        mOriginalDelegate.sendAccessibilityEvent(host, eventType);
    }

    /**
     * Sends an accessibility event. This method behaves exactly as
     * {@link #sendAccessibilityEvent(View, int)} but takes as an argument an
     * empty {@link AccessibilityEvent} and does not perform a check whether
     * accessibility is enabled.
     * <p>
     * The default implementation behaves as
     * {@link View#sendAccessibilityEventUnchecked(AccessibilityEvent)
     * View#sendAccessibilityEventUnchecked(AccessibilityEvent)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param event The event to send.
     *
     * @see View#sendAccessibilityEventUnchecked(AccessibilityEvent)
     *      View#sendAccessibilityEventUnchecked(AccessibilityEvent)
     */
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
        mOriginalDelegate.sendAccessibilityEventUnchecked(host, event);
    }

    /**
     * Dispatches an {@link AccessibilityEvent} to the host {@link View} first and then
     * to its children for adding their text content to the event.
     * <p>
     * The default implementation behaves as
     * {@link View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     * View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param event The event.
     * @return True if the event population was completed.
     *
     * @see View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     *      View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     */
    public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        return mOriginalDelegate.dispatchPopulateAccessibilityEvent(host, event);
    }

    /**
     * Gives a chance to the host View to populate the accessibility event with its
     * text content.
     * <p>
     * The default implementation behaves as
     * {@link ViewCompat#onPopulateAccessibilityEvent(View, AccessibilityEvent)
     * ViewCompat#onPopulateAccessibilityEvent(AccessibilityEvent)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param event The accessibility event which to populate.
     *
     * @see ViewCompat#onPopulateAccessibilityEvent(View ,AccessibilityEvent)
     *      ViewCompat#onPopulateAccessibilityEvent(View, AccessibilityEvent)
     */
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        mOriginalDelegate.onPopulateAccessibilityEvent(host, event);
    }

    /**
     * Initializes an {@link AccessibilityEvent} with information about the
     * the host View which is the event source.
     * <p>
     * The default implementation behaves as
     * {@link ViewCompat#onInitializeAccessibilityEvent(View v, AccessibilityEvent event)
     * ViewCompat#onInitalizeAccessibilityEvent(View v, AccessibilityEvent event)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param event The event to initialize.
     *
     * @see ViewCompat#onInitializeAccessibilityEvent(View, AccessibilityEvent)
     *      ViewCompat#onInitializeAccessibilityEvent(View, AccessibilityEvent)
     */
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        mOriginalDelegate.onInitializeAccessibilityEvent(host, event);
    }

    /**
     * Initializes an {@link AccessibilityNodeInfoCompat} with information about the host view.
     * <p>
     * The default implementation behaves as
     * {@link ViewCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)
     * ViewCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param info The instance to initialize.
     *
     * @see ViewCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)
     *      ViewCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)
     */
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        mOriginalDelegate.onInitializeAccessibilityNodeInfo(
                host, info.unwrap());
    }

    /**
     * Called when a child of the host View has requested sending an
     * {@link AccessibilityEvent} and gives an opportunity to the parent (the host)
     * to augment the event.
     * <p>
     * The default implementation behaves as
     * {@link ViewGroupCompat#onRequestSendAccessibilityEvent(ViewGroup, View, AccessibilityEvent)
     * ViewGroupCompat#onRequestSendAccessibilityEvent(ViewGroup, View, AccessibilityEvent)} for
     * the case of no accessibility delegate been set.
     * </p>
     *
     * @param host The View hosting the delegate.
     * @param child The child which requests sending the event.
     * @param event The event to be sent.
     * @return True if the event should be sent
     *
     * @see ViewGroupCompat#onRequestSendAccessibilityEvent(ViewGroup, View, AccessibilityEvent)
     *      ViewGroupCompat#onRequestSendAccessibilityEvent(ViewGroup, View, AccessibilityEvent)
     */
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
            AccessibilityEvent event) {
        return mOriginalDelegate.onRequestSendAccessibilityEvent(host, child, event);
    }

    /**
     * Gets the provider for managing a virtual view hierarchy rooted at this View
     * and reported to {@link android.accessibilityservice.AccessibilityService}s
     * that explore the window content.
     * <p>
     * The default implementation behaves as
     * {@link ViewCompat#getAccessibilityNodeProvider(View) ViewCompat#getAccessibilityNodeProvider(View)}
     * for the case of no accessibility delegate been set.
     * </p>
     *
     * @return The provider.
     *
     * @see AccessibilityNodeProviderCompat
     */
    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
        if (Build.VERSION.SDK_INT >= 16) {
            Object provider = mOriginalDelegate.getAccessibilityNodeProvider(host);
            if (provider != null) {
                return new AccessibilityNodeProviderCompat(provider);
            }
        }
        return null;
    }

    /**
     * Performs the specified accessibility action on the view. For
     * possible accessibility actions look at {@link AccessibilityNodeInfoCompat}.
     * <p>
     * The default implementation behaves as
     * {@link View#performAccessibilityAction(int, Bundle)
     *  View#performAccessibilityAction(int, Bundle)} for the case of
     *  no accessibility delegate been set.
     * </p>
     *
     * @param action The action to perform.
     * @return Whether the action was performed.
     *
     * @see View#performAccessibilityAction(int, Bundle)
     *      View#performAccessibilityAction(int, Bundle)
     */
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        boolean success = false;
        List<AccessibilityActionCompat> actions = getActionList(host);
        for (int i = 0; i < actions.size(); i++) {
            AccessibilityActionCompat actionCompat = actions.get(i);
            if (actionCompat.getId() == action) {
                success = actionCompat.perform(host, args);
                break;
            }
        }
        if (!success && Build.VERSION.SDK_INT >= 16) {
            success = mOriginalDelegate.performAccessibilityAction(host, action, args);
        }
        if (!success && action == R.id.accessibility_action_clickable_span) {
            success = performClickableSpanAction(
                    args.getInt(AccessibilityClickableSpanCompat.SPAN_ID, -1), host);
        }
        return success;
    }

    private boolean performClickableSpanAction(int clickableSpanId, View host) {
        SparseArray<WeakReference<ClickableSpan>> spans =
                (SparseArray<WeakReference<ClickableSpan>>)
                        host.getTag(R.id.tag_accessibility_clickable_spans);
        if (spans != null) {
            WeakReference<ClickableSpan> reference = spans.get(clickableSpanId);
            if (reference != null) {
                ClickableSpan span = reference.get();
                if (isSpanStillValid(span, host)) {
                    span.onClick(host);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSpanStillValid(ClickableSpan span, View view) {
        if (span != null) {
            AccessibilityNodeInfo info = view.createAccessibilityNodeInfo();
            ClickableSpan[] spans = AccessibilityNodeInfoCompat.getClickableSpans(info.getText());
            for (int i = 0; spans != null && i < spans.length; i++) {
                if (span.equals(spans[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<AccessibilityActionCompat> getActionList(View view) {
        List<AccessibilityActionCompat> actions = (List<AccessibilityActionCompat>)
                view.getTag(R.id.tag_accessibility_actions);
        return actions == null ? Collections.<AccessibilityActionCompat>emptyList() : actions;
    }
}
