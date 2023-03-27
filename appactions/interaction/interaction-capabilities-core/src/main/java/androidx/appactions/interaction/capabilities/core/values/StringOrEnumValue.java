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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoOneOf;

/**
 * Represents a string or enum argument value for {@code ActionCapability}.
 *
 * @param <EnumT>
 */
@AutoOneOf(StringOrEnumValue.Kind.class)
public abstract class StringOrEnumValue<EnumT extends Enum<EnumT>> {
    /** Creates a StringOrEnumValue instance with the given String. */
    @NonNull
    public static <EnumT extends Enum<EnumT>> StringOrEnumValue<EnumT> ofStringValue(
            @NonNull String s) {
        return AutoOneOf_StringOrEnumValue.stringValue(s);
    }

    /** Creates a StringOrEnumValue instance with the given Enum value. */
    @NonNull
    public static <EnumT extends Enum<EnumT>> StringOrEnumValue<EnumT> ofEnumValue(
            @NonNull EnumT enumValue) {
        return AutoOneOf_StringOrEnumValue.enumValue(enumValue);
    }

    /** The Kind of this StringOrEnumValue. */
    @NonNull
    public abstract Kind getKind();

    /** The String value of this StringOrEnumValue, for Kind.STRING_VALUE. */
    @NonNull
    public abstract String stringValue();

    /** The Enum value of this StringOrEnumValue, for Kind.ENUM_VALUE. */
    @NonNull
    public abstract EnumT enumValue();

    /** Possible argument type. */
    public enum Kind {
        STRING_VALUE,
        ENUM_VALUE,
    }
}
