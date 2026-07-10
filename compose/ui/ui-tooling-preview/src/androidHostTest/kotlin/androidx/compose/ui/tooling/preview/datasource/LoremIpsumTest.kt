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

package androidx.compose.ui.tooling.preview.datasource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoremIpsumTest {
    @Test
    fun testLoremIpsumProvider() {
        assertEquals("", LoremIpsum(0).values.single())
        assertEquals(LOREM_IPSUM_10_WORDS, LoremIpsum(10).values.single())
        assertEquals(LOREM_IPSUM_110_WORDS, LoremIpsum(110).values.single())
        assertEquals(LOREM_IPSUM_2_PARAGRAPHS, LoremIpsum(111).values.single())
        assertEquals(2000, LoremIpsum(2000).values.single().split(" ").size)
        assertTrue(LoremIpsum().values.single().startsWith("Lorem ipsum dolor sit amet"))
    }

    private companion object {
        const val LOREM_IPSUM_10_WORDS =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer sodales"

        const val LOREM_IPSUM_110_WORDS =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Integer sodales laoreet commodo. " +
                "Phasellus a purus eu risus elementum consequat. " +
                "Aenean eu elit ut nunc convallis laoreet non ut libero. " +
                "Suspendisse interdum placerat risus vel ornare. " +
                "Donec vehicula, turpis sed consectetur ullamcorper, " +
                "ante nunc egestas quam, ultricies adipiscing velit enim at nunc. " +
                "Aenean id diam neque. " +
                "Praesent ut lacus sed justo viverra fermentum et ut sem. " +
                "Fusce convallis gravida lacinia. " +
                "Integer semper dolor ut elit sagittis lacinia. " +
                "Praesent sodales scelerisque eros at rhoncus. " +
                "Duis posuere sapien vel ipsum ornare interdum at eu quam. " +
                "Vestibulum vel massa erat. Aenean quis sagittis purus. " +
                "Phasellus arcu purus, rutrum id consectetur non, bibendum at nibh."

        const val LOREM_IPSUM_2_PARAGRAPHS =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Integer sodales laoreet commodo. " +
                "Phasellus a purus eu risus elementum consequat. " +
                "Aenean eu elit ut nunc convallis laoreet non ut libero. " +
                "Suspendisse interdum placerat risus vel ornare. " +
                "Donec vehicula, turpis sed consectetur ullamcorper, " +
                "ante nunc egestas quam, ultricies adipiscing velit enim at nunc. " +
                "Aenean id diam neque. " +
                "Praesent ut lacus sed justo viverra fermentum et ut sem. " +
                "Fusce convallis gravida lacinia. " +
                "Integer semper dolor ut elit sagittis lacinia. " +
                "Praesent sodales scelerisque eros at rhoncus. " +
                "Duis posuere sapien vel ipsum ornare interdum at eu quam. " +
                "Vestibulum vel massa erat. Aenean quis sagittis purus. " +
                "Phasellus arcu purus, rutrum id consectetur non, bibendum at nibh. " +
                "\n\n" +
                "Duis"
    }
}
