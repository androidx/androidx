/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.wear.tiles.TileProviderService;
import androidx.wear.tiles.TilesTestRunner;
import androidx.wear.tiles.proto.ActionProto.Action;
import androidx.wear.tiles.proto.ActionProto.AndroidActivity;
import androidx.wear.tiles.proto.ActionProto.AndroidBooleanExtra;
import androidx.wear.tiles.proto.ActionProto.AndroidDoubleExtra;
import androidx.wear.tiles.proto.ActionProto.AndroidExtra;
import androidx.wear.tiles.proto.ActionProto.AndroidIntExtra;
import androidx.wear.tiles.proto.ActionProto.AndroidLongExtra;
import androidx.wear.tiles.proto.ActionProto.AndroidStringExtra;
import androidx.wear.tiles.proto.ActionProto.LaunchAction;
import androidx.wear.tiles.proto.ActionProto.LoadAction;
import androidx.wear.tiles.proto.ColorProto.ColorProp;
import androidx.wear.tiles.proto.DimensionProto.ContainerDimension;
import androidx.wear.tiles.proto.DimensionProto.DegreesProp;
import androidx.wear.tiles.proto.DimensionProto.DpProp;
import androidx.wear.tiles.proto.DimensionProto.ExpandedDimensionProp;
import androidx.wear.tiles.proto.DimensionProto.ImageDimension;
import androidx.wear.tiles.proto.DimensionProto.ProportionalDimensionProp;
import androidx.wear.tiles.proto.DimensionProto.SpacerDimension;
import androidx.wear.tiles.proto.DimensionProto.WrappedDimensionProp;
import androidx.wear.tiles.proto.LayoutElementProto.Arc;
import androidx.wear.tiles.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto.ArcLine;
import androidx.wear.tiles.proto.LayoutElementProto.ArcSpacer;
import androidx.wear.tiles.proto.LayoutElementProto.ArcText;
import androidx.wear.tiles.proto.LayoutElementProto.Box;
import androidx.wear.tiles.proto.LayoutElementProto.Column;
import androidx.wear.tiles.proto.LayoutElementProto.FontStyle;
import androidx.wear.tiles.proto.LayoutElementProto.HorizontalAlignment;
import androidx.wear.tiles.proto.LayoutElementProto.HorizontalAlignmentProp;
import androidx.wear.tiles.proto.LayoutElementProto.Image;
import androidx.wear.tiles.proto.LayoutElementProto.Layout;
import androidx.wear.tiles.proto.LayoutElementProto.LayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto.Row;
import androidx.wear.tiles.proto.LayoutElementProto.Spacer;
import androidx.wear.tiles.proto.LayoutElementProto.Span;
import androidx.wear.tiles.proto.LayoutElementProto.SpanImage;
import androidx.wear.tiles.proto.LayoutElementProto.SpanText;
import androidx.wear.tiles.proto.LayoutElementProto.Spannable;
import androidx.wear.tiles.proto.LayoutElementProto.Text;
import androidx.wear.tiles.proto.LayoutElementProto.VerticalAlignment;
import androidx.wear.tiles.proto.LayoutElementProto.VerticalAlignmentProp;
import androidx.wear.tiles.proto.ModifiersProto.Border;
import androidx.wear.tiles.proto.ModifiersProto.Clickable;
import androidx.wear.tiles.proto.ModifiersProto.Modifiers;
import androidx.wear.tiles.proto.ModifiersProto.Padding;
import androidx.wear.tiles.proto.ModifiersProto.Semantics;
import androidx.wear.tiles.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.tiles.proto.ResourceProto.ImageResource;
import androidx.wear.tiles.proto.ResourceProto.Resources;
import androidx.wear.tiles.proto.StateProto.State;
import androidx.wear.tiles.proto.TypesProto.StringProp;
import androidx.wear.tiles.renderer.test.R;
import androidx.wear.widget.ArcLayout;
import androidx.wear.widget.CurvedTextView;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.IOException;
import java.util.Map;

