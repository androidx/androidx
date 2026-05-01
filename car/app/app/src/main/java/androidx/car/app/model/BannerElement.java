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
@RestrictTo(LIBRARY)
@ExperimentalCarApi
@CarProtocol
@KeepFields
@RequiresCarApi(9)
public final class BannerElement {

    /** Type indicating the element is an {@link Action}. */
    public static final int TYPE_ACTION = 1;

    /**
     * Type indicating the element is a {@link CarIcon} meant to be displayed as an icon (a
     * tintable, padded image).
     */
    public static final int TYPE_ICON = 2;

    /**
     * Type indicating the element is a {@link CarIcon} meant to be displayed as an image (a
     * full-sized image).
     */
    public static final int TYPE_IMAGE = 3;

    private final @ElementType int mType;
    private final @Nullable Action mAction;
    private final @Nullable CarIcon mIcon;

    BannerElement(@ElementType int type, @Nullable Action action, @Nullable CarIcon icon) {
        mType = type;
        mAction = action;
        mIcon = icon;
    }

    /** Constructs an empty instance, used by serialization code. */
    private BannerElement() {
        mType = TYPE_ACTION;
        mAction = null;
        mIcon = null;
    }

    /** Returns the type of the element. */
    public @ElementType int getType() {
        return mType;
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
     * <p>The host should check {@link #getType()} to determine whether this should be rendered as
     * an icon or an image.
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mAction, mIcon);
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
                && Objects.equals(mAction, otherElement.mAction)
                && Objects.equals(mIcon, otherElement.mIcon);
    }

    @Override
    public @NonNull String toString() {
        return "[type: " + mType + ", action: " + mAction + ", icon: " + mIcon + "]";
    }

    @RestrictTo(LIBRARY)
    @IntDef(value = {TYPE_ACTION, TYPE_ICON, TYPE_IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ElementType {
    }
}
