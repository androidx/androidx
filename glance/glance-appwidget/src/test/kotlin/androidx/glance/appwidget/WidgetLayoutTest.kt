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
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionLaunchActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.test.R
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.appwidget.proto.LayoutProto
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WidgetLayoutTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun testLayout() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column(horizontalAlignment = Alignment.End) {
                CheckBox(
                    checked = false,
                    onCheckedChange = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
                Button(text = "test", onClick = actionLaunchActivity<Activity>())
                Image(
                    ImageProvider(R.drawable.oval),
                    "description",
                    modifier = GlanceModifier.width(12.dp).defaultWeight(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        val node = createNode(context, root)
        assertThat(node.type).isEqualTo(LayoutProto.LayoutType.REMOTE_VIEWS_ROOT)
        assertThat(node.width).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(node.height).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(node.childrenCount).isEqualTo(1)

        val column = node.childrenList[0]
        assertThat(column.type).isEqualTo(LayoutProto.LayoutType.COLUMN)
        assertThat(column.width).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(column.height).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(column.horizontalAlignment).isEqualTo(LayoutProto.HorizontalAlignment.END)

        val (checkBox, button, image) = column.childrenList
        assertThat(checkBox.type).isEqualTo(LayoutProto.LayoutType.CHECK_BOX)
        assertThat(checkBox.width).isEqualTo(LayoutProto.DimensionType.FILL)
        assertThat(checkBox.height).isEqualTo(LayoutProto.DimensionType.FILL)
        assertThat(button.type).isEqualTo(LayoutProto.LayoutType.BUTTON)
        assertThat(button.width).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(button.height).isEqualTo(LayoutProto.DimensionType.WRAP)
        assertThat(image.type).isEqualTo(LayoutProto.LayoutType.IMAGE)
        assertThat(image.width).isEqualTo(LayoutProto.DimensionType.EXACT)
        assertThat(image.height).isEqualTo(LayoutProto.DimensionType.EXPAND)
        assertThat(image.imageScale).isEqualTo(LayoutProto.ContentScale.CROP)
    }

    @Test
    fun testChange_size() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column {
                CheckBox(
                    checked = true,
onCheckedChange = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
                Button(text = "test", onClick = actionLaunchActivity<Activity>())
            }
        }
        val root2 = runTestingComposition {
            Column {
                CheckBox(
                    checked = true,
                    onCheckedChange = null,
                    modifier = GlanceModifier.wrapContentWidth().fillMaxHeight()
                )
                Button(text = "test", onClick = actionLaunchActivity<Activity>())
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testChange_imageScale() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Fit)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testNotChange_imageDescription() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "other", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(0)
    }

    @Test
    fun testChange_columnAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Column(horizontalAlignment = Alignment.Start) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testNotChange_columnAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Column(verticalAlignment = Alignment.Top) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(0)
    }

    @Test
    fun testChange_rowAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Row(verticalAlignment = Alignment.Top) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testNotChange_rowAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Row(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Row(horizontalAlignment = Alignment.Start) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(0)
    }

    @Test
    fun testChange_boxHorizontalAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Box(contentAlignment = Alignment.Center) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Box(contentAlignment = Alignment.TopCenter) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testChange_boxVerticalAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Box(contentAlignment = Alignment.Center) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Box(contentAlignment = Alignment.CenterStart) {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testChange_lazyColumnAlignment() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            LazyColumn(horizontalAlignment = Alignment.Start) {
                item {
                    Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
                }
            }
        }
        val root2 = runTestingComposition {
            LazyColumn(horizontalAlignment = Alignment.End) {
                item {
                    Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
                }
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(1)
    }

    @Test
    fun testNotChange_lazyColumnAlignmentContent() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            LazyColumn(horizontalAlignment = Alignment.Start) {
                item {
                    Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
                }
            }
        }
        val root2 = runTestingComposition {
            LazyColumn(horizontalAlignment = Alignment.Start) {
                item {
                    Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Fit)
                }
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(0)
    }

    @Test
    fun testNotChange() = fakeCoroutineScope.runBlockingTest {
        val appId = 787
        val root = runTestingComposition {
            Column {
                CheckBox(
                    checked = true,
                    onCheckedChange = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
                Button(text = "test", onClick = actionLaunchActivity<Activity>())
            }
        }
        val root2 = runTestingComposition {
            Column {
                CheckBox(
                    checked = false,
                    onCheckedChange = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
                Button(text = "testtesttest", onClick = actionLaunchActivity<Activity>())
            }
        }

        val layoutConfig = LayoutConfiguration.create(context, appId)
        assertThat(layoutConfig.addLayout(root)).isEqualTo(0)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(0)
    }

    @Test
    fun widgetAllocation_dontReuse() = fakeCoroutineScope.runBlockingTest {
        val appId = 999
        val root = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Crop)
            }
        }
        val root2 = runTestingComposition {
            Column {
                Image(ImageProvider(R.drawable.oval), "test", contentScale = ContentScale.Fit)
            }
        }

        val layoutConfig = LayoutConfiguration.create(
            context,
            appWidgetId = appId,
            nextIndex = TopLevelLayoutsCount - 1,
            existingLayoutIds = listOf(0, 1, 2)
        )
        assertThat(layoutConfig.addLayout(root)).isEqualTo(TopLevelLayoutsCount - 1)
        assertThat(layoutConfig.addLayout(root2)).isEqualTo(3)
    }
}