@RunWith(TilesTestRunner.class)
@DoNotInstrument
public class TileRendererInternalTest {
    private static final String TEST_CLICKABLE_CLASS_NAME = "Hello";
    private static final String TEST_CLICKABLE_PACKAGE_NAME = "World";

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 400;

    @Rule public final Expect expect = Expect.create();

    @Test
    public void inflate_textView() {
        String textContents = "Hello World";
        LayoutElement root = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

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
        LayoutElement root = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setFontStyle(FontStyle.newBuilder()
                                .setColor(ColorProp.newBuilder().setArgb(color))))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getTextColors().getDefaultColor()).isEqualTo(color);
    }

    @Test
    public void inflate_textView_withoutText() {
        LayoutElement root = LayoutElement.newBuilder().setText(Text.newBuilder()).build();

        FrameLayout rootLayout = inflateProto(root);

        TextView tv = (TextView) rootLayout.getChildAt(0);
        expect.that(tv.getText().toString()).isEmpty();
    }

    @Test
    public void inflate_textView_withSemanticsModifier() {
        String textContents = "Hello World";
        LayoutElement root = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setModifiers(Modifiers.newBuilder()
                                .setSemantics(Semantics.newBuilder()
                                        .setContentDescription("Hello World Text Element"))))
                        .build();

        FrameLayout rootLayout = inflateProto(root);

        // Check that there's a text element in the layout...
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // Check the text contents.
        expect.that(tv.getText().toString()).isEqualTo(textContents);

        // Check the accessibility label.
        expect.that(tv.getContentDescription().toString()).isEqualTo("Hello World Text Element");
        expect.that(tv.getImportantForAccessibility())
                .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        expect.that(tv.isFocusable()).isTrue();
    }

    @Test
    public void inflate_box_withIllegalSize() {
        // Inner box's width set to "expand". Having a single "expand" element in a "wrap" element
        // is an undefined state, so the outer box should not be displayed.
        Box innerBox = Box.newBuilder()
                .setWidth(ContainerDimension.newBuilder()
                        .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance()))
                .addContents(LayoutElement.newBuilder()
                        .setText(Text.newBuilder()
                                .setText(StringProp.newBuilder().setValue("foo"))))
                .build();

        LayoutElement root = LayoutElement.newBuilder()
                .setBox(Box.newBuilder()
                        // Outer box's width and height left at default value of "wrap"
                        .addContents(LayoutElement.newBuilder().setBox(innerBox)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        // Check that the outer box is not displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_spacer() {
        int width = 10;
        int height = 20;
        LayoutElement root = LayoutElement.newBuilder()
                .setSpacer(Spacer.newBuilder()
                        .setHeight(SpacerDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(height)))
                        .setWidth(SpacerDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(width))))
                .build();

        FrameLayout rootLayout = inflateProto(root);

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
        LayoutElement root = LayoutElement.newBuilder()
                .setSpacer(Spacer.newBuilder()
                        .setModifiers(Modifiers.newBuilder()
                                .setBorder(Border.newBuilder()
                                        .setWidth(DpProp.newBuilder().setValue(2))))
                        .setHeight(SpacerDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(height)))
                        .setWidth(SpacerDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(width))))
                .build();

        FrameLayout rootLayout = inflateProto(root);

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

        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(protoResId)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        // An image without dimensions will not be displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_image_withDimensions() {
        // Must match a resource ID in buildResources
        String protoResId = "android";

        LayoutElement root = buildImage(protoResId, 30, 20);

        FrameLayout rootLayout = inflateProto(root);

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(30);
        expect.that(iv.getMeasuredHeight()).isEqualTo(20);
    }

    @Test
    public void inflate_image_withInvalidRatio() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setHeight(ImageDimension.newBuilder()
                                .setProportionalDimension(ProportionalDimensionProp
                                        .getDefaultInstance()))
                        .setWidth(ImageDimension.newBuilder()
                                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance())))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        // An image with invalid ratio will not be displayed.
        assertThat(rootLayout.getChildCount()).isEqualTo(0);
    }

    @Test
    public void inflate_image_byName() {
        // Must match a resource ID in buildResources
        String protoResId = "android_image_by_name";

        LayoutElement root = buildImage(protoResId, 30, 20);

        FrameLayout rootLayout = inflateProto(root);
        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(30);
        expect.that(iv.getMeasuredHeight()).isEqualTo(20);
    }

    @Test
    public void inflate_clickableModifier_withLaunchAction() throws IOException {
        final String packageName = "androidx.wear.tiles.test";
        final String className = "androidx.wear.tiles.test.TestActivity";
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

        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setPackageName(packageName)
                        .setClassName(className)
                        .putKeyToExtra(
                                "stringValue",
                                AndroidExtra.newBuilder()
                                        .setStringVal(AndroidStringExtra.newBuilder()
                                                .setValue(stringVal))
                                        .build())
                        .putKeyToExtra(
                                "int32Value",
                                AndroidExtra.newBuilder()
                                        .setIntVal(AndroidIntExtra.newBuilder()
                                                .setValue(int32Val))
                                        .build())
                        .putKeyToExtra(
                                "int64Value",
                                AndroidExtra.newBuilder()
                                        .setLongVal(AndroidLongExtra.newBuilder()
                                                .setValue(int64Val))
                                        .build())
                        .putKeyToExtra(
                                "doubleValue",
                                AndroidExtra.newBuilder()
                                        .setDoubleVal(AndroidDoubleExtra.newBuilder()
                                                .setValue(doubleVal))
                                        .build())
                        .putKeyToExtra(
                                "boolValue",
                                AndroidExtra.newBuilder()
                                        .setBooleanVal(AndroidBooleanExtra.newBuilder()
                                                .setValue(true))
                                        .build()))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement textElement = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setModifiers(Modifiers.newBuilder()
                                .setClickable(Clickable.newBuilder()
                                        .setId("foo")
                                        .setOnClick(action))))
                .build();

        FrameLayout rootLayout = inflateProto(textElement);

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // The clickable view must have the same tag as the corresponding prototile clickable.
        expect.that(tv.getTag()).isEqualTo("foo");

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
        final String packageName = "androidx.wear.tiles.test";
        final String className = "androidx.wear.tiles.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);

        // Activity is not exported. Renderer shouldn't even try and call it.
        ai.exported = false;

        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setPackageName(packageName)
                        .setClassName(className))
                .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement root = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setModifiers(Modifiers.newBuilder()
                                .setClickable(Clickable.newBuilder()
                                        .setId("foo")
                                        .setOnClick(action))))
                .build();

        FrameLayout rootLayout = inflateProto(root);

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
        final String packageName = "androidx.wear.tiles.test";
        final String className = "androidx.wear.tiles.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);

        // Activity has a permission associated with it; shouldn't be called.
        ai.exported = true;
        ai.permission = "android.MY_PERMISSION";

        LaunchAction launchAction =
                LaunchAction.newBuilder()
                        .setAndroidActivity(AndroidActivity.newBuilder()
                                .setPackageName(packageName).setClassName(className))
                        .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement root =
                LayoutElement.newBuilder()
                        .setText(
                                Text.newBuilder()
                                        .setText(StringProp.newBuilder().setValue(textContents))
                                        .setModifiers(
                                                Modifiers.newBuilder()
                                                        .setClickable(Clickable.newBuilder()
                                                                .setId("foo").setOnClick(action))))
                        .build();

        FrameLayout rootLayout = inflateProto(root);

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        shadowOf((Application) getApplicationContext()).clearNextStartedActivities();

        // Try and fire the intent.
        tv.performClick();

        expect.that(
                shadowOf((Application) getApplicationContext()).getNextStartedActivity()).isNull();
    }

    @Test
    public void inflate_clickableModifier_withLoadAction() {
        final String textContents = "I am a clickable";

        Action action = Action.newBuilder().setLoadAction(LoadAction.getDefaultInstance()).build();

        LayoutElement root = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setModifiers(Modifiers.newBuilder()
                                .setClickable(Clickable.newBuilder()
                                        .setId("foo")
                                        .setOnClick(action))))
                .build();

        State.Builder receivedState = State.newBuilder();
        FrameLayout rootLayout =
                inflateProto(root, 0, resourceResolvers(), receivedState::mergeFrom);

        // Should be just a text view as the root.
        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        assertThat(rootLayout.getChildAt(0)).isInstanceOf(TextView.class);

        TextView tv = (TextView) rootLayout.getChildAt(0);

        // The clickable view must have the same tag as the corresponding prototile clickable.
        expect.that(tv.getTag()).isEqualTo("foo");

        // Ensure that the text still went through properly.
        expect.that(tv.getText().toString()).isEqualTo(textContents);

        // Try and fire the intent.
        tv.performClick();
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(receivedState.getLastClickableId()).isEqualTo("foo");
    }

    @Test
    public void inflate_clickableModifier_withAndroidActivity_hasSourceBounds() {
        final String packageName = "androidx.wear.tiles.test";
        final String className = "androidx.wear.tiles.test.TestActivity";
        final String textContents = "I am a clickable";

        // Register the activity so the intent can be resolved.
        ComponentName cn = new ComponentName(packageName, className);
        ShadowPackageManager pkgManager = shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo ai = pkgManager.addActivityIfNotPresent(cn);
        ai.exported = true;

        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setPackageName(packageName)
                        .setClassName(className))
                .build();

        Action action = Action.newBuilder().setLaunchAction(launchAction).build();

        LayoutElement textElement = LayoutElement.newBuilder()
                .setText(Text.newBuilder()
                        .setText(StringProp.newBuilder().setValue(textContents))
                        .setModifiers(Modifiers.newBuilder()
                                .setClickable(Clickable.newBuilder()
                                        .setId("foo")
                                        .setOnClick(action))))
                .build();

        FrameLayout rootLayout = inflateProto(textElement);

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
        // Shorter than 360 degrees, so should be drawn as an arc:
        ArcLine innerArcLine = ArcLine.newBuilder()
                .setLength(DegreesProp.newBuilder().setValue(30))
                .setThickness(DpProp.newBuilder().setValue(12))
                .build();

        LayoutElement root = LayoutElement.newBuilder()
                .setArc(Arc.newBuilder()
                        .setAnchorAngle(DegreesProp.newBuilder().setValue(0).build())
                        .addContents(ArcLayoutElement.newBuilder().setLine(innerArcLine)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedLineView line = (WearCurvedLineView) arcLayout.getChildAt(0);
        assertThat(line.getSweepAngleDegrees()).isEqualTo(30);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(line.getThickness()).isEqualTo(12);
    }

    @Test
    public void inflate_arc_withLineDrawnWithAddOval() {
        // Longer than 360 degrees, so should be drawn as an oval:
        ArcLine arcLine = ArcLine.newBuilder()
                .setLength(DegreesProp.newBuilder().setValue(500))
                .setThickness(DpProp.newBuilder().setValue(12))
                .build();

        LayoutElement root = LayoutElement.newBuilder()
                .setArc(Arc.newBuilder()
                        .setAnchorAngle(DegreesProp.newBuilder().setValue(0).build())
                        .addContents(ArcLayoutElement.newBuilder().setLine(arcLine)))

                .build();

        FrameLayout rootLayout = inflateProto(root);

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
        ArcText text1 = ArcText.newBuilder()
                .setText(StringProp.newBuilder().setValue("text1"))
                .build();
        ArcText text2 = ArcText.newBuilder()
                .setText(StringProp.newBuilder().setValue("text2"))
                .build();


        LayoutElement root = LayoutElement.newBuilder()
                .setArc(Arc.newBuilder()
                        .setAnchorAngle(DegreesProp.newBuilder().setValue(0).build())
                        .addContents(ArcLayoutElement.newBuilder().setText(text1))
                        .addContents(ArcLayoutElement.newBuilder().setText(text2)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

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
        ArcSpacer arcSpacer = ArcSpacer.newBuilder()
                .setLength(DegreesProp.newBuilder().setValue(90))
                .setThickness(DpProp.newBuilder().setValue(20))
                .build();

        LayoutElement root = LayoutElement.newBuilder()
                .setArc(Arc.newBuilder()
                        .setAnchorAngle(DegreesProp.newBuilder().setValue(0).build())
                        .addContents(ArcLayoutElement.newBuilder().setSpacer(arcSpacer)))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        assertThat(rootLayout.getChildCount()).isEqualTo(1);
        ArcLayout arcLayout = (ArcLayout) rootLayout.getChildAt(0);
        assertThat(arcLayout.getChildCount()).isEqualTo(1);
        WearCurvedSpacer spacer = (WearCurvedSpacer) arcLayout.getChildAt(0);
        assertThat(spacer.getSweepAngleDegrees()).isEqualTo(90);
        // Dimensions are in DP, but the density is currently 1 in the tests, so this is fine:
        assertThat(spacer.getThickness()).isEqualTo(20);
    }

    @Test
    public void inflate_row() {
        final String protoResId = "android";

        LayoutElement image = buildImage(protoResId, 30, 20);

        LayoutElement root = LayoutElement.newBuilder()
                .setRow(Row.newBuilder().addContents(image).addContents(image))
                .build();

        FrameLayout layout = inflateProto(root);

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

        LayoutElement root = LayoutElement.newBuilder()
                .setColumn(Column.newBuilder().addContents(image).addContents(image))
                .build();

        FrameLayout layout = inflateProto(root);

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
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(protoResId))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(widthDp)))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(heightDp))))
                .build();
    }

    private static LayoutElement buildExampleRowLayoutWithAlignment(VerticalAlignment alignment) {
        final String protoResId = "android";

        LayoutElement image1 = buildImage(protoResId, 30, 30);
        LayoutElement image2 = buildImage(protoResId, 30, 50);

        Row row = Row.newBuilder()
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
            FrameLayout topFrameLayout = inflateProto(root);
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

        LayoutElement image1 = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(resName))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(30)))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(30))))
                .build();

        LayoutElement image2 = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(resName))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(50)))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(30))))
                .build();

        Column column = Column.newBuilder()
                .addContents(image1)
                .addContents(image2)
                .setHorizontalAlignment(HorizontalAlignmentProp.newBuilder()
                        .setValue(alignment))
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
            FrameLayout topFrameLayout = inflateProto(root);
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

        LayoutElement image1 = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(resName))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(30)))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(30))))
                .build();

        LayoutElement image2 = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue(resName))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(50)))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(50))))
                .build();

        Box box = Box.newBuilder()
                .addContents(image1)
                .addContents(image2)
                .setVerticalAlignment(VerticalAlignmentProp.newBuilder().setValue(vAlign))
                .setHorizontalAlignment(HorizontalAlignmentProp.newBuilder()
                        .setValue(hAlign))
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
                FrameLayout topFrameLayout = inflateProto(root);

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
        LayoutElement root = LayoutElement.newBuilder().build();

        inflateProto(root);
    }

    @Test
    public void buildClickableIntent_setsPackageName() {
        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                .build();

        Intent i = TileRendererInternal.buildLaunchActionIntent(launchAction, "");

        expect.that(i.getComponent().getClassName()).isEqualTo(TEST_CLICKABLE_CLASS_NAME);
        expect.that(i.getComponent().getPackageName()).isEqualTo(TEST_CLICKABLE_PACKAGE_NAME);
    }

    @Test
    public void buildClickableIntent_launchAction_containsClickableId() {
        String testId = "HELLOWORLD";

        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                .build();

        Intent i = TileRendererInternal.buildLaunchActionIntent(launchAction, testId);

        expect.that(i.getStringExtra(TileProviderService.EXTRA_CLICKABLE_ID)).isEqualTo(testId);
    }

    @Test
    public void buildClickableIntent_noClickableExtraIfNotSet() {
        LaunchAction launchAction = LaunchAction.newBuilder()
                .setAndroidActivity(AndroidActivity.newBuilder()
                        .setClassName(TEST_CLICKABLE_CLASS_NAME)
                        .setPackageName(TEST_CLICKABLE_PACKAGE_NAME))
                .build();

        Intent i = TileRendererInternal.buildLaunchActionIntent(launchAction, "");

        expect.that(i.hasExtra(TileProviderService.EXTRA_CLICKABLE_ID)).isFalse();
    }

    @Test
    public void inflate_imageView_noResourceId() {
        LayoutElement root = LayoutElement.newBuilder().setImage(Image.newBuilder()).build();

        inflateProto(root);
    }

    @Test
    public void inflate_imageView_resourceHasNoAndroidResource() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue("no_android_resource_set")))
                .build();

        inflateProto(root);
    }

    @Test
    public void inflate_imageView_androidResourceDoesNotExist() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue("does_not_exist")))
                .build();

        inflateProto(root);
    }

    @Test
    public void inflate_imageView_resourceReferenceDoesNotExist() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue("aaaaaaaaaaaaaa")))
                .build();

        inflateProto(root);
    }

    @Test
    public void inflate_imageView_expandsToParentEvenWhenImageBitmapIsNotSet() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue("invalid"))
                        .setHeight(ImageDimension.newBuilder()
                                .setExpandedDimension(ExpandedDimensionProp
                                        .getDefaultInstance()))
                        .setWidth(ImageDimension.newBuilder()
                                .setExpandedDimension(ExpandedDimensionProp
                                        .getDefaultInstance())))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(iv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT);
    }

    @Test
    public void inflate_imageView_expandsToParentContainerEvenWhenImageBitmapIsNotSet() {
        Image invalidImage = Image.newBuilder()
                .setResourceId(StringProp.newBuilder().setValue("invalid"))
                .setHeight(ImageDimension.newBuilder()
                        .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance()))
                .setWidth(ImageDimension.newBuilder()
                        .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance()))
                .build();

        LayoutElement root = LayoutElement.newBuilder()
                .setBox(Box.newBuilder()
                        .setHeight(ContainerDimension.newBuilder()
                                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance()))
                        .setWidth(ContainerDimension.newBuilder()
                                .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance()))
                                .setModifiers(Modifiers.newBuilder()
                                        .setPadding(Padding.newBuilder()
                                                .setTop(DpProp.newBuilder().setValue(50))))
                                .addContents(LayoutElement.newBuilder().setImage(invalidImage)))
                .build();

        FrameLayout rootLayout = inflateProto(root);
        FrameLayout boxLayout = (FrameLayout) rootLayout.getChildAt(0);
        RatioViewWrapper iv = (RatioViewWrapper) boxLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(SCREEN_WIDTH);
        expect.that(iv.getMeasuredHeight()).isEqualTo(SCREEN_HEIGHT - 50);
    }

    @Test
    public void inflate_imageView_usesDimensionsEvenWhenImageBitmapIsNotSet() {
        LayoutElement root = LayoutElement.newBuilder()
                .setImage(Image.newBuilder()
                        .setResourceId(StringProp.newBuilder().setValue("invalid"))
                        .setHeight(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(100)))
                        .setWidth(ImageDimension.newBuilder()
                                .setLinearDimension(DpProp.newBuilder().setValue(100))))
                .build();

        FrameLayout rootLayout = inflateProto(root);

        RatioViewWrapper iv = (RatioViewWrapper) rootLayout.getChildAt(0);

        expect.that(iv.getMeasuredWidth()).isEqualTo(100);
        expect.that(iv.getMeasuredHeight()).isEqualTo(100);
    }

    @Test
    public void inflate_spannable_imageOccupiesSpace() {
        LayoutElement rootWithoutImage = LayoutElement.newBuilder()
                .setSpannable(Spannable.newBuilder()
                        .addSpans(textSpan("Foo"))
                        .addSpans(textSpan("Bar")))
                .build();

        LayoutElement rootWithImage = LayoutElement.newBuilder()
                .setSpannable(Spannable.newBuilder()
                        .addSpans(textSpan("Foo"))
                        .addSpans(Span.newBuilder()
                                .setImage(SpanImage.newBuilder()
                                        .setResourceId(StringProp.newBuilder()
                                                .setValue("android"))
                                        .setHeight(DpProp.newBuilder().setValue(50))
                                        .setWidth(DpProp.newBuilder().setValue(50))))
                                .addSpans(textSpan("Bar")))
                .build();

        FrameLayout rootLayoutWithoutImage = inflateProto(rootWithoutImage);
        TextView tvInRootLayoutWithoutImage = (TextView) rootLayoutWithoutImage.getChildAt(0);
        FrameLayout rootLayoutWithImage = inflateProto(rootWithImage);
        TextView tvInRootLayoutWithImage = (TextView) rootLayoutWithImage.getChildAt(0);

        int widthDiff =
                tvInRootLayoutWithImage.getMeasuredWidth()
                        - tvInRootLayoutWithoutImage.getMeasuredWidth();

        // Check that the layout with the image is larger by exactly the image's width.
        expect.that(widthDiff).isEqualTo(50);

        assertThat(tvInRootLayoutWithoutImage.getText().toString()).isEqualTo("FooBar");
        assertThat(tvInRootLayoutWithImage.getText().toString()).isEqualTo("FooABar");
    }

    @Test
    public void inflate_image_intrinsicSizeIsIgnored() {
        LayoutElement root = LayoutElement.newBuilder()
                .setBox(Box.newBuilder()
                        .setWidth(ContainerDimension.newBuilder()
                                .setWrappedDimension(WrappedDimensionProp.newBuilder()))
                        .setHeight(ContainerDimension.newBuilder()
                                .setWrappedDimension(
                                        WrappedDimensionProp.newBuilder()))
                        .addContents(LayoutElement.newBuilder()
                                .setImage(Image.newBuilder()
                                        .setWidth(ImageDimension.newBuilder()
                                                .setLinearDimension(DpProp.newBuilder()
                                                        .setValue(24f)))
                                        .setHeight(ImageDimension.newBuilder()
                                                .setLinearDimension(DpProp.newBuilder()
                                                        .setValue(24f)))
                                        .setResourceId(StringProp.newBuilder()
                                                .setValue("android"))))
                        .addContents(LayoutElement.newBuilder()
                                .setImage(Image.newBuilder()
                                        .setWidth(ImageDimension.newBuilder()
                                                .setExpandedDimension(
                                                        ExpandedDimensionProp.newBuilder()))
                                        .setHeight(ImageDimension.newBuilder()
                                                .setExpandedDimension(
                                                        ExpandedDimensionProp.newBuilder()))
                                        .setResourceId(StringProp.newBuilder()
                                                .setValue("large_image_120dp")))))
                        .build();

        FrameLayout rootLayout = inflateProto(root, /* theme= */ 0, resourceResolvers(), p -> {});

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
    public void inflate_image_undefinedSizeIgnoresIntrinsicSize() {
        // This can happen in the case that a Tile is ever inflated into a Scrolling layout. In that
        // case, the scrolling layout will measure all children with height = UNDEFINED, which can
        // lead to an Image still using its intrinsic size.
        LayoutElement root = LayoutElement.newBuilder()
                .setBox(Box.newBuilder()
                        .setWidth(ContainerDimension.newBuilder()
                                .setWrappedDimension(WrappedDimensionProp.newBuilder()))
                        .setHeight(ContainerDimension.newBuilder()
                                .setWrappedDimension(WrappedDimensionProp.newBuilder()))
                        .addContents(LayoutElement.newBuilder()
                                .setImage(Image.newBuilder()
                                        .setWidth(ImageDimension.newBuilder()
                                                .setLinearDimension(DpProp.newBuilder()
                                                        .setValue(24f)))
                                        .setHeight(ImageDimension.newBuilder()
                                                .setLinearDimension(DpProp.newBuilder()
                                                        .setValue(24f)))
                                        .setResourceId(StringProp.newBuilder()
                                                .setValue("android"))))
                        .addContents(LayoutElement.newBuilder()
                                .setImage(Image.newBuilder()
                                        .setWidth(ImageDimension.newBuilder()
                                                .setExpandedDimension(
                                                        ExpandedDimensionProp.newBuilder()))
                                        .setHeight(ImageDimension.newBuilder()
                                                .setExpandedDimension(
                                                        ExpandedDimensionProp.newBuilder()))
                                        .setResourceId(StringProp.newBuilder()
                                                .setValue("large_image_120dp")))))
                        .build();

        FrameLayout rootLayout = inflateProto(root, /* theme= */ 0, resourceResolvers(), p -> {});

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

    private static Span textSpan(String text) {
        return Span.newBuilder()
                .setText(
                        SpanText.newBuilder()
                                .setText(StringProp.newBuilder().setValue(text))
                                .build())
                .build();
    }

    private static ResourceResolvers.Builder resourceResolvers() {
        return StandardResourceResolvers.forLocalApp(buildResources(), getApplicationContext());
    }

    private static FrameLayout inflateProto(LayoutElement rootElement) {
        return inflateProto(rootElement, 0, resourceResolvers(), p -> {});
    }

    private static FrameLayout inflateProto(
            LayoutElement rootElement,
            int theme,
            ResourceResolvers.Builder resourceResolvers,
            TileRendererInternal.LoadActionListener loadActionListener) {
        Context context = getApplicationContext();

        FrameLayout rootLayout = new FrameLayout(context);

        TileRendererInternal renderer =
                new TileRendererInternal(
                        context,
                        Layout.newBuilder().setRoot(rootElement).build(),
                        resourceResolvers.build(),
                        theme,
                        ContextCompat.getMainExecutor(getApplicationContext()),
                        loadActionListener);
        renderer.inflate(rootLayout);

        // Run a layout pass etc. This is required for basically everything that tries to make
        // assertions about width/height, or relative placement.
        int screenWidth = MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY);
        int screenHeight = MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY);
        rootLayout.measure(screenWidth, screenHeight);
        rootLayout.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        return rootLayout;
    }

    private static Resources buildResources() {
        return Resources.newBuilder()
                .putIdToImage(
                        "android",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(AndroidImageResourceByResId.newBuilder()
                                        .setResourceId(R.drawable.android_24dp))
                                .build())
                .putIdToImage(
                        "does_not_exist",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(AndroidImageResourceByResId.newBuilder()
                                        .setResourceId(-1))
                                .build())
                .putIdToImage(
                        "large_image_120dp",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(AndroidImageResourceByResId.newBuilder()
                                        .setResourceId(R.drawable.ic_channel_foreground))
                                .build())
                .putIdToImage("no_android_resource_set", ImageResource.getDefaultInstance())
                .build();
    }
}
