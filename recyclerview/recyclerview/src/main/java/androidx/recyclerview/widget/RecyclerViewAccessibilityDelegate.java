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

package androidx.recyclerview.widget;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The AccessibilityDelegate used by RecyclerView.
 * <p>
 * This class handles basic accessibility actions and delegates them to LayoutManager.
 */
public class RecyclerViewAccessibilityDelegate extends AccessibilityDelegateCompat {
    final RecyclerView mRecyclerView;
    private final ItemDelegate mItemDelegate;


    public RecyclerViewAccessibilityDelegate(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        AccessibilityDelegateCompat itemDelegate = getItemDelegate();
        if (itemDelegate != null && itemDelegate instanceof ItemDelegate) {
            mItemDelegate = (ItemDelegate) itemDelegate;
        } else {
            mItemDelegate = new ItemDelegate(this);
        }
    }

    boolean shouldIgnore() {
        return mRecyclerView.hasPendingAdapterUpdates();
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        if (super.performAccessibilityAction(host, action, args)) {
            return true;
        }
        if (!shouldIgnore() && mRecyclerView.getLayoutManager() != null) {
            return mRecyclerView.getLayoutManager().performAccessibilityAction(action, args);
        }

        return false;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        if (!shouldIgnore() && mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.getLayoutManager().onInitializeAccessibilityNodeInfo(info);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(host, event);
        if (host instanceof RecyclerView && !shouldIgnore()) {
            RecyclerView rv = (RecyclerView) host;
            if (rv.getLayoutManager() != null) {
                rv.getLayoutManager().onInitializeAccessibilityEvent(event);
            }
        }
    }

    /**
     * Gets the AccessibilityDelegate for an individual item in the RecyclerView.
     * A basic item delegate is provided by default, but you can override this
     * method to provide a custom per-item delegate.
     * For now, returning an {@code AccessibilityDelegateCompat} as opposed to an
     * {@code ItemDelegate} will prevent use of the {@code ViewCompat} accessibility API on
     * item views.
     */
    @NonNull
    public AccessibilityDelegateCompat getItemDelegate() {
        return mItemDelegate;
    }

    /**
     * The default implementation of accessibility delegate for the individual items of the
     * RecyclerView.
     * <p>
     * If you are overriding {@code RecyclerViewAccessibilityDelegate#getItemDelegate()} but still
     * want to keep some default behavior, you can create an instance of this class and delegate to
     * the parent as necessary.
     */
    public static class ItemDelegate extends AccessibilityDelegateCompat {
        final RecyclerViewAccessibilityDelegate mRecyclerViewDelegate;
        private Map<View, AccessibilityDelegateCompat> mOriginalItemDelegates = new WeakHashMap<>();

        /**
         * Creates an item delegate for the given {@code RecyclerViewAccessibilityDelegate}.
         *
         * @param recyclerViewDelegate The parent RecyclerView's accessibility delegate.
         */
        public ItemDelegate(@NonNull RecyclerViewAccessibilityDelegate recyclerViewDelegate) {
            mRecyclerViewDelegate = recyclerViewDelegate;
        }

        /**
         * Saves a reference to the original delegate of the itemView so that it's behavior can be
         * combined with the ItemDelegate's behavior.
         */
        void saveOriginalDelegate(View itemView) {
            AccessibilityDelegateCompat delegate = ViewCompat.getAccessibilityDelegate(itemView);
            if (delegate != null && delegate != this) {
                mOriginalItemDelegates.put(itemView, delegate);
            }
        }

        /**
         * @return The delegate associated with itemView before the view was bound.
         */
        AccessibilityDelegateCompat getAndRemoveOriginalDelegateForItem(View itemView) {
            return mOriginalItemDelegates.remove(itemView);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            if (!mRecyclerViewDelegate.shouldIgnore()
                    && mRecyclerViewDelegate.mRecyclerView.getLayoutManager() != null) {
                mRecyclerViewDelegate.mRecyclerView.getLayoutManager()
                        .onInitializeAccessibilityNodeInfoForItem(host, info);
                AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
                if (originalDelegate != null) {
                    originalDelegate.onInitializeAccessibilityNodeInfo(host, info);
                } else {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                }
            } else {
                super.onInitializeAccessibilityNodeInfo(host, info);
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (!mRecyclerViewDelegate.shouldIgnore()
                    && mRecyclerViewDelegate.mRecyclerView.getLayoutManager() != null) {
                AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
                if (originalDelegate != null) {
                    if (originalDelegate.performAccessibilityAction(host, action, args)) {
                        return true;
                    }
                } else if (super.performAccessibilityAction(host, action, args)) {
                    return true;
                }
                return mRecyclerViewDelegate.mRecyclerView.getLayoutManager()
                        .performAccessibilityActionForItem(host, action, args);
            } else {
                return super.performAccessibilityAction(host, action, args);
            }
        }

        @Override
        public void sendAccessibilityEvent(@NonNull View host, int eventType) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                originalDelegate.sendAccessibilityEvent(host, eventType);
            } else {
                super.sendAccessibilityEvent(host, eventType);
            }
        }

        @Override
        public void sendAccessibilityEventUnchecked(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                originalDelegate.sendAccessibilityEventUnchecked(host, event);
            } else {
                super.sendAccessibilityEventUnchecked(host, event);
            }
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                return originalDelegate.dispatchPopulateAccessibilityEvent(host, event);
            } else {
                return super.dispatchPopulateAccessibilityEvent(host, event);
            }
        }

        @Override
        public void onPopulateAccessibilityEvent(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                originalDelegate.onPopulateAccessibilityEvent(host, event);
            } else {
                super.onPopulateAccessibilityEvent(host, event);
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                originalDelegate.onInitializeAccessibilityEvent(host, event);
            } else {
                super.onInitializeAccessibilityEvent(host, event);
            }
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(@NonNull ViewGroup host,
                @NonNull View child, @NonNull AccessibilityEvent event) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                return originalDelegate.onRequestSendAccessibilityEvent(host, child, event);
            } else {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
        }

        @Override
        @Nullable
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(@NonNull View host) {
            AccessibilityDelegateCompat originalDelegate = mOriginalItemDelegates.get(host);
            if (originalDelegate != null) {
                return originalDelegate.getAccessibilityNodeProvider(host);
            } else {
                return super.getAccessibilityNodeProvider(host);
            }
        }
    }
}

