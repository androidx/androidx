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
import androidx.appactions.interaction.capabilities.core.properties.Entity;
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/** Testing implementation of a capability Property. */
@AutoValue
public abstract class Property {

    public static Builder newBuilder() {
        return new AutoValue_Property.Builder();
    }

    public abstract ParamProperty<Entity> requiredEntityField();

    public abstract Optional<ParamProperty<StringValue>> optionalStringField();

    public abstract Optional<ParamProperty<TestEnum>> enumField();

    public abstract Optional<ParamProperty<StringValue>> repeatedStringField();

    /** Builder for the testing Property. */
    @AutoValue.Builder
    public abstract static class Builder implements BuilderOf<Property> {

        public abstract Builder setRequiredEntityField(ParamProperty<Entity> property);

        public abstract Builder setOptionalStringField(ParamProperty<StringValue> property);

        public abstract Builder setEnumField(ParamProperty<TestEnum> property);

        public abstract Builder setRepeatedStringField(ParamProperty<StringValue> property);

        @NonNull
        @Override
        public abstract Property build();
    }
}
