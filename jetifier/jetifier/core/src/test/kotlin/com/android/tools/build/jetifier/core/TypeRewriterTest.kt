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

package com.android.tools.build.jetifier.core

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.google.common.truth.Truth
import org.junit.Test

class TypeRewriterTest {

    @Test fun simpleRewrite_typesMap() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            typesMap = TypesMap(
                JavaType.fromDotVersion("test.sample.Class")
                        to JavaType.fromDotVersion("test.sample2.Class2")
            )
        )
    }

    @Test fun prefixAllowedForRewrite() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            packagePrefix = "notTest/",
            typesMap = TypesMap(
                JavaType.fromDotVersion("test.sample.Class")
                        to JavaType.fromDotVersion("test.sample2.Class2")
            )
        )
    }

    @Test fun typeMissingInMap_returnNull() {
        testRewrite(
            from = "test.sample.Class",
            to = null
        )
    }

    @Test fun typeMissingInMap_useFallback_shouldRewrite() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            rewriteRulesMap = RewriteRulesMap(RewriteRule(
                "test/sample/Cl(.*)",
                "test/sample2/Cl{0}2"
            )),
            useFallback = true
        )
    }

    @Test fun typeMissingInMap_useFallback_innerClass_shouldRewrite() {
        testRewrite(
            from = "test.sample.Class\$Inner",
            to = "test.sample2.Class2\$Inner",
            rewriteRulesMap = RewriteRulesMap(RewriteRule(
                    "test/sample/Class(.*)",
                    "test/sample2/Class2{0}"
            )),
            useFallback = true
        )
    }

    @Test fun typeMissingInMap_useFallback_reversedMap_shouldRewrite() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            rewriteRulesMap = RewriteRulesMap(RewriteRule(
                "test/sample2/Cl(.*)2",
                "test/sample/Cl{0}"
            )).reverse(),
            useFallback = true
        )
    }

    @Test fun useBothMaps_typesMapHasPriority() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            typesMap = TypesMap(
                JavaType.fromDotVersion("test.sample.Class")
                        to JavaType.fromDotVersion("test.sample2.Class2")
            ),
            rewriteRulesMap = RewriteRulesMap(RewriteRule(
                "test/sample/Cl(.*)",
                "test/sample3/Cl{0}3"
            )),
            useFallback = true
        )
    }

    @Test fun ignoreRule_shouldNotRewrite() {
        testRewrite(
            from = "test.sample.Class",
            to = "test.sample2.Class2",
            typesMap = TypesMap(
                JavaType.fromDotVersion("test.sample.Class")
                        to JavaType.fromDotVersion("test.sample2.Class2")
            ),
            rewriteRulesMap = RewriteRulesMap(RewriteRule(
                "test/sample/Cl(.*)",
                "ignoreInRuntime"
            ))
        )
    }

    fun testRewrite(
        from: String,
        to: String?,
        packagePrefix: String = "test/",
        typesMap: TypesMap = TypesMap.EMPTY,
        rewriteRulesMap: RewriteRulesMap = RewriteRulesMap.EMPTY,
        useFallback: Boolean = false
    ) {
        val config = Config.fromOptional(
            restrictToPackagePrefixes = setOf(packagePrefix),
            rulesMap = rewriteRulesMap,
            typesMap = typesMap
        )

        val rewriter = TypeRewriter(config, useFallback)
        val result = rewriter.rewriteType(JavaType.fromDotVersion(from))

        if (to == null) {
            Truth.assertThat(result).isNull()
        } else {
            Truth.assertThat(result).isEqualTo(JavaType.fromDotVersion(to))
        }
    }
}