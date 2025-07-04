/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.expression.DynamicDataMapUtil.dynamicDataMapOf;
import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.DynamicDataMap;
import androidx.wear.protolayout.expression.DynamicDataPair;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.MutableDynamicDataMap;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.expression.proto.DynamicDataProto;
import androidx.wear.protolayout.proto.StateProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Builders for state of a layout. */
public final class StateBuilders {
    private StateBuilders() {}

    /** {@link State} information. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class State {
        private final StateProto.State mImpl;
        private final @Nullable Fingerprint mFingerprint;

        private static final int MAX_STATE_ENTRY_COUNT = 30;

        State(StateProto.State impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Returns the maximum number for state entries that can be added to the {@link State} using
         * {@link Builder#addKeyToValueMapping(AppDataKey, DynamicDataValue)}.
         *
         * <p>The ProtoLayout state model is not designed to handle large volumes of layout provided
         * state. So we limit the number of state entries to keep the on-the-wire size and state
         * store update times manageable.
         */
        public static int getMaxStateEntryCount() {
            return MAX_STATE_ENTRY_COUNT;
        }

        /** Gets the ID of the clickable that was last clicked. */
        public @NonNull String getLastClickableId() {
            return mImpl.getLastClickableId();
        }

        /**
         * Gets any shared state between the provider and renderer. This method returns the same
         * underlying data as {@link #getKeyToValueMapping()}.
         */
        public @NonNull DynamicDataMap getStateMap() {
            MutableDynamicDataMap map = new MutableDynamicDataMap();
            for (Entry<String, DynamicDataProto.DynamicDataValue> entry :
                    mImpl.getIdToValueMap().entrySet()) {
                map.set(
                        new AppDataKey<>(entry.getKey()),
                        DynamicDataBuilders.dynamicDataValueFromProto(entry.getValue()));
            }
            return map;
        }

        /** Gets any shared state between the provider and renderer. */
        public @NonNull Map<AppDataKey<?>, DynamicDataValue<?>> getKeyToValueMapping() {
            Map<AppDataKey<?>, DynamicDataValue<?>> map = new HashMap<>();
            for (Entry<String, DynamicDataProto.DynamicDataValue> entry :
                    mImpl.getIdToValueMap().entrySet()) {
                map.put(
                        new AppDataKey<>(entry.getKey()),
                        DynamicDataBuilders.dynamicDataValueFromProto(entry.getValue()));
            }
            return Collections.unmodifiableMap(map);
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull State fromProto(
                StateProto.@NonNull State proto, @Nullable Fingerprint fingerprint) {
            return new State(proto, fingerprint);
        }

        /**
         * Creates a new wrapper instance from the proto. Intended for testing purposes only. An
         * object created using this method can't be added to any other wrapper.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull State fromProto(StateProto.@NonNull State proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public StateProto.@NonNull State toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "State{"
                    + "lastClickableId="
                    + getLastClickableId()
                    + ", keyToValueMapping="
                    + getKeyToValueMapping()
                    + "}";
        }

        /** Builder for {@link State} */
        public static final class Builder {
            private final StateProto.State.Builder mImpl = StateProto.State.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-688813584);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the mapping for any shared state between the provider and renderer. This method
             * replaces the current state map with the entries from {@code map}.Any previous entries
             * added using this {@link #setStateMap} or {@link #addKeyToValueMapping} will be
             * overwritten.
             *
             * @throws IllegalArgumentException if the size of {@code map} is larger than the
             *     allowed limit ({@link #getMaxStateEntryCount()}).
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStateMap(@NonNull DynamicDataMap map) {
                if (map.getSize() >= getMaxStateEntryCount()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "State map size is too large: %d. "
                                            + "Maximum allowed size is %d.",
                                    map.getSize(), getMaxStateEntryCount()));
                }
                mImpl.clearIdToValue();
                for (Map.Entry<
                                DynamicDataKey<? extends DynamicType>,
                                DynamicDataBuilders.DynamicDataValue<? extends DynamicType>>
                        entry : map.getEntries()) {
                    mImpl.putIdToValue(
                            entry.getKey().getKey(), entry.getValue().toDynamicDataValueProto());
                    mFingerprint.recordPropertyUpdate(
                            entry.getKey().getKey().hashCode(),
                            checkNotNull(entry.getValue().getFingerprint()).aggregateValueAsInt());
                }
                return this;
            }

            /**
             * Sets the mapping for any shared state between the provider and renderer. This method
             * replaces the current state map with the entries from {@code map}. Any previous
             * entries added using this {@link #setStateMap} or {@link #addKeyToValueMapping} will
             * be overwritten.
             *
             * @throws IllegalArgumentException if the size of {@code map} is larger than the
             *     allowed limit ({@link #getMaxStateEntryCount()}).
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStateMap(@NonNull DynamicDataPair<?>... pairs) {
                return setStateMap(dynamicDataMapOf(pairs));
            }

            /**
             * Adds the entries into any shared state between the provider and renderer.
             *
             * @throws IllegalArgumentException if adding the new key/value will make the state
             *     larger than the allowed limit ({@link #getMaxStateEntryCount()}).
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder addToStateMap(@NonNull DynamicDataPair<?>... entries) {
                if (mImpl.getIdToValueMap().size() + entries.length > getMaxStateEntryCount()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Can't add the entries to the state. Adding more will increase"
                                        + " the size to %d beyond the maximum allowed size of %d.",
                                    mImpl.getIdToValueMap().size() + entries.length,
                                    getMaxStateEntryCount()));
                }
                for (DynamicDataPair<?> entry : entries) {
                    mImpl.putIdToValue(
                            entry.getKey().getKey(), entry.getValue().toDynamicDataValueProto());
                    mFingerprint.recordPropertyUpdate(
                            entry.getKey().getKey().hashCode(),
                            checkNotNull(entry.getValue().getFingerprint()).aggregateValueAsInt());
                }
                return this;
            }

            /**
             * Adds an entry into any shared state between the provider and renderer.
             *
             * @throws IllegalStateException if adding the new key/value will make the state larger
             *     than the allowed limit ({@link #getMaxStateEntryCount()}).
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @SuppressLint("MissingGetterMatchingBuilder")
            public <T extends DynamicType> @NonNull Builder addKeyToValueMapping(
                    @NonNull AppDataKey<T> sourceKey, @NonNull DynamicDataValue<T> value) {
                if (mImpl.getIdToValueMap().size() >= getMaxStateEntryCount()) {
                    throw new IllegalStateException(
                            String.format(
                                    "Can't add more entries to the state. It is already at its "
                                            + "maximum allowed size of %d.",
                                    getMaxStateEntryCount()));
                }
                mImpl.putIdToValue(sourceKey.getKey(), value.toDynamicDataValueProto());
                mFingerprint.recordPropertyUpdate(
                        sourceKey.getKey().hashCode(),
                        checkNotNull(value.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if number of key/value pairs are greater than {@link
             *     #getMaxStateEntryCount()}.
             */
            public @NonNull State build() {
                if (mImpl.getIdToValueMap().size() > getMaxStateEntryCount()) {
                    throw new IllegalStateException(
                            String.format(
                                    "State size is too large: %d. "
                                            + "Maximum allowed state size is %d.",
                                    mImpl.getIdToValueMap().size(), getMaxStateEntryCount()));
                }
                return new State(mImpl.build(), mFingerprint);
            }
        }
    }
}
