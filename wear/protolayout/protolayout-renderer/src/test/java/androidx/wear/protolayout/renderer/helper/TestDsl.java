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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.proto.ActionProto.Action;
import androidx.wear.protolayout.proto.ActionProto.PendingIntentAction;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignmentProp;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignmentProp;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.DimensionProto.ImageDimension;
import androidx.wear.protolayout.proto.DimensionProto.SpProp;
import androidx.wear.protolayout.proto.DimensionProto.SpacerDimension;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.LayoutElementProto.Arc;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcAdapter;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcText;
import androidx.wear.protolayout.proto.LayoutElementProto.Box;
import androidx.wear.protolayout.proto.LayoutElementProto.Column;
import androidx.wear.protolayout.proto.LayoutElementProto.Image;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.Row;
import androidx.wear.protolayout.proto.LayoutElementProto.Spacer;
import androidx.wear.protolayout.proto.LayoutElementProto.Span;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanText;
import androidx.wear.protolayout.proto.LayoutElementProto.Spannable;
import androidx.wear.protolayout.proto.LayoutElementProto.Text;
import androidx.wear.protolayout.proto.ModifiersProto;
import androidx.wear.protolayout.proto.TypesProto.BoolProp;
import androidx.wear.protolayout.proto.TypesProto.Int32Prop;
import androidx.wear.protolayout.proto.TypesProto.StringProp;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A simple DSL for more easily producing layout protos in tests.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * layout(
 *   column(
 *     props -> {
 *       props.heightDp = 20;
 *       props.modifiers.border.widthDp = 2;
 *     },
 *     row(
 *       text("Foo"),
 *       text("Bar")
 *     )
 *   )
 * )
 * }</pre>
 */
public class TestDsl {

    private TestDsl() {}

    /** An intermediate opaque layout node produced by the builders in this class. */
    public static final class LayoutNode {
        private LayoutElement.Builder mLayoutElement;
        private ArcLayoutElement.Builder mArcLayoutElement;
        private Span.Builder mSpanElement;
        private NodeFingerprint mFingerprint;
    }

    /** Corresponds to {@link ModifiersProto.Border} */
    public static final class Border {
        public int widthDp;
        public int colorArgb;

        private ModifiersProto.Border toProto() {
            return ModifiersProto.Border.newBuilder()
                    .setWidth(DpProp.newBuilder().setValue(widthDp))
                    .setColor(ColorProp.newBuilder().setArgb(colorArgb))
                    .build();
        }
    }

    /** Creates to {@link ModifiersProto.Clickable} with a {@link PendingIntentAction} */
    public static ModifiersProto.Clickable pendingIntentActionClickable(String clickableId) {
        return ModifiersProto.Clickable.newBuilder()
                .setId(clickableId)
                .setOnClick(
                        Action.newBuilder()
                                .setPendingIntentAction(PendingIntentAction.newBuilder()))
                .build();
    }

    /** Corresponds to {@link ModifiersProto.Modifiers} */
    public static final class Modifiers {
        public Border border = new Border();

        private ModifiersProto.Modifiers toProto() {
            ModifiersProto.Modifiers.Builder proto = ModifiersProto.Modifiers.newBuilder();
            proto.setBorder(border.toProto());
            return proto.build();
        }
    }

    /** Corresponds to {@link LayoutElementProto.FontStyle} */
    public static final class FontStyle {
        public float sizeSp;
        public boolean italic;
        public int colorArgb;

        private LayoutElementProto.FontStyle toProto() {
            return LayoutElementProto.FontStyle.newBuilder()
                    .addSize(sp(sizeSp))
                    .setItalic(bool(italic))
                    .setColor(color(colorArgb))
                    .build();
        }
    }

    /** Properties of a Box, with each field directly accessible for ease of use. */
    public static final class BoxProps {
        public Modifiers modifiers = new Modifiers();
        public int widthDp;
        public int heightDp;
        public HorizontalAlignment horizontalAlignment;
        public VerticalAlignment verticalAlignment;

