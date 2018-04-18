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

package androidx.slice.core;

import android.app.PendingIntent;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.IconCompat;

/**
 * Interface for a slice action, supports tappable icons, custom toggle icons, and default toggles.
 */
public interface SliceAction {

    /**
     * @param description the content description for this action.
     */
    @Nullable
    SliceAction setContentDescription(@NonNull CharSequence description);

    /**
     * @param isChecked whether the state of this action is checked or not; only used for toggle
     *                  actions.
     */
    SliceAction setChecked(boolean isChecked);

    /**
     * Sets the priority of this action, with the lowest priority having the highest ranking.
     */
    SliceAction setPriority(@IntRange(from = 0) int priority);

    /**
     * @return the {@link PendingIntent} associated with this action.
     */
    @NonNull
    PendingIntent getAction();

    /**
     * @return the {@link IconCompat} to display for this action. This can be null when the action
     * represented is a default toggle.
     */
    @Nullable
    IconCompat getIcon();

    /**
     * @return the title for this action.
     */
    @NonNull
    CharSequence getTitle();

    /**
     * @return the content description to use for this action.
     */
    @Nullable
    CharSequence getContentDescription();

    /**
     * @return the priority associated with this action, -1 if unset.
     */
    int getPriority();

    /**
     * @return whether this action represents a toggle (i.e. has a checked and unchecked state).
     */
    boolean isToggle();

    /**
     * @return whether the state of this action is checked or not; only used for toggle actions.
     */
    boolean isChecked();

    /**
     * @return the image mode to use for this action.
     */
    @SliceHints.ImageMode int getImageMode();

    /**
     * @return whether this action is a toggle using the standard switch control.
     */
    boolean isDefaultToggle();
}
