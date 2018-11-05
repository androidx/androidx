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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable sequence of graphic elements (e.g.: text, images) to be displayed one after another in
 * the same way as a {@link CharSequence} would. Elements in this sequence are represented by
 * {@link RichTextElement} instances.
 */
@VersionedParcelize
public class RichText implements VersionedParcelable {
    @ParcelField(1)
    List<RichTextElement> mElements;

    /**
     * Used by {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    RichText() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    RichText(@NonNull List<RichTextElement> elements) {
        mElements = new ArrayList<>(elements);
    }

    /**
     * Builder for creating a {@link RichText}
     */
    public static final class Builder {
        private List<RichTextElement> mElements = new ArrayList<>();

        /**
         * Adds a graphic element to the rich text sequence.
         *
         * @param element a graphic element to add to the sequence.
         * @return this object for chaining
         */
        @NonNull
        public Builder addElement(@NonNull RichTextElement element) {
            mElements.add(Preconditions.checkNotNull(element));
            return this;
        }

        /**
         * Returns a {@link RichText} built with the provided information.
         */
        @NonNull
        public RichText build() {
            return new RichText(mElements);
        }
    }

    /**
     * Returns the sequence of graphic elements
     */
    @NonNull
    List<RichTextElement> getElements() {
        return Common.immutableOrEmpty(mElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RichText richText = (RichText) o;
        return Objects.equals(getElements(), richText.getElements());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElements());
    }

    @Override
    public String toString() {
        return String.format("{elements: %s}", mElements);
    }
}
