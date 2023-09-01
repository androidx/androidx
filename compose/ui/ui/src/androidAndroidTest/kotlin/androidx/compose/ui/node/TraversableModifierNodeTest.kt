/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.TraversableNode.Companion.VisitSubtreeIfAction
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TraversableModifierNodeTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var parentNode: ClassOneWithSharedKeyTraversalNode

    private lateinit var childA: ClassOneWithSharedKeyTraversalNode
    private lateinit var childB: ClassTwoWithSharedKeyTraversalNode
    private lateinit var childC: ClassThreeWithOtherKeyTraversalNode

    private lateinit var grandChildNodeA: ClassOneWithSharedKeyTraversalNode
    private lateinit var grandChildNodeB: ClassTwoWithSharedKeyTraversalNode
    private lateinit var grandChildNodeC: ClassThreeWithOtherKeyTraversalNode

    private lateinit var grandChildNodeD: ClassOneWithSharedKeyTraversalNode
    private lateinit var grandChildNodeF: ClassThreeWithOtherKeyTraversalNode

    private lateinit var grandChildNodeG: ClassOneWithSharedKeyTraversalNode

    /**
     * The UI hierarchy for this test is setup as:
     *
     *  Parent Column (ClassOneWithSharedKeyTraversalNode)
     *    ⤷ ChildA Row (ClassOneWithSharedKeyTraversalNode)
     *        ⤷ GrandchildA Box (ClassOneWithSharedKeyTraversalNode)
     *        ⤷ GrandchildB Box (ClassTwoWithSharedKeyTraversalNode)
     *        ⤷ GrandchildC Box (ClassThreeWithOtherKeyTraversalNode)
     *
     *    ⤷ ChildB Row (ClassTwoWithSharedKeyTraversalNode)
     *         ⤷ GrandchildD Box (ClassOneWithSharedKeyTraversalNode)
     *         ⤷ GrandchildE Box (ClassTwoWithSharedKeyTraversalNode)
     *         ⤷ GrandchildF Box (ClassThreeWithOtherKeyTraversalNode)
     *
     *    ⤷ ChildC Row (ClassThreeWithOtherKeyTraversalNode)
     *         ⤷ GrandchildG Box (ClassOneWithSharedKeyTraversalNode)
     *         ⤷ GrandchildH Box (ClassTwoWithSharedKeyTraversalNode)
     *         ⤷ GrandchildI Box (ClassThreeWithOtherKeyTraversalNode)
     *
     *    ⤷ ChildD Row (ClassTwoWithSharedKeyTraversalNode)
     *         ⤷ GrandchildJ Box (ClassOneWithSharedKeyTraversalNode)
     *
     */
    @Composable
    private fun createUi() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red)
                .testTraversalNodeClassOneWithSharedKey("Parent") {
                    parentNode = this
                },
        ) {
            // Child A
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green)
                    .testTraversalNodeClassOneWithSharedKey("Child_A") {
                        childA = this
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Blue)
                        .testTraversalNodeClassOneWithSharedKey("Grandchild_A") {
                            grandChildNodeA = this
                        }
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.White)
                        .testTraversalNodeClassTwoWithSharedKey("Grandchild_B") {
                            grandChildNodeB = this
                        }
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black)
                        .testTraversalNodeClassThreeWithOtherKey("Grandchild_C") {
                            grandChildNodeC = this
                        }
                ) { }
            }
            // Child B
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Magenta)
                    .testTraversalNodeClassTwoWithSharedKey("Child_B") {
                        childB = this
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Yellow)
                        .testTraversalNodeClassOneWithSharedKey("Grandchild_D") {
                            grandChildNodeD = this
                        }
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Blue)
                        .testTraversalNodeClassTwoWithSharedKey("Grandchild_E")
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Gray)
                        .testTraversalNodeClassThreeWithOtherKey("Grandchild_F") {
                            grandChildNodeF = this
                        }
                ) { }
            }
            // Child C
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Cyan)
                    .testTraversalNodeClassThreeWithOtherKey("Child_C") {
                        childC = this
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Blue)
                        .testTraversalNodeClassOneWithSharedKey("Grandchild_G") {
                            grandChildNodeG = this
                        }
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Magenta)
                        .testTraversalNodeClassTwoWithSharedKey("Grandchild_H")
                ) { }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black)
                        .testTraversalNodeClassThreeWithOtherKey("Grandchild_I")
                ) { }
            }

            // Child D
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green)
                    .testTraversalNodeClassTwoWithSharedKey("Child_D")
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black)
                        .testTraversalNodeClassOneWithSharedKey("Grandchild_J")
                ) { }
            }
        }
    }

    @Before
    fun setup() {
        rule.setContent {
            createUi()
        }
    }

    // *********** Nearest Traversable Ancestor Tests ***********
    @Test
    fun nearestTraversableAncestor_ancestorsWithTheSameClass() {
        var nearestAncestorNode: TraversableNode? = null

        // Starts at grandchild A (which has a parent and grandparent of the same class)
        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeA.nearestTraversableAncestor()
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(childA)
        }

        // Starts at grandchild D (which has a parent of a different class + same key and
        // grandparent of the same class).
        nearestAncestorNode = null

        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeD.nearestTraversableAncestor()
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(parentNode)
        }

        // Starts at grandchild G (which has a parent of a different class + different key and
        // a grandparent of the same class).
        nearestAncestorNode = null

        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeG.nearestTraversableAncestor()
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(parentNode)
        }
    }

    @Test
    fun nearestTraversableAncestor_ancestorsWithOutTheSameClass() {
        var nearestAncestorNode: TraversableNode? = null

        // Starts at grandchild B (which has a parent and grandparent of different class but the
        // same key). Neither should match.
        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeB.nearestTraversableAncestor()
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        nearestAncestorNode = null

        // Starts at grandchild C (which has a parent and grandparent of different class and a
        // different key). Neither should match.
        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeC.nearestTraversableAncestor()
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }
    }

    @Test
    fun nearestTraversableAncestorWithKey_ancestorsWithTheSameKey() {
        var nearestAncestorNode: TraversableNode? = null

        // Starts from grandchild A with SHARED_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeA.nearestTraversableAncestorWithKey(SHARED_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(childA)
        }

        nearestAncestorNode = null

        // Starts from grandchild D with SHARED_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeD.nearestTraversableAncestorWithKey(SHARED_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(childB)
        }

        nearestAncestorNode = null

        // Starts from grandchild G with SHARED_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeG.nearestTraversableAncestorWithKey(SHARED_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(parentNode)
        }

        nearestAncestorNode = null

        // Starts from grandchild G with OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeG.nearestTraversableAncestorWithKey(OTHER_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(childC)
        }
    }

    @Test
    fun nearestTraversableAncestorWithKey_ancestorsWithoutTheSameKey() {
        var nearestAncestorNode: TraversableNode? = null

        // Starts from grandchild A with OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeA.nearestTraversableAncestorWithKey(OTHER_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        nearestAncestorNode = null

        // Starts from grandchild B with OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeB.nearestTraversableAncestorWithKey(OTHER_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        nearestAncestorNode = null

        // Starts from grandchild C with OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeC.nearestTraversableAncestorWithKey(OTHER_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        nearestAncestorNode = null

        // Starts from grandchild F with OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            nearestAncestorNode =
                grandChildNodeF.nearestTraversableAncestorWithKey(OTHER_TRAVERSAL_NODE_KEY)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }
    }

    @Test
    fun nearestTraversableAncestorWithKey_nullKey() {
        var nearestAncestorNode: TraversableNode? = null

        // Starts from grandchild A with null key.
        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeA.nearestTraversableAncestorWithKey(null)
        }

        rule.runOnIdle {
            // No ancestors have a key of null
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        // Starts from grandchild D with null key.
        nearestAncestorNode = null

        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeD.nearestTraversableAncestorWithKey(null)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }

        // Starts from grandchild F with null key.
        nearestAncestorNode = null

        rule.runOnIdle {
            nearestAncestorNode = grandChildNodeF.nearestTraversableAncestorWithKey(null)
        }

        rule.runOnIdle {
            Truth.assertThat(nearestAncestorNode).isEqualTo(null)
        }
    }

    // *********** Traverse Ancestors Tests ***********
    @Test
    fun traverseAncestors_sameClass() {
        var sameClassAncestors = 0

        // Starts from grandchild (which has a parent and grandparent of the same class).
        rule.runOnIdle {
            grandChildNodeA.traverseAncestors {
                sameClassAncestors++
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassAncestors).isEqualTo(2)
        }

        // Starts at grandchild D (which has a parent of a different class + same key and
        // grandparent of the same class).
        sameClassAncestors = 0

        rule.runOnIdle {
            grandChildNodeD.traverseAncestors {
                sameClassAncestors++
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassAncestors).isEqualTo(1)
        }

        // Starts at grandchild G (which has a parent of a different class + different key and
        // a grandparent of the same class).
        sameClassAncestors = 0

        rule.runOnIdle {
            grandChildNodeG.traverseAncestors {
                sameClassAncestors++
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassAncestors).isEqualTo(1)
        }
    }

    @Test
    fun traverseAncestors_sameClassWithCancellation() {
        var sameClassAncestors = 0

        // Starts at grandchild A (which has a parent and grandparent of the same class).
        rule.runOnIdle {
            grandChildNodeA.traverseAncestors {
                sameClassAncestors++
                // Cancel traversal
                false
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassAncestors).isEqualTo(1)
        }
    }

    @Test
    fun traverseAncestorsWithKey_sameKey() {
        var totalMatchingAncestors = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        // Starts at grandchild A (which has a parent and grandparent of the same class).
        rule.runOnIdle {
            grandChildNodeA.traverseAncestorsWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(2)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(2)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }

        // Starts at grandchild D (which has a parent of a different class + same key and
        // grandparent of the same class).
        totalMatchingAncestors = 0
        classOneWithSharedKeyTraversalNodeAncestors = 0
        classTwoWithSharedKeyTraversalNodeAncestors = 0
        classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            grandChildNodeD.traverseAncestorsWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(2)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(1)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(1)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }

        // Starts at grandchild G (which has a parent of a different class + different key and
        // a grandparent of the same class).
        totalMatchingAncestors = 0
        classOneWithSharedKeyTraversalNodeAncestors = 0
        classTwoWithSharedKeyTraversalNodeAncestors = 0
        classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            grandChildNodeG.traverseAncestorsWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(1)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(1)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }

        // Starts at grandchild G (which has a parent of OTHER_TRAVERSAL_NODE_KEY and
        // a grandparent without OTHER_TRAVERSAL_NODE_KEY.).
        totalMatchingAncestors = 0
        classOneWithSharedKeyTraversalNodeAncestors = 0
        classTwoWithSharedKeyTraversalNodeAncestors = 0
        classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            grandChildNodeG.traverseAncestorsWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(1)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(1)
        }
    }

    @Test
    fun traverseAncestorsWithKey_differentKeyFromCallingNode() {
        var totalMatchingAncestors = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        // Starts at grandchild A (which has a parent and grandparent with keys other than
        // OTHER_TRAVERSAL_NODE_KEY.
        rule.runOnIdle {
            grandChildNodeA.traverseAncestorsWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(0)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }

        // Starts at grandchild D (which has a parent and grandparent with keys other than
        // OTHER_TRAVERSAL_NODE_KEY.
        totalMatchingAncestors = 0
        classOneWithSharedKeyTraversalNodeAncestors = 0
        classTwoWithSharedKeyTraversalNodeAncestors = 0
        classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            grandChildNodeD.traverseAncestorsWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(0)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }
    }

    // Matches only keys that are set to null (of which there are none).
    @Test
    fun traverseAncestorsWithKey_nullKey() {
        var totalMatchingAncestors = 0
        var sameClassAncestors = 0
        var sameKeyDifferentClassAncestors = 0
        var differentKeyDifferentClassAncestors = 0

        // Starts at grandchild A (which has a parent and grandparent of the same class).
        rule.runOnIdle {
            grandChildNodeA.traverseAncestorsWithKey(null) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(0)
            Truth.assertThat(sameClassAncestors).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassAncestors).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassAncestors).isEqualTo(0)
        }

        // Starts at grandchild D (which has a parent of a different class + same key and
        // grandparent of the same class).
        totalMatchingAncestors = 0
        sameClassAncestors = 0
        sameKeyDifferentClassAncestors = 0
        differentKeyDifferentClassAncestors = 0

        rule.runOnIdle {
            grandChildNodeD.traverseAncestorsWithKey(null) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(0)
            Truth.assertThat(sameClassAncestors).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassAncestors).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassAncestors).isEqualTo(0)
        }

        // Starts at grandchild G (which has a parent of a different class + different key and
        // a grandparent of the same class).
        totalMatchingAncestors = 0
        sameClassAncestors = 0
        sameKeyDifferentClassAncestors = 0
        differentKeyDifferentClassAncestors = 0

        rule.runOnIdle {
            grandChildNodeG.traverseAncestorsWithKey(null) {
                totalMatchingAncestors++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingAncestors).isEqualTo(0)
            Truth.assertThat(sameClassAncestors).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassAncestors).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassAncestors).isEqualTo(0)
        }
    }

    // *********** Traverse Children Tests ***********
    @Test
    fun traverseChildren_sameClass() {
        var sameClassChildren = 0

        rule.runOnIdle {
            parentNode.traverseChildren {
                sameClassChildren++
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassChildren).isEqualTo(1)
        }
    }

    @Test
    fun traverseChildrenWithKey_sameKey() {
        var totalMatchingChildren = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassChildren = 0
        var sameKeyDifferentClassChildren = 0

        rule.runOnIdle {
            parentNode.traverseChildrenWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingChildren++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassChildren++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassChildren++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingChildren).isEqualTo(3)
            Truth.assertThat(sameClassChildren).isEqualTo(1)
            Truth.assertThat(sameKeyDifferentClassChildren).isEqualTo(2)
        }
    }

    @Test
    fun traverseChildrenWithKey_differentKeyFromCallingNode() {
        var totalMatchingChildren = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        // Only class with key = OTHER_TRAVERSAL_NODE_KEY.
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            parentNode.traverseChildrenWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingChildren++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingChildren).isEqualTo(1)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(1)
        }
    }

    // Matches only keys that are set to null (of which there are none).
    @Test
    fun traverseChildrenWithKey_nullKey() {
        var totalMatchingChildren = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            // parentNode is of type ClassOneWithSharedKeyTraversalNode
            parentNode.traverseChildrenWithKey(null) {
                totalMatchingChildren++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingChildren).isEqualTo(0)
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(0)
        }
    }

    // *********** Traverse Subtree Tests ***********
    @Test
    fun traverseSubtree_sameClass() {
        var sameClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtree {
                sameClassNodes++
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassNodes).isEqualTo(5)
        }
    }

    @Test
    fun traverseSubtreeWithKey_fromParentWithSameKey() {
        var totalMatchingNodes = 0
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(10)
            Truth.assertThat(sameClassNodes).isEqualTo(5)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(5)
        }
    }

    @Test
    fun traverseSubtreeWithKey_differentKeyFromCallingNode() {
        var totalMatchingNodes = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        // Only class with key = OTHER_TRAVERSAL_NODE_KEY.
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(4)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(4)
        }
    }

    // Matches only keys that are set to null (of which there are none).
    @Test
    fun traverseSubtreeWithKey_nullKey() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var differentKeyDifferentClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeWithKey(null) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassNodes++
                    }
                }
                // Continue traversal
                true
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(0)
            Truth.assertThat(sameClassNodes).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassNodes).isEqualTo(0)
        }
    }

    // *********** Traverse Subtree If Tests ***********
    @Test
    fun traverseSubtreeIf_alwaysContinueTraversal() {
        var sameClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIf {
                sameClassNodes++
                VisitSubtreeIfAction.VisitSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassNodes).isEqualTo(5)
        }
    }

    @Test
    fun traverseSubtreeIf_alwaysSkipSubtree() {
        var sameClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIf {
                sameClassNodes++
                VisitSubtreeIfAction.SkipSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassNodes).isEqualTo(4)
        }
    }

    @Test
    fun traverseSubtreeIf_skipOneSubtree() {
        var sameClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIf {
                sameClassNodes++

                // This will skip the subtree under childA, thus remove the grandchildA of
                // ClassOneWithSharedKeyTraversalNode from the count
                if (it == childA) {
                    VisitSubtreeIfAction.SkipSubtree
                } else {
                    VisitSubtreeIfAction.VisitSubtree
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sameClassNodes).isEqualTo(4)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_alwaysContinueTraversal() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    else -> {
                        otherNodes++
                    }
                }
                VisitSubtreeIfAction.VisitSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(10)
            Truth.assertThat(sameClassNodes).isEqualTo(5)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(5)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_alwaysCancelTraversal() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    else -> {
                        otherNodes++
                    }
                }
                VisitSubtreeIfAction.CancelTraversal
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(1)
            Truth.assertThat(sameClassNodes).isEqualTo(1)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(0)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_alwaysSkipSubtree() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    else -> {
                        otherNodes++
                    }
                }
                VisitSubtreeIfAction.SkipSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(5)
            Truth.assertThat(sameClassNodes).isEqualTo(2)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(3)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_skipSubtreeOfSameClass() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                val action = when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                        VisitSubtreeIfAction.SkipSubtree
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }

                    else -> {
                        otherNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }
                }
                action
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(8)
            Truth.assertThat(sameClassNodes).isEqualTo(4)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(4)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_cancelTraversalOfSameClass() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                val action = when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                        VisitSubtreeIfAction.CancelTraversal
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }

                    else -> {
                        otherNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }
                }
                action
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(1)
            Truth.assertThat(sameClassNodes).isEqualTo(1)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(0)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_skipSubtreeOfDifferentClassSameKey() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                val action = when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                        VisitSubtreeIfAction.SkipSubtree
                    }

                    else -> {
                        otherNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }
                }
                action
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(7)
            Truth.assertThat(sameClassNodes).isEqualTo(3)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(4)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKey_sameKeyFromCallingNode_cancelTraversalOfDifferentClassSameKey() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var otherNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(SHARED_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                val action = when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                        VisitSubtreeIfAction.CancelTraversal
                    }

                    else -> {
                        otherNodes++
                        VisitSubtreeIfAction.VisitSubtree
                    }
                }
                action
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(3)
            Truth.assertThat(sameClassNodes).isEqualTo(2)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(1)
            Truth.assertThat(otherNodes).isEqualTo(0)
        }
    }

    @Test
    fun traverseSubtreeWithKeyIf_differentKeyFromCallingNode_alwaysContinueTraversal() {
        var totalMatchingNodes = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        // Only class with key = OTHER_TRAVERSAL_NODE_KEY.
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                VisitSubtreeIfAction.VisitSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(4)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(4)
        }
    }

    @Test
    fun traverseSubtreeWithKeyIf_differentKeyFromCallingNode_alwaysSkipSubtree() {
        var totalMatchingNodes = 0
        var classOneWithSharedKeyTraversalNodeAncestors = 0
        var classTwoWithSharedKeyTraversalNodeAncestors = 0
        // Only class with key = OTHER_TRAVERSAL_NODE_KEY.
        var classThreeWithOtherKeyTraversalNodeAncestors = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(OTHER_TRAVERSAL_NODE_KEY) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        classOneWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        classTwoWithSharedKeyTraversalNodeAncestors++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        classThreeWithOtherKeyTraversalNodeAncestors++
                    }
                }
                VisitSubtreeIfAction.SkipSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(3)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classOneWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            // Should be zero because it won't match the shared key
            Truth.assertThat(classTwoWithSharedKeyTraversalNodeAncestors).isEqualTo(0)
            Truth.assertThat(classThreeWithOtherKeyTraversalNodeAncestors).isEqualTo(3)
        }
    }

    // Matches only keys that are set to null (of which there are none).
    @Test
    fun traverseSubtreeWithKeyIf_nullKey_alwaysContinueTraversal() {
        var totalMatchingNodes = 0
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var differentKeyDifferentClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(null) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassNodes++
                    }
                }
                VisitSubtreeIfAction.VisitSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(0)
            Truth.assertThat(sameClassNodes).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassNodes).isEqualTo(0)
        }
    }

    // Matches only keys that are set to null (of which there are none).
    @Test
    fun traverseSubtreeWithKeyIf_nullKey_alwaysSkipSubtree() {
        var totalMatchingNodes = 0
        // All these are in relation to the parent class where we run the traversal.
        var sameClassNodes = 0
        var sameKeyDifferentClassNodes = 0
        var differentKeyDifferentClassNodes = 0

        rule.runOnIdle {
            parentNode.traverseSubtreeIfWithKey(null) {
                totalMatchingNodes++

                when (it) {
                    is ClassOneWithSharedKeyTraversalNode -> {
                        sameClassNodes++
                    }

                    is ClassTwoWithSharedKeyTraversalNode -> {
                        sameKeyDifferentClassNodes++
                    }

                    is ClassThreeWithOtherKeyTraversalNode -> {
                        differentKeyDifferentClassNodes++
                    }
                }
                VisitSubtreeIfAction.SkipSubtree
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalMatchingNodes).isEqualTo(0)
            Truth.assertThat(sameClassNodes).isEqualTo(0)
            Truth.assertThat(sameKeyDifferentClassNodes).isEqualTo(0)
            Truth.assertThat(differentKeyDifferentClassNodes).isEqualTo(0)
        }
    }
}

