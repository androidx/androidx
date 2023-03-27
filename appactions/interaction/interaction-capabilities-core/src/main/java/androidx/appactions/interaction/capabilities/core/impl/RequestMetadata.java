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

package androidx.appactions.interaction.capabilities.core.impl;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment;

import com.google.auto.value.AutoValue;

/** Represents metadata from the Assistant FulfillmentRequest. */
@AutoValue
public abstract class RequestMetadata {

    /** Create a Builder instance for building a RequestMetadata instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_RequestMetadata.Builder();
    }

    /** The Type of request Assistant is sending on this FulfillmentRequest. */
    @NonNull
    public abstract Fulfillment.Type requestType();

    /** Builder for RequestMetadata. */
    @AutoValue.Builder
    public abstract static class Builder {
        /** Sets the FulfillmentRequest.Type. */
        @NonNull
        public abstract Builder setRequestType(@NonNull Fulfillment.Type requestType);

        /** Builds the RequestMetadata instance. */
        @NonNull
        public abstract RequestMetadata build();
    }
}
