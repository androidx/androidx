/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore

import kotlin.concurrent.Volatile

data class TestingSerializerConfig(
    @Volatile var failReadWithCorruptionException: Boolean = false,
    @Volatile var failingRead: Boolean = false,
    @Volatile var failingWrite: Boolean = false,
    @Volatile var defaultValue: Byte = 0,
    // This field enables more granular control and flexibility on failReadWithCorruptionException.
    // TestSerializer uses the values from this list in sequence first before it always uses the
    // value of failReadWithCorruptionException.
    @Volatile var listOfFailReadWithCorruptionException: List<Boolean> = listOf(),
    // This field enables the TestingSerializer to keep the count of file writes that is readable
    // from the test methods.
    @Volatile var writeCount: Int = 0,
)
