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

package androidx.appactions.interaction.capabilities.core.properties;

import static java.util.Arrays.stream;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The property which describes an Enum parameter for {@code ActionCapability}.
 *
 * @param <EnumT>
 */
@AutoValue
public abstract class EnumProperty<EnumT extends Enum<EnumT>> extends ParamProperty<EnumT> {

    /**
     * Returns a Builder to build an EnumProperty instance.
     *
     * @param enumType the class of the enum.
     */
    @NonNull
    public static <EnumT extends Enum<EnumT>> Builder<EnumT> newBuilder(
            @NonNull Class<EnumT> enumType) {
        return new Builder<>(enumType);
    }

    // TODO (b/260137899)

    abstract Class<EnumT> enumType();

    /**
     * Builder for {@link EnumProperty}.
     *
     * @param <EnumT>
     */
    public static final class Builder<EnumT extends Enum<EnumT>> {

        private final ArrayList<EnumT> mPossibleEnumList = new ArrayList<>();
        private final Class<EnumT> mEnumType;
        private boolean mIsRequired;
        private boolean mEntityMatchRequired;

        private Builder(Class<EnumT> enumType) {
            this.mEnumType = enumType;
        }

        /**
         * Adds all app supported entity for this enum parameter. If any supported enum value is
         * added
         * then the entity matched is reuqired.
         *
         * @param supportedEnumValue supported enum values.
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public Builder<EnumT> addSupportedEnumValues(@NonNull EnumT... supportedEnumValue) {
            stream(supportedEnumValue).forEach(mPossibleEnumList::add);
            this.mEntityMatchRequired = true;
            return this;
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        @NonNull
        public Builder<EnumT> setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /** Builds the property for this Enum parameter. */
        @NonNull
        public EnumProperty<EnumT> build() {
            return new AutoValue_EnumProperty<>(
                    Collections.unmodifiableList(mPossibleEnumList),
                    mIsRequired,
                    mEntityMatchRequired,
                    /* isProhibited= */ false,
                    mEnumType);
        }
    }
}
