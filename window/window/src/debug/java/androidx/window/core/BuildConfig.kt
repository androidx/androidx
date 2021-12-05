/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.core

import androidx.window.core.SpecificationComputer.VerificationMode
import androidx.window.core.SpecificationComputer.VerificationMode.STRICT

/**
 * A configuration to be used for the debug flavor of the library. Default [VerificationMode] is
 * [STRICT] so to surface errors.
 */
internal object BuildConfig {
    val verificationMode: VerificationMode = STRICT
}