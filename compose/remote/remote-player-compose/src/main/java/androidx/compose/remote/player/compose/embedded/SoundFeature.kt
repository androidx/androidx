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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.PlaySound
import androidx.compose.remote.core.operations.SoundData
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.view.platform.SoundSupport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Plays document sounds (`SoundData`, `SoundExpression`, `PlaySound`) from the experimental
 * profile, through the View player's `SoundSupport` (low-latency `AudioTrack`s).
 *
 * Only documents that define or play sounds get an engine, so plain documents allocate no audio
 * resources. As in the View player, `PlaySound` fires each time the draw stream runs it, so
 * documents gate it with an impulse or conditional.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SoundFeature internal constructor(private val engineFactory: RcSoundEngineFactory) :
    RcPlayerFeature(KEY) {
    override val opcodes: IntSet =
        intSetOf(Operations.DATA_SOUND, Operations.SOUND_EXPRESSION, Operations.PLAY_SOUND)

    override val profiles: Int
        get() = RcProfiles.PROFILE_EXPERIMENTAL

    override fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean {
        if (op !is PlaySound) return false
        op.apply(scope.remoteContext)
        return true
    }

    @Composable
    override fun Install(scope: RcFeatureDocumentScope) {
        val hasSound =
            remember(scope) {
                scope.document.containsOperation {
                    it is PlaySound || it is SoundData || it is SoundExpression
                }
            }
        if (!hasSound) return
        val context = LocalContext.current
        // Keyed on engineFactory, so a replacement feature with the same key but another factory
        // swaps the engine.
        DisposableEffect(scope, context, engineFactory) {
            val handle = engineFactory.create(context)
            val engine = ReloadingSoundEngine(handle.engine)
            // On first attach, CoreDocument passes the sounds it buffered at document setup to the
            // engine. It drops that buffer afterwards, so when the player re-enters composition
            // with the same document, the document's sound operations load them again. Sounds the
            // buffer just delivered are skipped, so each is loaded once.
            scope.document.setSoundEngine(engine)
            engine.reload {
                scope.document.containsOperation {
                    if (it is SoundData || it is SoundExpression) it.apply(scope.remoteContext)
                    false
                }
            }
            onDispose {
                // CoreDocument has no way to clear the engine, so park a silent one.
                scope.document.setSoundEngine(SilentSoundEngine)
                handle.release()
            }
        }
    }

    public companion object {
        private val KEY = Key("Sound")

        /** Plays sounds through the View player's `SoundSupport`. */
        public val Default: SoundFeature = SoundFeature(DefaultRcSoundEngineFactory)
    }
}

private val SilentSoundEngine =
    object : CoreDocument.SoundEngine {
        override fun loadSound(soundId: Int, data: ByteArray) {}

        override fun playSound(soundId: Int) {}
    }

/**
 * Forwards to [delegate], tracking which sounds it has loaded so that [reload] only loads the ones
 * still missing.
 */
private class ReloadingSoundEngine(private val delegate: CoreDocument.SoundEngine) :
    CoreDocument.SoundEngine {
    private val loaded = MutableIntSet()
    private var reloading = false

    override fun loadSound(soundId: Int, data: ByteArray) {
        if (reloading && soundId in loaded) return
        loaded.add(soundId)
        delegate.loadSound(soundId, data)
    }

    override fun playSound(soundId: Int) {
        delegate.playSound(soundId)
    }

    /** Runs [block], which loads the document's sounds, skipping any already loaded. */
    inline fun reload(block: () -> Unit) {
        reloading = true
        try {
            block()
        } finally {
            reloading = false
        }
    }
}

/** A [CoreDocument.SoundEngine] plus the cleanup to run when the player leaves composition. */
internal class RcSoundEngineHandle(val engine: CoreDocument.SoundEngine, val release: () -> Unit)

/** Creates the sound engine for a player. */
internal fun interface RcSoundEngineFactory {
    fun create(context: Context): RcSoundEngineHandle
}

/** The View player's [SoundSupport] (low-latency `AudioTrack`s), shared by both players. */
internal val DefaultRcSoundEngineFactory = RcSoundEngineFactory { context ->
    val support = SoundSupport().apply { init(context) }
    RcSoundEngineHandle(support.buildEngine(), support::release)
}
