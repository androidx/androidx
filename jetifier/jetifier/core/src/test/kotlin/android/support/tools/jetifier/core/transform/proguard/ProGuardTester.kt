/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.proguard

import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule
import android.support.tools.jetifier.core.transform.TransformationContext
import com.google.common.truth.Truth
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * Helper to test ProGuard rewriting logic using lightweight syntax.
 */
class ProGuardTester {

    private var javaTypes = emptyList<Pair<String, String>>()
    private var rewriteRules = emptyList<Pair<String, String>>()
    private var proGuardTypes = emptyList<Pair<ProGuardType, ProGuardType>>()
    private var prefixes = emptyList<String>()

    fun forGivenPrefixes(vararg prefixes: String): ProGuardTester {
        this.prefixes = prefixes.toList()
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
        this.proGuardTypes = rules.map {
            ProGuardType.fromDotNotation(it.first) to ProGuardType.fromDotNotation(it.second) }
            .toList()
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
        return Config(
            restrictToPackagePrefixes = prefixes,
            rewriteRules = rewriteRules.map { RewriteRule(it.first, it.second) },
            pomRewriteRules = emptyList(),
            typesMap = TypesMap(
                types = javaTypes.map { JavaType(it.first) to JavaType(it.second) }.toMap()
            ),
            proGuardMap = ProGuardTypesMap(proGuardTypes.toMap()))
    }


    class ProGuardTesterForFile(private val config: Config, private val given: String) {

        fun rewritesTo(expected: String) {
            val context = TransformationContext(config, rewritingSupportLib = false)
            val transformer = ProGuardTransformer(context)
            val file = ArchiveFile(Paths.get("proguard.txt"), given.toByteArray())
            transformer.runTransform(file)

            val result = file.data.toString(StandardCharsets.UTF_8)

            Truth.assertThat(result).isEqualTo(expected)
            Truth.assertThat(context.wasErrorFound()).isFalse()
        }
    }

    class ProGuardTesterForType(private val config: Config, private val given: String) {

        fun getsRewrittenTo(expectedType: String) {
            val context = TransformationContext(config, rewritingSupportLib = false)
            val mapper = ProGuardTypesMapper(context)
            val result = mapper.replaceType(given)

            Truth.assertThat(result).isEqualTo(expectedType)
            Truth.assertThat(context.wasErrorFound()).isFalse()
        }
    }

    class ProGuardTesterForArgs(private val config: Config, private val given: String) {

        fun getRewrittenTo(expectedArguments: String) {
            val context = TransformationContext(config, rewritingSupportLib = false)
            val mapper = ProGuardTypesMapper(context)
            val result = mapper.replaceMethodArgs(given)

            Truth.assertThat(result).isEqualTo(expectedArguments)
            Truth.assertThat(context.wasErrorFound()).isFalse()
        }
    }
}