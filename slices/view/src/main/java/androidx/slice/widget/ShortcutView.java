/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.widget;

import static androidx.slice.core.SliceHints.ICON_IMAGE;

import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.view.R;

import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class ShortcutView extends SliceChildView {

    private static final String TAG = "ShortcutView";

    private ListContent mListContent;
    private SliceItem mActionItem;
    private IconCompat mIcon;
    private Set<SliceItem> mLoadingActions;

    private int mLargeIconSize;
    private int mSmallIconSize;

    public ShortcutView(Context context) {
        super(context);
        final Resources res = getResources();
        mSmallIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mLargeIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
    }

    @Override
    public void setSliceContent(ListContent sliceContent) {
        resetView();
        mListContent = sliceContent;
        if (mListContent == null) {
            return;
        }
        SliceActionImpl shortcutAction = (SliceActionImpl) mListContent.getShortcut(getContext());
        mActionItem = shortcutAction.getActionItem();
        mIcon = shortcutAction.getIcon();
        boolean tintable = shortcutAction.getImageMode() == ICON_IMAGE;
        int color = mListContent.getAccentColor();
        final int accentColor = color != -1 ? color : SliceViewUtil.getColorAccent(getContext());
        Drawable circle = DrawableCompat.wrap(new ShapeDrawable(new OvalShape()));
        DrawableCompat.setTint(circle, accentColor);
        ImageView iv = new ImageView(getContext());
        if (mIcon != null && tintable) {
            // Only set the background if we're tintable
            iv.setBackground(circle);
        }
        addView(iv);
        if (mIcon != null) {
            final int iconSize = tintable ? mSmallIconSize : mLargeIconSize;
            SliceViewUtil.createCircledIcon(getContext(), iconSize, mIcon,
                    !tintable, this /* parent */);
            setClickable(true);
        } else {
            setClickable(false);
        }

        // Set the parent layout gravity to center in order to align icons.
        LayoutParams lp = (LayoutParams) iv.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        setLayoutParams(lp);
    }

    @Override
    public @SliceView.SliceMode int getMode() {
        return SliceView.MODE_SHORTCUT;
    }

    @Override
    public boolean performClick() {
        if (mListContent == null) {
            return false;
        }
        if (!callOnClick()) {
            try {
                if (mActionItem != null) {
                    mActionItem.fireAction(null, null);
                    if (mObserver != null) {
                        EventInfo ei = new EventInfo(SliceView.MODE_SHORTCUT,
                                EventInfo.ACTION_TYPE_BUTTON,
                                EventInfo.ROW_TYPE_SHORTCUT, 0 /* rowIndex */);
                        SliceItem interactedItem = mActionItem != null
                                ? mActionItem
                                : mListContent.getSliceItem();
                        mObserver.onSliceAction(ei, interactedItem);
                    }
                }
            } catch (CanceledException e) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
        return true;
    }

    @Override
    public void setLoadingActions(Set<SliceItem> actions) {
        mLoadingActions = actions;
    }

    @Override
    public Set<SliceItem> getLoadingActions() {
        return mLoadingActions;
    }

    @Override
    public void resetView() {
        mListContent = null;
        mActionItem = null;
        mIcon = null;
        setBackground(null);
        removeAllViews();
    }
}
