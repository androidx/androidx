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

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The property which describes a string parameter for {@code ActionCapability}. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class StringProperty extends ParamProperty<StringProperty.PossibleValue> {
    /** A default StringProperty instance. This property will accept any string value. */
    public static final StringProperty EMPTY = StringProperty.newBuilder().build();

    /**
     * Using StringProperty.PROHIBITED will ensure no Argument containing a value for this field
     * will
     * be received by this capability.
     */
    public static final StringProperty PROHIBITED =
            StringProperty.newBuilder().setIsProhibited(true).build();

    /** Returns a Builder instance for StringProperty. */
    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    // TODO (b/260137899)

    /** Represents a single possible value for StringProperty. */
    @AutoValue
    public abstract static class PossibleValue {

        /** Creates a new StringProperty.PossibleValue instance. */
        @NonNull
        public static final PossibleValue of(@NonNull String name,
                @NonNull String... alternateNames) {
            return new AutoValue_StringProperty_PossibleValue(
                    name, Collections.unmodifiableList(Arrays.asList(alternateNames)));
        }

        /** The name of this possible value. */
        @NonNull
        public abstract String getName();

        /** The alternate names of this possible value. */
        @NonNull
        public abstract List<String> getAlternateNames();
    }

    /** Builder for {@link StringProperty}. */
    public static class Builder {

        private final ArrayList<PossibleValue> mPossibleValueList = new ArrayList<>();
        private boolean mIsRequired;
        private boolean mEntityMatchRequired;
        private boolean mIsProhibited;

        private Builder() {
        }

        /**
         * Adds a possible string value for this property.
         *
         * @param name           the possible string value.
         * @param alternateNames the alternate names for this value.
         */
        @NonNull
        public Builder addPossibleValue(@NonNull String name, @NonNull String... alternateNames) {
            mPossibleValueList.add(PossibleValue.of(name, alternateNames));
            return this;
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        @NonNull
        public Builder setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /**
         * Sets whether or not this property requires that the value for this property must match
         * one of
         * the string values in the defined possible values.
         */
        @NonNull
        public Builder setValueMatchRequired(boolean valueMatchRequired) {
            this.mEntityMatchRequired = valueMatchRequired;
            return this;
        }

        /**
         * Sets whether the String property is prohibited in the response.
         *
         * @param isProhibited Whether this property is prohibited in the response.
         */
        @NonNull
        public Builder setIsProhibited(boolean isProhibited) {
            this.mIsProhibited = isProhibited;
            return this;
        }

        /** Builds the property for this string parameter. */
        @NonNull
        public StringProperty build() {
            return new AutoValue_StringProperty(
                    Collections.unmodifiableList(mPossibleValueList),
                    mIsRequired,
                    mEntityMatchRequired,
                    mIsProhibited);
        }
    }
}
