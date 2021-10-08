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

package androidx.slice.builders;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.util.Pair;
import androidx.remotecallback.RemoteCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Builder to construct a selection which can be added to a {@link ListBuilder}.
 *
 * A selection presents a list of options to the user and allows the user to select exactly one
 * option.
 */
@RequiresApi(19)
public class SelectionBuilder {
    private final List<Pair<String, CharSequence>> mOptions;
    private final Set<String> mOptionKeys;
    private String mSelectedOption;
    private SliceAction mPrimaryAction;
    private PendingIntent mInputAction;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private CharSequence mContentDescription;
    private int mLayoutDirection;

    /**
     * Creates a SelectionBuilder with no options.
     */
    public SelectionBuilder() {
        mOptions = new ArrayList<>();
        mOptionKeys = new ArraySet<>();
        mSelectedOption = null;
        mLayoutDirection = -1;
    }

    /**
     * Adds an option to this SelectionBuilder.
     *
     * The new option will be appended to the list of options.
     *
     * @param optionKey the key that will be returned if the user selects this option
     * @param optionText the text that will be displayed to the user for this option
     * @return this SelectionBuilder
     */
    public SelectionBuilder addOption(String optionKey, CharSequence optionText) {
        if (mOptionKeys.contains(optionKey)) {
            throw new IllegalArgumentException("optionKey " + optionKey + " is a duplicate");
        }

        mOptions.add(new Pair<>(optionKey, optionText));
        mOptionKeys.add(optionKey);
        return this;
    }

    /**
     * Sets the primary action for the selection slice.
     *
     * The action specified here will be sent when the whole slice is clicked, or when the app
     * presenting the slice is not capable of rendering a selection interface.
     *
     * @param primaryAction the action to trigger when the user clicks the whole slice
     * @return this SelectionBuilder
     */
    public SelectionBuilder setPrimaryAction(@NonNull SliceAction primaryAction) {
        mPrimaryAction = primaryAction;
        return this;
    }

    /**
     * Sets the {@link PendingIntent} to send when the selection is made or changed.
     *
     * The intent will include an extra with the key {@link androidx.slice.Slice#EXTRA_SELECTION}
     * and a {@link String} value containing the key of the key of the selected option.
     *
     * @param inputAction the intent to send when the user makes or changes the selection
     * @return this SelectionBuilder
     */
    public SelectionBuilder setInputAction(@NonNull PendingIntent inputAction) {
        mInputAction = inputAction;
        return this;
    }

    /**
     * Sets the {@link RemoteCallback} to send when the selection is made or changed.
     *
     * The intent will include an extra with the key {@link androidx.slice.Slice#EXTRA_SELECTION}
     * and a {@link String} value containing the key of the key of the selected option.
     *
     * @param inputAction the intent to send when the user makes or changes the selection
     * @return this SelectionBuilder
     */
    public SelectionBuilder setInputAction(@NonNull RemoteCallback inputAction) {
        mInputAction = inputAction.toPendingIntent();
        return this;
    }

    /**
     * Sets which option is selected by default.
     *
     * @param selectedOption the key of the selected option
     * @return this SelectionBuilder
     */
    public SelectionBuilder setSelectedOption(String selectedOption) {
        mSelectedOption = selectedOption;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the title
     * @return this SelectionBuilder
     */
    public SelectionBuilder setTitle(@Nullable CharSequence title) {
        mTitle = title;
        return this;
    }

    /**
     * Sets the subtitle.
     *
     * @param subtitle the subtitle
     * @return this SelectionBuilder
     */
    public SelectionBuilder setSubtitle(@Nullable CharSequence subtitle) {
        mSubtitle = subtitle;
        return this;
    }

    /**
     * Sets the content description.
     *
     * @param contentDescription the content description
     * @return this SelectionBuilder
     */
    public SelectionBuilder setContentDescription(@Nullable CharSequence contentDescription) {
        mContentDescription = contentDescription;
        return this;
    }

    /**
     * Sets the layout direction.
     *
     * @param layoutDirection the layout direction
     * @return this SelectionBuilder
     */
    public SelectionBuilder setLayoutDirection(
            @androidx.slice.builders.ListBuilder.LayoutDirection int layoutDirection) {
        mLayoutDirection = layoutDirection;
        return this;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public List<Pair<String, CharSequence>> getOptions() {
        return mOptions;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public SliceAction getPrimaryAction() {
        return mPrimaryAction;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public PendingIntent getInputAction() {
        return mInputAction;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public String getSelectedOption() {
        return mSelectedOption;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void check() {
        if (getPrimaryAction() == null) {
            throw new IllegalArgumentException("primaryAction must be set");
        }
        if (getInputAction() == null) {
            throw new IllegalArgumentException("inputAction must be set");
        }
        if (mSelectedOption != null && !mOptionKeys.contains(mSelectedOption)) {
            throw new IllegalArgumentException("selectedOption must be present in options");
        }
    }
}