        private void applyTo(Box.Builder box) {
            box.setModifiers(modifiers.toProto());
            box.setWidth(dpContainerDim(widthDp));
            box.setHeight(dpContainerDim(heightDp));
            box.setHorizontalAlignment(
                    HorizontalAlignmentProp.newBuilder().setValue(horizontalAlignment).build());
            box.setVerticalAlignment(
                    VerticalAlignmentProp.newBuilder().setValue(verticalAlignment).build());
        }

        private int fingerprint() {
            return Objects.hash(
                    modifiers.toProto(), widthDp, heightDp, horizontalAlignment, verticalAlignment);
        }
    }

    /** Properties of a Row, with each field directly accessible for ease of use. */
    public static final class RowProps {
        public Modifiers modifiers = new Modifiers();
        public int widthDp;
        public int heightDp;

        private void applyTo(Row.Builder row) {
            row.setModifiers(modifiers.toProto());
            row.setWidth(dpContainerDim(widthDp));
            row.setHeight(dpContainerDim(heightDp));
        }

        private int fingerprint() {
            return Objects.hash(modifiers.toProto(), widthDp, heightDp);
        }
    }

    /** Properties of a Column, with each field directly accessible for ease of use. */
    public static final class ColumnProps {
        public Modifiers modifiers = new Modifiers();
        public int widthDp;
        public int heightDp;

        private void applyTo(Column.Builder column) {
            column.setModifiers(modifiers.toProto());
            column.setWidth(dpContainerDim(widthDp));
            column.setHeight(dpContainerDim(heightDp));
        }

        private int fingerprint() {
            return Objects.hash(modifiers.toProto(), widthDp, heightDp);
        }
    }

    /** Properties of a Text object, with each field directly accessible for ease of use. */
    public static final class TextProps {
        public Modifiers modifiers = new Modifiers();
        public int maxLines = 1;
        public float lineHeightSp = 1;
        public FontStyle fontStyle = new FontStyle();

        private void applyTo(Text.Builder text) {
            text.setModifiers(modifiers.toProto());
            text.setMaxLines(int32(maxLines));
            text.setLineHeight(sp(lineHeightSp));
            text.setFontStyle(fontStyle.toProto());
        }

        private int fingerprint() {
            return Objects.hash(modifiers.toProto(), maxLines, fontStyle.toProto());
        }
    }

    /** Properties of a Image object, with each field directly accessible for ease of use. */
    public static final class ImageProps {
        public Modifiers modifiers = new Modifiers();
        public int widthDp;
        public int heightDp;

        private void applyTo(Image.Builder image) {
            image.setModifiers(modifiers.toProto());
            image.setWidth(dpImageDim(widthDp));
            image.setHeight(dpImageDim(heightDp));
        }

        private int fingerprint() {
            return Objects.hash(modifiers.toProto(), widthDp, heightDp);
        }
    }

    /** Properties of a Spacer object, with each field directly accessible for ease of use. */
    public static final class SpacerProps {
        public Modifiers modifiers = new Modifiers();
        public int widthDp;
        public int heightDp;

        private void applyTo(Spacer.Builder spacer) {
            spacer.setModifiers(modifiers.toProto());
            spacer.setWidth(dpSpacerDim(widthDp));
            spacer.setHeight(dpSpacerDim(heightDp));
        }

        private int fingerprint() {
            return Objects.hash(modifiers.toProto(), widthDp, heightDp);
        }
    }

    /** Properties of an Arc, with each field directly accessible for ease of use. */
    public static final class ArcProps {
        public float anchorAngleDegrees;

        private void applyTo(Arc.Builder arc) {
            arc.setAnchorAngle(degrees(anchorAngleDegrees));
        }

        private int fingerprint() {
            return Float.hashCode(anchorAngleDegrees);
        }
    }

    /** Properties of an Spannable, with each field directly accessible for ease of use. */
    public static final class SpannableProps {
        public int maxLines;

        private void applyTo(Spannable.Builder spannable) {
            spannable.setMaxLines(int32(maxLines));
        }

