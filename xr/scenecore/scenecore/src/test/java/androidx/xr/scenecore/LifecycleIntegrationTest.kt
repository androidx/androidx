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

package androidx.xr.scenecore

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RoboConfig

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [RoboConfig.TARGET_SDK])
class LifecycleIntegrationTest {
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.get()
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun sessionDestroy_withSceneAndEntities_shouldNotCrash() {
        activityController.create().start().resume()

        val result = Session.create(context = activity, coroutineContext = testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        val session = (result as SessionCreateSuccess).session

        // Run the dispatcher to allow session to register its lifecycle observer
        testDispatcher.scheduler.runCurrent()

        val entity1 = PanelEntity.create(session, TextView(activity), IntSize2d(100, 100), "test1")
        val entity2 = PanelEntity.create(session, TextView(activity), IntSize2d(100, 100), "test2")
        entity2.parent = entity1

        // Add a component to entity2
        val inputListener = Consumer<InputEvent> {}
        val interactable = InteractableComponent.create(session, directExecutor(), inputListener)
        entity2.addComponent(interactable)

        activityController.pause().stop().destroy()

        // Success is no crash
        assertThat(session.state.value).isNotNull()
    }

    private fun directExecutor() = java.util.concurrent.Executor { it.run() }
}
