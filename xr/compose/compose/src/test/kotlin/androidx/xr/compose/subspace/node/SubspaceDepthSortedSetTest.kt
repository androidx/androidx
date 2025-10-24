/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.AndroidComposeSpatialElement
import androidx.xr.compose.testing.SubspaceTestingActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceDepthSortedSet]. */
@RunWith(AndroidJUnit4::class)
class SubspaceDepthSortedSetTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspaceDepthSortedSet_rootIsFirst_returnsTrueAlways() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)

        assertThat(subspaceDepthSortedSet.pop()).isEqualTo(rootNode)
    }

    @Test
    fun subspaceDepthSortedSet_addingAndRemovingChild_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)

        assertThat(subspaceDepthSortedSet.contains(childNode)).isTrue()

        subspaceDepthSortedSet.remove(childNode)

        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }

    @Test
    fun subspaceDepthSortedSet_clearSet_returnsAnEmptySet() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)
        subspaceDepthSortedSet.clear()

        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }

    @Test
    fun subspaceDepthSortedSet_addOrderDoesNotAffectSorting_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(parentNode)
        subspaceDepthSortedSet.add(childNode)

        assertThat(subspaceDepthSortedSet.pop()).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.pop()).isEqualTo(childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(parentNode)

        assertThat(subspaceDepthSortedSet.pop()).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.pop()).isEqualTo(childNode)
        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }
}
