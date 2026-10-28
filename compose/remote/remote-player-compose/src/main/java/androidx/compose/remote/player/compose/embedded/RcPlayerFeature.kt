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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.Composable

/**
 * An optional group of operations, or a host capability, that [RcPlayer] can execute on top of its
 * built-in support for the baseline and AndroidX profiles.
 *
 * The player's draw loop never refers to a feature's operations directly. Anything the loop has no
 * branch for is offered to each feature's [paint] in turn, so an app that never references a
 * feature lets R8 remove the feature and everything it depends on (audio, sensors, mesh rendering).
 *
 * A feature only reaches the player through [RcFeatureDrawScope] and [RcFeatureDocumentScope].
 * These expose the document and the player's `RemoteContext`, so a feature can read and update
 * document state, but not the player's draw loop, layout tree or composition.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RcPlayerFeature(
    /**
     * Identifies the feature. Within an [RcPlayerFeatureSet] a later feature with the same key
     * replaces an earlier one, which lets an app override a default feature.
     */
    public val key: Key
) {
    /** Identity of a feature. Compared by reference, so declare one instance per feature. */
    public class Key(public val name: String) {
        override fun toString(): String = "RcPlayerFeature.Key($name)"
    }

    /**
     * Opcodes (see `Operations`) this feature executes. Used to check that a player covers a
     * document's profile; dispatch itself goes through [paint].
     */
    public abstract val opcodes: IntSet

    /**
     * Profile bits from [RcProfiles] that this feature helps a player support. A player supports a
     * profile once every opcode it adds beyond the built-in profiles is claimed by some feature.
     */
    public open val profiles: Int
        get() = RcProfiles.PROFILE_BASELINE

    /**
     * Executes [op] in the draw pass. Called on the draw thread for each operation the built-in
     * loop does not handle. Returns `true` if the feature handled [op]; `false` leaves it to the
     * next feature and then the player's handling of unknown operations.
     */
    public open fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean = false

    /**
     * Document-lifetime side effects, such as engines or listeners. Called while the player is
     * composed; anything acquired here must be released in a `DisposableEffect`'s `onDispose`.
     */
    @Composable public open fun Install(scope: RcFeatureDocumentScope) {}

    override fun toString(): String = key.name
}
