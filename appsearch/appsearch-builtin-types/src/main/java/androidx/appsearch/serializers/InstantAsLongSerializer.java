/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.serializers;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appsearch.app.LongSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.DateTimeException;

/**
 * Serializes a {@link Instant} to and from a long.
 *
 * The Instant is serialized and deserialized from and to epoch milliseconds.
 */
// @exportToFramework:skipFile()
@RequiresApi(api = Build.VERSION_CODES.O)
public class InstantAsLongSerializer implements LongSerializer<Instant> {

    /**
     * Serializes an Instant to a long.
     *
     * @param instant The Instant to serialize.
     * @return The long serialized from the Instant.
     */
    @Override
    public long serialize(@NonNull Instant instant) {
        return instant.toEpochMilli();
    }

    /**
     * Deserializes a long to an Instant.
     *
     * @param epochMilli The long to deserialize.
     * @return The Instant deserialized from the long.
     */
    @Override
    public @NonNull Instant deserialize(long epochMilli) {
        try {
            return Instant.ofEpochMilli(epochMilli);
        } catch (DateTimeException e) {
            return Instant.EPOCH;
        }
    }
}