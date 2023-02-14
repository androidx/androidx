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

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The property which describes a parameter with String or Enum entity for {@code ActionCapability}.
 *
 * @param <EnumT>
 */
@AutoValue
public abstract class StringOrEnumProperty<EnumT extends Enum<EnumT>>
        extends ParamProperty<StringOrEnumProperty.PossibleValue<EnumT>> {

    /** Returns a Builder for StringOrEnumProperty for the given enumType. */
    @NonNull
    public static <EnumT extends Enum<EnumT>> Builder<EnumT> newBuilder(
            @NonNull Class<EnumT> enumType) {
        return new Builder<>(enumType);
    }

    @NonNull
    abstract Class<EnumT> enumType();

    // TODO (b/260137899)

    /**
     * Returns a new Builder that contains all the existing configuration in this
     * StringOrEnumProperty.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public Builder<EnumT> toBuilder() {
        Builder<EnumT> builder = new Builder<>(enumType());
        if (!getPossibleValues().isEmpty()) {
            for (PossibleValue<EnumT> possibleValue : getPossibleValues()) {
                if (possibleValue.getKind() == PossibleValue.Kind.STRING_VALUE) {
                    builder.addPossibleValue(
                            possibleValue.stringValue().getName(),
                            possibleValue.stringValue().getAlternateNames().toArray(String[]::new));
                } else if (possibleValue.getKind() == PossibleValue.Kind.ENUM_VALUE) {
                    builder.addPossibleValue(possibleValue.enumValue());
                }
            }
        }
        return builder.setIsRequired(isRequired());
    }

    /**
     * Represents a single possible value in StringOrEnumProperty.
     *
     * @param <EnumT>
     */
    @AutoOneOf(PossibleValue.Kind.class)
    public abstract static class PossibleValue<EnumT extends Enum<EnumT>> {
        /** Create a new StringOrEnumProperty.PossibleValue for Kind.STRING_VALUE. */
        @NonNull
        public static <EnumT extends Enum<EnumT>> PossibleValue<EnumT> of(
                @NonNull String name, @NonNull String... alternateNames) {
            return AutoOneOf_StringOrEnumProperty_PossibleValue.stringValue(
                    StringProperty.PossibleValue.of(name, alternateNames));
        }

        /** Create a new StringOrEnumProperty.PossibleValue for Kind.ENUM_VALUE. */
        @NonNull
        public static <EnumT extends Enum<EnumT>> PossibleValue<EnumT> of(
                @NonNull EnumT possibleValue) {
            return AutoOneOf_StringOrEnumProperty_PossibleValue.enumValue(possibleValue);
        }

        /** The Kind of this possible value. */
        @NonNull
        public abstract Kind getKind();

        /** Returns the StringProperty.PossibleValue, corresponds to Kind.STRING_VALUE. */
        @NonNull
        public abstract StringProperty.PossibleValue stringValue();

        /** Returns the StringProperty.PossibleValue, corresponds to Kind.ENUM_VALUE. */
        @NonNull
        public abstract EnumT enumValue();

        /** The Kind of PossibleValue. */
        public enum Kind {
            STRING_VALUE,
            ENUM_VALUE,
        }
    }

    /**
     * Builder for {@link StringOrEnumProperty}.
     *
     * @param <EnumT>
     */
    public static class Builder<EnumT extends Enum<EnumT>> {

        private final ArrayList<PossibleValue<EnumT>> mPossibleValueList = new ArrayList<>();
        private final Class<EnumT> mEnumType;
        private boolean mIsRequired;
        private boolean mEntityMatchRequired;

        private Builder(Class<EnumT> enumType) {
            this.mEnumType = enumType;
        }

        /**
         * Adds a possible string value for this property.
         *
         * @param name           the possible string value.
         * @param alternateNames the alternative names for this value.
         */
        @NonNull
        public Builder<EnumT> addPossibleValue(
                @NonNull String name, @NonNull String... alternateNames) {
            mPossibleValueList.add(PossibleValue.of(name, alternateNames));
            this.mEntityMatchRequired = true;
            return this;
        }

        /**
         * Adds possible Enum values for this parameter.
         *
         * @param enumValues possible enum entity values.
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public Builder<EnumT> addPossibleValue(@NonNull EnumT... enumValues) {
            stream(enumValues).forEach(
                    enumValue -> mPossibleValueList.add(PossibleValue.of(enumValue)));
            this.mEntityMatchRequired = true;
            return this;
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        @NonNull
        public Builder<EnumT> setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /**
         * Sets whether matching a possible value is required for this parameter. Note that this
         * value
         * can be overrided by assistant.
         *
         * @param valueMatchRequired whether value match is required
         */
        @NonNull
        public Builder<EnumT> setValueMatchRequired(boolean valueMatchRequired) {
            this.mEntityMatchRequired = valueMatchRequired;
            return this;
        }

        /** Builds the property for this Entity or Enum parameter. */
        @NonNull
        public StringOrEnumProperty<EnumT> build() {
            return new AutoValue_StringOrEnumProperty<>(
                    Collections.unmodifiableList(mPossibleValueList),
                    mIsRequired,
                    mEntityMatchRequired,
                    /* isProhibited= */ false,
                    mEnumType);
        }
    }
}
