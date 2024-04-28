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

import static androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement.InnerCase.ADAPTER;

import androidx.annotation.Nullable;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement.InnerCase;

import java.util.Collections;
import java.util.List;

/**
 * Util to fingerprint existing Layout proto objects, without requiring the Layout to be built with
 * androidx builders. This uses proto hashCode methods and therefore is less efficient than using
 * builders to produce fingerprints. Its main purpose is to allow generation of fingerprints from
 * golden textproto files in tests.
 */
public class TestFingerprinter {
    private static final TestFingerprinter DEFAULT_INSTANCE = new TestFingerprinter();
    private static final int MULTIPLIER_FOR_LINEAR_ELEMENT = 277;
    private static final int MULTIPLIER_FOR_ARC_ELEMENT = 3307;

    private TestFingerprinter() {}

    /** Get the default instance. */
    public static TestFingerprinter getDefault() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Build a layout that has the given {@code rootElement} at its root, and its fingerprint
     * automatically generated.
     */
    public Layout buildLayoutWithFingerprints(LayoutElement rootElement) {
        return Layout.newBuilder()
                .setRoot(rootElement)
                .setFingerprint(buildTreeFingerprint(rootElement))
                .build();
    }

    private TreeFingerprint buildTreeFingerprint(LayoutElement rootElement) {
        NodeFingerprint rootFingerprint = addNodeToParent(rootElement, null);
        return TreeFingerprint.newBuilder().setRoot(rootFingerprint).build();
    }

    private List<LayoutElement> getChildren(LayoutElementProto.LayoutElement element) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return element.getColumn().getContentsList();
            case BOX:
                return element.getBox().getContentsList();
            case ROW:
                return element.getRow().getContentsList();
            default:
                return Collections.emptyList();
        }
    }

    private List<LayoutElementProto.ArcLayoutElement> getArcChildren(
            LayoutElementProto.LayoutElement element) {
        if (element.getInnerCase() == InnerCase.ARC) {
            return element.getArc().getContentsList();
        } else {
            return Collections.emptyList();
        }
    }

    private NodeFingerprint addNodeToParent(
            LayoutElementProto.LayoutElement element,
            @Nullable NodeFingerprint.Builder parentFingerprintBuilder) {
        NodeFingerprint.Builder currentFingerprintBuilder =
                NodeFingerprint.newBuilder()
                        .setSelfTypeValue(getSelfTypeFingerprint(element))
                        .setSelfPropsValue(getSelfPropsFingerprint(element));
        for (LayoutElementProto.LayoutElement child : getChildren(element)) {
            addNodeToParent(child, currentFingerprintBuilder);
        }
        for (LayoutElementProto.ArcLayoutElement child : getArcChildren(element)) {
            addNodeToParent(child, currentFingerprintBuilder);
        }

        NodeFingerprint currentFingerprint = currentFingerprintBuilder.build();
        if (parentFingerprintBuilder != null) {
            addNodeToParent(currentFingerprint, parentFingerprintBuilder);
        }
        return currentFingerprint;
    }

    private void addNodeToParent(
            LayoutElementProto.ArcLayoutElement element,
            NodeFingerprint.Builder parentFingerprintBuilder) {
        NodeFingerprint.Builder currentFingerprint =
                NodeFingerprint.newBuilder()
                        .setSelfTypeValue(getSelfTypeFingerprint(element))
                        .setSelfPropsValue(getSelfPropsFingerprint(element));
        if (element.getInnerCase() == ADAPTER) {
            addNodeToParent(element.getAdapter().getContent(), currentFingerprint);
        }
        addNodeToParent(currentFingerprint.build(), parentFingerprintBuilder);
    }

    private void addNodeToParent(
            NodeFingerprint node, NodeFingerprint.Builder parentFingerprintBuilder) {
        parentFingerprintBuilder.addChildNodes(node);
        parentFingerprintBuilder.setChildNodesValue(
                31 * parentFingerprintBuilder.getChildNodesValue() + getAggregateFingerprint(node));
    }

    private int getAggregateFingerprint(NodeFingerprint node) {
        int aggregateValue = node.getSelfTypeValue();
        aggregateValue = (31 * aggregateValue) + node.getSelfPropsValue();
        aggregateValue = (31 * aggregateValue) + node.getChildNodesValue();
        return aggregateValue;
    }

    private int getSelfTypeFingerprint(LayoutElementProto.LayoutElement element) {
        return MULTIPLIER_FOR_LINEAR_ELEMENT * element.getInnerCase().getNumber();
    }

    private int getSelfTypeFingerprint(LayoutElementProto.ArcLayoutElement element) {
        return MULTIPLIER_FOR_ARC_ELEMENT * element.getInnerCase().getNumber();
    }

    private int getSelfPropsFingerprint(LayoutElementProto.LayoutElement element) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return element.getColumn().toBuilder().clearContents().build().hashCode();
            case BOX:
                return element.getBox().toBuilder().clearContents().build().hashCode();
            case ROW:
                return element.getRow().toBuilder().clearContents().build().hashCode();
            case ARC:
                return element.getArc().toBuilder().clearContents().build().hashCode();
            default:
                return element.hashCode();
        }
    }

    private int getSelfPropsFingerprint(LayoutElementProto.ArcLayoutElement element) {
        return element.hashCode();
    }
}
