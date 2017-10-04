/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.view.accessibility;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeProvider}.
 */
public class AccessibilityNodeProviderCompat {
    @RequiresApi(16)
    static class AccessibilityNodeProviderApi16 extends AccessibilityNodeProvider {
        final AccessibilityNodeProviderCompat mCompat;

        AccessibilityNodeProviderApi16(AccessibilityNodeProviderCompat compat) {
            mCompat = compat;
        }

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            final AccessibilityNodeInfoCompat compatInfo =
                    mCompat.createAccessibilityNodeInfo(virtualViewId);
            if (compatInfo == null) {
                return null;
            } else {
                return compatInfo.unwrap();
            }
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                String text, int virtualViewId) {
            final List<AccessibilityNodeInfoCompat> compatInfos =
                    mCompat.findAccessibilityNodeInfosByText(text, virtualViewId);
            if (compatInfos == null) {
                return null;
            } else {
                final List<AccessibilityNodeInfo> infoList = new ArrayList<>();
                final int infoCount = compatInfos.size();
                for (int i = 0; i < infoCount; i++) {
                    AccessibilityNodeInfoCompat infoCompat = compatInfos.get(i);
                    infoList.add(infoCompat.unwrap());
                }
                return infoList;
            }
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            return mCompat.performAction(virtualViewId, action, arguments);
        }
    }

    @RequiresApi(19)
    static class AccessibilityNodeProviderApi19 extends AccessibilityNodeProviderApi16 {
        AccessibilityNodeProviderApi19(AccessibilityNodeProviderCompat compat) {
            super(compat);
        }

        @Override
        public AccessibilityNodeInfo findFocus(int focus) {
            final AccessibilityNodeInfoCompat compatInfo = mCompat.findFocus(focus);
            if (compatInfo == null) {
                return null;
            } else {
                return compatInfo.unwrap();
            }
        }
    }

    /**
     * The virtual id for the hosting View.
     */
    public static final int HOST_VIEW_ID = -1;

    private final Object mProvider;

    /**
     * Creates a new instance.
     */
    public AccessibilityNodeProviderCompat() {
        if (Build.VERSION.SDK_INT >= 19) {
            mProvider = new AccessibilityNodeProviderApi19(this);
        } else if (Build.VERSION.SDK_INT >= 16) {
            mProvider = new AccessibilityNodeProviderApi16(this);
        } else {
            mProvider = null;
        }
    }

    /**
     * Creates a new instance wrapping an
     * {@link android.view.accessibility.AccessibilityNodeProvider}.
     *
     * @param provider The provider.
     */
    public AccessibilityNodeProviderCompat(Object provider) {
        mProvider = provider;
    }

    /**
     * @return The wrapped {@link android.view.accessibility.AccessibilityNodeProvider}.
     */
    public Object getProvider() {
        return mProvider;
    }

    /**
     * Returns an {@link AccessibilityNodeInfoCompat} representing a virtual view,
     * i.e. a descendant of the host View, with the given <code>virtualViewId</code>
     * or the host View itself if <code>virtualViewId</code> equals to {@link #HOST_VIEW_ID}.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     * The implementer is responsible for obtaining an accessibility node info from the
     * pool of reusable instances and setting the desired properties of the node info
     * before returning it.
     * </p>
     *
     * @param virtualViewId A client defined virtual view id.
     * @return A populated {@link AccessibilityNodeInfoCompat} for a virtual descendant
     *     or the host View.
     *
     * @see AccessibilityNodeInfoCompat
     */
    @Nullable
    public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(int virtualViewId) {
        return null;
    }

    /**
     * Performs an accessibility action on a virtual view, i.e. a descendant of the
     * host View, with the given <code>virtualViewId</code> or the host View itself
     * if <code>virtualViewId</code> equals to {@link #HOST_VIEW_ID}.
     *
     * @param virtualViewId A client defined virtual view id.
     * @param action The action to perform.
     * @param arguments Optional arguments.
     * @return True if the action was performed.
     *
     * @see #createAccessibilityNodeInfo(int)
     * @see AccessibilityNodeInfoCompat
     */
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        return false;
    }

    /**
     * Finds {@link AccessibilityNodeInfoCompat}s by text. The match is case insensitive
     * containment. The search is relative to the virtual view, i.e. a descendant of the
     * host View, with the given <code>virtualViewId</code> or the host View itself
     * <code>virtualViewId</code> equals to {@link #HOST_VIEW_ID}.
     *
     * @param virtualViewId A client defined virtual view id which defined
     *     the root of the tree in which to perform the search.
     * @param text The searched text.
     * @return A list of node info.
     *
     * @see #createAccessibilityNodeInfo(int)
     * @see AccessibilityNodeInfoCompat
     */
    @Nullable
    public List<AccessibilityNodeInfoCompat> findAccessibilityNodeInfosByText(String text,
            int virtualViewId) {
        return null;
    }

    /**
     * Find the virtual view, i.e. a descendant of the host View, that has the
     * specified focus type.
     *
     * @param focus The focus to find. One of
     *            {@link AccessibilityNodeInfoCompat#FOCUS_INPUT} or
     *            {@link AccessibilityNodeInfoCompat#FOCUS_ACCESSIBILITY}.
     * @return The node info of the focused view or null.
     * @see AccessibilityNodeInfoCompat#FOCUS_INPUT
     * @see AccessibilityNodeInfoCompat#FOCUS_ACCESSIBILITY
     */
    @Nullable
    public AccessibilityNodeInfoCompat findFocus(int focus) {
        return null;
    }
}
