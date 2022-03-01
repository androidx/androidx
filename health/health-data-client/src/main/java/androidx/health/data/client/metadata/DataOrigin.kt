/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.health.data.client.metadata

import androidx.annotation.RestrictTo
import androidx.health.data.client.records.Record

/**
 * Specifies the original source of any [Record]: application that inserted it and device on which
 * the data was generated.
 *
 * [applicationId] is populated automatically by Health Platform at insertion time based on the
 * caller application ID.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
data class DataOrigin(val applicationId: String)
