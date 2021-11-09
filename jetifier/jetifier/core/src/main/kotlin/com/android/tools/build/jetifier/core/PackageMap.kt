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

import com.android.tools.build.jetifier.core.type.PackageName
import com.google.gson.annotations.SerializedName

/**
 * Package map to be used to rewrite packages. The rewrite rules allow duplicities where the
 * artifact name prefix defined in a rule determines if such rule should be used or skipped.
 * The priority is determined only by the order (top to bottom). Having a rule with no file prefix
 * as first means that it is always applied.
 *
 * We use this only for the support library rewriting to rewrite packages in manifest files.
 */
class PackageMap(private val rules: List<PackageRule>) {

    companion object {
        val EMPTY = PackageMap(emptyList())
    }

    /**
     * Creates reversed version of this map (from becomes to and vice versa).
     */
    fun reverse(): PackageMap {
        return PackageMap(
            rules
                .map { PackageRule(from = it.to, to = it.from) }
                .toList()
        )
    }

    /**
     * Returns a new package name for the given [fromPackage].
     */
    fun getPackageFor(fromPackage: PackageName): PackageName? {
        val rule = rules.find { it.from == fromPackage.fullName }
        if (rule != null) {
            return PackageName(rule.to)
        }
        return null
    }

    /** Returns JSON data model of this class */
    fun toJson(): List<PackageRule.JsonData> {
        return rules.map { it.toJson() }
    }

    data class PackageRule(val from: String, val to: String) {

        /** Returns JSON data model of this class */
        fun toJson(): JsonData {
            return JsonData(from, to)
        }

        /**
         * JSON data model for [PackageRule].
         */
        data class JsonData(
            @SerializedName("from")
            val from: String,
            @SerializedName("to")
            val to: String
        ) {
            /** Creates instance of [PackageRule] */
            fun toMappings(): PackageRule {
                return PackageRule(from, to)
            }
        }
    }
}
