/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.DrawCircle;
import androidx.compose.remote.core.operations.DrawContent;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.MatrixRestore;
import androidx.compose.remote.core.operations.MatrixRotate;
import androidx.compose.remote.core.operations.MatrixSave;
import androidx.compose.remote.core.operations.MatrixScale;
import androidx.compose.remote.core.operations.MatrixTranslate;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.PaintData;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing;
import androidx.compose.remote.core.types.IntegerConstant;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.Arrays;

public class CustomVisibilityAnimationTest {

    static class TestRemoteContext extends MacroTest.MockRemoteContext {
        TestRemoteContext() {
            super();
            PaintContext pc = org.mockito.Mockito.mock(PaintContext.class);
            org.mockito.Mockito.when(pc.getContext()).thenReturn(this);
            org.mockito.Mockito.when(pc.isAnimationEnabled())
                    .thenAnswer(invocation -> isAnimationEnabled());
            org.mockito.Mockito.when(pc.getMeasureVersion())
                    .thenReturn(
                            androidx.compose.remote.core.operations.layout.managers.LayoutManager
                                    .DEFAULT_MEASURE_TYPE);
            setPaintContext(pc);
        }

        @Override
        public void putObject(int id, @NonNull Object value) {
            mRemoteComposeState.updateObject(id, value);
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport);
        }
    }

    @Test
    public void testCustomExitAnimationExecution() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a FloatFunctionDefine for custom exit animation (id = 100)
        int[] args = new int[] {500, 501, 502, 503, 504}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer.getBuffer(), 100, args);
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Build document: RootLayoutComponent -> BoxLayout with AnimationSpec and Visibility
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 10, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 100f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 100f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 550);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                300f,
                GeneralEasing.CUBIC_STANDARD,
                300f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                100,
                100);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout content
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout
        ContainerEnd.apply(buffer.getBuffer()); // RootLayoutComponent

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(550, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.currentTime = 1000L;

        // Initial layout pass with animations disabled so component is cleanly VISIBLE
        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(10);
        assertNotNull("Box component should exist", box);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);
        assertEquals(100f, box.getWidth(), 0.01f);
        assertEquals(100f, box.getHeight(), 0.01f);
        assertNull("No animation should be active before trigger", box.mAnimateMeasure);

        // Enable animations and trigger visibility change to GONE
        context.setAnimationEnabled(true);
        context.loadInteger(550, Component.Visibility.GONE);

        // Measure pass to register target visibility change and start AnimateMeasure
        context.currentTime = 1000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull("AnimateMeasure should be created on visibility change", box.mAnimateMeasure);

        // Mid-animation: t = 1150 (150ms / 300ms -> ~50% progress)
        context.currentTime = 1150L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull("AnimateMeasure should still be active at 50%", box.mAnimateMeasure);
        assertTrue("AnimateMeasure should not be done at 50%", !box.mAnimateMeasure.isDone());

        // Verify variable binding for custom animation
        float progress = context.getFloat(500);
        assertTrue(
                "Progress should be between 0.1 and 0.9, got " + progress,
                progress > 0.1f && progress < 0.9f);
        assertEquals(100f, context.getFloat(501), 0.01f); // width
        assertEquals(100f, context.getFloat(502), 0.01f); // height

        // End of animation: t = 1350 (>300ms)
        context.currentTime = 1350L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertEquals(
                "Component visibility should be GONE after animation ends",
                Component.Visibility.GONE,
                box.mVisibility);
    }

    @Test
    public void testCustomEnterAnimationExecution() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a FloatFunctionDefine for custom enter animation (id = 200)
        int[] args = new int[] {600, 601, 602, 603, 604}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer.getBuffer(), 200, args);
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Build document: RootLayoutComponent -> BoxLayout starting GONE
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 20, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 120f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 80f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 650);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                200,
                200);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout content
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout
        ContainerEnd.apply(buffer.getBuffer()); // RootLayoutComponent

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(650, Component.Visibility.GONE);
        doc.applyDataOperations(context);
        context.setAnimationEnabled(true);
        context.currentTime = 2000L;

        // Initial layout pass with GONE
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(20);
        assertNotNull("Box component should exist", box);
        assertEquals(Component.Visibility.GONE, box.mVisibility);

        // Change visibility to VISIBLE
        context.loadInteger(650, Component.Visibility.VISIBLE);

        context.currentTime = 2000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull("AnimateMeasure should be active for enter animation", box.mAnimateMeasure);

        // Mid-animation: t = 2200 (200ms / 400ms -> ~50% progress)
        context.currentTime = 2200L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        float enterProgress = context.getFloat(600);
        assertTrue(
                "Enter progress should be between 0.1 and 0.9, got " + enterProgress,
                enterProgress > 0.1f && enterProgress < 0.9f);
        assertEquals(120f, context.getFloat(601), 0.01f); // width
        assertEquals(80f, context.getFloat(602), 0.01f); // height

        // End of animation: t = 2450 (>400ms)
        context.currentTime = 2450L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertEquals(
                "Component visibility should be VISIBLE after animation ends",
                Component.Visibility.VISIBLE,
                box.mVisibility);
    }

    @Test
    public void testCustomParticleExplosionAnimationExecution() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a FloatFunctionDefine for custom particle explosion exit animation (id = 300)
        int[] args = new int[] {700, 701, 702, 703, 704}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer.getBuffer(), 300, args);
        // Scaled content dissolution + particle draw
        MatrixSave.apply(buffer.getBuffer());
        MatrixScale.apply(buffer.getBuffer(), 1f, 1f, 100f, 50f);
        DrawContent.apply(buffer.getBuffer());
        MatrixRestore.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Build document: RootLayoutComponent -> BoxLayout
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 30, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 200f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 100f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 750);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                500f,
                GeneralEasing.CUBIC_STANDARD,
                500f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                300,
                300);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout content
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout
        ContainerEnd.apply(buffer.getBuffer()); // RootLayoutComponent

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(750, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.currentTime = 3000L;

        // Initial layout pass with animations disabled
        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(30);
        assertNotNull("Box component should exist", box);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);
        assertEquals(200f, box.getWidth(), 0.01f);
        assertEquals(100f, box.getHeight(), 0.01f);
        assertNull(box.mAnimateMeasure);

        // Enable animations and trigger visibility change to GONE
        context.setAnimationEnabled(true);
        context.loadInteger(750, Component.Visibility.GONE);

        // Measure pass to register target visibility change and start AnimateMeasure
        context.currentTime = 3000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull("AnimateMeasure should be active for explosion exit", box.mAnimateMeasure);

        // Mid-animation: t = 3250 (250ms / 500ms -> ~50% progress)
        context.currentTime = 3250L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull("AnimateMeasure should still be active at 50%", box.mAnimateMeasure);
        assertTrue("AnimateMeasure should not be done at 50%", !box.mAnimateMeasure.isDone());

        // Verify particle function received dynamic progress, width, and height
        float progress = context.getFloat(700);
        assertTrue(
                "Explosion progress should be between 0.1 and 0.9, got " + progress,
                progress > 0.1f && progress < 0.9f);
        assertEquals(200f, context.getFloat(701), 0.01f); // width
        assertEquals(100f, context.getFloat(702), 0.01f); // height

        // End of animation: t = 3550 (>500ms)
        context.currentTime = 3550L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertEquals(
                "Component visibility should be GONE after explosion ends",
                Component.Visibility.GONE,
                box.mVisibility);
    }

    @Test
    public void testCustomVortexAnimationExecution() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Custom Vortex Exit Function (id = 400)
        int[] args = new int[] {800, 801, 802, 803, 804}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer.getBuffer(), 400, args);
        MatrixSave.apply(buffer.getBuffer());
        MatrixScale.apply(buffer.getBuffer(), 0.5f, 0.5f, 100f, 50f);
        DrawContent.apply(buffer.getBuffer());
        MatrixRestore.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Document: Root -> Box
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 40, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 150f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 75f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 850);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                650f,
                GeneralEasing.CUBIC_STANDARD,
                650f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                400,
                400);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(850, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.currentTime = 4000L;

        // Initial layout pass with animations disabled
        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(40);
        assertNotNull(box);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);
        assertNull(box.mAnimateMeasure);

        // Enable animations and trigger exit to GONE
        context.setAnimationEnabled(true);
        context.loadInteger(850, Component.Visibility.GONE);
        context.currentTime = 4000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull(box.mAnimateMeasure);

        // Mid-animation at 50%
        context.currentTime = 4325L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        float progress = context.getFloat(800);
        assertTrue("Vortex progress should be active", progress > 0.1f && progress < 0.9f);
        assertEquals(150f, context.getFloat(801), 0.01f);
        assertEquals(75f, context.getFloat(802), 0.01f);

        // Finish animation
        context.currentTime = 4700L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);
        assertEquals(Component.Visibility.GONE, box.mVisibility);
    }

    @Test
    public void testCustomGlitchHologramAnimationExecution() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Custom Hologram Laser Enter Function (id = 500)
        int[] args = new int[] {900, 901, 902, 903, 904}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer.getBuffer(), 500, args);
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Document: Root -> Box (initially GONE)
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 50, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 180f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 90f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 950);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                600f,
                GeneralEasing.CUBIC_STANDARD,
                600f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                500,
                500);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(950, Component.Visibility.GONE);
        doc.applyDataOperations(context);
        context.currentTime = 5000L;

        // Initial layout pass with animations disabled
        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(50);
        assertNotNull(box);
        assertEquals(Component.Visibility.GONE, box.mVisibility);
        assertNull(box.mAnimateMeasure);

        // Enable animations and trigger enter to VISIBLE
        context.setAnimationEnabled(true);
        context.loadInteger(950, Component.Visibility.VISIBLE);
        context.currentTime = 5000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        assertNotNull(box.mAnimateMeasure);

        // Mid-animation
        context.currentTime = 5300L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        float progress = context.getFloat(900);
        assertTrue("Hologram enter progress should be active", progress > 0.1f && progress < 0.9f);
        assertEquals(180f, context.getFloat(901), 0.01f);
        assertEquals(90f, context.getFloat(902), 0.01f);

        // End of animation
        context.currentTime = 5650L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);
    }

    @Test
    public void generateRcDocument() throws Exception {
        WireBuffer buffer = new WireBuffer();

        short[] tags =
                new short[] {
                    Header.DOC_WIDTH,
                    Header.DOC_HEIGHT,
                    Header.DOC_PROFILES,
                    Header.DOC_DENSITY_BEHAVIOR
                };
        Object[] values =
                new Object[] {
                    400,
                    600,
                    RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL,
                    CoreDocument.DENSITY_BEHAVIOR_DP
                };
        Header.apply(buffer, 7, tags, values);

        // Named variables for visibility states 1..5
        for (int i = 1; i <= 5; i++) {
            IntegerConstant.apply(buffer, i, Component.Visibility.VISIBLE);
            NamedVariable.apply(buffer, i, NamedVariable.INT_TYPE, "vis" + i);
        }

        // Custom Visibility Animation 1 (Scale + Rotate, function ID 101)
        int[] args1 = new int[] {10, 11, 12, 13, 14}; // progress, w, h, x, y
        FloatFunctionDefine.apply(buffer, 101, args1);
        MatrixSave.apply(buffer);
        MatrixRotate.apply(buffer, 0f, 130f, 27f);
        MatrixScale.apply(buffer, 1f, 1f, 130f, 27f);
        DrawContent.apply(buffer);
        MatrixRestore.apply(buffer);
        ContainerEnd.apply(buffer);

        // Custom Visibility Animation 2 (Slide + Ring, function ID 102)
        int[] args2 = new int[] {20, 21, 22, 23, 24};
        FloatFunctionDefine.apply(buffer, 102, args2);
        MatrixSave.apply(buffer);
        MatrixTranslate.apply(buffer, 0f, 0f);
        DrawContent.apply(buffer);
        MatrixRestore.apply(buffer);
        // Accent ring
        MatrixSave.apply(buffer);
        PaintBundle strokePaint = new PaintBundle();
        strokePaint.setColor(0xFF00E5FF);
        strokePaint.setStyle(1); // 1 = STROKE
        strokePaint.setStrokeWidth(3f);
        PaintData.apply(buffer, strokePaint);
        DrawCircle.apply(buffer, 130f, 27f, 35f);
        MatrixRestore.apply(buffer);
        ContainerEnd.apply(buffer);

        // Custom Visibility Animation 3 (Particle Explosion, function ID 103)
        int[] args3 = new int[] {30, 31, 32, 33, 34};
        FloatFunctionDefine.apply(buffer, 103, args3);
        MatrixSave.apply(buffer);
        MatrixScale.apply(buffer, 1f, 1f, 130f, 27f);
        DrawContent.apply(buffer);
        MatrixRestore.apply(buffer);
        // Shockwave ring
        MatrixSave.apply(buffer);
        PaintBundle shockwavePaint = new PaintBundle();
        shockwavePaint.setColor(0xFFFFD54F);
        shockwavePaint.setStyle(1); // 1 = STROKE
        shockwavePaint.setStrokeWidth(3f);
        PaintData.apply(buffer, shockwavePaint);
        DrawCircle.apply(buffer, 130f, 27f, 35f);
        MatrixRestore.apply(buffer);
        // Particle burst
        MatrixSave.apply(buffer);
        PaintBundle particlePaint = new PaintBundle();
        particlePaint.setColor(0xFF9C27B0);
        particlePaint.setStyle(0); // 0 = FILL
        PaintData.apply(buffer, particlePaint);
        DrawCircle.apply(buffer, 110f, 15f, 6f);
        DrawCircle.apply(buffer, 150f, 15f, 6f);
        DrawCircle.apply(buffer, 130f, 8f, 5f);
        DrawCircle.apply(buffer, 130f, 42f, 5f);
        DrawCircle.apply(buffer, 90f, 27f, 6f);
        DrawCircle.apply(buffer, 170f, 27f, 6f);
        MatrixRestore.apply(buffer);
        ContainerEnd.apply(buffer);

        // Custom Visibility Animation 4 (Cosmic Vortex, function ID 104)
        int[] args4 = new int[] {40, 41, 42, 43, 44};
        FloatFunctionDefine.apply(buffer, 104, args4);
        MatrixSave.apply(buffer);
        MatrixRotate.apply(buffer, 0f, 130f, 27f);
        MatrixScale.apply(buffer, 1f, 1f, 130f, 27f);
        DrawContent.apply(buffer);
        MatrixRestore.apply(buffer);
        MatrixSave.apply(buffer);
        PaintBundle vortexPaint = new PaintBundle();
        vortexPaint.setColor(0xFF00E5FF);
        vortexPaint.setStyle(1);
        vortexPaint.setStrokeWidth(2f);
        PaintData.apply(buffer, vortexPaint);
        DrawCircle.apply(buffer, 130f, 27f, 28f);
        MatrixRestore.apply(buffer);
        ContainerEnd.apply(buffer);

        // Custom Visibility Animation 5 (Cyberpunk Hologram / Laser Scan, function ID 105)
        int[] args5 = new int[] {50, 51, 52, 53, 54};
        FloatFunctionDefine.apply(buffer, 105, args5);
        MatrixSave.apply(buffer);
        DrawContent.apply(buffer);
        MatrixRestore.apply(buffer);
        ContainerEnd.apply(buffer);

        // Root Layout -> Column -> Boxes 1..5
        RootLayoutComponent.apply(buffer, -1);
        ColumnLayout.apply(buffer, 1, -1, ColumnLayout.CENTER, ColumnLayout.CENTER, 0f);
        WidthModifierOperation.apply(buffer, DimensionModifierOperation.Type.FILL.ordinal(), 1f);
        HeightModifierOperation.apply(buffer, DimensionModifierOperation.Type.FILL.ordinal(), 1f);
        PaddingModifierOperation.apply(buffer, 16f, 16f, 16f, 16f);
        LayoutComponentContent.apply(buffer, -1);

        int[][] cardConfigs =
                new int[][] {
                    {10, 1, 101, 0xFFFF5722}, // Scale & Rotate (Deep Orange)
                    {20, 2, 102, 0xFF3F51B5}, // Slide & Ring (Indigo)
                    {30, 3, 103, 0xFF9C27B0}, // Particle Explosion (Purple)
                    {40, 4, 104, 0xFF1A237E}, // Cosmic Vortex (Navy)
                    {50, 5, 105, 0xFF00695C}, // Cyberpunk Hologram (Teal)
                };

        for (int[] cfg : cardConfigs) {
            int compId = cfg[0];
            int visId = cfg[1];
            int funcId = cfg[2];
            int color = cfg[3];
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            BoxLayout.apply(buffer, compId, -1, BoxLayout.CENTER, BoxLayout.CENTER);
            WidthModifierOperation.apply(
                    buffer, DimensionModifierOperation.Type.EXACT.ordinal(), 260f);
            HeightModifierOperation.apply(
                    buffer, DimensionModifierOperation.Type.EXACT.ordinal(), 54f);
            ComponentVisibilityOperation.apply(buffer, visId);
            AnimationSpec.apply(
                    buffer,
                    compId / 10,
                    450f,
                    GeneralEasing.CUBIC_STANDARD,
                    600f,
                    GeneralEasing.CUBIC_STANDARD,
                    AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                    AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                    funcId,
                    funcId);
            BackgroundModifierOperation.apply(
                    buffer, 0, 0, 0, 0, r, g, b, 1.0f, ShapeType.ROUNDED_RECTANGLE);
            LayoutComponentContent.apply(buffer, -1);
            ContainerEnd.apply(buffer); // Box content
            ContainerEnd.apply(buffer); // Box
        }

        ContainerEnd.apply(buffer); // Column content
        ContainerEnd.apply(buffer); // Column
        ContainerEnd.apply(buffer); // Root

        byte[] exportBytes = Arrays.copyOf(buffer.getBuffer(), buffer.getIndex());
        assertTrue("Buffer should have written bytes", exportBytes.length > 0);
    }

    @Test
    public void midAnimationVisibilityToggleDoesNotRestartAfterCompletion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags =
                new short[] {
                    Header.DOC_WIDTH,
                    Header.DOC_HEIGHT,
                    Header.DOC_PROFILES,
                };
        Object[] values =
                new Object[] {
                    400,
                    400,
                    513,
                };
        buffer.addHeader(tags, values);

        int enterFnId = 601;
        int exitFnId = 602;
        FloatFunctionDefine.apply(
                buffer.getBuffer(), enterFnId, new int[] {900, 901, 902, 903, 904});
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        FloatFunctionDefine.apply(
                buffer.getBuffer(), exitFnId, new int[] {900, 901, 902, 903, 904});
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 60, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 120f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 80f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 960);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                500f,
                GeneralEasing.CUBIC_LINEAR,
                500f,
                GeneralEasing.CUBIC_LINEAR,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                enterFnId,
                exitFnId);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        context.loadInteger(960, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.currentTime = 1000L;

        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);

        Component box = doc.getComponent(60);
        assertNotNull(box);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);
        assertNull(box.mAnimateMeasure);

        // 1. Toggle VISIBLE -> GONE at t=1000
        context.setAnimationEnabled(true);
        context.loadInteger(960, Component.Visibility.GONE);
        context.currentTime = 1000L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);
        assertNotNull(box.mAnimateMeasure);

        // 2. Halfway through exit animation (t=1250), toggle back GONE -> VISIBLE
        context.loadInteger(960, Component.Visibility.VISIBLE);
        context.currentTime = 1250L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);
        assertNotNull("Animation should still be active after retargeting", box.mAnimateMeasure);
        assertEquals(
                "Component visibility should reflect updated target",
                Component.Visibility.VISIBLE,
                box.mVisibility);

        // 3. Advance past completion of the retargeted animation (t=1800)
        context.currentTime = 1800L;
        doc.paint(context, 0);
        assertNull("Animation should be finished and cleared at t=1800", box.mAnimateMeasure);
        assertEquals(Component.Visibility.VISIBLE, box.mVisibility);

        // 4. Subsequent measure/paint pass must NOT start a duplicate animation
        context.currentTime = 1850L;
        doc.measure(context, 0f, 400f, 0f, 400f);
        doc.paint(context, 0);
        assertNull(
                "No duplicate animation should start after completion",
                box.mAnimateMeasure);
    }

    @Test
    public void densityBehaviorDpScalesExactModifiersAndEmptyBitmaps() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags =
                new short[] {
                    Header.DOC_WIDTH,
                    Header.DOC_HEIGHT,
                    Header.DOC_PROFILES,
                    Header.DOC_DENSITY_BEHAVIOR,
                };
        Object[] values =
                new Object[] {
                    400,
                    400,
                    513,
                    CoreDocument.DENSITY_BEHAVIOR_DP,
                };
        buffer.addHeader(tags, values);

        int exitFnId = 602;
        FloatFunctionDefine.apply(
                buffer.getBuffer(), exitFnId, new int[] {900, 901, 902, 903, 904});
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 60, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 260f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 30f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 960);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                500f,
                GeneralEasing.CUBIC_LINEAR,
                500f,
                GeneralEasing.CUBIC_LINEAR,
                AnimationSpec.ANIMATION.FADE_IN.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                -1,
                exitFnId);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context, null);
        context.setDensity(2.5f);
        context.loadInteger(960, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);

        // Changing density marks ID_DENSITY listeners dirty; next paint re-evaluates them
        context.setDensity(3.0f);
        context.currentTime = 1000L;
        context.setAnimationEnabled(false);
        doc.measure(context, 0f, 1200f, 0f, 1200f);
        doc.paint(context, 0);

        Component box = doc.getComponent(60);
        assertNotNull(box);
        assertEquals(780f, box.getWidth(), 0.01f);
        assertEquals(90f, box.getHeight(), 0.01f);

        // Trigger exit animation and verify w (901) and h (902) passed to function are 780x90
        context.setAnimationEnabled(true);
        context.loadInteger(960, Component.Visibility.GONE);
        context.currentTime = 1100L;
        doc.measure(context, 0f, 1200f, 0f, 1200f);
        doc.paint(context, 0);

        assertEquals(780f, context.getFloat(901), 0.01f);
        assertEquals(90f, context.getFloat(902), 0.01f);
    }

    @Test
    public void testBeforeAndAfterLayoutSequencing() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Custom enter (id = 301, records progress into 701) & exit (id = 302, records into 702)
        FloatFunctionDefine.apply(buffer.getBuffer(), 301, new int[] {701});
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        FloatFunctionDefine.apply(buffer.getBuffer(), 302, new int[] {702});
        DrawContent.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        androidx.compose.remote.core.operations.layout.managers.ColumnLayout.apply(
                buffer.getBuffer(),
                70,
                -1,
                androidx.compose.remote.core.operations.layout.managers.ColumnLayout.START,
                androidx.compose.remote.core.operations.layout.managers.ColumnLayout.TOP,
                0f);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 200f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 300f);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);

        // Box 1 (id = 71): height 100, exitSequence = BEFORE, enterSequence = AFTER
        // motionDuration = 400ms, visibilityDuration = 600ms
        BoxLayout.apply(buffer.getBuffer(), 71, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 200f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 100f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 750);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                400f,
                GeneralEasing.CUBIC_LINEAR,
                600f,
                GeneralEasing.CUBIC_LINEAR,
                AnimationSpec.packAnimation(
                        AnimationSpec.ANIMATION.CUSTOM, AnimationSpec.SEQUENCE.AFTER),
                AnimationSpec.packAnimation(
                        AnimationSpec.ANIMATION.CUSTOM, AnimationSpec.SEQUENCE.BEFORE),
                301,
                302);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Box 2 (id = 72, sibling below Box 1): height 100, motionDuration = 400ms
        BoxLayout.apply(buffer.getBuffer(), 72, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 200f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 100f);
        AnimationSpec.apply(
                buffer.getBuffer(),
                2,
                400f,
                GeneralEasing.CUBIC_LINEAR,
                400f,
                GeneralEasing.CUBIC_LINEAR,
                AnimationSpec.ANIMATION.FADE_IN.ordinal(),
                AnimationSpec.ANIMATION.FADE_OUT.ordinal());
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        ContainerEnd.apply(buffer.getBuffer()); // ColumnLayout content
        ContainerEnd.apply(buffer.getBuffer()); // ColumnLayout
        ContainerEnd.apply(buffer.getBuffer()); // RootLayoutComponent

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        context.mWidth = 400f;
        context.mHeight = 400f;
        doc.initializeContext(context);
        context.loadInteger(750, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.setAnimationEnabled(true);

        // Initial layout at t = 1000: Box 1 at y = 0, Box 2 at y = 100
        context.currentTime = 1000L;
        doc.paint(context, 0);

        Component box1 = doc.getComponent(71);
        Component box2 = doc.getComponent(72);
        assertNotNull(box1);
        assertNotNull(box2);
        assertEquals(0f, box1.getY(), 0.01f);
        assertEquals(100f, box2.getY(), 0.01f);

        // 1. Trigger EXIT (VISIBLE -> GONE) with exitSequence = BEFORE (visibilityDuration = 600ms)
        context.loadInteger(750, Component.Visibility.GONE);
        context.currentTime = 1000L;
        doc.paint(context, 0);

        // At t = 1300 (halfway through 600ms exit animation):
        // Box 1 should be playing exit animation (~0.5 progress), while Box 2 STILL holds y = 100!
        context.currentTime = 1300L;
        doc.paint(context, 0);
        assertEquals(0.5f, context.getFloat(702), 0.05f);
        assertEquals("Sibling should not move while BEFORE exit animation runs",
                100f, box2.getY(), 0.01f);

        // At t = 1600 (600ms exit animation finishes):
        // Box 1 becomes GONE and triggers Phase 2 layout so Box 2 starts animating y: 100 -> 0
        context.currentTime = 1600L;
        doc.paint(context, 0);
        assertEquals(Component.Visibility.GONE, box1.mVisibility);

        // At t = 1800 (200ms into 400ms sibling layout motion):
        // Box 2 should be halfway between y = 100 and y = 0 (y = 50)
        context.currentTime = 1800L;
        doc.paint(context, 0);
        assertEquals("Sibling should animate its bounds after BEFORE exit finishes",
                50f, box2.getY(), 1.0f);

        // At t = 2000 (400ms sibling layout motion finishes): Box 2 reaches y = 0
        context.currentTime = 2000L;
        doc.paint(context, 0);
        assertEquals(0f, box2.getY(), 0.01f);

        // 2. Trigger ENTER (GONE -> VISIBLE) with enterSequence = AFTER
        // (motionDuration = 400ms, visibilityDuration = 600ms)
        context.loadFloat(701, -1f);
        context.loadInteger(750, Component.Visibility.VISIBLE);
        context.currentTime = 3000L;
        doc.paint(context, 0);

        // At t = 3200 (200ms into 400ms layout motion):
        // Sibling Box 2 is moving down (y ~ 50), while Box 1's enter animation has NOT started yet!
        context.currentTime = 3200L;
        doc.paint(context, 0);
        assertEquals(50f, box2.getY(), 1.0f);
        assertEquals("Enter animation should not execute before layout finishes",
                -1f, context.getFloat(701), 0.01f);

        // At t = 3700 (400ms layout + 300ms into 600ms visibility enter = 50% enter progress):
        // Sibling Box 2 is at y = 100, and Box 1's enter animation is at 0.5 progress!
        context.currentTime = 3700L;
        doc.paint(context, 0);
        assertEquals(100f, box2.getY(), 0.01f);
        assertEquals(0.5f, context.getFloat(701), 0.05f);
    }

    @Test
    public void nestedComponentsSharingSameOffscreenVisibilityAnimation_preservesArgsAndBitmaps() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    400, 400, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        int sharedFnId = 800;
        int argProgress = 810;
        int argW = 811;
        int argH = 812;
        int argX = 813;
        int argY = 814;
        int argId = 815;
        int offscreenBmpId = 820;

        // Define a single shared visibility animation function used by BOTH parent and child
        FloatFunctionDefine.apply(
                buffer.getBuffer(),
                sharedFnId,
                new int[] {argProgress, argW, argH, argX, argY, argId});
        buffer.createOffscreenBitmap(offscreenBmpId);
        // Capture component to offscreen bitmap (for parent, this triggers child's animation!)
        androidx.compose.remote.core.operations.DrawToBitmap.apply(
                buffer.getBuffer(),
                offscreenBmpId,
                androidx.compose.remote.core.operations.DrawToBitmap.MODE_COMPONENT_ID,
                argId);
        DrawContent.apply(buffer.getBuffer());
        androidx.compose.remote.core.operations.DrawToBitmap.apply(buffer.getBuffer(), 0, 0, 0);
        // AFTER DrawContent / DrawToBitmap(0), evaluate argW and argH into 910 and 911:
        // Since parent executes this line AFTER child's nested call finishes, 910 and 911 will
        // equal parent's 300x160 only if child's nested execution restored parent's args!
        androidx.compose.remote.core.operations.FloatExpression.apply(
                buffer.getBuffer(),
                910,
                new float[] {androidx.compose.remote.core.operations.Utils.asNan(argW)},
                null);
        androidx.compose.remote.core.operations.FloatExpression.apply(
                buffer.getBuffer(),
                911,
                new float[] {androidx.compose.remote.core.operations.Utils.asNan(argH)},
                null);
        ContainerEnd.apply(buffer.getBuffer());

        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        // Parent Box (300x160)
        BoxLayout.apply(buffer.getBuffer(), 80, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 300f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 160f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 850);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                sharedFnId,
                sharedFnId);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);

        // Child Box inside Parent (120x50) using the EXACT SAME sharedFnId
        BoxLayout.apply(buffer.getBuffer(), 81, -1, BoxLayout.START, BoxLayout.TOP);
        WidthModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 120f);
        HeightModifierOperation.apply(
                buffer.getBuffer(), DimensionModifierOperation.Type.EXACT.ordinal(), 50f);
        ComponentVisibilityOperation.apply(buffer.getBuffer(), 851);
        AnimationSpec.apply(
                buffer.getBuffer(),
                1,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                400f,
                GeneralEasing.CUBIC_STANDARD,
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                AnimationSpec.ANIMATION.CUSTOM.ordinal(),
                sharedFnId,
                sharedFnId);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer()); // End Child Box

        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer()); // End Parent Box
        ContainerEnd.apply(buffer.getBuffer()); // End Root

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        context.mWidth = 400f;
        context.mHeight = 400f;
        doc.initializeContext(context);
        context.loadInteger(850, Component.Visibility.VISIBLE);
        context.loadInteger(851, Component.Visibility.VISIBLE);
        doc.applyDataOperations(context);
        context.setAnimationEnabled(true);

        context.currentTime = 1000L;
        doc.paint(context, 0);

        // Trigger simultaneous exit animations on BOTH parent and child
        context.loadInteger(850, Component.Visibility.GONE);
        context.loadInteger(851, Component.Visibility.GONE);
        context.currentTime = 1000L;
        doc.paint(context, 0);

        context.currentTime = 1200L;
        doc.paint(context, 0);

        // Parent's post-capture expressions (executed after child's nested animation) must read
        // parent's restored 300x160 bounds, not child's 120x50 bounds!
        assertEquals(300f, context.getFloat(910), 0.01f);
        assertEquals(160f, context.getFloat(911), 0.01f);
    }
}
