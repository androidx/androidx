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

package androidx.savedstate.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.serialization.SavedStateConfig.Builder
import kotlin.jvm.JvmField
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Configuration for [SavedState] encoding and decoding. Use the factory function with the same name
 * to create instances of this class.
 *
 * @property serializersModule The [SerializersModule] to use for encoding or decoding.
 * @see SavedStateConfig.Builder
 */
public class SavedStateConfig private constructor(public val serializersModule: SerializersModule) {
    /** Builder for [SavedStateConfig]. Used by `SavedStateCodecConfig` factory function. */
    @Suppress(
        "MissingBuildMethod", // We do have an internal `build()` and the class is internal as well.
    )
    public class Builder internal constructor() {
        /** The [SerializersModule] to use. */
        @get:Suppress("GetterOnBuilder") // Kotlin doesn't support `public set` with `private get`:
        // https://youtrack.jetbrains.com/issue/KT-3110
        @set:Suppress("SetterReturnsThis")
        public var serializersModule: SerializersModule = EmptySerializersModule()

        internal fun build(): SavedStateConfig {
            return SavedStateConfig(serializersModule = serializersModule)
        }
    }

    public companion object {
        /** An instance of [SavedStateConfig] with default configuration. */
        @JvmField public val DEFAULT: SavedStateConfig = SavedStateConfig {}
    }
}

/**
 * Factory function for creating instances of [SavedStateConfig].
 *
 * Example usage:
 *
 * @sample androidx.savedstate.config
 * @param builderAction The function to configure the builder.
 */
public fun SavedStateConfig(builderAction: Builder.() -> Unit): SavedStateConfig {
    return Builder().apply { builderAction() }.build()
}