        private int fingerprint() {
            return Float.hashCode(maxLines);
        }
    }

    public static Layout layout(LayoutNode root) {
        return Layout.newBuilder()
                .setRoot(root.mLayoutElement)
                .setFingerprint(TreeFingerprint.newBuilder().setRoot(root.mFingerprint))
                .build();
    }

    public static LayoutNode box(Consumer<BoxProps> propsConsumer, LayoutNode... nodes) {
        return boxInternal(propsConsumer, nodes);
    }

    public static LayoutNode box(LayoutNode... nodes) {
        return boxInternal(/* propsConsumer= */ null, nodes);
    }

    private static LayoutNode boxInternal(
            @Nullable Consumer<BoxProps> propsConsumer, LayoutNode... nodes) {
        LayoutNode element = new LayoutNode();
        Box.Builder builder = Box.newBuilder().addAllContents(linearContents(nodes));
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            BoxProps props = new BoxProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setBox(builder.build());
        element.mFingerprint = fingerprint("box", selfPropsFingerprint, nodes);
        return element;
    }

    public static LayoutNode row(Consumer<RowProps> propsConsumer, LayoutNode... nodes) {
        return rowInternal(propsConsumer, nodes);
    }

    public static LayoutNode row(LayoutNode... nodes) {
        return rowInternal(/* propsConsumer= */ null, nodes);
    }

    private static LayoutNode rowInternal(
            @Nullable Consumer<RowProps> propsConsumer, LayoutNode... nodes) {
        LayoutNode element = new LayoutNode();
        Row.Builder builder = Row.newBuilder().addAllContents(linearContents(nodes));
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            RowProps props = new RowProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setRow(builder.build());
        element.mFingerprint = fingerprint("row", selfPropsFingerprint, nodes);
        return element;
    }

    public static LayoutNode column(Consumer<ColumnProps> propsConsumer, LayoutNode... nodes) {
        return columnInternal(propsConsumer, nodes);
    }

    public static LayoutNode column(LayoutNode... nodes) {
        return columnInternal(/* propsConsumer= */ null, nodes);
    }

    private static LayoutNode columnInternal(
            @Nullable Consumer<ColumnProps> propsConsumer, LayoutNode... nodes) {
        LayoutNode element = new LayoutNode();
        Column.Builder builder = Column.newBuilder().addAllContents(linearContents(nodes));
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            ColumnProps props = new ColumnProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setColumn(builder.build());
        element.mFingerprint = fingerprint("column", selfPropsFingerprint, nodes);
        return element;
    }

    public static LayoutNode dynamicFixedText(String fixedText) {
        return dynamicFixedText(/* propsConsumer= */ null, fixedText, /* valueForLayout= */ null);
    }

    public static LayoutNode dynamicFixedText(String fixedText, @Nullable String valueForLayout) {
        return dynamicFixedText(/* propsConsumer= */ null, fixedText, valueForLayout);
    }

    public static LayoutNode dynamicFixedText(Consumer<TextProps> propsConsumer, String fixedText) {
        return textInternal(propsConsumer, dynamicStr(fixedText, /* valueForLayout= */ null));
    }

    public static LayoutNode dynamicFixedText(
            Consumer<TextProps> propsConsumer, String fixedText, @Nullable String valueForLayout) {
        return textInternal(propsConsumer, dynamicStr(fixedText, valueForLayout));
    }

    public static LayoutNode text(String text) {
        return text(/* propsConsumer= */ null, text);
    }

    public static LayoutNode text(Consumer<TextProps> propsConsumer, String text) {
        return textInternal(propsConsumer, str(text));
    }

