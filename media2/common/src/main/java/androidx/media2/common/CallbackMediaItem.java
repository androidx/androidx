/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Structure for media item descriptor for {@link DataSourceCallback}.
 * <p>
 * Users should use {@link Builder} to create {@link CallbackMediaItem}.
 * <p>
 * You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem} for detail.
 *
 * @see MediaItem
 */
@VersionedParcelize(isCustom = true)
public class CallbackMediaItem extends MediaItem {
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    DataSourceCallback mDataSourceCallback;

    /**
     * Used for VersionedParcelable
     */
    CallbackMediaItem() {
        // no-op
    }

    CallbackMediaItem(Builder builder) {
        super(builder);
        mDataSourceCallback = builder.mDataSourceCallback;
    }

    /**
     * Return the DataSourceCallback that implements the callback for the data source of this media
     * item.
     *
     * @return the DataSourceCallback that implements the callback for the data source of this
     *         media item,
     */
    public @NonNull DataSourceCallback getDataSourceCallback() {
        return mDataSourceCallback;
    }

    /**
     * This Builder class simplifies the creation of a {@link CallbackMediaItem} object.
     */
    public static final class Builder extends MediaItem.Builder {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
                DataSourceCallback mDataSourceCallback;

        /**
         * Creates a new Builder object.
         * @param dsc2 the DataSourceCallback for the media you want to play
         */
        public Builder(@NonNull DataSourceCallback dsc2) {
            Preconditions.checkNotNull(dsc2);
            mDataSourceCallback = dsc2;
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setMetadata(@Nullable MediaMetadata metadata) {
            return (Builder) super.setMetadata(metadata);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setStartPosition(long position) {
            return (Builder) super.setStartPosition(position);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setEndPosition(long position) {
            return (Builder) super.setEndPosition(position);
        }

        /**
         * @return A new CallbackMediaItem with values supplied by the Builder.
         */
        @Override
        @NonNull
        public CallbackMediaItem build() {
            return new CallbackMediaItem(this);
        }
    }
}
