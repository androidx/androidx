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
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder;
import androidx.appactions.interaction.capabilities.core.properties.Property;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;

import com.google.auto.value.AutoValue;

import java.util.Optional;

public final class CapabilityTwoStrings {

    private static final String CAPABILITY_NAME = "actions.intent.TEST";
    public static final ActionSpec<Properties, Arguments, Void> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setDescriptor(Properties.class)
                    .setArguments(Arguments.class, Arguments::newBuilder)
                    .bindOptionalParameter(
                            "stringSlotA",
                            Properties::stringSlotA,
                            Arguments.Builder::setStringSlotA,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .bindOptionalParameter(
                            "stringSlotB",
                            Properties::stringSlotB,
                            Arguments.Builder::setStringSlotB,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .build();

    private CapabilityTwoStrings() {
    }

    /** Two required strings */
    @AutoValue
    public abstract static class Arguments {
        public static Builder newBuilder() {
            return new AutoValue_CapabilityTwoStrings_Arguments.Builder();
        }

        public abstract Optional<String> stringSlotA();

        public abstract Optional<String> stringSlotB();

        /** Builder for the testing Arguments. */
        @AutoValue.Builder
        public abstract static class Builder implements BuilderOf<Arguments> {

            public abstract Builder setStringSlotA(@NonNull String value);

            public abstract Builder setStringSlotB(@NonNull String value);

            @NonNull
            @Override
            public abstract Arguments build();
        }
    }

    /** Two required strings */
    @AutoValue
    public abstract static class Properties {
        @NonNull
        public static Builder newBuilder() {
            return new AutoValue_CapabilityTwoStrings_Properties.Builder();
        }

        public abstract Optional<Property<StringValue>> stringSlotA();

        public abstract Optional<Property<StringValue>> stringSlotB();

        /** Builder for {@link Property} */
        @AutoValue.Builder
        public abstract static class Builder {

            @NonNull
            public abstract Builder setStringSlotA(@NonNull Property<StringValue> value);

            @NonNull
            public abstract Builder setStringSlotB(@NonNull Property<StringValue> value);

            @NonNull
            public abstract Properties build();
        }
    }

    public interface ExecutionSession extends BaseExecutionSession<Arguments, Void> {}
}
