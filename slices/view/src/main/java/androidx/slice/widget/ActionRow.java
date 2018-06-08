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

import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;

import static androidx.slice.core.SliceHints.ICON_IMAGE;

import android.app.PendingIntent.CanceledException;
import android.app.RemoteInput;
import android.app.slice.Slice;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ActionRow extends FrameLayout {

    private static final int MAX_ACTIONS = 5;
    private static final String TAG = "ActionRow";

    private final int mSize;
    private final int mIconPadding;
    private final LinearLayout mActionsGroup;
    private final boolean mFullActions;
    private int mColor = Color.BLACK;

    public ActionRow(Context context, boolean fullActions) {
        super(context);
        mFullActions = fullActions;
        mSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                context.getResources().getDisplayMetrics());
        mIconPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                context.getResources().getDisplayMetrics());
        mActionsGroup = new LinearLayout(context);
        mActionsGroup.setOrientation(LinearLayout.HORIZONTAL);
        mActionsGroup.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mActionsGroup);
    }

    private void setColor(int color) {
        mColor = color;
        for (int i = 0; i < mActionsGroup.getChildCount(); i++) {
            View view = mActionsGroup.getChildAt(i);
            int mode = (Integer) view.getTag();
            boolean tint = mode == ICON_IMAGE;
            if (tint) {
                ImageViewCompat.setImageTintList((ImageView) view, ColorStateList.valueOf(mColor));
            }
        }
    }

    private ImageView addAction(IconCompat icon, boolean allowTint) {
        ImageView imageView = new ImageView(getContext());
        imageView.setPadding(mIconPadding, mIconPadding, mIconPadding, mIconPadding);
        imageView.setScaleType(ScaleType.FIT_CENTER);
        imageView.setImageDrawable(icon.loadDrawable(getContext()));
        if (allowTint) {
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(mColor));
        }
        imageView.setBackground(SliceViewUtil.getDrawable(getContext(),
                android.R.attr.selectableItemBackground));
        imageView.setTag(allowTint);
        addAction(imageView);
        return imageView;
    }

    /**
     * Set the actions and color for this action row.
     */
    public void setActions(@NonNull List<SliceItem> actions, int color) {
        removeAllViews();
        mActionsGroup.removeAllViews();
        addView(mActionsGroup);
        if (color != -1) {
            setColor(color);
        }
        for (final SliceItem action : actions) {
            if (mActionsGroup.getChildCount() >= MAX_ACTIONS) {
                return;
            }
            final SliceItem input = SliceQuery.find(action, FORMAT_REMOTE_INPUT);
            final SliceItem image = SliceQuery.find(action, FORMAT_IMAGE);
            if (input != null && image != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    handleSetRemoteInputActions(input, image, action);
                } else {
                    Log.w(TAG, "Received RemoteInput on API <20 " + input);
                }
            } else if (action.hasHint(Slice.HINT_SHORTCUT)) {
                final SliceActionImpl ac = new SliceActionImpl(action);
                IconCompat iconItem = ac.getIcon();
                if (iconItem != null && ac.getActionItem() != null) {
                    boolean tint = ac.getImageMode() == ICON_IMAGE;
                    addAction(iconItem, tint).setOnClickListener(
                            new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        // TODO - should log events here
                                        ac.getActionItem().fireAction(null, null);
                                    } catch (CanceledException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                }
            }
        }
        setVisibility(getChildCount() != 0 ? View.VISIBLE : View.GONE);
    }

    private void addAction(View child) {
        mActionsGroup.addView(child, new LinearLayout.LayoutParams(mSize, mSize, 1));
    }

    @RequiresApi(21)
    private void handleSetRemoteInputActions(final SliceItem input, SliceItem image,
            final SliceItem action) {
        if (input.getRemoteInput().getAllowFreeFormInput()) {
            boolean tint = !image.hasHint(HINT_NO_TINT);
            addAction(image.getIcon(), tint).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleRemoteInputClick(v, action,
                                    input.getRemoteInput());
                        }
                    });
            createRemoteInputView(mColor, getContext());
        }
    }

    @RequiresApi(21)
    private void createRemoteInputView(int color, Context context) {
        View riv = RemoteInputView.inflate(context, this);
        riv.setVisibility(View.INVISIBLE);
        addView(riv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        riv.setBackgroundColor(color);
    }

    @RequiresApi(21)
    private boolean handleRemoteInputClick(View view, SliceItem action,
            RemoteInput input) {
        if (input == null) {
            return false;
        }

        ViewParent p = view.getParent().getParent();
        RemoteInputView riv = null;
        while (p != null) {
            if (p instanceof View) {
                View pv = (View) p;
                riv = findRemoteInputView(pv);
                if (riv != null) {
                    break;
                }
            }
            p = p.getParent();
        }
        if (riv == null) {
            return false;
        }

        int width = view.getWidth();
        if (view instanceof TextView) {
            // Center the reveal on the text which might be off-center from the TextView
            TextView tv = (TextView) view;
            if (tv.getLayout() != null) {
                int innerWidth = (int) tv.getLayout().getLineWidth(0);
                innerWidth += tv.getCompoundPaddingLeft() + tv.getCompoundPaddingRight();
                width = Math.min(width, innerWidth);
            }
        }
        int cx = view.getLeft() + width / 2;
        int cy = view.getTop() + view.getHeight() / 2;
        int w = riv.getWidth();
        int h = riv.getHeight();
        int r = Math.max(
                Math.max(cx + cy, cx + (h - cy)),
                Math.max((w - cx) + cy, (w - cx) + (h - cy)));

        riv.setRevealParameters(cx, cy, r);
        riv.setAction(action);
        riv.setRemoteInput(new RemoteInput[] {
                input
        }, input);
        riv.focusAnimated();
        return true;
    }

    @RequiresApi(21)
    private RemoteInputView findRemoteInputView(View v) {
        if (v == null) {
            return null;
        }
        return (RemoteInputView) v.findViewWithTag(RemoteInputView.VIEW_TAG);
    }
}
