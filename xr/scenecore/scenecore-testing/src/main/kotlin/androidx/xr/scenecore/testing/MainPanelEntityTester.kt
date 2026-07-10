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

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.MainPanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.testing.internal.FakePanelEntity as InternalFakePanelEntity
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime as InternalFakeSceneRuntime
import androidx.xr.scenecore.toRtPixelDimensions

/**
 * A test data accessor for the `session.scene.mainPanelEntity`, which is associated with the main
 * window for the Runtime, to inspect and manipulate underlying fake data.
 */
public class MainPanelEntityTester internal constructor(rtPanelEntity: InternalFakePanelEntity) :
    PanelEntityTester(rtPanelEntity) {

    internal companion object {
        internal fun create(runtime: InternalFakeSceneRuntime): MainPanelEntityTester {
            @Suppress("DEPRECATION")
            return MainPanelEntityTester(runtime.mainPanelEntity as InternalFakePanelEntity)
        }
    }

    /**
     * Changes the perceived resolution of the main window, invoking all registered listeners.
     *
     * This triggers callbacks registered via
     * [MainPanelEntity.addPerceivedResolutionChangedListener]. This will also affect the value
     * returned by [MainPanelEntity.getPerceivedResolution].
     *
     * @param dimensions The pixel dimensions of the panel on the camera view.
     */
    public fun triggerOnPerceivedResolutionChanged(dimensions: IntSize2d) {
        val rtDims = dimensions.toRtPixelDimensions()
        val rtResult = RtPerceivedResolutionResult.Success(rtDims)
        rtPanelEntity.setPerceivedResolution(rtResult)
        checkNotNull(InternalFakeSceneRuntime.instance)
            .onPerceivedResolutionChanged(dimensions.width, dimensions.height)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MainPanelEntityTester) return false

        if (rtPanelEntity != other.rtPanelEntity) return false

        return true
    }

    override fun hashCode(): Int {
        return rtPanelEntity.hashCode()
    }
}
