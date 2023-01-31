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

import java.util.Collections;

/**
 * A simple property which describes a parameter for {@code ActionCapability}. This property has
 * simple configurations available and is not tied to a specific type.
 */
@AutoValue
public abstract class SimpleProperty extends ParamProperty<Void> {
    /** Returns a default SimpleProperty instance. */
    public static final SimpleProperty DEFAULT = SimpleProperty.newBuilder().build();

    /**
     * Using SimpleProperty.REQUIRED ensures that the corresponding Argument will contain a
     * value.
     */
    public static final SimpleProperty REQUIRED =
            SimpleProperty.newBuilder().setIsRequired(true).build();

    // TODO (b/260137899)

    /** Returns a Builder instance for SimpleProperty. */
    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    /** Builder for {@link SimpleProperty}. */
    public static class Builder {

        private boolean mIsRequired;

        private Builder() {
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        @NonNull
        public Builder setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /** Builds the property for this string parameter. */
        @NonNull
        public SimpleProperty build() {
            return new AutoValue_SimpleProperty(
                    /** getPossibleValues */
                    Collections.unmodifiableList(Collections.emptyList()),
                    mIsRequired,
                    /* valueMatchRequired= */ false,
                    /* prohibited= */ false);
        }
    }
}