// Keys used across all test classes for testing [TraversalNode].
private const val SHARED_TRAVERSAL_NODE_KEY = "SHARED_TRAVERSAL_NODE_KEY"
private const val OTHER_TRAVERSAL_NODE_KEY = "OTHER_TRAVERSAL_NODE_KEY"

// *********** Class One code (uses shared key in tests and contains funs for testing). ***********
private fun Modifier.testTraversalNodeClassOneWithSharedKey(
    label: String,
    block: (ClassOneWithSharedKeyTraversalNode.() -> Unit)? = null
) = this then TestTraversalModifierElementClassOneWithSharedKey(
    label = label,
    block = block
)

private data class TestTraversalModifierElementClassOneWithSharedKey(
    val label: String,
    val block: (ClassOneWithSharedKeyTraversalNode.() -> Unit)?
) : ModifierNodeElement<ClassOneWithSharedKeyTraversalNode>() {
    override fun create() =
        ClassOneWithSharedKeyTraversalNode(label = label, block = block)

    override fun update(node: ClassOneWithSharedKeyTraversalNode) {
        node.label = label
        node.block = block
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "testTraversalNodeClassOneWithSharedKey"
        properties["label"] = label
        properties["block"] = block
    }
}

/*
 * Main class for testing all the [TraversableNode] functions. The [block] parameter is for setting
 * variable in the test to this instance so those publicly available [TraversableNode] functions
 * can be called directly for testing.
 *
 * This isn't an example of how to use [TraversableNode]. Instead, you should call all the
 * traversal methods from within your class when you need to do some operation on nodes of the same
 * kind/key in the tree.
 */
