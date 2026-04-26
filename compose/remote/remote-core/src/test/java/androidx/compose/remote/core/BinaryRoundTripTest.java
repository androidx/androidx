/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.BitmapFontData;
import androidx.compose.remote.core.operations.ClickArea;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.ClipRect;
import androidx.compose.remote.core.operations.ColorAttribute;
import androidx.compose.remote.core.operations.ColorConstant;
import androidx.compose.remote.core.operations.ColorExpression;
import androidx.compose.remote.core.operations.ComponentValue;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.DataDynamicListFloat;
import androidx.compose.remote.core.operations.DataListFloat;
import androidx.compose.remote.core.operations.DataListIds;
import androidx.compose.remote.core.operations.DataMapIds;
import androidx.compose.remote.core.operations.DataMapLookup;
import androidx.compose.remote.core.operations.DebugMessage;
import androidx.compose.remote.core.operations.DrawArc;
import androidx.compose.remote.core.operations.DrawBitmap;
import androidx.compose.remote.core.operations.DrawBitmapFontText;
import androidx.compose.remote.core.operations.DrawBitmapInt;
import androidx.compose.remote.core.operations.DrawBitmapScaled;
import androidx.compose.remote.core.operations.DrawCircle;
import androidx.compose.remote.core.operations.DrawContent;
import androidx.compose.remote.core.operations.DrawLine;
import androidx.compose.remote.core.operations.DrawOval;
import androidx.compose.remote.core.operations.DrawPath;
import androidx.compose.remote.core.operations.DrawRect;
import androidx.compose.remote.core.operations.DrawRoundRect;
import androidx.compose.remote.core.operations.DrawSector;
import androidx.compose.remote.core.operations.DrawText;
import androidx.compose.remote.core.operations.DrawTextAnchored;
import androidx.compose.remote.core.operations.DrawTextOnPath;
import androidx.compose.remote.core.operations.DrawToBitmap;
import androidx.compose.remote.core.operations.DrawTweenPath;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.FloatFunctionCall;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.HapticFeedback;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.IdLookup;
import androidx.compose.remote.core.operations.ImageAttribute;
import androidx.compose.remote.core.operations.IncludeReferencedOperations;
import androidx.compose.remote.core.operations.IntegerExpression;
import androidx.compose.remote.core.operations.MatrixFromPath;
import androidx.compose.remote.core.operations.MatrixRestore;
import androidx.compose.remote.core.operations.MatrixRotate;
import androidx.compose.remote.core.operations.MatrixSave;
import androidx.compose.remote.core.operations.MatrixScale;
import androidx.compose.remote.core.operations.MatrixSkew;
import androidx.compose.remote.core.operations.MatrixTranslate;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.PaintData;
import androidx.compose.remote.core.operations.ParticlesCompare;
import androidx.compose.remote.core.operations.ParticlesCreate;
import androidx.compose.remote.core.operations.ParticlesLoop;
import androidx.compose.remote.core.operations.PathAppend;
import androidx.compose.remote.core.operations.PathCombine;
import androidx.compose.remote.core.operations.PathCreate;
import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.PathExpression;
import androidx.compose.remote.core.operations.PathTween;
import androidx.compose.remote.core.operations.ReferencedOperations;
import androidx.compose.remote.core.operations.Rem;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.RootContentDescription;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.TextAttribute;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.TextFromFloat;
import androidx.compose.remote.core.operations.TextLength;
import androidx.compose.remote.core.operations.TextLookup;
import androidx.compose.remote.core.operations.TextLookupInt;
import androidx.compose.remote.core.operations.TextMeasure;
import androidx.compose.remote.core.operations.TextMerge;
import androidx.compose.remote.core.operations.TextSubtext;
import androidx.compose.remote.core.operations.TextTransform;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.WakeIn;
import androidx.compose.remote.core.operations.layout.CanvasContent;
import androidx.compose.remote.core.operations.layout.CanvasOperations;
import androidx.compose.remote.core.operations.layout.ClickModifierOperation;
import androidx.compose.remote.core.operations.layout.ComponentStart;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.ImpulseOperation;
import androidx.compose.remote.core.operations.layout.ImpulseProcess;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.LoopOperation;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation;
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout;
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.CoreText;
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout;
import androidx.compose.remote.core.operations.layout.managers.FlowLayout;
import androidx.compose.remote.core.operations.layout.managers.ImageLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.layout.managers.StateLayout;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.managers.TextStyle;
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation;
import androidx.compose.remote.core.operations.loom.PatternArgument;
import androidx.compose.remote.core.operations.loom.PatternBlock;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternForEach;
import androidx.compose.remote.core.operations.loom.PatternInflation;
import androidx.compose.remote.core.operations.matrix.MatrixConstant;
import androidx.compose.remote.core.operations.matrix.MatrixExpression;
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.types.BooleanConstant;
import androidx.compose.remote.core.types.IntegerConstant;
import androidx.compose.remote.core.types.LongConstant;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

