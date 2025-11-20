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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.BaseComposeTest
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.makeTestActivityRule
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
    @get:Rule override val activityRule = makeTestActivityRule()

    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun testObservingUiComposition() {
        var handle: CompositionObserverHandle? = null
        compose {
                val view = LocalView.current
                val composition = view.getTag(R.id.wrapped_composition_tag) as Composition
                handle =
                    composition.setObserver(
                        object : CompositionObserver {
                            override fun onBeginComposition(composition: ObservableComposition) {}

                            override fun onEndComposition(composition: ObservableComposition) {}

                            override fun onScopeEnter(scope: RecomposeScope) {}

                            override fun onScopeExit(scope: RecomposeScope) {}

                            override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                            override fun onScopeDisposed(scope: RecomposeScope) {}

                            override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
                        }
                    )
            }
            .then {
                assertNotNull(handle)
                handle?.dispose()
            }
    }
}
