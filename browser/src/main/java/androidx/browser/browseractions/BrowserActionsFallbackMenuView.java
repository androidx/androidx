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

package androidx.browser.browseractions;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.RestrictTo;
import androidx.browser.R;

/**
 * The class responsible for deciding the size of Browser Actions context menu.
 */
/** @hide */
@RestrictTo(LIBRARY_GROUP)
public class BrowserActionsFallbackMenuView extends LinearLayout {
    private final int mBrowserActionsMenuMinPaddingPx;
    private final int mBrowserActionsMenuMaxWidthPx;

    public BrowserActionsFallbackMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBrowserActionsMenuMinPaddingPx = getResources().getDimensionPixelOffset(
                R.dimen.browser_actions_context_menu_min_padding);
        mBrowserActionsMenuMaxWidthPx = getResources().getDimensionPixelOffset(
                R.dimen.browser_actions_context_menu_max_width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int appWindowWidthPx = getResources().getDisplayMetrics().widthPixels;
        int contextMenuWidth = Math.min(appWindowWidthPx - 2 * mBrowserActionsMenuMinPaddingPx,
                mBrowserActionsMenuMaxWidthPx);
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(contextMenuWidth, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
