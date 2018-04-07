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
import static androidx.slice.core.SliceHints.ICON_IMAGE;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.view.R;

/**
 * Supports displaying {@link androidx.slice.core.SliceActionImpl}s.
 * @hide
 */
@RestrictTo(LIBRARY)
public class SliceActionView extends FrameLayout implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "SliceActionView";

    private static final int[] STATE_CHECKED = {
            android.R.attr.state_checked
    };

    private SliceActionImpl mSliceAction;
    private EventInfo mEventInfo;
    private SliceView.OnSliceActionListener mObserver;

    private View mActionView;

    private int mIconSize;
    private int mImageSize;

    public SliceActionView(Context context) {
        super(context);
        Resources res = getContext().getResources();
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
    }

    /**
     * Populates the view with the provided action.
     */
    public void setAction(@NonNull SliceActionImpl action, EventInfo info,
                          SliceView.OnSliceActionListener listener, int color) {
        mSliceAction = action;
        mEventInfo = info;
        mObserver = listener;
        mActionView = null;

        if (action.isDefaultToggle()) {
            Switch switchView = new Switch(getContext());
            addView(switchView);
            switchView.setChecked(action.isChecked());
            switchView.setOnCheckedChangeListener(this);
            switchView.setMinimumHeight(mImageSize);
            switchView.setMinimumWidth(mImageSize);
            if (color != -1) {
                // TODO - find nice way to tint toggles
            }
            mActionView = switchView;

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
            if (color != -1 && mSliceAction.getImageMode() == ICON_IMAGE) {
                // TODO - Consider allowing option for untinted custom toggles
                DrawableCompat.setTint(d, color);
            }
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mActionView.getLayoutParams();
            lp.width = mImageSize;
            lp.height = mImageSize;
            mActionView.setLayoutParams(lp);
            int p = action.getImageMode() == ICON_IMAGE ? mIconSize / 2 : 0;
            mActionView.setPadding(p, p, p, p);
            mActionView.setBackground(SliceViewUtil.getDrawable(getContext(),
                    android.R.attr.selectableItemBackground));
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
    public void onClick(View v) {
        if (mSliceAction == null || mActionView == null) {
            return;
        }
        sendAction();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mSliceAction == null || mActionView == null) {
            return;
        }
        sendAction();
    }

    private void sendAction() {
        // TODO - Show loading indicator here?
        try {
            PendingIntent pi = mSliceAction.getAction();
            if (mSliceAction.isToggle()) {
                // Update the intent extra state
                boolean isChecked = ((Checkable) mActionView).isChecked();
                Intent i = new Intent().putExtra(EXTRA_TOGGLE_STATE, isChecked);
                mSliceAction.getActionItem().fireAction(getContext(), i);

                // Update event info state
                if (mEventInfo != null) {
                    mEventInfo.state = isChecked ? EventInfo.STATE_ON : EventInfo.STATE_OFF;
                }
            } else {
                mSliceAction.getActionItem().fireAction(null, null); 
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
                mergeDrawableStates(drawableState, STATE_CHECKED);
            }
            return drawableState;
        }
    }
}
