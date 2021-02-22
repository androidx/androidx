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
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@MediumTest
class DefaultSpecialEffectsControllerTest {
    @Test
    fun fragmentManagerGetSetSpecialEffectsController() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val factory = SpecialEffectsControllerFactory {
                mock(SpecialEffectsController::class.java)
            }
            fm.specialEffectsControllerFactory = factory
            assertThat(fm.specialEffectsControllerFactory)
                .isEqualTo(factory)
        }
    }

    /**
     * Ensure that FragmentManager returns [DefaultSpecialEffectsController] implementations
     * from its default factory
     */
    @Test
    fun fragmentManagerDefaultFactory() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            val factory = fm.specialEffectsControllerFactory
            val controller = factory.createController(container)
            assertThat(controller)
                .isInstanceOf(DefaultSpecialEffectsController::class.java)
        }
    }

    /**
     * Ensure that FragmentManager returns [DefaultSpecialEffectsController] implementations
     * from its default factory when using [SpecialEffectsController.getOrCreateController].
     */
    @Test
    fun fragmentManagerGetOrCreateController() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val controller = SpecialEffectsController.getOrCreateController(container, fm)
            assertThat(controller)
                .isInstanceOf(DefaultSpecialEffectsController::class.java)
        }
    }
}
