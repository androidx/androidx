/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.startup

import android.content.ContentValues
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@MediumTest
class InitializationProviderTest {

    private lateinit var provider: InitializationProvider

    @Before
    fun setUp() {
        provider = InitializationProvider()
    }

    @Test(expected = IllegalStateException::class)
    fun insertThrowsException() {
        val uri = mock(Uri::class.java)
        val values = ContentValues()
        provider.insert(uri, values)
    }

    @Test(expected = IllegalStateException::class)
    fun updateThrowsException() {
        val uri = mock(Uri::class.java)
        val values = ContentValues()
        provider.update(uri, values, null, null)
    }

    @Test(expected = IllegalStateException::class)
    fun deleteThrowsException() {
        val uri = mock(Uri::class.java)
        provider.delete(uri, null, null)
    }

    @Test(expected = IllegalStateException::class)
    fun getTypeThrowsException() {
        val uri = mock(Uri::class.java)
        provider.getType(uri)
    }
}
