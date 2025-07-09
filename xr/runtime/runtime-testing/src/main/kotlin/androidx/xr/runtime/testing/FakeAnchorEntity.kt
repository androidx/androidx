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

package androidx.xr.runtime.testing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorEntity
import androidx.xr.runtime.internal.AnchorEntity.OnStateChangedListener
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PlaneSemantic
import androidx.xr.runtime.internal.PlaneType
import androidx.xr.runtime.math.Pose
import java.time.Duration
import java.util.UUID

/** Test-only implementation of [AnchorEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(Build.VERSION_CODES.O)
public class FakeAnchorEntity(
    /**
     * The configuration data used to create this [FakeAnchorEntity]. This field is currently used
     * by [FakeJxrPlatformAdapter.createAnchorEntity] only.
     *
     * In tests, this property can be inspected to verify that the anchor was instantiated with the
     * correct parameters, such as bounds or plane type. It can also be modified to simulate
     * different anchor configurations.
     *
     * @see AnchorCreationData
     */
    internal val anchorCreationData: AnchorCreationData = AnchorCreationData(),

    /**
     * The underlying [Anchor] instance that this fake entity represents.
     *
     * In tests, this property can be accessed to inspect or modify the state of the underlying
     * (fake) anchor, such as its pose or tracking state. It is initialized by default with a
     * [FakeRuntimeAnchor] instance.
     *
     * @see Anchor
     * @see FakeRuntimeAnchor
     */
    internal val anchor: Anchor = FakeRuntimeAnchor(Pose(), FakeRuntimePlane()),
) : FakeSystemSpaceEntity(), AnchorEntity {

    /**
     * A data class holding the configuration parameters for a [FakeAnchorEntity].
     *
     * This class is used within tests to specify the initial properties of a fake anchor, such as
     * its dimensions, plane classification, and search timeout. It allows for the creation of
     * test-specific anchor configurations and enables verification of these parameters after
     * instantiation.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public data class AnchorCreationData(
        val bounds: Dimensions = Dimensions(1f, 1f, 1f),
        val planeType: PlaneType = PlaneType.ANY,
        val planeSemantic: PlaneSemantic = PlaneSemantic.ANY,
        val searchTimeout: Duration = Duration.ZERO,
        val uuid: UUID = UUID.randomUUID(),
    )

    private var onStateChangedListener: OnStateChangedListener =
        OnStateChangedListener { newState ->
            _state = newState
        }

    private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED

    /** The current state of the anchor. */
    override val state: @AnchorEntity.State Int
        get() = _state

    /** Registers a listener to be called when the state of the anchor changes. */
    @Suppress("ExecutorRegistration")
    override fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener
    }

    /** Returns the native pointer of the anchor. */
    // TODO(b/373711152) : Remove this property once the Jetpack XR Runtime API migration is done.
    override val nativePointer: Long
        get() = 0L

    /**
     * Test function to invoke the onStateChanged listener callback.
     *
     * This function is used to simulate the update of the underlying [AnchorEntity.State],
     * triggering the registered listener. In tests, you can call this function to manually trigger
     * the listener and verify that your code responds correctly to state updates.
     */
    public fun onStateChanged(newState: @AnchorEntity.State Int) {
        onStateChangedListener.onStateChanged(newState)
    }
}
