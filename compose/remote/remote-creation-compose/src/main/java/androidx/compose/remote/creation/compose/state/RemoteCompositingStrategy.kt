/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Immutable

/**
 * Controls offscreen buffer compositing and alpha modulation for graphics layers.
 *
 * Mirrors [androidx.compose.ui.graphics.layer.CompositingStrategy] without depending on
 * `compose.ui`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@kotlin.jvm.JvmInline
public value class RemoteCompositingStrategy internal constructor(public val value: Int) {

    override fun toString(): String =
        when (this) {
            Auto -> "Auto"
            Offscreen -> "Offscreen"
            ModulateAlpha -> "ModulateAlpha"
            else -> "Unknown"
        }

    public companion object {
        public val Auto: RemoteCompositingStrategy = RemoteCompositingStrategy(0)
        public val Offscreen: RemoteCompositingStrategy = RemoteCompositingStrategy(1)
        public val ModulateAlpha: RemoteCompositingStrategy = RemoteCompositingStrategy(2)
    }
}
