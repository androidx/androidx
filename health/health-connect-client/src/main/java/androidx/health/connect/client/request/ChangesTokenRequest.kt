/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.request

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import kotlin.reflect.KClass

/**
 * Request object to fetch Changes-Token for given [recordTypes] in Android Health Platform.
 *
 * @param recordTypes Set of [Record] types the token will observe change for.
 * @param dataOriginFilters Optional set of [DataOrigin] filters, default is empty set for no
 * filter.
 */
class ChangesTokenRequest(
    internal val recordTypes: Set<KClass<out Record>>,
    internal val dataOriginFilters: Set<DataOrigin> = setOf()
)
