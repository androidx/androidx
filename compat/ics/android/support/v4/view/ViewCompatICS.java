/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.RequiresApi;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

@RequiresApi(14)
class ViewCompatICS {

    public static void setTooltipText(View view, CharSequence tooltipText) {
        if (TextUtils.isEmpty(tooltipText)) {
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
            view.setOnHoverListener(null);
        } else {
            new TooltipHandler(view, tooltipText);
        }
    }

    private static class TooltipHandler implements View.OnLongClickListener, View.OnHoverListener {
        private final View mAnchor;
        private final CharSequence mTooltipText;
        private final Runnable mShowRunnable = new Runnable() {
            @Override
            public void run() {
                show(Toast.LENGTH_LONG);
            }
        };
        private Toast mTooltip;

        TooltipHandler(View anchor, CharSequence tooltipText) {
            mAnchor = anchor;
            mTooltipText = tooltipText;

            mAnchor.setOnLongClickListener(this);
            mAnchor.setOnHoverListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            show(Toast.LENGTH_SHORT);
            return true;
        }

        @Override
        public boolean onHover(View v, MotionEvent event) {
            AccessibilityManager manager = (AccessibilityManager)
                    mAnchor.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager.isEnabled()
                    && AccessibilityManagerCompat.isTouchExplorationEnabled(manager)) {
                return false;
            }
            final int action = event.getAction();
            if (action == MotionEvent.ACTION_HOVER_MOVE) {
                hide();
                mAnchor.getHandler().postDelayed(
                        mShowRunnable, ViewConfiguration.getLongPressTimeout());
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                hide();
            }
            return false;
        }

        private void show(int duration) {
            final Context context = mAnchor.getContext();
            final Resources resources = context.getResources();
            final int screenWidth = resources.getDisplayMetrics().widthPixels;
            final int screenHeight = resources.getDisplayMetrics().heightPixels;

            final Rect displayFrame = new Rect();
            mAnchor.getWindowVisibleDisplayFrame(displayFrame);
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
            mAnchor.getLocationOnScreen(anchorPos);
            int referenceX = anchorPos[0] + mAnchor.getWidth() / 2;
            if (ViewCompat.getLayoutDirection(mAnchor) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                referenceX = screenWidth - referenceX; // mirror
            }
            final int anchorTop = anchorPos[1];
            hide();
            mTooltip = Toast.makeText(context, mTooltipText, duration);
            if (anchorTop < displayFrame.height() * 0.8) {
                // Show along the bottom of the anchor view.
                mTooltip.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
                        anchorTop + mAnchor.getHeight() - displayFrame.top);
            } else {
                // Show along the top of the anchor view.
                mTooltip.setGravity(Gravity.BOTTOM | GravityCompat.END, referenceX,
                        displayFrame.bottom - anchorTop);
            }
            mTooltip.show();
        }

        private void hide() {
            if (mTooltip != null) {
                mTooltip.cancel();
                mTooltip = null;
            }
            mAnchor.getHandler().removeCallbacks(mShowRunnable);
        }
    }
}
