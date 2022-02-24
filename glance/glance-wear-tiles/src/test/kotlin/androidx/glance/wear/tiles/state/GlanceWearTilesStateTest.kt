/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.state.GlanceState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.wear.tiles.GlanceTileService
import androidx.glance.wear.tiles.WearTileId
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceWearTilesStateTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        runBlocking {
            GlanceState.deleteStore(
                context,
                PreferencesGlanceStateDefinition,
                wearTileId.tileServiceClass.name
            )
        }
    }

    @Test
    fun updateWearTileState_performUpdate() {
        val key = intPreferencesKey("int_key")
        val storedPrefs = runBlocking {
            updateWearTileState(context, PreferencesGlanceStateDefinition, wearTileId) { prefs ->
                prefs.toMutablePreferences().apply {
                    set(key, 1)
                }
            }
            getWearTileState<Preferences>(context, PreferencesGlanceStateDefinition, wearTileId)
        }
        assertThat(storedPrefs).isEqualTo(preferencesOf(key to 1))
    }

    private companion object {
        val wearTileId = WearTileId(TestGlanceTileService::class.java)
    }

    private inner class TestGlanceTileService : GlanceTileService() {
        override val stateDefinition = PreferencesGlanceStateDefinition

        @Composable
        override fun Content() {
            Text("Hello World!")
        }
    }
}
