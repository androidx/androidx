/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.testConfiguration

import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.gson.GsonBuilder

/**
 * List of App APKs required to install the app.
 *
 * APKs must be installed group by group in same order as listed.
 */
data class AppApksModel(val apkGroups: List<ApkFileGroup>) {

    @Suppress("UnstableApiUsage") // guava Hashing is marked as @Beta
    fun sha256(): String? {
        val shaHashes = apkGroups.flatMap(ApkFileGroup::apks).map(ApkFile::sha256)
        if (shaHashes.isEmpty()) {
            return null
        }

        if (shaHashes.size == 1) {
            return shaHashes[0]
        }

        val hasher = Hashing.sha256().newHasher()
        shaHashes.forEach { hasher.putString(it, Charsets.UTF_8) }
        return BaseEncoding.base16().lowerCase().encode(hasher.hash().asBytes())
    }

    fun toJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(this)

    companion object {
        fun fromJson(json: String): AppApksModel =
            GsonBuilder().create().fromJson(json, AppApksModel::class.java)
    }
}

/** Group of APKs / Splits that needs to be installed together. */
data class ApkFileGroup(val apks: List<ApkFile>) {
    fun isUsingApkSplits(): Boolean = apks.size > 1
}

/** Single APK / Split file */
data class ApkFile(val name: String, val sha256: String = "")

internal fun singleFileAppApksModel(name: String, sha256: String = ""): AppApksModel =
    AppApksModel(apkGroups = listOf(singleFileApkFileGroup(name, sha256)))

internal fun singleFileApkFileGroup(name: String, sha256: String = ""): ApkFileGroup =
    ApkFileGroup(apks = listOf(ApkFile(name, sha256)))
