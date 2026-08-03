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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * An internal wrapper class for elements (actions, icons, images) that can be displayed
 * as leading or trailing items in a {@link Banner}.
 */
@ExperimentalCarApi
@CarProtocol
@KeepFields
@RequiresCarApi(9)
public final class BannerElement {

    /** Type indicating the element is an {@link Action}. */
    public static final int TYPE_ACTION = 1;

    /** Type indicating the element is an image/icon. */
    public static final int TYPE_IMAGE = 2;

    private final @ElementType int mType;
    private final @Banner.BannerImageType int mImageType;
    private final @Nullable Action mAction;
    private final @Nullable CarIcon mImage;

    /** Creates a {@link BannerElement} for an {@link Action}. */
    @RestrictTo(LIBRARY)
    public static @NonNull BannerElement createForAction(@NonNull Action action) {
        return new BannerElement(TYPE_ACTION, Objects.requireNonNull(action), null);
    }

    /** Creates a {@link BannerElement} for a {@link CarIcon} with a specific image type. */
    @RestrictTo(LIBRARY)
    public static @NonNull BannerElement createForImageType(@NonNull CarIcon image,
            @Banner.BannerImageType int imageType) {
        return new BannerElement(TYPE_IMAGE, imageType, null, Objects.requireNonNull(image));
    }

    private BannerElement(@ElementType int type, @Banner.BannerImageType int imageType,
            @Nullable Action action, @Nullable CarIcon image) {
        mType = type;
        mImageType = imageType;
        mAction = action;
        mImage = image;
    }

    private BannerElement(@ElementType int type, @Nullable Action action, @Nullable CarIcon image) {
        mType = type;
        mImageType = Banner.IMAGE_TYPE_SMALL;
        mAction = action;
        mImage = image;
    }

    /** Constructs an empty instance, used by serialization code. */
    private BannerElement() {
        mType = TYPE_ACTION;
        mImageType = Banner.IMAGE_TYPE_SMALL;
        mAction = null;
        mImage = null;
    }

    /** Returns the type of the element. */
    public @ElementType int getType() {
        return mType;
    }

    /** Returns the image type of the element. */
    @Banner.BannerImageType
    public int getImageType() {
        return mImageType;
    }

    /**
     * Returns the {@link Action} in this element, or {@code null} if not set.
     *
     * <p>{@link Action}s with no {@link Action#getOnClickDelegate()} should be treated like icons.
     */
    public @Nullable Action getAction() {
        return mAction;
    }

    /**
     * Returns the {@link CarIcon} in this element, or {@code null} if not set.
     *
     * <p>The host should check {@link #getType()} and {@link #getImageType()} to determine
     * whether this should be rendered as an icon or an image.
     */
    public @Nullable CarIcon getImage() {
        return mImage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mImageType, mAction, mImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BannerElement)) {
            return false;
        }
        BannerElement otherElement = (BannerElement) other;
        return mType == otherElement.mType
                && mImageType == otherElement.mImageType
                && Objects.equals(mAction, otherElement.mAction)
                && Objects.equals(mImage, otherElement.mImage);
    }

    @Override
    public @NonNull String toString() {
        return "BannerElement { type: " + mType
                + ", imageType: " + mImageType
                + ", action: " + mAction
                + ", image: " + mImage + " }";
    }

    @RestrictTo(LIBRARY)
    @IntDef(value = {TYPE_ACTION, TYPE_IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ElementType {
    }
}