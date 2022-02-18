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

package androidx.build.libabigail.symbolfiles

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Assert.assertThrows
import org.junit.Test

class SymbolFileParserTest {

    @Test
    fun testNextLine() {
        val input = ByteArrayInputStream("""
            foo
            bar
            # baz
            qux
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        assertThat(parser.currentLine.trim()).isEqualTo("foo")
        assertThat(parser.nextLine().trim()).isEqualTo("bar")
        assertThat(parser.currentLine.trim()).isEqualTo("bar")
        assertThat(parser.nextLine().trim()).isEqualTo("qux")
        assertThat(parser.currentLine.trim()).isEqualTo("qux")
        assertThat(parser.nextLine().trim()).isEqualTo("")
        assertThat(parser.currentLine.trim()).isEqualTo("")
    }

    @Test
    fun testParseVersion() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                baz;
                qux;
            };
            VERSION_2 {
            } VERSION_1;
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val version = parser.parseNextVersion()
        assertThat(version.name).isEqualTo("VERSION_1")
        assertThat(version.base).isNull()
        assertThat(version.symbols).isEqualTo(
            listOf(
                Symbol("baz"),
                Symbol("qux")
            )
        )
        parser.nextLine()
        val version2 = parser.parseNextVersion()
        assertThat(version2.name).isEqualTo("VERSION_2")
        assertThat(version2.base).isEqualTo("VERSION_1")
    }

    @Test
    fun testParseVersionEOF() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val throwable = assertThrows(ParseError::class.java) {
            parser.parseNextVersion()
        }
        assertThat(throwable.message).isEqualTo("Unexpected EOF in version block.")
    }

    @Test
    fun testUnknownScopeLabel() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                foo:
            }
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val throwable = assertThrows(ParseError::class.java) {
            parser.parseNextVersion()
        }
        assertThat(throwable.message).isEqualTo("Unknown visibility label: foo")
    }

    @Test
    fun testParseSymbol() {
        val input = ByteArrayInputStream("""
            foo;
            bar;
            "baz()";
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val symbol = parser.parseNextSymbol(cppSymbol = false)
        assertThat(symbol.name).isEqualTo("foo")
        parser.nextLine()
        val symbol2 = parser.parseNextSymbol(cppSymbol = false)
        assertThat(symbol2.name).isEqualTo("bar")
        parser.nextLine()
        val symbol3 = parser.parseNextSymbol(cppSymbol = true)
        assertThat(symbol3.name).isEqualTo("baz()")
    }

    @Test
    fun testWildcardSymbolGlobal() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                *;
            };
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val throwable = assertThrows(ParseError::class.java) {
            parser.parseNextVersion()
        }
        assertThat(throwable.message).isEqualTo("Wildcard global symbols are not permitted.")
    }

    @Test
    fun testWildcardSymbolLocal() {
        val input = ByteArrayInputStream(
            """
            VERSION_1 {
                local:
                    *;
            };
        """.trimIndent().toByteArray()
        )
        val parser = SymbolFileParser(input)
        val version = parser.parseNextVersion()
        assertThat(version.symbols).isEmpty()
    }

    @Test
    fun testMissingSemicolon() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                foo
            };
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val throwable = assertThrows(ParseError::class.java) {
            parser.parseNextVersion()
        }
        assertThat(throwable.message).isEqualTo("Expected ; to terminate symbol: foo")
    }

    @Test
    fun testParseFailsInvalidInput() {
        val input = ByteArrayInputStream("""
            foo
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val throwable = assertThrows(ParseError::class.java) {
            parser.parse()
        }
        assertThat(throwable.message).isEqualTo("Unexpected contents at top level: foo")
    }

    @Test
    fun testParse() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                local:
                    hidden1;
                global:
                    foo;
                    bar;
            };
            VERSION_2 {
                # Implicit global scope.
                    woodly;
                    doodly;
                local:
                    qwerty;
            } VERSION_1;
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val versions = parser.parse()
        assertThat(versions).isEqualTo(
            listOf(
                Version(
                    name = "VERSION_1",
                    symbols = listOf(
                        Symbol("foo"),
                        Symbol("bar"),
                    )
                ),
                Version(
                    name = "VERSION_2",
                    base = "VERSION_1",
                    symbols = listOf(
                        Symbol("woodly"),
                        Symbol("doodly"),
                    )
                )
            )
        )
    }
    @Test
    fun testParseExternCpp() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                local:
                    hidden1;
                global:
                    extern "C++" {
                        "foo()";
                        "bar(int)";
                    };
            };
        """.trimIndent().toByteArray())
        val parser = SymbolFileParser(input)
        val versions = parser.parse()
        assertThat(versions).isEqualTo(
            listOf(
                Version(
                    name = "VERSION_1",
                    symbols = listOf(
                        Symbol("foo()"),
                        Symbol("bar(int)"),
                    )
                )
            )
        )
    }
}