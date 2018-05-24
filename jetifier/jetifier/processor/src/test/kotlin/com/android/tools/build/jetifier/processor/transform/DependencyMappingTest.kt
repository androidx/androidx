/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.processor.Processor
import com.google.common.truth.Truth
import org.junit.Test

class DependencyMappingTest {

    @Test fun mapTest_oneToOne_shouldMap() {
        MappingTester.testRewrite(
            from = "hello:world:1.0.0",
            to = "hi:all:2.0.0",
            rules = setOf(
                PomRewriteRule(
                    from = PomDependency(groupId = "hello", artifactId = "world"),
                    to = PomDependency(groupId = "hi", artifactId = "all", version = "2.0.0")
                ))
        )
    }

    object MappingTester {

        fun testRewrite(from: String, to: String?, rules: Set<PomRewriteRule>) {
            val config = Config.fromOptional(
                pomRewriteRules = rules
            )

            val processor = Processor.createProcessor(config)
            val result = processor.mapDependency(from)

            if (to == null) {
                Truth.assertThat(result).isNull()
            } else {
                Truth.assertThat(result).isEqualTo(to)
            }
        }
    }
}