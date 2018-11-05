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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * An item in a {@link RichText} sequence, acting as a union of different graphic elements that can
 * be displayed one after another.
 * <p>
 * All {@link RichTextElement} must contain a textual representation of its content, which will be
 * used by consumers incapable of rendering the desired graphic element. A {@link RichTextElement}
 * can only contain one other graphic element. Consumers must attempt to render such element and
 * only fallback to text if needed.
 * <p>
 * New graphic element types might be added in the future. If such elements are unknown to the
 * consumer, they will be delivered to the consumer as just text.
 */
@VersionedParcelize
public class RichTextElement implements VersionedParcelable {
    @ParcelField(1)
    String mText;
    @ParcelField(2)
    ImageReference mImage;

    /**
     * Used by {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    RichTextElement() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public RichTextElement(@NonNull String text, @Nullable ImageReference image) {
        mText = Preconditions.checkNotNull(text, "A textual representation of this "
                + "element must be provided.");
        mImage = image;
    }

    /**
     * Builder for creating a {@link RichTextElement}
     */
    public static class Builder {
        private ImageReference mImage;

        /**
         * Sets an image to be displayed as part of the {@link RichText} sequence. Images in the
         * same {@link RichText} sequence are expected to be rendered with equal height but variable
         * width.
         *
         * @param image image reference to be used to represent this element, or null if only the
         *              textual representation should be used.
         * @return this object for chaining
         */
        public Builder setImage(@Nullable ImageReference image) {
            // Note: if new graphic element types are added in the future, this API should enforce
            // that no more than one of them is set at each moment.
            mImage = image;
            return this;
        }

        /**
         * Builds a {@link RichTextElement} with the given textual representation, and any other
         * optional representation provided to this builder. If no other graphic element is provided
         * or if such graphic element cannot be rendered by the consumer, this text will be used
         * instead.
         *
         * @param text textual representation to use
         */
        public RichTextElement build(@NonNull String text) {
            return new RichTextElement(Preconditions.checkNotNull(text), mImage);
        }
    }

    /**
     * Returns the textual representation of this element
     */
    @NonNull
    String getText() {
        return Common.nonNullOrEmpty(mText);
    }

    /**
     * Returns an image representing this element, or null if the textual representation should be
     * used instead.
     */
    @Nullable
    ImageReference getImage() {
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
