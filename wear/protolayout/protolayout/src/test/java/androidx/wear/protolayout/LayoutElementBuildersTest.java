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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.expand;
import static androidx.wear.protolayout.DimensionBuilders.sp;
import static androidx.wear.protolayout.DimensionBuilders.weight;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Color;

import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.DimensionProto;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.TypesProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LayoutElementBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final DimensionBuilders.DegreesProp DEGREES_PROP =
            new DimensionBuilders.DegreesProp.Builder(10)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))
                    .build();
    private static final DimensionBuilders.AngularLayoutConstraint DEGREES_PROP_CONSTRAINT =
            new DimensionBuilders.AngularLayoutConstraint.Builder(20)
                    .setAngularAlignment(LayoutElementBuilders.ANGULAR_ALIGNMENT_END)
                    .build();
    private static final DimensionBuilders.DpProp DP_PROP =
            new DimensionBuilders.DpProp.Builder(10)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))
                    .build();
    private static final DimensionBuilders.ExpandedDimensionProp EXPAND_PROP = expand();
    private static final DimensionBuilders.ExpandedDimensionProp EXPAND_WEIGHT_PROP = weight(12);

    private static final DimensionBuilders.HorizontalLayoutConstraint HORIZONTAL_LAYOUT_CONSTRAINT =
            new DimensionBuilders.HorizontalLayoutConstraint.Builder(20)
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                    .build();
    private static final DimensionBuilders.VerticalLayoutConstraint VERTICAL_LAYOUT_CONSTRAINT =
            new DimensionBuilders.VerticalLayoutConstraint.Builder(20)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                    .build();
    private static final TypeBuilders.StringProp STATIC_STRING_PROP =
            new TypeBuilders.StringProp.Builder("string").build();
    private static final TypeBuilders.StringProp STRING_PROP =
            new TypeBuilders.StringProp.Builder("string")
                    .setDynamicValue(
                            DynamicBuilders.DynamicString.from(new AppDataKey<>(STATE_KEY)))
                    .build();
    private static final TypeBuilders.StringLayoutConstraint STRING_LAYOUT_CONSTRAINT =
            new TypeBuilders.StringLayoutConstraint.Builder("pattern")
                    .setAlignment(LayoutElementBuilders.TEXT_ALIGN_END)
                    .build();

    @Test
    public void testArcLineSetLength() {
        LayoutElementBuilders.ArcLine arcLine =
                new LayoutElementBuilders.ArcLine.Builder()
                        .setLength(DEGREES_PROP)
                        .setLayoutConstraintsForDynamicLength(DEGREES_PROP_CONSTRAINT)
                        .build();

        DimensionProto.DegreesProp lengthProto = arcLine.toProto().getLength();

        assertThat(lengthProto.getValue()).isEqualTo(DEGREES_PROP.getValue());
        assertThat(lengthProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
        assertThat(lengthProto.getValueForLayout()).isEqualTo(DEGREES_PROP_CONSTRAINT.getValue());
        assertThat(lengthProto.getAngularAlignmentForLayoutValue())
                .isEqualTo(DEGREES_PROP_CONSTRAINT.getAngularAlignment());
    }

    @Test
    public void testArcLineSetStrokeCap() {
        LayoutElementBuilders.ArcLine arcLine =
                new LayoutElementBuilders.ArcLine.Builder()
                        .setStrokeCap(LayoutElementBuilders.STROKE_CAP_BUTT)
                        .build();

        LayoutElementProto.StrokeCapProp strokeCapProp = arcLine.toProto().getStrokeCap();

        assertThat(strokeCapProp.getValue())
                .isEqualTo(LayoutElementProto.StrokeCap.STROKE_CAP_BUTT);
    }

    @Test
    public void testArcLineSetStrokeCapProp() {
        LayoutElementBuilders.ArcLine arcLine =
                new LayoutElementBuilders.ArcLine.Builder()
                        .setStrokeCap(
                                new LayoutElementBuilders.StrokeCapProp.Builder()
                                        .setValue(LayoutElementBuilders.STROKE_CAP_BUTT)
                                        .build())
                        .build();

        LayoutElementProto.StrokeCapProp strokeCapProp = arcLine.toProto().getStrokeCap();

        assertThat(strokeCapProp.getValue())
                .isEqualTo(LayoutElementProto.StrokeCap.STROKE_CAP_BUTT);
    }

    @Test
    public void arcLineSetLength_withoutLayoutConstraint_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LayoutElementBuilders.ArcLine.Builder().setLength(DEGREES_PROP).build());
    }

    @Test
    public void testArcSetAnchorAngle() {
        LayoutElementBuilders.Arc arc =
                new LayoutElementBuilders.Arc.Builder().setAnchorAngle(DEGREES_PROP).build();

        DimensionProto.DegreesProp anchorAngleProto = arc.toProto().getAnchorAngle();

        assertThat(anchorAngleProto.getValue()).isEqualTo(DEGREES_PROP.getValue());
        assertThat(anchorAngleProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void testSpacerSetWidthLinear() {
        LayoutElementBuilders.Spacer spacer =
                new LayoutElementBuilders.Spacer.Builder()
                        .setWidth(DP_PROP)
                        .setLayoutConstraintsForDynamicWidth(HORIZONTAL_LAYOUT_CONSTRAINT)
                        .build();

        DimensionProto.DpProp spacerWidth = spacer.toProto().getWidth().getLinearDimension();

        assertThat(spacerWidth.getValue()).isEqualTo(DP_PROP.getValue());
        assertThat(spacerWidth.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
        assertThat(spacerWidth.getValueForLayout())
                .isEqualTo(HORIZONTAL_LAYOUT_CONSTRAINT.getValue());
        assertThat(spacerWidth.getHorizontalAlignmentForLayoutValue())
                .isEqualTo(HORIZONTAL_LAYOUT_CONSTRAINT.getHorizontalAlignment());
    }

    @Test
    public void spacerSetWidth_withoutLayoutConstraint_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LayoutElementBuilders.Spacer.Builder().setWidth(DP_PROP).build());
    }

    @Test
    public void testSpacerSetHeightLinear() {
        LayoutElementBuilders.Spacer spacer =
                new LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DP_PROP)
                        .setLayoutConstraintsForDynamicHeight(VERTICAL_LAYOUT_CONSTRAINT)
                        .build();

        DimensionProto.DpProp spacerHeight = spacer.toProto().getHeight().getLinearDimension();

        assertThat(spacerHeight.getValue()).isEqualTo(DP_PROP.getValue());
        assertThat(spacerHeight.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
        assertThat(spacerHeight.getValueForLayout())
                .isEqualTo(VERTICAL_LAYOUT_CONSTRAINT.getValue());
        assertThat(spacerHeight.getVerticalAlignmentForLayoutValue())
                .isEqualTo(VERTICAL_LAYOUT_CONSTRAINT.getVerticalAlignment());
    }

    @Test
    public void spacerSetHeight_withoutLayoutConstraint_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LayoutElementBuilders.Spacer.Builder().setHeight(DP_PROP).build());
    }

    @Test
    public void testSpacerSetWidthSetHeightExpand() {
        LayoutElementBuilders.Spacer spacer =
                new LayoutElementBuilders.Spacer.Builder()
                        .setWidth(EXPAND_PROP)
                        .setHeight(EXPAND_PROP)
                        .build();

        LayoutElementProto.Spacer spacerProto = spacer.toProto();

        assertThat(spacerProto.getWidth().hasLinearDimension()).isFalse();
        assertThat(spacerProto.getHeight().hasLinearDimension()).isFalse();
        assertThat(spacerProto.getWidth().hasExpandedDimension()).isTrue();
        assertThat(spacerProto.getHeight().hasExpandedDimension()).isTrue();
    }

    @Test
    public void testSpacerSetWidthSetHeightExpandWithWeight() {
        LayoutElementBuilders.Spacer spacer =
                new LayoutElementBuilders.Spacer.Builder()
                        .setWidth(EXPAND_WEIGHT_PROP)
                        .setHeight(EXPAND_WEIGHT_PROP)
                        .build();

        LayoutElementProto.Spacer spacerProto = spacer.toProto();

        assertThat(spacerProto.getWidth().hasLinearDimension()).isFalse();
        assertThat(spacerProto.getHeight().hasLinearDimension()).isFalse();
        assertThat(spacerProto.getWidth().hasExpandedDimension()).isTrue();
        assertThat(spacerProto.getHeight().hasExpandedDimension()).isTrue();

        DimensionProto.ExpandedDimensionProp spacerWidth =
                spacer.toProto().getWidth().getExpandedDimension();
        DimensionProto.ExpandedDimensionProp spacerHeight =
                spacer.toProto().getHeight().getExpandedDimension();

        assertThat(spacerWidth.getLayoutWeight().getValue())
                .isEqualTo(EXPAND_WEIGHT_PROP.getLayoutWeight().getValue());
        assertThat(spacerHeight.getLayoutWeight().getValue())
                .isEqualTo(EXPAND_WEIGHT_PROP.getLayoutWeight().getValue());
    }

    @Test
    public void text_defaultExcludeFontPadding() {
        String staticValue = "Text";
        // We don't set anything related to the font padding to test that default value is true.
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(new TypeBuilders.StringProp.Builder(staticValue).build())
                        .build();

        assertThat(text.toProto().getAndroidTextStyle().getExcludeFontPadding()).isTrue();
    }

    @Test
    public void testTextSetText() {
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(STRING_PROP)
                        .setLayoutConstraintsForDynamicText(STRING_LAYOUT_CONSTRAINT)
                        .build();

        TypesProto.StringProp textProto = text.toProto().getText();

        assertThat(textProto.getValue()).isEqualTo(STRING_PROP.getValue());
        assertThat(textProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
        assertThat(textProto.getValueForLayout())
                .isEqualTo(STRING_LAYOUT_CONSTRAINT.getPatternForLayout());
        assertThat(textProto.getTextAlignmentForLayoutValue())
                .isEqualTo(STRING_LAYOUT_CONSTRAINT.getAlignment());
    }

    @Test
    public void testTextSetOverflow_ellipsize() {
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(STATIC_STRING_PROP)
                        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                        .build();

        LayoutElementProto.Text textProto = text.toProto();

        assertThat(textProto.getText().getValue()).isEqualTo(STATIC_STRING_PROP.getValue());
        assertThat(textProto.getOverflow().getValue().getNumber())
                .isEqualTo(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTextSetOverflow_ellipsizeEnd() {
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(STATIC_STRING_PROP)
                        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                        .build();

        LayoutElementProto.Text textProto = text.toProto();

        assertThat(textProto.getText().getValue()).isEqualTo(STATIC_STRING_PROP.getValue());
        assertThat(textProto.getOverflow().getValue().getNumber())
                .isEqualTo(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END);
    }

    @Test
    public void testTextSetOverflow_truncate() {
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(STATIC_STRING_PROP)
                        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_TRUNCATE)
                        .build();

        LayoutElementProto.Text textProto = text.toProto();

        assertThat(textProto.getText().getValue()).isEqualTo(STATIC_STRING_PROP.getValue());
        assertThat(textProto.getOverflow().getValue().getNumber())
                .isEqualTo(LayoutElementBuilders.TEXT_OVERFLOW_TRUNCATE);
    }

    @Test
    public void testTextSetOverflow_marquee() {
        LayoutElementBuilders.Text text =
                new LayoutElementBuilders.Text.Builder()
                        .setText(STATIC_STRING_PROP)
                        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE)
                        .build();

        LayoutElementProto.Text textProto = text.toProto();

        assertThat(textProto.getText().getValue()).isEqualTo(STATIC_STRING_PROP.getValue());
        assertThat(textProto.getOverflow().getValue().getNumber())
                .isEqualTo(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE);
    }

    @Test
    public void testFontStyleSetMultipleSizes() {
        int size1 = 12;
        int size2 = 30;
        int lastSize = 20;
        int[] expectedSizes = {size1, size2, lastSize};
        LayoutElementBuilders.FontStyle fontStyle =
                new LayoutElementBuilders.FontStyle.Builder()
                        .setSizes(size1, size2, lastSize)
                        .build();

        LayoutElementProto.FontStyle fontStyleProto = fontStyle.toProto();

        assertThat(
                        fontStyleProto.getSizeList().stream()
                                .mapToInt(sp -> (int) sp.getValue())
                                .toArray())
                .isEqualTo(expectedSizes);
        // Make sure that if 1 size is used than it's the last one.
        assertThat(fontStyle.getSize().getValue()).isEqualTo(lastSize);
        assertThat(fontStyle.getSizes().stream().mapToInt(sp -> (int) sp.getValue()).toArray())
                .isEqualTo(expectedSizes);
    }

    @Test
    public void testFontStyleSetSize_moreTimes_usesLastOne() {
        int lastSize = 20;
        LayoutElementBuilders.FontStyle fontStyle =
                new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(12))
                        .setSize(sp(30))
                        .setSize(sp(lastSize))
                        .build();

        LayoutElementProto.FontStyle fontStyleProto = fontStyle.toProto();

        assertThat(fontStyleProto.getSizeList()).hasSize(1);
        assertThat(fontStyleProto.getSizeList().get(0).getValue()).isEqualTo(lastSize);
        // Make sure that if 1 size is used than it's the last one.
        assertThat(fontStyle.getSize().getValue()).isEqualTo(lastSize);
        assertThat(fontStyleProto.getSizeList()).hasSize(1);
        assertThat(fontStyle.getSizes().get(0).getValue()).isEqualTo(lastSize);
    }

    @Test
    public void testFontStyleSetSize_setSizes_overrides() {
        int size1 = 12;
        int size2 = 30;
        int[] expectedSizes = {size1, size2};
        LayoutElementBuilders.FontStyle fontStyle =
                new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(20))
                        .setSizes(size1, size2)
                        .build();

        LayoutElementProto.FontStyle fontStyleProto = fontStyle.toProto();

        assertThat(
                        fontStyleProto.getSizeList().stream()
                                .mapToInt(sp -> (int) sp.getValue())
                                .toArray())
                .isEqualTo(expectedSizes);
        // Make sure that if 1 size is used than it's the last one.
        assertThat(fontStyle.getSize().getValue()).isEqualTo(size2);
        assertThat(fontStyle.getSizes().stream().mapToInt(sp -> (int) sp.getValue()).toArray())
                .isEqualTo(expectedSizes);
    }

    @Test
    public void testFontStyleSetSize_tooManySizes_throws() {
        int[] sizes =
                new int[LayoutElementBuilders.FontStyle.Builder.TEXT_SIZES_LIMIT + 1];
        assertThrows(
                IllegalArgumentException.class,
                () -> new LayoutElementBuilders.FontStyle.Builder().setSizes(sizes).build());
    }

    @Test
    public void testFontStyleSetSize_atLeastOneNegative_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new LayoutElementBuilders.FontStyle.Builder()
                                .setSizes(-1, 5, 1)
                                .build());
    }

    @Test
    public void testFontStyleSetSize_atLeastOneZero_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new LayoutElementBuilders.FontStyle.Builder()
                                .setSizes(1, 2, 0)
                                .build());
    }

    @Test
    public void textSetText_withoutLayoutConstraint_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LayoutElementBuilders.Text.Builder().setText(STRING_PROP).build());
    }

    @Test
    public void arcLineSetStrokeCap_withShadow() {
        float blurRadius = 5f;
        int color = Color.BLUE;
        ModifiersBuilders.Shadow shadow =
                new ModifiersBuilders.Shadow.Builder()
                        .setBlurRadius(dp(blurRadius))
                        .setColor(argb(color))
                        .build();

        LayoutElementBuilders.ArcLine arcLine =
                new LayoutElementBuilders.ArcLine.Builder()
                        .setStrokeCap(
                                new LayoutElementBuilders.StrokeCapProp.Builder()
                                        .setShadow(shadow)
                                        .build())
                        .build();

        assertThat(arcLine.getStrokeCap().getShadow().getBlurRadius().getValue())
                .isEqualTo(shadow.getBlurRadius().getValue());
        assertThat(arcLine.getStrokeCap().getShadow().getColor().getArgb())
                .isEqualTo(shadow.getColor().getArgb());
    }

    @Test
    public void arcs_withSetDirection() {
        int arcLineDirection = LayoutElementBuilders.ARC_DIRECTION_COUNTER_CLOCKWISE;
        int arcTextDirection = LayoutElementBuilders.ARC_DIRECTION_NORMAL;
        int arcDirection = LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE;

        LayoutElementBuilders.Arc arc = new LayoutElementBuilders.Arc.Builder()
                .setArcDirection(arcDirection)
                .addContent(
                        new LayoutElementBuilders.ArcLine.Builder()
                                .setArcDirection(arcLineDirection)
                                .build())
                .addContent(
                        new LayoutElementBuilders.ArcText.Builder()
                                .setArcDirection(arcTextDirection)
                                .build())
                .build();

        assertThat(arc.getArcDirection().getValue()).isEqualTo(arcDirection);
        assertThat(
                ((LayoutElementBuilders.ArcLine) arc.getContents().get(0))
                        .getArcDirection().getValue())
                .isEqualTo(arcLineDirection);
        assertThat(
                ((LayoutElementBuilders.ArcText) arc.getContents().get(1))
                        .getArcDirection().getValue())
                .isEqualTo(arcTextDirection);
    }
}
