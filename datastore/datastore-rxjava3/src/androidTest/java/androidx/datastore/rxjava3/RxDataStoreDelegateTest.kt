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

package androidx.datastore.rxjava3

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
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

val Context.rxDataStore by rxDataStore("file1", TestingSerializer())

val Context.rxdsWithMigration by rxDataStore(
    "file2", TestingSerializer(),
    produceMigrations = {
        listOf(
            object : DataMigration<Byte> {
                override suspend fun shouldMigrate(currentData: Byte) = true
                override suspend fun migrate(currentData: Byte) = 123.toByte()
                override suspend fun cleanUp() {}
            }
        )
    }
)

val Context.rxdsWithCorruptionHandler by rxDataStore(
    "file3",
    TestingSerializer(failReadWithCorruptionException = true),
    corruptionHandler = ReplaceFileCorruptionHandler { 123 }
)

val Context.rxDataStoreForFileNameCheck by rxDataStore("file4", TestingSerializer())

@ExperimentalCoroutinesApi
class RxDataStoreDelegateTest {
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
        assertThat(context.rxDataStore.data().blockingFirst()).isEqualTo(0)
        assertThat(context.rxDataStore.updateDataAsync { Single.just(it.inc()) }.blockingGet())
            .isEqualTo(1)
    }

    @Test
    fun testWithMigration() {
        assertThat(context.rxdsWithMigration.data().blockingFirst()).isEqualTo(123)
    }

    @Test
    fun testCorruptedRunsCorruptionHandler() {
        // File needs to exist or we don't actually hit the serializer:
        File(context.filesDir, "datastore/file3").let { file ->
            file.parentFile!!.mkdirs()
            FileOutputStream(file).use {
                it.write(0)
            }
        }

        assertThat(context.rxdsWithCorruptionHandler.data().blockingFirst()).isEqualTo(123)
    }

    @Test
    fun testCorrectFileNameUsed() {
        context.rxDataStoreForFileNameCheck.updateDataAsync { Single.just(it.inc()) }.blockingGet()
        context.rxDataStoreForFileNameCheck.dispose()
        context.rxDataStoreForFileNameCheck.shutdownComplete().blockingAwait()

        assertThat(
            RxDataStoreBuilder(
                { File(context.filesDir, "datastore/file4") }, TestingSerializer()
            ).build().data().blockingFirst()
        ).isEqualTo(1)
    }
}