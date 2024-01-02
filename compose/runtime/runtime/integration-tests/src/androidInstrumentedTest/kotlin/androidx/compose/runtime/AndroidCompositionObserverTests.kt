/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.observe
import androidx.compose.ui.R
import androidx.compose.ui.platform.LocalView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidCompositionObserverTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun testObservingUiComposition() {
        var handle: CompositionObserverHandle? = null
        compose {
            val view = LocalView.current
            val composition = view.getTag(R.id.wrapped_composition_tag) as Composition
            handle = composition.observe(object : CompositionObserver {
                override fun onBeginComposition(
                    composition: Composition,
                    invalidationMap: Map<RecomposeScope, Set<Any>?>
                ) {
                }

                override fun onEndComposition(composition: Composition) {
                }
            })
        }.then {
            assertNotNull(handle)
            handle?.dispose()
        }
    }
}
