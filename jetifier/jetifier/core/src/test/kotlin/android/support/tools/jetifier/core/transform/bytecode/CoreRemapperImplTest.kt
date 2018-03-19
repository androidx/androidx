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

package android.support.tools.jetifier.core.transform.bytecode

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.PackageMap
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.proguard.ProGuardTypesMap
import com.google.common.truth.Truth
import org.junit.Test
import org.objectweb.asm.ClassWriter

class CoreRemapperImplTest {

    @Test
    fun remapString_shouldUseFallbackForField() {
        val remapper = prepareRemapper(
            TypesMap(mapOf(
                JavaType.fromDotVersion("androidx.test.InputConnectionCompat")
                    to JavaType.fromDotVersion("android.support.test.InputConnectionCompat")
            )),
            "androidx/")

        val given = "androidx.test.InputConnectionCompat.CONTENT_URI"
        val expected = "android.support.test.InputConnectionCompat.CONTENT_URI"

        Truth.assertThat(remapper.rewriteString(given)).isEqualTo(expected)
    }

    private fun prepareRemapper(
            typesMap: TypesMap,
            restrictToPackagePrefix: String? = null
    ): CoreRemapperImpl {
        val prefixes = if (restrictToPackagePrefix == null) {
            emptyList()
        } else {
            listOf(restrictToPackagePrefix)
        }

        val config = Config(
                restrictToPackagePrefixes = prefixes,
                rewriteRules = emptyList(),
                typesMap = typesMap,
                slRules = emptyList(),
                pomRewriteRules = emptySet(),
                proGuardMap = ProGuardTypesMap.EMPTY,
                packageMap = PackageMap.EMPTY)

        val context = TransformationContext(config,
                isInReversedMode = true)

        val writer = ClassWriter(0 /* flags */)
        return CoreRemapperImpl(context, writer)
    }
}