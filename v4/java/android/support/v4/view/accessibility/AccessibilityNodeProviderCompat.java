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
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeProvider}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class AccessibilityNodeProviderCompat {

    interface AccessibilityNodeProviderImpl {
        public Object newAccessibilityNodeProviderBridge(AccessibilityNodeProviderCompat compat);
    }

    static class AccessibilityNodeProviderStubImpl implements AccessibilityNodeProviderImpl {
        @Override
        public Object newAccessibilityNodeProviderBridge(AccessibilityNodeProviderCompat compat) {
            return null;
        }
    }

    static class AccessibilityNodeProviderJellyBeanImpl extends AccessibilityNodeProviderStubImpl {
        @Override
        public Object newAccessibilityNodeProviderBridge(
                final AccessibilityNodeProviderCompat compat) {
            return AccessibilityNodeProviderCompatJellyBean.newAccessibilityNodeProviderBridge(
                    new AccessibilityNodeProviderCompatJellyBean.AccessibilityNodeInfoBridge() {
                        @Override
                        public boolean performAccessibilityAction(int action, int virtualViewId) {
                            return compat.performAccessibilityAction(action, virtualViewId);
                        }

                        @Override
                        public List<Object> findAccessibilityNodeInfosByText(
                                            String text, int virtualViewId) {
                            List<AccessibilityNodeInfoCompat> compatInfos =
                                compat.findAccessibilityNodeInfosByText(text, virtualViewId);
                            List<Object> infos = new ArrayList<Object>();
                            final int infoCount = compatInfos.size();
                            for (int i = 0; i < infoCount; i++) {
                                AccessibilityNodeInfoCompat infoCompat = compatInfos.get(i);
                                infos.add(infoCompat.getInfo());
                            }
                            return infos;
                        }

                        @Override
                        public Object createAccessibilityNodeInfo(
                                int virtualViewId) {
                            return compat.createAccessibilityNodeInfo(virtualViewId).getInfo();
                        }
                    });
        }
    }

    private static final AccessibilityNodeProviderImpl IMPL;

    private final Object mProvider;

    static {
        // TODO: Update the conditional to use SDK_INT when we have an SDK version set.
        //       (tracked by bug:5947249)
        if (Build.VERSION.CODENAME.equals("JellyBean")) { // JellyBean
            IMPL = new AccessibilityNodeProviderJellyBeanImpl();
        } else {
            IMPL = new AccessibilityNodeProviderStubImpl();
        }
    }

    /**
     * Creates a new instance.
     */
    public AccessibilityNodeProviderCompat() {
        mProvider = IMPL.newAccessibilityNodeProviderBridge(this);
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
     * or the host View itself if <code>virtualViewId</code> equals to {@link View#NO_ID}.
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
    public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(int virtualViewId) {
        return null;
    }

    /**
     * Performs an accessibility action on a virtual view, i.e. a descendant of the
     * host View, with the given <code>virtualViewId</code> or the host View itself
     * if <code>virtualViewId</code> equals to {@link View#NO_ID}.
     *
     * @param action The action to perform.
     * @param virtualViewId A client defined virtual view id.
     * @return True if the action was performed.
     *
     * @see #createAccessibilityNodeInfo(int)
     * @see AccessibilityNodeInfoCompat
     */
    public boolean performAccessibilityAction(int action, int virtualViewId) {
        return false;
    }

    /**
     * Finds {@link AccessibilityNodeInfoCompat}s by text. The match is case insensitive
     * containment. The search is relative to the virtual view, i.e. a descendant of the
     * host View, with the given <code>virtualViewId</code> or the host View itself
     * <code>virtualViewId</code> equals to {@link View#NO_ID}.
     *
     * @param virtualViewId A client defined virtual view id which defined
     *     the root of the tree in which to perform the search.
     * @param text The searched text.
     * @return A list of node info.
     *
     * @see #createAccessibilityNodeInfo(int)
     * @see AccessibilityNodeInfoCompat
     */
    public List<AccessibilityNodeInfoCompat> findAccessibilityNodeInfosByText(String text,
            int virtualViewId) {
        return null;
    }
}
