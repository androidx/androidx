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

package androidx.app.slice.widget;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_SOURCE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.widget.ImageView;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(23)
public class ShortcutView extends SliceChildView {

    private static final String TAG = "ShortcutView";

    private Slice mSlice;
    private Uri mUri;
    private SliceItem mActionItem;
    private SliceItem mLabel;
    private SliceItem mIcon;

    private int mLargeIconSize;
    private int mSmallIconSize;

    private SliceView.SliceObserver mObserver;

    public ShortcutView(Context context) {
        super(context);
        final Resources res = getResources();
        mSmallIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mLargeIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
    }

    @Override
    public void setSlice(Slice slice) {
        resetView();
        mSlice = slice;
        determineShortcutItems(getContext(), slice);
        SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        if (colorItem == null) {
            colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        }
        final int color = colorItem != null
                ? colorItem.getInt()
                : SliceViewUtil.getColorAccent(getContext());
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.setTint(color);
        ImageView iv = new ImageView(getContext());
        iv.setBackground(circle);
        addView(iv);
        if (mIcon != null) {
            final boolean isLarge = mIcon.hasHint(HINT_LARGE)
                    || SUBTYPE_SOURCE.equals(mIcon.getSubType());
            final int iconSize = isLarge ? mLargeIconSize : mSmallIconSize;
            SliceViewUtil.createCircledIcon(getContext(), iconSize, mIcon.getIcon(),
                    isLarge, this /* parent */);
            mUri = slice.getUri();
            setClickable(true);
        } else {
            setClickable(false);
        }
    }

    @Override
    public @SliceView.SliceMode int getMode() {
        return SliceView.MODE_SHORTCUT;
    }

    @Override
    public boolean performClick() {
        if (!callOnClick()) {
            try {
                if (mActionItem != null) {
                    mActionItem.getAction().send();
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW).setData(mUri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                }
                if (mObserver != null) {
                    EventInfo ei = new EventInfo(SliceView.MODE_SHORTCUT,
                            EventInfo.ACTION_TYPE_BUTTON,
                            EventInfo.ROW_TYPE_SHORTCUT, 0 /* rowIndex */);
                    SliceItem interactedItem = mActionItem != null
                            ? mActionItem
                            : new SliceItem(mSlice, FORMAT_SLICE, null /* subtype */,
                                    mSlice.getHints());
                    mObserver.onSliceAction(ei, interactedItem);
                }
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Looks at the slice and determines which items are best to use to compose the shortcut.
     */
    private void determineShortcutItems(Context context, Slice slice) {
        SliceItem titleItem = SliceQuery.find(slice, FORMAT_ACTION,
                HINT_TITLE, null);

        if (titleItem != null) {
            // Preferred case: hinted action containing hinted image and text
            mActionItem = titleItem;
            mIcon = SliceQuery.find(titleItem.getSlice(), FORMAT_IMAGE, HINT_TITLE,
                    null);
            mLabel = SliceQuery.find(titleItem.getSlice(), FORMAT_TEXT, HINT_TITLE,
                    null);
        } else {
            // No hinted action; just use the first one
            mActionItem = SliceQuery.find(slice, FORMAT_ACTION, (String) null, null);
        }
        // First fallback: any hinted image and text
        if (mIcon == null) {
            mIcon = SliceQuery.find(slice, FORMAT_IMAGE, HINT_TITLE,
                    null);
        }
        if (mLabel == null) {
            mLabel = SliceQuery.find(slice, FORMAT_TEXT, HINT_TITLE,
                    null);
        }
        // Second fallback: first image and text
        if (mIcon == null) {
            mIcon = SliceQuery.find(slice, FORMAT_IMAGE, (String) null,
                    null);
        }
        if (mLabel == null) {
            mLabel = SliceQuery.find(slice, FORMAT_TEXT, (String) null,
                    null);
        }
        // Final fallback: use app info
        if (mIcon == null || mLabel == null || mActionItem == null) {
            PackageManager pm = context.getPackageManager();
            ProviderInfo providerInfo = pm.resolveContentProvider(
                    slice.getUri().getAuthority(), 0);
            ApplicationInfo appInfo = providerInfo.applicationInfo;
            if (appInfo != null) {
                if (mIcon == null) {
                    Slice.Builder sb = new Slice.Builder(slice.getUri());
                    Drawable icon = pm.getApplicationIcon(appInfo);
                    sb.addIcon(SliceViewUtil.createIconFromDrawable(icon), HINT_LARGE);
                    mIcon = sb.build().getItems().get(0);
                }
                if (mLabel == null) {
                    Slice.Builder sb = new Slice.Builder(slice.getUri());
                    sb.addText(pm.getApplicationLabel(appInfo), null);
                    mLabel = sb.build().getItems().get(0);
                }
                if (mActionItem == null) {
                    mActionItem = new SliceItem(PendingIntent.getActivity(context, 0,
                            pm.getLaunchIntentForPackage(appInfo.packageName), 0),
                            new Slice.Builder(slice.getUri()).build(), FORMAT_SLICE,
                            null /* subtype */, null);
                }
            }
        }
    }

    @Override
    public void resetView() {
        mSlice = null;
        mUri = null;
        mActionItem = null;
        mLabel = null;
        mIcon = null;
        setBackground(null);
        removeAllViews();
    }
}
