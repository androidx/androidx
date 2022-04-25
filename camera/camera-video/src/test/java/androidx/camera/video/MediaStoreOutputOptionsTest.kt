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

package androidx.camera.video

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MediaStoreOutputOptionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun equals_reflexive() {
        val options = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()

        assertThat(options == options).isTrue()
    }

    @Test
    fun equals_symmetric() {
        val options1 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()
        val options2 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()

        assertThat(options1 == options2 && options2 == options1).isTrue()
    }

    @Test
    fun equals_transitive() {
        val options1 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()
        val options2 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()
        val options3 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()

        assertThat(options1 == options2 && options2 == options3 && options3 == options1).isTrue()
    }

    @Test
    fun equals_consistent() {
        val options1 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()
        val options2 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()
        val options3 = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY)
            .setFileSizeLimit(100)
            .build()

        assertThat(options1 == options2 && options1 == options2).isTrue()
        assertThat(options1 != options3 && options1 != options3).isTrue()
    }

    @Test
    fun equals_nullCheck() {
        val options = MediaStoreOutputOptions.Builder(context.contentResolver, Uri.EMPTY).build()

        assertThat(options.equals(null)).isFalse()
    }
}