    private static LayoutNode textInternal(
            @Nullable Consumer<TextProps> propsConsumer, StringProp text) {
        LayoutNode element = new LayoutNode();
        Text.Builder builder = Text.newBuilder().setText(text);
        int selfPropsFingerprint = text.hashCode();
        if (propsConsumer != null) {
            TextProps props = new TextProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setText(builder.build());
        element.mFingerprint = fingerprint("text", selfPropsFingerprint);
        return element;
    }

    public static LayoutNode image(Consumer<ImageProps> propsConsumer, String resourceId) {
        return imageInternal(propsConsumer, resourceId);
    }

    public static LayoutNode image(String resourceId) {
        return imageInternal(/* propsConsumer= */ null, resourceId);
    }

    private static LayoutNode imageInternal(
            @Nullable Consumer<ImageProps> propsConsumer, String resourceId) {
        LayoutNode element = new LayoutNode();
        Image.Builder builder = Image.newBuilder().setResourceId(str(resourceId));
        int selfPropsFingerprint = resourceId.hashCode();
        if (propsConsumer != null) {
            ImageProps props = new ImageProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setImage(builder.build());
        element.mFingerprint = fingerprint("image", selfPropsFingerprint);
        return element;
    }

    public static LayoutNode spacer(Consumer<SpacerProps> propsConsumer) {
        return spacerInternal(propsConsumer);
    }

    public static LayoutNode spacer() {
        return spacerInternal(/* propsConsumer= */ null);
    }

    private static LayoutNode spacerInternal(@Nullable Consumer<SpacerProps> propsConsumer) {
        LayoutNode element = new LayoutNode();
        Spacer.Builder builder = Spacer.newBuilder();
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            SpacerProps props = new SpacerProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = props.fingerprint();
        }
        element.mLayoutElement = LayoutElement.newBuilder().setSpacer(builder.build());
        element.mFingerprint = fingerprint("spacer", selfPropsFingerprint);
        return element;
    }

    public static LayoutNode arc(Consumer<ArcProps> propsConsumer, LayoutNode... nodes) {
        return arcInternal(propsConsumer, nodes);
    }

    public static LayoutNode arc(LayoutNode... nodes) {
        return arcInternal(/* propsConsumer= */ null, nodes);
    }

    public static LayoutNode arcAdapter(LayoutNode layoutNode) {
        return arcAdapterInternal(layoutNode);
    }

    private static LayoutNode arcInternal(
            @Nullable Consumer<ArcProps> propsConsumer, LayoutNode... nodes) {
        LayoutNode element = new LayoutNode();
        Arc.Builder builder = Arc.newBuilder().addAllContents(radialContents(nodes));
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            ArcProps props = new ArcProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        element.mLayoutElement = LayoutElement.newBuilder().setArc(builder.build());
        element.mFingerprint = fingerprint("arc", selfPropsFingerprint, nodes);
        return element;
    }

    private static LayoutNode arcAdapterInternal(LayoutNode node) {
        LayoutNode element = new LayoutNode();
        ArcAdapter.Builder builder = ArcAdapter.newBuilder().setContent(node.mLayoutElement);
        int selfPropsFingerprint = 0;
        element.mArcLayoutElement = ArcLayoutElement.newBuilder().setAdapter(builder.build());
        element.mFingerprint = fingerprint("arcAdapter", selfPropsFingerprint, node);
        return element;
    }

    public static LayoutNode arcText(String text) {
        LayoutNode element = new LayoutNode();
        element.mArcLayoutElement =
                ArcLayoutElement.newBuilder().setText(ArcText.newBuilder().setText(str(text)));
        element.mFingerprint = fingerprint("arcText", text.hashCode());
        return element;
    }

    public static LayoutNode spannable(
            Consumer<SpannableProps> propsConsumer, LayoutNode... nodes) {
        return spannableInternal(propsConsumer, nodes);
    }

    public static LayoutNode spannable(LayoutNode... nodes) {
        return spannableInternal(/* propsConsumer= */ null, nodes);
    }

    private static LayoutNode spannableInternal(
            @Nullable Consumer<SpannableProps> propsConsumer, LayoutNode... nodes) {
        LayoutNode element = new LayoutNode();
        Spannable.Builder builder = Spannable.newBuilder().addAllSpans(spanContents(nodes));
        int selfPropsFingerprint = 0;
        if (propsConsumer != null) {
            SpannableProps props = new SpannableProps();
            propsConsumer.accept(props);
            props.applyTo(builder);
            selfPropsFingerprint = combine(selfPropsFingerprint, props.fingerprint());
        }
        // A Spannable is *not* considered a container for diffing purposes (i.e. any updated
        // children will cause the entire Spannable to be updated). So we include the fingerprint of
        // the nodes in the Spannable's self fingerprint. This mirrors behaviour from
        // Spannable::Builder::addSpan at http://shortn/_cUyrG0M1N2
        selfPropsFingerprint = combine(selfPropsFingerprint, fingerprints(nodes).hashCode());
        element.mLayoutElement = LayoutElement.newBuilder().setSpannable(builder.build());
        element.mFingerprint = fingerprint("spannable", selfPropsFingerprint);
        return element;
    }

    public static LayoutNode spanText(String text) {
        LayoutNode element = new LayoutNode();
        element.mSpanElement = Span.newBuilder().setText(SpanText.newBuilder().setText(str(text)));
        element.mFingerprint = fingerprint("spanText", text.hashCode());
        return element;
    }

    private static NodeFingerprint fingerprint(
            String selfTypeName, int selfPropsValue, LayoutNode... nodes) {
        return NodeFingerprint.newBuilder()
                .setSelfTypeValue(selfTypeName.hashCode())
                .setSelfPropsValue(selfPropsValue)
                .setChildNodesValue(fingerprints(nodes).hashCode())
                .addAllChildNodes(fingerprints(nodes))
                .build();
    }

    private static int combine(int fingerprint1, int fingerprint2) {
        return 31 * fingerprint1 + fingerprint2;
    }

    private static List<LayoutElement> linearContents(LayoutNode[] nodes) {
        return stream(nodes).map(n -> n.mLayoutElement.build()).collect(toList());
    }

    private static List<NodeFingerprint> fingerprints(LayoutNode[] nodes) {
        return stream(nodes).map(n -> n.mFingerprint).collect(toList());
    }

    private static List<ArcLayoutElement> radialContents(LayoutNode[] nodes) {
        return stream(nodes).map(n -> n.mArcLayoutElement.build()).collect(toList());
    }

    private static List<Span> spanContents(LayoutNode[] nodes) {
        return stream(nodes).map(n -> n.mSpanElement.build()).collect(toList());
    }

    private static ContainerDimension dpContainerDim(float dp) {
        return ContainerDimension.newBuilder().setLinearDimension(dp(dp)).build();
    }

    private static ImageDimension dpImageDim(float dp) {
        return ImageDimension.newBuilder().setLinearDimension(dp(dp)).build();
    }

    private static SpacerDimension dpSpacerDim(float dp) {
        return SpacerDimension.newBuilder().setLinearDimension(dp(dp)).build();
    }

    private static ColorProp color(int value) {
        return ColorProp.newBuilder().setArgb(value).build();
    }

    private static BoolProp bool(boolean value) {
        return BoolProp.newBuilder().setValue(value).build();
    }

    private static DpProp dp(float value) {
        return DpProp.newBuilder().setValue(value).build();
    }

    private static SpProp sp(float value) {
        return SpProp.newBuilder().setValue(value).build();
    }

    private static DegreesProp degrees(float degrees) {
        return DegreesProp.newBuilder().setValue(degrees).build();
    }

    private static Int32Prop int32(int value) {
        return Int32Prop.newBuilder().setValue(value).build();
    }

    private static StringProp str(String value) {
        return StringProp.newBuilder().setValue(value).build();
    }

    private static StringProp dynamicStr(String fixedValue, @Nullable String valueForLayout) {
        StringProp.Builder builder =
                StringProp.newBuilder()
                        .setDynamicValue(
                                DynamicString.newBuilder()
                                        .setFixed(FixedString.newBuilder().setValue(fixedValue)));
        if (valueForLayout != null) {
            builder.setValueForLayout(valueForLayout);
        }
        return builder.build();
    }
}
