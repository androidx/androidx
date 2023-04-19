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

import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.LIST_ITEM_TYPE_SPEC;

import androidx.annotation.NonNull;
import androidx.appactions.builtintypes.experimental.types.ListItem;
import androidx.appactions.interaction.capabilities.core.AppEntityListener;
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder;
import androidx.appactions.interaction.capabilities.core.properties.Property;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/** Used to test the filling behavior of structured entities (e.g. ListItem) */
public final class CapabilityStructFill {

    private static final String CAPABILITY_NAME = "actions.intent.TEST";
    public static final ActionSpec<Properties, Arguments, Void> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setDescriptor(Properties.class)
                    .setArguments(Arguments.class, Arguments::newBuilder)
                    .bindOptionalParameter(
                            "listItem",
                            Properties::listItem,
                            Arguments.Builder::setListItem,
                            ParamValueConverter.Companion.of(LIST_ITEM_TYPE_SPEC),
                            EntityConverter.Companion.of(LIST_ITEM_TYPE_SPEC)::convert)
                    .bindOptionalParameter(
                            "string",
                            Properties::anyString,
                            Arguments.Builder::setAnyString,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .build();

    private CapabilityStructFill() {}

    /** Two required strings */
    @AutoValue
    public abstract static class Arguments {
        public static Builder newBuilder() {
            return new AutoValue_CapabilityStructFill_Arguments.Builder();
        }

        public abstract Optional<ListItem> listItem();

        public abstract Optional<String> anyString();

        /** Builder for the testing Arguments. */
        @AutoValue.Builder
        public abstract static class Builder implements BuilderOf<Arguments> {

            public abstract Builder setListItem(@NonNull ListItem value);

            public abstract Builder setAnyString(@NonNull String value);

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
            return new AutoValue_CapabilityStructFill_Properties.Builder();
        }

        public abstract Optional<Property<ListItem>> listItem();

        public abstract Optional<Property<StringValue>> anyString();

        /** Builder for {@link Property} */
        @AutoValue.Builder
        public abstract static class Builder {

            @NonNull
            public abstract Builder setListItem(@NonNull Property<ListItem> value);

            @NonNull
            public abstract Builder setAnyString(@NonNull Property<StringValue> value);

            @NonNull
            public abstract Properties build();
        }
    }

    public interface ExecutionSession extends BaseExecutionSession<Arguments, Void> {
        @NonNull
        AppEntityListener<ListItem> getListItemListener();
    }
}
