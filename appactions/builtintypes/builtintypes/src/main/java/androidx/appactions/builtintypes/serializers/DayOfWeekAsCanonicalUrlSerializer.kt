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

import androidx.appactions.builtintypes.types.DayOfWeek
import androidx.appsearch.app.StringSerializer
import kotlin.String
import kotlin.collections.firstOrNull

/**
 * Serializes [DayOfWeek] as its canonical url String so it may be stored as a
 * `@Document.StringProperty`.
 *
 * @see DayOfWeek.canonicalUrl
 */
public class DayOfWeekAsCanonicalUrlSerializer : StringSerializer<DayOfWeek> {
  override fun serialize(instance: DayOfWeek): String = instance.canonicalUrl

  override fun deserialize(`value`: String): DayOfWeek? =
    DayOfWeek.values().firstOrNull { it.canonicalUrl == value }
}
