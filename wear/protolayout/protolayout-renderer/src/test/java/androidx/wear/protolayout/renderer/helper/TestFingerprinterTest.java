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
package androidx.wear.protolayout.renderer.helper;

import static androidx.wear.protolayout.renderer.helper.TestDsl.arc;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arcAdapter;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arcText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.column;
import static androidx.wear.protolayout.renderer.helper.TestDsl.layout;
import static androidx.wear.protolayout.renderer.helper.TestDsl.row;
import static androidx.wear.protolayout.renderer.helper.TestDsl.text;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class TestFingerprinterTest {
    @Test
    public void fingerprintsGeneratedForAllNodes() {
        Layout layout =
                TestFingerprinter.getDefault()
                        .buildLayoutWithFingerprints(referenceLayout().getRoot());
        NodeFingerprint root = layout.getFingerprint().getRoot();
        Set<Integer> selfPropsFingerprints = new HashSet<>();
        Set<Integer> childFingerprints = new HashSet<>();
        // 1
        NodeFingerprint node = root;
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(4);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.1
        node = root.getChildNodes(0);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(2);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.1.1
        node = root.getChildNodes(0).getChildNodes(0);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesList()).isEmpty();
        assertThat(node.getChildNodesValue()).isEqualTo(0);
        // 1.1.2
        node = root.getChildNodes(0).getChildNodes(1);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesList()).isEmpty();
        assertThat(node.getChildNodesValue()).isEqualTo(0);
        // 1.2
        node = root.getChildNodes(1);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(1);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.2.1
        node = root.getChildNodes(1).getChildNodes(0);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesList()).isEmpty();
        assertThat(node.getChildNodesValue()).isEqualTo(0);
        // 1.3
        node = root.getChildNodes(2);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesList()).isEmpty();
        assertThat(node.getChildNodesValue()).isEqualTo(0);
        // 1.4
        node = root.getChildNodes(3);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(2);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.4.1
        node = root.getChildNodes(3).getChildNodes(0);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(0);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.4.2
        node = root.getChildNodes(3).getChildNodes(1);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(1);
        assertThat(childFingerprints.add(node.getChildNodesValue())).isTrue();
        // 1.4.2.1
        node = root.getChildNodes(3).getChildNodes(1).getChildNodes(0);
        assertThat(selfPropsFingerprints.add(node.getSelfPropsValue())).isTrue();
        assertThat(node.getChildNodesCount()).isEqualTo(0);
    }

    @Test
    public void fingerprintsSameForUnchangedNodes1() {
        Layout refLayout =
                TestFingerprinter.getDefault()
                        .buildLayoutWithFingerprints(referenceLayout().getRoot());
        NodeFingerprint refRoot = refLayout.getFingerprint().getRoot();
        Layout layout =
                TestFingerprinter.getDefault()
                        .buildLayoutWithFingerprints(layoutWithDifferentColumnHeight().getRoot());
        NodeFingerprint root = layout.getFingerprint().getRoot();
        // 1
        NodeFingerprint refNode = refRoot;
        NodeFingerprint node = root;
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue())
                .isNotEqualTo(refNode.getSelfPropsValue()); // Only difference
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1
        refNode = refRoot.getChildNodes(0);
        node = root.getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1.1
        refNode = refRoot.getChildNodes(0).getChildNodes(0);
        node = root.getChildNodes(0).getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1.2
        refNode = refRoot.getChildNodes(0).getChildNodes(1);
        node = root.getChildNodes(0).getChildNodes(1);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.2
        refNode = refRoot.getChildNodes(1);
        node = root.getChildNodes(1);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.2.1
        refNode = refRoot.getChildNodes(1).getChildNodes(0);
        node = root.getChildNodes(1).getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.3
        refNode = refRoot.getChildNodes(2);
        node = root.getChildNodes(2);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.4
        refNode = refRoot.getChildNodes(3);
        node = root.getChildNodes(3);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
    }

    @Test
    public void fingerprintsSameForUnchangedNodes2() {
        Layout refLayout =
                TestFingerprinter.getDefault()
                        .buildLayoutWithFingerprints(referenceLayout().getRoot());
        NodeFingerprint refRoot = refLayout.getFingerprint().getRoot();
        Layout layout =
                TestFingerprinter.getDefault()
                        .buildLayoutWithFingerprints(layoutWithDifferentText().getRoot());
        NodeFingerprint root = layout.getFingerprint().getRoot();
        // 1
        NodeFingerprint refNode = refRoot;
        NodeFingerprint node = root;
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isNotEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1
        refNode = refRoot.getChildNodes(0);
        node = root.getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1.1
        refNode = refRoot.getChildNodes(0).getChildNodes(0);
        node = root.getChildNodes(0).getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.1.2
        refNode = refRoot.getChildNodes(0).getChildNodes(1);
        node = root.getChildNodes(0).getChildNodes(1);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.2
        refNode = refRoot.getChildNodes(1);
        node = root.getChildNodes(1);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isNotEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.2.1
        refNode = refRoot.getChildNodes(1).getChildNodes(0);
        node = root.getChildNodes(1).getChildNodes(0);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue())
                .isNotEqualTo(refNode.getSelfPropsValue()); // Updated text
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.3
        refNode = refRoot.getChildNodes(2);
        node = root.getChildNodes(2);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
        // 1.4
        refNode = refRoot.getChildNodes(3);
        node = root.getChildNodes(3);
        assertThat(node.getSelfTypeValue()).isEqualTo(refNode.getSelfTypeValue());
        assertThat(node.getSelfPropsValue()).isEqualTo(refNode.getSelfPropsValue());
        assertThat(node.getChildNodesValue()).isEqualTo(refNode.getChildNodesValue());
        assertThat(node.getChildNodesCount()).isEqualTo(refNode.getChildNodesCount());
    }

    private static Layout referenceLayout() {
        return layout(
                column( // 1
                        props -> {
                            props.heightDp = 50;
                            props.widthDp = 60;
                        },
                        row( // 1.1
                                text(props -> props.maxLines = 3, "Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                props -> {
                                    props.heightDp = 20;
                                    props.widthDp = 40;
                                },
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                props -> props.anchorAngleDegrees = 15, // arc props
                                arcText("arctext"), // 1.4.1
                                arcAdapter( // 1.4.2
                                        text("text") // 1.4.2.1
                                        ))));
    }

    private static Layout layoutWithDifferentColumnHeight() {
        return layout(
                column( // 1
                        props -> {
                            props.heightDp = 40;
                            props.widthDp = 60;
                        },
                        row( // 1.1
                                text(props -> props.maxLines = 3, "Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                props -> {
                                    props.heightDp = 20;
                                    props.widthDp = 40;
                                },
                                text("Baz") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                props -> props.anchorAngleDegrees = 15, // arc props
                                arcText("arctext"), // 1.4.1
                                arcAdapter( // 1.4.2
                                        text("text") // 1.4.2.1
                                        ))));
    }

    private static Layout layoutWithDifferentText() {
        return layout(
                column( // 1
                        props -> {
                            props.heightDp = 50;
                            props.widthDp = 60;
                        },
                        row( // 1.1
                                text(props -> props.maxLines = 3, "Foo"), // 1.1.1
                                text("Bar") // 1.1.2
                                ),
                        row( // 1.2
                                props -> {
                                    props.heightDp = 20;
                                    props.widthDp = 40;
                                },
                                text("NEW TEXT") // 1.2.1
                                ),
                        text("blah blah"), // 1.3
                        arc( // 1.4
                                props -> props.anchorAngleDegrees = 15, // arc props
                                arcText("arctext"), // 1.4.1
                                arcAdapter( // 1.4.2
                                        text("text") // 1.4.2.1
                                        ))));
    }
}
