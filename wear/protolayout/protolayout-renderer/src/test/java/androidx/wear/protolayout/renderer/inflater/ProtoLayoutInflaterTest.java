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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
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
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.getFrameLayoutGravity;
import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.getRenderedMetadata;
import static androidx.wear.protolayout.renderer.test.R.drawable.android_animated_24dp;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils.TruncateAt;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32ToFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateColorSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateStringSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;
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
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignmentProp;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignmentProp;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.DimensionProto.ArcLineLength;
import androidx.wear.protolayout.proto.DimensionProto.ArcSpacerLength;
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedAngularDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ImageDimension;
import androidx.wear.protolayout.proto.DimensionProto.ProportionalDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.SpacerDimension;
import androidx.wear.protolayout.proto.DimensionProto.WrappedDimensionProp;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.LayoutElementProto.Arc;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLine;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcSpacer;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcText;
import androidx.wear.protolayout.proto.LayoutElementProto.Box;
import androidx.wear.protolayout.proto.LayoutElementProto.ColorFilter;
import androidx.wear.protolayout.proto.LayoutElementProto.Column;
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
import androidx.wear.protolayout.proto.ModifiersProto.AnimatedVisibility;
import androidx.wear.protolayout.proto.ModifiersProto.Border;
import androidx.wear.protolayout.proto.ModifiersProto.Clickable;
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
import androidx.wear.protolayout.proto.TypesProto.FloatProp;
import androidx.wear.protolayout.proto.TypesProto.Int32Prop;
import androidx.wear.protolayout.proto.TypesProto.StringProp;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline;
import androidx.wear.protolayout.renderer.helper.TestFingerprinter;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.InflateResult;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewGroupMutation;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewMutationException;
import androidx.wear.protolayout.renderer.test.R;
import androidx.wear.widget.ArcLayout;
import androidx.wear.widget.CurvedTextView;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class ProtoLayoutInflaterTest {
    private static final String TEST_CLICKABLE_CLASS_NAME = "Hello";
    private static final String TEST_CLICKABLE_PACKAGE_NAME = "World";
    private static final String EXTRA_CLICKABLE_ID = "extra.clickable.id";

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 400;

    @Rule public final Expect expect = Expect.create();

    private final StateStore mStateStore = new StateStore(ImmutableMap.of());
    private ProtoLayoutDynamicDataPipeline mDataPipeline;

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

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "content",
                        StateEntryValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(initialDynamicContentDescription))
                                .build(),
                        "state",
                        StateEntryValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(initialDynamicStateDescription))
                                .build()));

        info = AccessibilityNodeInfoCompat.wrap(tv.createAccessibilityNodeInfo());
        assertThat(mStateStore.getStateEntryValuesProto("content").getStringVal().getValue())
                .isEqualTo(initialDynamicContentDescription);
        assertThat(info.getContentDescription().toString())
                .isEqualTo(initialDynamicContentDescription);
        assertThat(info.getStateDescription().toString()).isEqualTo(initialDynamicStateDescription);

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "content",
                        StateEntryValue.newBuilder()
                                .setStringVal(
                                        FixedString.newBuilder()
                                                .setValue(targetDynamicContentDescription))
                                .build(),
                        "state",
                        StateEntryValue.newBuilder()
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
    public void inflate_arc_withSpacer() {
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
    public void inflate_arc_withMaxAngleAndWeights() {
        ArcSpacerLength spacerLength =
                ArcSpacerLength.newBuilder()
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

    @NonNull
    private static ArcLayoutElement.Builder arcLayoutElement(ArcSpacer.Builder setAngularLength) {
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

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "anim_val",
                        StateEntryValue.newBuilder()
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
        long startTime = SystemClock.uptimeMillis();
        MotionEvent evt =
                MotionEvent.obtain(
                        /* downTime= */ startTime,
                        /* eventTime= */ startTime,
                        MotionEvent.ACTION_DOWN,
                        /* x= */ 5f,
                        /* y= */ 5f,
                        /* metaState= */ 0);
        tv.dispatchTouchEvent(evt);
        evt.recycle();

        evt =
                MotionEvent.obtain(
                        /* downTime= */ startTime,
                        /* eventTime= */ startTime + 100,
                        MotionEvent.ACTION_UP,
                        /* x= */ 5f,
                        /* y= */ 5f,
                        /* metaState= */ 0);
        tv.dispatchTouchEvent(evt);
        evt.recycle();

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(hasFiredList).hasSize(1);
    }

    @NonNull
    private static SpanModifiers.Builder spanClickMod() {
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

    @NonNull
    private static ImageDimension.Builder linImageDim(DpProp.Builder builderForValue) {
        return ImageDimension.newBuilder().setLinearDimension(builderForValue);
    }

    @NonNull
    private static ContainerDimension.Builder wrap() {
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
                                                                                        arcLength,
                                                                                        180f))
                                                                        .setThickness(dp(12)))))
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
    public void inflate_arcLine_dynamicData_updatesArcLength() {
        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
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
                                                                                        arcLength,
                                                                                        180f))
                                                                        .setThickness(dp(12)))))
                        .build();

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        SizedArcContainer sizedContainer = (SizedArcContainer) arcLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) sizedContainer.getChildAt(0);
        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(10);

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(20))
                                .build()));

        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(20);
    }

    @Test
    public void inflate_arcLine_withoutValueForLayout_noLegacyMode_usesZero() {
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

        FrameLayout rootLayout = renderer(fingerprintedLayout(root)).inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        SizedArcContainer sizedContainer = (SizedArcContainer) arcLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) sizedContainer.getChildAt(0);
        assertThat(sizedContainer.getSweepAngleDegrees()).isEqualTo(0f);
        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(45f);
        assertThat(line.getMaxSweepAngleDegrees()).isEqualTo(0);
    }

    @NonNull
    private static DegreesProp.Builder degreesDynamic(DynamicFloat arcLength) {
        return DegreesProp.newBuilder().setDynamicValue(arcLength);
    }

    @NonNull
    private static DegreesProp.Builder degreesDynamic(
            DynamicFloat arcLength, float valueForLayout) {
        return DegreesProp.newBuilder()
                .setValueForLayout(valueForLayout)
                .setDynamicValue(arcLength);
    }

    @Test
    public void inflate_arcLine_withoutValueForLayout_noLegacyMode_usesArcLength() {
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
                renderer(
                                newRendererConfigBuilder(fingerprintedLayout(root))
                                        .setAllowLayoutChangingBindsWithoutDefault(true))
                        .inflate();

        shadowOf(Looper.getMainLooper()).idle();

        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        assertThat(line.getSweepAngleDegrees()).isEqualTo(45f);
        assertThat(line.getLineSweepAngleDegrees()).isEqualTo(45f);
    }

    @Test
    public void inflate_text_dynamicColor_updatesColor() {
        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
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

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(0x11111111))
                                .build()));

        assertThat(tv.getCurrentTextColor()).isEqualTo(0x11111111);
    }

    @Test
    public void inflate_image_dynamicTint_changesTintColor() {
        // Must match a resource ID in buildResources
        String protoResId = "android";

        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        "tint",
                        StateEntryValue.newBuilder()
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

        FrameLayout tv1Wrapper = (FrameLayout) column.getChildAt(0);
        FrameLayout tv2Wrapper = (FrameLayout) column.getChildAt(1);
        TextView tv1 = (TextView) tv1Wrapper.getChildAt(0);
        TextView tv2 = (TextView) tv2Wrapper.getChildAt(0);
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
        FrameLayout tv1WrapperAfterMutation = (FrameLayout) columnAfterMutation.getChildAt(0);
        FrameLayout tv2WrapperAfterMutation = (FrameLayout) columnAfterMutation.getChildAt(1);
        TextView tv1AfterMutation = (TextView) tv1WrapperAfterMutation.getChildAt(0);
        TextView tv2AfterMutation = (TextView) tv2WrapperAfterMutation.getChildAt(0);

        // Unchanged views should be left exactly the same:
        assertThat(columnAfterMutation).isSameInstanceAs(column);
        assertThat(tv1WrapperAfterMutation).isSameInstanceAs(tv1Wrapper);
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

        FrameLayout tv1Wrapper = (FrameLayout) column.getChildAt(0);
        TextView tv1 = (TextView) tv1Wrapper.getChildAt(0);
        FrameLayout tv2Wrapper = (FrameLayout) column.getChildAt(1);
        TextView tv2 = (TextView) tv2Wrapper.getChildAt(0);
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
        FrameLayout tv1WrapperAfterMutation = (FrameLayout) columnAfterMutation.getChildAt(0);
        TextView tv1AfterMutation = (TextView) tv1WrapperAfterMutation.getChildAt(0);
        FrameLayout tv2WrapperAfterMutation = (FrameLayout) columnAfterMutation.getChildAt(1);
        TextView tv2AfterMutation = (TextView) tv2WrapperAfterMutation.getChildAt(0);

        // Unchanged views should be left exactly the same:
        expect.that(tv1AfterMutation).isSameInstanceAs(tv1);
        expect.that(tv2AfterMutation).isSameInstanceAs(tv2);
        expect.that(tv1WrapperAfterMutation).isSameInstanceAs(tv1Wrapper);
        expect.that(tv2WrapperAfterMutation).isSameInstanceAs(tv2Wrapper);
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

        FrameLayout tv1Wrapper = (FrameLayout) column.getChildAt(0);
        FrameLayout tv2Wrapper = (FrameLayout) column.getChildAt(1);
        TextView tv1 = (TextView) tv1Wrapper.getChildAt(0);
        TextView tv2 = (TextView) tv2Wrapper.getChildAt(0);
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
                        getRenderedMetadata(inflatedViewParent), layout2);
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
                        getRenderedMetadata(inflatedViewParent1), layout2);
        ViewGroupMutation mutation3 =
                renderer.mRenderer.computeMutation(
                        getRenderedMetadata(inflatedViewParent1), layout3);

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
                        getRenderedMetadata(inflatedViewParent), layout2);
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
                        getRenderedMetadata(inflatedViewParent), layout3);
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
                        getRenderedMetadata(inflatedViewParent), layout2);
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
                        getRenderedMetadata(inflatedViewParent), layout2);
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
                        getRenderedMetadata(inflatedViewParent), layout2);
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
                .setLoadActionExecutor(ContextCompat.getMainExecutor(getApplicationContext()));
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
                        /* canUpdateGateways= */ true, null, mStateStore, quotaManager);
        rendererConfigBuilder.setDynamicDataPipeline(mDataPipeline);
        return new Renderer(rendererConfigBuilder.build(), mDataPipeline);
    }

    @SuppressWarnings("RestrictTo")
    private static final class Renderer {
        final ProtoLayoutInflater mRenderer;
        final ProtoLayoutDynamicDataPipeline mDataPipeline;

        Renderer(
                ProtoLayoutInflater.Config rendererConfig,
                ProtoLayoutDynamicDataPipeline dataPipeline) {
            this.mRenderer = new ProtoLayoutInflater(rendererConfig);
            this.mDataPipeline = dataPipeline;
        }

        FrameLayout inflate() {
            FrameLayout rootLayout = new FrameLayout(getApplicationContext());
            // This needs to be an attached view to test animations in data pipeline.
            Robolectric.buildActivity(Activity.class).setup().get().setContentView(rootLayout);
            InflateResult inflateResult = mRenderer.inflate(rootLayout);
            if (inflateResult != null) {
                inflateResult.updateDynamicDataPipeline(/* isReattaching= */ false);
            }
            shadowOf(Looper.getMainLooper()).idle();
            doLayout(rootLayout);

            return rootLayout;
        }

        ViewGroupMutation computeMutation(RenderedMetadata renderedMetadata, Layout targetLayout) {
            return mRenderer.computeMutation(renderedMetadata, targetLayout);
        }

        boolean applyMutation(ViewGroup parent, ViewGroupMutation mutation) {
            try {
                ListenableFuture<Void> applyMutationFuture =
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

    @NonNull
    private static Trigger onVisibleTrigger() {
        return Trigger.newBuilder()
                .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance())
                .build();
    }

    @NonNull
    private static AnimatableDynamicFloat.Builder stateDynamicFloat() {
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

    @NonNull
    private static ContainerDimension expandWeight() {
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
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
        ListenableFuture<Void> applyMutationFuture =
                renderer.mRenderer.applyMutation(inflatedViewParent, mutation);

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(mDataPipeline.getRunningAnimationsCount()).isEqualTo(2);

        ShadowChoreographer.setPaused(false);
        shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));
        applyMutationFuture.get();
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

    @NonNull
    private static EnterTransition.Builder enterFadeIn(int delay) {
        return EnterTransition.newBuilder().setFadeIn(fadeIn(delay));
    }

    @NonNull
    private static FadeInTransition.Builder fadeIn(int delay) {
        return FadeInTransition.newBuilder()
                .setAnimationSpec(
                        AnimationSpec.newBuilder().setAnimationParameters(
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

    @NonNull
    private Text.Builder textAnimVisibility(AnimatedVisibility.Builder snapTo, String text) {
        return Text.newBuilder()
                .setModifiers(Modifiers.newBuilder().setContentUpdateAnimation(snapTo.build()))
                .setText(string(text).build());
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

    @NonNull
    private static DpProp.Builder dp(float value) {
        return DpProp.newBuilder().setValue(value);
    }

    @NonNull
    private static ContainerDimension.Builder expand() {
        return ContainerDimension.newBuilder()
                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance());
    }

    @NonNull
    private static StrokeCapProp.Builder strokeCapButt() {
        return StrokeCapProp.newBuilder().setValue(LayoutElementProto.StrokeCap.STROKE_CAP_BUTT);
    }

    @NonNull
    private static DegreesProp.Builder degrees(int value) {
        return DegreesProp.newBuilder().setValue(value);
    }

    @NonNull
    private static ExpandedAngularDimensionProp expandAngular(float value) {
        return ExpandedAngularDimensionProp.newBuilder()
                .setLayoutWeight(FloatProp.newBuilder().setValue(value).build())
                .build();
    }

    @NonNull
    private static StringProp.Builder string(String value) {
        return StringProp.newBuilder().setValue(value);
    }

    @NonNull
    private static ImageDimension.Builder expandImage() {
        return ImageDimension.newBuilder()
                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance());
    }
}
