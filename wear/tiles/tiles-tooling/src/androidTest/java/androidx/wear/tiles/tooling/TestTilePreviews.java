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

package androidx.wear.tiles.tooling;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.tiles.tooling.preview.TilePreviewHelper.singleTimelineEntryTileBuilder;

import android.content.Context;

import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle;
import androidx.wear.protolayout.LayoutElementBuilders.Layout;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Text;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.protolayout.TimelineBuilders.Timeline;
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry;
import androidx.wear.protolayout.TypeBuilders;
import androidx.wear.protolayout.expression.AnimationParameterBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.PlatformHealthSources;
import androidx.wear.protolayout.material.CircularProgressIndicator;
import androidx.wear.protolayout.material.Colors;
import androidx.wear.protolayout.material.ProgressIndicatorColors;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.tooling.preview.Preview;
import androidx.wear.tiles.tooling.preview.TilePreviewData;

public class TestTilePreviews {
    private static final String RESOURCES_VERSION = "1";
    private static final Resources RESOURCES =
            new Resources.Builder().setVersion(RESOURCES_VERSION).build();

    private static LayoutElement layoutElement() {
        return new Text.Builder()
                .setText("Hello world!")
                .setFontStyle(new FontStyle.Builder().setColor(argb(0xFF000000)).build())
                .build();
    }

    private static Layout layout() {
        return new Layout.Builder().setRoot(layoutElement()).build();
    }

    private static Tile tile() {
        return new Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(
                        new Timeline.Builder()
                                .addTimelineEntry(
                                        new TimelineEntry.Builder().setLayout(layout()).build())
                                .build())
                .build();
    }

    /** Declaration of a static tile preview method */
    @Preview
    public static TilePreviewData tilePreview() {
        return new TilePreviewData((request) -> RESOURCES, (request) -> tile());
    }

    @Preview
    static TilePreviewData tileLayoutPreview() {
        return new TilePreviewData((request) -> singleTimelineEntryTileBuilder(layout()).build());
    }

    @Preview
    static TilePreviewData tileLayoutElementPreview() {
        return new TilePreviewData(
                (request) -> singleTimelineEntryTileBuilder(layoutElement()).build());
    }

    @Preview
    private static TilePreviewData tilePreviewWithPrivateVisibility() {
        return new TilePreviewData((request) -> tile());
    }

    static int duplicateFunctionName(int x) {
        return x;
    }

    @Preview
    static TilePreviewData duplicateFunctionName() {
        return new TilePreviewData((request) -> tile());
    }

    @Preview
    static TilePreviewData tilePreviewWithContextParameter(Context context) {
        return new TilePreviewData((request) -> tile());
    }

    @Preview
    static void tilePreviewWithWrongReturnType() {}

    @Preview
    static TilePreviewData tilePreviewWithNonContextParameter(int i) {
        return new TilePreviewData((request) -> tile());
    }

    @Preview
    TilePreviewData nonStaticMethod() {
        return new TilePreviewData((request) -> tile());
    }

    private static LayoutElementBuilders.Text heartRateText() {
        return new LayoutElementBuilders.Text.Builder()
                .setText(
                        new TypeBuilders.StringProp.Builder("--")
                                .setDynamicValue(PlatformHealthSources.heartRateBpm().format())
                                .build())
                .setLayoutConstraintsForDynamicText(
                        new TypeBuilders.StringLayoutConstraint.Builder("XX")
                                .setAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                                .build())
                .setFontStyle(
                        new LayoutElementBuilders.FontStyle.Builder()
                                .setColor(argb(0xFF000000))
                                .build())
                .build();
    }

    private static Tile tileWithPlatformData() {
        return new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(
                        new TimelineBuilders.Timeline.Builder()
                                .addTimelineEntry(
                                        new TimelineBuilders.TimelineEntry.Builder()
                                                .setLayout(
                                                        new LayoutElementBuilders.Layout.Builder()
                                                                .setRoot(heartRateText())
                                                                .build())
                                                .build())
                                .build())
                .build();
    }

    @Preview
    static TilePreviewData tilePreviewWithDefaultPlatformData() {
        return new TilePreviewData((request) -> tileWithPlatformData());
    }

    @Preview
    static TilePreviewData tilePreviewWithOverriddenPlatformData() {
        PlatformDataValues platformDataValues =
                PlatformDataValues.of(
                        PlatformHealthSources.Keys.HEART_RATE_BPM,
                        DynamicDataValue.fromFloat(180f));
        return new TilePreviewData(
                (request) -> RESOURCES, platformDataValues, (request) -> tileWithPlatformData());
    }

    private static final float START_VALUE = 15f;
    private static final float END_VALUE = 105f;
    private static final long ANIMATION_DURATION_IN_MILLIS = 2000L; // 2 seconds

