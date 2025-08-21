/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation

// import dev.romainguy.graphics.path.iterator as PWiterator
import android.graphics.Path
import android.graphics.PathIterator
import android.os.Build
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.graphics.path.iterator
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Test

class PathTest {

    val platform = AndroidxPlatformServices()

    val safePath =
        Path().apply {
            reset()
            moveTo(100f, 200f)
            quadTo(10f, 300f, 600f, 300f)
            lineTo(2f, 600f)
            close()
        }

    val remoteComposePathWithNan =
        Path().apply {
            reset()
            moveTo(100f, 200f)
            quadTo(10f, 300f, 600f, 300f)
            lineTo(2f, RemoteContext.FLOAT_WINDOW_WIDTH)
            close()
        }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun PlatformSafe() {
        Assume.assumeTrue("Path.pathIterator from API 34", Build.VERSION.SDK_INT >= 34)

        val i = safePath.pathIterator

        assertEquals(PathIterator.VERB_MOVE, i.peek())

        for (seg in i) {
            println(seg.verb)
        }
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun PlatformNan() {
        Assume.assumeTrue("Path.pathIterator from API 34", Build.VERSION.SDK_INT >= 34)

        // TODO b/372206123 Find solution for Pathway on API 35, and Androidx Path Iterator with NaN
        Assume.assumeTrue(
            "Path.pathIterator fails on NaN from API 34 b/372206123",
            Build.VERSION.SDK_INT < 34,
        )

        val i = remoteComposePathWithNan.pathIterator

        assertEquals(PathIterator.VERB_MOVE, i.peek())

        for (seg in i) {
            println(seg.verb)
        }
    }

    @Test
    fun AndroidxSafe() {
        val i = safePath.iterator()

        assertEquals(4, i.calculateSize())
        assertEquals("Move", i.peek().name)

        for (seg in i) {
            println(seg.type)
        }
    }

    @Test
    fun AndroidxNan() {
        // TODO b/372206123 Find solution for Pathway on API 35, and Androidx Path Iterator with NaN
        Assume.assumeTrue(
            "Path.iterator fails on NaN from API 34 b/372206123",
            Build.VERSION.SDK_INT < 34,
        )

        val i = remoteComposePathWithNan.iterator()

        assertEquals(4, i.calculateSize())
        assertEquals("Move", i.peek().name)

        for (seg in i) {
            println(seg.type)
        }
    }

    //    @Test
    //    fun PathwaySafe() {
    //        // TODO b/372206123 Find solution for Pathway on API 35, and Androidx Path Iterator
    // with NaN
    //        Assume.assumeTrue(
    //            "Pathway iterator fails from API 35 b/372206123",
    //            Build.VERSION.SDK_INT < 35,
    //        )
    //
    //        val i = safePath.PWiterator()
    //
    //        assertEquals(4, i.size())
    //        assertEquals("Move", i.peek().name)
    //
    //        for (seg in i) {
    //            println(seg.type)
    //        }
    //    }
    //
    //    @Test
    //    fun PathwayNan() {
    //        // TODO b/372206123 Find solution for Pathway on API 35, and Androidx Path Iterator
    // with NaN
    //        Assume.assumeTrue(
    //            "Pathway iterator fails from API 35 b/372206123",
    //            Build.VERSION.SDK_INT < 35,
    //        )
    //
    //        val i = remoteComposePathWithNan.PWiterator()
    //
    //        assertEquals(4, i.size())
    //        assertEquals("Move", i.peek().name)
    //
    //        for (seg in i) {
    //            println(seg.type)
    //        }
    //    }
}
