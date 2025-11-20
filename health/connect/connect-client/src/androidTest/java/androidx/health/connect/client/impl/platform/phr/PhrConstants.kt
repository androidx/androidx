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

package androidx.health.connect.client.impl.platform.phr

import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.records.FhirVersion

// Note: lazy must be used to avoid crashes when tests are run on a device where PHR is not
// available. In tests where these constants are used, assumeTrue() is used to make sure PHR is
// available.
internal object PhrConstants {
    @OptIn(ExperimentalPersonalHealthRecordApi::class)
    val FHIR_VERSION_4_0_1 by lazy { FhirVersion.Companion.parseFhirVersion("4.0.1") }
}
