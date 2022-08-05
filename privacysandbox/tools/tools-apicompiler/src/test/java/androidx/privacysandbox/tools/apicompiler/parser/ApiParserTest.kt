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

package androidx.privacysandbox.tools.apicompiler.parser

import androidx.privacysandbox.tools.apicompiler.model.AnnotatedInterface
import androidx.privacysandbox.tools.apicompiler.model.ParsedApi
import androidx.privacysandbox.tools.apicompiler.util.parseSource
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiParserTest {

    @Test
    fun parseInterface() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk
                """
            )
        assertThat(parseSource(source)).isEqualTo(
            ParsedApi(
                services = setOf(
                    AnnotatedInterface(
                        className = "MySdk",
                        packageName = "com.mysdk"
                    )
                )
            )
        )
    }
}