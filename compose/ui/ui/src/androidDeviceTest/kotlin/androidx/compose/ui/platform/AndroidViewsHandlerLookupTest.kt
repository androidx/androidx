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

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidViewsHandlerLookupTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun semanticsIdToView_resolvesViewAndRemovesWhenDetached() {
        var androidComposeView: AndroidComposeView? = null
        var includeAndroidView by mutableStateOf(true)
        var capturedSemanticsId = -1

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            Box(Modifier.size(100.dp)) {
                if (includeAndroidView) {
                    AndroidView(factory = { View(it) }, modifier = Modifier.size(50.dp))
                }
            }
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            val handler = view.androidViewsHandler!!
            val entry = handler.layoutNodeToHolder.entries.first()
            capturedSemanticsId = entry.key.semanticsId
            val expectedViewHolder = entry.value

            assertThat(view.semanticsIdToView(capturedSemanticsId))
                .isSameInstanceAs(expectedViewHolder)
            assertThat(view.semanticsIdToView(-9999)).isNull()

            includeAndroidView = false
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            assertThat(view.semanticsIdToView(capturedSemanticsId)).isNull()
        }
    }

    @Test
    fun semanticsIdToView_updatesMappingAcrossLayoutNodeOnReuse() {
        var androidComposeView: AndroidComposeView? = null
        var reuseKey by mutableIntStateOf(0)

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            ReusableContent(key = reuseKey) {
                AndroidView(
                    factory = { View(it) },
                    onReset = {},
                    modifier = Modifier.size(50.dp),
                )
            }
        }

        var oldSemanticsId = -1
        var expectedViewHolder: View? = null

        rule.runOnIdle {
            val view = androidComposeView!!
            val entry = view.androidViewsHandler!!.layoutNodeToHolder.entries.first()
            oldSemanticsId = entry.key.semanticsId
            expectedViewHolder = entry.value

            assertThat(view.semanticsIdToView(oldSemanticsId)).isSameInstanceAs(expectedViewHolder)

            // Trigger LayoutNode.onReuse(), which assigns a new semanticsId without calling
            // addAndroidView again.
            reuseKey = 1
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            val entry = view.androidViewsHandler!!.layoutNodeToHolder.entries.first()
            val newSemanticsId = entry.key.semanticsId

            assertThat(newSemanticsId).isNotEqualTo(oldSemanticsId)
            assertThat(view.semanticsIdToView(oldSemanticsId)).isNull()
            assertThat(view.semanticsIdToView(newSemanticsId)).isSameInstanceAs(expectedViewHolder)
        }
    }

    @Test
    fun semanticsIdToView_returnsNullWhenLayoutNodeIsDeactivated() {
        var androidComposeView: AndroidComposeView? = null
        var active by mutableStateOf(true)
        var capturedSemanticsId = -1
        lateinit var capturedNode: LayoutNode
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            SubcomposeLayout(state, Modifier.size(100.dp)) { constraints ->
                val placeables =
                    if (active) {
                        subcompose("slot") {
                                AndroidView(
                                    factory = { View(it) },
                                    onReset = {},
                                    modifier = Modifier.size(50.dp),
                                )
                            }
                            .map { it.measure(constraints) }
                    } else {
                        emptyList()
                    }
                layout(100, 100) { placeables.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            val entry = view.androidViewsHandler!!.layoutNodeToHolder.entries.first()
            capturedNode = entry.key
            capturedSemanticsId = capturedNode.semanticsId
            assertThat(view.semanticsIdToView(capturedSemanticsId)).isNotNull()

            // Deactivating the SubcomposeLayout slot calls LayoutNode.onDeactivate(), keeping the
            // LayoutNode attached in the reuse pool while removing the inner View.
            active = false
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            assertThat(capturedNode.isAttached).isTrue()
            assertThat(capturedNode.isDeactivated).isTrue()
            assertThat(view.androidViewsHandler!!.layoutNodeToHolder).containsKey(capturedNode)
            assertThat(view.semanticsIdToView(capturedSemanticsId)).isNull()
        }
    }
}
