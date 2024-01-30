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

package androidx.camera.core.processing

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [Edge].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class EdgeTest {

    companion object {
        const val DATA = "data"
    }

    @Test
    fun acceptData_propagatedToListener() {
        // Arrange.
        val edge = Edge<String>()
        var propagatedData: String? = null
        edge.setListener {
            propagatedData = it
        }
        // Act.
        edge.accept(DATA)
        // Assert.
        assertThat(propagatedData).isEqualTo(DATA)
    }

    @Test(expected = NullPointerException::class)
    fun acceptDataWithoutListener_throwsException() {
        Edge<String>().accept(DATA)
    }
}
