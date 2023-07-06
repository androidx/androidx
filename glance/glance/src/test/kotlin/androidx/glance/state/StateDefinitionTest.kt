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

package androidx.glance.state

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StateDefinitionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val counterKey = intPreferencesKey("counter")
    private val stringKey = stringPreferencesKey("string")

    @Test
    fun emptyStore() {
        runBlocking {
            val uiString = "testUiString1"
            val store = assertNotNull(
                GlanceState.getValue(
                    context,
                    PreferencesGlanceStateDefinition,
                    uiString
                )
            )
            assertThat(store.contains(counterKey)).isFalse()
            assertThat(store.contains(stringKey)).isFalse()
        }
    }

    @Test
    fun counterKey() {
        runBlocking {
            val uiString = "testUiString2"
            GlanceState.updateValue(context, PreferencesGlanceStateDefinition, uiString) { prefs ->
                prefs.toMutablePreferences().apply { this[counterKey] = 0 }.toPreferences()
            }

            var store = assertNotNull(
                GlanceState.getValue(
                    context,
                    PreferencesGlanceStateDefinition,
                    uiString
                )
            )
            assertThat(store.contains(counterKey)).isTrue()
            assertThat(store[counterKey]).isEqualTo(0)

            GlanceState.updateValue(context, PreferencesGlanceStateDefinition, uiString) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[counterKey] = prefs[counterKey]!! + 1
                }.toPreferences()
            }
            store = assertNotNull(
                GlanceState.getValue(
                    context,
                    PreferencesGlanceStateDefinition,
                    uiString
                )
            )
            assertThat(store.contains(counterKey)).isTrue()
            assertThat(store[counterKey]).isEqualTo(1)
        }
    }

    @Test
    fun stringKey() {
        val uiString = "testUiString3"
        val storedMessage1 = "Test string"
        val storedMessage2 = "Another test string"

        runBlocking {
            GlanceState.updateValue(context, PreferencesGlanceStateDefinition, uiString) { prefs ->
                prefs.toMutablePreferences().apply { this[stringKey] = storedMessage1 }
                    .toPreferences()
            }

            var store = assertNotNull(
                GlanceState.getValue(
                    context,
                    PreferencesGlanceStateDefinition,
                    uiString
                )
            )
            assertThat(store.contains(counterKey)).isFalse()
            assertThat(store.contains(stringKey)).isTrue()
            assertThat(store[stringKey]).isEqualTo(storedMessage1)

            GlanceState.updateValue(context, PreferencesGlanceStateDefinition, uiString) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[stringKey] = storedMessage2
                }.toPreferences()
            }
            store = assertNotNull(
                GlanceState.getValue(
                    context,
                    PreferencesGlanceStateDefinition,
                    uiString
                )
            )
            assertThat(store.contains(counterKey)).isFalse()
            assertThat(store.contains(stringKey)).isTrue()
            assertThat(store[stringKey]).isEqualTo(storedMessage2)
        }
    }
}
