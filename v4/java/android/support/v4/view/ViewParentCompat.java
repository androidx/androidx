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

package android.support.v4.view;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Helper for accessing features in {@link ViewParent}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class ViewParentCompat {

    interface ViewParentCompatImpl {
        public boolean requestSendAccessibilityEvent(
                ViewParent parent, View child, AccessibilityEvent event);
    }

    static class ViewParentCompatStubImpl implements ViewParentCompatImpl {
        @Override
        public boolean requestSendAccessibilityEvent(
                ViewParent parent, View child, AccessibilityEvent event) {
            // Emulate what ViewRootImpl does in ICS and above.
            if (child == null) {
                return false;
            }
            final AccessibilityManager manager = (AccessibilityManager) child.getContext()
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);
            manager.sendAccessibilityEvent(event);
            return true;
        }
    }

    static class ViewParentCompatICSImpl extends ViewParentCompatStubImpl {
        @Override
        public boolean requestSendAccessibilityEvent(
                ViewParent parent, View child, AccessibilityEvent event) {
            return ViewParentCompatICS.requestSendAccessibilityEvent(parent, child, event);
        }
    }

    static final ViewParentCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 14) {
            IMPL = new ViewParentCompatICSImpl();
        } else {
            IMPL = new ViewParentCompatStubImpl();
        }
    }

    /*
     * Hide the constructor.
     */
    private ViewParentCompat() {

    }

    /**
     * Called by a child to request from its parent to send an {@link AccessibilityEvent}.
     * The child has already populated a record for itself in the event and is delegating
     * to its parent to send the event. The parent can optionally add a record for itself.
     * <p>
     * Note: An accessibility event is fired by an individual view which populates the
     *       event with a record for its state and requests from its parent to perform
     *       the sending. The parent can optionally add a record for itself before
     *       dispatching the request to its parent. A parent can also choose not to
     *       respect the request for sending the event. The accessibility event is sent
     *       by the topmost view in the view tree.</p>
     *
     * @param parent The parent whose method to invoke.
     * @param child The child which requests sending the event.
     * @param event The event to be sent.
     * @return True if the event was sent.
     */
    public static boolean requestSendAccessibilityEvent(
            ViewParent parent, View child, AccessibilityEvent event) {
        return IMPL.requestSendAccessibilityEvent(parent, child, event);
    }
}
