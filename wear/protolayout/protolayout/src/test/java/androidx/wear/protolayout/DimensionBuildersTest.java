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

import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.weight;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.DimensionProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DimensionBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final DimensionBuilders.DpProp DP_PROP =
            new DimensionBuilders.DpProp.Builder(3.14f)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    private static final DimensionBuilders.DegreesProp DEGREES_PROP =
            new DimensionBuilders.DegreesProp.Builder(3.14f)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    @Test
    public void dpPropSupportsDynamicValue() {
        DimensionProto.DpProp dpPropProto = DP_PROP.toProto();

        assertThat(dpPropProto.getValue()).isEqualTo(DP_PROP.getValue());
        assertThat(dpPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void dpProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, new DimensionBuilders.DpProp.Builder()
                .setDynamicValue(
                        DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))::build);
    }

    @Test
    public void degreesPropSupportsDynamicValue() {
        DimensionProto.DegreesProp degreesPropProto = DEGREES_PROP.toProto();

        assertThat(degreesPropProto.getValue()).isEqualTo(DEGREES_PROP.getValue());
        assertThat(degreesPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void degreesProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, new DimensionBuilders.DegreesProp.Builder()
                .setDynamicValue(
                        DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))::build);
    }

    @Test
    public void expandedLayoutWeight() {
        float layoutWeight = 3.14f;
        DimensionBuilders.ContainerDimension dimensionProp = weight(layoutWeight);

        DimensionProto.ContainerDimension dimensionProto =
                dimensionProp.toContainerDimensionProto();
        assertThat(dimensionProto.getExpandedDimension().getLayoutWeight().getValue())
                .isWithin(.001f)
                .of(layoutWeight);
    }

    @Test
    public void wrappedMinSize() {
        DimensionBuilders.DpProp minSize = dp(42);
        DimensionBuilders.ContainerDimension dimensionProp =
                new DimensionBuilders.WrappedDimensionProp.Builder()
                        .setMinimumSize(minSize)
                        .build();

        DimensionProto.ContainerDimension dimensionProto =
                dimensionProp.toContainerDimensionProto();
        assertThat(dimensionProto.getWrappedDimension().getMinimumSize().getValue())
                .isEqualTo(minSize.getValue());
    }

    @Test
    public void wrappedMinSize_throwsWhenSetToDynamicValue() {
        DimensionBuilders.DpProp minSizeDynamic =
                new DimensionBuilders.DpProp.Builder(42)
                        .setDynamicValue(
                                DynamicBuilders.DynamicFloat.from(new AppDataKey<>("some-state")))
                        .build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DimensionBuilders.WrappedDimensionProp.Builder()
                                .setMinimumSize(minSizeDynamic));
    }

   @Test
    public void pivotDimensionWithDpValue() {
       DimensionBuilders.PivotDimension pivotDimension =
               new DimensionBuilders.DpProp.Builder(42)
                       .setDynamicValue(
                               DynamicBuilders.DynamicFloat.from(new AppDataKey<>("some-state")))
                       .build();


       DimensionProto.PivotDimension dimensionProto = pivotDimension.toPivotDimensionProto();
       assertThat(dimensionProto.getInnerCase())
               .isEqualTo(DimensionProto.PivotDimension.InnerCase.OFFSET_DP);
       assertThat(dimensionProto.getOffsetDp().getValue())
               .isEqualTo(42);
       assertThat(dimensionProto.getOffsetDp().getDynamicValue().getStateSource().getSourceKey())
               .isEqualTo("some-state");
   }

    @Test
    public void pivotDimensionWithBoundingBoxRatio() {
        DimensionBuilders.PivotDimension pivotDimension =
                new DimensionBuilders.BoundingBoxRatio.Builder(
                        new TypeBuilders.FloatProp.Builder(0.8f)
                                .setDynamicValue(
                                        DynamicBuilders.DynamicFloat.constant(0.2f))
                                .build())
                        .build();


        DimensionProto.PivotDimension dimensionProto = pivotDimension.toPivotDimensionProto();
        assertThat(dimensionProto.getInnerCase())
                .isEqualTo(DimensionProto.PivotDimension.InnerCase.LOCATION_RATIO);
        assertThat(dimensionProto.getLocationRatio().getRatio().getValue())
                .isEqualTo(0.8f);
        assertThat(dimensionProto.getLocationRatio()
                .getRatio().getDynamicValue().getFixed().getValue())
                .isEqualTo(0.2f);
    }

}
