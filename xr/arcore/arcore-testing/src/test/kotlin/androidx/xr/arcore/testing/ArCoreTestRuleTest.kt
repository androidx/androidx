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

package androidx.xr.arcore.testing

import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class ArCoreTestRuleTest {
    @Rule @JvmField val underTest = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        activityController.create().start().resume()

        Session.create(context = activity, coroutineContext = testDispatcher)
    }

    @Test
    fun init_persistedAnchorPoses_isEmpty() {
        assertThat(underTest.persistedAnchorPoses).isEmpty()
    }

    @Test
    fun init_planes_isEmpty() {
        assertThat(underTest.planes).isEmpty()
    }

    @Test
    fun init_augmentableObjects_isEmpty() {
        assertThat(underTest.augmentedObjects.isEmpty())
    }

    @Test
    fun init_otherFaces_isEmpty() {
        assertThat(underTest.faces).isEmpty()
    }

    @Test
    fun persistAnchorPose_addsToMapWithUUID() {
        val pose = Pose()

        val uuid = underTest.persistAnchor(pose)

        assertThat(underTest.persistedAnchorPoses.size).isEqualTo(1)
        assertThat(underTest.persistedAnchorPoses).containsKey(uuid)
        assertThat(underTest.persistedAnchorPoses[uuid]).isEqualTo(pose)
    }
}
