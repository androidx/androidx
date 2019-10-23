/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.truth

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavController
import com.google.common.truth.Fact.fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * A Truth Subject for making assertions about [NavController].
 */
class NavControllerSubject private constructor(
    metadata: FailureMetadata,
    private val actual: NavController
) : Subject(metadata, actual) {

    /**
     * Assert that the [NavController] has the given current destination
     * in its [androidx.navigation.NavGraph].
     *
     * @param navDest The ID resource of a [androidx.navigation.NavDestination]
     */
    fun isCurrentDestination(@IdRes navDest: Int) {
        val actualDest = actual.currentDestination?.id
        if (actualDest != navDest) {
            failWithoutActual(
                fact("expected id", "0x${navDest.toString(16)}"),
                fact("but was", "0x${actualDest?.toString(16)}"),
                fact("current destination is", actual.currentDestination)
            )
        }
    }

    /**
     * Assert that the [NavController] has the given [androidx.navigation.NavGraph] as
     * its current graph.
     *
     * @param navGraph The ID resource of a [androidx.navigation.NavGraph]
     */
    fun isGraph(@IdRes navGraph: Int) {
        val actualGraph = actual.graph.id
        if (actualGraph != navGraph) {
            failWithoutActual(
                fact("expected id", "0x${navGraph.toString(16)}"),
                fact("but was", "0x${actualGraph.toString(16)}"),
                fact("current graph is", actual.graph)
            )
        }
    }

    companion object {
        @SuppressLint("MemberVisibilityCanBePrivate")
        val factory = Factory<NavControllerSubject, NavController> {
                metadata, actual -> NavControllerSubject(metadata, actual) }

        @JvmStatic
        fun assertThat(actual: NavController): NavControllerSubject {
            return assertAbout(factory).that(actual)
        }
    }
}
