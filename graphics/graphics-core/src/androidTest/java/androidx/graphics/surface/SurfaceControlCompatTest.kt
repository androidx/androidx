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

package androidx.graphics.surface

import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(Build.VERSION_CODES.Q)
@SdkSuppress(minSdkVersion = 29)
class SurfaceControlCompatTest {

    @Test
    fun testCreateFromWindow() {
        var surfaceControl = SurfaceControl.Builder()
            .setName("SurfaceControlCompact_createFromWindow")
            .build()
        try {
            SurfaceControlCompat(Surface(surfaceControl), "SurfaceControlCompatTest")
        } catch (e: IllegalArgumentException) {
            fail()
        }
    }

    @Ignore("broken: b/216102328")
    @Test
    fun testSurfaceTransactionCreate() {
        try {
            SurfaceControlCompat.Transaction()
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        }
    }
}