private class ClassOneWithSharedKeyTraversalNode(
    var label: String,
    var block: (ClassOneWithSharedKeyTraversalNode.() -> Unit)?
) : Modifier.Node(), TraversableNode {
    override val traverseKey = SHARED_TRAVERSAL_NODE_KEY

    init {
        block?.let {
            it()
        }
    }

    override fun toString() =
        "ClassOneWithSharedKeyTraversalNode($label) of $SHARED_TRAVERSAL_NODE_KEY"
}

// *********** Test Class Two code (uses shared key in tests, simple test class). ***********
private fun Modifier.testTraversalNodeClassTwoWithSharedKey(
    label: String,
    block: (ClassTwoWithSharedKeyTraversalNode.() -> Unit)? = null
) = this then TestTraversalModifierElementClassTwoWithSharedKey(
    label = label,
    block = block
)

private data class TestTraversalModifierElementClassTwoWithSharedKey(
    val label: String,
    val block: (ClassTwoWithSharedKeyTraversalNode.() -> Unit)?
) : ModifierNodeElement<ClassTwoWithSharedKeyTraversalNode>() {
    override fun create() = ClassTwoWithSharedKeyTraversalNode(
        label = label,
        block = block
    )

    override fun update(node: ClassTwoWithSharedKeyTraversalNode) {
        node.label = label
        node.block = block
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "testTraversalNodeClassTwoWithSharedKey"
        properties["label"] = label
        properties["block"] = block
    }
}

