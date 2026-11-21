/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.view.platform;

import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_DISPATCH;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NONE;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.input.PointerInputEvent;
import androidx.compose.remote.player.core.RemoteDocument;

import org.jspecify.annotations.NonNull;

/**
 * Translates Android MotionEvents into PointerInputEvents and tracks velocity.
 *
 * <p>Only the first finger of a gesture is forwarded to the document. Additional fingers are
 * ignored, and the gesture ends when the first finger is lifted, even if other fingers are still
 * down.
 */
class PointerInputManager {

    private VelocityTracker mVelocityTracker = null;

    /**
     * Id of the pointer whose events are forwarded: the first finger of the current gesture, or
     * {@link MotionEvent#INVALID_POINTER_ID} when that finger is not down.
     */
    private int mTrackedPointerId = MotionEvent.INVALID_POINTER_ID;

    /**
     * Dispatches a motion event to the document event manager.
     *
     * @param remoteContext current remote context
     * @param remoteDocument current remote document
     * @param event motion event to dispatch
     * @return status code from event dispatch, or {@link
     *     androidx.compose.remote.core.input.PointerInputRouter#POINTER_STATUS_NONE} if the event
     *     was not forwarded to the document
     */
    public int dispatchTouchEvent(
            @NonNull RemoteContext remoteContext,
            @NonNull RemoteDocument remoteDocument,
            @NonNull MotionEvent event) {
        int subtype;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // The first finger goes down.
                mTrackedPointerId = event.getPointerId(event.getActionIndex());
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(event);
                subtype = POINTER_SUBTYPE_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                // The first finger was already lifted: only other fingers are moving.
                if (mTrackedPointerId == MotionEvent.INVALID_POINTER_ID) {
                    return POINTER_STATUS_NONE;
                }
                mVelocityTracker.addMovement(event);
                subtype = POINTER_SUBTYPE_MOVE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // A finger goes up while another one stays down. It ends the gesture if it is
                // the first finger. Any other finger is ignored.
                if (event.getPointerId(event.getActionIndex()) != mTrackedPointerId) {
                    return POINTER_STATUS_NONE;
                }
                mVelocityTracker.computeCurrentVelocity(1000);
                subtype = POINTER_SUBTYPE_UP;
                break;
            case MotionEvent.ACTION_UP:
                // The last finger goes up. The gesture has already ended if this is not the
                // first finger.
                if (mTrackedPointerId == MotionEvent.INVALID_POINTER_ID) {
                    return POINTER_STATUS_NONE;
                }
                mVelocityTracker.computeCurrentVelocity(1000);
                subtype = POINTER_SUBTYPE_UP;
                break;
            case MotionEvent.ACTION_CANCEL:
                // The first finger was already lifted: the gesture has already ended.
                if (mTrackedPointerId == MotionEvent.INVALID_POINTER_ID) {
                    return POINTER_STATUS_NONE;
                }
                mVelocityTracker.clear();
                subtype = POINTER_SUBTYPE_CANCEL;
                break;
            default:
                // ACTION_POINTER_DOWN (an additional finger goes down) and anything else.
                return POINTER_STATUS_NONE;
        }

        int index = event.findPointerIndex(mTrackedPointerId);
        if (index < 0) {
            return POINTER_STATUS_NONE;
        }
        int pointerId = mTrackedPointerId;
        float x = event.getX(index);
        float y = event.getY(index);
        if (subtype == POINTER_SUBTYPE_UP || subtype == POINTER_SUBTYPE_CANCEL) {
            mTrackedPointerId = MotionEvent.INVALID_POINTER_ID;
        }

        // Keep the document level touch variables up to date so that plain expressions
        // referencing TOUCH_POS_X / TOUCH_POS_Y / TOUCH_EVENT_TIME keep working, as they did
        // with the legacy touch path. These are in window coordinates; handlers reached through
        // the PointerInputRouter receive component local coordinates in the event data instead.
        remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_X, x);
        remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_Y, y);
        remoteContext.loadFloat(
                RemoteContext.ID_TOUCH_EVENT_TIME, remoteContext.getAnimationTime());

        PointerInputEvent pointerInputEvent = PointerInputEvent.obtain();
        pointerInputEvent.setPass(POINTER_PASS_DISPATCH);
        pointerInputEvent.setSubtype(subtype);
        float[] data = pointerInputEvent.getData();

        assert data != null && data.length >= 5;
        data[0] = pointerId;
        data[1] = x;
        data[2] = y;
        data[3] = mVelocityTracker.getXVelocity(pointerId);
        data[4] = mVelocityTracker.getYVelocity(pointerId);

        return remoteContext.getEventManager().dispatchEvent(
                remoteContext,
                remoteDocument.getDocument(),
                pointerInputEvent
        );
    }
}
