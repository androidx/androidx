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

package androidx.xr.compose.testing

import android.app.Activity
import android.view.View
import androidx.annotation.RestrictTo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.util.fastForEach
import androidx.xr.compose.R
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SubspaceTestContext(private val testRule: AndroidComposeTestRule<*, *>) {
    /**
     * Collects all [SubspaceSemanticsInfo]s from all compose hierarchies.
     *
     * Can crash in case it hits time out. This is not supposed to be handled as it surfaces only in
     * incorrect tests.
     */
    internal fun getAllSemanticsNodes(
        atLeastOneRootRequired: Boolean
    ): Iterable<SubspaceSemanticsInfo> {
        // Block and wait for compose state to settle before looking for root nodes.
        testRule.waitForIdle()
        val roots = testRule.activity.contentView.registeredRoots
        check(!atLeastOneRootRequired || roots.isNotEmpty()) {
            """No subspace compose hierarchies found in the app. Possible reasons include:
        (1) the Activity that calls setContent with a Subspace did not launch;
        (2) setContent with a Subspace was not called;
        (3) setContent with a Subspace was called before the ComposeTestRule ran;
        (4) a Subspace was not used in setContent
        If setContent is called by the Activity, make sure the Activity is
        launched after the ComposeTestRule runs"""
        }

        return roots.flatMap { it.getAllSemanticsNodes() }
    }
}

private fun SubspaceSemanticsInfo.getAllSemanticsNodes(): Iterable<SubspaceSemanticsInfo> {
    val nodes = mutableListOf<SubspaceSemanticsInfo>()

    fun findAllSemanticNodesRecursive(currentNode: SubspaceSemanticsInfo) {
        nodes.add(currentNode)
        currentNode.semanticsChildren.fastForEach { child -> findAllSemanticNodesRecursive(child) }
    }

    findAllSemanticNodesRecursive(this)

    return nodes
}

private val Activity.contentView: View
    get() = window.decorView

private val View.registeredRoots: MutableList<SubspaceSemanticsInfo>
    get() {
        @Suppress("UNCHECKED_CAST")
        return (getTag(R.id.compose_xr_registered_roots)
            ?: mutableListOf<SubspaceSemanticsInfo>().also {
                setTag(R.id.compose_xr_registered_roots, it)
            })
            as MutableList<SubspaceSemanticsInfo>
    }
