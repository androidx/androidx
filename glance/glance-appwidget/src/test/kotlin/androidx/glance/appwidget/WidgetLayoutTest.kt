/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.app.Activity
import android.content.Context
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.action.actionLaunchActivity
import androidx.glance.appwidget.proto.LayoutProto
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.wrapContentWidth
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WidgetLayoutTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testLayout() {
        runBlocking {
            val root = runTestingComposition {
                Column {
                    CheckBox(checked = false, modifier = GlanceModifier.fillMaxSize())
                    Button(text = "test", onClick = actionLaunchActivity<Activity>())
                }
            }
            var node = createNode(root)
            assertThat(node.type).isEqualTo(LayoutProto.LayoutType.REMOTE_VIEWS_ROOT)
            assertThat(node.width.hasFillDimension()).isFalse()
            assertThat(node.height.hasFillDimension()).isFalse()
            assertThat(node.childrenCount).isEqualTo(1)

            node = node.childrenList[0]
            assertThat(node.type).isEqualTo(LayoutProto.LayoutType.COLUMN)
            assertThat(node.width.hasFillDimension()).isFalse()
            assertThat(node.height.hasFillDimension()).isFalse()
            assertThat(node.childrenCount).isEqualTo(2)
            assertThat(node.childrenList[0].type).isEqualTo(LayoutProto.LayoutType.CHECK_BOX)
            assertThat(node.childrenList[0].width.hasFillDimension()).isTrue()
            assertThat(node.childrenList[0].height.hasFillDimension()).isTrue()
            assertThat(node.childrenList[1].type).isEqualTo(LayoutProto.LayoutType.BUTTON)
            assertThat(node.childrenList[1].width.hasFillDimension()).isFalse()
            assertThat(node.childrenList[1].height.hasFillDimension()).isFalse()
        }
    }

    @Test
    fun testChange() {
        runBlocking {
            val appId = 999
            val root = runTestingComposition {
                Column {
                    CheckBox(checked = true, modifier = GlanceModifier.fillMaxSize())
                    Button(text = "test", onClick = actionLaunchActivity<Activity>())
                }
            }
            val root2 = runTestingComposition {
                Column {
                    CheckBox(
                        checked = true,
                        modifier = GlanceModifier.wrapContentWidth().fillMaxHeight()
                    )
                    Button(text = "test", onClick = actionLaunchActivity<Activity>())
                }
            }

            assertThat(updateWidgetLayout(context, appId, root)).isTrue()
            assertThat(updateWidgetLayout(context, appId, root)).isFalse()
            assertThat(updateWidgetLayout(context, appId, root2)).isTrue()
        }
    }

    @Test
    fun testNotChange() {
        runBlocking {
            val appId = 787
            val root = runTestingComposition {
                Column {
                    CheckBox(checked = true, modifier = GlanceModifier.fillMaxSize())
                    Button(text = "test", onClick = actionLaunchActivity<Activity>())
                }
            }
            val root2 = runTestingComposition {
                Column {
                    CheckBox(checked = false, modifier = GlanceModifier.fillMaxSize())
                    Button(text = "testtesttest", onClick = actionLaunchActivity<Activity>())
                }
            }

            assertThat(updateWidgetLayout(context, appId, root)).isTrue()
            assertThat(updateWidgetLayout(context, appId, root)).isFalse()
            assertThat(updateWidgetLayout(context, appId, root2)).isFalse()
        }
    }
}