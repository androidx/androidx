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

package androidx.wear.protolayout.renderer.common;

import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.DISCARDED_FINGERPRINT_VALUE;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arc;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arcText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.column;
import static androidx.wear.protolayout.renderer.helper.TestDsl.layout;
import static androidx.wear.protolayout.renderer.helper.TestDsl.row;
import static androidx.wear.protolayout.renderer.helper.TestDsl.spanText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.spannable;
import static androidx.wear.protolayout.renderer.helper.TestDsl.text;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.Span;
import androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.LayoutDiff;
import androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.TreeNodeWithChange;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ProtoLayoutDifferTest {
    @Test
    public void createNodePosId_withSameParams() {
        String posId1 = ProtoLayoutDiffer.createNodePosId("parent", 1);
        String posId2 = ProtoLayoutDiffer.createNodePosId("parent", 1);
        assertThat(posId1).isNotEmpty();
        assertThat(posId2).isNotEmpty();
        assertThat(posId1).isEqualTo(posId2);
    }

    @Test
    public void createNodePosId_withDifferentParams() {
        String posId1 = ProtoLayoutDiffer.createNodePosId("parent", 1);
        String posId2 = ProtoLayoutDiffer.createNodePosId("parent", 2);
        String posId3 = ProtoLayoutDiffer.createNodePosId("foo", 2);
        assertThat(posId1).isNotEmpty();
        assertThat(posId2).isNotEmpty();
        assertThat(posId1).isNotEqualTo(posId2);
        assertThat(posId2).isNotEqualTo(posId3);
    }

    @Test
    public void getParentNodePosId_withValidValue() {
        String grandParentNodePosId = ProtoLayoutDiffer.ROOT_NODE_ID;
        String parentNodePosId = ProtoLayoutDiffer.createNodePosId(grandParentNodePosId, 1);
        String nodePosId = ProtoLayoutDiffer.createNodePosId(parentNodePosId, 3);

        assertThat(ProtoLayoutDiffer.getParentNodePosId(nodePosId)).isEqualTo(parentNodePosId);
        assertThat(ProtoLayoutDiffer.getParentNodePosId(parentNodePosId))
                .isEqualTo(grandParentNodePosId);
    }

    @Test
    public void getParentNodePosId_withInvalidValues() {
        assertThat(ProtoLayoutDiffer.getParentNodePosId(ProtoLayoutDiffer.ROOT_NODE_ID)).isNull();
        assertThat(ProtoLayoutDiffer.getParentNodePosId("not_a_pos_id")).isNull();
    }

    @Test
    public void getChangedNodes_withNoChange() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(referenceLayout().getFingerprint(), referenceLayout());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).isEmpty();
    }

    @Test
    public void getChangedNodes_forLayoutWithNoFingerprint() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(),
                        referenceLayout().toBuilder().clearFingerprint().build());
        assertThat(diff).isNull();
    }

    @Test
    public void getChangedNodes_bothFingerprintsDiscarded_allDiffs() {
        Layout layout =
                layout(
                        column( // 1
                                row( // 1.1
                                        text("Foo"), // 1.1.1
                                        text("Bar") // 1.1.2
                                        )));
        NodeFingerprint discardedFingerPrintRoot =
                buildShadowDiscardedFingerprint(
                        layout.getFingerprint().getRoot(),
                        "1",
                        ImmutableList.of("1", "1.1", "1.1.1", "1.1.2"),
                        ImmutableList.of("1", "1.1"));

        Layout discardedFingerprintLayout =
                Layout.newBuilder()
                        .setRoot(layout.getRoot())
                        .setFingerprint(
                                TreeFingerprint.newBuilder().setRoot(discardedFingerPrintRoot))
                        .build();

        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        discardedFingerprintLayout.getFingerprint(), discardedFingerprintLayout);
        assertThat(diff.getChangedNodes()).hasSize(1);
    }

    @Test
    public void getChangedNodes_selfChange_childrenUnaffected() {
        Layout layout =
                layout(
                        column( // 1
                                row( // 1.1
                                        text("Foo"), // 1.1.1
                                        text("Bar") // 1.1.2
                                        )));
        NodeFingerprint discardedFingerPrintRoot =
                buildShadowDiscardedFingerprint(
                        layout.getFingerprint().getRoot(),
                        "1",
                        ImmutableList.of("1.1"),
                        ImmutableList.of());

        Layout discardedFingerprintLayout =
                Layout.newBuilder()
                        .setRoot(layout.getRoot())
                        .setFingerprint(
                                TreeFingerprint.newBuilder().setRoot(discardedFingerPrintRoot))
                        .build();

        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(layout.getFingerprint(), discardedFingerprintLayout);
        assertThat(diff.getChangedNodes()).hasSize(1);
        assertThat(diff.getChangedNodes().get(0).isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_selfChangeAndChildren_isAllChange() {
        Layout layout =
                layout(
                        column( // 1
                                row( // 1.1
                                        text("Foo"), // 1.1.1
                                        text("Bar") // 1.1.2
                                        )));
        NodeFingerprint discardedFingerPrintRoot =
                buildShadowDiscardedFingerprint(
                        layout.getFingerprint().getRoot(),
                        "1",
                        ImmutableList.of("1.1"),
                        ImmutableList.of("1.1"));

        Layout discardedFingerprintLayout =
                Layout.newBuilder()
                        .setRoot(layout.getRoot())
                        .setFingerprint(
                                TreeFingerprint.newBuilder().setRoot(discardedFingerPrintRoot))
                        .build();

        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(layout.getFingerprint(), discardedFingerprintLayout);
        assertThat(diff.getChangedNodes()).hasSize(1);
        assertThat(diff.getChangedNodes().get(0).isSelfOnlyChange()).isFalse();
    }

    @Test
    public void getChangedNodes_withOneUpdatedNode() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(), layoutWithOneUpdatedNode());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(1);
        TreeNodeWithChange changedNode = diff.getChangedNodes().get(0);
        assertThat(changedNode.getLayoutElement()).isNotNull();
        assertThat(changedNode.getPosId()).isEqualTo("pT1.1.2");
        assertThat(textValue(changedNode.getLayoutElement())).isEqualTo("UPDATED");
        assertThat(changedNode.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_withOneUpdatedParentAndOneUpdatedChild() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(),
                        layoutWithOneUpdatedParentAndOneUpdatedChild());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(2);

        TreeNodeWithChange changedParentNode = diff.getChangedNodes().get(0);
        assertThat(changedParentNode.getLayoutElement()).isNotNull();
        assertThat(changedParentNode.getPosId()).isEqualTo("pT1.1");
        assertThat(changedParentNode.isSelfOnlyChange()).isTrue();

        TreeNodeWithChange changedChildNode = diff.getChangedNodes().get(1);
        assertThat(changedChildNode.getLayoutElement()).isNotNull();
        assertThat(changedChildNode.getPosId()).isEqualTo("pT1.1.2");
        assertThat(textValue(changedChildNode.getLayoutElement())).isEqualTo("UPDATED");
        assertThat(changedChildNode.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_withTwoUpdatedNodes() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(), layoutWithTwoUpdatedNodes());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(2);
        TreeNodeWithChange changedNode1 = diff.getChangedNodes().get(0);
        assertThat(changedNode1.getLayoutElement()).isNotNull();
        assertThat(changedNode1.getPosId()).isEqualTo("pT1.1.1");
        assertThat(textValue(changedNode1.getLayoutElement())).isEqualTo("UPDATED1");
        assertThat(changedNode1.isSelfOnlyChange()).isTrue();
        TreeNodeWithChange changedNode2 = diff.getChangedNodes().get(1);
        assertThat(changedNode2.getLayoutElement()).isNotNull();
        assertThat(textValue(changedNode2.getLayoutElement())).isEqualTo("UPDATED2");
        assertThat(changedNode2.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_withDifferentNumberOfChildren() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(), layoutWithDifferentNumberOfChildren());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(1);
        TreeNodeWithChange changedNode = diff.getChangedNodes().get(0);
        assertThat(changedNode.getPosId()).isEqualTo("pT1.1");
        assertThat(changedNode.getLayoutElement()).isNotNull();
        assertThat(changedNode.getLayoutElement().hasRow()).isTrue();
        assertThat(changedNode.isSelfOnlyChange()).isFalse();
    }

    @Test
    public void getChangedNodes_withChangeInArc() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(), layoutWithChangeInArc());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(1);
        TreeNodeWithChange changedNode = diff.getChangedNodes().get(0);
        assertThat(changedNode.getPosId()).isEqualTo("pT1.4.1");
        assertThat(changedNode.getArcLayoutElement()).isNotNull();
        assertThat(textValue(changedNode.getArcLayoutElement())).isEqualTo("UPDATED");
        assertThat(changedNode.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_withChangeInSpannable() {
        Layout layout1 = layout(spannable(spanText("Hello"), spanText("World")));
        Layout layout2 = layout(spannable(spanText("Hello"), spanText("Mars")));
        LayoutDiff diff = ProtoLayoutDiffer.getDiff(layout1.getFingerprint(), layout2);
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(1);
        TreeNodeWithChange changedNode = diff.getChangedNodes().get(0);
        // Although the change is in one of the Spans, we consider the Spannable itself to have
        // changed.
        assertThat(changedNode.getPosId()).isEqualTo("pT1");
        assertThat(changedNode.getLayoutElement()).isNotNull();
        assertThat(changedNode.getLayoutElement().hasSpannable()).isTrue();
        assertThat(textValue(changedNode.getLayoutElement().getSpannable().getSpans(1)))
                .isEqualTo("Mars");
        assertThat(changedNode.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void getChangedNodes_withUpdateToNodeSelfFingerprint() {
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(
                        referenceLayout().getFingerprint(),
                        layoutWithUpdateToNodeSelfFingerprint());
        assertThat(diff).isNotNull();
        assertThat(diff.getChangedNodes()).hasSize(1);
        TreeNodeWithChange changedNode = diff.getChangedNodes().get(0);
        assertThat(changedNode.getPosId()).isEqualTo("pT1.2");
        assertThat(changedNode.getLayoutElement()).isNotNull();
        assertThat(changedNode.getLayoutElement().hasRow()).isTrue();
        assertThat(changedNode.isSelfOnlyChange()).isTrue();
    }

    @Test
    public void areSameFingerprints() {
        assertThat(
                        ProtoLayoutDiffer.areSameFingerprints(
                                referenceLayout().getFingerprint(),
                                referenceLayout().getFingerprint()))
                .isTrue();
        assertThat(
                        ProtoLayoutDiffer.areSameFingerprints(
                                referenceLayout().getFingerprint(),
                                layoutWithOneUpdatedNode().getFingerprint()))
                .isFalse();
        assertThat(
                        ProtoLayoutDiffer.areSameFingerprints(
                                referenceLayout().getFingerprint(),
                                layoutWithDifferentNumberOfChildren().getFingerprint()))
                .isFalse();
        assertThat(
                        ProtoLayoutDiffer.areSameFingerprints(
                                referenceLayout().getFingerprint(),
                                layoutWithUpdateToNodeSelfFingerprint().getFingerprint()))
                .isFalse();
    }

    @Test
    public void isChildOf_forAnActualChild_returnsTrue() {
        String childPosId = "pT1.2.3";
        String parentPosId = "pT1.2";
        assertThat(ProtoLayoutDiffer.isDescendantOf(childPosId, parentPosId)).isTrue();
    }

    @Test
    public void isChildOf_forANonChild_returnsFalse() {
        String childPosId = "pT1.22.3";
        String parentPosId = "pT1.2";
        assertThat(ProtoLayoutDiffer.isDescendantOf(childPosId, parentPosId)).isFalse();
    }

    private NodeFingerprint buildShadowDiscardedFingerprint(
            NodeFingerprint fingerprintRoot,
            String rootPosId,
            List<String> discardedNodes,
            List<String> discardedChilds) {
        NodeFingerprint.Builder shadowNodeBuilder = NodeFingerprint.newBuilder();
        shadowNodeBuilder.setSelfTypeValue(fingerprintRoot.getSelfTypeValue());
        if (discardedNodes.contains(rootPosId)) {
            shadowNodeBuilder.setSelfPropsValue(DISCARDED_FINGERPRINT_VALUE);
        } else {
            shadowNodeBuilder.setSelfPropsValue(fingerprintRoot.getSelfPropsValue());
        }
        boolean discardChildren = discardedChilds.contains(rootPosId);
        if (discardChildren) {
            shadowNodeBuilder.setChildNodesValue(DISCARDED_FINGERPRINT_VALUE);
        } else {
            shadowNodeBuilder.setChildNodesValue(fingerprintRoot.getChildNodesValue());
        }
        int childIndex = 1;
        for (NodeFingerprint childNode : fingerprintRoot.getChildNodesList()) {
            NodeFingerprint childNodeFingerprint =
                    buildShadowDiscardedFingerprint(
                            childNode,
                            rootPosId + "." + childIndex++,
                            discardedNodes,
                            discardedChilds);
            if (!discardChildren) {
                shadowNodeBuilder.addChildNodes(childNodeFingerprint);
            }
            if (childNodeFingerprint.getSelfPropsValue() == DISCARDED_FINGERPRINT_VALUE) {
                shadowNodeBuilder.setChildNodesValue(DISCARDED_FINGERPRINT_VALUE);
            }
        }
        return shadowNodeBuilder.build();
    }

    private static Layout referenceLayout() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                arcText("arctext") // 1.4.1
                                )));
    }

    private static Layout layoutWithOneUpdatedNode() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("Foo"), // 1.1.1
                                text("UPDATED") // 1.1.2
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                arcText("arctext") // 1.4.1
                                )));
    }

    private static Layout layoutWithOneUpdatedParentAndOneUpdatedChild() {
        return layout(
                column( // 1
                        row( // 1.1
                                modifiers -> modifiers.widthDp = 123,
                                text("Foo"), // 1.1.1
                                text("UPDATED") // 1.1.2
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                arcText("arctext") // 1.4.1
                                )));
    }

    private static Layout layoutWithTwoUpdatedNodes() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("UPDATED1"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("UPDATED2"), // 1.3
                        arc( // 1.4
                                arcText("arctext") // 1.4.1
                                )));
    }

    private static Layout layoutWithDifferentNumberOfChildren() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("Foo"), // 1.1.1
                                text("Bar"), // 1.1.2
                                text("EXTRA") // 1.1.3
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                arcText("arctext") // 1.4.1
                                )));
    }

    private static Layout layoutWithChangeInArc() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                arcText("UPDATED") // 1.4.1
                                )));
    }

    private static Layout layoutWithUpdateToNodeSelfFingerprint() {
        return layout(
                column( // 1
                        row( // 1.1
                                text("Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                props -> {
                                    props.modifiers.border.widthDp = 5;
                                },
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc(arcText("arctext")) // 1.4
                        ));
    }

    private static String textValue(LayoutElement element) {
        return element.getText().getText().getValue();
    }

    private static String textValue(ArcLayoutElement element) {
        return element.getText().getText().getValue();
    }

    private static String textValue(Span element) {
        return element.getText().getText().getValue();
    }
}