    private static Tile tileWithAnimations() {
        AnimationParameterBuilders.AnimationSpec.Builder animationSpecBuilder =
                new AnimationParameterBuilders.AnimationSpec.Builder()
                        .setAnimationParameters(
                                new AnimationParameterBuilders.AnimationParameters.Builder()
                                        .setDurationMillis(ANIMATION_DURATION_IN_MILLIS)
                                        .build());
        AnimationParameterBuilders.AnimationSpec animationSpec = animationSpecBuilder.build();

        DynamicBuilders.DynamicFloat floatAnimation =
                DynamicBuilders.DynamicFloat.animate(START_VALUE, END_VALUE, animationSpec);
        DynamicBuilders.DynamicColor colorAnimation =
                DynamicBuilders.DynamicColor.animate(Colors.PRIMARY, Colors.SURFACE, animationSpec);

        CircularProgressIndicator.Builder circularProgressIndicatorBuilder =
                new CircularProgressIndicator.Builder()
                        .setProgress(
                                new TypeBuilders.FloatProp.Builder(0.25f)
                                        .setDynamicValue(floatAnimation)
                                        .build())
                        .setCircularProgressIndicatorColors(
                                new ProgressIndicatorColors(
                                        new ColorBuilders.ColorProp.Builder(-0x1)
                                                .setDynamicValue(colorAnimation)
                                                .build(),
                                        new ColorBuilders.ColorProp.Builder(-0x1).build()));
        LayoutElementBuilders.LayoutElement layoutElement =
                circularProgressIndicatorBuilder.build();

        LayoutElementBuilders.Layout.Builder layoutBuilder =
                new Layout.Builder().setRoot(layoutElement);
        Layout layout = layoutBuilder.build();

        TimelineBuilders.Timeline.Builder timelineBuilder =
                new Timeline.Builder()
                        .addTimelineEntry(new TimelineEntry.Builder().setLayout(layout).build());
        Timeline timeline = timelineBuilder.build();

        TileBuilders.Tile.Builder tileBuilder =
                new Tile.Builder().setResourcesVersion(RESOURCES_VERSION).setTileTimeline(timeline);
        return tileBuilder.build();
    }

    @Preview
    static TilePreviewData testGetAnimations() {
        return new TilePreviewData((request) -> tileWithAnimations());
    }

    private static Tile tileWithAnimationDependedOnAnotherAnimation() {
        AnimationParameterBuilders.AnimationSpec.Builder animationSpecBuilder =
                new AnimationParameterBuilders.AnimationSpec.Builder()
                        .setAnimationParameters(
                                new AnimationParameterBuilders.AnimationParameters.Builder()
                                        .setDurationMillis(ANIMATION_DURATION_IN_MILLIS)
                                        .build());
        AnimationParameterBuilders.AnimationSpec animationSpec = animationSpecBuilder.build();

        DynamicBuilders.DynamicFloat floatAnimation =
                DynamicBuilders.DynamicFloat.animate(START_VALUE, END_VALUE, animationSpec)
                        .times(100)
                        .animate(animationSpec);

        CircularProgressIndicator.Builder circularProgressIndicatorBuilder =
                new CircularProgressIndicator.Builder()
                        .setProgress(
                                new TypeBuilders.FloatProp.Builder(0.25f)
                                        .setDynamicValue(floatAnimation)
                                        .build());
        LayoutElementBuilders.LayoutElement layoutElement =
                circularProgressIndicatorBuilder.build();

        Layout layout = new Layout.Builder().setRoot(layoutElement).build();

        Timeline timeline =
                new Timeline.Builder()
                        .addTimelineEntry(new TimelineEntry.Builder().setLayout(layout).build())
                        .build();

        TileBuilders.Tile.Builder tileBuilder =
                new Tile.Builder().setResourcesVersion(RESOURCES_VERSION).setTileTimeline(timeline);
        return tileBuilder.build();
    }

    @Preview
    static TilePreviewData testGetTerminalAndNotTerminalAnimation() {
        return new TilePreviewData((request) -> tileWithAnimationDependedOnAnotherAnimation());
    }

    private static Tile tileAnimationsWithCondition() {
        DynamicBuilders.DynamicFloat dynamicFloat = DynamicBuilders.DynamicFloat.animate(1, 10);

        DynamicBuilders.DynamicFloat floatAnimation =
                DynamicBuilders.DynamicFloat.onCondition(dynamicFloat.gt(5f))
                        .use(dynamicFloat)
                        .elseUse(100f);

        CircularProgressIndicator.Builder circularProgressIndicatorBuilder =
                new CircularProgressIndicator.Builder()
                        .setProgress(
                                new TypeBuilders.FloatProp.Builder(0.25f)
                                        .setDynamicValue(floatAnimation)
                                        .build());
        LayoutElementBuilders.LayoutElement layoutElement =
                circularProgressIndicatorBuilder.build();

        Layout layout = new Layout.Builder().setRoot(layoutElement).build();

        Timeline timeline =
                new Timeline.Builder()
                        .addTimelineEntry(new TimelineEntry.Builder().setLayout(layout).build())
                        .build();

        TileBuilders.Tile.Builder tileBuilder =
                new Tile.Builder().setResourcesVersion(RESOURCES_VERSION).setTileTimeline(timeline);
        return tileBuilder.build();
    }

    @Preview
    static TilePreviewData testGetAnimationsWithCondition() {
        return new TilePreviewData((request) -> tileAnimationsWithCondition());
    }
}
