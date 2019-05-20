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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * An item in a {@link RichText} sequence, acting as a union of different graphic elements that can
 * be displayed one after another.
 * <p>
 * A {@link RichTextElement} can contain text and a graphic element as its representation.
 * OEM cluster rendering services must attempt to render the graphic element if present. In case of
 * failure to render the element, the first line of fallback should be {@link #getText()}. If that
 * is also empty, fallback to {@link RichText#getText()} will be used.
 * <p>
 * New graphic element types might be added in the future. If such elements are unknown to the
 * OEM cluster rendering service, the elements will be delivered to the OEM cluster rendering
 * services as just text.
 */
@VersionedParcelize
public class RichTextElement implements VersionedParcelable {
    @ParcelField(1)
    String mText;
    @ParcelField(2)
    ImageReference mImage;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    RichTextElement() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public RichTextElement(@Nullable String text, @Nullable ImageReference image) {
        mText = text;
        mImage = image;
    }

    /**
     * Builder for creating a {@link RichTextElement}
     */
    public static final class Builder {
        private ImageReference mImage;
        private String mText;

        /**
         * Sets an image to be displayed as part of the {@link RichText} sequence. Images in the
         * same {@link RichText} sequence are expected to be rendered with equal height but variable
         * width.
         *
         * @param image image reference to be used to represent this element, or null if only the
         *              textual representation should be used.
         * @return this object for chaining
         */
        @NonNull
        public Builder setImage(@Nullable ImageReference image) {
            // Note: if new graphic element types are added in the future, this API should enforce
            // that no more than one of them is set at each moment.
            mImage = image;
            return this;
        }

        /**
         * Sets the textual representation for this element to be displayed as part of the
         * {@link RichText} sequence.
         *
         * @param text textual representation to use
         * @return this object for chaining
         */
        @NonNull
        public Builder setText(@Nullable String text) {
            mText = text;
            return this;
        }

        /**
         * Builds a {@link RichTextElement} with an optional textual representation, and any other
         * optional representation provided to this builder. If no other graphic element is provided
         * or if such graphic element cannot be rendered by the OEM cluster rendering service, text
         * will be used instead.
         */
        @NonNull
        public RichTextElement build() {
            return new RichTextElement(Common.nonNullOrEmpty(mText), mImage);
        }
    }

    /**
     * Returns the textual representation of this element.
     * <p>
     * If {@link #getImage()} is provided, then this is used as a fallback in the case of render
     * failures.
     */
    @NonNull
    public String getText() {
        return Common.nonNullOrEmpty(mText);
    }

    /**
     * Returns an image representing this element. This representation should be used over
     * the textual representation {@link #getText()} whenever possible.
     * <p>
     * In case of failure to render, initial fallback to {@link #getText()} should be used.
     * Fallback to {@link RichText#getText()} should be used if textual fallback is not provided
     * (empty string).
     */
    @Nullable
    public ImageReference getImage() {
        return mImage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RichTextElement element = (RichTextElement) o;
        return Objects.equals(getText(), element.getText())
                && Objects.equals(getImage(), element.getImage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getText(), getImage());
    }

    @Override
    public String toString() {
        return String.format("{text: '%s', image: %s}", mText, mImage);
    }
}
