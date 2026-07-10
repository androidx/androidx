/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.json

class RemoteComposeJsonParserTest {
    /*
    @Test
    fun testSimpleLayoutConversion() {
        val json = """
        {
          "header": { "width": 800, "height": 600 },
          "resources": {
            "floats": [{ "id": 1, "v": 100.0 }],
            "text": [{ "id": 2, "v": "Hello" }]
          },
          "root": {
            "type": "Column",
            "modifiers": [
              { "type": "Padding", "all": 16 }
            ],
            "children": [
              {
                "type": "Row",
                "modifiers": [
                  { "type": "Width", "v": 200 }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val parser = RemoteComposeJsonParser()
        val operations = parser.parse(json)

        val expectedTypes = listOf(
            Header::class.java,
            FloatConstant::class.java,
            TextData::class.java,
            ColumnLayout::class.java,
            PaddingModifierOperation::class.java,
            RowLayout::class.java,
            WidthModifierOperation::class.java,
            ContainerEnd::class.java, // Row closure
            ContainerEnd::class.java  // Column closure
        )

        Assert.assertEquals("Number of operations mismatch", expectedTypes.size, operations.size)
        for (i in expectedTypes.indices) {
            Assert.assertEquals("Operation $i type mismatch", expectedTypes[i], operations[i].javaClass)
        }
    }

    @Test
    fun testTickerSimplifiedConversion() {
        val json = """
        {
          "header": { "width": 1080, "height": 1920 },
          "root": {
            "type": "Column",
            "modifiers": [{ "type": "Width", "v": "FILL" }],
            "children": [
              {
                "type": "Row",
                "modifiers": [{ "type": "Padding", "l": 32, "t": 32, "r": 32, "b": 32 }],
                "children": [
                  { "type": "CoreText", "text": "Watchlist" }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val parser = RemoteComposeJsonParser()
        val operations = parser.parse(json)

        Assert.assertTrue("Should contain PaddingModifierOperation", operations.any { it is PaddingModifierOperation })
        Assert.assertTrue("Should contain ColumnLayout", operations.any { it is ColumnLayout })
        Assert.assertTrue("Should contain RowLayout", operations.any { it is RowLayout })
        Assert.assertTrue("Should end with ContainerEnd", operations.last() is ContainerEnd)
    }

    @Test
    fun testRootLayoutId() {
        val json = """
        {
          "root": {
            "type": "Root",
            "id": 5000
          }
        }
        """.trimIndent()
        val parser = RemoteComposeJsonParser()
        val operations = parser.parse(json)
        val root = operations[0] as RootLayoutComponent
        // RootLayoutComponent has mComponentId inherited from Component
        // But since we can't access private fields easily in test, we check toString or deepToString
        Assert.assertTrue("Root ID should be 5000", root.toString().contains("5000"))
    }
    */
}
