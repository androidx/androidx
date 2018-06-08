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

package com.android.tools.build.jetifier.processor.transform.proguard

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.proguard.ProGuardType
import com.android.tools.build.jetifier.core.proguard.ProGuardTypesMap
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.google.common.truth.Truth
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * Helper to test ProGuard rewriting logic using lightweight syntax.
 */
class ProGuardTester {

    private var javaTypes = emptyList<Pair<String, String>>()
    private var rewriteRules = emptyList<Pair<String, String>>()
    private var proGuardTypes = emptyList<Pair<ProGuardType, Set<ProGuardType>>>()
    private var prefixes = emptySet<String>()

    fun forGivenPrefixes(vararg prefixes: String): ProGuardTester {
        this.prefixes = prefixes.toSet()
        return this
    }

    fun forGivenTypesMap(vararg types: Pair<String, String>): ProGuardTester {
        this.javaTypes = types.toList()
        return this
    }

    fun forGivenRules(vararg rules: Pair<String, String>): ProGuardTester {
        this.rewriteRules = rules.toList()
        return this
    }

    fun forGivenProGuardMap(vararg rules: Pair<String, String>): ProGuardTester {
        return forGivenProGuardMapSet(*(rules.map { it.first to setOf(it.second) }.toTypedArray()))
    }

    fun forGivenProGuardMapSet(vararg rules: Pair<String, Set<String>>): ProGuardTester {
        this.proGuardTypes = rules.map {
            ProGuardType.fromDotNotation(it.first) to it.second.map {
                ProGuardType.fromDotNotation(it) }
            .toSet() }.toList()
        return this
    }

    fun testThatGivenType(givenType: String): ProGuardTesterForType {
        return ProGuardTesterForType(createConfig(), givenType)
    }

    fun testThatGivenArguments(givenArgs: String): ProGuardTesterForArgs {
        return ProGuardTesterForArgs(createConfig(), givenArgs)
    }

    fun testThatGivenProGuard(given: String): ProGuardTesterForFile {
        return ProGuardTesterForFile(createConfig(), given)
    }

    private fun createConfig(): Config {
        return Config.fromOptional(
            restrictToPackagePrefixes = prefixes,
            rulesMap = RewriteRulesMap(rewriteRules
                .map { RewriteRule(it.first, it.second) }
                .toList()),
            typesMap = TypesMap(
                types = javaTypes.map { JavaType(it.first) to JavaType(it.second) }.toMap()
            ),
            proGuardMap = ProGuardTypesMap(proGuardTypes.toMap()))
    }

    class ProGuardTesterForFile(private val config: Config, private val given: String) {

        fun rewritesTo(expected: String) {
            val context = TransformationContext(config)
            val transformer = ProGuardTransformer(context)
            val file = ArchiveFile(Paths.get("proguard.txt"), given.toByteArray())
            transformer.runTransform(file)

            val result = file.data.toString(StandardCharsets.UTF_8)

            Truth.assertThat(result).isEqualTo(expected)
            Truth.assertThat(context.errorsTotal()).isEqualTo(0)
        }
    }

    class ProGuardTesterForType(private val config: Config, private val given: String) {

        fun getsRewrittenTo(vararg expectedType: String) {
            val context = TransformationContext(config, useFallbackIfTypeIsMissing = false)
            val mapper = ProGuardTypesMapper(context)
            val result = mapper.replaceType(given)

            Truth.assertThat(result).containsExactlyElementsIn(expectedType.toSet())
            Truth.assertThat(context.errorsTotal()).isEqualTo(0)
        }
    }

    class ProGuardTesterForArgs(private val config: Config, private val given: String) {

        fun getRewrittenTo(expectedArguments: String) {
            val context = TransformationContext(config)
            val mapper = ProGuardTypesMapper(context)
            val result = mapper.replaceMethodArgs(given)

            Truth.assertThat(result.size).isEqualTo(1)
            Truth.assertThat(result.first()).isEqualTo(expectedArguments)
            Truth.assertThat(context.errorsTotal()).isEqualTo(0)
        }
    }
}