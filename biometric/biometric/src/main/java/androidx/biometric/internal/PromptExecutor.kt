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

package androidx.biometric.internal

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/** An executor used by [android.hardware.biometrics.BiometricPrompt] to run framework code. */
internal class PromptExecutor : Executor {
    private val promptHandler = Handler(Looper.getMainLooper())

    override fun execute(runnable: Runnable) {
        promptHandler.post(runnable)
    }
}
