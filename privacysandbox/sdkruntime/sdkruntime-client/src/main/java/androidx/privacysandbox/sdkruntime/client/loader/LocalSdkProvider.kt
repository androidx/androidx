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
package androidx.privacysandbox.sdkruntime.client.loader

import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import org.jetbrains.annotations.TestOnly

/**
 * Provides interface for interaction with locally loaded SDK.
 * Handle different protocol versions inside.
 *
 */
internal abstract class LocalSdkProvider protected constructor(
    @get:TestOnly val sdkProvider: Any
) {

    abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat

    abstract fun beforeUnloadSdk()
}
