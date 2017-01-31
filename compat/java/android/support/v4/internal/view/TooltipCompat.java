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
import android.content.res.Resources;
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
     * @param tooltipText Tooltip text
     */
    public static void setTooltipText(View view, final CharSequence tooltipText) {
        if (TextUtils.isEmpty(tooltipText)) {
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
        } else {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showTooltipToast(v, tooltipText);
                    return true;
                }
            });
        }
    }

    private static void showTooltipToast(View anchor, CharSequence tooltipText) {
        final Context context = anchor.getContext();
        final Resources resources = context.getResources();
        final int screenWidth = resources.getDisplayMetrics().widthPixels;
        final int screenHeight = resources.getDisplayMetrics().heightPixels;

        final Rect displayFrame = new Rect();
        anchor.getWindowVisibleDisplayFrame(displayFrame);
        if (displayFrame.left < 0 && displayFrame.top < 0) {
            // No meaningful display frame, the anchor view is probably in a subpanel
            // (such as a popup window). Use the screen frame as a reasonable approximation.
            final int statusBarHeight;
            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = resources.getDimensionPixelSize(resourceId);
            } else {
                statusBarHeight = 0;
            }
            displayFrame.set(0, statusBarHeight, screenWidth, screenHeight);
        }

        final int[] anchorPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);
        int referenceX = anchorPos[0] + anchor.getWidth() / 2;
        if (ViewCompat.getLayoutDirection(anchor) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            referenceX = screenWidth - referenceX; // mirror
        }
        final int anchorTop = anchorPos[1];
        Toast toast = Toast.makeText(context, tooltipText, Toast.LENGTH_SHORT);
        if (anchorTop < displayFrame.height() * 0.8) {
            // Show along the bottom of the anchor view.
            toast.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
                    anchorTop + anchor.getHeight() - displayFrame.top);
        } else {
            // Show along the top of the anchor view.
            toast.setGravity(Gravity.BOTTOM | GravityCompat.END, referenceX,
                    displayFrame.bottom - anchorTop);
        }
        toast.show();
    }
}
