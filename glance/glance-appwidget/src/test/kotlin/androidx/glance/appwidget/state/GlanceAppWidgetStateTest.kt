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

package androidx.glance.appwidget.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.createUniqueRemoteUiName
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceAppWidgetStateTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        runBlocking {
            GlanceState.deleteStore(
                context,
                PreferencesGlanceStateDefinition,
                createUniqueRemoteUiName(appWidgetId.appWidgetId)
            )
        }
    }

    @Test
    fun getAppWidgetState_noStateDefine_shouldFail() {
        val appWidget = GlanceAppWidgetProviderWithoutState()
        val ex = runBlocking {
            assertFailsWith<IllegalStateException> {
                appWidget.getAppWidgetState(context, appWidgetId)
            }
        }
        assertThat(ex.message).isEqualTo("No state defined in this provider")
    }

    @Test
    fun getAppWidgetState_default_shouldReturnEmptyState() {
        val appWidget = GlanceAppWidgetProviderPreferencesState()
        val result = runBlocking {
            appWidget.getAppWidgetState<Preferences>(context, appWidgetId)
        }
        assertThat(result).isEqualTo(emptyPreferences())
    }

    @Test
    fun updateAppWidgetState_performUpdate() {
        val key = intPreferencesKey("int_key")
        val appWidget = GlanceAppWidgetProviderPreferencesState()
        val storedPrefs = runBlocking {
            updateAppWidgetState(context, appWidgetId) {
                it[key] = 1
            }
            appWidget.getAppWidgetState<Preferences>(context, appWidgetId)
        }
        assertThat(storedPrefs).isEqualTo(preferencesOf(key to 1))
    }

    class GlanceAppWidgetProviderWithoutState : GlanceAppWidget() {
        override val stateDefinition: GlanceStateDefinition<*>? = null

        @Composable
        override fun Content() {
        }
    }

    class GlanceAppWidgetProviderPreferencesState : GlanceAppWidget() {
        @Composable
        override fun Content() {
        }
    }

    private companion object {
        val appWidgetId = AppWidgetId(1)
    }
}
