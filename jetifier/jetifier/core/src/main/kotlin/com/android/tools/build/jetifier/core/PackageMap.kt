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
                to = "androidx/exifinterface"
            ),
            PackageRule(
                from = "android/support/heifwriter",
                to = "androidx/heifwriter"
            ),
            PackageRule(
                from = "android/support/graphics/drawable",
                to = "androidx/vectordrawable"
            ),
            PackageRule(
                from = "android/support/graphics/drawable/animated",
                to = "androidx/vectordrawable"
            ),
            PackageRule(
                from = "android/support/media/tv",
                to = "androidx/tvprovider"
            ),
            PackageRule(
                from = "androidx/textclassifier",
                to = "androidx/textclassifier"
            ),
            PackageRule(
                from = "androidx/recyclerview/selection",
                to = "androidx/recyclerview/selection"),
            PackageRule(
                from = "android/support/v4",
                to = "androidx/legacy/v4"
            ),
            PackageRule(
                from = "android/support/print",
                to = "androidx/print"
            ),
            PackageRule(
                from = "android/support/documentfile",
                to = "androidx/documentfile"
            ),
            PackageRule(
                from = "android/support/coordinatorlayout",
                to = "androidx/coordinatorlayout"
            ),
            PackageRule(
                from = "android/support/swiperefreshlayout",
                to = "androidx/swiperefreshlayout"
            ),
            PackageRule(
                from = "android/support/slidingpanelayout",
                to = "androidx/slidingpanelayout"
            ),
            PackageRule(
                from = "android/support/asynclayoutinflater",
                to = "androidx/asynclayoutinflater"
            ),
            PackageRule(
                from = "android/support/interpolator",
                to = "androidx/interpolator"
            ),
            PackageRule(
                from = "android/support/v7/palette",
                to = "androidx/palette"
            ),
            PackageRule(
                from = "android/support/v7/cardview",
                to = "androidx/cardview"
            ),
            PackageRule(
                from = "android/support/customview",
                to = "androidx/customview"
            ),
            PackageRule(
                from = "android/support/loader",
                to = "androidx/loader"
            ),
            PackageRule(
                from = "android/support/cursoradapter",
                to = "androidx/cursoradapter"
            ),
            PackageRule(
                from = "android/support/v7/mediarouter",
                to = "androidx/mediarouter"
            ),
            PackageRule(
                from = "android/support/v7/appcompat",
                to = "androidx/appcompat"
            ),
            PackageRule(
                from = "android/support/v7/recyclerview",
                to = "androidx/recyclerview"
            ),
            PackageRule(
                from = "android/support/v7/viewpager",
                to = "androidx/viewpager"
            ),
            PackageRule(
                from = "android/support/percent",
                to = "androidx/percentlayout"
            ),
            PackageRule(
                from = "android/support/v7/gridlayout",
                to = "androidx/gridlayout"
            ),
            PackageRule(
                from = "android/support/v13",
                to = "androidx/legacy/v13"
            ),
            PackageRule(
                from = "android/support/v7/preference",
                to = "androidx/preference"
            ),
            PackageRule(
                from = "android/support/v14/preference",
                to = "androidx/legacy/preference"
            ),
            PackageRule(
                from = "android/support/v17/leanback",
                to = "androidx/leanback"
            ),
            PackageRule(
                from = "android/support/v17/preference",
                to = "androidx/leanback/preference"
            ),
            PackageRule(
                from = "android/support/compat",
                to = "androidx/core"
            ),
            PackageRule(
                from = "android/support/mediacompat",
                to = "androidx/media"
            ),
            PackageRule(
                from = "android/support/fragment",
                to = "androidx/fragment"
            ),
            PackageRule(
                from = "android/support/coreutils",
                to = "androidx/legacy/coreutils"
            ),
            PackageRule(
                from = "android/support/dynamicanimation",
                to = "androidx/dynamicanimation"
            ),
            PackageRule(
                from = "android/support/customtabs",
                to = "androidx/browser"
            ),
            PackageRule(
                from = "android/support/coreui",
                to = "androidx/legacy/coreui"
            ),
            PackageRule(
                from = "android/support/content",
                to = "androidx/contentpager"
            ),
            PackageRule(
                from = "android/support/transition",
                to = "androidx/transition"
            ),
            PackageRule(
                from = "android/support/recommendation",
                to = "androidx/recommendation"
            ),
            PackageRule(
                from = "android/support/drawerlayout",
                to = "androidx/drawerlayout"
            ),
            PackageRule(
                from = "android/support/wear",
                to = "androidx/wear"
            ),
            PackageRule(
                from = "android/support/design",
                to = "com/google/android/material"
            ),
            PackageRule(
                from = "android/support/text/emoji/appcompat",
                to = "androidx/emoji/appcompat"
            ),
            PackageRule(
                from = "android/support/text/emoji/bundled",
                to = "androidx/emoji/bundled"
            ),
            PackageRule(
                from = "android/support/text/emoji",
                to = "androidx/emoji"
            ),
            PackageRule(
                from = "androidx/text/emoji/bundled",
                to = "androidx/text/emoji/bundled"
            ),
            PackageRule(
                from = "android/support/localbroadcastmanager",
                to = "androidx/localbroadcastmanager"
            ),
            PackageRule(
                from = "androidx/text/emoji/bundled",
                to = "androidx/text/emoji/bundled"
            ),
            PackageRule(
                from = "androidx/webkit",
                to = "androidx/webkit"
            ),
            PackageRule(
                from = "androidx/slice/view",
                to = "androidx/slice/view"
            ),
            PackageRule(
                from = "androidx/slice/core",
                to = "androidx/slice/core"
            ),
            PackageRule(
                from = "androidx/slice/builders",
                to = "androidx/slice/builders"
            ),
            PackageRule(
                from = "android/arch/paging/runtime",
                to = "androidx/paging/runtime"
            ),
            PackageRule(
                from = "android/arch/core/testing",
                to = "androidx/arch/core/testing"
            ),
            PackageRule(
                from = "android/arch/core",
                to = "androidx/arch/core"
            ),
            PackageRule(
                from = "android/arch/persistence/db/framework",
                to = "androidx/sqlite/db/framework"
            ),
            PackageRule(
                from = "android/arch/persistence/db",
                to = "androidx/sqlite/db"
            ),
            PackageRule(
                from = "android/arch/persistence/room/rxjava2",
                to = "androidx/room/rxjava2"
            ),
            PackageRule(
                from = "android/arch/persistence/room/guava",
                to = "androidx/room/guava"
            ),
            PackageRule(
                from = "android/arch/persistence/room/testing",
                to = "androidx/room/testing"
            ),
            PackageRule(
                from = "android/arch/persistence/room",
                to = "androidx/room"
            ),
            PackageRule(
                from = "android/arch/lifecycle/extensions",
                to = "androidx/lifecycle/extensions"
            ),
            PackageRule(
                from = "android/arch/lifecycle/livedata/core",
                to = "androidx/lifecycle/livedata/core"
            ),
            PackageRule(
                from = "android/arch/lifecycle",
                to = "androidx/lifecycle"
            ),
            PackageRule(
                from = "android/arch/lifecycle/viewmodel",
                to = "androidx/lifecycle/viewmodel"
            ),
            PackageRule(
                from = "android/arch/lifecycle/livedata",
                to = "androidx/lifecycle/livedata"
            ),
            PackageRule(
                from = "android/arch/lifecycle/reactivestreams",
                to = "androidx/lifecycle/reactivestreams"
            ),
            PackageRule(
                from = "android/support/multidex/instrumentation",
                to = "androidx/multidex/instrumentation"
            ),
            PackageRule(
                from = "android/support/multidex",
                to = "androidx/multidex"
            )
        )

        val EMPTY = PackageMap(emptyList())
    }

    /**
     * Creates reversed version of this map (from becomes to and vice versa).
     */
    fun reverse(): PackageMap {
        return PackageMap(rules
            .map { PackageRule(from = it.to, to = it.from) }
            .toList())
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

    data class PackageRule(val from: String, val to: String)
}