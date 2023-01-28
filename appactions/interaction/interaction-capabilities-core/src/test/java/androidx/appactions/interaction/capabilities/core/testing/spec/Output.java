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

import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Optional;

/** Testing implementation of a capability Output. */
@AutoValue
public abstract class Output {

    public static Builder builder() {
        return new AutoValue_Output.Builder();
    }

    public abstract Optional<String> optionalStringField();

    @SuppressWarnings("AutoValueImmutableFields")
    public abstract List<String> repeatedStringField();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setOptionalStringField(String value);

        public abstract Builder setRepeatedStringField(List<String> value);

        public abstract Output build();
    }
}
