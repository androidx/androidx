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

@file:JvmName("RemoteImageBitmapComposeKt")

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/**
 * Creates a [RemoteImageBitmap] instance from a [ImageBitmap] value.
 *
 * @param value The [ImageBitmap] value.
 * @return A [RemoteImageBitmap] representing the provided bitmap.
 */
public fun RemoteImageBitmap(value: ImageBitmap): RemoteImageBitmap =
    MutableRemoteImageBitmap(
        constantValueOrNull = value,
        cacheKey = RemoteConstantCacheKey(value),
        idProvider = { creationState ->
            creationState.document.addBitmap(value.asAndroidBitmap())
        },
    )

/**
 * Creates a named [RemoteImageBitmap] with an initial value.
 *
 * Named remote bitmaps can be set via AndroidRemoteContext.setNamedBitmap.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [ImageBitmap] value for the named remote bitmap.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteImageBitmap] representing the named bitmap.
 */
@RememberInComposition
public fun RemoteImageBitmap.Companion.createNamedRemoteImageBitmap(
    name: String,
    defaultValue: ImageBitmap,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteImageBitmap =
    MutableRemoteImageBitmap(
        constantValueOrNull = null,
        cacheKey = RemoteNamedCacheKey(domain, name),
    ) { creationState ->
        creationState.document.addNamedBitmap(
            domain.prefixed(name),
            defaultValue.asAndroidBitmap(),
        )
    }

/**
 * Creates a [MutableRemoteImageBitmap] with an [initialValue].
 *
 * @param initialValue The initial [ImageBitmap] value.
 * @return a [MutableRemoteImageBitmap] initialized with [initialValue]
 */
@RememberInComposition
public fun MutableRemoteImageBitmap(initialValue: ImageBitmap): MutableRemoteImageBitmap =
    MutableRemoteImageBitmap(
        constantValueOrNull = initialValue,
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState ->
            creationState.document.addBitmap(initialValue.asAndroidBitmap())
        },
    )

/**
 * Factory for mutable remote bitmap state.
 *
 * @param initialValue The initial [ImageBitmap] value.
 * @return A [MutableRemoteImageBitmap] instance that will be remembered across recompositions.
 */
@RemoteComposable
@Composable
public fun rememberMutableRemoteImageBitmap(initialValue: ImageBitmap): MutableRemoteImageBitmap {
    return remember { MutableRemoteImageBitmap(initialValue) }
}

/** Extension property to convert a [ImageBitmap] to a [RemoteImageBitmap]. */
public val ImageBitmap.rb: RemoteImageBitmap
    get() = asRemoteImageBitmap()

/**
 * Extension function to convert a [ImageBitmap] to a [RemoteImageBitmap].
 *
 * @return a [RemoteImageBitmap] representing this [ImageBitmap]
 */
public fun ImageBitmap.asRemoteImageBitmap(): RemoteImageBitmap = RemoteImageBitmap(this)

/**
 * Extension property to get the [ImageBitmap] of this [RemoteImageBitmap], or null if not an
 * ImageBitmap constant.
 */
public val RemoteImageBitmap.imageBitmap: ImageBitmap?
    get() = constantValueOrNull as? ImageBitmap
