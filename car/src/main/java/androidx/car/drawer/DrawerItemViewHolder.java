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

package androidx.car.drawer;

import android.car.drivingstate.CarUxRestrictions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsUtils;

/**
 * Re-usable {@link RecyclerView.ViewHolder} for displaying items in the
 * {@link androidx.car.drawer.CarDrawerAdapter}.
 */
public class DrawerItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mIcon;
    private final TextView mTitle;
    private final TextView mText;
    private final ImageView mEndIcon;

    DrawerItemViewHolder(View view) {
        super(view);
        mIcon = view.findViewById(R.id.icon);
        if (mIcon == null) {
            throw new IllegalArgumentException("Icon view cannot be null!");
        }

        mTitle = view.findViewById(R.id.title);
        if (mTitle == null) {
            throw new IllegalArgumentException("Title view cannot be null!");
        }

        // Next two are optional and may be null.
        mText = view.findViewById(R.id.text);
        mEndIcon = view.findViewById(R.id.end_icon);
    }

    /** Returns the view that should be used to display the main icon. */
    @NonNull
    public ImageView getIcon() {
        return mIcon;
    }

    /** Returns the view that will display the main title. */
    @NonNull
    public TextView getTitle() {
        return mTitle;
    }

    /** Returns the view that is used for text that is smaller than the title text. */
    @Nullable
    public TextView getText() {
        return mText;
    }

    /** Returns the icon that is displayed at the end of the view. */
    @Nullable
    public ImageView getEndIcon() {
        return mEndIcon;
    }

    /**
     * Sets the listener that will be notified when the view held by this ViewHolder has been
     * clicked. Passing {@code null} will clear any previously set listeners.
     */
    void setItemClickListener(@Nullable DrawerItemClickListener listener) {
        itemView.setOnClickListener(listener != null
                ? v -> listener.onItemClick(getAdapterPosition())
                : null);
    }

    /**
     * Update children views to comply with car UX restrictions.
     *
     * <p>{@code Text} might be truncated to meet length limit required by regulation.
     *
     * @param restrictions current car UX restrictions.
     */
    void complyWithUxRestrictions(CarUxRestrictions restrictions) {
        CarUxRestrictionsUtils.comply(itemView.getContext(), restrictions, getText());
    }
}
