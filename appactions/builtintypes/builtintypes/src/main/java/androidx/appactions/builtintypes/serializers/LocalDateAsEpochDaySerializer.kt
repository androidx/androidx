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
import java.time.LocalDate

/**
 * Serializes a [LocalDate] to an epoch day so it may be stored as a `@Document.LongProperty`.
 *
 * @see LocalDate.toEpochDay
 */
class LocalDateAsEpochDaySerializer : LongSerializer<LocalDate> {
  override fun serialize(instance: LocalDate): Long = instance.toEpochDay()

  override fun deserialize(value: Long): LocalDate = LocalDate.ofEpochDay(value)
}
