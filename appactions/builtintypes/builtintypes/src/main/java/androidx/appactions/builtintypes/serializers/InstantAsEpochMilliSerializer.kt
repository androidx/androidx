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

package androidx.appactions.builtintypes.serializers

import androidx.appsearch.app.LongSerializer
import java.time.Instant

/**
 * Serializes an [Instant] to epoch millis so it may be stored as a `@Document.LongProperty`.
 *
 * @see Instant.toEpochMilli
 */
class InstantAsEpochMilliSerializer : LongSerializer<Instant> {
  override fun serialize(instance: Instant): Long = instance.toEpochMilli()

  override fun deserialize(value: Long): Instant = Instant.ofEpochMilli(value)
}
