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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents a Density-independent pixel (Dp) value.
 *
 * @property value The [RemoteFloat] that holds the actual Dp value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDp(public val value: RemoteFloat) : BaseRemoteState<Dp>() {

    override val constantValueOrNull: Dp?
        get() = value.constantValueOrNull?.dp

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /**
     * Function to convert this [RemoteDp] to a density-independent pixel value. It multiplies the
     * current float value by the screen\'s density.
     */
    public fun toPx(density: RemoteDensity): RemoteFloat {
        return density.density * value
    }
}

/** Extension property to convert an [Int] to a [RemoteDp]. */
public val Int.rdp: RemoteDp
    get() {
        return RemoteDp(this.rf)
    }

/** Extension property to convert a [Dp] to a [RemoteDp]. */
public fun Dp.asRdp(): RemoteDp {
    return RemoteDp(this.value.rf)
}
