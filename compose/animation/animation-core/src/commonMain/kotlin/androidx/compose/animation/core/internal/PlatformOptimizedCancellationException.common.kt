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

<<<<<<<< HEAD:compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/internal/PlatformOptimizedCancellationException.common.kt
package androidx.compose.animation.core.internal

import kotlinx.coroutines.CancellationException

/**
 * Represents a platform-optimized cancellation exception.
 * This allows us to configure exceptions separately on JVM and other platforms.
 */
internal expect abstract class PlatformOptimizedCancellationException(
    message: String? = null
) : CancellationException
========
package androidx.compose.ui.platform

/**
 * Represents a request to open a platform-specific text input session via
 * `PlatformTextInputModifierNode.textInputSession`.
 */
expect interface PlatformTextInputMethodRequest
>>>>>>>> sync-androidx/revert/revert-1.6.0-alpha08_merge-1.6.0-beta01:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/PlatformTextInputMethodRequest.kt
