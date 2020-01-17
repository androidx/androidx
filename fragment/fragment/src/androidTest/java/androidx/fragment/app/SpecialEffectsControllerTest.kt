/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.fragment.app

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SpecialEffectsControllerTest {
    @Test
    fun factoryCreateController() {
        val map = mutableMapOf<ViewGroup, TestSpecialEffectsController>()
        val factory = SpecialEffectsControllerFactory { container ->
            TestSpecialEffectsController(container).also {
                map[container] = it
            }
        }
        val container = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val controller = factory.createController(container)
        assertThat(controller)
            .isEqualTo(map[container])
        assertThat(controller.container)
            .isEqualTo(container)

        // Ensure that a new container gets a new controller
        val secondContainer = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val secondController = factory.createController(secondContainer)
        assertThat(secondController)
            .isEqualTo(map[secondContainer])
        assertThat(secondController)
            .isNotEqualTo(controller)
    }

    @Test
    fun getOrCreateController() {
        var count = 0
        val map = mutableMapOf<ViewGroup, TestSpecialEffectsController>()
        val factory = SpecialEffectsControllerFactory { container ->
            count++
            TestSpecialEffectsController(container).also {
                map[container] = it
            }
        }
        val container = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val controller = SpecialEffectsController.getOrCreateController(container, factory)
        assertThat(controller)
            .isEqualTo(map[container])
        assertThat(controller.container)
            .isEqualTo(container)
        assertThat(count)
            .isEqualTo(1)

        // Recreating the controller shouldn't cause the count to increase
        val recreatedController = SpecialEffectsController.getOrCreateController(
            container, factory)
        assertThat(recreatedController)
            .isEqualTo(controller)
        assertThat(recreatedController.container)
            .isEqualTo(container)
        assertThat(count)
            .isEqualTo(1)

        // But creating a controller for a different view returns a new instance
        val secondContainer = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val secondController = SpecialEffectsController.getOrCreateController(
            secondContainer, factory)
        assertThat(secondController)
            .isEqualTo(map[secondContainer])
        assertThat(secondController.container)
            .isEqualTo(secondContainer)
        assertThat(count)
            .isEqualTo(2)
    }
}

internal class TestSpecialEffectsController(
    container: ViewGroup
) : SpecialEffectsController(container)
