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
package androidx.health.data.client.request

import androidx.annotation.RestrictTo
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.records.Record
import kotlin.reflect.KClass

/**
 * Request object to fetch Changes-Token for given [recordTypes] in Android Health Platform.
 *
 * @property recordTypes Set of [Record] types the token will observe change for
 * @property dataOriginFilters Optional set of [DataOrigin] filters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ChangesTokenRequest(
    val recordTypes: Set<KClass<out Record>>,
    val dataOriginFilters: Set<DataOrigin> = setOf()
)
