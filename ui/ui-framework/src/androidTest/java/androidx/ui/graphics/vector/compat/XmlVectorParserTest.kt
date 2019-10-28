/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector.compat

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Px

import androidx.ui.graphics.vector.VectorPath
import androidx.ui.framework.test.R
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.vector.PathCommand
import androidx.ui.graphics.vector.PathNode
import androidx.ui.res.loadVectorResource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class XmlVectorParserTest {

    @Test
    fun testParseXml() {
        val res = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val asset = loadVectorResource(
            null,
            res,
            R.drawable.test_compose_vector
        )
        val density = res.displayMetrics.density
        val expectedSize = Px(density*24)
        assertEquals(expectedSize, asset.defaultWidth)
        assertEquals(expectedSize, asset.defaultHeight)
        assertEquals(24.0f, asset.viewportWidth)
        assertEquals(24.0f, asset.viewportHeight)
        assertEquals(1, asset.root.size)

        val node = asset.root.iterator().next() as VectorPath
        assertEquals(Color(0xFFFF0000), (node.fill as SolidColor).value)

        val path = node.pathData
        assertEquals(3, path.size)
        assertEquals(PathCommand.MoveTo, path.get(0).command)
        assertEquals(20.0f, path.get(0).args[0])
        assertEquals(10.0f, path.get(0).args[1])

        assertEquals(PathCommand.RelativeLineTo, path.get(1).command)
        assertEquals(6, path.get(1).args.size)
        assertEquals(10.0f, path.get(1).args[0])
        assertEquals(0.0f, path.get(1).args[1])
        assertEquals(0.0f, path.get(1).args[2])
        assertEquals(10.0f, path.get(1).args[3])
        assertEquals(-10.0f, path.get(1).args[4])
        assertEquals(0.0f, path.get(1).args[5])

        assertEquals(PathCommand.RelativeClose, path.get(2).command)
        assertEquals(0, path.get(2).args.size)
    }
}