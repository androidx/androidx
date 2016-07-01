/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewCompat.FocusDirection;
import android.support.v4.view.ViewCompat.FocusRealDirection;
import android.support.v4.view.ViewParentCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ExploreByTouchHelper is a utility class for implementing accessibility
 * support in custom {@link View}s that represent a collection of View-like
 * logical items. It extends {@link AccessibilityNodeProviderCompat} and
 * simplifies many aspects of providing information to accessibility services
 * and managing accessibility focus. This class does not currently support
 * hierarchies of logical items.
 * <p>
 * Clients should override abstract methods on this class and attach it to the
 * host view using {@link ViewCompat#setAccessibilityDelegate}:
 * <p>
 * <pre>
 * class MyCustomView extends View {
 *     private MyVirtualViewHelper mVirtualViewHelper;
 *
 *     public MyCustomView(Context context, ...) {
 *         ...
 *         mVirtualViewHelper = new MyVirtualViewHelper(this);
 *         ViewCompat.setAccessibilityDelegate(this, mVirtualViewHelper);
 *     }
 *
 *     &#64;Override
 *     public boolean dispatchHoverEvent(MotionEvent event) {
 *       return mHelper.dispatchHoverEvent(this, event)
 *           || super.dispatchHoverEvent(event);
 *     }
 *
 *     &#64;Override
 *     public boolean dispatchKeyEvent(KeyEvent event) {
 *       return mHelper.dispatchKeyEvent(event)
 *           || super.dispatchKeyEvent(event);
 *     }
 *
 *     &#64;Override
 *     public boolean onFocusChanged(boolean gainFocus, int direction,
 *         Rect previouslyFocusedRect) {
 *       super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
 *       mHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
 *     }
 * }
 * mAccessHelper = new MyExploreByTouchHelper(someView);
 * ViewCompat.setAccessibilityDelegate(someView, mAccessHelper);
 * </pre>
 */
public abstract class ExploreByTouchHelper extends AccessibilityDelegateCompat {
    /** Virtual node identifier value for invalid nodes. */
    public static final int INVALID_ID = Integer.MIN_VALUE;

    /** Virtual node identifier value for the host view's node. */
    public static final int HOST_ID = View.NO_ID;

    /** Default class name used for virtual views. */
    private static final String DEFAULT_CLASS_NAME = "android.view.View";

    /** Default bounds used to determine if the client didn't set any. */
    private static final Rect INVALID_PARENT_BOUNDS = new Rect(
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    // Temporary, reusable data structures.
    private final Rect mTempScreenRect = new Rect();
    private final Rect mTempParentRect = new Rect();
    private final Rect mTempVisibleRect = new Rect();
    private final int[] mTempGlobalRect = new int[2];

    /** System accessibility manager, used to check state and send events. */
    private final AccessibilityManager mManager;

    /** View whose internal structure is exposed through this helper. */
    private final View mHost;

    /** Virtual node provider used to expose logical structure to services. */
    private MyNodeProvider mNodeProvider;

    /** Identifier for the virtual view that holds accessibility focus. */
    private int mAccessibilityFocusedVirtualViewId = INVALID_ID;

    /** Identifier for the virtual view that holds keyboard focus. */
    private int mKeyboardFocusedVirtualViewId = INVALID_ID;

    /** Identifier for the virtual view that is currently hovered. */
    private int mHoveredVirtualViewId = INVALID_ID;

    /**
     * Constructs a new helper that can expose a virtual view hierarchy for the
     * specified host view.
     *
     * @param host view whose virtual view hierarchy is exposed by this helper
     */
    public ExploreByTouchHelper(View host) {
        if (host == null) {
            throw new IllegalArgumentException("View may not be null");
        }

        mHost = host;

        final Context context = host.getContext();
        mManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        // Host view must be focusable so that we can delegate to virtual
        // views.
        host.setFocusable(true);
        if (ViewCompat.getImportantForAccessibility(host)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(
                    host, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @Override
    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
        if (mNodeProvider == null) {
            mNodeProvider = new MyNodeProvider();
        }
        return mNodeProvider;
    }

    /**
     * Delegates hover events from the host view.
     * <p>
     * Dispatches hover {@link MotionEvent}s to the virtual view hierarchy when
     * the Explore by Touch feature is enabled.
     * <p>
     * This method should be called by overriding the host view's
     * {@link View#dispatchHoverEvent(MotionEvent)} method:
     * <pre>&#64;Override
     * public boolean dispatchHoverEvent(MotionEvent event) {
     *   return mHelper.dispatchHoverEvent(this, event)
     *       || super.dispatchHoverEvent(event);
     * }
     * </pre>
     *
     * @param event The hover event to dispatch to the virtual view hierarchy.
     * @return Whether the hover event was handled.
     */
    public final boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        if (!mManager.isEnabled()
                || !AccessibilityManagerCompat.isTouchExplorationEnabled(mManager)) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEventCompat.ACTION_HOVER_MOVE:
            case MotionEventCompat.ACTION_HOVER_ENTER:
                final int virtualViewId = getVirtualViewAt(event.getX(), event.getY());
                updateHoveredVirtualView(virtualViewId);
                return (virtualViewId != INVALID_ID);
            case MotionEventCompat.ACTION_HOVER_EXIT:
                if (mAccessibilityFocusedVirtualViewId != INVALID_ID) {
                    updateHoveredVirtualView(INVALID_ID);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Delegates key events from the host view.
     * <p>
     * This method should be called by overriding the host view's
     * {@link View#dispatchKeyEvent(KeyEvent)} method:
     * <pre>&#64;Override
     * public boolean dispatchKeyEvent(KeyEvent event) {
     *   return mHelper.dispatchKeyEvent(event)
     *       || super.dispatchKeyEvent(event);
     * }
     * </pre>
     */
    public final boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        boolean handled = false;

        final int action = event.getAction();
        if (action != KeyEvent.ACTION_UP) {
            final int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (KeyEventCompat.hasNoModifiers(event)) {
                        final int direction = keyToDirection(keyCode);
                        final int count = 1 + event.getRepeatCount();
                        for (int i = 0; i < count; i++) {
                            if (moveFocus(direction, null)) {
                                handled = true;
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (KeyEventCompat.hasNoModifiers(event)) {
                        if (event.getRepeatCount() == 0) {
                            clickKeyboardFocusedVirtualView();
                            handled = true;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (KeyEventCompat.hasNoModifiers(event)) {
                        handled = moveFocus(View.FOCUS_FORWARD, null);
                    } else if (KeyEventCompat.hasModifiers(event, KeyEvent.META_SHIFT_ON)) {
                        handled = moveFocus(View.FOCUS_BACKWARD, null);
                    }
                    break;
            }
        }

        return handled;
    }

    /**
     * Delegates focus changes from the host view.
     * <p>
     * This method should be called by overriding the host view's
     * {@link View#onFocusChanged(boolean, int, Rect)} method:
     * <pre>&#64;Override
     * public boolean onFocusChanged(boolean gainFocus, int direction,
     *     Rect previouslyFocusedRect) {
     *   super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
     *   mHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
     * }
     * </pre>
     */
    public final void onFocusChanged(boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        if (mKeyboardFocusedVirtualViewId != INVALID_ID) {
            clearKeyboardFocusForVirtualView(mKeyboardFocusedVirtualViewId);
        }

        if (gainFocus) {
            moveFocus(direction, previouslyFocusedRect);
        }
    }

    /**
     * @return the identifier of the virtual view that has accessibility focus
     *         or {@link #INVALID_ID} if no virtual view has accessibility
     *         focus
     */
    public final int getAccessibilityFocusedVirtualViewId() {
        return mAccessibilityFocusedVirtualViewId;
    }

    /**
     * @return the identifier of the virtual view that has keyboard focus
     *         or {@link #INVALID_ID} if no virtual view has keyboard focus
     */
    public final int getKeyboardFocusedVirtualViewId() {
        return mKeyboardFocusedVirtualViewId;
    }

    /**
     * Maps key event codes to focus directions.
     *
     * @param keyCode the key event code
     * @return the corresponding focus direction
     */
    @FocusRealDirection
    private static int keyToDirection(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return View.FOCUS_RIGHT;
            default:
                return View.FOCUS_DOWN;
        }
    }

    /**
     * Obtains the bounds for the specified virtual view.
     *
     * @param virtualViewId the identifier of the virtual view
     * @param outBounds the rect to populate with virtual view bounds
     */
    private void getBoundsInParent(int virtualViewId, Rect outBounds) {
        final AccessibilityNodeInfoCompat node = obtainAccessibilityNodeInfo(virtualViewId);
        node.getBoundsInParent(outBounds);
    }

    /**
     * Adapts AccessibilityNodeInfoCompat for obtaining bounds.
     */
    private static final FocusStrategy.BoundsAdapter<AccessibilityNodeInfoCompat> NODE_ADAPTER =
            new FocusStrategy.BoundsAdapter<AccessibilityNodeInfoCompat>() {
                @Override
                public void obtainBounds(AccessibilityNodeInfoCompat node, Rect outBounds) {
                    node.getBoundsInParent(outBounds);
                }
            };

    /**
     * Adapts SparseArrayCompat for iterating through values.
     */
    private static final FocusStrategy.CollectionAdapter<SparseArrayCompat<
            AccessibilityNodeInfoCompat>, AccessibilityNodeInfoCompat> SPARSE_VALUES_ADAPTER =
            new FocusStrategy.CollectionAdapter<SparseArrayCompat<
                    AccessibilityNodeInfoCompat>, AccessibilityNodeInfoCompat>() {
                @Override
                public AccessibilityNodeInfoCompat get(
                        SparseArrayCompat<AccessibilityNodeInfoCompat> collection, int index) {
                    return collection.valueAt(index);
                }

                @Override
                public int size(SparseArrayCompat<AccessibilityNodeInfoCompat> collection) {
                    return collection.size();
                }
            };

    /**
     * Attempts to move keyboard focus in the specified direction.
     *
     * @param direction the direction in which to move keyboard focus
     * @param previouslyFocusedRect the bounds of the previously focused item,
     *                              or {@code null} if not available
     * @return {@code true} if keyboard focus moved to a virtual view managed
     *         by this helper, or {@code false} otherwise
     */
    private boolean moveFocus(@FocusDirection int direction, @Nullable Rect previouslyFocusedRect) {
        final SparseArrayCompat<AccessibilityNodeInfoCompat> allNodes = getAllNodes();

        final int focusedNodeId = mKeyboardFocusedVirtualViewId;
        final AccessibilityNodeInfoCompat focusedNode =
                focusedNodeId == INVALID_ID ? null : allNodes.get(focusedNodeId);

        final AccessibilityNodeInfoCompat nextFocusedNode;
        switch (direction) {
            case View.FOCUS_FORWARD:
            case View.FOCUS_BACKWARD:
                final boolean isLayoutRtl =
                        ViewCompat.getLayoutDirection(mHost) == ViewCompat.LAYOUT_DIRECTION_RTL;
                nextFocusedNode = FocusStrategy.findNextFocusInRelativeDirection(allNodes,
                        SPARSE_VALUES_ADAPTER, NODE_ADAPTER, focusedNode, direction, isLayoutRtl,
                        false);
                break;
            case View.FOCUS_LEFT:
            case View.FOCUS_UP:
            case View.FOCUS_RIGHT:
            case View.FOCUS_DOWN:
                final Rect selectedRect = new Rect();
                if (mKeyboardFocusedVirtualViewId != INVALID_ID) {
                    // Focus is moving from a virtual view within the host.
                    getBoundsInParent(mKeyboardFocusedVirtualViewId, selectedRect);
                } else if (previouslyFocusedRect != null) {
                    // Focus is moving from a real view outside the host.
                    selectedRect.set(previouslyFocusedRect);
                } else {
                    // Focus is moving from... somewhere? Make a guess.
                    // Usually this happens when another view was too lazy
                    // to pass the previously focused rect (ex. ScrollView
                    // when moving UP or DOWN).
                    guessPreviouslyFocusedRect(mHost, direction, selectedRect);
                }
                nextFocusedNode = FocusStrategy.findNextFocusInAbsoluteDirection(allNodes,
                        SPARSE_VALUES_ADAPTER, NODE_ADAPTER, focusedNode, selectedRect, direction);
                break;
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_FORWARD, FOCUS_BACKWARD, FOCUS_UP, FOCUS_DOWN, "
                        + "FOCUS_LEFT, FOCUS_RIGHT}.");
        }

        final int nextFocusedNodeId;
        if (nextFocusedNode == null) {
            nextFocusedNodeId = INVALID_ID;
        } else {
            final int index = allNodes.indexOfValue(nextFocusedNode);
            nextFocusedNodeId = allNodes.keyAt(index);
        }

        return requestKeyboardFocusForVirtualView(nextFocusedNodeId);
    }

    private SparseArrayCompat<AccessibilityNodeInfoCompat> getAllNodes() {
        final List<Integer> virtualViewIds = new ArrayList<>();
        getVisibleVirtualViews(virtualViewIds);

        final SparseArrayCompat<AccessibilityNodeInfoCompat> allNodes = new SparseArrayCompat<>();
        for (int virtualViewId = 0; virtualViewId < virtualViewIds.size(); virtualViewId++) {
            final AccessibilityNodeInfoCompat virtualView = createNodeForChild(virtualViewId);
            allNodes.put(virtualViewId, virtualView);
        }

        return allNodes;
    }

    /**
     * Obtains a best guess for the previously focused rect for keyboard focus
     * moving in the specified direction.
     *
     * @param host the view into which focus is moving
     * @param direction the absolute direction in which focus is moving
     * @param outBounds the rect to populate with the best-guess bounds for the
     *                  previous focus rect
     */
    private static Rect guessPreviouslyFocusedRect(@NonNull View host,
            @FocusRealDirection int direction, @NonNull Rect outBounds) {
        final int w = host.getWidth();
        final int h = host.getHeight();

        switch (direction) {
            case View.FOCUS_LEFT:
                outBounds.set(w, 0, w, h);
                break;
            case View.FOCUS_UP:
                outBounds.set(0, h, w, h);
                break;
            case View.FOCUS_RIGHT:
                outBounds.set(-1, 0, -1, h);
                break;
            case View.FOCUS_DOWN:
                outBounds.set(0, -1, w, -1);
                break;
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }

        return outBounds;
    }

    /**
     * Performs a click action on the keyboard focused virtual view, if any.
     *
     * @return {@code true} if the click action was performed successfully or
     *         {@code false} otherwise
     */
    private boolean clickKeyboardFocusedVirtualView() {
        return mKeyboardFocusedVirtualViewId != INVALID_ID && onPerformActionForVirtualView(
                mKeyboardFocusedVirtualViewId, AccessibilityNodeInfoCompat.ACTION_CLICK, null);
    }

    /**
     * Populates an event of the specified type with information about an item
     * and attempts to send it up through the view hierarchy.
     * <p>
     * You should call this method after performing a user action that normally
     * fires an accessibility event, such as clicking on an item.
     * <p>
     * <pre>public void performItemClick(T item) {
     *   ...
     *   sendEventForVirtualViewId(item.id, AccessibilityEvent.TYPE_VIEW_CLICKED);
     * }
     * </pre>
     *
     * @param virtualViewId the identifier of the virtual view for which to
     *                      send an event
     * @param eventType the type of event to send
     * @return {@code true} if the event was sent successfully, {@code false}
     *         otherwise
     */
    public final boolean sendEventForVirtualView(int virtualViewId, int eventType) {
        if ((virtualViewId == INVALID_ID) || !mManager.isEnabled()) {
            return false;
        }

        final ViewParent parent = mHost.getParent();
        if (parent == null) {
            return false;
        }

        final AccessibilityEvent event = createEvent(virtualViewId, eventType);
        return ViewParentCompat.requestSendAccessibilityEvent(parent, mHost, event);
    }

    /**
     * Notifies the accessibility framework that the properties of the parent
     * view have changed.
     * <p>
     * You <strong>must</strong> call this method after adding or removing
     * items from the parent view.
     */
    public final void invalidateRoot() {
        invalidateVirtualView(HOST_ID, AccessibilityEventCompat.CONTENT_CHANGE_TYPE_SUBTREE);
    }

    /**
     * Notifies the accessibility framework that the properties of a particular
     * item have changed.
     * <p>
     * You <strong>must</strong> call this method after changing any of the
     * properties set in
     * {@link #onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)}.
     *
     * @param virtualViewId the virtual view id to invalidate, or
     *                      {@link #HOST_ID} to invalidate the root view
     * @see #invalidateVirtualView(int, int)
     */
    public final void invalidateVirtualView(int virtualViewId) {
        invalidateVirtualView(virtualViewId,
                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED);
    }

    /**
     * Notifies the accessibility framework that the properties of a particular
     * item have changed.
     * <p>
     * You <strong>must</strong> call this method after changing any of the
     * properties set in
     * {@link #onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)}.
     *
     * @param virtualViewId the virtual view id to invalidate, or
     *                      {@link #HOST_ID} to invalidate the root view
     * @param changeTypes the bit mask of change types. May be {@code 0} for the
     *                    default (undefined) change type or one or more of:
     *         <ul>
     *         <li>{@link AccessibilityEventCompat#CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION}
     *         <li>{@link AccessibilityEventCompat#CONTENT_CHANGE_TYPE_SUBTREE}
     *         <li>{@link AccessibilityEventCompat#CONTENT_CHANGE_TYPE_TEXT}
     *         <li>{@link AccessibilityEventCompat#CONTENT_CHANGE_TYPE_UNDEFINED}
     *         </ul>
     */
    public final void invalidateVirtualView(int virtualViewId, int changeTypes) {
        if (virtualViewId != INVALID_ID && mManager.isEnabled()) {
            final ViewParent parent = mHost.getParent();
            if (parent != null) {
                // Send events up the hierarchy so they can be coalesced.
                final AccessibilityEvent event = createEvent(virtualViewId,
                        AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
                AccessibilityEventCompat.setContentChangeTypes(event, changeTypes);
                ViewParentCompat.requestSendAccessibilityEvent(parent, mHost, event);
            }
        }
    }

    /**
     * Returns the virtual view ID for the currently accessibility focused
     * item.
     *
     * @return the identifier of the virtual view that has accessibility focus
     *         or {@link #INVALID_ID} if no virtual view has accessibility
     *         focus
     * @deprecated Use {@link #getAccessibilityFocusedVirtualViewId()}.
     */
    @Deprecated
    public int getFocusedVirtualView() {
        return getAccessibilityFocusedVirtualViewId();
    }

    /**
     * Called when the focus state of a virtual view changes.
     *
     * @param virtualViewId the virtual view identifier
     * @param hasFocus      {@code true} if the view has focus, {@code false}
     *                      otherwise
     */
    protected void onVirtualViewKeyboardFocusChanged(int virtualViewId, boolean hasFocus) {
        // Stub method.
    }

    /**
     * Sets the currently hovered item, sending hover accessibility events as
     * necessary to maintain the correct state.
     *
     * @param virtualViewId the virtual view id for the item currently being
     *                      hovered, or {@link #INVALID_ID} if no item is
     *                      hovered within the parent view
     */
    private void updateHoveredVirtualView(int virtualViewId) {
        if (mHoveredVirtualViewId == virtualViewId) {
            return;
        }

        final int previousVirtualViewId = mHoveredVirtualViewId;
        mHoveredVirtualViewId = virtualViewId;

        // Stay consistent with framework behavior by sending ENTER/EXIT pairs
        // in reverse order. This is accurate as of API 18.
        sendEventForVirtualView(virtualViewId, AccessibilityEventCompat.TYPE_VIEW_HOVER_ENTER);
        sendEventForVirtualView(
                previousVirtualViewId, AccessibilityEventCompat.TYPE_VIEW_HOVER_EXIT);
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} for the specified
     * virtual view id, which includes the host view ({@link #HOST_ID}).
     *
     * @param virtualViewId the virtual view id for the item for which to
     *                      construct an event
     * @param eventType the type of event to construct
     * @return an {@link AccessibilityEvent} populated with information about
     *         the specified item
     */
    private AccessibilityEvent createEvent(int virtualViewId, int eventType) {
        switch (virtualViewId) {
            case HOST_ID:
                return createEventForHost(eventType);
            default:
                return createEventForChild(virtualViewId, eventType);
        }
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} for the host node.
     *
     * @param eventType the type of event to construct
     * @return an {@link AccessibilityEvent} populated with information about
     *         the specified item
     */
    private AccessibilityEvent createEventForHost(int eventType) {
        final AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        ViewCompat.onInitializeAccessibilityEvent(mHost, event);
        return event;
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(host, event);

        // Allow the client to populate the event.
        onPopulateEventForHost(event);
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} populated with
     * information about the specified item.
     *
     * @param virtualViewId the virtual view id for the item for which to
     *                      construct an event
     * @param eventType the type of event to construct
     * @return an {@link AccessibilityEvent} populated with information about
     *         the specified item
     */
    private AccessibilityEvent createEventForChild(int virtualViewId, int eventType) {
        final AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        final AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
        final AccessibilityNodeInfoCompat node = obtainAccessibilityNodeInfo(virtualViewId);

        // Allow the client to override these properties,
        record.getText().add(node.getText());
        record.setContentDescription(node.getContentDescription());
        record.setScrollable(node.isScrollable());
        record.setPassword(node.isPassword());
        record.setEnabled(node.isEnabled());
        record.setChecked(node.isChecked());

        // Allow the client to populate the event.
        onPopulateEventForVirtualView(virtualViewId, event);

        // Make sure the developer is following the rules.
        if (event.getText().isEmpty() && (event.getContentDescription() == null)) {
            throw new RuntimeException("Callbacks must add text or a content description in "
                    + "populateEventForVirtualViewId()");
        }

        // Don't allow the client to override these properties.
        record.setClassName(node.getClassName());
        record.setSource(mHost, virtualViewId);
        event.setPackageName(mHost.getContext().getPackageName());

        return event;
    }

    /**
     * Obtains a populated {@link AccessibilityNodeInfoCompat} for the
     * virtual view with the specified identifier.
     * <p>
     * This method may be called with identifier {@link #HOST_ID} to obtain a
     * node for the host view.
     *
     * @param virtualViewId the identifier of the virtual view for which to
     *                      construct a node
     * @return an {@link AccessibilityNodeInfoCompat} populated with information
     *         about the specified item
     */
    @NonNull
    private AccessibilityNodeInfoCompat obtainAccessibilityNodeInfo(int virtualViewId) {
        if (virtualViewId == HOST_ID) {
            return createNodeForHost();
        }

        return createNodeForChild(virtualViewId);
    }

    /**
     * Constructs and returns an {@link AccessibilityNodeInfoCompat} for the
     * host view populated with its virtual descendants.
     *
     * @return an {@link AccessibilityNodeInfoCompat} for the parent node
     */
    @NonNull
    private AccessibilityNodeInfoCompat createNodeForHost() {
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain(mHost);
        ViewCompat.onInitializeAccessibilityNodeInfo(mHost, info);

        // Add the virtual descendants.
        final ArrayList<Integer> virtualViewIds = new ArrayList<>();
        getVisibleVirtualViews(virtualViewIds);

        final int realNodeCount = info.getChildCount();
        if (realNodeCount > 0 && virtualViewIds.size() > 0) {
            throw new RuntimeException("Views cannot have both real and virtual children");
        }

        for (int i = 0, count = virtualViewIds.size(); i < count; i++) {
            info.addChild(mHost, virtualViewIds.get(i));
        }

        return info;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);

        // Allow the client to populate the host node.
        onPopulateNodeForHost(info);
    }

    /**
     * Constructs and returns an {@link AccessibilityNodeInfoCompat} for the
     * specified item. Automatically manages accessibility focus actions.
     * <p>
     * Allows the implementing class to specify most node properties, but
     * overrides the following:
     * <ul>
     * <li>{@link AccessibilityNodeInfoCompat#setPackageName}
     * <li>{@link AccessibilityNodeInfoCompat#setClassName}
     * <li>{@link AccessibilityNodeInfoCompat#setParent(View)}
     * <li>{@link AccessibilityNodeInfoCompat#setSource(View, int)}
     * <li>{@link AccessibilityNodeInfoCompat#setVisibleToUser}
     * <li>{@link AccessibilityNodeInfoCompat#setBoundsInScreen(Rect)}
     * </ul>
     * <p>
     * Uses the bounds of the parent view and the parent-relative bounding
     * rectangle specified by
     * {@link AccessibilityNodeInfoCompat#getBoundsInParent} to automatically
     * update the following properties:
     * <ul>
     * <li>{@link AccessibilityNodeInfoCompat#setVisibleToUser}
     * <li>{@link AccessibilityNodeInfoCompat#setBoundsInParent}
     * </ul>
     *
     * @param virtualViewId the virtual view id for item for which to construct
     *                      a node
     * @return an {@link AccessibilityNodeInfoCompat} for the specified item
     */
    @NonNull
    private AccessibilityNodeInfoCompat createNodeForChild(int virtualViewId) {
        final AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();

        // Ensure the client has good defaults.
        node.setEnabled(true);
        node.setFocusable(true);
        node.setClassName(DEFAULT_CLASS_NAME);
        node.setBoundsInParent(INVALID_PARENT_BOUNDS);
        node.setBoundsInScreen(INVALID_PARENT_BOUNDS);

        // Allow the client to populate the node.
        onPopulateNodeForVirtualView(virtualViewId, node);

        // Make sure the developer is following the rules.
        if ((node.getText() == null) && (node.getContentDescription() == null)) {
            throw new RuntimeException("Callbacks must add text or a content description in "
                    + "populateNodeForVirtualViewId()");
        }

        node.getBoundsInParent(mTempParentRect);
        if (mTempParentRect.equals(INVALID_PARENT_BOUNDS)) {
            throw new RuntimeException("Callbacks must set parent bounds in "
                    + "populateNodeForVirtualViewId()");
        }

        final int actions = node.getActions();
        if ((actions & AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS) != 0) {
            throw new RuntimeException("Callbacks must not add ACTION_ACCESSIBILITY_FOCUS in "
                    + "populateNodeForVirtualViewId()");
        }
        if ((actions & AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS) != 0) {
            throw new RuntimeException("Callbacks must not add ACTION_CLEAR_ACCESSIBILITY_FOCUS in "
                    + "populateNodeForVirtualViewId()");
        }

        // Don't allow the client to override these properties.
        node.setPackageName(mHost.getContext().getPackageName());
        node.setSource(mHost, virtualViewId);
        node.setParent(mHost);

        // Manage internal accessibility focus state.
        if (mAccessibilityFocusedVirtualViewId == virtualViewId) {
            node.setAccessibilityFocused(true);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
        } else {
            node.setAccessibilityFocused(false);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
        }

        // Manage internal keyboard focus state.
        final boolean isFocused = mKeyboardFocusedVirtualViewId == virtualViewId;
        if (isFocused) {
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS);
        } else if (node.isFocusable()) {
            node.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
        }
        node.setFocused(isFocused);

        // Set the visibility based on the parent bound.
        if (intersectVisibleToUser(mTempParentRect)) {
            node.setVisibleToUser(true);
            node.setBoundsInParent(mTempParentRect);
        }

        // If not explicitly specified, calculate screen-relative bounds and
        // offset for scroll position based on bounds in parent.
        node.getBoundsInScreen(mTempScreenRect);
        if (mTempScreenRect.equals(INVALID_PARENT_BOUNDS)) {
            mHost.getLocationOnScreen(mTempGlobalRect);
            node.getBoundsInParent(mTempScreenRect);
            mTempScreenRect.offset(mTempGlobalRect[0] - mHost.getScrollX(),
                    mTempGlobalRect[1] - mHost.getScrollY());
            node.setBoundsInScreen(mTempScreenRect);
        }

        return node;
    }

    private boolean performAction(int virtualViewId, int action, Bundle arguments) {
        switch (virtualViewId) {
            case HOST_ID:
                return performActionForHost(action, arguments);
            default:
                return performActionForChild(virtualViewId, action, arguments);
        }
    }

    private boolean performActionForHost(int action, Bundle arguments) {
        return ViewCompat.performAccessibilityAction(mHost, action, arguments);
    }

    private boolean performActionForChild(int virtualViewId, int action, Bundle arguments) {
        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS:
                return requestAccessibilityFocus(virtualViewId);
            case AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                return clearAccessibilityFocus(virtualViewId);
            case AccessibilityNodeInfoCompat.ACTION_FOCUS:
                return requestKeyboardFocusForVirtualView(virtualViewId);
            case AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS:
                return clearKeyboardFocusForVirtualView(virtualViewId);
            default:
                return onPerformActionForVirtualView(virtualViewId, action, arguments);
        }
    }

    /**
     * Computes whether the specified {@link Rect} intersects with the visible
     * portion of its parent {@link View}. Modifies {@code localRect} to contain
     * only the visible portion.
     *
     * @param localRect a rectangle in local (parent) coordinates
     * @return whether the specified {@link Rect} is visible on the screen
     */
    private boolean intersectVisibleToUser(Rect localRect) {
        // Missing or empty bounds mean this view is not visible.
        if ((localRect == null) || localRect.isEmpty()) {
            return false;
        }

        // Attached to invisible window means this view is not visible.
        if (mHost.getWindowVisibility() != View.VISIBLE) {
            return false;
        }

        // An invisible predecessor means that this view is not visible.
        ViewParent viewParent = mHost.getParent();
        while (viewParent instanceof View) {
            final View view = (View) viewParent;
            if ((ViewCompat.getAlpha(view) <= 0) || (view.getVisibility() != View.VISIBLE)) {
                return false;
            }
            viewParent = view.getParent();
        }

        // A null parent implies the view is not visible.
        if (viewParent == null) {
            return false;
        }

        // If no portion of the parent is visible, this view is not visible.
        if (!mHost.getLocalVisibleRect(mTempVisibleRect)) {
            return false;
        }

        // Check if the view intersects the visible portion of the parent.
        return localRect.intersect(mTempVisibleRect);
    }

    /**
     * Attempts to give accessibility focus to a virtual view.
     * <p>
     * A virtual view will not actually take focus if
     * {@link AccessibilityManager#isEnabled()} returns false,
     * {@link AccessibilityManager#isTouchExplorationEnabled()} returns false,
     * or the view already has accessibility focus.
     *
     * @param virtualViewId the identifier of the virtual view on which to
     *                      place accessibility focus
     * @return whether this virtual view actually took accessibility focus
     */
    private boolean requestAccessibilityFocus(int virtualViewId) {
        if (!mManager.isEnabled()
                || !AccessibilityManagerCompat.isTouchExplorationEnabled(mManager)) {
            return false;
        }
        // TODO: Check virtual view visibility.
        if (mAccessibilityFocusedVirtualViewId != virtualViewId) {
            // Clear focus from the previously focused view, if applicable.
            if (mAccessibilityFocusedVirtualViewId != INVALID_ID) {
                clearAccessibilityFocus(mAccessibilityFocusedVirtualViewId);
            }

            // Set focus on the new view.
            mAccessibilityFocusedVirtualViewId = virtualViewId;

            // TODO: Only invalidate virtual view bounds.
            mHost.invalidate();
            sendEventForVirtualView(virtualViewId,
                    AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            return true;
        }
        return false;
    }

    /**
     * Attempts to clear accessibility focus from a virtual view.
     *
     * @param virtualViewId the identifier of the virtual view from which to
     *                      clear accessibility focus
     * @return whether this virtual view actually cleared accessibility focus
     */
    private boolean clearAccessibilityFocus(int virtualViewId) {
        if (mAccessibilityFocusedVirtualViewId == virtualViewId) {
            mAccessibilityFocusedVirtualViewId = INVALID_ID;
            mHost.invalidate();
            sendEventForVirtualView(virtualViewId,
                    AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            return true;
        }
        return false;
    }

    /**
     * Attempts to give keyboard focus to a virtual view.
     *
     * @param virtualViewId the identifier of the virtual view on which to
     *                      place keyboard focus
     * @return whether this virtual view actually took keyboard focus
     */
    public final boolean requestKeyboardFocusForVirtualView(int virtualViewId) {
        if (!mHost.isFocused() && !mHost.requestFocus()) {
            // Host must have real keyboard focus.
            return false;
        }

        if (mKeyboardFocusedVirtualViewId == virtualViewId) {
            // The virtual view already has focus.
            return false;
        }

        if (mKeyboardFocusedVirtualViewId != INVALID_ID) {
            clearKeyboardFocusForVirtualView(mKeyboardFocusedVirtualViewId);
        }

        mKeyboardFocusedVirtualViewId = virtualViewId;

        onVirtualViewKeyboardFocusChanged(virtualViewId, true);
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_FOCUSED);

        return true;
    }

    /**
     * Attempts to clear keyboard focus from a virtual view.
     *
     * @param virtualViewId the identifier of the virtual view from which to
     *                      clear keyboard focus
     * @return whether this virtual view actually cleared keyboard focus
     */
    public final boolean clearKeyboardFocusForVirtualView(int virtualViewId) {
        if (mKeyboardFocusedVirtualViewId != virtualViewId) {
            // The virtual view is not focused.
            return false;
        }

        mKeyboardFocusedVirtualViewId = INVALID_ID;

        onVirtualViewKeyboardFocusChanged(virtualViewId, false);
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_FOCUSED);

        return true;
    }

    /**
     * Provides a mapping between view-relative coordinates and logical
     * items.
     *
     * @param x The view-relative x coordinate
     * @param y The view-relative y coordinate
     * @return virtual view identifier for the logical item under
     *         coordinates (x,y) or {@link #HOST_ID} if there is no item at
     *         the given coordinates
     */
    protected abstract int getVirtualViewAt(float x, float y);

    /**
     * Populates a list with the view's visible items. The ordering of items
     * within {@code virtualViewIds} specifies order of accessibility focus
     * traversal.
     *
     * @param virtualViewIds The list to populate with visible items
     */
    protected abstract void getVisibleVirtualViews(List<Integer> virtualViewIds);

    /**
     * Populates an {@link AccessibilityEvent} with information about the
     * specified item.
     * <p>
     * The helper class automatically populates the following fields based on
     * the values set by
     * {@link #onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)},
     * but implementations may optionally override them:
     * <ul>
     * <li>event text, see {@link AccessibilityEvent#getText()}
     * <li>content description, see
     * {@link AccessibilityEvent#setContentDescription(CharSequence)}
     * <li>scrollability, see {@link AccessibilityEvent#setScrollable(boolean)}
     * <li>password state, see {@link AccessibilityEvent#setPassword(boolean)}
     * <li>enabled state, see {@link AccessibilityEvent#setEnabled(boolean)}
     * <li>checked state, see {@link AccessibilityEvent#setChecked(boolean)}
     * </ul>
     * <p>
     * The following required fields are automatically populated by the
     * helper class and may not be overridden:
     * <ul>
     * <li>item class name, set to the value used in
     * {@link #onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)}
     * <li>package name, set to the package of the host view's
     * {@link Context}, see {@link AccessibilityEvent#setPackageName}
     * <li>event source, set to the host view and virtual view identifier,
     * see {@link AccessibilityRecordCompat#setSource(View, int)}
     * </ul>
     *
     * @param virtualViewId The virtual view id for the item for which to
     *            populate the event
     * @param event The event to populate
     */
    protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
        // Default implementation is no-op.
    }

    /**
     * Populates an {@link AccessibilityEvent} with information about the host
     * view.
     * <p>
     * The default implementation is a no-op.
     *
     * @param event the event to populate with information about the host view
     */
    protected void onPopulateEventForHost(AccessibilityEvent event) {
        // Default implementation is no-op.
    }

    /**
     * Populates an {@link AccessibilityNodeInfoCompat} with information
     * about the specified item.
     * <p>
     * Implementations <strong>must</strong> populate the following required
     * fields:
     * <ul>
     * <li>event text, see
     * {@link AccessibilityNodeInfoCompat#setText(CharSequence)} or
     * {@link AccessibilityNodeInfoCompat#setContentDescription(CharSequence)}
     * <li>bounds in parent coordinates, see
     * {@link AccessibilityNodeInfoCompat#setBoundsInParent(Rect)}
     * </ul>
     * <p>
     * The helper class automatically populates the following fields with
     * default values, but implementations may optionally override them:
     * <ul>
     * <li>enabled state, set to {@code true}, see
     * {@link AccessibilityNodeInfoCompat#setEnabled(boolean)}
     * <li>keyboard focusability, set to {@code true}, see
     * {@link AccessibilityNodeInfoCompat#setFocusable(boolean)}
     * <li>item class name, set to {@code android.view.View}, see
     * {@link AccessibilityNodeInfoCompat#setClassName(CharSequence)}
     * </ul>
     * <p>
     * The following required fields are automatically populated by the
     * helper class and may not be overridden:
     * <ul>
     * <li>package name, identical to the package name set by
     * {@link #onPopulateEventForVirtualView(int, AccessibilityEvent)}, see
     * {@link AccessibilityNodeInfoCompat#setPackageName}
     * <li>node source, identical to the event source set in
     * {@link #onPopulateEventForVirtualView(int, AccessibilityEvent)}, see
     * {@link AccessibilityNodeInfoCompat#setSource(View, int)}
     * <li>parent view, set to the host view, see
     * {@link AccessibilityNodeInfoCompat#setParent(View)}
     * <li>visibility, computed based on parent-relative bounds, see
     * {@link AccessibilityNodeInfoCompat#setVisibleToUser(boolean)}
     * <li>accessibility focus, computed based on internal helper state, see
     * {@link AccessibilityNodeInfoCompat#setAccessibilityFocused(boolean)}
     * <li>keyboard focus, computed based on internal helper state, see
     * {@link AccessibilityNodeInfoCompat#setFocused(boolean)}
     * <li>bounds in screen coordinates, computed based on host view bounds,
     * see {@link AccessibilityNodeInfoCompat#setBoundsInScreen(Rect)}
     * </ul>
     * <p>
     * Additionally, the helper class automatically handles keyboard focus and
     * accessibility focus management by adding the appropriate
     * {@link AccessibilityNodeInfoCompat#ACTION_FOCUS},
     * {@link AccessibilityNodeInfoCompat#ACTION_CLEAR_FOCUS},
     * {@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS}, or
     * {@link AccessibilityNodeInfoCompat#ACTION_CLEAR_ACCESSIBILITY_FOCUS}
     * actions. Implementations must <strong>never</strong> manually add these
     * actions.
     * <p>
     * The helper class also automatically modifies parent- and
     * screen-relative bounds to reflect the portion of the item visible
     * within its parent.
     *
     * @param virtualViewId The virtual view identifier of the item for
     *            which to populate the node
     * @param node The node to populate
     */
    protected abstract void onPopulateNodeForVirtualView(
            int virtualViewId, AccessibilityNodeInfoCompat node);

    /**
     * Populates an {@link AccessibilityNodeInfoCompat} with information
     * about the host view.
     * <p>
     * The default implementation is a no-op.
     *
     * @param node the node to populate with information about the host view
     */
    protected void onPopulateNodeForHost(AccessibilityNodeInfoCompat node) {
        // Default implementation is no-op.
    }

    /**
     * Performs the specified accessibility action on the item associated
     * with the virtual view identifier. See
     * {@link AccessibilityNodeInfoCompat#performAction(int, Bundle)} for
     * more information.
     * <p>
     * Implementations <strong>must</strong> handle any actions added manually
     * in
     * {@link #onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)}.
     * <p>
     * The helper class automatically handles focus management resulting
     * from {@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS}
     * and
     * {@link AccessibilityNodeInfoCompat#ACTION_CLEAR_ACCESSIBILITY_FOCUS}
     * actions.
     *
     * @param virtualViewId The virtual view identifier of the item on which
     *            to perform the action
     * @param action The accessibility action to perform
     * @param arguments (Optional) A bundle with additional arguments, or
     *            null
     * @return true if the action was performed
     */
    protected abstract boolean onPerformActionForVirtualView(
            int virtualViewId, int action, Bundle arguments);

    /**
     * Exposes a virtual view hierarchy to the accessibility framework.
     */
    private class MyNodeProvider extends AccessibilityNodeProviderCompat {
        @Override
        public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(int virtualViewId) {
            // The caller takes ownership of the node and is expected to
            // recycle it when done, so always return a copy.
            final AccessibilityNodeInfoCompat node =
                    ExploreByTouchHelper.this.obtainAccessibilityNodeInfo(virtualViewId);
            return AccessibilityNodeInfoCompat.obtain(node);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            return ExploreByTouchHelper.this.performAction(virtualViewId, action, arguments);
        }
    }
}
