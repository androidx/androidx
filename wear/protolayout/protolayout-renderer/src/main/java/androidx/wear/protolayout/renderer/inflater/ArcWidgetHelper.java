/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.isRtlLayoutDirectionFromLocale;

import static java.lang.Math.min;

import android.view.View;

import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection;

import org.jspecify.annotations.NonNull;

final class ArcWidgetHelper {
    /**
     * Returns true when the given point is inside the arc. In particular, the coordinates should be
     * considered as if the arc was drawn centered at the default angle (12 o clock).
     */
    // This is Copy from
    // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:wear/wear/src/main/java/androidx/wear/widget/CurvedTextView.java;l=248;bpv=1;bpt=0;drc=03209658917989bcc65d389ee19f83ec6a53174e
    static boolean isPointInsideArcArea(
            View view, float x, float y, float arcWidth, float arcSweepAngle) {
        float radius2 = min(view.getWidth(), view.getHeight()) / 2f - view.getPaddingTop();
        float radius1 = radius2 - arcWidth;

        float dx = x - view.getWidth() / 2f;
        float dy = y - view.getHeight() / 2f;

        float r2 = dx * dx + dy * dy;
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false;
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        float angle = (float) Math.toDegrees(Math.atan2(Math.abs(dx), -dy));
        return angle < arcSweepAngle / 2;
    }

    static int getSignForClockwise(
            @NonNull View view, @NonNull ArcDirection arcDirection, int defaultValue) {
        switch (arcDirection) {
            case ARC_DIRECTION_CLOCKWISE:
                return 1;
            case ARC_DIRECTION_COUNTER_CLOCKWISE:
                return -1;
            case ARC_DIRECTION_NORMAL:
                return isRtlLayoutDirectionFromLocale() ? -1 : 1;
            case UNRECOGNIZED:
                return view.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR ? 1 : -1;
        }
        return defaultValue;
    }

    private ArcWidgetHelper() {}
}