/** Exhaustive test for deepCopy implementation of all operations. */
public class BinaryRoundTripTest {
    @Test
    public void testDeepCopyAll() {
        ArrayList<Operation> originalOps = new ArrayList<>();

        // --- Protocol & Data Types ---
        originalOps.add(new Header(100, 100, 0, 0, 0, 1.0f, 0L));
        originalOps.add(new Theme(1));
        originalOps.add(new ClickArea(1, 100, 0, 0, 100, 100, -1));
        originalOps.add(new RootContentBehavior(0, 0, 0, 0));
        originalOps.add(new RootContentDescription(100));
        originalOps.add(new BooleanConstant(1, true));
        originalOps.add(new IntegerConstant(2, 42));
        originalOps.add(new FloatConstant(3, 3.14f));
        originalOps.add(new LongConstant(4, 123456789L));
        originalOps.add(new TextData(5, "test"));
        originalOps.add(new ColorConstant(6, 0xFFFF0000));
        originalOps.add(
                new ColorExpression(7, ColorExpression.ARGB_MODE, 0xFFFF0000, 0xFF00FF00, 0.5f));
        originalOps.add(new FloatExpression(8, new float[] {1}, null));
        originalOps.add(new IntegerExpression(9, 0, new int[] {1, 2, 3}));
        originalOps.add(new NamedVariable(10, 1, "var"));
        originalOps.add(new DataListIds(11, new int[] {1, 2, 3}));
        originalOps.add(new DataListFloat(12, new float[] {1.1f, 2.2f}));
        originalOps.add(new DataDynamicListFloat(13, 10));
        originalOps.add(new DataMapIds(14, new String[] {"a"}, new byte[] {1}, new int[] {1}));
        originalOps.add(new DataMapLookup(15, 14, 0));

        // --- Specialized Data ---
        originalOps.add(new BitmapData(100, 10, 10, new byte[100]));
        originalOps.add(new PathData(101, new float[] {0, 0, 10, 10}, 0));
        originalOps.add(
                new ShaderData(102, 100, new HashMap<>(), new HashMap<>(), new HashMap<>()));
        originalOps.add(new BitmapFontData(103, new BitmapFontData.Glyph[0]));

        // --- Drawing Operations ---
        originalOps.add(new DrawRect(0, 0, 100, 100));
        originalOps.add(new DrawCircle(50, 50, 50));
        originalOps.add(new DrawLine(0, 0, 100, 100));
        originalOps.add(new DrawRoundRect(0, 0, 100, 100, 10, 10));
        originalOps.add(new DrawArc(0, 0, 100, 100, 0, 90));
        originalOps.add(new DrawSector(0, 0, 100, 100, 0, 90));
        originalOps.add(new DrawOval(0, 0, 100, 100));
        originalOps.add(new DrawPath(101));
        originalOps.add(new DrawText(100, 0, 4, 0, 0, 0, 4, false));
        originalOps.add(new DrawTextAnchored(100, 0, 0, 0.5f, 0.5f, 0));
        originalOps.add(new DrawTextOnPath(100, 101, 0, 0));
        originalOps.add(new DrawBitmap(1, 0, 0, 10, 10, 0));
        originalOps.add(new DrawBitmapInt(1, 0, 0, 10, 10, 0, 0, 10, 10, 0));
        originalOps.add(new DrawBitmapScaled(1, 0, 0, 10, 10, 0, 0, 10, 10, 0, 1.0f, 0));
        originalOps.add(new DrawBitmapFontText(100, 103, 0, 4, 0, 0, 0));
        originalOps.add(new DrawTweenPath(101, 101, 0.5f, 0f, 1f));
        originalOps.add(new DrawToBitmap(1, 0, 0xFFFF0000));
        originalOps.add(new DrawContent());

        // --- Matrix & Transforms ---
        originalOps.add(new MatrixTranslate(10, 10));
        originalOps.add(new MatrixRotate(45, 50, 50));
        originalOps.add(new MatrixScale(2, 2, 0, 0));
        originalOps.add(new MatrixSkew(0.1f, 0.1f));
        originalOps.add(new MatrixSave());
        originalOps.add(new MatrixRestore());
        originalOps.add(new MatrixConstant(200, 0, new float[9]));
        originalOps.add(new MatrixExpression(201, 0, new float[3]));
        originalOps.add(new MatrixVectorMath((short) 1, new int[] {1, 2}, 200, new float[2]));
        originalOps.add(new MatrixFromPath(101, 0.5f, 0f, 0));

        // --- Clips ---
        originalOps.add(new ClipRect(0, 0, 50, 50));
        originalOps.add(new ClipPath(101, 0));

        // --- Path & Text specialized ---
        originalOps.add(new PathCreate(300, 0, 0));
        originalOps.add(new PathAppend(300, new float[] {10, 10, 20, 20}));
        originalOps.add(new PathCombine(301, 300, 300, (byte) 0));
        originalOps.add(new PathTween(302, 300, 300, 0.5f));
        originalOps.add(new PathExpression(303, new float[1], new float[1], 0, 1, 10, (short) 0));

        originalOps.add(new TextMerge(400, 5, 5));
        originalOps.add(new TextFromFloat(401, 3.14f, (short) 1, (short) 2, 0));
        originalOps.add(new TextLength(402, 5));
        originalOps.add(new TextMeasure(403, 5, 0));
        originalOps.add(new TextLookup(404, 11, 0f));
        originalOps.add(new TextLookupInt(405, 11, 2));
        originalOps.add(new IdLookup(406, 11, 0f));
        originalOps.add(new TextSubtext(407, 5, 0, 4));
        originalOps.add(new TextTransform(408, 5, 0, 4, (short) 1));

        // --- Attributes & Paint ---
        originalOps.add(new TextAttribute(500, 5, (short) 1));
        originalOps.add(new ColorAttribute(501, 6, (short) 1));
        originalOps.add(new ImageAttribute(502, 1, (short) 1, null));
        originalOps.add(new TimeAttribute(503, 0, (short) 1, null));
        originalOps.add(new PaintData(new PaintBundle()));

        // --- Layout Components ---
        originalOps.add(new RootLayoutComponent(0, 0, 0, 1000, 1000, null, -1));
        originalOps.add(new LayoutComponentContent(1, 0, 0, 100, 100, null, -1));
        originalOps.add(new BoxLayout(null, 2, -1, 0, 0));
        originalOps.add(new FitBoxLayout(null, 3, -1, 0, 0));
        originalOps.add(new ColumnLayout(null, 4, -1, 0, 0, 0f));
        originalOps.add(new CollapsibleColumnLayout(null, 5, -1, 0, 0, 0f));
        originalOps.add(new RowLayout(null, 6, -1, 0, 0, 0f));
        originalOps.add(new CollapsibleRowLayout(null, 7, -1, 0, 0, 0f));
        originalOps.add(new FlowLayout(null, 8, -1, 0, 0, 0f, 0, 0));
        originalOps.add(new CanvasLayout(null, 9, -1));
        originalOps.add(new CanvasContent(10, 0, 0, 100, 100, null, -1));
        originalOps.add(new CanvasOperations());
        originalOps.add(new ImageLayout(null, 11, -1, 1, 0, 1f));
        originalOps.add(new StateLayout(null, 12, -1, 2, 0, 0, 0, 0));
        originalOps.add(new TextLayout(null, 13, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        originalOps.add(
                new CoreText(
                        null, 14, -1, 5, 0, -1, 16f, 8f, 32f, 0, 400f, -1, 1, 1, 1, 0f, 0f, 1f, 0,
                        0, 0, false, false, null, null, false, 0, -1));
        originalOps.add(
                new TextStyle(
                        15, 0, null, 16f, null, null, 0, null, 0, 0, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null));
        originalOps.add(new ComponentStart(1, 16, 0f, 0f));
        originalOps.add(new ComponentValue(1, 16, 17));
        originalOps.add(new ContainerEnd());

        // --- Modifiers ---
        originalOps.add(new BackgroundModifierOperation(0, 0, 0, 0, 1f, 1f, 1f, 1f, 0));
        originalOps.add(new BorderModifierOperation(0, 0, 0, 0, 1f, 1f, 1f, 1f, 1f, 1f, 0));
        originalOps.add(new PaddingModifierOperation(10, 10, 10, 10));
        originalOps.add(new OffsetModifierOperation(5, 5));
        originalOps.add(new ZIndexModifierOperation(10f));
        originalOps.add(new RippleModifierOperation());
        originalOps.add(new AlignByModifierOperation(8f, (short) 0));
        originalOps.add(new ScrollModifierOperation(1, 0f, 100f, 0f));
        originalOps.add(new MarqueeModifierOperation(1, 1, 1, 1, 1, 1));
        originalOps.add(new ClickModifierOperation());
        originalOps.add(new DrawContentOperation());
        originalOps.add(new ClipRectModifierOperation());
        originalOps.add(new RoundedClipRectModifierOperation(0, 0, 10, 10));
        originalOps.add(new CollapsiblePriorityModifierOperation(1, 10f));
        originalOps.add(new ComponentVisibilityOperation(2));
        originalOps.add(new DimensionConstraintsModifierOperation((byte) 0, 0f, 100f));
        originalOps.add(new GraphicsLayerModifierOperation());
        originalOps.add(new HeightInModifierOperation(0f, 100f));
        originalOps.add(new HeightModifierOperation(DimensionModifierOperation.Type.EXACT, 50f));
        originalOps.add(new WidthInModifierOperation(0f, 100f));
        originalOps.add(new WidthModifierOperation(DimensionModifierOperation.Type.EXACT, 50f));
        originalOps.add(new TouchCancelModifierOperation());
        originalOps.add(new TouchDownModifierOperation());
        originalOps.add(new TouchUpModifierOperation());
        originalOps.add(new CoreSemantics(100, (byte) 1, 101, 102, 1, true, true));

        // --- Actions ---
        originalOps.add(new RunActionOperation());
        originalOps.add(new ValueFloatChangeActionOperation(3, 2.0f));
        originalOps.add(new ValueIntegerChangeActionOperation(2, 100));
        originalOps.add(new ValueStringChangeActionOperation(5, 5));
        originalOps.add(new ValueFloatExpressionChangeActionOperation(3, 8));
        originalOps.add(new ValueIntegerExpressionChangeActionOperation(2, 9));
        originalOps.add(new HostActionOperation(1));
        originalOps.add(new HostActionMetadataOperation(1, 2));
        originalOps.add(new HostNamedActionOperation(1, 2, 3));

        // --- Macros, Loops & Animation ---
        originalOps.add(new PatternDefine(600, new int[] {1}));
        originalOps.add(new PatternArgument(0));
        originalOps.add(new PatternBlock(0));
        originalOps.add(new PatternInflation(600, new int[] {101}));
        originalOps.add(new PatternForEach(11, 601));
        originalOps.add(new ReferencedOperations(602));
        originalOps.add(new IncludeReferencedOperations(602));

        originalOps.add(new LoopOperation(700, 0f, 1f, 10f));
        originalOps.add(new ImpulseOperation(1000f, 0f));
        originalOps.add(new ImpulseProcess());
        originalOps.add(
                new AnimationSpec(
                        1,
                        1000,
                        (short) 1,
                        1000,
                        (short) 1,
                        AnimationSpec.ANIMATION.values()[0],
                        AnimationSpec.ANIMATION.values()[0]));

        // --- Particles ---
        originalOps.add(new ParticlesCreate(800, new int[] {1}, new float[][] {{1}}, 100));
        originalOps.add(new ParticlesLoop(800, new float[] {1}, new float[][] {{1}}));
        originalOps.add(
                new ParticlesCompare(
                        800,
                        (short) 0,
                        0f,
                        1f,
                        new float[] {1},
                        new float[][] {{1}},
                        new float[][] {{1}}));

        // --- Misc ---
        originalOps.add(new ConditionalOperations((byte) 0, 0f, 1f));
        originalOps.add(new HapticFeedback(1));
        originalOps.add(new DebugMessage(100, 1.0f, 0));
        originalOps.add(new Rem("remark"));
        originalOps.add(new WakeIn(10.0f));
        /*
        originalOps.add(
                new Skip(
                        (short) 1,
                        0,
                        100,
                        new SystemInfo() {
                            @Override
                            public int getLibraryApiLevel() {
                                return 1;
                            }

                            @Override
                            public int getProfile() {
                                return 0;
                            }
                        }));

         */
        originalOps.add(new FloatFunctionDefine(900, new int[1]));
        originalOps.add(new FloatFunctionCall(901, new float[1]));
        originalOps.add(
                new TouchExpression(
                        902, new float[0], 0, 0, 100, 0, 0, 0, new float[0], new float[0]));

        // Replace deepCopy with buffer round-trip
        WireBuffer originalWBuffer = new WireBuffer();
        for (Operation op : originalOps) {
            Operation.writeRecursive(op, originalWBuffer);
        }

        originalWBuffer.setIndex(0);
        ArrayList<Operation> copiedOps = new ArrayList<>();
        androidx.compose.remote.core.Operations.UniqueIntMap<CompanionOperation> map =
                Operations.getOperations(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
                                | androidx.compose.remote.core.RcProfiles.PROFILE_EXPERIMENTAL
                                | androidx.compose.remote.core.RcProfiles.PROFILE_DEPRECATED);
        androidx.compose.remote.core.operations.loom.RemapContext ctx =
                androidx.compose.remote.core.operations.loom.RemapContext.identity();

        while (originalWBuffer.available()) {
            int opId = originalWBuffer.readByte();
            CompanionOperation companion = map.get(opId);
            if (companion == null) {
                throw new RuntimeException("Unknown operation " + opId);
            }
            companion.read(ctx.wrap(originalWBuffer), copiedOps);
        }

        WireBuffer copiedWBuffer = new WireBuffer();
        for (Operation op : copiedOps) {
            op.write(copiedWBuffer);
        }

        byte[] originalBytes = originalWBuffer.getBuffer();
        byte[] copiedBytes = copiedWBuffer.getBuffer();

        assertEquals(
                "Buffers should have same size",
                originalWBuffer.getIndex(),
                copiedWBuffer.getIndex());
        for (int i = 0; i < originalWBuffer.getIndex(); i++) {
            assertEquals(
                    "Byte at index " + i + " should match for op at index around " + i,
                    originalBytes[i],
                    copiedBytes[i]);
        }
    }
}
