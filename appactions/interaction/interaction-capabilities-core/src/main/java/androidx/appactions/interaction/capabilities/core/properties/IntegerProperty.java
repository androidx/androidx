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

/** The property which describes an integer parameter for {@code ActionCapability}. */
@AutoValue
public abstract class IntegerProperty extends ParamProperty<Integer> {
    // The default instance of IntegerProperty. When add EMPTY IntegerProperty to capability, the
    // intent param will be set with default values.
    public static final IntegerProperty EMPTY = IntegerProperty.newBuilder().build();

    /**
     * Using IntegerProperty.REQUIRED ensures that any FulfillmentRequest to this capability will
     * contain a value for this property.
     */
    public static final IntegerProperty REQUIRED =
            IntegerProperty.newBuilder().setIsRequired(true).build();

    // TODO (b/260137899)

    /** Creates a new Builder for an IntegerProperty. */
    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    /** Builder for {@link IntegerProperty}. */
    public static class Builder {
        private boolean mIsRequired;

        private Builder() {
        }

        /** Sets whether this property is required for fulfillment. */
        @NonNull
        public Builder setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /** Builds the property for this integer parameter. */
        @NonNull
        public IntegerProperty build() {
            return new AutoValue_IntegerProperty(
                    /** getPossibleValues */
                    Collections.unmodifiableList(Collections.emptyList()),
                    mIsRequired,
                    /* valueMatchRequired= */ false,
                    /* prohibited= */ false);
        }
    }
}
