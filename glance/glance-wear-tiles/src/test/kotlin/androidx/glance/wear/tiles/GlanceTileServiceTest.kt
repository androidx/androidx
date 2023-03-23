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

package androidx.glance.wear.tiles

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.wear.tiles.test.R
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.testing.TestTileClient
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Arrays
import kotlin.test.assertIs
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Ignore

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceTileServiceTest {
    private lateinit var executor: InlineExecutorService
    private lateinit var fakeCoroutineScope: TestScope
    private lateinit var tileService: TestGlanceTileService
    private lateinit var tileServiceClient: TestTileClient<GlanceTileService>
    private lateinit var tileServiceWithTimeline: TestGlanceTileServiceWithTimeline
    private lateinit var tileServiceClientWithTimeline: TestTileClient<GlanceTileService>
    private lateinit var tileServiceWithState: TestGlanceTileServiceWithState
    private lateinit var tileServiceClientWithState: TestTileClient<GlanceTileService>
    private lateinit var ovalBitmap: Bitmap
    private var ovalBitmapHashCode: Int = 0

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope(UnconfinedTestDispatcher())

        tileService = TestGlanceTileService()
        tileServiceClient = TestTileClient(
            tileService,
            fakeCoroutineScope,
            fakeCoroutineScope.coroutineContext[CoroutineDispatcher]!!
        )

        tileServiceWithTimeline = TestGlanceTileServiceWithTimeline()
        tileServiceClientWithTimeline = TestTileClient(
            tileServiceWithTimeline,
            fakeCoroutineScope,
            fakeCoroutineScope.coroutineContext[CoroutineDispatcher]!!
        )

        executor = InlineExecutorService()
        tileServiceWithState = TestGlanceTileServiceWithState(fakeCoroutineScope)
        tileServiceClientWithState = TestTileClient(
            tileServiceWithState,
            fakeCoroutineScope,
            fakeCoroutineScope.coroutineContext[CoroutineDispatcher]!!
        )

        ovalBitmap =
            getApplicationContext<Context>()
                .getDrawable(R.drawable.oval)!!
                .toBitmap()
        val buffer = ByteArrayOutputStream().apply {
            ovalBitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }
            .toByteArray()
        ovalBitmapHashCode = Arrays.hashCode(buffer)
    }

    @Test
    fun tileProviderReturnsTile() = fakeCoroutineScope.runTest {
        // Request is currently un-used, provide an empty one.
        val tileRequest = RequestBuilders.TileRequest.Builder().build()

        // Requests need to be split; we need to allow Robolectric to schedule the service calls on
        // the main looper, so we can't just do requestTile().await().
        val tileFuture = tileServiceClient.requestTile(tileRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val tile = tileFuture.await()

        val resourcesIds = arrayOf("android_" + R.drawable.oval)
        val resourcesVersion = Arrays.hashCode(resourcesIds).toString()
        assertThat(tile.resourcesVersion).isEqualTo(resourcesVersion)

        // No freshness interval (for now)
        assertThat(tile.freshnessIntervalMillis).isEqualTo(0)

        assertThat(tile.timeline!!.timelineEntries).hasSize(1)
        val entry = tile.timeline!!.timelineEntries[0]
        assertThat(entry.validity).isNull()

        // It always emits a box as the root-level layout.
        val box = assertIs<LayoutElementBuilders.Box>(entry.layout!!.root!!)
        assertThat(box.contents).hasSize(2)
        val text = assertIs<LayoutElementBuilders.Text>(box.contents[0])

        assertThat(text.text!!.value).isEqualTo("Hello World!")
    }

    @Ignore("resourcesVersion is not matching - b/246239580")
    @Test
    fun tileProviderReturnsTimelineTile() = fakeCoroutineScope.runTest {
        // Request is currently un-used, provide an empty one.
        val tileRequest = RequestBuilders.TileRequest.Builder().build()

        // Requests need to be split; we need to allow Robolectric to schedule the service calls on
        // the main looper, so we can't just do requestTile().await().
        val tileFuture = tileServiceClientWithTimeline.requestTile(tileRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val tile = tileFuture.await()

        val resourcesIds = arrayOf(
            "android_" + R.drawable.ic_launcher_background,
            "android_" + ovalBitmapHashCode
        )

        val resourcesVersion = Arrays.hashCode(resourcesIds).toString()
        assertWithMessage("tile.resourcesVersion")
            .that(tile.resourcesVersion).isEqualTo(resourcesVersion)

        // No freshness interval (for now)
        assertThat(tile.freshnessIntervalMillis).isEqualTo(0)

        assertThat(tile.timeline!!.timelineEntries).hasSize(4)

        checkTimelineEntry(
            tile.timeline!!.timelineEntries[0],
            0,
            Long.MAX_VALUE,
            "No event"
        )

        checkTimelineEntry(
            tile.timeline!!.timelineEntries[1],
            time1.toEpochMilli(),
            time2.toEpochMilli(),
            "Coffee"
        )

        checkTimelineEntry(
            tile.timeline!!.timelineEntries[2],
            time2.toEpochMilli(),
            time3.toEpochMilli(),
            "Work"
        )

        checkTimelineEntry(
            tile.timeline!!.timelineEntries[3],
            time4.toEpochMilli(),
            Long.MAX_VALUE,
            "Dinner"
        )
    }

    @Test
    fun tileProviderReturnsResources() = fakeCoroutineScope.runTest {
        val tileRequest = RequestBuilders.TileRequest.Builder().build()
        val tileFuture = tileServiceClient.requestTile(tileRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val tile = tileFuture.await()

        val resourcesRequest =
            RequestBuilders.ResourcesRequest.Builder()
                .setVersion(tile.resourcesVersion)
                .build()
        val resourcesFuture = tileServiceClient.requestResources(resourcesRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val resources = resourcesFuture.await()

        assertThat(resources.version).isEqualTo(tile.resourcesVersion)
        assertThat(resources.idToImageMapping.size).isEqualTo(1)
        assertThat(resources.idToImageMapping.containsKey("android_" + R.drawable.oval)).isTrue()
    }

    @Test
    fun tileProviderReturnsTimelineResources() = fakeCoroutineScope.runTest {
        val tileRequest = RequestBuilders.TileRequest.Builder().build()
        val tileFuture = tileServiceClientWithTimeline.requestTile(tileRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val tile = tileFuture.await()

        val resourcesRequest =
            RequestBuilders.ResourcesRequest.Builder()
                .setVersion(tile.resourcesVersion)
                .build()

        val resourcesFuture = tileServiceClientWithTimeline.requestResources(resourcesRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val resources = resourcesFuture.await()

        assertThat(resources.version).isEqualTo(tile.resourcesVersion)
        assertThat(resources.idToImageMapping.size).isEqualTo(2)
        assertThat(resources.idToImageMapping.containsKey("android_" + ovalBitmapHashCode)).isTrue()
        assertThat(
            resources.idToImageMapping.containsKey("android_" + R.drawable.ic_launcher_background)
        ).isTrue()
    }

    @Test
    fun tileProviderReturnsTileWithState() = runBlocking {
        tileServiceWithState.updateTileState<Preferences>() { prefs ->
            prefs.toMutablePreferences().apply {
                this[tileServiceWithState.prefsNameKey] = "AndroidX"
            }.toPreferences()
        }

        val tileRequest = RequestBuilders.TileRequest.Builder().build()
        val tileFuture = tileServiceClientWithState.requestTile(tileRequest)

        shadowOf(Looper.getMainLooper()).idle()

        val tile = tileFuture.await()

        assertThat(tile.timeline!!.timelineEntries).hasSize(1)
        val entry = tile.timeline!!.timelineEntries[0]
        val box = assertIs<LayoutElementBuilders.Box>(entry.layout!!.root!!)
        val text = assertIs<LayoutElementBuilders.Text>(box.contents[0])
        assertThat(text.text!!.value).isEqualTo("Hello AndroidX")
    }

    @Test
    fun tileStateDefinition() = runBlocking {
        tileServiceWithState.updateTileState<Preferences> { prefs ->
            prefs.toMutablePreferences().apply {
                this[tileServiceWithState.prefsNameKey] = "AndroidX"
            }.toPreferences()
        }

        var store = tileServiceWithState.getTileState<Preferences>()

        assertThat(store).isNotNull()
        assertThat(store[tileServiceWithState.prefsNameKey]).isEqualTo("AndroidX")

        tileServiceWithState.updateTileState<Preferences> { prefs ->
            prefs.toMutablePreferences().apply {
                this[tileServiceWithState.prefsNameKey] =
                    this[tileServiceWithState.prefsNameKey] + " Glance"
            }.toPreferences()
        }

        store = tileServiceWithState.getTileState()

        assertThat(store).isNotNull()
        assertThat(store[tileServiceWithState.prefsNameKey]).isEqualTo("AndroidX Glance")
    }

    private fun checkTimelineEntry(
        entry: TimelineBuilders.TimelineEntry,
        startMillis: Long,
        endMillis: Long,
        textValue: String
    ) {
        assertThat(entry.validity!!.startMillis).isEqualTo(startMillis)
        assertThat(entry.validity!!.endMillis).isEqualTo(endMillis)
        var box = assertIs<LayoutElementBuilders.Box>(entry.layout!!.root!!)
        var text = assertIs<LayoutElementBuilders.Text>(box.contents[0])
        assertThat(text.text!!.value).isEqualTo(textValue)
    }

    private inner class TestGlanceTileService : GlanceTileService() {
        @Composable
        override fun Content() {
            Text("Hello World!")
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "Oval",
                modifier = GlanceModifier.size(40.dp),
                contentScale = ContentScale.FillBounds
            )
        }
    }

    private inner class TestGlanceTileServiceWithTimeline : GlanceTileService() {
        override val timelineMode = testTimelineMode

        @Composable
        override fun Content() {
            when (LocalTimeInterval.current) {
                testTimelineMode.timeIntervals.elementAt(0) -> {
                    Text("No event")
                }

                testTimelineMode.timeIntervals.elementAt(1) -> {
                    Text("Coffee")
                    Image(
                        provider = ImageProvider(ovalBitmap),
                        contentDescription = "Oval",
                        modifier = GlanceModifier.size(40.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }

                testTimelineMode.timeIntervals.elementAt(2) -> {
                    Text("Work")
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_background),
                        contentDescription = "Icon",
                        modifier = GlanceModifier.size(40.dp),
                    )
                }

                testTimelineMode.timeIntervals.elementAt(3) -> {
                    Text("Dinner")
                }
            }
        }
    }

    private inner class TestGlanceTileServiceWithState(scope: CoroutineScope) :
        GlanceTileService() {
        override val stateDefinition = PreferencesGlanceStateDefinition

        init {
            stateDefinition.setCoroutineScope(scope)
        }

        val prefsNameKey = stringPreferencesKey("user_name")

        @Composable
        override fun Content() {
            val prefs = currentState<Preferences>()
            val userName = prefs[prefsNameKey] ?: "World"
            Text("Hello $userName")
        }
    }

    private companion object {
        private val time1 = Instant.parse("2021-11-12T13:15:30.00Z")
        private val time2 = Instant.parse("2021-11-12T13:45:30.00Z")
        private val time3 = Instant.parse("2021-11-12T17:45:30.00Z")
        private val time4 = Instant.parse("2021-11-12T18:30:30.00Z")
        val testTimelineMode = TimelineMode.TimeBoundEntries(
            setOf(
                TimeInterval(),
                TimeInterval(time1, time2),
                TimeInterval(time2, time3),
                TimeInterval(time4)
            )
        )
    }
}