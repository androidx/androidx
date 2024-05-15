/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.core.util.Preconditions.checkNotNull;

import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.ArrayMap;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.TouchDelegateInfo;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper class to handle situations where you want multiple views to have a larger touch area than
 * its actual view bounds. Those views whose touch area is changed is called the delegate view. This
 * class should be used by an ancestor of the delegate. To use a TouchDelegateComposite, first
 * create an instance that specifies the bounds that should be mapped to the delegate and the
 * delegate view itself.
 *
 * <p>The ancestor should then forward all of its touch events received in its {@link
 * android.view.View#onTouchEvent(MotionEvent)} to {@link #onTouchEvent(MotionEvent)}.
 */
class TouchDelegateComposite extends TouchDelegate {

    @NonNull private final WeakHashMap<View, DelegateInfo> mDelegates = new WeakHashMap<>();

    /**
     * Constructor
     *
     * @param delegateView The view that should receive motion events.
     * @param actualBounds The hit rect of the view.
     * @param extendedBounds The hit rect to be delegated.
     */
    TouchDelegateComposite(
            @NonNull View delegateView, @NonNull Rect actualBounds, @NonNull Rect extendedBounds) {
        super(new Rect(), delegateView);
        mDelegates.put(delegateView, new DelegateInfo(delegateView, actualBounds, extendedBounds));
    }

    @VisibleForTesting
    TouchDelegateComposite(
            @NonNull View delegateView,
            @NonNull Rect actualBounds,
            @NonNull Rect extendedBounds,
            @NonNull TouchDelegate touchDelegate) {
        super(new Rect(), delegateView);
        mDelegates.put(delegateView, new DelegateInfo(actualBounds, extendedBounds, touchDelegate));
    }

    void mergeFrom(@NonNull TouchDelegateComposite touchDelegate) {
        mDelegates.putAll(touchDelegate.mDelegates);
    }

    void removeDelegate(@NonNull View delegateView) {
        mDelegates.remove(delegateView);
    }

    boolean isEmpty() {
        return mDelegates.isEmpty();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean eventForwarded = false;
        float x = event.getX();
        float y = event.getY();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Only forward the ACTION_DOWN touch event to the delegate view whose extended bounds
            // contains the touch point, and with its ACTUAL bound closest to the touch point.
            View view = null;
            int ix = (int) x;
            int iy = (int) y;
            int sqDistance = Integer.MAX_VALUE;
            for (Map.Entry<View, DelegateInfo> entry : mDelegates.entrySet()) {
                if (entry.getValue().mExtendedBounds.contains(ix, iy)) {
                    int sd = squaredDistance(entry.getValue().mActualBounds, ix, iy);
                    if (sd < sqDistance) {
                        sqDistance = sd;
                        view = entry.getKey();
                    }
                }
            }
            if (view == null) {
                return false;
            }
            return checkNotNull(mDelegates.get(view)).mTouchDelegate.onTouchEvent(event);
        } else {
            // For other motion event, forward to ALL the delegate view whose extended bounds with
            // touch slop contains the touch point.
            for (DelegateInfo delegateInfo : mDelegates.values()) {
                // set the event location back to the original coordinates, which might get offset
                // by the previous TouchDelegate#onTouchEvent call.
                event.setLocation(x, y);
                eventForwarded |= delegateInfo.mTouchDelegate.onTouchEvent(event);
            }
        }
        return eventForwarded;
    }

    @SuppressLint("ClassVerificationFailure")
    @Override
    @NonNull
    public AccessibilityNodeInfo.TouchDelegateInfo getTouchDelegateInfo() {
        if (VERSION.SDK_INT >= VERSION_CODES.Q && !mDelegates.isEmpty()) {
            Map<Region, View> targetMap = new ArrayMap<>(mDelegates.size());
            for (Map.Entry<View, DelegateInfo> entry : mDelegates.entrySet()) {
                AccessibilityNodeInfo.TouchDelegateInfo info =
                        entry.getValue().mTouchDelegate.getTouchDelegateInfo();
                if (info.getRegionCount() > 0) {
                    targetMap.put(info.getRegionAt(0), entry.getKey());
                }
            }
            return new TouchDelegateInfo(targetMap);
        } else {
            return super.getTouchDelegateInfo();
        }
    }

    /** Calculate the squared distance from a point to a rectangle. */
    private int squaredDistance(@NonNull Rect rect, int pointX, int pointY) {
        int deltaX = max(max(rect.left - pointX, 0), pointX - rect.right);
        int deltaY = max(max(rect.top - pointY, 0), pointY - rect.bottom);
        return deltaX * deltaX + deltaY * deltaY;
    }

    private static final class DelegateInfo {
        @NonNull final Rect mActualBounds;
        @NonNull final Rect mExtendedBounds;
        @NonNull final TouchDelegate mTouchDelegate;

        DelegateInfo(
                @NonNull View delegateView,
                @NonNull Rect actualBounds,
                @NonNull Rect extendedBounds) {
            mActualBounds = actualBounds;
            mExtendedBounds = extendedBounds;
            mTouchDelegate = new TouchDelegate(extendedBounds, delegateView);
        }

        private DelegateInfo(
                @NonNull Rect actualBounds,
                @NonNull Rect extendedBounds,
                @NonNull TouchDelegate touchDelegate) {
            mActualBounds = actualBounds;
            mExtendedBounds = extendedBounds;
            mTouchDelegate = touchDelegate;
        }
    }
}
