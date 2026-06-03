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
package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric

@RunWith(ParameterizedRobolectricTestRunner::class)
class SpaceEntityTest(private val testSpaceType: Class<out SpaceEntity>) {

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var anchorSpace: AnchorSpace
    private lateinit var entity: Entity
    private lateinit var testSpace: SpaceEntity

    companion object {
        /** Creates and returns a list of EntitySpaces values. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: testSpaceType={0}")
        fun spaces(): List<Any> {
            return listOf(ActivitySpace::class.java, AnchorSpace::class.java)
        }
    }

    @Before
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher, activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        session.configure(
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .build()
        )

        activitySpace = session.scene.activitySpace
        anchorSpace =
            AnchorSpace.create(
                session,
                FloatSize2d(),
                PlaneOrientation.ALL,
                PlaneSemanticType.ALL,
                10.seconds.toJavaDuration(),
            )

        entity = Entity.create(session, "test")

        testSpace = getTestSpace(testSpaceType)
    }

    private fun getTestSpace(clazz: Class<out SpaceEntity>): SpaceEntity {
        return when (clazz) {
            ActivitySpace::class.java -> activitySpace
            AnchorSpace::class.java -> anchorSpace
            else -> throw IllegalArgumentException("Invalid test space type: $clazz.")
        }
    }

    @Test
    fun settingParent_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) { testSpace.parent = entity }
    }

    @Test
    fun setPose_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) { testSpace.setPose(Pose()) }
    }

    @Test
    fun getPose_relativeToActivitySpace_success() {
        assertThat(testSpace.getPose(Space.ACTIVITY)).isInstanceOf(Pose::class.java)
    }

    @Test
    fun getPose_relativeToParent_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { testSpace.getPose(Space.PARENT) }
    }

    @Test
    fun setScale_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) { testSpace.setScale(1f) }
    }

    @Test
    fun setScale_Vector3_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) { testSpace.setScale(Vector3()) }
    }

    @Test
    fun getScale_relativeToActivitySpace_success() {
        assertThat(testSpace.getScale(Space.ACTIVITY)).isNotNull()
    }

    @Test
    fun getScale_relativeToParent_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { testSpace.getScale(Space.PARENT) }
    }

    @Test
    fun getNonUniformScale_relativeToActivitySpace_success() {
        assertThat(testSpace.getNonUniformScale(Space.ACTIVITY)).isInstanceOf(Vector3::class.java)
    }

    @Test
    fun getNonUniformScale_relativeToParent_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            testSpace.getNonUniformScale(Space.PARENT)
        }
    }
}
