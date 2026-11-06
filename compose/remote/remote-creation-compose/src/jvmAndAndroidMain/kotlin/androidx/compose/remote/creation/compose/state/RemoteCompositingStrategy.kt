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

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/** Determines when to render content into an offscreen buffer before compositing. */
@Immutable
@JvmInline
public value class RemoteCompositingStrategy internal constructor(internal val value: Int) {

    override fun toString(): String =
        when (this) {
            Auto -> "Auto"
            Offscreen -> "Offscreen"
            ModulateAlpha -> "ModulateAlpha"
            else -> "Unknown"
        }

    public companion object {
        /**
         * Renders content directly into the destination unless layer parameters require an
         * offscreen buffer.
         */
        public val Auto: RemoteCompositingStrategy = RemoteCompositingStrategy(0)

        /**
         * Always renders layer content into an intermediate offscreen buffer before compositing.
         */
        public val Offscreen: RemoteCompositingStrategy = RemoteCompositingStrategy(1)

        /** Modulates alpha directly during rendering operations where supported. */
        public val ModulateAlpha: RemoteCompositingStrategy = RemoteCompositingStrategy(2)
    }
}
