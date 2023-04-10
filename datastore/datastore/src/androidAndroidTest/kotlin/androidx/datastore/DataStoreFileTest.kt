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

package androidx.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class DataStoreFileTest {
    @Test
    fun testDataStoreFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.dataStoreFile("name")

        val expectedFile = File(context.filesDir, "datastore/name")
        assertThat(file.absolutePath).isEqualTo(expectedFile.absolutePath)
    }
}