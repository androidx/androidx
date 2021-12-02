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

package androidx.slice.widget;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.core.SliceHints.ACTION_WITH_LABEL;
import static androidx.slice.core.SliceHints.ICON_IMAGE;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.view.R;

/**
 * Supports displaying {@link androidx.slice.core.SliceActionImpl}s.
 * @hide
 */
@SuppressWarnings("AppCompatCustomView")
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class SliceActionView extends FrameLayout implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "SliceActionView";

    private static final int HEIGHT_UNBOUND = -1;

    static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    interface SliceActionLoadingListener {
        void onSliceActionLoading(SliceItem actionItem, int position);
    }

    private SliceActionImpl mSliceAction;
    private EventInfo mEventInfo;
    private SliceView.OnSliceActionListener mObserver;
    private SliceActionLoadingListener mLoadingListener;

    private View mActionView;
    private ProgressBar mProgressView;

    private int mIconSize;
    private int mImageSize;
    private int mTextActionPadding;

    public SliceActionView(Context context, SliceStyle style, RowStyle rowStyle) {
        super(context);
        Resources res = getContext().getResources();
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        mTextActionPadding = 0;
        if (rowStyle != null) {
            mIconSize = rowStyle.getIconSize();
            mImageSize = rowStyle.getImageSize();
            mTextActionPadding = rowStyle.getTextActionPadding();
        }
    }

    /**
     * Populates the view with the provided action.
     */
    public void setAction(@NonNull SliceActionImpl action, EventInfo info,
            SliceView.OnSliceActionListener listener, int color,
            SliceActionLoadingListener loadingListener) {
        if (mActionView != null) {
            removeView(mActionView);
            mActionView = null;
        }
        if (mProgressView != null) {
            removeView(mProgressView);
            mProgressView = null;
        }
        mSliceAction = action;
        mEventInfo = info;
        mObserver = listener;
        mActionView = null;
        mLoadingListener = loadingListener;

        if (action.isDefaultToggle()) {
            Switch switchView = (Switch) LayoutInflater.from(getContext()).inflate(
                    R.layout.abc_slice_switch, this, false);
            switchView.setChecked(action.isChecked());
            switchView.setOnCheckedChangeListener(this);
            switchView.setMinimumHeight(mImageSize);
            switchView.setMinimumWidth(mImageSize);
            addView(switchView);
            if (color != -1) {
                // See frameworks/base/core/res/res/color/switch_track_material.xml.
                final int uncheckedTrackColor = SliceViewUtil.getColorAttr(getContext(),
                        android.R.attr.colorForeground);

                ColorStateList trackTintList = new ColorStateList(
                        new int[][]{ CHECKED_STATE_SET, EMPTY_STATE_SET },
                        new int[]{ color, uncheckedTrackColor });

                Drawable trackDrawable = DrawableCompat.wrap(switchView.getTrackDrawable());
                DrawableCompat.setTintList(trackDrawable, trackTintList);
                switchView.setTrackDrawable(trackDrawable);

                // See frameworks/base/core/res/res/drawable/switch_thumb_material_anim.xml.
                int uncheckedThumbColor = SliceViewUtil.getColorAttr(getContext(),
                        androidx.appcompat.R.attr.colorSwitchThumbNormal);
                if (uncheckedThumbColor == 0) {
                    // We aren't in an appcompat theme, pull the default light switch color.
                    uncheckedThumbColor = ContextCompat.getColor(getContext(),
                            androidx.appcompat.R.color.switch_thumb_normal_material_light);
                }

                ColorStateList thumbTintList = new ColorStateList(
                        new int[][]{ CHECKED_STATE_SET, EMPTY_STATE_SET },
                        new int[]{ color, uncheckedThumbColor });

                Drawable thumbDrawable = DrawableCompat.wrap(switchView.getThumbDrawable());
                DrawableCompat.setTintList(thumbDrawable, thumbTintList);
                switchView.setThumbDrawable(thumbDrawable);
            }
            mActionView = switchView;

        } else if (action.getImageMode() == ACTION_WITH_LABEL) {
            Button textButton = new Button(getContext());
            mActionView = textButton;
            ((Button) mActionView).setText(action.getTitle());
            addView(mActionView);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mActionView.getLayoutParams();
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.height = LayoutParams.WRAP_CONTENT;
            mActionView.setLayoutParams(lp);
            int p = mTextActionPadding;

            mActionView.setPadding(p, p, p, p);
            mActionView.setOnClickListener(this);
        } else if (action.getIcon() != null) {
            if (action.isToggle()) {
                ImageToggle imageToggle = new ImageToggle(getContext());
                imageToggle.setChecked(action.isChecked());
                mActionView = imageToggle;
            } else {
                mActionView = new ImageView(getContext());
            }
            addView(mActionView);

            Drawable d = mSliceAction.getIcon().loadDrawable(getContext());
            ((ImageView) mActionView).setImageDrawable(d);
            if (color != -1 && mSliceAction.getImageMode() == ICON_IMAGE && d != null) {
                // TODO - Consider allowing option for untinted custom toggles
                DrawableCompat.setTint(d, color);
            }
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mActionView.getLayoutParams();
            lp.width = mImageSize;
            lp.height = mImageSize;
            mActionView.setLayoutParams(lp);
            int p = 0;
            if (action.getImageMode() == ICON_IMAGE) {
                p = mImageSize == HEIGHT_UNBOUND
                    ? mIconSize / 2 : (mImageSize - mIconSize) / 2;
            }
            mActionView.setPadding(p, p, p, p);
            int touchFeedbackAttr = android.R.attr.selectableItemBackground;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                touchFeedbackAttr = android.R.attr.selectableItemBackgroundBorderless;
            }
            mActionView.setBackground(SliceViewUtil.getDrawable(getContext(), touchFeedbackAttr));
            mActionView.setOnClickListener(this);
        }

        if (mActionView != null) {
            CharSequence contentDescription = action.getContentDescription() != null
                    ? action.getContentDescription()
                    : action.getTitle();
            mActionView.setContentDescription(contentDescription);
        }
    }

    /**
     * Indicates whether this action should show loading or not.
     */
    public void setLoading(boolean isLoading) {
        if (isLoading) {
            if (mProgressView == null) {
                mProgressView = (ProgressBar) LayoutInflater.from(getContext()).inflate(
                        R.layout.abc_slice_progress_view, this, false);
                addView(mProgressView);
            }
            SliceViewUtil.tintIndeterminateProgressBar(getContext(), mProgressView);
        }
        mActionView.setVisibility(isLoading ? GONE : VISIBLE);
        mProgressView.setVisibility(isLoading ? VISIBLE : GONE);
    }

    /**
     * Toggles this action if it is toggleable.
     */
    public void toggle() {
        if (mActionView != null && mSliceAction != null && mSliceAction.isToggle()) {
            ((Checkable) mActionView).toggle();
        }
    }

    /**
     * @return the action represented in this view.
     */
    @Nullable
    public SliceActionImpl getAction() {
        return mSliceAction;
    }

    @Override
    public void onClick(@NonNull View v) {
        if (mSliceAction == null || mActionView == null) {
            return;
        }
        sendActionInternal();
    }

    @Override
    public void onCheckedChanged(@Nullable CompoundButton buttonView, boolean isChecked) {
        if (mSliceAction == null || mActionView == null) {
            return;
        }
        sendActionInternal();
    }

    /**
     * Triggers the action associated with this action view; if it is a toggle it will update
     * the toggle state.
     */
    public void sendAction() {
        if (mSliceAction == null) {
            return;
        }
        if (mSliceAction.isToggle()) {
            toggle();
        } else {
            sendActionInternal();
        }
    }

    private void sendActionInternal() {
        if (mSliceAction == null || mSliceAction.getActionItem() == null) {
            return;
        }
        try {
            Intent i = null;
            if (mSliceAction.isToggle()) {
                // Update the intent extra state
                boolean isChecked = ((Checkable) mActionView).isChecked();
                i = new Intent()
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(EXTRA_TOGGLE_STATE, isChecked);
                // Update event info state
                if (mEventInfo != null) {
                    mEventInfo.state = isChecked ? EventInfo.STATE_ON : EventInfo.STATE_OFF;
                }
            }
            SliceItem actionItem = mSliceAction.getActionItem();
            boolean isLoading = actionItem.fireActionInternal(getContext(), i);
            if (isLoading) {
                setLoading(true);
                if (mLoadingListener != null) {
                    int position = mEventInfo != null ? mEventInfo.rowIndex : -1;
                    mLoadingListener.onSliceActionLoading(mSliceAction.getSliceItem(), position);
                }
            }
            if (mObserver != null && mEventInfo != null) {
                mObserver.onSliceAction(mEventInfo, mSliceAction.getSliceItem());
            }
        } catch (PendingIntent.CanceledException e) {
            if (mActionView instanceof Checkable) {
                mActionView.setSelected(!((Checkable) mActionView).isChecked());
            }
            Log.e(TAG, "PendingIntent for slice cannot be sent", e);
        }
    }

    /**
     * Simple class allowing a toggleable image button.
     */
    private static class ImageToggle extends ImageView implements Checkable, View.OnClickListener {
        private boolean mIsChecked;
        private View.OnClickListener mListener;

        ImageToggle(Context context) {
            super(context);
            super.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            toggle();
        }

        @Override
        public void toggle() {
            setChecked(!isChecked());
        }

        @Override
        public void setChecked(boolean checked) {
            if (mIsChecked != checked) {
                mIsChecked = checked;
                refreshDrawableState();
                if (mListener != null) {
                    mListener.onClick(this);
                }
            }
        }

        @Override
        public void setOnClickListener(View.OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public boolean isChecked() {
            return mIsChecked;
        }

        @Override
        public int[] onCreateDrawableState(int extraSpace) {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
            if (mIsChecked) {
                mergeDrawableStates(drawableState, CHECKED_STATE_SET);
            }
            return drawableState;
        }
    }
}
