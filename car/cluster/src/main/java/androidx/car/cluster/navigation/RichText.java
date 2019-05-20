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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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
 * A {@link RichText} is an immutable sequence of graphic elements (e.g.: text, images)
 * to be displayed one after another.
 * <p>
 * Elements in this sequence are represented by {@link RichTextElement} instances.
 * <p>
 * Each sequence will have a textual representation provided by {@link #getText()} and in the case
 * of the absence of a rich representation, the sequence of elements {@link #getElements()} may be
 * left empty. The textual representation may also be used as a fallback for when
 * {@link RichTextElement}s fail to render.
 * <p>
 * Spaces and other separators should be provided by the third-party navigation apps, while OEM
 * rendering services shouldn't add additional ones, in order to avoid duplications.
 */
@VersionedParcelize
public class RichText implements VersionedParcelable {
    @ParcelField(1)
    List<RichTextElement> mElements;

    @ParcelField(2)
    String mText;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    RichText() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    RichText(@NonNull String text, @NonNull List<RichTextElement> elements) {
        mText = text;
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
        public RichText build(@NonNull String text) {
            return new RichText(Preconditions.checkNotNull(text), mElements);
        }
    }

    /**
     * Returns the plaintext string of this {@link RichText}.
     */
    @NonNull
    public String getText() {
        return Common.nonNullOrEmpty(mText);
    }

    /**
     * Returns the sequence of graphic elements.
     * <p>
     * If no rich representation is available, the list may be empty and {@link #getText()} should
     * be used as a fallback.
     */
    @NonNull
    public List<RichTextElement> getElements() {
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
        return Objects.equals(getText(), richText.getText())
                && Objects.equals(getElements(), richText.getElements());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getText(), getElements());
    }

    @Override
    public String toString() {
        return String.format("{text: '%s', elements: %s}", mText, mElements);
    }
}
