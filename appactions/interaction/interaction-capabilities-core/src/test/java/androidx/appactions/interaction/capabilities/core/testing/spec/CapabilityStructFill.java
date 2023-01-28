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
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder;
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.capabilities.core.values.ListItem;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/** Used to test the filling behavior of structured entities (e.g. ListItem) */
public final class CapabilityStructFill {

    private static final String CAPABILITY_NAME = "actions.intent.TEST";
    public static final ActionSpec<Property, Argument, Void> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setDescriptor(Property.class)
                    .setArgument(Argument.class, Argument::newBuilder)
                    .bindStructParameter(
                            "listItem",
                            Property::itemList,
                            Argument.Builder::setListItem,
                            TypeConverters::toListItem)
                    .bindOptionalStringParameter(
                            "string", Property::anyString, Argument.Builder::setAnyString)
                    .build();

    private CapabilityStructFill() {
    }

    /** Two required strings */
    @AutoValue
    public abstract static class Argument {
        public static Builder newBuilder() {
            return new AutoValue_CapabilityStructFill_Argument.Builder();
        }

        public abstract Optional<ListItem> listItem();

        public abstract Optional<String> anyString();

        /** Builder for the testing Argument. */
        @AutoValue.Builder
        public abstract static class Builder implements BuilderOf<Argument> {

            public abstract Builder setListItem(@NonNull ListItem value);

            public abstract Builder setAnyString(@NonNull String value);

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
            return new AutoValue_CapabilityStructFill_Property.Builder();
        }

        public abstract Optional<SimpleProperty> itemList();

        public abstract Optional<StringProperty> anyString();

        /** Builder for {@link Property} */
        @AutoValue.Builder
        public abstract static class Builder {

            @NonNull
            public abstract Builder setItemList(@NonNull SimpleProperty value);

            @NonNull
            public abstract Builder setAnyString(@NonNull StringProperty value);

            @NonNull
            public abstract Property build();
        }
    }
}
