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

package androidx.appactions.interaction.capabilities.core.testing.spec;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;

import com.google.auto.value.AutoValue;

import java.util.Optional;

public final class CapabilityTwoStrings {

    private static final String CAPABILITY_NAME = "actions.intent.TEST";
    public static final ActionSpec<Property, Argument, Void> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setDescriptor(Property.class)
                    .setArgument(Argument.class, Argument::newBuilder)
                    .bindOptionalStringParameter(
                            "stringSlotA", Property::stringSlotA, Argument.Builder::setStringSlotA)
                    .bindOptionalStringParameter(
                            "stringSlotB", Property::stringSlotB, Argument.Builder::setStringSlotB)
                    .build();

    private CapabilityTwoStrings() {
    }

    /** Two required strings */
    @AutoValue
    public abstract static class Argument {
        public static Builder newBuilder() {
            return new AutoValue_CapabilityTwoStrings_Argument.Builder();
        }

        public abstract Optional<String> stringSlotA();

        public abstract Optional<String> stringSlotB();

        /** Builder for the testing Argument. */
        @AutoValue.Builder
        public abstract static class Builder implements BuilderOf<Argument> {

            public abstract Builder setStringSlotA(@NonNull String value);

            public abstract Builder setStringSlotB(@NonNull String value);

            @NonNull
            @Override
            public abstract Argument build();
        }
    }

    /** Two required strings */
    @AutoValue
    public abstract static class Property {
        @NonNull
        public static Builder newBuilder() {
            return new AutoValue_CapabilityTwoStrings_Property.Builder();
        }

        public abstract Optional<StringProperty> stringSlotA();

        public abstract Optional<StringProperty> stringSlotB();

        /** Builder for {@link Property} */
        @AutoValue.Builder
        public abstract static class Builder {

            @NonNull
            public abstract Builder setStringSlotA(@NonNull StringProperty value);

            @NonNull
            public abstract Builder setStringSlotB(@NonNull StringProperty value);

            @NonNull
            public abstract Property build();
        }
    }
}