private class ClassTwoWithSharedKeyTraversalNode(
    var label: String,
    var block: (ClassTwoWithSharedKeyTraversalNode.() -> Unit)?
) :
    Modifier.Node(), TraversableNode {

    override val traverseKey = SHARED_TRAVERSAL_NODE_KEY

    init {
        block?.let {
            it()
        }
    }

    override fun toString() =
        "ClassTwoWithSharedKeyTraversalNode($label) of $SHARED_TRAVERSAL_NODE_KEY"
}

// *********** Test Class Three code (uses other key in tests, simple test class). ***********
private fun Modifier.testTraversalNodeClassThreeWithOtherKey(
    label: String,
    block: (ClassThreeWithOtherKeyTraversalNode.() -> Unit)? = null
) = this then TestTraversalModifierElementClassThreeWithOtherKey(
    label = label,
    block
)

private data class TestTraversalModifierElementClassThreeWithOtherKey(
    val label: String,
    val block: (ClassThreeWithOtherKeyTraversalNode.() -> Unit)?
) : ModifierNodeElement<ClassThreeWithOtherKeyTraversalNode>() {

    override fun create() =
        ClassThreeWithOtherKeyTraversalNode(label = label, block = block)

    override fun update(node: ClassThreeWithOtherKeyTraversalNode) {
        node.label = label
        node.block = block
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "testTraversalNodeOtherKey"
        properties["label"] = label
        properties["block"] = block
    }
}

private class ClassThreeWithOtherKeyTraversalNode(
    var label: String,
    var block: (ClassThreeWithOtherKeyTraversalNode.() -> Unit)?
) : Modifier.Node(), TraversableNode {
    override val traverseKey = OTHER_TRAVERSAL_NODE_KEY

    init {
        block?.let {
            it()
        }
    }

    override fun toString() =
        "ClassThreeWithOtherKeyTraversalNode($label) of $OTHER_TRAVERSAL_NODE_KEY"
}
