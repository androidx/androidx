/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.internal.view;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.RestrictTo;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

/**
 * Toast-based popup used to emulate a tooltip on platform versions prior to API 26.
 * Unlike the platform version of the tooltip, it is invoked only by a long press, but not
 * mouse hover.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TooltipCompat {

    /**
     * Set a tooltip on a view. The tooltip text will be displayed on long click,
     * in a toast aligned with the view.
     * @param view View to align to
     * @param tooltip Tooltip text
     */
    public static void setTooltip(View view, final CharSequence tooltip) {
        if (TextUtils.isEmpty(tooltip)) {
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
        } else {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showTooltipToast(v, tooltip);
                    return true;
                }
            });
        }
    }

    private static void showTooltipToast(View v, CharSequence tooltip) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);

        final Context context = v.getContext();
        final int width = v.getWidth();
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        int referenceX = screenPos[0] + width / 2;
        if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            referenceX = screenWidth - referenceX; // mirror
        }
        Toast toast = Toast.makeText(context, tooltip, Toast.LENGTH_SHORT);
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            toast.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
                    screenPos[1] + height - displayFrame.top);
        } else {
            // Show along the bottom center
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        }
        toast.show();
    }
}
