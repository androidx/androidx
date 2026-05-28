/*
 * Copyright 2026 The Android Open Source Project
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
 * See the License for the License file.
 */

package androidx.a2ui.core.protocol

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class A2UiDataPathTest {

    @Test
    fun constructor_rawPath_isPreservedUnmodified() {
        val rawInput = "/foo/bar//"
        val dataPath = A2uiDataPath(rawInput)
        assertThat(dataPath.path).isEqualTo(rawInput)
    }

    @Test
    fun constructor_emptyPath_segmentsAreEmpty() {
        val dataPath = A2uiDataPath("")
        assertThat(dataPath.normalizedPath).isEmpty()
        assertThat(dataPath.segments).isEmpty()
    }

    @Test
    fun constructor_rootPath_segmentsAreEmpty() {
        val dataPath = A2uiDataPath("/")
        assertThat(dataPath.normalizedPath).isEmpty()
        assertThat(dataPath.segments).isEmpty()
    }

    @Test
    fun constructor_pathWithDoubleSlashOnly_segmentsAreEmpty() {
        val dataPath = A2uiDataPath("//")
        assertThat(dataPath.normalizedPath).isEqualTo("/")
        assertThat(dataPath.segments).isEmpty()
    }

    @Test
    fun constructor_simplePath_segmentsAreCorrect() {
        val dataPath = A2uiDataPath("/foo/bar")
        assertThat(dataPath.normalizedPath).isEqualTo("/foo/bar")
        assertThat(dataPath.segments).containsExactly("foo", "bar").inOrder()
    }

    @Test
    fun constructor_pathWithoutLeadingSlash_segmentsAreCorrect() {
        val dataPath = A2uiDataPath("foo/bar")
        assertThat(dataPath.normalizedPath).isEqualTo("foo/bar")
        assertThat(dataPath.segments).containsExactly("foo", "bar").inOrder()
    }

    @Test
    fun constructor_pathWithSpacesAndSpecialCharacters_segmentsArePreserved() {
        val dataPath = A2uiDataPath("/foo bar/baz!@#")
        assertThat(dataPath.normalizedPath).isEqualTo("/foo bar/baz!@#")
        assertThat(dataPath.segments).containsExactly("foo bar", "baz!@#").inOrder()
    }

    @Test
    fun constructor_pathWithMiddleConsecutiveSlashes_middleEmptySegmentsArePreserved() {
        val dataPath = A2uiDataPath("/foo//bar")
        assertThat(dataPath.normalizedPath).isEqualTo("/foo//bar")
        assertThat(dataPath.segments).containsExactly("foo", "", "bar").inOrder()
    }

    @Test
    fun constructor_pathWithTrailingSlash_segmentsAndNormalizedPathAreCorrect() {
        val dataPath = A2uiDataPath("/foo/bar/")
        assertThat(dataPath.normalizedPath).isEqualTo("/foo/bar")
        assertThat(dataPath.segments).containsExactly("foo", "bar").inOrder()
    }

    @Test
    fun constructor_pathWithDoubleTrailingSlash_segmentsAndNormalizedPathAreCorrect() {
        val dataPath = A2uiDataPath("/foo/bar//")
        assertThat(dataPath.normalizedPath).isEqualTo("/foo/bar/")
        assertThat(dataPath.segments).containsExactly("foo", "bar", "").inOrder()
    }

    @Test
    fun constructor_pathWithEscapedSlash_slashIsDecoded() {
        val dataPath = A2uiDataPath("/a~1b")
        assertThat(dataPath.normalizedPath).isEqualTo("/a~1b")
        assertThat(dataPath.segments).containsExactly("a/b")
    }

    @Test
    fun constructor_pathWithEscapedTilde_tildeIsDecoded() {
        val dataPath = A2uiDataPath("/a~0b")
        assertThat(dataPath.normalizedPath).isEqualTo("/a~0b")
        assertThat(dataPath.segments).containsExactly("a~b")
    }

    @Test
    fun constructor_pathWithMultipleEscapedSequences_allAreDecoded() {
        val dataPath = A2uiDataPath("/~1~0")
        assertThat(dataPath.normalizedPath).isEqualTo("/~1~0")
        assertThat(dataPath.segments).containsExactly("/~")
    }

    @Test
    fun constructor_pathWithInvalidEscapeSequence_isAcceptedGracefully() {
        val dataPath = A2uiDataPath("/~2/foo~")
        assertThat(dataPath.normalizedPath).isEqualTo("/~2/foo~")
        assertThat(dataPath.segments).containsExactly("~2", "foo~").inOrder()
    }

    @Test
    fun isAbsolute_emptyPath_returnsTrue() {
        assertThat(A2uiDataPath("").isAbsolute).isTrue()
    }

    @Test
    fun isAbsolute_rootPath_returnsTrue() {
        assertThat(A2uiDataPath("/").isAbsolute).isTrue()
    }

    @Test
    fun isAbsolute_absolutePath_returnsTrue() {
        assertThat(A2uiDataPath("/foo/bar").isAbsolute).isTrue()
    }

    @Test
    fun isAbsolute_relativePath_returnsFalse() {
        assertThat(A2uiDataPath("foo/bar").isAbsolute).isFalse()
    }

    @Test
    fun equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(A2uiDataPath("/"), A2uiDataPath(""))
            .addEqualityGroup(A2uiDataPath("/foo/bar"), A2uiDataPath("/foo/bar/"))
            .addEqualityGroup(A2uiDataPath("foo/bar"), A2uiDataPath("foo/bar/"))
            .addEqualityGroup(A2uiDataPath("/bar/foo"))
            .addEqualityGroup(A2uiDataPath("/a~1b"))
            .addEqualityGroup(A2uiDataPath("/a/b"))
            .addEqualityGroup(A2uiDataPath("/foo"))
            .testEquals()
    }

    @Test
    fun toString_anyPath_returnsExpectedFormat() {
        val path = A2uiDataPath("/foo/bar")
        assertThat(path.toString())
            .isEqualTo("A2uiDataPath(path='/foo/bar', normalizedPath='/foo/bar', isAbsolute=true)")
    }
}
