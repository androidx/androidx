/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities.systemrouting;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A model that holds data about a system routes source.
 */
public final class SystemRoutesSourceItem implements SystemRoutesAdapterItem {
    public static final int ROUTE_SOURCE_MEDIA_ROUTER = 0;
    public static final int ROUTE_SOURCE_MEDIA_ROUTER2 = 1;
    public static final int ROUTE_SOURCE_ANDROIDX_ROUTER = 2;
    public static final int ROUTE_SOURCE_BLUETOOTH_MANAGER = 3;
    public static final int ROUTE_SOURCE_AUDIO_MANAGER = 4;

    @IntDef({
            ROUTE_SOURCE_MEDIA_ROUTER,
            ROUTE_SOURCE_MEDIA_ROUTER2,
            ROUTE_SOURCE_ANDROIDX_ROUTER,
            ROUTE_SOURCE_BLUETOOTH_MANAGER,
            ROUTE_SOURCE_AUDIO_MANAGER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @Type
    private final int mType;

    private SystemRoutesSourceItem(@NonNull Builder builder) {
        mType = builder.mType;
    }

    /**
     * Returns a route source item type.
     * see {@link SystemRoutesSourceItem.Type}
     */
    public int getType() {
        return mType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemRoutesSourceItem that = (SystemRoutesSourceItem) o;
        return mType == that.mType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType);
    }

    /**
     * Helps to construct {@link SystemRoutesSourceItem}.
     */
    public static final class Builder {

        @Type
        private final int mType;

        public Builder(@Type int type) {
            mType = type;
        }

        /**
         * Builds {@link SystemRoutesSourceItem}.
         */
        @NonNull
        public SystemRoutesSourceItem build() {
            return new SystemRoutesSourceItem(this);
        }
    }
}
