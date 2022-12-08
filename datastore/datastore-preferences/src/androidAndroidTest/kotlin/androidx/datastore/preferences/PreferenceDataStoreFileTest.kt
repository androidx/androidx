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

package androidx.datastore.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

public class PreferenceDataStoreFileTest {
    @Test
    fun testPreferencesDataStoreFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.preferencesDataStoreFile("name")

        val expectedFile = File(context.filesDir, "datastore/name.preferences_pb")
        assertEquals(expectedFile.absolutePath, file.absolutePath)
    }
}
