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

package androidx.wear.protolayout.renderer.inflater;

import static android.os.Looper.getMainLooper;
import static android.view.View.INVISIBLE;
import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.VISIBLE;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection.ARC_DIRECTION_CLOCKWISE;
import static androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection.ARC_DIRECTION_COUNTER_CLOCKWISE;
import static androidx.wear.protolayout.proto.ModifiersProto.SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_INSIDE;
import static androidx.wear.protolayout.proto.ModifiersProto.SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_OUTSIDE;
import static androidx.wear.protolayout.renderer.R.id.clickable_id_tag;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arc;
import static androidx.wear.protolayout.renderer.helper.TestDsl.arcText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.box;
import static androidx.wear.protolayout.renderer.helper.TestDsl.column;
import static androidx.wear.protolayout.renderer.helper.TestDsl.dynamicFixedText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.image;
import static androidx.wear.protolayout.renderer.helper.TestDsl.layout;
import static androidx.wear.protolayout.renderer.helper.TestDsl.row;
import static androidx.wear.protolayout.renderer.helper.TestDsl.text;
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.DEFAULT_MIN_CLICKABLE_SIZE_DP;
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.TEXT_AUTOSIZES_LIMIT;
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.getFrameLayoutGravity;
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.getRenderedMetadata;
import static androidx.wear.protolayout.renderer.test.R.drawable.android_animated_24dp;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils.TruncateAt;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32ToFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateBoolSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateColorSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateStringSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.proto.ActionProto.Action;
import androidx.wear.protolayout.proto.ActionProto.AndroidActivity;
import androidx.wear.protolayout.proto.ActionProto.AndroidBooleanExtra;
import androidx.wear.protolayout.proto.ActionProto.AndroidDoubleExtra;
import androidx.wear.protolayout.proto.ActionProto.AndroidExtra;
import androidx.wear.protolayout.proto.ActionProto.AndroidIntExtra;
import androidx.wear.protolayout.proto.ActionProto.AndroidLongExtra;
import androidx.wear.protolayout.proto.ActionProto.AndroidStringExtra;
import androidx.wear.protolayout.proto.ActionProto.LaunchAction;
import androidx.wear.protolayout.proto.ActionProto.LoadAction;
import androidx.wear.protolayout.proto.ActionProto.PendingIntentAction;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignmentProp;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignmentProp;
import androidx.wear.protolayout.proto.ColorProto.Brush;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.ColorProto.ColorStop;
import androidx.wear.protolayout.proto.ColorProto.SweepGradient;
import androidx.wear.protolayout.proto.DimensionProto;
import androidx.wear.protolayout.proto.DimensionProto.AngularDimension;
import androidx.wear.protolayout.proto.DimensionProto.ArcLineLength;
import androidx.wear.protolayout.proto.DimensionProto.BoundingBoxRatio;
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedAngularDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ExtensionDimension;
import androidx.wear.protolayout.proto.DimensionProto.ImageDimension;
import androidx.wear.protolayout.proto.DimensionProto.PivotDimension;
import androidx.wear.protolayout.proto.DimensionProto.ProportionalDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.SpacerDimension;
import androidx.wear.protolayout.proto.DimensionProto.WrappedDimensionProp;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.LayoutElementProto.Arc;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcAdapter;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirectionProp;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLine;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcSpacer;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcText;
import androidx.wear.protolayout.proto.LayoutElementProto.Box;
import androidx.wear.protolayout.proto.LayoutElementProto.ColorFilter;
import androidx.wear.protolayout.proto.LayoutElementProto.Column;
import androidx.wear.protolayout.proto.LayoutElementProto.DashedArcLine;
import androidx.wear.protolayout.proto.LayoutElementProto.ExtensionLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.FontFeatureSetting;
import androidx.wear.protolayout.proto.LayoutElementProto.FontSetting;
import androidx.wear.protolayout.proto.LayoutElementProto.FontStyle;
import androidx.wear.protolayout.proto.LayoutElementProto.Image;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.MarqueeParameters;
import androidx.wear.protolayout.proto.LayoutElementProto.Row;
import androidx.wear.protolayout.proto.LayoutElementProto.Spacer;
import androidx.wear.protolayout.proto.LayoutElementProto.Span;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanImage;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanText;
import androidx.wear.protolayout.proto.LayoutElementProto.Spannable;
import androidx.wear.protolayout.proto.LayoutElementProto.StrokeCapProp;
import androidx.wear.protolayout.proto.LayoutElementProto.Text;
import androidx.wear.protolayout.proto.LayoutElementProto.TextOverflow;
import androidx.wear.protolayout.proto.LayoutElementProto.TextOverflowProp;
import androidx.wear.protolayout.proto.ModifiersProto;
import androidx.wear.protolayout.proto.ModifiersProto.AnimatedVisibility;
import androidx.wear.protolayout.proto.ModifiersProto.Border;
import androidx.wear.protolayout.proto.ModifiersProto.Clickable;
import androidx.wear.protolayout.proto.ModifiersProto.Corner;
import androidx.wear.protolayout.proto.ModifiersProto.CornerRadius;
import androidx.wear.protolayout.proto.ModifiersProto.EnterTransition;
import androidx.wear.protolayout.proto.ModifiersProto.ExitTransition;
import androidx.wear.protolayout.proto.ModifiersProto.FadeInTransition;
import androidx.wear.protolayout.proto.ModifiersProto.FadeOutTransition;
import androidx.wear.protolayout.proto.ModifiersProto.Modifiers;
import androidx.wear.protolayout.proto.ModifiersProto.Padding;
import androidx.wear.protolayout.proto.ModifiersProto.Semantics;
import androidx.wear.protolayout.proto.ModifiersProto.SemanticsRole;
import androidx.wear.protolayout.proto.ModifiersProto.SlideBound;
import androidx.wear.protolayout.proto.ModifiersProto.SlideDirection;
import androidx.wear.protolayout.proto.ModifiersProto.SlideInTransition;
import androidx.wear.protolayout.proto.ModifiersProto.SlideParentBound;
import androidx.wear.protolayout.proto.ModifiersProto.SlideParentSnapOption;
import androidx.wear.protolayout.proto.ModifiersProto.SpanModifiers;
import androidx.wear.protolayout.proto.ResourceProto.AndroidAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.ImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.ImageResource;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.proto.ResourceProto.Resources;
import androidx.wear.protolayout.proto.StateProto.State;
import androidx.wear.protolayout.proto.TriggerProto.OnVisibleTrigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TypesProto.BoolProp;
import androidx.wear.protolayout.proto.TypesProto.FloatProp;
import androidx.wear.protolayout.proto.TypesProto.Int32Prop;
import androidx.wear.protolayout.proto.TypesProto.StringProp;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme;
import androidx.wear.protolayout.renderer.common.RenderingArtifact;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline;
import androidx.wear.protolayout.renderer.helper.TestFingerprinter;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.InflateResult;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewGroupMutation;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewMutationException;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.ViewProperties;
import androidx.wear.protolayout.renderer.test.R;
import androidx.wear.widget.ArcLayout;
import androidx.wear.widget.CurvedTextView;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ProtoLayoutInflaterTest {
    private static final String TEST_CLICKABLE_CLASS_NAME = "Hello";
    private static final String TEST_CLICKABLE_PACKAGE_NAME = "World";
    private static final String EXTRA_CLICKABLE_ID = "extra.clickable.id";

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 400;
    private static final int DEFAULT_WEIGHT = 1;

    @Rule public final Expect expect = Expect.create();

    private final StateStore mStateStore = new StateStore(ImmutableMap.of());
    private ProtoLayoutDynamicDataPipeline mDataPipeline;

    @After
    public void tearDown() {
        Renderer.cleanUp();
    }

    @Test
    public void inflate_textView() {
        String textContents = "Hello World";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(Text.newBuilder().setText(string(textContents)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Text pulled from the proto.
        expect.that(tv.getText().toString()).isEqualTo(textContents);
    }

    @Test
    public void inflate_textView_withColor() {
        int color = 0xFF112233;
        String textContents = "Hello World";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setFontStyle(
                                                FontStyle.newBuilder()
                                                        .setColor(
                                                                ColorProp.newBuilder()
                                                                        .setArgb(color))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getTextColors().getDefaultColor()).isEqualTo(color);
    }

    @Test
    public void inflate_textView_withoutText() {
        LayoutElement root = LayoutElement.newBuilder().setText(Text.getDefaultInstance()).build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getText().toString()).isEmpty();
    }

    @Test
    public void inflate_textView_withEmptyValueForLayout() {
        StringProp stringProp =
                StringProp.newBuilder()
                        .setValue("abcde")
                        .setDynamicValue(
                                DynamicString.newBuilder()
                                        .setFixed(
                                                FixedString.newBuilder()
                                                        .setValue("Dynamic Fixed Text")))
                        .setValueForLayout("")
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder().setText(Text.newBuilder().setText(stringProp)).build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getWidth()).isGreaterThan(0);
    }

    // obsoleteContentDescription is tested for backward compatibility
    @SuppressWarnings("deprecation")
    @Test
    public void inflate_textView_withObsoleteSemanticsContentDescription() {
        String textContents = "Hello World";
        String textDescription = "Hello World Text Element";
        Semantics semantics =
                Semantics.newBuilder().setObsoleteContentDescription(textDescription).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Check the text contents.
        assertThat(tv.getText().toString()).isEqualTo(textContents);

        // Check the accessibility label.
        AccessibilityNodeInfoCompat info =
                AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(info.getContentDescription().toString()).isEqualTo(textDescription);
        assertThat(info.isImportantForAccessibility()).isTrue();
        assertThat(tv.isImportantForAccessibility()).isTrue();
        assertThat(info.isFocusable()).isTrue();
    }

    // obsoleteContentDescription is tested for backward compatibility
    @SuppressWarnings("deprecation")
    @Test
    @Config(minSdk = VERSION_CODES.P) // android.view.View#isAccessibilityHeading API requirement
    public void inflate_textView_withSemanticsContentDescription() {
        String textContents = "Hello World";
        String staticDescription = "StaticDescription";

        StringProp descriptionProp = string(staticDescription).build();
        Semantics semantics =
                Semantics.newBuilder()
                        .setObsoleteContentDescription("ObsoleteContentDescription")
                        .setContentDescription(descriptionProp)
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Check the text contents.
        assertThat(tv.getText().toString()).isEqualTo(textContents);

        // Check the accessibility label.
        AccessibilityNodeInfoCompat info =
                AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(info.getContentDescription().toString()).isEqualTo(staticDescription);
        assertThat(info.isImportantForAccessibility()).isTrue();
        assertThat(tv.isImportantForAccessibility()).isTrue();
        assertThat(info.isFocusable()).isTrue();
        assertThat(tv.isAccessibilityHeading()).isFalse();
    }

    @Test
    public void inflate_textView_withDynamicSemanticsDescription() {
        String textContents = "Hello World";
        String staticDescription = "StaticDescription";
        String initialDynamicContentDescription = "content 1";
        String targetDynamicContentDescription = "content 2";
        String initialDynamicStateDescription = "state 1";
        String targetDynamicStateDescription = "state 2";

        StringProp contentDescriptionProp =
                string(staticDescription)
                        .setDynamicValue(
                                DynamicString.newBuilder()
                                        .setStateSource(
                                                StateStringSource.newBuilder()
                                                        .setSourceKey("content")
                                                        .build())
                                        .build())
                        .build();
        StringProp stateDescriptionProp =
                string(staticDescription)
                        .setDynamicValue(
                                DynamicString.newBuilder()
                                        .setStateSource(
                                                StateStringSource.newBuilder()
                                                        .setSourceKey("state")
                                                        .build())
                                        .build())
                        .build();

        Semantics semantics =
                Semantics.newBuilder()
                        .setStateDescription(stateDescriptionProp)
                        .setContentDescription(contentDescriptionProp)
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        AccessibilityNodeInfoCompat info =
                AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(info.isImportantForAccessibility()).isTrue();
        assertThat(tv.isImportantForAccessibility()).isTrue();
        assertThat(info.isFocusable()).isTrue();

        AppDataKey<DynamicBuilders.DynamicString> keyContent = new AppDataKey<>("content");
        AppDataKey<DynamicBuilders.DynamicString> keyState = new AppDataKey<>("state");
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyContent,
                        DynamicDataValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(initialDynamicContentDescription))
                                .build(),
                        keyState,
                        DynamicDataValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(initialDynamicStateDescription))
                                .build()));

        info = AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(mStateStore.getDynamicDataValuesProto(keyContent).getStringVal().getValue())
                .isEqualTo(initialDynamicContentDescription);
        assertThat(info.getContentDescription().toString())
                .isEqualTo(initialDynamicContentDescription);
        assertThat(info.getStateDescription().toString()).isEqualTo(initialDynamicStateDescription);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyContent,
                        DynamicDataValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(targetDynamicContentDescription))
                                .build(),
                        keyState,
                        DynamicDataValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(targetDynamicStateDescription))
                                .build()));

        info = AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(info.getContentDescription().toString())
                .isEqualTo(targetDynamicContentDescription);
        assertThat(info.getStateDescription().toString()).isEqualTo(targetDynamicStateDescription);
    }

    @Test
    @Config(minSdk = VERSION_CODES.P) // android.view.View#isAccessibilityHeading API requirement
    public void inflate_textView_withSemanticsHeading() {
        String textContents = "Heading";

        Semantics semantics = Semantics.newBuilder().setHeading(true).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Check the text contents.
        assertThat(tv.getText().toString()).isEqualTo(textContents);

        // Check the accessibility heading.
        assertThat(tv.isAccessibilityHeading()).isTrue();
    }

    @Test
    public void inflate_box_withIllegalSize() {
        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(Text.newBuilder().setText(string("foo")))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        // Outer box's width and height left at default value of
                                        // "wrap"
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setBox(
                                                                // Inner box's width set to
                                                                // "expand". Having a single
                                                                // "expand"
                                                                // element in a "wrap" element is an
                                                                // undefined state, so the outer box
                                                                // should not be displayed.
                                                                Box.newBuilder()
                                                                        .setWidth(expand())
                                                                        .addContents(textElement))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that the outer box is not displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_box_withExtensionElement() {
        int width = 10;
        int height = 12;
        byte[] payload = "Hello World".getBytes(UTF_8);
        LayoutElement extension =
                LayoutElement.newBuilder()
                        .setExtension(
                                ExtensionLayoutElement.newBuilder()
                                        .setExtensionId("foo")
                                        .setPayload(ByteString.copyFrom(payload))
                                        .setWidth(
                                                ExtensionDimension.newBuilder()
                                                        .setLinearDimension(dp(width))
                                                        .build())
                                        .setHeight(
                                                ExtensionDimension.newBuilder()
                                                        .setLinearDimension(dp(height))
                                                        .build()))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        // Outer box's width and height left at default value of
                                        // "wrap"
                                        .addContents(extension))
                        .build();

        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(root))
                                        .setExtensionViewProvider(
                                                (extensionPayload, id) -> {
                                                    TextView returnedView =
                                                            new TextView(getApplicationContext());
                                                    returnedView.setText("testing");

                                                    return returnedView;
                                                }))
                        .inflate();

        // Check that the outer box is displayed and it has a child.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);

        ViewGroup boxView = (ViewGroup) rootLayout.getChildAt(0);
        assertThat(boxView.getChildCount()).isEqualTo(1);

        assertThat(boxView.getMeasuredWidth()).isEqualTo(width);
        assertThat(boxView.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_box_withSemanticsModifier() {
        String textDescription = "this is a button";
        Semantics semantics =
                Semantics.newBuilder()
                        .setContentDescription(string(textDescription))
                        .setRole(SemanticsRole.SEMANTICS_ROLE_BUTTON)
                        .build();
        String text = "some button";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string(text))))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View button = rootLayout.getChildAt(0);
        AccessibilityNodeInfoCompat info =
                AccessibilityNodeInfoCompat.wrap(button.createAccessibilityNodeInfo());
        expect.that(info.getContentDescription().toString()).contains(textDescription);
        expect.that(info.getClassName().toString()).contains("android.widget.Button");
        expect.that(info.isImportantForAccessibility()).isTrue();
        assertThat(button.isImportantForAccessibility()).isTrue();
    }

    @Test
    public void inflate_box_withSemanticsStateDescription() {
        String textDescription = "this is a switch";
        String offState = "off";
        Semantics semantics =
                Semantics.newBuilder()
                        .setContentDescription(string(textDescription))
                        .setStateDescription(string(offState))
                        .setRole(SemanticsRole.SEMANTICS_ROLE_SWITCH)
                        .build();
        String text = "a switch";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string(text))))
                                        .setModifiers(
                                                Modifiers.newBuilder().setSemantics(semantics)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View switchView = rootLayout.getChildAt(0);
        AccessibilityNodeInfoCompat info =
                AccessibilityNodeInfoCompat.wrap(switchView.createAccessibilityNodeInfo());
        expect.that(info.getContentDescription().toString()).contains(textDescription);
        expect.that(info.getStateDescription().toString()).contains(offState);
        expect.that(info.getClassName().toString()).contains("android.widget.Switch");
        expect.that(info.isImportantForAccessibility()).isTrue();
        assertThat(switchView.isImportantForAccessibility()).isTrue();
    }

    @Test
    public void inflate_spacer() {
        int width = 10;
        int height = 20;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(height)))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(width))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View tv = rootLayout.getChildAt(0);

        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(tv.getMeasuredWidth()).isEqualTo(width);
        expect.that(tv.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_spacer_noModifiers_hasMinDim() {
        int width = 10;
        int height = 20;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(height)))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(width))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View tv = rootLayout.getChildAt(0);

        // This tests that minimum dimension is correctly set.
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(tv.getMeasuredWidth()).isEqualTo(width);
        expect.that(tv.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_spacer_afterMutation_hasMinDim() {
        // Confirms that all dimensions are correctly set after mutation.
        int width = 10;
        int newWidth = width - 5;
        int height = 20;
        int newHeight = height - 6;

        Layout layout1 = layoutBoxWithSpacer(width, height);
        Layout layout2 = layoutBoxWithSpacer(newWidth, newHeight);

        // Add initial layout.
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();

        // Compute the mutation.
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation.
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        // This contains layout after the mutation.
        ViewGroup boxAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        View spacerAfterMutation = boxAfterMutation.getChildAt(0);

        // This tests that the layout dimension is correctly set.
        expect.that(spacerAfterMutation.getMeasuredWidth()).isEqualTo(newWidth);
        expect.that(spacerAfterMutation.getMeasuredHeight()).isEqualTo(newHeight);
    }

    @Test
    public void inflate_expandableBox_withExpandChildWithWrapChild_appliesMutationCorrectly() {
        // Box - Box - Text
        Text.Builder text1 = Text.newBuilder().setText(StringProp.newBuilder().setValue("1"));
        LayoutElement.Builder children1 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder().setText(text1).build())
                                        .build());
        Modifiers.Builder modifier1 =
                Modifiers.newBuilder().setClickable(Clickable.newBuilder().setId("1").build());
        LayoutElement layout1 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setModifiers(modifier1)
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(children1))
                        .build();

        // Box (has diff for self) - Box - Text (has diff for self)
        Text.Builder text2 = Text.newBuilder().setText(StringProp.newBuilder().setValue("2"));
        LayoutElement.Builder children2 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder().setText(text2).build())
                                        .build());
        Modifiers.Builder modifier2 =
                Modifiers.newBuilder().setClickable(Clickable.newBuilder().setId("2").build());
        LayoutElement layout2 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setModifiers(modifier2)
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(children2))
                        .build();

        // Add initial layout.
        Renderer renderer = renderer(fingerprintedLayout(layout1));
        ViewGroup inflatedViewParent = renderer.inflate();

        // Check that there's a single element in the layout
        assertThat(inflatedViewParent.getChildCount()).isEqualTo(1);

        // Compute the mutation.
        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), fingerprintedLayout(layout2));
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation.
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);

        // Make sure that mutation was successful.
        assertThat(mutationResult).isTrue();
    }

    @Test
    public void inflate_spacerWithModifiers() {
        int width = 10;
        int height = 20;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setBorder(
                                                                Border.newBuilder()
                                                                        .setWidth(dp(2))
                                                                        .build()))
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(height)))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(width))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View tv = rootLayout.getChildAt(0);

        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(tv.getMeasuredWidth()).isEqualTo(width);
        expect.that(tv.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_spacer_withModifiers_afterMutation_hasMinDim() {
        // Confirms that all dimensions are correctly set after mutation.
        int width = 10;
        int newWidth = width - 5;
        int height = 20;
        int newHeight = height - 6;
        Modifiers.Builder modifiers =
                Modifiers.newBuilder().setBorder(Border.newBuilder().setWidth(dp(2)).build());

        Layout layout1 = layoutBoxWithSpacer(width, height, modifiers);

        // Add initial layout.
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();

        Layout layout2 = layoutBoxWithSpacer(newWidth, newHeight, modifiers);

        // Compute the mutation.
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation.
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        ViewGroup boxAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        View spacerAfterMutation = boxAfterMutation.getChildAt(0);

        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(spacerAfterMutation.getMeasuredWidth()).isEqualTo(newWidth);
        expect.that(spacerAfterMutation.getMeasuredHeight()).isEqualTo(newHeight);
    }

    @Test
    public void inflate_spacerWithDynamicDimension_andModifiers() {
        int width = 100;
        int widthForLayout = 112;
        int height = 200;
        int heightForLayout = 212;
        DynamicFloat dynamicWidth =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(width).build())
                        .build();
        DynamicFloat dynamicHeight =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(height).build())
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicHeight,
                                                                        heightForLayout)))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicWidth,
                                                                        widthForLayout)))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setBackground(
                                                                ModifiersProto.Background
                                                                        .getDefaultInstance())))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ViewGroup wrapper = (ViewGroup) rootLayout.getChildAt(0);

        // Check the spacer wrapper.
        expect.that(wrapper.getMeasuredWidth()).isEqualTo(widthForLayout);
        expect.that(wrapper.getMeasuredHeight()).isEqualTo(heightForLayout);

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        View spacer = wrapper.getChildAt(0);
        // Check the actual spacer.
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(spacer.getMeasuredWidth()).isEqualTo(width);
        expect.that(spacer.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_spacerWithDynamicDimension() {
        int width = 100;
        int widthForLayout = 112;
        int height = 200;
        int heightForLayout = 212;
        DynamicFloat dynamicWidth =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(width).build())
                        .build();
        DynamicFloat dynamicHeight =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(height).build())
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicHeight,
                                                                        heightForLayout)))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicWidth,
                                                                        widthForLayout))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ViewGroup wrapper = (ViewGroup) rootLayout.getChildAt(0);

        // Check the spacer wrapper.
        expect.that(wrapper.getMeasuredWidth()).isEqualTo(widthForLayout);
        expect.that(wrapper.getMeasuredHeight()).isEqualTo(heightForLayout);

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        View spacer = wrapper.getChildAt(0);
        // Check the actual spacer.
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(spacer.getMeasuredWidth()).isEqualTo(width);
        expect.that(spacer.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_spacerWithExpand() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setExpandedDimension(
                                                                ExpandedDimensionProp
                                                                        .getDefaultInstance()))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setExpandedDimension(
                                                                ExpandedDimensionProp
                                                                        .getDefaultInstance())))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View tv = rootLayout.getChildAt(0);

        expect.that(tv.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(tv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);
    }

    @Test
    public void inflate_spacersWithWeightExpanded() {
        int widthWeight1 = 1;
        int widthWeight2 = 3;
        int heightWeight1 = 2;
        int heightWeight2 = 4;

        // SCREEN_WIDTH * widthWeight1 / (widthWeight1 + widthWeight2)
        int expectedWidth1 = 100;
        // SCREEN_WIDTH - expectedWidth1
        int expectedWidth2 = 300;
        // SCREEN_HEIGHT * heightWeight1 / (heightWeight1 + heightWeight2)
        int expectedHeight1 = 133;
        // SCREEN_HEIGHT - heightWeight1
        int expectedHeight2 = 267;

        // A column with a row (Spacer + Spacer) and Spacer, everything has weighted expand
        // dimension.

        Row rowWithSpacers =
                Row.newBuilder()
                        .setWidth(expand())
                        .setHeight(
                                ContainerDimension.newBuilder()
                                        .setExpandedDimension(expandWithWeight(heightWeight1))
                                        .build())
                        .addContents(
                                LayoutElement.newBuilder()
                                        .setSpacer(
                                                buildExpandedSpacer(widthWeight1, DEFAULT_WEIGHT)))
                        .addContents(
                                LayoutElement.newBuilder()
                                        .setSpacer(
                                                buildExpandedSpacer(widthWeight2, DEFAULT_WEIGHT)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setColumn(
                                Column.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder().setRow(rowWithSpacers))
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setSpacer(
                                                                buildExpandedSpacer(
                                                                        DEFAULT_WEIGHT,
                                                                        heightWeight2)))
                                        .build())
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ViewGroup column = (ViewGroup) rootLayout.getChildAt(0);
        ViewGroup row = (ViewGroup) column.getChildAt(0);
        View spacer1 = row.getChildAt(0);
        View spacer2 = row.getChildAt(1);
        View spacer = column.getChildAt(1);

        expect.that(spacer1.getMeasuredWidth()).isEqualTo(expectedWidth1);
        expect.that(spacer2.getMeasuredWidth()).isEqualTo(expectedWidth2);
        expect.that(row.getMeasuredHeight()).isEqualTo(expectedHeight1);
        expect.that(spacer.getMeasuredHeight()).isEqualTo(expectedHeight2);
    }

    @Test
    public void inflate_spacerWithMixExpandAndFixed() {
        int width = 100;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setExpandedDimension(
                                                                ExpandedDimensionProp
                                                                        .getDefaultInstance()))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(width))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View tv = rootLayout.getChildAt(0);

        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(tv.getMeasuredWidth()).isEqualTo(width);
        expect.that(tv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);
    }

    @Test
    public void inflate_spacerWithExpandHeightAndDynamicWidth() {
        int width = 100;
        int widthForLayout = 112;
        DynamicFloat dynamicWidth =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(width).build())
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setExpandedDimension(
                                                                ExpandedDimensionProp
                                                                        .getDefaultInstance()))
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicWidth,
                                                                        widthForLayout))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ViewGroup wrapper = (ViewGroup) rootLayout.getChildAt(0);

        // Check the spacer wrapper.
        expect.that(wrapper.getMeasuredWidth()).isEqualTo(widthForLayout);
        expect.that(wrapper.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        View spacer = wrapper.getChildAt(0);
        // Check the actual spacer.
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(spacer.getMeasuredWidth()).isEqualTo(width);
        expect.that(spacer.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);
    }

    @Test
    public void inflate_spacerWithExpandWidthAndDynamicHeight() {
        int height = 100;
        int heightForLayout = 112;
        DynamicFloat dynamicHeight =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(height).build())
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setExpandedDimension(
                                                                ExpandedDimensionProp
                                                                        .getDefaultInstance()))
                                        .setHeight(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(
                                                                dynamicDp(
                                                                        dynamicHeight,
                                                                        heightForLayout))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        // Check that there's a single element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ViewGroup wrapper = (ViewGroup) rootLayout.getChildAt(0);

        // Check the spacer wrapper.
        expect.that(wrapper.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(wrapper.getMeasuredHeight()).isEqualTo(heightForLayout);

        // Wait for evaluation to finish.
        shadowOf(Looper.getMainLooper()).idle();

        View spacer = wrapper.getChildAt(0);
        // Check the actual spacer.
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine.
        expect.that(spacer.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(spacer.getMeasuredHeight()).isEqualTo(height);
    }

    @Test
    public void inflate_image_withoutDimensions() {
        // Must match a resource ID in buildResources
        String protoResId = "android";

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(Image.newBuilder().setResourceId(string(protoResId)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // An image without dimensions will not be displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_image_withDimensions() {
        // Must match a resource ID in buildResources
        String protoResId = "android";

        LayoutElement root = buildImage(protoResId, 30, 20);

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(30);
        expect.that(iv.getMeasuredHeight()).isEqualTo(20);
    }

    @Test
    public void inflate_image_withInvalidRatio() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setHeight(
                                                ImageDimension.newBuilder()
                                                        .setProportionalDimension(
                                                                ProportionalDimensionProp
                                                                        .getDefaultInstance()))
                                        .setWidth(expandImage()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // An image with invalid ratio will not be displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_image_byName() {
        // Must match a resource ID in buildResources
        String protoResId = "android_image_by_name";

        LayoutElement root = buildImage(protoResId, 30, 20);

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(30);
        expect.that(iv.getMeasuredHeight()).isEqualTo(20);
    }

    @Test
    public void inflate_clickableModifier_withLaunchAction() throws IOException {
        final String packageName = "com.foo.protolayout.test";
        final String className = "com.foo.protolayout.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);
        ai.exported = true;

        String stringVal = "foobar";
        int int32Val = 123;
        long int64Val = 1234567890123456789L;
        double doubleVal = 0.1234;

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setPackageName(packageName)
                                        .setClassName(className)
                                        .putKeyToExtra(
                                                "stringValue",
                                                AndroidExtra.newBuilder()
                                                        .setStringVal(
                                                                AndroidStringExtra.newBuilder()
                                                                        .setValue(stringVal))
                                                        .build())
                                        .putKeyToExtra(
                                                "int32Value",
                                                AndroidExtra.newBuilder()
                                                        .setIntVal(
                                                                AndroidIntExtra.newBuilder()
                                                                        .setValue(int32Val))
                                                        .build())
                                        .putKeyToExtra(
                                                "int64Value",
                                                AndroidExtra.newBuilder()
                                                        .setLongVal(
                                                                AndroidLongExtra.newBuilder()
                                                                        .setValue(int64Val))
                                                        .build())
                                        .putKeyToExtra(
                                                "doubleValue",
                                                AndroidExtra.newBuilder()
                                                        .setDoubleVal(
                                                                AndroidDoubleExtra.newBuilder()
                                                                        .setValue(doubleVal))
                                                        .build())
                                        .putKeyToExtra(
                                                "boolValue",
                                                AndroidExtra.newBuilder()
                                                        .setBooleanVal(
                                                                AndroidBooleanExtra.newBuilder()
                                                                        .setValue(true))
                                                        .build()))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(textElement)).inflate();

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // The clickable view must have the same tag as the corresponding layout clickable.
        expect.that(tv.getTag(clickable_id_tag)).isEqualTo("foo");

        // Ensure that the text still went through properly.
        expect.that(tv.getText().toString()).isEqualTo(textContents);

        // Try and fire the intent.
        tv.performClick();

        Intent firedIntent =
                shadowOf((Application) getApplicationContext()).getNextStartedActivity();
        expect.that(firedIntent.getComponent()).isEqualTo(cn);
        expect.that(firedIntent.getStringExtra("stringValue")).isEqualTo(stringVal);
        expect.that(firedIntent.getIntExtra("int32Value", 0)).isEqualTo(int32Val);
        expect.that(firedIntent.getLongExtra("int64Value", 0)).isEqualTo(int64Val);
        expect.that(firedIntent.getDoubleExtra("doubleValue", 0)).isEqualTo(doubleVal);
        expect.that(firedIntent.getBooleanExtra("boolValue", false)).isEqualTo(true);
    }

    @Test
    public void inflate_clickableModifier_withLaunchAction_notExportedIsNotOp() {
        final String packageName = "com.foo.protolayout.test";
        final String className = "com.foo.protolayout.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);

        // Activity is not exported. Renderer shouldn't even try and call it.
        ai.exported = false;

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setPackageName(packageName)
                                        .setClassName(className))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        shadowOf((Application) getApplicationContext()).clearNextStartedActivities();

        // Try and fire the intent.
        tv.performClick();

        expect.that(shadowOf((Application) getApplicationContext()).getNextStartedActivity())
                .isNull();
    }

    @Test
    public void inflate_clickableModifier_withLaunchAction_requiresPermissionIsNoOp() {
        final String packageName = "com.foo.protolayout.test";
        final String className = "com.foo.protolayout.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);

        // Activity has a permission associated with it; shouldn't be called.
        ai.exported = true;
        ai.permission = "android.MY_SENSITIVE_PERMISSION";

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setPackageName(packageName)
                                        .setClassName(className))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        shadowOf((Application) getApplicationContext()).clearNextStartedActivities();

        // Try and fire the intent.
        tv.performClick();

        expect.that(shadowOf((Application) getApplicationContext()).getNextStartedActivity())
                .isNull();
    }

    @Test
    public void inflate_clickableModifier_withLoadAction() {
        final String textContents = "I am a clickable";

        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        State.Builder receivedState = State.newBuilder();
        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setLoadActionListener(receivedState::mergeFrom))
                        .inflate();

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // The clickable view must have the same tag as the corresponding layout clickable.
        expect.that(tv.getTag(clickable_id_tag)).isEqualTo("foo");

        // Ensure that the text still went through properly.
        expect.that(tv.getText().toString()).isEqualTo(textContents);

        // Try and fire the intent.
        tv.performClick();
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(receivedState.getLastClickableId()).isEqualTo("foo");
    }

    @Test
    public void inflate_clickableModifier_withPendingIntentAction() {
        final String textContents = "I am a clickable";
        final String clickableId = "foo";

        Action action =
                Action.newBuilder()
                        .setPendingIntentAction(PendingIntentAction.getDefaultInstance())
                        .build();
        Clickable clickable = Clickable.newBuilder().setId(clickableId).setOnClick(action).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder().setClickable(clickable)))
                        .build();

        AtomicReference<String> clickedId = new AtomicReference<>("");
        AtomicReference<@Nullable View> clickedView = new AtomicReference<>(null);

        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setPendingIntentActionListener(
                                                (source, id) -> {
                                                    clickedId.set(id);
                                                    clickedView.set(source);
                                                }))
                        .inflate();

        // Get the text view from the inflation result, it is the only child of the root.
        TextView textView = (TextView) rootLayout.getChildAt(0);
        // Try and fire the intent.
        textView.performClick();
        shadowOf(getMainLooper()).idle();

        expect.that(clickedId.get()).isEqualTo(clickableId);
        expect.that(clickedView.get()).isEqualTo(textView);
    }

    @Test
    public void inflate_clickableModifier_withAndroidActivity_hasSourceBounds() {
        final String packageName = "com.foo.protolayout.test";
        final String className = "com.foo.protolayout.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);
        ai.exported = true;

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setPackageName(packageName)
                                        .setClassName(className))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(textElement)).inflate();

        // Need to run a layout / measure pass so that the Text element has some bounds...
        rootLayout.measure(
                MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY));
        rootLayout.layout(0, 0, rootLayout.getMeasuredWidth(), rootLayout.getMeasuredHeight());

        TextView tv = (TextView) rootLayout.getChildAt(0);
        tv.performClick();

        Intent firedIntent =
                shadowOf((Application) getApplicationContext()).getNextStartedActivity();

        int[] screenLocation = new int[2];
        tv.getLocationOnScreen(screenLocation);
        Rect screenLocationRect =
                new Rect(
                        /* left= */ screenLocation[0],
                        /* top= */ screenLocation[1],
                        /* right= */ screenLocation[0] + tv.getWidth(),
                        /* bottom= */ screenLocation[1] + tv.getHeight());

        expect.that(firedIntent.getSourceBounds()).isEqualTo(screenLocationRect);
    }

    @Test
    public void inflate_clickableModifier_extendsClickTargetArea() {
        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();

        int parentSize = 60;
        int clickTargetSize = (int) DEFAULT_MIN_CLICKABLE_SIZE_DP;
        int childSize = 30;

        ContainerDimension parentBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentSize)).build();
        ContainerDimension childBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(childSize)).build();

        LayoutElement childBox =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(childBoxSize)
                                        .setHeight(childBoxSize)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo")
                                                                        .setOnClick(action))))
                        .build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(parentBoxSize)
                                        .setHeight(parentBoxSize)
                                        .addContents(childBox))
                        .build();

        State.Builder receivedState = State.newBuilder();
        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setLoadActionListener(receivedState::mergeFrom))
                        .inflate();
        shadowOf(Looper.getMainLooper()).idle();

        // Should be just a parent box with a child box.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View parent = rootLayout.getChildAt(0);
        assertThat(parent).isInstanceOf(FrameLayout.class);
        View child = ((FrameLayout) parent).getChildAt(0);
        assertThat(child).isInstanceOf(FrameLayout.class);

        // The clickable view must have the same tag as the corresponding layout clickable.
        expect.that(child.getTag(clickable_id_tag)).isEqualTo("foo");

        // Dispatch a click event to the child View; it should trigger the LoadAction...
        dispatchTouchEvent(child, childSize / 2f, childSize / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo");

        // -----------parent size 60------------------//
        //     ------clickable target size 48 ----    //
        //         ---clickable size 30 --            //
        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        int loc = (int) ((parentSize - clickTargetSize) / 2f + (clickTargetSize - childSize) / 4f);
        dispatchTouchEvent(parent, loc, loc);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo");

        // Dispatch a click event to the parent View outside the expanded clickable area;
        // it should NOT trigger the LoadAction...
        receivedState.clearLastClickableId();
        loc = (parentSize - clickTargetSize) / 4;
        dispatchTouchEvent(parent, loc, loc);
        expect.that(receivedState.getLastClickableId()).isEqualTo("");
    }

    @Test
    public void inflate_clickableModifier_extendMultipleClickTargetAreas() {
        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();

        int rowWidth = 80;
        int rowHeight = 30;
        int spacerSize = 5;
        int childSize = 20;
        int clickTargetSize = 30;
        ContainerDimension parentRowWidth =
                ContainerDimension.newBuilder().setLinearDimension(dp(rowWidth)).build();
        ContainerDimension parentRowHeight =
                ContainerDimension.newBuilder().setLinearDimension(dp(rowHeight)).build();
        ContainerDimension childBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(childSize)).build();
        LayoutElement.Builder spacer =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(spacerSize))
                                                        .build()));

        //           |--clickable area child box 1 (5 - 35)--|
        //                                          |---clickable area child box 2 (30-60)--|
        // | spacer | spacer |      child box 1     | spacer|        child box 2       |
        LayoutElement box1 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(childBoxSize)
                                        .setHeight(childBoxSize)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setMinimumClickableWidth(
                                                                                dp(clickTargetSize))
                                                                        .setMinimumClickableHeight(
                                                                                dp(clickTargetSize))
                                                                        .setOnClick(action)
                                                                        .setId("foo1"))))
                        .build();

        LayoutElement box2 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(childBoxSize)
                                        .setHeight(childBoxSize)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setMinimumClickableWidth(
                                                                                dp(clickTargetSize))
                                                                        .setMinimumClickableHeight(
                                                                                dp(clickTargetSize))
                                                                        .setOnClick(action)
                                                                        .setId("foo2"))))
                        .build();

        VerticalAlignmentProp verticalAlignment =
                VerticalAlignmentProp.newBuilder()
                        .setValue(VerticalAlignment.VERTICAL_ALIGN_CENTER)
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(
                                Row.newBuilder()
                                        .setWidth(parentRowWidth)
                                        .setHeight(parentRowHeight)
                                        .setVerticalAlignment(verticalAlignment)
                                        .addContents(spacer)
                                        .addContents(spacer)
                                        .addContents(box1)
                                        .addContents(spacer)
                                        .addContents(box2))
                        .build();

        State.Builder receivedState = State.newBuilder();
        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setLoadActionListener(receivedState::mergeFrom))
                        .inflate();

        ShadowLooper.runUiThreadTasks();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View parent = rootLayout.getChildAt(0);
        assertThat(parent).isInstanceOf(LinearLayout.class);
        View childBox1 = ((LinearLayout) parent).getChildAt(2);
        assertThat(childBox1).isInstanceOf(FrameLayout.class);
        View childBox2 = ((LinearLayout) parent).getChildAt(4);
        assertThat(childBox2).isInstanceOf(FrameLayout.class);

        // The clickable view must have the same tag as the corresponding layout clickable.
        expect.that(childBox1.getTag(clickable_id_tag)).isEqualTo("foo1");
        expect.that(childBox2.getTag(clickable_id_tag)).isEqualTo("foo2");

        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize + 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo1");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize * 2.5f + childSize - 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo1");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize * 2.5f + childSize + 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo2");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize * 3 + childSize * 2 + 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo2");

        // Dispatch a click event to the child View; it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(childBox1, childSize / 2f, childSize / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo1");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(childBox2, childSize / 2f, childSize / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo2");

        // Dispatch a click event to the parent View outside the expanded clickable area;
        // it should NOT trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, rowWidth - 1, rowHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("");
    }

    @Test
    public void inflateThenMutate_withChangeToText_clickableModifier_extendClickTargetSize() {
        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();
        Modifiers testModifiers1 =
                Modifiers.newBuilder()
                        .setClickable(Clickable.newBuilder().setOnClick(action).setId("foo1"))
                        .build();

        Modifiers testModifiers2 =
                Modifiers.newBuilder()
                        .setClickable(Clickable.newBuilder().setOnClick(action).setId("foo2"))
                        .build();

        int parentSize = 45;
        ContainerDimension parentBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentSize)).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(parentBoxSize)
                                        .setHeight(parentBoxSize)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string("world"))
                                                                        .setModifiers(
                                                                                testModifiers1))))
                        .build();

        State.Builder receivedState = State.newBuilder();
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(fingerprintedLayout(root), resourceResolvers())
                                .setLoadActionListener(receivedState::mergeFrom));
        FrameLayout rootLayout = renderer.inflate();
        ViewGroup parent = (ViewGroup) rootLayout.getChildAt(0);
        assertThat(((TextView) parent.getChildAt(0)).getText().toString()).isEqualTo("world");

        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, parentSize - 1, parentSize - 1);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo1");

        // Produce a new layout with only one Text element changed.
        LayoutElement root2 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(parentBoxSize)
                                        .setHeight(parentBoxSize)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string("mars"))
                                                                        .setModifiers(
                                                                                testModifiers2))))
                        .build();

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(rootLayout), fingerprintedLayout(root2));
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(rootLayout, mutation);
        assertThat(mutationResult).isTrue();
        assertThat(((TextView) parent.getChildAt(0)).getText().toString()).isEqualTo("mars");

        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, parentSize - 1, parentSize - 1);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo2");
    }

    @Test
    public void inflateThenMutate_withAddedText_clickableModifier_extendsMultiClickTargetAreas() {
        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();
        Modifiers testModifiers1 =
                Modifiers.newBuilder()
                        .setClickable(Clickable.newBuilder().setOnClick(action).setId("www"))
                        .build();

        Modifiers testModifiers2 =
                Modifiers.newBuilder()
                        .setClickable(Clickable.newBuilder().setOnClick(action).setId("mmm"))
                        .build();

        int spacerSize = 50;
        LayoutElement.Builder spacer =
                LayoutElement.newBuilder()
                        .setSpacer(
                                Spacer.newBuilder()
                                        .setWidth(
                                                SpacerDimension.newBuilder()
                                                        .setLinearDimension(dp(spacerSize))
                                                        .build()));

        int parentHeight = 45;
        int parentWidth = 125;
        ContainerDimension parentHeightDimension =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentHeight)).build();
        ContainerDimension parentWidthDimension =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentWidth)).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(
                                Row.newBuilder()
                                        .setWidth(parentWidthDimension)
                                        .setHeight(parentHeightDimension)
                                        .addContents(spacer)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string("www"))
                                                                        .setModifiers(
                                                                                testModifiers1))))
                        .build();

        State.Builder receivedState = State.newBuilder();
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(fingerprintedLayout(root), resourceResolvers())
                                .setLoadActionListener(receivedState::mergeFrom));
        FrameLayout rootLayout = renderer.inflate();
        ViewGroup parent = (ViewGroup) rootLayout.getChildAt(0);
        assertThat(((TextView) parent.getChildAt(1)).getText().toString()).isEqualTo("www");

        // | spacer |www|
        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize - 1, parentHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("www");

        // Produce a new layout with only one Text element changed.
        LayoutElement root2 =
                LayoutElement.newBuilder()
                        .setRow(
                                Row.newBuilder()
                                        .setWidth(parentWidthDimension)
                                        .setHeight(parentHeightDimension)
                                        .addContents(spacer)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string("www"))
                                                                        .setModifiers(
                                                                                testModifiers1)))
                                        .addContents(spacer)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                Text.newBuilder()
                                                                        .setText(string("mmm"))
                                                                        .setModifiers(
                                                                                testModifiers2))))
                        .build();

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(rootLayout), fingerprintedLayout(root2));
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(rootLayout, mutation);
        shadowOf(getMainLooper()).idle();
        try {
            applyMutationFuture.get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        // | spacer |www| spacer |mmm
        parent = (ViewGroup) rootLayout.getChildAt(0);
        TextView text1 = (TextView) parent.getChildAt(1);
        assertThat(text1.getText().toString()).isEqualTo("www");
        assertThat(((TextView) parent.getChildAt(3)).getText().toString()).isEqualTo("mmm");

        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize - 1, parentHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("www");
        receivedState.clearLastClickableId();
        dispatchTouchEvent(
                parent, spacerSize * 2 + 5 /* approximate www text size*/, parentHeight / 2f);
        expect.that(receivedState.getLastClickableId()).isEqualTo("mmm");

        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, spacerSize + 1, 1);
        expect.that(receivedState.getLastClickableId()).isEqualTo("www");
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, parentWidth - 1, parentHeight - 1);
        expect.that(receivedState.getLastClickableId()).isEqualTo("mmm");
    }

    @Test
    @Config(minSdk = VERSION_CODES.Q)
    public void
            inflateThenMutate_withClickableSizeChange_clickableModifier_extendClickTargetSize() {
        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();
        int parentSize = 50;
        ContainerDimension parentBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentSize)).build();
        ContainerDimension childBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(parentSize / 2f)).build();

        Modifiers testModifiers1 =
                Modifiers.newBuilder()
                        .setClickable(Clickable.newBuilder().setOnClick(action).setId("foo1"))
                        .build();

        // Child box has a size smaller than the minimum clickable size, touch delegation is
        // required.
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(parentBoxSize)
                                        .setHeight(parentBoxSize)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setBox(
                                                                Box.newBuilder()
                                                                        .setWidth(childBoxSize)
                                                                        .setHeight(childBoxSize)
                                                                        .setModifiers(
                                                                                testModifiers1))))
                        .build();

        State.Builder receivedState = State.newBuilder();
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(fingerprintedLayout(root), resourceResolvers())
                                .setLoadActionListener(receivedState::mergeFrom));
        FrameLayout rootLayout = renderer.inflate();
        ViewGroup parent = (ViewGroup) rootLayout.getChildAt(0);
        // Confirm the touch delegation has happened.
        assertThat(parent.getTouchDelegate()).isNotNull();
        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should trigger the LoadAction...
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, 5, 5);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo1");

        // Produce a new layout with child box specifies its minimum clickable size, NO touch
        // delegation is required.
        Modifiers testModifiers2 =
                Modifiers.newBuilder()
                        .setClickable(
                                Clickable.newBuilder()
                                        .setOnClick(action)
                                        .setId("foo2")
                                        .setMinimumClickableWidth(dp(parentSize / 2f))
                                        .setMinimumClickableHeight(dp(parentSize / 2f)))
                        .build();
        LayoutElement root2 =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(parentBoxSize)
                                        .setHeight(parentBoxSize)
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setBox(
                                                                Box.newBuilder()
                                                                        .setWidth(childBoxSize)
                                                                        .setHeight(childBoxSize)
                                                                        .setModifiers(
                                                                                testModifiers2))))
                        .build();

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(rootLayout), fingerprintedLayout(root2));
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(rootLayout, mutation);
        assertThat(mutationResult).isTrue();

        // Verify that the parent removed the touch delegation.
        // Keep an empty touch delegate composite will lead to failure when calling
        // {@link TouchDelegateComposite#getTouchDelegateInfo}
        assertThat(parent.getTouchDelegate()).isNull();

        // Dispatch a click event to the parent View within the expanded clickable area;
        // it should no longer trigger the LoadAction.
        receivedState.clearLastClickableId();
        dispatchTouchEvent(parent, 5, 5);
        expect.that(receivedState.getLastClickableId()).isEmpty();
        View box = parent.getChildAt(0);
        dispatchTouchEvent(box, 1, 1);
        expect.that(receivedState.getLastClickableId()).isEqualTo("foo2");
    }

    @Test
    public void inflate_clickable_withoutRippleEffect_rippleDrawableNotAdded() throws IOException {
        final String textContentsWithRipple = "clickable with ripple";
        final String textContentsWithoutRipple = "clickable without ripple";

        Action action = Action.newBuilder().setLoadAction(LoadAction.newBuilder()).build();

        LayoutElement textElementWithRipple =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContentsWithRipple))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo1")
                                                                        .setOnClick(action))))
                        .build();
        LayoutElement textElementWithoutRipple =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContentsWithoutRipple))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(
                                                                Clickable.newBuilder()
                                                                        .setId("foo2")
                                                                        .setVisualFeedbackEnabled(
                                                                                false)
                                                                        .setOnClick(action))))
                        .build();

        FrameLayout rootLayout =
                renderer(
                                fingerprintedLayout(
                                        LayoutElement.newBuilder()
                                                .setColumn(
                                                        Column.newBuilder()
                                                                .addContents(textElementWithRipple)
                                                                .addContents(
                                                                        textElementWithoutRipple)
                                                                .build())
                                                .build()))
                        .inflate();

        // Column
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(LinearLayout.class);
        ViewGroup columnElement = (ViewGroup) rootLayout.getChildAt(0);
        // Text elements
        assertThat(columnElement.getChildCount()).isEqualTo(2);
        assertThat(columnElement.getChildAt(0)).isInstanceOf(TextView.class);
        assertThat(columnElement.getChildAt(1)).isInstanceOf(TextView.class);

        TextView tvWithRipple = (TextView) columnElement.getChildAt(0);
        TextView tvWithoutRipple = (TextView) columnElement.getChildAt(1);

        assertThat(tvWithRipple.getText().toString()).isEqualTo(textContentsWithRipple);
        assertThat(tvWithoutRipple.getText().toString()).isEqualTo(textContentsWithoutRipple);

        // Ripple drawable gets set as foreground for the view.
        assertThat(tvWithRipple.getForeground()).isNotNull();
        assertThat(tvWithoutRipple.getForeground()).isNull();
    }

    @Test
    public void inflate_hiddenModifier_inhibitsClicks() {
        final String textContents = "I am a clickable";

        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                createTextWithVisibility(
                                                                        textContents,
                                                                        "back",
                                                                        action,
                                                                        true)))
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setText(
                                                                createTextWithVisibility(
                                                                        textContents,
                                                                        "front",
                                                                        action,
                                                                        false))))
                        .build();

        State.Builder receivedState = State.newBuilder();
        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setLoadActionListener(receivedState::mergeFrom))
                        .inflate();

        // Try to tap the stacked clickables.
        dispatchTouchEvent(rootLayout, 5f, 5f);
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(receivedState.getLastClickableId()).isEqualTo("back");
    }

    @Test
    public void inflate_arc_withLineDrawnWithArcTo() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setLine(
                                                                ArcLine.newBuilder()
                                                                        // Shorter than 360 degrees,
                                                                        // so should be drawn as an
                                                                        // arc:
                                                                        .setLength(degrees(30))
                                                                        .setStrokeCap(
                                                                                strokeCapButt())
                                                                        .setThickness(dp(12)))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        assertThat(line.getSweepAngleDegrees()).isEqualTo(30);
        assertThat(line.getStrokeCap()).isEqualTo(Cap.BUTT);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(line.getThickness()).isEqualTo(12);
    }

    @Test
    public void inflate_arc_clockwise() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setArcDirection(
                                                ArcDirectionProp.newBuilder()
                                                        .setValue(ARC_DIRECTION_CLOCKWISE)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.isClockwise()).isTrue();
        assertThat(arcLayout.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);
    }

    @Test
    public void inflate_arc_counterClockwise() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setArcDirection(
                                                ArcDirectionProp.newBuilder()
                                                        .setValue(ARC_DIRECTION_COUNTER_CLOCKWISE)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.isClockwise()).isFalse();
        assertThat(arcLayout.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);
    }

    @Test
    public void inflate_arc_withGradient() {
        Brush.Builder brush =
                Brush.newBuilder()
                        .setSweepGradient(
                                SweepGradient.newBuilder()
                                        .addColorStops(colorStop(Color.BLUE, 0.5f))
                                        .addColorStops(colorStop(Color.RED, 1f)));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setLine(
                                                                ArcLine.newBuilder()
                                                                        .setLength(degrees(30))
                                                                        .setStrokeCap(
                                                                                strokeCapButt())
                                                                        .setThickness(dp(12))
                                                                        .setBrush(brush))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        assertThat(line.mSweepGradientHelper).isNotNull();
    }

    @Test
    public void inflate_arc_withLineDrawnWithAddOval() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setLine(
                                                                ArcLine.newBuilder()
                                                                        // Longer than 360 degrees,
                                                                        // so should be drawn as an
                                                                        // oval:
                                                                        .setLength(degrees(500))
                                                                        .setThickness(dp(12)))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        assertThat(line.getSweepAngleDegrees()).isEqualTo(500);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(line.getThickness()).isEqualTo(12);
    }

    @Test
    public void inflate_arc_withText() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setText(
                                                                ArcText.newBuilder()
                                                                        .setText(string("text1"))))
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setText(
                                                                ArcText.newBuilder()
                                                                        .setText(string("text2")))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(2);
        CurvedTextView textView1 = (CurvedTextView) arcLayout.getChildAt(0);
        assertThat(textView1.getText()).isEqualTo("text1");
        CurvedTextView textView2 = (CurvedTextView) arcLayout.getChildAt(1);
        assertThat(textView2.getText()).isEqualTo("text2");
    }

    @Test
    public void inflate_arc_withText_autoSize_notSet() {
        int lastSize = 12;
        FontStyle.Builder style =
                FontStyle.newBuilder().addAllSize(buildSizesList(new int[] {10, 20, lastSize}));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setText(
                                                                ArcText.newBuilder()
                                                                        .setText(string("text1"))
                                                                        .setFontStyle(style))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        CurvedTextView tv = (CurvedTextView) arcLayout.getChildAt(0);
        assertThat(tv.getText()).isEqualTo("text1");
        expect.that(tv.getTextSize()).isEqualTo(lastSize);
    }

    @Test
    public void inflate_arc_withSpacerInDegrees() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                arcLayoutElement(
                                                        ArcSpacer.newBuilder()
                                                                .setLength(degrees(90)))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedSpacer spacer = (WearCurvedSpacer) arcLayout.getChildAt(0);
        assertThat(spacer.getSweepAngleDegrees()).isEqualTo(90);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(spacer.getThickness()).isEqualTo(20);
    }

    @Test
    public void inflate_arc_withSpacerInDp() {
        float containerSize = 100;
        float thickness = 10;
        float spacerLength = 20;
        ContainerDimension containerDimension =
                ContainerDimension.newBuilder().setLinearDimension(dp(containerSize)).build();
        Arc.Builder arcBuilder =
                Arc.newBuilder()
                        .setAnchorAngle(degrees(0).build())
                        .addContents(
                                ArcLayoutElement.newBuilder()
                                        .setSpacer(
                                                ArcSpacer.newBuilder()
                                                        .setAngularLength(
                                                                AngularDimension.newBuilder()
                                                                        .setDp(dp(spacerLength)))
                                                        .setThickness(dp(thickness))));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(containerDimension)
                                        .setHeight(containerDimension)
                                        .addContents(LayoutElement.newBuilder().setArc(arcBuilder)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        float radius = (containerSize - thickness) / 2;
        float sweepAngle = (float) Math.toDegrees(spacerLength / radius);

        ArcLayout arcLayout = (ArcLayout) ((ViewGroup) rootLayout.getChildAt(0)).getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedSpacer spacer = (WearCurvedSpacer) arcLayout.getChildAt(0);
        assertThat(spacer.getSweepAngleDegrees()).isEqualTo(sweepAngle);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(spacer.getThickness()).isEqualTo((int) thickness);
    }

    @Test
    public void inflate_arc_withMaxAngleAndWeights() {
        AngularDimension spacerLength =
                AngularDimension.newBuilder()
                        .setExpandedAngularDimension(expandAngular(1.0f))
                        .build();
        ArcLineLength lineLength =
                ArcLineLength.newBuilder().setExpandedAngularDimension(expandAngular(2.0f)).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .setMaxAngle(DegreesProp.newBuilder().setValue(90f).build())
                                        .addContents(
                                                arcLayoutElement(
                                                        ArcSpacer.newBuilder()
                                                                .setAngularLength(spacerLength)))
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setLine(
                                                                ArcLine.newBuilder()
                                                                        .setAngularLength(
                                                                                lineLength)
                                                                        .setThickness(dp(12)))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.isClockwise()).isTrue();
        assertThat(arcLayout.getMaxAngleDegrees()).isEqualTo(90f);
        assertThat(arcLayout.getChildCount()).isEqualTo(2);

        WearCurvedSpacer spacer = (WearCurvedSpacer) arcLayout.getChildAt(0);
        assertThat(spacer.getSweepAngleDegrees()).isEqualTo(30f);

        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(1);
        assertThat(line.getSweepAngleDegrees()).isEqualTo(60f);
    }

    private static ArcLayoutElement.@NonNull Builder arcLayoutElement(
            ArcSpacer.Builder setAngularLength) {
        return ArcLayoutElement.newBuilder().setSpacer(setAngularLength.setThickness(dp(20)));
    }

    @Test
    public void inflate_row() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 20);

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(Row.newBuilder().addContents(image).addContents(image))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup. Technically, we know it's a LinearLayout, but that
        // could change in the future. For now, just ensure that the two images are laid out
        // horizontally.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);

        assertThat(firstChild.getChildCount()).isEqualTo(2);
        assertThat(firstChild.getChildAt(0)).isInstanceOf(RatioViewWrapper.class);
        assertThat(firstChild.getChildAt(1)).isInstanceOf(RatioViewWrapper.class);

        RatioViewWrapper child1 = (RatioViewWrapper) firstChild.getChildAt(0);
        RatioViewWrapper child2 = (RatioViewWrapper) firstChild.getChildAt(1);

        // There's no padding here...
        expect.that(child2.getX()).isEqualTo(child1.getX() + child1.getMeasuredWidth());

        // In this case, because both children are the same size, they should definitely share the
        // same Y coordinate.
        expect.that(child1.getY()).isEqualTo(child2.getY());
    }

    @Test
    public void inflate_column() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 20);

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setColumn(Column.newBuilder().addContents(image).addContents(image))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);

        assertThat(firstChild.getChildCount()).isEqualTo(2);
        assertThat(firstChild.getChildAt(0)).isInstanceOf(RatioViewWrapper.class);
        assertThat(firstChild.getChildAt(1)).isInstanceOf(RatioViewWrapper.class);

        RatioViewWrapper child1 = (RatioViewWrapper) firstChild.getChildAt(0);
        RatioViewWrapper child2 = (RatioViewWrapper) firstChild.getChildAt(1);

        // There's no padding here...
        expect.that(child2.getY()).isEqualTo(child1.getY() + child1.getMeasuredHeight());

        // In this case, because both children are the same size, they should definitely share the
        // same X coordinate.
        expect.that(child1.getX()).isEqualTo(child2.getX());
    }

    private static LayoutElement buildImage(String protoResId, float widthDp, float heightDp) {
        return LayoutElement.newBuilder()
                .setImage(
                        Image.newBuilder()
                                .setResourceId(string(protoResId))
                                .setWidth(linImageDim(dp(widthDp)))
                                .setHeight(linImageDim(dp(heightDp))))
                .build();
    }

    private static LayoutElement buildExampleRowLayoutWithAlignment(VerticalAlignment alignment) {
        final String protoResId = "android";

        LayoutElement image1 = buildImage(protoResId, 30, 30);
        LayoutElement image2 = buildImage(protoResId, 30, 50);

        Row row =
                Row.newBuilder()
                        .addContents(image1)
                        .addContents(image2)
                        .setVerticalAlignment(
                                VerticalAlignmentProp.newBuilder().setValue(alignment))
                        .build();

        // Gravity = top.
        return LayoutElement.newBuilder().setRow(row).build();
    }

    @Test
    public void inflate_row_withGravity() {
        Map<VerticalAlignment, Integer> expectedY =
                ImmutableMap.of(
                        VerticalAlignment.VERTICAL_ALIGN_TOP, 0,
                        VerticalAlignment.VERTICAL_ALIGN_CENTER, 10,
                        VerticalAlignment.VERTICAL_ALIGN_BOTTOM, 20);

        for (Map.Entry<VerticalAlignment, Integer> entry : expectedY.entrySet()) {
            LayoutElement root = buildExampleRowLayoutWithAlignment(entry.getKey());
            FrameLayout topFrameLayout = renderer(fingerprintedLayout(root)).inflate();
            ViewGroup topViewGroup = (ViewGroup) topFrameLayout.getChildAt(0);
            RatioViewWrapper image1 = (RatioViewWrapper) topViewGroup.getChildAt(0);
            RatioViewWrapper image2 = (RatioViewWrapper) topViewGroup.getChildAt(1);

            // Image 1 is the smaller of the two, so its Y coordinate should move accordingly.
            expect.that(image1.getY()).isEqualTo(image2.getY() + entry.getValue());
        }
    }

    private static LayoutElement buildExampleColumnLayoutWithAlignment(
            HorizontalAlignment alignment) {
        final String resName = "android";

        LayoutElement image1 =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string(resName))
                                        .setWidth(linImageDim(dp(30)))
                                        .setHeight(linImageDim(dp(30))))
                        .build();

        LayoutElement image2 =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string(resName))
                                        .setWidth(linImageDim(dp(50)))
                                        .setHeight(linImageDim(dp(30))))
                        .build();

        Column column =
                Column.newBuilder()
                        .addContents(image1)
                        .addContents(image2)
                        .setHorizontalAlignment(
                                HorizontalAlignmentProp.newBuilder().setValue(alignment))
                        .build();

        // Gravity = top.
        return LayoutElement.newBuilder().setColumn(column).build();
    }

    @Test
    public void inflate_column_withGravity() {
        Map<HorizontalAlignment, Integer> expectedX =
                ImmutableMap.of(
                        HorizontalAlignment.HORIZONTAL_ALIGN_START, 0,
                        HorizontalAlignment.HORIZONTAL_ALIGN_CENTER, 10,
                        HorizontalAlignment.HORIZONTAL_ALIGN_END, 20);

        for (Map.Entry<HorizontalAlignment, Integer> entry : expectedX.entrySet()) {
            LayoutElement root = buildExampleColumnLayoutWithAlignment(entry.getKey());
            FrameLayout topFrameLayout = renderer(fingerprintedLayout(root)).inflate();
            ViewGroup topViewGroup = (ViewGroup) topFrameLayout.getChildAt(0);
            RatioViewWrapper image1 = (RatioViewWrapper) topViewGroup.getChildAt(0);
            RatioViewWrapper image2 = (RatioViewWrapper) topViewGroup.getChildAt(1);

            // Image 1 is the smaller of the two, so its X coordinate should move accordingly.
            expect.that(image1.getX()).isEqualTo(image2.getX() + entry.getValue());
        }
    }

    private static LayoutElement buildExampleContainerLayoutWithAlignment(
            HorizontalAlignment hAlign, VerticalAlignment vAlign) {
        final String resName = "android";

        LayoutElement image1 =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string(resName))
                                        .setWidth(linImageDim(dp(30)))
                                        .setHeight(linImageDim(dp(30))))
                        .build();

        LayoutElement image2 =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string(resName))
                                        .setWidth(linImageDim(dp(50)))
                                        .setHeight(linImageDim(dp(50))))
                        .build();

        Box box =
                Box.newBuilder()
                        .addContents(image1)
                        .addContents(image2)
                        .setVerticalAlignment(VerticalAlignmentProp.newBuilder().setValue(vAlign))
                        .setHorizontalAlignment(
                                HorizontalAlignmentProp.newBuilder().setValue(hAlign))
                        .build();

        // Gravity = top.
        return LayoutElement.newBuilder().setBox(box).build();
    }

    @Test
    public void inflate_stack_withAlignment() {
        Map<HorizontalAlignment, Integer> expectedX =
                ImmutableMap.of(
                        HorizontalAlignment.HORIZONTAL_ALIGN_START, 0,
                        HorizontalAlignment.HORIZONTAL_ALIGN_CENTER, 10,
                        HorizontalAlignment.HORIZONTAL_ALIGN_END, 20);

        Map<VerticalAlignment, Integer> expectedY =
                ImmutableMap.of(
                        VerticalAlignment.VERTICAL_ALIGN_TOP, 0,
                        VerticalAlignment.VERTICAL_ALIGN_CENTER, 10,
                        VerticalAlignment.VERTICAL_ALIGN_BOTTOM, 20);

        for (Map.Entry<HorizontalAlignment, Integer> hEntry : expectedX.entrySet()) {
            for (Map.Entry<VerticalAlignment, Integer> vEntry : expectedY.entrySet()) {
                LayoutElement root =
                        buildExampleContainerLayoutWithAlignment(hEntry.getKey(), vEntry.getKey());
                FrameLayout topFrameLayout = renderer(fingerprintedLayout(root)).inflate();

                ViewGroup topViewGroup = (ViewGroup) topFrameLayout.getChildAt(0);
                RatioViewWrapper image1 = (RatioViewWrapper) topViewGroup.getChildAt(0);
                RatioViewWrapper image2 = (RatioViewWrapper) topViewGroup.getChildAt(1);

                // Image 1 is the smaller of the two, so its coordinates should move accordingly.
                expect.that(image1.getX()).isEqualTo(image2.getX() + hEntry.getValue());
                expect.that(image1.getY()).isEqualTo(image2.getY() + vEntry.getValue());
            }
        }
    }

    @Test
    public void inflate_layoutElement_noChild() {
        // Just an empty layout. This is just to ensure that the renderer doesn't crash with a
        // "barely valid" proto.
        LayoutElement root = LayoutElement.getDefaultInstance();

        renderer(fingerprintedLayout(root)).inflate();
    }

    @Test
    public void buildClickableIntent_setsPackageName() {
        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                        .build();

        Intent i =
                ProtoLayoutInflater.buildLaunchActionIntent(launchAction, "", EXTRA_CLICKABLE_ID);

        expect.that(i.getComponent().getClassName()).isEqualTo(TEST_CLICKABLE_CLASS_NAME);
        expect.that(i.getComponent().getPackageName()).isEqualTo(TEST_CLICKABLE_PACKAGE_NAME);
    }

    @Test
    public void buildClickableIntent_launchAction_containsClickableId() {
        String testId = "HELLOWORLD";

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                        .build();

        Intent i =
                ProtoLayoutInflater.buildLaunchActionIntent(
                        launchAction, testId, EXTRA_CLICKABLE_ID);

        expect.that(i.getStringExtra(EXTRA_CLICKABLE_ID)).isEqualTo(testId);
    }

    @Test
    public void buildClickableIntent_noClickableExtraIfNotSet() {
        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(
                                AndroidActivity.newBuilder()
                                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                        .build();

        Intent i =
                ProtoLayoutInflater.buildLaunchActionIntent(launchAction, "", EXTRA_CLICKABLE_ID);

        expect.that(i.hasExtra(EXTRA_CLICKABLE_ID)).isFalse();
    }

    @Test
    public void inflate_imageView_noResourceId() {
        LayoutElement root =
                LayoutElement.newBuilder().setImage(Image.getDefaultInstance()).build();

        renderer(fingerprintedLayout(root)).inflate();
    }

    @Test
    public void inflate_imageView_resourceHasNoAndroidResource() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder().setResourceId(string("no_android_resource_set")))
                        .build();

        renderer(fingerprintedLayout(root)).inflate();
    }

    @Test
    public void inflate_imageView_androidResourceDoesNotExist() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(Image.newBuilder().setResourceId(string("does_not_exist")))
                        .build();

        renderer(fingerprintedLayout(root)).inflate();
    }

    @Test
    public void inflate_imageView_resourceReferenceDoesNotExist() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(Image.newBuilder().setResourceId(string("aaaaaaaaaaaaaa")))
                        .build();

        renderer(fingerprintedLayout(root)).inflate();
    }

    @Test
    public void inflate_imageView_expandsToParentEvenWhenImageBitmapIsNotSet() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string("invalid"))
                                        .setHeight(expandImage())
                                        .setWidth(expandImage()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(iv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);
    }

    @Test
    public void inflate_imageView_expandsToParentContainerEvenWhenImageBitmapIsNotSet() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setHeight(expand())
                                        .setWidth(expand())
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setPadding(
                                                                Padding.newBuilder()
                                                                        .setTop(dp(50))))
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setImage(
                                                                Image.newBuilder()
                                                                        .setResourceId(
                                                                                string("invalid"))
                                                                        .setHeight(expandImage())
                                                                        .setWidth(expandImage()))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        FrameLayout boxLayout = (FrameLayout) rootLayout.getChildAt(0);
        RatioViewWrapper iv = (RatioViewWrapper) boxLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(iv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT - 50);
    }

    @Test
    public void inflate_imageView_usesDimensionsEvenWhenImageBitmapIsNotSet() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string("invalid"))
                                        .setHeight(linImageDim(dp(100)))
                                        .setWidth(linImageDim(dp(100))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(100);
        expect.that(iv.getMeasuredHeight()).isEqualTo(100);
    }

    @Test
    public void inflate_imageView_largeImage_isIgnored() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string("large_image"))
                                        .setHeight(linImageDim(dp(50)))
                                        .setWidth(linImageDim(dp(50))))
                        .build();
        Resources resources =
                Resources.newBuilder()
                        .putIdToImage(
                                "large_image",
                                ImageResource.newBuilder()
                                        .setInlineResource(
                                                InlineImageResource.newBuilder()
                                                        .setFormat(ImageFormat.IMAGE_FORMAT_RGB_565)
                                                        .setWidthPx(10000)
                                                        .setHeightPx(10000)
                                                        .setData(
                                                                ByteString.copyFrom(
                                                                        new byte[10000 * 10000])))
                                        .build())
                        .build();
        ResourceResolvers.Builder resourceResolver =
                StandardResourceResolvers.forLocalApp(
                        resources,
                        getApplicationContext(),
                        ContextCompat.getMainExecutor(getApplicationContext()),
                        true);

        FrameLayout rootLayout =
                renderer(newRendererConfigBuilder(fingerprintedLayout(root), resourceResolver))
                        .inflate();
        shadowOf(getMainLooper()).idle();

        RatioViewWrapper rvw = (RatioViewWrapper) rootLayout.getChildAt(0);
        ImageView iv = (ImageView) rvw.getChildAt(0);
        expect.that(rvw.getMeasuredWidth()).isEqualTo(50);
        expect.that(rvw.getMeasuredHeight()).isEqualTo(50);
        expect.that(iv.getDrawable()).isNull();
        expect.that(iv.getMeasuredWidth()).isEqualTo(50);
        expect.that(iv.getMeasuredHeight()).isEqualTo(50);
    }

    @Test
    public void inflate_imageView_nonAnimatingAvdResource_noAnimation() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(
                                                string("android_AVD_pretending_to_be_static"))
                                        .setHeight(linImageDim(dp(50)))
                                        .setWidth(linImageDim(dp(50)))
                                        .build())
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper rvw = (RatioViewWrapper) rootLayout.getChildAt(0);
        ImageView imageAVD = (ImageView) rvw.getChildAt(0);
        Drawable drawableAVD = imageAVD.getDrawable();
        assertThat(drawableAVD).isInstanceOf(AnimatedVectorDrawable.class);
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void inflate_imageView_withAVDResource() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string("android_AVD"))
                                        .setHeight(linImageDim(dp(50)))
                                        .setWidth(linImageDim(dp(50)))
                                        .build())
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper rvw = (RatioViewWrapper) rootLayout.getChildAt(0);
        ImageView imageAVD = (ImageView) rvw.getChildAt(0);
        Drawable drawableAVD = imageAVD.getDrawable();
        assertThat(drawableAVD).isInstanceOf(AnimatedVectorDrawable.class);
    }

    @Test
    public void inflate_imageView_withSeekableAVDResource() {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string("android_seekable_AVD"))
                                        .setHeight(linImageDim(dp(50)))
                                        .setWidth(linImageDim(dp(50)))
                                        .build())
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        RatioViewWrapper rvw = (RatioViewWrapper) rootLayout.getChildAt(0);
        ImageView imageAVDSeekable = (ImageView) rvw.getChildAt(0);
        Drawable drawableAVDSeekable = imageAVDSeekable.getDrawable();
        assertThat(drawableAVDSeekable).isInstanceOf(SeekableAnimatedVectorDrawable.class);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        new AppDataKey<DynamicBuilders.DynamicFloat>("anim_val"),
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(0.44f))
                                .build()));
        shadowOf(getMainLooper()).idle();
        assertThat(((SeekableAnimatedVectorDrawable) drawableAVDSeekable).getCurrentPlayTime())
                .isEqualTo(440L);
    }

    @Test
    public void inflate_spannable_imageOccupiesSpace() {
        LayoutElement rootWithoutImage =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(textSpan("Foo"))
                                        .addSpans(textSpan("Bar")))
                        .build();

        LayoutElement rootWithImage =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(textSpan("Foo"))
                                        .addSpans(
                                                Span.newBuilder()
                                                        .setImage(
                                                                SpanImage.newBuilder()
                                                                        .setResourceId(
                                                                                string("android"))
                                                                        .setHeight(dp(50))
                                                                        .setWidth(dp(50))))
                                        .addSpans(textSpan("Bar")))
                        .build();

        FrameLayout rootLayoutWithoutImage =
                renderer(fingerprintedLayout(rootWithoutImage)).inflate();
        TextView tvInRootLayoutWithoutImage = (TextView) rootLayoutWithoutImage.getChildAt(0);
        FrameLayout rootLayoutWithImage = renderer(fingerprintedLayout(rootWithImage)).inflate();
        TextView tvInRootLayoutWithImage = (TextView) rootLayoutWithImage.getChildAt(0);

        int widthDiff =
                tvInRootLayoutWithImage.getMeasuredWidth()
                        - tvInRootLayoutWithoutImage.getMeasuredWidth();

        // Check that the layout with the image is larger by exactly the image's width.
        expect.that(widthDiff).isEqualTo(50);

        assertThat(tvInRootLayoutWithoutImage.getText().toString()).isEqualTo("FooBar");
        assertThat(tvInRootLayoutWithImage.getText().toString()).isEqualTo("Foo\u200D \u200DBar");
    }

    @Test
    public void inflate_spannable_onClickCanFire() {
        StringProp.Builder text = string("Hello World");
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(
                                                Span.newBuilder()
                                                        .setText(
                                                                SpanText.newBuilder()
                                                                        .setText(text)
                                                                        .setModifiers(
                                                                                spanClickMod()))))
                        .build();

        List<Boolean> hasFiredList = new ArrayList<>();
        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(
                                                fingerprintedLayout(root), resourceResolvers())
                                        .setLoadActionListener(p -> hasFiredList.add(true))
                                        .setProtoLayoutTheme(
                                                loadTheme(R.style.MyProtoLayoutSansSerifTheme)))
                        .inflate();

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Dispatch a click event to the first View; it should trigger the LoadAction...
        dispatchTouchEvent(tv, 5f, 5f);

        assertThat(hasFiredList).hasSize(1);
    }

    private static SpanModifiers.@NonNull Builder spanClickMod() {
        return SpanModifiers.newBuilder()
                .setClickable(
                        Clickable.newBuilder()
                                .setOnClick(
                                        Action.newBuilder()
                                                .setLoadAction(LoadAction.getDefaultInstance())));
    }

    @Test
    public void inflate_textView_marqueeAnimation() {
        String textContents = "Marquee Animation";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(1))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isEqualTo(TruncateAt.MARQUEE);
        expect.that(tv.isSelected()).isTrue();
        expect.that(tv.isHorizontalFadingEdgeEnabled()).isTrue();
        expect.that(tv.getMarqueeRepeatLimit()).isEqualTo(-1); // Default value.
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            expect.that(tv.isSingleLine()).isTrue();
        }
    }

    @Test
    public void inflate_textView_ellipsize() {
        // Manually set more lines because StaticLayout otherwise reports lineCount as 1.
        String textContents =
                "Text that is very\n"
                        + "large so it will go to many lines\n"
                        + " and it will\n"
                        + " overflow a lot";
        Text.Builder text1 =
                Text.newBuilder()
                        .setLineHeight(sp(16)) // Translates to around 35px or more
                        .setText(string(textContents))
                        .setFontStyle(FontStyle.newBuilder().addSize(sp(16)))
                        .setMaxLines(Int32Prop.newBuilder().setValue(6))
                        .setOverflow(
                                TextOverflowProp.newBuilder()
                                        .setValue(TextOverflow.TEXT_OVERFLOW_ELLIPSIZE));
        Layout layout1 =
                fingerprintedLayout(
                        LayoutElement.newBuilder()
                                .setBox(buildFixedSizeBoxWithText(text1))
                                .build());

        Text.Builder text2 =
                Text.newBuilder()
                        .setText(string(textContents))
                        // Diff
                        .setLineHeight(sp(4))
                        .setFontStyle(FontStyle.newBuilder().addSize(sp(4)))
                        .setMaxLines(Int32Prop.newBuilder().setValue(10))
                        .setOverflow(
                                TextOverflowProp.newBuilder()
                                        .setValue(TextOverflow.TEXT_OVERFLOW_ELLIPSIZE));
        Layout layout2 =
                fingerprintedLayout(
                        LayoutElement.newBuilder()
                                .setBox(buildFixedSizeBoxWithText(text2))
                                .build());

        // Initial layout.
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        shadowOf(Looper.getMainLooper()).idle();
        TextView textView1 =
                (TextView) ((ViewGroup) inflatedViewParent.getChildAt(0)).getChildAt(0);

        // Apply the mutation.
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        // This contains layout after the mutation.
        TextView textView2 =
                (TextView) ((ViewGroup) inflatedViewParent.getChildAt(0)).getChildAt(0);

        expect.that(textView1.getEllipsize()).isEqualTo(TruncateAt.END);
        expect.that(textView1.getMaxLines()).isEqualTo(2);

        expect.that(textView2.getEllipsize()).isEqualTo(TruncateAt.END);
        expect.that(textView2.getMaxLines()).isEqualTo(3);
    }

    // Typeface gets shadowed, so FontVariationSetting won't be set in TextView, as shadow returns
    // null for supported axes.
    @Test
    public void inflate_textView_fontFeatureSetting() {
        String textContents = "Text that is very large so it will go to many lines";
        FontSetting.Builder randomSetting =
                FontSetting.newBuilder()
                        .setFeature(
                                FontFeatureSetting.newBuilder()
                                        .setTag(ByteBuffer.wrap("rndm".getBytes(UTF_8)).getInt()));
        FontSetting.Builder tnumSetting =
                FontSetting.newBuilder()
                        .setFeature(
                                FontFeatureSetting.newBuilder()
                                        .setTag(ByteBuffer.wrap("tnum".getBytes(UTF_8)).getInt()));
        Text.Builder text1 =
                Text.newBuilder()
                        .setLineHeight(sp(16))
                        .setText(string(textContents))
                        .setFontStyle(
                                FontStyle.newBuilder()
                                        .addSize(sp(16))
                                        .addSettings(tnumSetting)
                                        .addSettings(randomSetting));
        Layout layout = fingerprintedLayout(LayoutElement.newBuilder().setText(text1).build());

        // Initial layout.
        Renderer renderer = renderer(layout);
        ViewGroup inflatedViewParent = renderer.inflate();
        TextView textView = (TextView) inflatedViewParent.getChildAt(0);

        shadowOf(Looper.getMainLooper()).idle();

        expect.that(textView.getFontFeatureSettings()).isEqualTo("'tnum'");
    }

    private static Box.Builder buildFixedSizeBoxWithText(Text.Builder content) {
        return Box.newBuilder()
                .setWidth(ContainerDimension.newBuilder().setLinearDimension(dp(100)))
                // Line of text would be ~35
                .setHeight(ContainerDimension.newBuilder().setLinearDimension(dp(120)))
                .addContents(LayoutElement.newBuilder().setText(content));
    }

    @Test
    public void inflate_textView_marquee_animationsDisabled() {
        String textContents = "Marquee Animation";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(1))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE)
                                                        .build()))
                        .build();

        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(root))
                                        .setAnimationEnabled(false))
                        .inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isNull();
    }

    @Test
    public void inflate_textView_marqueeAnimationInMultiLine() {
        String textContents = "Marquee Animation";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(2))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isEqualTo(TruncateAt.MARQUEE);
        expect.that(tv.isSelected()).isFalse();
        expect.that(tv.isHorizontalFadingEdgeEnabled()).isFalse();
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            expect.that(tv.isSingleLine()).isFalse();
        }
    }

    @Test
    public void inflate_textView_marqueeAnimation_repeatLimit() {
        String textContents = "Marquee Animation";
        int marqueeIterations = 5;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(textContents))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(1))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE)
                                                        .build())
                                        .setMarqueeParameters(
                                                MarqueeParameters.newBuilder()
                                                        .setIterations(marqueeIterations)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isEqualTo(TruncateAt.MARQUEE);
        expect.that(tv.isSelected()).isTrue();
        expect.that(tv.isHorizontalFadingEdgeEnabled()).isTrue();
        expect.that(tv.getMarqueeRepeatLimit()).isEqualTo(marqueeIterations);
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            expect.that(tv.isSingleLine()).isTrue();
        }
    }

    @Test
    public void inflate_textView_autosize_set() {
        String text = "Test text";
        int[] presetSizes = new int[] {12, 20, 10};
        List<DimensionProto.SpProp> sizes = buildSizesList(presetSizes);

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(text))
                                        .setFontStyle(FontStyle.newBuilder().addAllSize(sizes)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(textElement))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ViewGroup firstChild = (ViewGroup) rootLayout.getChildAt(0);
        TextView tv = (TextView) firstChild.getChildAt(0);

        // TextView sorts preset sizes.
        Arrays.sort(presetSizes);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        expect.that(tv.getAutoSizeTextAvailableSizes()).isEqualTo(presetSizes);
        expect.that(tv.getTextSize()).isEqualTo(20);
    }

    @Test
    public void inflate_textView_autosize_setLimit_usesSingleSize() {
        String text = "Test text";
        int sizesLength = TEXT_AUTOSIZES_LIMIT + 5;
        int[] presetSizes = new int[sizesLength];
        int expectedLastSize = 120;
        for (int i = 0; i < sizesLength - 1; i++) {
            presetSizes[i] = i + 1;
        }
        presetSizes[sizesLength - 1] = expectedLastSize;
        List<DimensionProto.SpProp> sizes = buildSizesList(presetSizes);

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(text))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(4))
                                        .setFontStyle(FontStyle.newBuilder().addAllSize(sizes)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(textElement))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ViewGroup firstChild = (ViewGroup) rootLayout.getChildAt(0);
        TextView tv = (TextView) firstChild.getChildAt(0);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        expect.that(tv.getAutoSizeTextAvailableSizes()).isEmpty();
        expect.that(tv.getTextSize()).isEqualTo(expectedLastSize);
    }

    @Test
    public void inflate_textView_autosize_notSet() {
        String text = "Test text";
        int size = 24;
        List<DimensionProto.SpProp> sizes = buildSizesList(new int[] {size});

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(text))
                                        .setFontStyle(FontStyle.newBuilder().addAllSize(sizes)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(textElement))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ViewGroup firstChild = (ViewGroup) rootLayout.getChildAt(0);
        TextView tv = (TextView) firstChild.getChildAt(0);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        expect.that(tv.getAutoSizeTextAvailableSizes()).isEmpty();
        expect.that(tv.getTextSize()).isEqualTo(size);
    }

    @Test
    public void inflate_textView_autosize_setDynamic_noop() {
        String text = "Test text";
        int lastSize = 24;
        List<DimensionProto.SpProp> sizes = buildSizesList(new int[] {10, 30, lastSize});

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(dynamicString(text))
                                        .setFontStyle(FontStyle.newBuilder().addAllSize(sizes)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(textElement))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ArrayList<View> textChildren = new ArrayList<>();
        rootLayout.findViewsWithText(textChildren, text, View.FIND_VIEWS_WITH_TEXT);
        TextView tv = (TextView) textChildren.get(0);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        expect.that(tv.getAutoSizeTextAvailableSizes()).isEmpty();
        expect.that(tv.getTextSize()).isEqualTo(lastSize);
    }

    @Test
    public void inflate_textView_autosize_wrongSizes_noop() {
        String text = "Test text";
        List<DimensionProto.SpProp> sizes = buildSizesList(new int[] {0, -2, 0});

        LayoutElement textElement =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string(text))
                                        .setFontStyle(FontStyle.newBuilder().addAllSize(sizes)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(textElement))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ArrayList<View> textChildren = new ArrayList<>();
        rootLayout.findViewsWithText(textChildren, text, View.FIND_VIEWS_WITH_TEXT);
        TextView tv = (TextView) textChildren.get(0);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        expect.that(tv.getAutoSizeTextAvailableSizes()).isEmpty();
    }

    @Test
    public void inflate_spannable_marqueeAnimation() {
        String text = "Marquee Animation";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(
                                                Span.newBuilder()
                                                        .setText(
                                                                SpanText.newBuilder()
                                                                        .setText(string(text))))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(1)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isEqualTo(TruncateAt.MARQUEE);
        expect.that(tv.isSelected()).isTrue();
        expect.that(tv.isHorizontalFadingEdgeEnabled()).isTrue();
        expect.that(tv.getMarqueeRepeatLimit()).isEqualTo(-1); // Default value.
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            expect.that(tv.isSingleLine()).isTrue();
        }
    }

    @Test
    public void inflate_spantext_ignoresMultipleSizes() {
        String text = "Test text";
        int firstSize = 12;
        FontStyle.Builder style =
                FontStyle.newBuilder().addAllSize(buildSizesList(new int[] {firstSize, 10, 20}));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(
                                                Span.newBuilder()
                                                        .setText(
                                                                SpanText.newBuilder()
                                                                        .setText(string(text))
                                                                        .setFontStyle(style))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getAutoSizeTextType()).isEqualTo(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
    }

    @Test
    public void inflate_spannable_marqueeAnimation_repeatLimit() {
        String text = "Marquee Animation";
        int marqueeIterations = 5;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setSpannable(
                                Spannable.newBuilder()
                                        .addSpans(
                                                Span.newBuilder()
                                                        .setText(
                                                                SpanText.newBuilder()
                                                                        .setText(string(text))))
                                        .setOverflow(
                                                TextOverflowProp.newBuilder()
                                                        .setValue(
                                                                TextOverflow.TEXT_OVERFLOW_MARQUEE))
                                        .setMarqueeParameters(
                                                MarqueeParameters.newBuilder()
                                                        .setIterations(marqueeIterations))
                                        .setMaxLines(Int32Prop.newBuilder().setValue(1)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getEllipsize()).isEqualTo(TruncateAt.MARQUEE);
        expect.that(tv.isSelected()).isTrue();
        expect.that(tv.isHorizontalFadingEdgeEnabled()).isTrue();
        expect.that(tv.getMarqueeRepeatLimit()).isEqualTo(marqueeIterations);
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            expect.that(tv.isSingleLine()).isTrue();
        }
    }

    @Test
    public void inflate_image_intrinsicSizeIsIgnored() {
        Image.Builder image =
                Image.newBuilder()
                        .setWidth(expandImage())
                        .setHeight(expandImage())
                        .setResourceId(string("large_image_120dp"));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(wrap())
                                        .setHeight(wrap())
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setImage(
                                                                Image.newBuilder()
                                                                        .setWidth(
                                                                                linImageDim(
                                                                                        dp(24f)))
                                                                        .setHeight(
                                                                                linImageDim(
                                                                                        dp(24f)))
                                                                        .setResourceId(
                                                                                string("android"))))
                                        .addContents(LayoutElement.newBuilder().setImage(image)))
                        .build();

        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(root))
                                        .setProtoLayoutTheme(
                                                loadTheme(R.style.MyProtoLayoutSansSerifTheme)))
                        .inflate();

        // Outer box should be 24dp
        FrameLayout firstBox = (FrameLayout) rootLayout.getChildAt(0);
        expect.that(firstBox.getWidth()).isEqualTo(24);
        expect.that(firstBox.getHeight()).isEqualTo(24);

        // Both children (images) should have the same dimensions as the FrameLayout.
        RatioViewWrapper rvw1 = (RatioViewWrapper) firstBox.getChildAt(0);
        RatioViewWrapper rvw2 = (RatioViewWrapper) firstBox.getChildAt(1);

        expect.that(rvw1.getWidth()).isEqualTo(24);
        expect.that(rvw1.getHeight()).isEqualTo(24);

        expect.that(rvw2.getWidth()).isEqualTo(24);
        expect.that(rvw2.getHeight()).isEqualTo(24);

        ImageViewWithoutIntrinsicSizes image1 = (ImageViewWithoutIntrinsicSizes) rvw1.getChildAt(0);
        ImageViewWithoutIntrinsicSizes image2 = (ImageViewWithoutIntrinsicSizes) rvw2.getChildAt(0);

        expect.that(image1.getWidth()).isEqualTo(24);
        expect.that(image1.getHeight()).isEqualTo(24);

        expect.that(image2.getWidth()).isEqualTo(24);
        expect.that(image2.getHeight()).isEqualTo(24);
    }

    private static ImageDimension.@NonNull Builder linImageDim(DpProp.Builder builderForValue) {
        return ImageDimension.newBuilder().setLinearDimension(builderForValue);
    }

    private static ContainerDimension.@NonNull Builder wrap() {
        return ContainerDimension.newBuilder()
                .setWrappedDimension(WrappedDimensionProp.getDefaultInstance());
    }

    @Test
    public void inflate_image_undefinedSizeIgnoresIntrinsicSize() {
        // This can happen in the case that a layout is ever inflated into a Scrolling layout. In
        // that case, the scrolling layout will measure all children with height = UNDEFINED, which
        // can lead to an Image still using its intrinsic size.
        String resId = "large_image_120dp";
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(wrap())
                                        .setHeight(wrap())
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setImage(
                                                                Image.newBuilder()
                                                                        .setWidth(
                                                                                linImageDim(
                                                                                        dp(24f)))
                                                                        .setHeight(
                                                                                linImageDim(
                                                                                        dp(24f)))
                                                                        .setResourceId(
                                                                                string("android"))))
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setImage(
                                                                Image.newBuilder()
                                                                        .setWidth(expandImage())
                                                                        .setHeight(expandImage())
                                                                        .setResourceId(
                                                                                string(resId)))))
                        .build();

        FrameLayout rootLayout =
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(root))
                                        .setProtoLayoutTheme(
                                                loadTheme(R.style.MyProtoLayoutSansSerifTheme)))
                        .inflate();

        // Re-measure the root layout with an UNDEFINED constraint...
        int screenWidth = MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY);
        int screenHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        rootLayout.measure(screenWidth, screenHeight);
        rootLayout.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Outer box should be 24dp
        FrameLayout firstBox = (FrameLayout) rootLayout.getChildAt(0);
        expect.that(firstBox.getWidth()).isEqualTo(24);
        expect.that(firstBox.getHeight()).isEqualTo(24);

        // Both children (images) should have the same dimensions as the FrameLayout.
        RatioViewWrapper rvw1 = (RatioViewWrapper) firstBox.getChildAt(0);
        RatioViewWrapper rvw2 = (RatioViewWrapper) firstBox.getChildAt(1);

        expect.that(rvw1.getWidth()).isEqualTo(24);
        expect.that(rvw1.getHeight()).isEqualTo(24);

        expect.that(rvw2.getWidth()).isEqualTo(24);
        expect.that(rvw2.getHeight()).isEqualTo(24);

        ImageViewWithoutIntrinsicSizes image1 = (ImageViewWithoutIntrinsicSizes) rvw1.getChildAt(0);
        ImageViewWithoutIntrinsicSizes image2 = (ImageViewWithoutIntrinsicSizes) rvw2.getChildAt(0);

        expect.that(image1.getWidth()).isEqualTo(24);
        expect.that(image1.getHeight()).isEqualTo(24);

        expect.that(image2.getWidth()).isEqualTo(24);
        expect.that(image2.getHeight()).isEqualTo(24);
    }

    @Test
    public void inflate_arcLine_usesValueForLayout() {
        DynamicFloat arcLength =
                DynamicFloat.newBuilder().setFixed(FixedFloat.newBuilder().setValue(45f)).build();

        ArcLayoutElement arcLine =
                ArcLayoutElement.newBuilder()
                        .setLine(
                                ArcLine.newBuilder()
                                        // Shorter than 360 degrees, so should be drawn as an arc:
                                        .setLength(
                                                degreesDynamic(
                                                        arcLength, /* valueForLayout= */ 180f))
                                        .setThickness(dp(12)))
                        .build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(arcLine))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        SizedArcContainer sizedContainer = (SizedArcContainer) arcLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) sizedContainer.getChildAt(0);
        assertThat(sizedContainer.getSweepAngleDegrees()).isEqualTo(180f);
        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(45f);
        assertThat(line.getMaxSweepAngleDegrees()).isEqualTo(180f);
    }

    @Test
    public void inflate_arcLine_usesArcLength() {
        DynamicFloat arcLength =
                DynamicFloat.newBuilder().setFixed(FixedFloat.newBuilder().setValue(45f)).build();
        ArcLayoutElement arcLine =
                ArcLayoutElement.newBuilder()
                        .setLine(
                                ArcLine.newBuilder()
                                        .setLength(
                                                degreesDynamic(arcLength, /* valueForLayout= */ 0f))
                                        .setThickness(dp(12)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(arcLine))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        expect.that(line.getSweepAngleDegrees()).isEqualTo(45f);
        expect.that(line.getLineSweepAngleDegrees()).isEqualTo(45f);
        expect.that(line.getMaxSweepAngleDegrees())
                .isEqualTo(WearCurvedLineView.SWEEP_ANGLE_WRAP_LENGTH);
    }

    @Test
    public void inflate_arcLine_dynamicData_updatesArcLength() {
        AppDataKey<DynamicBuilders.DynamicInt32> keyFoo = new AppDataKey<>("foo");
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(10))
                                .build()));

        shadowOf(Looper.getMainLooper()).idle();

        DynamicFloat arcLength =
                DynamicFloat.newBuilder()
                        .setInt32ToFloatOperation(
                                Int32ToFloatOp.newBuilder()
                                        .setInput(
                                                DynamicInt32.newBuilder()
                                                        .setStateSource(
                                                                StateInt32Source.newBuilder()
                                                                        .setSourceKey("foo"))))
                        .build();

        ArcLayoutElement arcLine =
                ArcLayoutElement.newBuilder()
                        .setLine(
                                ArcLine.newBuilder()
                                        // Shorter than 360 degrees, so should be drawn as an arc:
                                        .setLength(
                                                degreesDynamic(
                                                        arcLength, /* valueForLayout= */ 180f))
                                        .setThickness(dp(12)))
                        .build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(arcLine))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        SizedArcContainer sizedContainer = (SizedArcContainer) arcLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) sizedContainer.getChildAt(0);
        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(10);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(20))
                                .build()));

        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(20);
    }

    private static DegreesProp.@NonNull Builder degreesDynamic(DynamicFloat arcLength) {
        return DegreesProp.newBuilder().setDynamicValue(arcLength);
    }

    private static DegreesProp.@NonNull Builder degreesDynamic(
            DynamicFloat arcLength, float valueForLayout) {
        return DegreesProp.newBuilder()
                .setValueForLayout(valueForLayout)
                .setDynamicValue(arcLength);
    }

    @Test
    public void inflate_arcLine_withoutValueForLayout_usesArcLength() {
        DynamicFloat arcLength =
                DynamicFloat.newBuilder().setFixed(FixedFloat.newBuilder().setValue(45f)).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .setAnchorAngle(degrees(0).build())
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setLine(
                                                                ArcLine.newBuilder()
                                                                        // Shorter than 360 degrees,
                                                                        // so should be drawn as an
                                                                        // arc:
                                                                        .setLength(
                                                                                degreesDynamic(
                                                                                        arcLength))
                                                                        .setThickness(dp(12)))))
                        .build();

        FrameLayout rootLayout =
                renderer(newRendererConfigBuilder(fingerprintedLayout(root))).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        expect.that(line.getSweepAngleDegrees()).isEqualTo(45f);
        expect.that(line.getLineSweepAngleDegrees()).isEqualTo(45f);
        expect.that(line.getMaxSweepAngleDegrees())
                .isEqualTo(WearCurvedLineView.SWEEP_ANGLE_WRAP_LENGTH);
    }

    @Test
    public void inflate_text_dynamicColor_updatesColor() {
        AppDataKey<DynamicBuilders.DynamicColor> keyFoo = new AppDataKey<>("foo");
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(0xFFFFFFFF))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        DynamicColor color =
                DynamicColor.newBuilder()
                        .setStateSource(StateColorSource.newBuilder().setSourceKey("foo"))
                        .build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string("Hello World"))
                                        .setFontStyle(
                                                FontStyle.newBuilder()
                                                        .setColor(
                                                                ColorProp.newBuilder()
                                                                        .setDynamicValue(color))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        shadowOf(Looper.getMainLooper()).idle();

        TextView tv = (TextView) rootLayout.getChildAt(0);
        assertThat(tv.getCurrentTextColor()).isEqualTo(0xFFFFFFFF);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(0x11111111))
                                .build()));

        assertThat(tv.getCurrentTextColor()).isEqualTo(0x11111111);
    }

    @Test
    public void inflate_image_dynamicTint_changesTintColor() {
        // Must match a resource ID in buildResources
        String protoResId = "android";

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        new AppDataKey<DynamicBuilders.DynamicColor>("tint"),
                        DynamicDataValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(0xFFFFFFFF))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        DynamicColor color =
                DynamicColor.newBuilder()
                        .setStateSource(StateColorSource.newBuilder().setSourceKey("tint"))
                        .build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setImage(
                                Image.newBuilder()
                                        .setResourceId(string(protoResId))
                                        .setColorFilter(
                                                ColorFilter.newBuilder()
                                                        .setTint(
                                                                ColorProp.newBuilder()
                                                                        .setDynamicValue(color)))
                                        .setHeight(expandImage())
                                        .setWidth(expandImage()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        RatioViewWrapper rvw = (RatioViewWrapper) rootLayout.getChildAt(0);
        ImageView iv = (ImageView) rvw.getChildAt(0);
        assertThat(iv.getImageTintList().getDefaultColor()).isEqualTo(0xFFFFFFFF);
    }

    @Test
    public void inflate_extension_onlySpaceIfNoExtension() {
        byte[] payload = "Hello World".getBytes(UTF_8);
        int size = 5;

        ExtensionDimension dim =
                ExtensionDimension.newBuilder().setLinearDimension(dp(size)).build();
        LayoutElement rootElement =
                LayoutElement.newBuilder()
                        .setExtension(
                                ExtensionLayoutElement.newBuilder()
                                        .setExtensionId("foo")
                                        .setPayload(ByteString.copyFrom(payload))
                                        .setWidth(dim)
                                        .setHeight(dim))
                        .build();

        FrameLayout inflatedLayout = renderer(fingerprintedLayout(rootElement)).inflate();

        assertThat(inflatedLayout.getChildCount()).isEqualTo(1);
        assertThat(inflatedLayout.getChildAt(0)).isInstanceOf(Space.class);

        Space s = (Space) inflatedLayout.getChildAt(0);
        assertThat(s.getMeasuredWidth()).isEqualTo(size);
        assertThat(s.getMeasuredHeight()).isEqualTo(size);
    }

    @Test
    public void inflate_rendererExtension_withExtension_callsExtension() {
        List<Pair<byte[], String>> invokedExtensions = new ArrayList<>();

        final byte[] payload = "Hello World".getBytes(UTF_8);
        final int size = 5;
        final String extensionId = "foo";

        ExtensionDimension dim =
                ExtensionDimension.newBuilder().setLinearDimension(dp(size)).build();
        LayoutElement rootElement =
                LayoutElement.newBuilder()
                        .setExtension(
                                ExtensionLayoutElement.newBuilder()
                                        .setExtensionId(extensionId)
                                        .setPayload(ByteString.copyFrom(payload))
                                        .setWidth(dim)
                                        .setHeight(dim))
                        .build();

        FrameLayout inflatedLayout =
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(rootElement))
                                        .setExtensionViewProvider(
                                                (extensionPayload, id) -> {
                                                    invokedExtensions.add(
                                                            new Pair<>(extensionPayload, id));
                                                    TextView returnedView =
                                                            new TextView(getApplicationContext());
                                                    returnedView.setText("testing");

                                                    return returnedView;
                                                }))
                        .inflate();

        assertThat(inflatedLayout.getChildCount()).isEqualTo(1);
        assertThat(inflatedLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) inflatedLayout.getChildAt(0);
        assertThat(tv.getText().toString()).isEqualTo("testing");

        assertThat(invokedExtensions).hasSize(1);
        assertThat(invokedExtensions.get(0).first).isEqualTo(payload);
        assertThat(invokedExtensions.get(0).second).isEqualTo(extensionId);
    }

    @Test
    public void inflateThenMutate_withChangeToText_causesUpdate() {
        Layout layout1 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(column.getChildCount()).isEqualTo(2);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Produce a new layout with only one Text element changed.
        Layout layout2 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("Mars") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);

        // Unchanged views should be left exactly the same:
        assertThat(columnAfterMutation).isSameInstanceAs(column);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        // Overall content should match layout2:
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("Mars");
    }

    @Test
    public void inflateThenMutate_withChangeToImageAndText_causesUpdate() {
        Layout layout1 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                row( // 1.2
                                        image( // 1.2.1
                                                props -> {
                                                    props.heightDp = 50;
                                                    props.widthDp = 50;
                                                },
                                                "android"),
                                        text("World") // 1.2.2
                                        )));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        ViewGroup row = (ViewGroup) column.getChildAt(1);
        ImageView image = (ImageView) ((ViewGroup) row.getChildAt(0)).getChildAt(0);
        TextView tv2 = (TextView) row.getChildAt(1);
        assertThat(column.getChildCount()).isEqualTo(2);
        assertThat(row.getChildCount()).isEqualTo(2);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");
        // Can't get android resource ID from image, so use the size to infer that we start with the
        // correct one.
        assertThat(image.getDrawable().getIntrinsicHeight()).isEqualTo(24);
        assertThat(image.getDrawable().getIntrinsicWidth()).isEqualTo(24);
        assertThat(image.getMeasuredHeight()).isEqualTo(50);
        assertThat(image.getMeasuredWidth()).isEqualTo(50);

        // Produce a new layout with one Text element and one Image changed.
        Layout layout2 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                row( // 1.2
                                        image( // 1.2.1
                                                props -> {
                                                    props.heightDp = 50;
                                                    props.widthDp = 50;
                                                },
                                                "large_image_120dp"),
                                        text("Mars") // 1.2.2
                                        )));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        ViewGroup rowAfterMutation = (ViewGroup) columnAfterMutation.getChildAt(1);
        ImageView imageAfterMutation =
                (ImageView) ((ViewGroup) rowAfterMutation.getChildAt(0)).getChildAt(0);
        TextView tv2AfterMutation = (TextView) row.getChildAt(1);

        // Unchanged views should be left exactly the same:
        assertThat(columnAfterMutation).isSameInstanceAs(column);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        assertThat(rowAfterMutation).isSameInstanceAs(row);
        assertThat(rowAfterMutation.getChildCount()).isEqualTo(2);
        // Overall content should match layout2:
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("Mars");
        // Can't get android resource ID from image, so use the size to infer that the image has
        // been correctly updated to a different one:
        assertThat(imageAfterMutation.getDrawable().getIntrinsicHeight()).isEqualTo(120);
        assertThat(imageAfterMutation.getDrawable().getIntrinsicWidth()).isEqualTo(120);
        assertThat(imageAfterMutation.getMeasuredHeight()).isEqualTo(50);
        assertThat(imageAfterMutation.getMeasuredWidth()).isEqualTo(50);
    }

    @Test
    public void inflateThenMutate_withChangeToProps_causesUpdate() {
        Layout layout1 =
                layout(
                        column( // 1
                                props -> props.widthDp = 55,
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(column.getMeasuredWidth()).isEqualTo(55);
        assertThat(column.getChildCount()).isEqualTo(2);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Produce a new layout with only the props of the container changed.
        Layout layout2 =
                layout(
                        column( // 1
                                props -> props.widthDp = 123,
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        // Check contents after mutation
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);
        assertThat(columnAfterMutation.getMeasuredWidth()).isEqualTo(123);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("World");
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        assertThat(tv2AfterMutation).isSameInstanceAs(tv2);
    }

    @Test
    public void inflateThenMutate_withChangeToPropsAndOneChild_doesntUpdateAllChildren() {
        Layout layout1 =
                layout(
                        column( // 1
                                props -> props.widthDp = 55,
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(column.getMeasuredWidth()).isEqualTo(55);
        assertThat(column.getChildCount()).isEqualTo(2);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Produce a new layout with the props of the container and one child changed.
        Layout layout2 =
                layout(
                        column( // 1
                                props -> props.widthDp = 123,
                                text("Hello"), // 1.1
                                text("MARS") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        // Check contents after mutation
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);
        assertThat(columnAfterMutation.getMeasuredWidth()).isEqualTo(123);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("MARS");
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
    }

    @Test
    public void inflateThenMutate_withNoChange_producesNoOpMutation() {
        Layout layout =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(column.getChildCount()).isEqualTo(2);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Compute the mutation for the same layout
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isTrue();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);

        // Everything should be exactly the same:
        assertThat(columnAfterMutation).isSameInstanceAs(column);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        assertThat(tv2AfterMutation).isSameInstanceAs(tv2);
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("World");
    }

    @Test
    public void inflateThenMutate_withDifferentNumberOfChildren_causesUpdate() {
        Layout layout1 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();

        // Check the pre-mutation layout
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        Layout layout2 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("World"), // 1.2
                                text("and"), // 1.3
                                text("Mars") // 1.4
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);
        TextView tv3AfterMutation = (TextView) columnAfterMutation.getChildAt(2);
        TextView tv4AfterMutation = (TextView) columnAfterMutation.getChildAt(3);

        // Check contents after mutation
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("World");
        assertThat(tv3AfterMutation.getText().toString()).isEqualTo("and");
        assertThat(tv4AfterMutation.getText().toString()).isEqualTo("Mars");
    }

    @Test
    public void inflateThenMutate_withDynamicText_dataPipelineIsUpdated() {
        Layout layout1 =
                layout(
                        column( // 1
                                dynamicFixedText("Hello"), // 1.1
                                dynamicFixedText("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        assertThat(column.getChildCount()).isEqualTo(2);

        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Produce a new layout with the second Text element changed.

        Layout layout2 =
                layout(
                        column( // 1
                                dynamicFixedText("Hello"), // 1.1
                                dynamicFixedText("Mars") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);

        // Unchanged views should be left exactly the same:
        assertThat(columnAfterMutation).isSameInstanceAs(column);
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        // Overall content should match layout2:
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("Mars");
    }

    @Test
    public void inflateThenMutate_withSelfMutation_dataPipelineIsPreserved() {
        Layout layout1 =
                layout(
                        column( // 1
                                props -> props.widthDp = 10,
                                dynamicFixedText("Hello"), // 1.1
                                dynamicFixedText("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        assertThat(column.getChildCount()).isEqualTo(2);

        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Produce a new layout with the column width changed.
        Layout layout2 =
                layout(
                        column( // 1
                                props -> props.widthDp = 20,
                                dynamicFixedText("Hello"), // 1.1
                                dynamicFixedText("World") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
        ViewGroup columnAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        assertThat(columnAfterMutation.getChildCount()).isEqualTo(2);
        TextView tv1AfterMutation = (TextView) columnAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) columnAfterMutation.getChildAt(1);

        // Unchanged views should be left exactly the same:
        expect.that(tv1AfterMutation).isSameInstanceAs(tv1);
        expect.that(tv2AfterMutation).isSameInstanceAs(tv2);
        // Overall content should match layout2:
        assertThat(tv1AfterMutation.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText().toString()).isEqualTo("World");
    }

    @Test
    public void reInflate_dataPipelineIsReset() {
        Layout layout =
                layout(
                        column( // 1
                                dynamicFixedText("Hello"), // 1.1
                                dynamicFixedText("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        assertThat(column.getChildCount()).isEqualTo(2);

        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        // Re-inflate and check that the number of nodes is the same as the previous inflation.
        renderer.inflate();
        assertThat(renderer.getDataPipelineSize()).isEqualTo(2);
    }

    @Test
    public void inflateWithNoFingerprint_producesNoRenderingMetadata() {
        Layout layout1WithNoFingerprints =
                layout(text("Hello")).toBuilder().clearFingerprint().build();
        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1WithNoFingerprints);

        ViewGroup inflatedViewParent = renderer.inflate();
        TextView tv = (TextView) inflatedViewParent.getChildAt(0);

        assertThat(tv.getText().toString()).isEqualTo("Hello");
        assertThat(getRenderedMetadata(inflatedViewParent)).isNull();
    }

    @Test
    public void inflateArcThenMutate_withChangeToText_causesUpdate() {
        Layout layout1 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(inflatedViewParent.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) inflatedViewParent.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(2);
        CurvedTextView tv1 = (CurvedTextView) arcLayout.getChildAt(0);
        CurvedTextView tv2 = (CurvedTextView) arcLayout.getChildAt(1);
        assertThat(tv1.getText()).isEqualTo("Hello");
        assertThat(tv2.getText()).isEqualTo("World");

        // Produce a new layout with only one Text element changed.
        Layout layout2 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("Mars") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();
        assertThat(inflatedViewParent.getChildCount()).isEqualTo(1);
        ArcLayout arcLayoutAfterMutation = (ArcLayout) inflatedViewParent.getChildAt(0);

        // Overall content should match layout2:
        CurvedTextView tv1AfterMutation = (CurvedTextView) arcLayout.getChildAt(0);
        assertThat(tv1AfterMutation.getText()).isEqualTo("Hello");
        CurvedTextView tv2AfterMutation = (CurvedTextView) arcLayout.getChildAt(1);
        assertThat(tv2AfterMutation.getText()).isEqualTo("Mars");

        // Unchanged views should be left exactly the same:
        assertThat(arcLayoutAfterMutation).isSameInstanceAs(arcLayout);
        assertThat(arcLayoutAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
    }

    @Test
    public void inflateArcThenMutate_withChangeToProps_causesUpdate() throws Exception {
        Layout layout1 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));

        // Check the premutation layout
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(inflatedViewParent.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) inflatedViewParent.getChildAt(0);
        assertThat(arcLayout.getAnchorAngleDegrees()).isEqualTo(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(2);
        CurvedTextView tv1 = (CurvedTextView) arcLayout.getChildAt(0);
        CurvedTextView tv2 = (CurvedTextView) arcLayout.getChildAt(1);
        assertThat(tv1.getText()).isEqualTo("Hello");
        assertThat(tv2.getText()).isEqualTo("World");

        Layout layout2 =
                layout(
                        arc( // 1
                                props -> props.anchorAngleDegrees = 35,
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout2, ViewProperties.EMPTY);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        // Check the post-mutation layout
        ArcLayout arcLayoutAfterMutation = (ArcLayout) inflatedViewParent.getChildAt(0);
        assertThat(arcLayoutAfterMutation.getChildCount()).isEqualTo(2);
        assertThat(arcLayoutAfterMutation.getAnchorAngleDegrees()).isEqualTo(35);
        CurvedTextView tv1AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(0);
        CurvedTextView tv2AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(1);
        assertThat(tv1AfterMutation.getText()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText()).isEqualTo("World");
        assertThat(tv1AfterMutation).isSameInstanceAs(tv1);
        assertThat(tv2AfterMutation).isSameInstanceAs(tv2);
    }

    @Test
    @Ignore("b/262537912")
    public void viewChangesWhileComputingMutation_applyMutationFails() throws Exception {
        Layout layout1 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));
        Layout layout2 =
                layout(
                        arc( // 1
                                props -> props.anchorAngleDegrees = 35,
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));
        Layout layout3 =
                layout(
                        arc( // 1
                                arcText("Hello") // 1.1
                                ));
        // Check the premutation layout
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent1 = renderer.inflate();
        // Compute the mutation
        ViewGroupMutation mutation2 =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent1), layout2, ViewProperties.EMPTY);
        ViewGroupMutation mutation3 =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent1), layout3, ViewProperties.EMPTY);

        renderer.mRenderer.applyMutation(inflatedViewParent1, mutation3).get();
        assertThrows(
                ViewMutationException.class,
                () -> renderer.mRenderer.applyMutation(inflatedViewParent1, mutation2).get());
    }

    @Test
    public void inflateArcThenMutate_withDifferentNumberOfChildren_causesUpdate() {
        Layout layout1 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));

        // Check the premutation layout
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        assertThat(inflatedViewParent.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) inflatedViewParent.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(2);
        CurvedTextView tv1 = (CurvedTextView) arcLayout.getChildAt(0);
        CurvedTextView tv2 = (CurvedTextView) arcLayout.getChildAt(1);
        assertThat(tv1.getText()).isEqualTo("Hello");
        assertThat(tv2.getText()).isEqualTo("World");

        Layout layout2 =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World"), // 1.2
                                arcText("and"), // 1.3
                                arcText("Mars") // 1.4
                                ));

        // Compute the mutation
        ViewGroupMutation mutation =
                renderer.computeMutation(getRenderedMetadata(inflatedViewParent), layout2);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isFalse();

        // Apply the mutation
        boolean mutationResult = renderer.applyMutation(inflatedViewParent, mutation);
        assertThat(mutationResult).isTrue();

        // Check the post-mutation layout
        ArcLayout arcLayoutAfterMutation = (ArcLayout) inflatedViewParent.getChildAt(0);
        assertThat(arcLayoutAfterMutation.getChildCount()).isEqualTo(4);
        CurvedTextView tv1AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(0);
        CurvedTextView tv2AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(1);
        CurvedTextView tv3AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(2);
        CurvedTextView tv4AfterMutation = (CurvedTextView) arcLayoutAfterMutation.getChildAt(3);
        assertThat(tv1AfterMutation.getText()).isEqualTo("Hello");
        assertThat(tv2AfterMutation.getText()).isEqualTo("World");
        assertThat(tv3AfterMutation.getText()).isEqualTo("and");
        assertThat(tv4AfterMutation.getText()).isEqualTo("Mars");
    }

    @Test
    public void inflateAndMutateTwice_causesTwoUpdates() throws Exception {
        Layout layout1 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("World") // 1.2
                                ));

        // Do the initial inflation.
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        ViewGroup column = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1 = (TextView) column.getChildAt(0);
        TextView tv2 = (TextView) column.getChildAt(1);
        assertThat(tv1.getText().toString()).isEqualTo("Hello");
        assertThat(tv2.getText().toString()).isEqualTo("World");

        Layout layout2 =
                layout(
                        column( // 1
                                text("Goodbye"), // 1.1
                                text("World") // 1.2
                                ));

        // Apply first mutation
        ViewGroupMutation mutation1 =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout2, ViewProperties.EMPTY);
        assertThat(mutation1).isNotNull();
        assertThat(mutation1.isNoOp()).isFalse();
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation1).get();

        ViewGroup columnAfterMutation1 = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation1 = (TextView) columnAfterMutation1.getChildAt(0);
        TextView tv2AfterMutation1 = (TextView) columnAfterMutation1.getChildAt(1);
        assertThat(tv1AfterMutation1.getText().toString()).isEqualTo("Goodbye");
        assertThat(tv2AfterMutation1.getText().toString()).isEqualTo("World");

        Layout layout3 =
                layout(
                        column( // 1
                                text("Hello"), // 1.1
                                text("Mars") // 1.2
                                ));

        // Apply second mutation
        ViewGroupMutation mutation2 =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout3, ViewProperties.EMPTY);
        assertThat(mutation2).isNotNull();
        assertThat(mutation2.isNoOp()).isFalse();
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation2).get();

        ViewGroup columnAfterMutation2 = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView tv1AfterMutation2 = (TextView) columnAfterMutation2.getChildAt(0);
        TextView tv2AfterMutation2 = (TextView) columnAfterMutation2.getChildAt(1);
        assertThat(tv1AfterMutation2.getText().toString()).isEqualTo("Hello");
        assertThat(tv2AfterMutation2.getText().toString()).isEqualTo("Mars");
    }

    @Test
    public void inflateArcThenMutate_withNoChange_producesNoOpMutation() {
        Layout layout =
                layout(
                        arc( // 1
                                arcText("Hello"), // 1.1
                                arcText("World") // 1.2
                                ));

        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout);
        ViewGroup inflatedViewParent = renderer.inflate();
        RenderedMetadata renderedMetadata = getRenderedMetadata(inflatedViewParent);

        // Compute the mutation for the same layout
        ViewGroupMutation mutation = renderer.computeMutation(renderedMetadata, layout);
        assertThat(mutation).isNotNull();
        assertThat(mutation.isNoOp()).isTrue();
    }

    @Test
    public void inflateArcWithNoFingerprint_producesNoRenderingMetadata() {
        Layout layout1WithNoFingerprints =
                layout(arc(arcText("Hello"))).toBuilder().clearFingerprint().build();
        // Check that we have the initial layout correctly rendered
        Renderer renderer = renderer(layout1WithNoFingerprints);

        ViewGroup inflatedViewParent = renderer.inflate();

        assertThat(getRenderedMetadata(inflatedViewParent)).isNull();
    }

    @Test
    public void boxWithChild_childChanges_appliesGravityToUpdatedChild() throws Exception {
        Layout layout1 =
                layout(
                        box( // 1
                                boxProps -> {
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_CENTER;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_CENTER;
                                },
                                text("Hello") // 1.1
                                ));
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        Layout layout2 =
                layout(
                        box( // 1
                                boxProps -> {
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_CENTER;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_CENTER;
                                },
                                text("World") // 1.1
                                ));

        ViewGroupMutation mutation =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout2, ViewProperties.EMPTY);
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        ViewGroup boxAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView textAfterMutation = (TextView) boxAfterMutation.getChildAt(0);
        LayoutParams layoutParamsAfterMutation = (LayoutParams) textAfterMutation.getLayoutParams();
        assertThat(layoutParamsAfterMutation.gravity)
                .isEqualTo(
                        getFrameLayoutGravity(
                                HorizontalAlignment.HORIZONTAL_ALIGN_CENTER,
                                VerticalAlignment.VERTICAL_ALIGN_CENTER));
    }

    @Test
    public void boxWithChild_boxChanges_appliesNewGravityToChild() throws Exception {
        Layout layout1 =
                layout(
                        box( // 1
                                boxProps -> {
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_CENTER;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_CENTER;
                                },
                                text("Hello") // 1.1
                                ));
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        Layout layout2 =
                layout(
                        box( // 1
                                boxProps -> {
                                    // A different set of alignments.
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_LEFT;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_BOTTOM;
                                },
                                text("Hello") // 1.1
                                ));

        ViewGroupMutation mutation =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout2, ViewProperties.EMPTY);
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        ViewGroup boxAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView textAfterMutation = (TextView) boxAfterMutation.getChildAt(0);
        LayoutParams layoutParamsAfterMutation = (LayoutParams) textAfterMutation.getLayoutParams();
        assertThat(layoutParamsAfterMutation.gravity)
                .isEqualTo(
                        getFrameLayoutGravity(
                                HorizontalAlignment.HORIZONTAL_ALIGN_LEFT,
                                VerticalAlignment.VERTICAL_ALIGN_BOTTOM));
    }

    @Test
    public void boxWithChild_bothChange_appliesNewGravityToUpdatedChild() throws Exception {
        Layout layout1 =
                layout(
                        box( // 1
                                boxProps -> {
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_CENTER;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_CENTER;
                                },
                                text("Hello") // 1.1
                                ));
        // Do the initial inflation.
        Renderer renderer = renderer(layout1);
        ViewGroup inflatedViewParent = renderer.inflate();
        Layout layout2 =
                layout(
                        box( // 1
                                boxProps -> {
                                    // A different set of alignments.
                                    boxProps.horizontalAlignment =
                                            HorizontalAlignment.HORIZONTAL_ALIGN_LEFT;
                                    boxProps.verticalAlignment =
                                            VerticalAlignment.VERTICAL_ALIGN_BOTTOM;
                                },
                                text("World") // 1.1
                                ));

        ViewGroupMutation mutation =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent), layout2, ViewProperties.EMPTY);
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        ViewGroup boxAfterMutation = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView textAfterMutation = (TextView) boxAfterMutation.getChildAt(0);
        LayoutParams layoutParamsAfterMutation = (LayoutParams) textAfterMutation.getLayoutParams();
        assertThat(layoutParamsAfterMutation.gravity)
                .isEqualTo(
                        getFrameLayoutGravity(
                                HorizontalAlignment.HORIZONTAL_ALIGN_LEFT,
                                VerticalAlignment.VERTICAL_ALIGN_BOTTOM));
    }

    private static Span textSpan(String text) {
        return Span.newBuilder()
                .setText(SpanText.newBuilder().setText(string(text)).build())
                .build();
    }

    private ResourceResolvers.Builder resourceResolvers() {
        return StandardResourceResolvers.forLocalApp(
                buildResources(),
                getApplicationContext(),
                ContextCompat.getMainExecutor(getApplicationContext()),
                true);
    }

    private static Layout fingerprintedLayout(LayoutElement rootElement) {
        return TestFingerprinter.getDefault().buildLayoutWithFingerprints(rootElement);
    }

    private static ProtoLayoutTheme loadTheme(int themeResId) {
        return new ProtoLayoutThemeImpl(getApplicationContext(), themeResId);
    }

    ProtoLayoutInflater.Config.Builder newRendererConfigBuilder(Layout layout) {
        return newRendererConfigBuilder(layout, resourceResolvers());
    }

    ProtoLayoutInflater.Config.Builder newRendererConfigBuilder(
            Layout layout, ResourceResolvers.Builder resourceResolvers) {
        return new ProtoLayoutInflater.Config.Builder(
                        getApplicationContext(), layout, resourceResolvers.build())
                .setClickableIdExtra(EXTRA_CLICKABLE_ID)
                .setLoadActionListener(p -> {})
                .setLoadActionExecutor(ContextCompat.getMainExecutor(getApplicationContext()))
                .setApplyFontVariantBodyAsDefault(true);
    }

    private Renderer renderer(Layout layout) {
        return renderer(newRendererConfigBuilder(layout), new FixedQuotaManagerImpl(MAX_VALUE));
    }

    // Renderer using a dataPipeline with default values.
    private Renderer renderer(ProtoLayoutInflater.Config.Builder rendererConfigBuilder) {
        return renderer(rendererConfigBuilder, new FixedQuotaManagerImpl(MAX_VALUE));
    }

    @SuppressWarnings("RestrictTo")
    private Renderer renderer(
            ProtoLayoutInflater.Config.Builder rendererConfigBuilder,
            FixedQuotaManagerImpl quotaManager) {
        mDataPipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* platformDataProviders= */ ImmutableMap.of(),
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        rendererConfigBuilder.setDynamicDataPipeline(mDataPipeline);
        return new Renderer(rendererConfigBuilder.build(), mDataPipeline);
    }

    @SuppressWarnings("RestrictTo")
    private static final class Renderer {
        final ProtoLayoutInflater mRenderer;
        final ProtoLayoutDynamicDataPipeline mDataPipeline;
        static ActivityController<Activity> sActivityController = null;

        Renderer(
                ProtoLayoutInflater.Config rendererConfig,
                ProtoLayoutDynamicDataPipeline dataPipeline) {
            this.mRenderer = new ProtoLayoutInflater(rendererConfig);
            this.mDataPipeline = dataPipeline;
        }

        static void cleanUp() {
            if (sActivityController != null) {
                sActivityController.destroy();
                sActivityController = null;
            }
        }

        FrameLayout inflate() {
            cleanUp();

            FrameLayout rootLayout = new FrameLayout(getApplicationContext());
            // This needs to be an attached view to test animations in data pipeline.
            sActivityController = Robolectric.buildActivity(Activity.class).setup();
            sActivityController.get().setContentView(rootLayout);
            InflateResult inflateResult = mRenderer.inflate(rootLayout);
            if (inflateResult != null) {
                inflateResult.updateDynamicDataPipeline(/* isReattaching= */ false);
            }
            shadowOf(Looper.getMainLooper()).idle();
            doLayout(rootLayout);

            return rootLayout;
        }

        ViewGroupMutation computeMutation(RenderedMetadata renderedMetadata, Layout targetLayout) {
            return mRenderer.computeMutation(renderedMetadata, targetLayout, ViewProperties.EMPTY);
        }

        boolean applyMutation(ViewGroup parent, ViewGroupMutation mutation) {
            try {
                ListenableFuture<RenderingArtifact> applyMutationFuture =
                        mRenderer.applyMutation(parent, mutation);
                shadowOf(Looper.getMainLooper()).idle();
                applyMutationFuture.get();
                doLayout(parent);
                return true;
            } catch (ViewMutationException | ExecutionException | InterruptedException ex) {
                return false;
            }
        }

        int getDataPipelineSize() {
            return mDataPipeline.size();
        }

        private void doLayout(View rootLayout) {
            // Run a layout pass etc. This is required for basically everything that tries to make
            // assertions about width/height, or relative placement.
            int screenWidth = MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY);
            int screenHeight = MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY);
            rootLayout.measure(screenWidth, screenHeight);
            rootLayout.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }
    }

    private static Resources buildResources() {
        return Resources.newBuilder()
                .putIdToImage(
                        "android",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(R.drawable.android_24dp))
                                .build())
                .putIdToImage(
                        "android_AVD",
                        ImageResource.newBuilder()
                                .setAndroidAnimatedResourceByResId(
                                        AndroidAnimatedImageResourceByResId.newBuilder()
                                                .setAnimatedImageFormat(
                                                        AnimatedImageFormat
                                                                .ANIMATED_IMAGE_FORMAT_AVD)
                                                .setResourceId(android_animated_24dp)
                                                .setStartTrigger(onVisibleTrigger()))
                                .build())
                .putIdToImage(
                        "android_AVD_pretending_to_be_static",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(android_animated_24dp))
                                .build())
                .putIdToImage(
                        "android_seekable_AVD",
                        ImageResource.newBuilder()
                                .setAndroidSeekableAnimatedResourceByResId(
                                        AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                                                .setAnimatedImageFormat(
                                                        AnimatedImageFormat
                                                                .ANIMATED_IMAGE_FORMAT_AVD)
                                                .setResourceId(android_animated_24dp)
                                                .setProgress(
                                                        DynamicFloat.newBuilder()
                                                                .setAnimatableDynamic(
                                                                        stateDynamicFloat())
                                                                .build())
                                                .build())
                                .build())
                .putIdToImage(
                        "does_not_exist",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder().setResourceId(-1))
                                .build())
                .putIdToImage(
                        "large_image_120dp",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(R.drawable.ic_channel_foreground))
                                .build())
                .putIdToImage("no_android_resource_set", ImageResource.getDefaultInstance())
                .build();
    }

    private static @NonNull Trigger onVisibleTrigger() {
        return Trigger.newBuilder()
                .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance())
                .build();
    }

    private static AnimatableDynamicFloat.@NonNull Builder stateDynamicFloat() {
        return AnimatableDynamicFloat.newBuilder()
                .setInput(
                        DynamicFloat.newBuilder()
                                .setStateSource(
                                        StateFloatSource.newBuilder().setSourceKey("anim_val")));
    }

    @Test
    public void inflate_row_withLayoutWeight() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 30);

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(
                                Row.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setRow(
                                                                Row.newBuilder()
                                                                        .setWidth(expandWeight())
                                                                        .addContents(image)
                                                                        .build())))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup which is a LinearLayout.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);
        ViewGroup rowWithWeight = (ViewGroup) firstChild.getChildAt(0);

        LinearLayout.LayoutParams linearLayoutParams =
                (LinearLayout.LayoutParams) rowWithWeight.getLayoutParams();

        expect.that(linearLayoutParams.weight).isEqualTo(10.0f);
    }

    private static @NonNull ContainerDimension expandWeight() {
        return ContainerDimension.newBuilder()
                .setExpandedDimension(
                        ExpandedDimensionProp.newBuilder()
                                .setLayoutWeight(FloatProp.newBuilder().setValue(10.0f).build())
                                .build())
                .build();
    }

    @Test
    public void inflate_column_withLayoutWeight() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 30);

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setColumn(
                                Column.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setColumn(
                                                                Column.newBuilder()
                                                                        .setHeight(expandWeight())
                                                                        .addContents(image)
                                                                        .build())))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup which is a LinearLayout.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);
        ViewGroup columnWithWeight = (ViewGroup) firstChild.getChildAt(0);

        LinearLayout.LayoutParams linearLayoutParams =
                (LinearLayout.LayoutParams) columnWithWeight.getLayoutParams();

        expect.that(linearLayoutParams.weight).isEqualTo(10.0f);
    }

    @Test
    public void inflate_box_withLayoutWeight() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 30);

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(
                                Row.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .addContents(
                                                LayoutElement.newBuilder()
                                                        .setBox(
                                                                Box.newBuilder()
                                                                        .setWidth(expandWeight())
                                                                        .addContents(image)
                                                                        .build())))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup which is a LinearLayout.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);
        ViewGroup boxWithWeight = (ViewGroup) firstChild.getChildAt(0);

        LinearLayout.LayoutParams linearLayoutParams =
                (LinearLayout.LayoutParams) boxWithWeight.getLayoutParams();

        expect.that(linearLayoutParams.weight).isEqualTo(10.0f);
    }

    @Test
    public void inflate_box_withHiddenModifier() {
        final String protoResId = "android";
        final String boolKey = "bool-key";

        LayoutElement image = buildImage(protoResId, 30, 30);

        BoolProp.Builder stateBoolPropBuilder =
                BoolProp.newBuilder()
                        .setValue(true)
                        .setDynamicValue(
                                DynamicBool.newBuilder()
                                        .setStateSource(
                                                StateBoolSource.newBuilder()
                                                        .setSourceKey(boolKey)));
        LayoutElement.Builder boxBuilder =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(image)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setHidden(stateBoolPropBuilder)));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(Row.newBuilder().addContents(boxBuilder).addContents(image))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup which is a LinearLayout.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);
        ViewGroup box = (ViewGroup) firstChild.getChildAt(0);
        ViewGroup secondImage = (ViewGroup) firstChild.getChildAt(1);

        // The box should be hidden but still take some space (as it wraps around its inner image)
        assertThat(box.getWidth()).isGreaterThan(0);
        assertThat(box.getVisibility()).isEqualTo(INVISIBLE);

        // The second image should start after the hidden (but not gone) box.
        int secondImageLeft = secondImage.getLeft();
        assertThat(secondImageLeft).isEqualTo(box.getWidth());
        assertThat(box.getWidth()).isEqualTo(secondImage.getWidth());

        // Try to unhide the box.
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        new AppDataKey<DynamicBuilders.DynamicBool>(boolKey),
                        DynamicDataValue.newBuilder()
                                .setBoolVal(FixedBool.newBuilder().setValue(false))
                                .build()));

        assertThat(box.getVisibility()).isEqualTo(VISIBLE);
        // The second image shouldn't move around.
        assertThat(secondImage.getLeft()).isEqualTo(secondImageLeft);
    }

    @Test
    public void inflate_box_withVisibleModifier() {
        final String protoResId = "android";
        final String boolKey = "bool-key";

        LayoutElement image = buildImage(protoResId, 30, 30);

        BoolProp.Builder stateBoolPropBuilder =
                BoolProp.newBuilder()
                        .setValue(true)
                        .setDynamicValue(
                                DynamicBool.newBuilder()
                                        .setStateSource(
                                                StateBoolSource.newBuilder()
                                                        .setSourceKey(boolKey)));
        BoolProp.Builder alwaysTrueBoolPropBuilder =
                BoolProp.newBuilder()
                        .setValue(true)
                        .setDynamicValue(
                                DynamicBool.newBuilder()
                                        .setFixed(FixedBool.newBuilder().setValue(true)));
        LayoutElement.Builder boxBuilder =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(image)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setVisible(stateBoolPropBuilder)
                                                        // This should be ignored
                                                        .setHidden(alwaysTrueBoolPropBuilder)));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setRow(Row.newBuilder().addContents(boxBuilder).addContents(image))
                        .build();

        FrameLayout layout = renderer(fingerprintedLayout(root)).inflate();

        // There should be a child ViewGroup which is a LinearLayout.
        assertThat(layout.getChildAt(0)).isInstanceOf(ViewGroup.class);
        ViewGroup firstChild = (ViewGroup) layout.getChildAt(0);
        ViewGroup box = (ViewGroup) firstChild.getChildAt(0);
        ViewGroup secondImage = (ViewGroup) firstChild.getChildAt(1);

        assertThat(box.getWidth()).isGreaterThan(0);
        assertThat(box.getVisibility()).isEqualTo(VISIBLE);

        // The second image should start after the hidden (but not gone) box.
        int secondImageLeft = secondImage.getLeft();
        assertThat(secondImageLeft).isEqualTo(box.getWidth());
        assertThat(box.getWidth()).isEqualTo(secondImage.getWidth());

        // Try to hide the box.
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        new AppDataKey<DynamicBuilders.DynamicBool>(boolKey),
                        DynamicDataValue.newBuilder()
                                .setBoolVal(FixedBool.newBuilder().setValue(false))
                                .build()));

        // The box should be hidden but still take some space (as it wraps around its inner image)
        assertThat(box.getVisibility()).isEqualTo(INVISIBLE);
        // The second image shouldn't move around.
        assertThat(secondImage.getLeft()).isEqualTo(secondImageLeft);
    }

    @Test
    public void inflate_box_withOpacityModifier() {
        float opacity = 0.7f;
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(expand())
                                        .setHeight(expand())
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setOpacity(
                                                                FloatProp.newBuilder()
                                                                        .setValue(opacity)
                                                                        .build())
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = rootLayout.getChildAt(0);

        assertThat(box.getAlpha()).isEqualTo(opacity);
    }

    @Test
    public void inflate_box_withTransformationModifier() {
        DpProp translationX = dp(10.f).build();
        DpProp translationY = dp(12.f).build();
        DegreesProp degree = degrees(60).build();
        FloatProp scaleX = FloatProp.newBuilder().setValue(2.f).build();
        FloatProp scaleY = FloatProp.newBuilder().setValue(3.f).build();
        PivotDimension pivotY =
                PivotDimension.newBuilder()
                        .setLocationRatio(
                                BoundingBoxRatio.newBuilder()
                                        .setRatio(FloatProp.newBuilder().setValue(0.4f).build())
                                        .build())
                        .build();
        ModifiersProto.Transformation transformation =
                ModifiersProto.Transformation.newBuilder()
                        .setTranslationX(translationX)
                        .setTranslationY(translationY)
                        .setRotation(degree)
                        .setScaleX(scaleX)
                        .setScaleY(scaleY)
                        .setPivotY(pivotY) // without setting pivotX
                        .build();

        ContainerDimension boxWidth =
                ContainerDimension.newBuilder().setLinearDimension(dp(100.f).build()).build();
        ContainerDimension boxHeight =
                ContainerDimension.newBuilder().setLinearDimension(dp(120.f).build()).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(boxWidth)
                                        .setHeight(boxHeight)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setTransformation(transformation)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = rootLayout.getChildAt(0);

        assertThat(box.getTranslationX()).isEqualTo(translationX.getValue());
        assertThat(box.getTranslationY()).isEqualTo(translationY.getValue());
        assertThat(box.getRotation()).isEqualTo(degree.getValue());
        assertThat(box.getScaleX()).isEqualTo(scaleX.getValue());
        assertThat(box.getScaleY()).isEqualTo(scaleY.getValue());
        // TODO(b/342379311): reenable the test when robolectric returns the correct default
        //  location.
        // pivot is default to the middle of the element.
        // assertThat(box.getPivotX()).isEqualTo(boxWidth.getLinearDimension().getValue() * 0.5f);
        assertThat(box.getPivotY())
                .isEqualTo(
                        boxHeight.getLinearDimension().getValue()
                                * pivotY.getLocationRatio().getRatio().getValue());
    }

    @Test
    public void inflate_box_wrapAndExpandSize_withPivotTransformationModifier() {
        PivotDimension pivotX = PivotDimension.newBuilder().setOffsetDp(dp(30.f)).build();
        PivotDimension pivotY =
                PivotDimension.newBuilder()
                        .setLocationRatio(
                                BoundingBoxRatio.newBuilder()
                                        .setRatio(FloatProp.newBuilder().setValue(0.4f).build())
                                        .build())
                        .build();
        ModifiersProto.Transformation transformation =
                ModifiersProto.Transformation.newBuilder()
                        .setPivotX(pivotX)
                        .setPivotY(pivotY)
                        .build();

        ContainerDimension outerBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(100.f).build()).build();
        ContainerDimension innerBoxSize =
                ContainerDimension.newBuilder().setLinearDimension(dp(60.f).build()).build();
        Box.Builder boxBuilder =
                Box.newBuilder()
                        .setWidth(expand())
                        .setHeight(wrap())
                        .setModifiers(
                                Modifiers.newBuilder().setTransformation(transformation).build())
                        .addContents(
                                LayoutElement.newBuilder()
                                        .setBox(
                                                Box.newBuilder()
                                                        .setWidth(innerBoxSize)
                                                        .setHeight(innerBoxSize)));
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(outerBoxSize)
                                        .setHeight(outerBoxSize)
                                        .addContents(LayoutElement.newBuilder().setBox(boxBuilder)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = ((ViewGroup) rootLayout.getChildAt(0)).getChildAt(0);

        assertThat(box.getPivotX())
                .isEqualTo(
                        outerBoxSize.getLinearDimension().getValue() * 0.5f
                                + pivotX.getOffsetDp().getValue());
        assertThat(box.getPivotY())
                .isEqualTo(
                        innerBoxSize.getLinearDimension().getValue()
                                * pivotY.getLocationRatio().getRatio().getValue());

        // default values for translation, rotation and scale
        assertThat(box.getTranslationX()).isEqualTo(0);
        assertThat(box.getTranslationY()).isEqualTo(0);
        assertThat(box.getRotation()).isEqualTo(0);
        assertThat(box.getScaleX()).isEqualTo(1);
        assertThat(box.getScaleY()).isEqualTo(1);
    }

    // TODO(b/342379311): reenable the test when robolectric returns the correct default location.
    @Ignore // b/342225240
    @Test
    public void inflate_box_withPivotTransformationModifier_noValidPivot_defaultToCenter() {
        // PivotDimension without offSetDp nor locationRation
        PivotDimension pivotDimension = PivotDimension.newBuilder().build();
        ModifiersProto.Transformation transformation =
                ModifiersProto.Transformation.newBuilder().setPivotX(pivotDimension).build();
        ContainerDimension boxWidth =
                ContainerDimension.newBuilder().setLinearDimension(dp(100.f).build()).build();
        ContainerDimension boxHeight =
                ContainerDimension.newBuilder().setLinearDimension(dp(120.f).build()).build();
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .setWidth(boxWidth)
                                        .setHeight(boxHeight)
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setTransformation(transformation)
                                                        .build()))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = rootLayout.getChildAt(0);
        assertThat(box.getPivotX()).isEqualTo(boxWidth.getLinearDimension().getValue() * 0.5f);
        assertThat(box.getPivotY()).isEqualTo(boxHeight.getLinearDimension().getValue() * 0.5f);
    }

    @Test
    public void inflate_textWithTransformation_inArcAdapter_skipped() {
        LayoutElement.Builder text =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(string("test"))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setTransformation(
                                                                ModifiersProto.Transformation
                                                                        .newBuilder()))
                                        .build());
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setAdapter(
                                                                ArcAdapter.newBuilder()
                                                                        .setContent(text))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        // The text inside the ArcAdapter is skipped
        assertThat(arcLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_boxWithFourAsymmetricalCorners() {
        float[] rValues = new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f};
        Corner corner =
                Corner.newBuilder()
                        .setTopLeftRadius(
                                CornerRadius.newBuilder().setX(dp(rValues[0])).setY(dp(rValues[1])))
                        .setTopRightRadius(
                                CornerRadius.newBuilder().setX(dp(rValues[2])).setY(dp(rValues[3])))
                        .setBottomRightRadius(
                                CornerRadius.newBuilder().setX(dp(rValues[4])).setY(dp(rValues[5])))
                        .setBottomLeftRadius(
                                CornerRadius.newBuilder().setX(dp(rValues[6])).setY(dp(rValues[7])))
                        .build();
        Box.Builder boxBuilder =
                Box.newBuilder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setModifiers(
                                Modifiers.newBuilder()
                                        .setBackground(
                                                ModifiersProto.Background.newBuilder()
                                                        .setCorner(corner))
                                        .build());
        LayoutElement root = LayoutElement.newBuilder().setBox(boxBuilder).build();
        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = rootLayout.getChildAt(0);
        Drawable background = box.getBackground();
        assertThat(background).isInstanceOf(GradientDrawable.class);
        float[] radii = ((GradientDrawable) background).getCornerRadii();
        assertThat(Arrays.equals(radii, rValues)).isTrue();
    }

    @Test
    public void inflate_boxWithOneAsymmetricalCorner() {
        float[] rValues = new float[] {1f, 1f, 3f, 4f, 1f, 1f, 1f, 1f};
        Corner corner =
                Corner.newBuilder()
                        .setRadius(dp(rValues[0]))
                        .setTopRightRadius(
                                CornerRadius.newBuilder().setX(dp(rValues[2])).setY(dp(rValues[3])))
                        .build();
        Box.Builder boxBuilder =
                Box.newBuilder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setModifiers(
                                Modifiers.newBuilder()
                                        .setBackground(
                                                ModifiersProto.Background.newBuilder()
                                                        .setCorner(corner))
                                        .build());
        LayoutElement root = LayoutElement.newBuilder().setBox(boxBuilder).build();
        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        View box = rootLayout.getChildAt(0);
        Drawable background = box.getBackground();
        assertThat(background).isInstanceOf(GradientDrawable.class);
        float[] radii = ((GradientDrawable) background).getCornerRadii();
        assertThat(Arrays.equals(radii, rValues)).isTrue();
    }

    @Test
    public void enterTransition_noQuota_notPlayed() throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(fingerprintedLayout(textFadeIn("Hello"))),
                        new FixedQuotaManagerImpl(/* quotaCap= */ 0));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void enterTransition_animationEnabled_hasEnterAnimation() throws Exception {
        Renderer renderer = renderer(fingerprintedLayout(textFadeIn("Hello")));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        // Idle for running code for starting animations.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);
    }

    @Test
    public void multipleEnterTransition_animationEnabled_correctlyReleaseQuota() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                LayoutElement.newBuilder()
                                        .setColumn(
                                                Column.newBuilder()
                                                        .addContents(textFadeIn("Hello"))
                                                        .addContents(textFadeInSlideIn("Hello2"))
                                                        .build())
                                        .build()));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                LayoutElement.newBuilder()
                                        .setColumn(
                                                Column.newBuilder()
                                                        .addContents(textFadeIn("World"))
                                                        .addContents(textFadeInSlideIn("World2"))
                                                        .build())
                                        .build()));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        // Idle for running code for starting animations.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(/* fadeInx2 + slideIn */ 3);

        // This is needed to let enter animations finish.
        ShadowChoreographer.setPaused(false);
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1));
        shadowOf(getMainLooper()).idle();
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        assertThat(mDataPipeline.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void multipleEnterTransition_withDelay_animationEnabled_notOverlapping_correctlyPlays()
            throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(
                                fingerprintedLayout(
                                        LayoutElement.newBuilder()
                                                .setColumn(
                                                        Column.newBuilder()
                                                                .addContents(
                                                                        textFadeIn(
                                                                                "Hello",
                                                                                /* delay= */ 0))
                                                                .addContents(
                                                                        textFadeIn(
                                                                                "Hello2",
                                                                                /* delay= */ 600)))
                                                .build())),
                        new FixedQuotaManagerImpl(/* quotaCap= */ 1));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();

        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                LayoutElement.newBuilder()
                                        .setColumn(
                                                Column.newBuilder()
                                                        .addContents(
                                                                textFadeIn("World", /* delay= */ 0))
                                                        .addContents(
                                                                textFadeIn(
                                                                        "World2",
                                                                        /* delay= */ 600)))
                                        .build()));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        // First content transition animation Idle for running code for starting animations.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        // Second content transition animation Idle for running code for starting animations.
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(500));
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        // Quota cap is 1, but since the first animating is finished, this should be played too.
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        // This is needed to let enter animations finish.
        ShadowChoreographer.setPaused(false);
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1));
        shadowOf(getMainLooper()).idle();
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        assertThat(mDataPipeline.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void multipleEnterTransition_withDelay_animationEnabled_overlapping_playesOne()
            throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(
                                fingerprintedLayout(
                                        LayoutElement.newBuilder()
                                                .setColumn(
                                                        Column.newBuilder()
                                                                .addContents(
                                                                        textFadeIn(
                                                                                "Hello",
                                                                                /* delay= */ 400))
                                                                .addContents(
                                                                        textFadeIn(
                                                                                "Hello2",
                                                                                /* delay= */ 600)))
                                                .build())),
                        new FixedQuotaManagerImpl(/* quotaCap= */ 1));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();

        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                LayoutElement.newBuilder()
                                        .setColumn(
                                                Column.newBuilder()
                                                        .addContents(
                                                                textFadeIn(
                                                                        "World", /* delay= */ 400))
                                                        .addContents(
                                                                textFadeIn(
                                                                        "World2",
                                                                        /* delay= */ 600)))
                                        .build()));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        // First content transition animation Idle for running code for starting animations.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        // Since we've run delayed tasks, second animation also got a chance to be run, but quota
        // prevented it.
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        // This is needed to let enter animations finish.
        ShadowChoreographer.setPaused(false);
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1));
        shadowOf(getMainLooper()).idle();
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        assertThat(mDataPipeline.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void enterTransition_animationDisabled_noEnterAnimations() throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(fingerprintedLayout(textFadeIn("Hello")))
                                .setAnimationEnabled(false));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void enterTransition_notFullyVisible_noEnterAnimation() throws Exception {
        Renderer renderer = renderer(fingerprintedLayout(textFadeIn("Hello")));
        mDataPipeline.setFullyVisible(false);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void exitTransition_noQuota_notPlayed_withDynamicNode() {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(
                                fingerprintedLayout(
                                        getDynamicTextElementWithExitAnimation(
                                                "Hello", /* iterations= */ 1))),
                        new FixedQuotaManagerImpl(/* quotaCap= */ 0));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));

        Runnable onEndTest = mock(Runnable.class);

        mutation.mPipelineMaker
                .get()
                .playExitAnimations(inflatedViewParent, /* isReattaching= */ false, onEndTest);

        shadowOf(Looper.getMainLooper()).idle();

        verify(onEndTest).run();
    }

    @Test
    public void exitTransition_noQuota_notPlayed() throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(
                                fingerprintedLayout(
                                        getTextElementWithExitAnimation(
                                                "Hello", /* iterations= */ 1))),
                        new FixedQuotaManagerImpl(/* quotaCap= */ 0));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void exitTransition_animationEnabled_hasExitAnimation() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 1)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(textFadeIn("World")));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        // Idle for running code for starting animations.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        // Idle for calling the onStart listener so that animation has started status.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        // Waiting on onAnimationEnd listener so that future is resolved.
        ShadowChoreographer.setPaused(false);
        shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));

        applyMutationFuture.get();
    }

    @Test
    public void exitTransition_indefiniteRepeatable_ignored() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 0)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 0)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        applyMutationFuture.get();
    }

    @Test
    public void exitTransition_animationDisabled_noExitAnimations() throws Exception {
        Renderer renderer =
                renderer(
                        newRendererConfigBuilder(
                                        fingerprintedLayout(
                                                getTextElementWithExitAnimation(
                                                        "Hello", /* iterations= */ 1)))
                                .setAnimationEnabled(false));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 1)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        applyMutationFuture.get();
    }

    @Test
    public void exitTransition_notFullyVisible_noExitAnimation() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 1)));
        mDataPipeline.setFullyVisible(false);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 1)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        applyMutationFuture.get();
    }

    @Test
    public void exitTransition_noChangeToLayoutContentWhileExitAnimationIsPlaying()
            throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 10)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 10)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);
        assertThat(((TextView) inflatedViewParent.getChildAt(0)).getText().toString())
                .isEqualTo("Hello");

        ShadowChoreographer.setPaused(false);
        shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));
        applyMutationFuture.get();
    }

    @Test
    public void exitTransition_afterExitAnimationsEnd_newLayoutGetApplied() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 10)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 10)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);
        shadowOf(getMainLooper()).idle();

        applyMutationFuture.get();
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        assertThat(((TextView) inflatedViewParent.getChildAt(0)).getText().toString())
                .isEqualTo("World");
    }

    @Test
    public void exitTransition_removedNodes_triggersExitAnimation() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getMultipleTextElementWithExitAnimation(
                                        Arrays.asList("Hello", "World"), /* iterations= */ 10)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getMultipleTextElementWithExitAnimation(
                                        Arrays.asList("Hello"), /* iterations= */ 10)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(2);

        ShadowChoreographer.setPaused(false);
        shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));
        applyMutationFuture.get();
    }

    @Test
    public void layoutGetsApplied_whenApplyingSecondMutation_beforeExitAnimationsAreFinished()
            throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("Hello", /* iterations= */ 10)));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation("World", /* iterations= */ 10)));
        ListenableFuture<RenderingArtifact> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        ViewGroupMutation secondMutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithExitAnimation(
                                        "Second mutation", /* iterations= */ 10)));

        ListenableFuture<RenderingArtifact> applySecondMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, secondMutation);

        // the previous mutation should be finished
        assertThat(applyMutationFuture.isDone()).isTrue();
        assertThat(((TextView) inflatedViewParent.getChildAt(0)).getText().toString())
                .isEqualTo("World");

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(1);

        ShadowChoreographer.setPaused(false);
        shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));
        applySecondMutationFuture.get();

        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(0);
        assertThat(((TextView) inflatedViewParent.getChildAt(0)).getText().toString())
                .isEqualTo("Second mutation");
    }

    @Test
    public void slideInTransition_snapToOutside_startsFromOutsideParentBounds() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithSlideInAnimation(
                                        "Hello",
                                        /* snapTo= */ SLIDE_PARENT_SNAP_TO_OUTSIDE.getNumber())));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithSlideInAnimation(
                                        "World",
                                        /* snapTo= */ SLIDE_PARENT_SNAP_TO_OUTSIDE.getNumber())));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));

        ViewGroup box = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView textView = (TextView) box.getChildAt(0);
        assertSlideInInitialOffset(
                box,
                textView,
                inflatedViewParent.getLeft() - (textView.getLeft() + textView.getWidth()));
    }

    @Test
    public void slideInTransition_snapToInside_startsFromInsideParentBounds() throws Exception {
        Renderer renderer =
                renderer(
                        fingerprintedLayout(
                                getTextElementWithSlideInAnimation(
                                        "Hello",
                                        /* snapTo= */ SLIDE_PARENT_SNAP_TO_INSIDE.getNumber())));
        mDataPipeline.setFullyVisible(true);
        FrameLayout inflatedViewParent = renderer.inflate();
        shadowOf(getMainLooper()).idle();
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));

        ViewGroupMutation mutation =
                renderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent),
                        fingerprintedLayout(
                                getTextElementWithSlideInAnimation(
                                        "World",
                                        /* snapTo= */ SLIDE_PARENT_SNAP_TO_INSIDE.getNumber())));
        renderer.mRenderer.applyMutation(inflatedViewParent, mutation).get();
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));

        ViewGroup box = (ViewGroup) inflatedViewParent.getChildAt(0);
        TextView textView = (TextView) box.getChildAt(0);
        assertSlideInInitialOffset(
                box, textView, inflatedViewParent.getLeft() - textView.getLeft());
    }

    @Test
    public void inflate_dashedArcLine_dynamicLength() {
        AppDataKey<DynamicBuilders.DynamicFloat> keyFoo = new AppDataKey<>("foo");
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(10F))
                                .build()));

        DynamicFloat arcLength =
                DynamicFloat.newBuilder()
                        .setStateSource(StateFloatSource.newBuilder().setSourceKey("foo").build())
                        .build();

        DashedArcLine dashedArcLine =
                DashedArcLine.newBuilder()
                        // Shorter than 360 degrees, so should be drawn as an arc:
                        .setLength(degreesDynamic(arcLength, /* valueForLayout= */ 180f))
                        .setThickness(dp(12))
                        .build();

        WearDashedArcLineView lineView = inflateDashedArcLine(dashedArcLine);
        assertThat(lineView.getSweepAngleDegrees()).isEqualTo(10);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(20F))
                                .build()));

        assertThat(lineView.getSweepAngleDegrees()).isEqualTo(20);
    }

    @Test
    public void inflate_dashedArcLine_usesArcLength() {
        DynamicFloat arcLength =
                DynamicFloat.newBuilder().setFixed(FixedFloat.newBuilder().setValue(45f)).build();

        DashedArcLine dashedArcLine =
                DashedArcLine.newBuilder()
                        .setLength(degreesDynamic(arcLength, /* valueForLayout= */ 0f))
                        .setThickness(dp(12))
                        .build();

        WearDashedArcLineView lineView = inflateDashedArcLine(dashedArcLine);
        expect.that(lineView.getSweepAngleDegrees()).isEqualTo(45f);
        expect.that(lineView.getMaxSweepAngleDegrees()).isEqualTo(360f);
    }

    @Test
    public void inflate_dashedArcLine_dynamicColor() {
        AppDataKey<DynamicBuilders.DynamicColor> keyFoo = new AppDataKey<>("foo");
        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(Color.CYAN))
                                .build()));

        DynamicColor arcColor =
                DynamicColor.newBuilder()
                        .setStateSource(StateColorSource.newBuilder().setSourceKey("foo").build())
                        .build();

        DashedArcLine dashedArcLine =
                DashedArcLine.newBuilder()
                        // Shorter than 360 degrees, so should be drawn as an arc:
                        .setLength(degrees(180))
                        .setColor(ColorProp.newBuilder().setDynamicValue(arcColor))
                        .setThickness(dp(12))
                        .build();

        WearDashedArcLineView lineView = inflateDashedArcLine(dashedArcLine);
        assertThat(lineView.getColor()).isEqualTo(Color.CYAN);

        mStateStore.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(Color.MAGENTA))
                                .build()));

        assertThat(lineView.getColor()).isEqualTo(Color.MAGENTA);
    }

    private WearDashedArcLineView inflateDashedArcLine(DashedArcLine dashedArcLine) {
        LayoutElement root =
                LayoutElement.newBuilder()
                        .setArc(
                                Arc.newBuilder()
                                        .addContents(
                                                ArcLayoutElement.newBuilder()
                                                        .setDashedLine(dashedArcLine)))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);

        if (arcLayout.getChildAt(0) instanceof SizedArcContainer) {
            return (WearDashedArcLineView)
                    ((SizedArcContainer) arcLayout.getChildAt(0)).getChildAt(0);
        }
        return (WearDashedArcLineView) arcLayout.getChildAt(0);
    }

    private void assertSlideInInitialOffset(
            ViewGroup box, TextView textView, float expectedInitialOffset) {
        Animation animation = textView.getAnimation();
        animation.initialize(
                textView.getWidth(), textView.getHeight(), box.getWidth(), box.getHeight());
        Transformation transformation = new Transformation();
        textView.getAnimation().getTransformation(0, transformation);
        float[] matrix = new float[9];
        transformation.getMatrix().getValues(matrix);
        assertThat(matrix[2]).isWithin(0.1f).of(expectedInitialOffset);
    }

    private LayoutElement textFadeIn(String text) {
        return textFadeIn(text, 0);
    }

    private LayoutElement textFadeIn(String text, int delay) {
        return LayoutElement.newBuilder()
                .setText(
                        Text.newBuilder()
                                .setModifiers(
                                        Modifiers.newBuilder()
                                                .setContentUpdateAnimation(
                                                        AnimatedVisibility.newBuilder()
                                                                .setEnterTransition(
                                                                        enterFadeIn(delay))))
                                .setText(string(text)))
                .build();
    }

    private static EnterTransition.@NonNull Builder enterFadeIn(int delay) {
        return EnterTransition.newBuilder().setFadeIn(fadeIn(delay));
    }

    private static FadeInTransition.@NonNull Builder fadeIn(int delay) {
        return FadeInTransition.newBuilder()
                .setAnimationSpec(
                        AnimationSpec.newBuilder()
                                .setAnimationParameters(
                                        AnimationParameters.newBuilder().setDelayMillis(delay)));
    }

    private LayoutElement textFadeInSlideIn(String text) {
        return LayoutElement.newBuilder()
                .setText(
                        textAnimVisibility(
                                AnimatedVisibility.newBuilder()
                                        .setEnterTransition(
                                                EnterTransition.newBuilder()
                                                        .setFadeIn(
                                                                FadeInTransition
                                                                        .getDefaultInstance())
                                                        .setSlideIn(
                                                                SlideInTransition.newBuilder()
                                                                        .build())),
                                text))
                .build();
    }

    private LayoutElement getTextElementWithExitAnimation(String text, int iterations) {
        return LayoutElement.newBuilder()
                .setText(
                        textAnimVisibility(
                                AnimatedVisibility.newBuilder()
                                        .setExitTransition(getFadeOutExitAnimation(iterations)),
                                text))
                .build();
    }

    private LayoutElement getDynamicTextElementWithExitAnimation(String text, int iterations) {
        return LayoutElement.newBuilder()
                .setText(
                        dynamicTextAnimVisibility(
                                AnimatedVisibility.newBuilder()
                                        .setExitTransition(getFadeOutExitAnimation(iterations)),
                                text))
                .build();
    }

    private LayoutElement getMultipleTextElementWithExitAnimation(
            List<String> texts, int iterations) {
        Column.Builder column = Column.newBuilder();
        for (String text : texts) {
            column.addContents(
                    LayoutElement.newBuilder()
                            .setText(
                                    textAnimVisibility(
                                            AnimatedVisibility.newBuilder()
                                                    .setExitTransition(
                                                            getFadeOutExitAnimation(iterations)),
                                            text)));
        }

        return LayoutElement.newBuilder().setColumn(column).build();
    }

    private ExitTransition.Builder getFadeOutExitAnimation(int iterations) {
        return ExitTransition.newBuilder()
                .setFadeOut(
                        FadeOutTransition.newBuilder()
                                .setAnimationSpec(
                                        AnimationSpec.newBuilder()
                                                .setRepeatable(
                                                        Repeatable.newBuilder()
                                                                .setIterations(iterations)
                                                                .build())));
    }

    private LayoutElement getTextElementWithSlideInAnimation(String text, int snapTo) {
        return LayoutElement.newBuilder()
                .setBox(
                        Box.newBuilder()
                                .setWidth(expand())
                                .setHeight(expand())
                                .addContents(
                                        LayoutElement.newBuilder()
                                                .setText(
                                                        textAnimVisibility(
                                                                AnimatedVisibility.newBuilder()
                                                                        .setEnterTransition(
                                                                                slideIn(snapTo)),
                                                                text))))
                .build();
    }

    private Text.@NonNull Builder textAnimVisibility(
            AnimatedVisibility.Builder snapTo, String text) {
        return Text.newBuilder()
                .setModifiers(Modifiers.newBuilder().setContentUpdateAnimation(snapTo.build()))
                .setText(string(text).build());
    }

    private Text.@NonNull Builder dynamicTextAnimVisibility(
            AnimatedVisibility.Builder snapTo, String text) {
        return Text.newBuilder()
                .setModifiers(Modifiers.newBuilder().setContentUpdateAnimation(snapTo.build()))
                .setText(
                        StringProp.newBuilder()
                                .setDynamicValue(
                                        DynamicString.newBuilder()
                                                .setFixed(
                                                        FixedString.newBuilder()
                                                                .setValue(text)
                                                                .build())
                                                .build())
                                .build());
    }

    private EnterTransition.Builder slideIn(int snapTo) {
        return EnterTransition.newBuilder()
                .setSlideIn(
                        SlideInTransition.newBuilder()
                                .setDirection(SlideDirection.SLIDE_DIRECTION_LEFT_TO_RIGHT)
                                .setInitialSlideBound(
                                        SlideBound.newBuilder()
                                                .setParentBound(
                                                        SlideParentBound.newBuilder()
                                                                .setSnapTo(
                                                                        SlideParentSnapOption
                                                                                .forNumber(snapTo))
                                                                .build())
                                                .build()));
    }

    private static DpProp.@NonNull Builder dp(float value) {
        return DpProp.newBuilder().setValue(value);
    }

    private static DpProp.@NonNull Builder dynamicDp(DynamicFloat value, float valueForLayout) {
        return DpProp.newBuilder().setDynamicValue(value).setValueForLayout(valueForLayout);
    }

    private static DimensionProto.@NonNull SpProp sp(float value) {
        return DimensionProto.SpProp.newBuilder().setValue(value).build();
    }

    private static ContainerDimension.@NonNull Builder expand() {
        return ContainerDimension.newBuilder()
                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance());
    }

    private static StrokeCapProp.@NonNull Builder strokeCapButt() {
        return StrokeCapProp.newBuilder().setValue(LayoutElementProto.StrokeCap.STROKE_CAP_BUTT);
    }

    private static DegreesProp.@NonNull Builder degrees(int value) {
        return DegreesProp.newBuilder().setValue(value);
    }

    private static ColorStop.@NonNull Builder colorStop(int color, float offset) {
        return colorStop(color).setOffset(FloatProp.newBuilder().setValue(offset));
    }

    private static ColorStop.@NonNull Builder colorStop(int color) {
        return ColorStop.newBuilder().setColor(argb(color));
    }

    private static ColorProp.@NonNull Builder argb(int value) {
        return ColorProp.newBuilder().setArgb(value);
    }

    private static @NonNull ExpandedAngularDimensionProp expandAngular(float value) {
        return ExpandedAngularDimensionProp.newBuilder()
                .setLayoutWeight(FloatProp.newBuilder().setValue(value).build())
                .build();
    }

    private static StringProp.@NonNull Builder string(String value) {
        return StringProp.newBuilder().setValue(value);
    }

    private static StringProp.@NonNull Builder dynamicString(String value) {
        return StringProp.newBuilder()
                .setValue(value)
                .setDynamicValue(
                        DynamicString.newBuilder()
                                .setFixed(FixedString.newBuilder().setValue(value)));
    }

    private static ImageDimension.@NonNull Builder expandImage() {
        return ImageDimension.newBuilder()
                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance());
    }

    private static @NonNull List<DimensionProto.SpProp> buildSizesList(int[] presetSizes) {
        List<DimensionProto.SpProp> sizes = new ArrayList<>(3);
        for (int s : presetSizes) {
            sizes.add(sp(s));
        }
        return sizes;
    }

    private static void dispatchTouchEvent(View view, float x, float y) {
        long startTime = SystemClock.uptimeMillis();
        MotionEvent evt =
                MotionEvent.obtain(
                        /* downTime= */ startTime,
                        /* eventTime= */ startTime,
                        MotionEvent.ACTION_DOWN,
                        /* x= */ x,
                        /* y= */ y,
                        /* metaState= */ 0);
        view.dispatchTouchEvent(evt);
        evt.recycle();

        evt =
                MotionEvent.obtain(
                        /* downTime= */ startTime,
                        /* eventTime= */ startTime + 10,
                        MotionEvent.ACTION_UP,
                        /* x= */ x,
                        /* y= */ y,
                        /* metaState= */ 0);
        view.dispatchTouchEvent(evt);
        evt.recycle();

        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Spacer.Builder buildExpandedSpacer(int widthWeight, int heightWeight) {
        return Spacer.newBuilder()
                .setWidth(
                        SpacerDimension.newBuilder()
                                .setExpandedDimension(expandWithWeight(widthWeight)))
                .setHeight(
                        SpacerDimension.newBuilder()
                                .setExpandedDimension(expandWithWeight(heightWeight)));
    }

    private static ExpandedDimensionProp expandWithWeight(int weight) {
        return ExpandedDimensionProp.newBuilder()
                .setLayoutWeight(FloatProp.newBuilder().setValue(weight).build())
                .build();
    }

    /** Builds a wrapper Box that contains Spacer with the given parameters. */
    private static Layout layoutBoxWithSpacer(int width, int height) {
        return layoutBoxWithSpacer(width, height, /* modifiers= */ null);
    }

    /** Builds a wrapper Box that contains Spacer with the given parameters. */
    private static Layout layoutBoxWithSpacer(
            int width, int height, Modifiers.@Nullable Builder modifiers) {
        Spacer.Builder spacer =
                Spacer.newBuilder()
                        .setWidth(SpacerDimension.newBuilder().setLinearDimension(dp(width)))
                        .setHeight(SpacerDimension.newBuilder().setLinearDimension(dp(height)));
        if (modifiers != null) {
            spacer.setModifiers(modifiers);
        }
        return fingerprintedLayout(
                LayoutElement.newBuilder()
                        .setBox(
                                Box.newBuilder()
                                        .addContents(LayoutElement.newBuilder().setSpacer(spacer)))
                        .build());
    }

    private static Text createTextWithVisibility(
            String text, String id, Action action, boolean visibility) {
        return Text.newBuilder()
                .setText(string(text))
                .setModifiers(
                        Modifiers.newBuilder()
                                .setVisible(BoolProp.newBuilder().setValue(visibility))
                                .setClickable(Clickable.newBuilder().setId(id).setOnClick(action)))
                .build();
    }
}
