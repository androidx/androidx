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

package android.support.tools.jetifier.core.transform

import android.support.tools.jetifier.core.rules.PackageName

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
        val DEFAULT_RULES = listOf(
            PackageRule(
                from = "android/support/exifinterface",
                to = "android/support/exifinterface"
            ),
            PackageRule(
                from = "android/support/graphics/drawable",
                to = "androidx/graphics/drawable"
            ),
            PackageRule(
                from = "android/support/graphics/drawable/animated",
                to = "androidx/graphics/drawable/animated"
            ),
            PackageRule(
                from = "androidx/media/tv",
                to = "androidx/media/tv"
            ),
            PackageRule(
                from = "androidx/view/textclassifier",
                to = "androidx/view/textclassifier"
            ),
            PackageRule(
                from = "androidx/widget/recyclerview/selection",
                to = "androidx/widget/recyclerview/selection"),
            PackageRule(
                from = "android/support/v4",
                to = "android/support/v4"
            ),
            PackageRule(
                from = "android/support/v7/palette",
                to = "android/support/v7/palette"
            ),
            PackageRule(
                from = "android/support/v7/cardview",
                to = "android/support/v7/cardview"
            ),
            PackageRule(
                from = "android/support/v7/mediarouter",
                to = "android/support/v7/mediarouter"
            ),
            PackageRule(
                from = "android/support/v7/appcompat",
                to = "android/support/v7/appcompat"
            ),
            PackageRule(
                from = "android/support/v7/recyclerview",
                to = "android/support/v7/recyclerview"
            ),
            PackageRule(
                from = "android/support/v7/gridlayout",
                to = "android/support/v7/gridlayout"
            ),
            PackageRule(
                from = "android/support/v13",
                to = "android/support/v13"
            ),
            PackageRule(
                from = "android/support/v7/preference",
                to = "androidx/preference",
                filePrefix = "preference-v7"
            ),
            PackageRule(
                from = "android/support/v14/preference",
                to = "androidx/preference",
                filePrefix = "preference-v14"
            ),
            PackageRule(
                from = "android/support/v17/lexanback",
                to = "androidx/leanback"
            ),
            PackageRule(
                from = "android/support/v17/preference",
                to = "androidx/leanback/preference"
            ),
            PackageRule(
                from = "android/support/compat",
                to = "android/support/compat"
            ),
            PackageRule(
                from = "android/support/mediacompat",
                to = "android/support/mediacompat"
            ),
            PackageRule(
                from = "android/support/fragment",
                to = "android/support/fragment"
            ),
            PackageRule(
                from = "android/support/coreutils",
                to = "android/support/coreutils"
            ),
            PackageRule(
                from = "android/support/dynamicanimation",
                to = "android/support/dynamicanimation"
            ),
            PackageRule(
                from = "android/support/customtabs",
                to = "androidx/browser/customtabs"
            ),
            PackageRule(
                from = "android/support/coreui",
                to = "android/support/coreui"
            ),
            PackageRule(
                from = "android/support/content",
                to = "android/support/content"
            ),
            PackageRule(
                from = "android/support/transition",
                to = "androidx/transition"
            ),
            PackageRule(
                from = "android/support/v17/leanback",
                to = "androidx/leanback"
            ),
            PackageRule(
                from = "android/support/recommendation",
                to = "android/support/recommendation"
            ),
            PackageRule(
                from = "android/support/wear",
                to = "androidx/wear"
            ),
            PackageRule(
                from = "android/support/design",
                to = "androidx/design"
            ),
            PackageRule(
                from = "androidx/text/emoji",
                to = "androidx/text/emoji"
            ),
            PackageRule(
                from = "androidx/text/emoji/appcompat",
                to = "androidx/text/emoji/appcompat"
            ),
            PackageRule(
                from = "androidx/text/emoji/bundled",
                to = "androidx/text/emoji/bundled"
            )
        )

        val EMPTY = PackageMap(emptyList())
    }

    /**
     * Creates reversed version of this map (from becomes to and vice versa).
     */
    fun reverse(): PackageMap {
        return PackageMap(rules
            .map { PackageRule(from = it.to, to = it.from, filePrefix = it.filePrefix) }
            .toList())
    }

    /**
     * Returns a new package name for the given [fromPackage] defined in artifact being called
     * [libraryName].
     */
    fun getPackageFor(fromPackage: PackageName, libraryName: String): PackageName? {
        val rule = rules.find {
            it.from == fromPackage.fullName
                && (it.filePrefix == null
                || libraryName.startsWith(it.filePrefix, ignoreCase = true))
        }
        if (rule != null) {
            return PackageName(rule.to)
        }
        return null
    }

    data class PackageRule(val from: String, val to: String, val filePrefix: String? = null)
}