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

package androidx.datastore.preferences.rxjava3

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

val intKey = intPreferencesKey("int_key")

val Context.rxDataStore by rxPreferencesDataStore("file1")

val Context.rxdsWithMigration by rxPreferencesDataStore(
    "file2",
    produceMigrations = {
        listOf(
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences) = true
                override suspend fun migrate(currentData: Preferences) =
                    preferencesOf(intKey to 123)

                override suspend fun cleanUp() {}
            }
        )
    }
)

val Context.rxdsWithCorruptionHandler by rxPreferencesDataStore(
    "file3",
    corruptionHandler = ReplaceFileCorruptionHandler { preferencesOf(intKey to 123) }
)

val Context.rxDataStoreForFileNameCheck by rxPreferencesDataStore("file5")

@ExperimentalCoroutinesApi
class RxPreferenceDataStoreDelegateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "/datastore").deleteRecursively()
    }

    @Test
    fun testBasic() {
        assertThat(context.rxDataStore.data().blockingFirst()).isEqualTo(preferencesOf())
        assertThat(
            context.rxDataStore.updateDataAsync {
                Single.just(mutablePreferencesOf(intKey to 123))
            }.blockingGet()
        ).isEqualTo(mutablePreferencesOf(intKey to 123))
    }

    @Test
    fun testWithMigration() {
        assertThat(
            context.rxdsWithMigration.data().blockingFirst()
        ).isEqualTo(preferencesOf(intKey to 123))
    }

    @Test
    fun testCorruptedRunsCorruptionHandler() {
        // File needs to exist or we don't actually hit the serializer:
        File(context.filesDir, "datastore/file3.preferences_pb").let { file ->
            file.parentFile!!.mkdirs()
            FileOutputStream(file).use {
                it.write(0) // 0 first byte isn't a valid proto
            }
        }

        assertThat(
            context.rxdsWithCorruptionHandler.data().blockingFirst()
        ).isEqualTo(preferencesOf(intKey to 123))
    }

    @Test
    fun testCorrectFileNameUsed() {
        context.rxDataStoreForFileNameCheck.updateDataAsync {
            Single.just(preferencesOf(intKey to 99))
        }.blockingGet()

        context.rxDataStoreForFileNameCheck.dispose()
        context.rxDataStoreForFileNameCheck.shutdownComplete().blockingAwait()

        assertThat(
            RxPreferenceDataStoreBuilder {
                File(context.filesDir, "datastore/file5.preferences_pb")
            }.build().data().blockingFirst()
        ).isEqualTo(preferencesOf(intKey to 99))
    }
}