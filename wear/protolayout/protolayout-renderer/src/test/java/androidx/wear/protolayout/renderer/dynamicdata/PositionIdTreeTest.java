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

package androidx.wear.protolayout.renderer.dynamicdata;

import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.FIRST_CHILD_INDEX;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.ROOT_NODE_ID;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.createNodePosId;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.renderer.dynamicdata.PositionIdTree.TreeNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(AndroidJUnit4.class)
public class PositionIdTreeTest {
    private static final String NODE_ROOT = ROOT_NODE_ID;
    private static final String NODE_1 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX);
    private static final String NODE_2 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX + 1);
    private static final String NODE_2_1 = createNodePosId(NODE_2, FIRST_CHILD_INDEX);
    private static final String NODE_2_2 = createNodePosId(NODE_2, FIRST_CHILD_INDEX + 1);
    private static final String NODE_3 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX + 2);
    private static final String NODE_3_1 = createNodePosId(NODE_3, FIRST_CHILD_INDEX);
    private static final String NODE_3_1_1 = createNodePosId(NODE_3_1, FIRST_CHILD_INDEX);
    private static final String NODE_3_1_1_1 = createNodePosId(NODE_3_1_1, FIRST_CHILD_INDEX);

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Mock TreeNode mNodeRoot;
    @Mock TreeNode mNode1;
    @Mock TreeNode mNode2;
    @Mock TreeNode mNode2Child1;
    @Mock TreeNode mNode2Child2;
    @Mock TreeNode mNode3;
    @Mock TreeNode mNode3Child1;
    @Mock TreeNode mNode3Child1Child1;
    @Mock TreeNode mNode3Child1Child1Child1;
    @Mock TreeNode mTestNode;

    PositionIdTree<TreeNode> mTree;
    List<TreeNode> mAllNodes;

    @Before
    public void setUp() {
        mTree = new PositionIdTree<>();
        mTree.addOrReplace(NODE_ROOT, mNodeRoot);
        mTree.addOrReplace(NODE_1, mNode1);
        mTree.addOrReplace(NODE_2, mNode2);
        mTree.addOrReplace(NODE_2_1, mNode2Child1);
        mTree.addOrReplace(NODE_2_2, mNode2Child2);
        mTree.addOrReplace(NODE_3, mNode3);

        mAllNodes = Arrays.asList(mNodeRoot, mNode1, mNode2, mNode2Child1, mNode2Child2, mNode3);
    }

    @Test
    public void emptyTree_getAllNodesIsEmpty() {
        mTree = new PositionIdTree<>();

        assertThat(mTree.getAllNodes()).isEmpty();
    }

    @Test
    public void clear_emptiesTheTreeAndDestroysAllNodes() {
        mTree.clear();

        assertThat(mTree.getAllNodes()).isEmpty();
        mAllNodes.forEach(treeNode -> verify(treeNode).destroy());
    }

    @Test
    public void addOrReplace_noOldValue_insertsInTheTree() {
        mTree.addOrReplace(createNodePosId(NODE_3, FIRST_CHILD_INDEX), mTestNode);

        assertThat(mTree.getAllNodes()).hasSize(mAllNodes.size() + 1);
        assertThat(mTree.getAllNodes()).contains(mTestNode);
    }

    @Test
    public void addOrReplace_oldValue_destroysTheOldValueAndReplacesIt() {
        List<TreeNode> expectedNodes =
                Stream.concat(
                                Stream.of(mTestNode),
                                mAllNodes.stream().filter(treeNode -> !treeNode.equals(mNode1)))
                        .collect(Collectors.toList());

        mTree.addOrReplace(NODE_1, mTestNode);

        assertThat(mTree.getAllNodes()).doesNotContain(mNode1);
        verify(mNode1).destroy();
        assertThat(mTree.getAllNodes()).containsExactlyElementsIn(expectedNodes);
        expectedNodes.forEach(treeNode -> verify(treeNode, never()).destroy());
    }

    @Test
    public void removeSubtreeByPosId_removesAndDestroysSubtree() {
        mTree.removeChildNodesFor(NODE_2);

        assertThat(mTree.getAllNodes()).contains(mNode2);
        assertThat(mTree.getAllNodes()).containsNoneOf(mNode2Child1, mNode2Child2);
        verify(mNode2, never()).destroy();
        verify(mNode2Child1).destroy();
        verify(mNode2Child2).destroy();
    }

    @Test
    public void foreach_runsOnEachNodeOnce() {
        List<TreeNode> loopedOverNodes = new ArrayList<>();

        mTree.forEach(loopedOverNodes::add);

        assertThat(loopedOverNodes).containsExactlyElementsIn(mAllNodes);
    }

    @Test
    public void findFirst_emptyTree_returnsNull() {
        mTree.clear();

        assertThat(mTree.findFirst(treeNode -> true)).isNull();
    }

    @Test
    public void findFirst_noMatch_returnsNull() {
        assertThat(mTree.findFirst(treeNode -> false)).isNull();
    }

    @Test
    public void findFirst_twoMatches_returnsFirstOne() {
        assertThat(mTree.findFirst(treeNode -> treeNode == mNode1 || treeNode == mNode2))
                .isEqualTo(mNode1);
    }

    @Test
    public void findAncestorValues_onlySearchesNodesAboveTheNode() {
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findAncestorsFor(NODE_2_1, nodesOfInterest::contains))
                .containsExactly(mNodeRoot, mNode2);
    }

    @Test
    public void findAncestorIds_onlySearchesNodesAboveTheNode() {
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findAncestorsNodesFor(NODE_2_1, nodesOfInterest::contains))
                .containsExactly(NODE_2, NODE_ROOT);
    }

    @Test
    public void findAncestorValues_disjointTree_searchesAllAboveNodes() {
        // Missing NODE_3_1
        mTree.addOrReplace(NODE_3_1_1, mNode3Child1Child1);
        mTree.addOrReplace(NODE_3_1_1_1, mNode3Child1Child1Child1);
        List<TreeNode> nodesOfInterest =
                Arrays.asList(mNodeRoot, mNode1, mNode3Child1Child1, mNode3);

        assertThat(mTree.findAncestorsFor(NODE_3_1_1_1, nodesOfInterest::contains))
                .containsExactly(mNodeRoot, mNode3Child1Child1, mNode3);
    }

    @Test
    public void findAncestorIds_disjointTree_searchesAllAboveNodes() {
        // Missing NODE_3_1
        mTree.addOrReplace(NODE_3_1_1, mNode3Child1Child1);
        mTree.addOrReplace(NODE_3_1_1_1, mNode3Child1Child1Child1);
        List<TreeNode> nodesOfInterest =
                Arrays.asList(mNodeRoot, mNode1, mNode3Child1Child1, mNode3);

        assertThat(mTree.findAncestorsNodesFor(NODE_3_1_1_1, nodesOfInterest::contains))
                .containsExactly(ROOT_NODE_ID, NODE_3_1_1, NODE_3);
    }

    @Test
    public void findAncestorValues_emptyTree_returnsNothing() {
        mTree.clear();
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findAncestorsFor(NODE_2_1, nodesOfInterest::contains)).isEmpty();
    }

    @Test
    public void findAncestorIds_emptyTree_returnsNothing() {
        mTree.clear();
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findAncestorsNodesFor(NODE_2_1, nodesOfInterest::contains)).isEmpty();
    }

    @Test
    public void findAncestorValues_noMatch_returnsNothing() {
        assertThat(mTree.findAncestorsFor(NODE_2_1, treeNode -> false)).isEmpty();
    }

    @Test
    public void findAncestorIds_noMatch_returnsNothing() {
        assertThat(mTree.findAncestorsNodesFor(NODE_2_1, treeNode -> false)).isEmpty();
    }

    @Test
    public void findChildren_onlySearchesBelowTheNode() {
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findChildrenFor(NODE_2, nodesOfInterest::contains))
                .containsExactly(mNode2Child1);
    }

    @Test
    public void findChildren_emptyTree_returnsNothing() {
        mTree.clear();
        List<TreeNode> nodesOfInterest = Arrays.asList(mNodeRoot, mNode2, mNode2Child1, mNode1);

        assertThat(mTree.findChildrenFor(NODE_2_1, nodesOfInterest::contains)).isEmpty();
    }

    @Test
    public void findChildren_noMatch_returnsNothing() {
        assertThat(mTree.findChildrenFor(NODE_2_1, treeNode -> false)).isEmpty();
    }

    @Test
    public void findChildren_disjointTree_onlySearchesUpToTheMissingNode() {
        mTree.addOrReplace(NODE_3_1, mNode3Child1);
        // Missing NODE_3_1_1
        mTree.addOrReplace(NODE_3_1_1_1, mNode3Child1Child1Child1);
        List<TreeNode> nodesOfInterest =
                Arrays.asList(mNode3, mNode3Child1, mNode3Child1Child1Child1);

        assertThat(mTree.findChildrenFor(NODE_3, nodesOfInterest::contains))
                .containsExactly(mNode3Child1);
    }

    @Test
    public void get_nodeExists_returnsTheNode() {
        assertThat(mTree.get(NODE_2)).isEqualTo(mNode2);
    }

    @Test
    public void get_nonExistentNode_returnsNull() {
        assertThat(mTree.get("NON_EXISTENT")).isNull();
    }
}
