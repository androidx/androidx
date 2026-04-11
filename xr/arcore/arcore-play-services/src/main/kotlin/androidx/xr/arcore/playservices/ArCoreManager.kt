/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.LifecycleManager
import com.google.ar.core.Session
import kotlin.time.ComparableTimeMark

// TODO:  b/396240241 -- Appropriately handle or translate any exceptions thrown by Android 1.x
/**
 * Manages the lifecycle of an ARCore session.
 *
 * @property timeSource the [ArCoreTimeSource] instance
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ArCoreManager internal constructor(internal val timeSource: ArCoreTimeSource) :
    LifecycleManager {

    internal lateinit var _session: Session

    /**
     * The underlying [Session] instance.
     *
     * @return the underlying [Session] instance
     * @sample androidx.xr.arcore.samples.getARCoreSession
     */
    @UnsupportedArCoreCompatApi public fun session(): Session = _session

    /**
     * This method implements the [LifecycleManager.create] method.
     *
     * This method must be called before any operations can be performed by the
     * [ArCorePerceptionManager].
     */
    override fun create() {}

    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    override var config: Config = Config()
        private set

    override fun configure(config: Config) {
        this.config = config
    }

    override fun resume() {}

    override suspend fun update(): ComparableTimeMark {
        return timeSource.markNow()
    }

    override fun pause() {}

    override fun stop() {}
}
