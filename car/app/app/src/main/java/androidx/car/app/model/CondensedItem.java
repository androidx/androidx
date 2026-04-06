/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A temporary stub for CondensedItem.
 */
@CarProtocol
@KeepFields
@RequiresCarApi(8)
@ExperimentalCarApi
public final class CondensedItem implements Item {
    private final @Nullable CarText mTitle;
    private final @Nullable CarText mText;
    private final @Nullable CarIcon mLeadingImage;

    private CondensedItem() {
        mTitle = null;
        mText = null;
        mLeadingImage = null;
    }

    private CondensedItem(Builder builder) {
        mTitle = builder.mTitle;
        mText = builder.mText;
        mLeadingImage = builder.mLeadingImage;
    }

    /** Returns the title of the item. */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /** Returns the text of the item. */
    public @Nullable CarText getText() {
        return mText;
    }

    /** Returns the leading image of the item. */
    public @Nullable CarIcon getLeadingImage() {
        return mLeadingImage;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CondensedItem)) {
            return false;
        }
        CondensedItem that = (CondensedItem) other;
        return Objects.equals(mTitle, that.mTitle)
                && Objects.equals(mText, that.mText)
                && Objects.equals(mLeadingImage, that.mLeadingImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mText, mLeadingImage);
    }

    @Override
    public @NonNull String toString() {
        return "CondensedItem { title: " + mTitle + ", text: " + mText + ", leadingImage: "
                + mLeadingImage + " }";
    }

    /** A builder for {@link CondensedItem}. */
    public static final class Builder {
        @Nullable CarText mTitle;
        @Nullable CarText mText;
        @Nullable CarIcon mLeadingImage;

        /** Creates a new builder. */
        public Builder() {
        }

        /** Sets the title of the item. */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(title);
            return this;
        }

        /** Sets the text of the item. */
        public @NonNull Builder setText(@NonNull CharSequence text) {
            mText = CarText.create(text);
            return this;
        }

        /** Sets the leading image of the item. */
        public @NonNull Builder setLeadingImage(@NonNull CarIcon leadingImage) {
            mLeadingImage = leadingImage;
            return this;
        }

        /** Builds the {@link CondensedItem}. */
        public @NonNull CondensedItem build() {
            return new CondensedItem(this);
        }
    }
}
