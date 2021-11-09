/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.metainf

import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.PackageName
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer
import kotlinx.metadata.jvm.KmPackageParts
import kotlinx.metadata.jvm.KotlinModuleMetadata

class KotlinModuleTransformer internal constructor(
    private val context: TransformationContext
) : Transformer {
    override fun canTransform(file: ArchiveFile): Boolean {
        return file.relativePath.toString().startsWith(META_INF_DIR) &&
            file.fileName.endsWith(KOTLIN_MODULE_SUFFIX) &&
            !file.isSingleFile
    }

    override fun runTransform(file: ArchiveFile) {
        val module = KotlinModuleMetadata.read(file.data)?.toKmModule()
            ?: return context.reportUnreadableKotlinModule(TAG, file.relativePath)
        val newPackageParts = module.packageParts.map { (packageName, packageParts) ->
            val pckg = PackageName.fromDotVersion(packageName)
            val result = context.config.packageMap.getPackageFor(pckg)
            val newPackageName = result?.toDotNotation()
            if (newPackageName == null && context.config.isEligibleForRewrite(pckg)) {
                context.reportNoPackageMappingFoundFailure(TAG, packageName, file.relativePath)
            }

            val newSingleFacades = packageParts.fileFacades.map(this::mapType).toMutableList()
            val newMultiFacades = packageParts.multiFileClassParts.map { (key, singleFile) ->
                mapType(key) to mapType(singleFile)
            }.toMap().toMutableMap()

            val newPackageParts = KmPackageParts(
                newSingleFacades,
                newMultiFacades,
            )
            (newPackageName ?: packageName) to newPackageParts
        }.toMap()
        module.packageParts.clear()
        module.packageParts.putAll(newPackageParts)
        file.setNewData(KotlinModuleMetadata.Writer().apply(module::accept).write().bytes)
    }

    private fun mapType(packageName: String): String {
        val javaType = JavaType(packageName)
        val newType = context.typeRewriter.rewriteType(javaType)
        if (newType == null) {
            context.reportNoMappingFoundFailure(TAG, javaType)
        }
        return newType?.fullName ?: packageName
    }
}

private const val META_INF_DIR = "META-INF"
private const val KOTLIN_MODULE_SUFFIX = ".kotlin_module"
private const val TAG = "KotlinModuleTransformer"