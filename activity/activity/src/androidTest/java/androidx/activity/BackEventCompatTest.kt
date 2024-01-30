/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.activity

import android.window.BackEvent
import android.window.BackEvent.EDGE_LEFT
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BackEventCompatTest {

    @Test
    fun testCreateBackEventCompat() {
        val event = BackEventCompat(1f, 2f, 3f, BackEventCompat.EDGE_LEFT)
        assertThat(event.touchX).isEqualTo(1f)
        assertThat(event.touchY).isEqualTo(2f)
        assertThat(event.progress).isEqualTo(3f)
        assertThat(event.swipeEdge).isEqualTo(BackEventCompat.EDGE_LEFT)
    }

    @RequiresApi(34)
    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun testCreateBackEventCompatFromBackEvent() {
        val event = BackEventCompat(BackEvent(1f, 2f, 3f, EDGE_LEFT))
        assertThat(event.touchX).isEqualTo(1f)
        assertThat(event.touchY).isEqualTo(2f)
        assertThat(event.progress).isEqualTo(3f)
        assertThat(event.swipeEdge).isEqualTo(BackEventCompat.EDGE_LEFT)
    }

    @RequiresApi(34)
    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun testToBackEventFromBackEventCompat() {
        val event = BackEventCompat(1f, 2f, 3f, BackEventCompat.EDGE_LEFT).toBackEvent()
        assertThat(event.touchX).isEqualTo(1f)
        assertThat(event.touchY).isEqualTo(2f)
        assertThat(event.progress).isEqualTo(3f)
        assertThat(event.swipeEdge).isEqualTo(BackEventCompat.EDGE_LEFT)
    }
}
