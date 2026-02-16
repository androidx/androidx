/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.BitmapFontData;
import androidx.compose.remote.core.operations.BitmapTextMeasure;
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
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath;
import androidx.compose.remote.core.operations.DrawBitmapInt;
import androidx.compose.remote.core.operations.DrawBitmapScaled;
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored;
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
import androidx.compose.remote.core.operations.DrawTextOnCircle;
import androidx.compose.remote.core.operations.DrawTextOnPath;
import androidx.compose.remote.core.operations.DrawToBitmap;
import androidx.compose.remote.core.operations.DrawTweenPath;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.FloatFunctionCall;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.FontData;
import androidx.compose.remote.core.operations.HapticFeedback;
import androidx.compose.remote.core.operations.IdLookup;
import androidx.compose.remote.core.operations.ImageAttribute;
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
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.UpdateDynamicFloatList;
import androidx.compose.remote.core.operations.Utils;
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
import androidx.compose.remote.core.operations.layout.managers.ImageLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.layout.managers.StateLayout;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation;
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
import androidx.compose.remote.core.operations.matrix.MatrixConstant;
import androidx.compose.remote.core.operations.matrix.MatrixExpression;
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.types.BooleanConstant;
import androidx.compose.remote.core.types.IntegerConstant;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Override of {@link RemoteComposeBuffer} that supports global optimizations. As the document is
 * written, RecordingRemoteComposeBuffer keeps track of which conditional container each operation
 * is written in (if any) and the operations it depends on, if any. After all operations have been
 * written, {@link #writeToBuffer()} must be called and operations are reordered such that they're
 * in the tightest scope that allows their side effects to be visible everywhere they're referenced.
 *
 * <p>This class makes several assumptions:
 *
 * <ol>
 *   <li>Operations are always written after any dependencies
 *   <li>All operations are needed and written lazily as used, I.e. we don't need to hoist
 *       operations into a tighter scope. NB if we ever wanted to drop this assumption, we can make
 *       idealSpan nullable, allowing us to move operations to a tighter scope and elide unused
 *       operations.
 * </ol>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecordingRemoteComposeBuffer extends RemoteComposeBuffer {
    private Span mSpanTreeRoot;
    private Span mInsertPoint;
    private final Map<Integer, SpanOp> mOperationMap = new HashMap<>();
    private final WireBuffer mTinyBuffer = new WireBuffer(5 * 1024);
    private final DependencyExtractingRemoteContext mDependencyExtractingRemoteContext =
            new DependencyExtractingRemoteContext();

    public RecordingRemoteComposeBuffer(int apiLevel) {
        super(apiLevel);
        mSpanTreeRoot = new Span(null, null, 0, 0);
        mInsertPoint = mSpanTreeRoot;
    }

    public RecordingRemoteComposeBuffer() {
        this(CoreDocument.DOCUMENT_API_LEVEL);
    }

    private static class SpanOp {
        final Operation mOp;
        Span mIdealSpan;
        final List<SpanOp> mDeps = new ArrayList<>();

        SpanOp(@NonNull Operation op, @NonNull Span idealSpan) {
            this.mOp = op;
            this.mIdealSpan = idealSpan;
        }

        /**
         * Records that this {@link SpanOp} is used by {@code span}, and it updates the {@link
         * #mIdealSpan} to the common ancestor span. This is done recursively for any dependencies.
         */
        void recordUsageBySpan(@NonNull Span span) {
            // Find the common ancestor of idealSpan & span
            Span newIdealSpan = findCommonAncestor(mIdealSpan, span);

            if (mIdealSpan != newIdealSpan) {
                mIdealSpan = newIdealSpan;
                // Ensure all dependants are also in idealSpan too
                for (int i = 0; i < mDeps.size(); i++) {
                    mDeps.get(i).recordUsageBySpan(mIdealSpan);
                }
                mIdealSpan.mOperations.add(this);
            }
        }

        void collectDependencies(
                @NonNull DependencyExtractingRemoteContext dependencyExtractingRemoteContext) {
            if (mOp instanceof VariableSupport) {
                dependencyExtractingRemoteContext.setSpanOp(this);
                ((VariableSupport) mOp).registerListening(dependencyExtractingRemoteContext);
            }
        }
    }

    private static class Span {
        final Span mParent;
        final Span mPrev;
        final int mDepth;
        final int mSiblingRank;
        final List<SpanOp> mOperations = new ArrayList<>();
        Span mChild;
        Span mNext;

        Span(@Nullable Span parent, @Nullable Span prev, int depth, int siblingRank) {
            this.mParent = parent;
            this.mPrev = prev;
            this.mDepth = depth;
            this.mSiblingRank = siblingRank;
        }

        void record(@NonNull WireBuffer buffer) {
            for (int i = 0; i < mOperations.size(); i++) {
                SpanOp op = mOperations.get(i);
                if (op.mIdealSpan == this) {
                    op.mOp.write(buffer);
                }
            }
            if (mChild != null) {
                mChild.record(buffer);
            }
            if (mNext != null) {
                mNext.record(buffer);
            }
        }
    }

    @NonNull
    private static Span findCommonAncestor(@NonNull Span a, @NonNull Span b) {
        Span currentA = a;
        Span currentB = b;
        while (currentA != currentB) {
            // Try to equalize depth & siblingRank
            if (currentA.mDepth > currentB.mDepth) {
                currentA = currentA.mParent;
            } else if (currentB.mDepth > currentA.mDepth) {
                currentB = currentB.mParent;
            } else if (currentA.mSiblingRank > currentB.mSiblingRank) {
                currentA = currentA.mPrev;
            } else if (currentB.mSiblingRank > currentA.mSiblingRank) {
                currentB = currentB.mPrev;
            } else {
                currentA = currentA.mParent;
                currentB = currentB.mParent;
            }
        }
        return currentA;
    }

    private void addOperation(@NonNull Operation op, int id) {
        SpanOp spanOp = new SpanOp(op, mInsertPoint);
        mOperationMap.put(id, spanOp);
        spanOp.collectDependencies(mDependencyExtractingRemoteContext);
        mInsertPoint.mOperations.add(spanOp);
    }

    private void addOperation(@NonNull Operation op) {
        SpanOp spanOp = new SpanOp(op, mInsertPoint);
        spanOp.collectDependencies(mDependencyExtractingRemoteContext);
        mInsertPoint.mOperations.add(spanOp);
    }

    private interface OperationBlock {
        void run(WireBuffer buffer, ArrayList<Operation> operations);
    }

    private void addOperation(int id, @NonNull OperationBlock block) {
        ArrayList<Operation> tempOpList = new ArrayList<>();
        mTinyBuffer.reset(0);
        block.run(mTinyBuffer, tempOpList);
        addOperation(tempOpList.get(tempOpList.size() - 1), id);
    }

    private void addOperation(@NonNull OperationBlock block) {
        ArrayList<Operation> tempOpList = new ArrayList<>();
        mTinyBuffer.reset(0);
        block.run(mTinyBuffer, tempOpList);
        addOperation(tempOpList.get(tempOpList.size() - 1));
    }

    /** Writes all operations to the buffer in the optimal order. */
    public void writeToBuffer() {
        mSpanTreeRoot.record(getBuffer());
    }

    @Override
    public void reset(int expectedSize) {
        mSpanTreeRoot = new Span(null, null, 0, 0);
    }

    @Override
    public void addRootContentDescription(int contentDescriptionId) {
        if (contentDescriptionId != 0) {
            addOperation(new RootContentDescription(contentDescriptionId));
        }
    }

    @Override
    public void drawBitmap(
            int imageId,
            int imageWidth,
            int imageHeight,
            int srcLeft,
            int srcTop,
            int srcRight,
            int srcBottom,
            int dstLeft,
            int dstTop,
            int dstRight,
            int dstBottom,
            int contentDescriptionId) {
        addOperation(
                new DrawBitmapInt(
                        imageId,
                        srcLeft,
                        srcTop,
                        srcRight,
                        srcBottom,
                        dstLeft,
                        dstTop,
                        dstRight,
                        dstBottom,
                        contentDescriptionId),
                imageId);
    }

    @Override
    public void mapLookup(int id, int mapId, int strId) {
        addOperation(new DataMapLookup(id, mapId, strId), id);
    }

    @Override
    public void addText(int id, @NonNull String text) {
        addOperation(new TextData(id, text), id);
    }

    @Override
    public void addClickArea(
            int id,
            int contentDescriptionId,
            float left,
            float top,
            float right,
            float bottom,
            int metadataId) {
        addOperation(
                new ClickArea(id, contentDescriptionId, left, top, right, bottom, metadataId), id);
    }

    @Override
    public void setRootContentBehavior(int scroll, int alignment, int sizing, int mode) {
        addOperation(new RootContentBehavior(scroll, alignment, sizing, mode));
    }

    @Override
    public void addDrawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        addOperation(new DrawArc(left, top, right, bottom, startAngle, sweepAngle));
    }

    @Override
    public void addDrawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        addOperation(new DrawSector(left, top, right, bottom, startAngle, sweepAngle));
    }

    @Override
    public void addDrawBitmap(
            int imageId, float left, float top, float right, float bottom,
            int contentDescriptionId) {
        addOperation(new DrawBitmap(imageId, left, top, right, bottom, contentDescriptionId));
    }

    @Override
    public void drawScaledBitmap(
            int imageId,
            float srcLeft,
            float srcTop,
            float srcRight,
            float srcBottom,
            float dstLeft,
            float dstTop,
            float dstRight,
            float dstBottom,
            int scaleType,
            float scaleFactor,
            int contentDescriptionId) {
        addOperation(
                new DrawBitmapScaled(
                        imageId,
                        srcLeft,
                        srcTop,
                        srcRight,
                        srcBottom,
                        dstLeft,
                        dstTop,
                        dstRight,
                        dstBottom,
                        scaleType,
                        scaleFactor,
                        contentDescriptionId));
    }

    @Override
    public int addBitmapFont(int id, BitmapFontData.Glyph @NonNull [] glyphs) {
        addOperation(new BitmapFontData(id, glyphs), id);
        return id;
    }

    @Override
    public int addBitmapFont(
            int id,
            BitmapFontData.Glyph @NonNull [] glyphs,
            @NonNull Map<String, Short> kerningTable) {
        addOperation(new BitmapFontData(id, glyphs, BitmapFontData.VERSION_2, kerningTable), id);
        return id;
    }

    @Override
    public void setBitmapName(int id, @NonNull String name) {
        addOperation(new NamedVariable(id, NamedVariable.IMAGE_TYPE, name));
    }

    @Override
    public void addDrawCircle(float centerX, float centerY, float radius) {
        addOperation(new DrawCircle(centerX, centerY, radius));
    }

    @Override
    public void addDrawLine(float x1, float y1, float x2, float y2) {
        addOperation(new DrawLine(x1, y1, x2, y2));
    }

    @Override
    public void addDrawOval(float left, float top, float right, float bottom) {
        addOperation(new DrawOval(left, top, right, bottom));
    }

    @Override
    public int pathTween(int out, int pid1, int pid2, float tween) {
        addOperation(new PathTween(out, pid1, pid2, tween));
        return out;
    }

    @Override
    public int pathCreate(int out, float x, float y) {
        addOperation(new PathCreate(out, x, y), out);
        return out;
    }

    @Override
    public void pathAppend(int id, float @NonNull ... path) {
        addOperation(new PathAppend(id, path));
    }

    @Override
    public void addDrawPath(int pathId) {
        addOperation(new DrawPath(pathId));
    }

    @Override
    public void addDrawRect(float left, float top, float right, float bottom) {
        addOperation(new DrawRect(left, top, right, bottom));
    }

    @Override
    public void addDrawRoundRect(
            float left, float top, float right, float bottom, float radiusX, float radiusY) {
        addOperation(new DrawRoundRect(left, top, right, bottom, radiusX, radiusY));
    }

    @Override
    public void addDrawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
        addOperation(new DrawTextOnPath(textId, pathId, hOffset, vOffset));
    }

    @Override
    // Can't use jSpecify here due to: error scoping construct cannot be annotated with type-use
    // annotation
    @SuppressWarnings("JSpecifyNullness")
    public void addDrawTextOnCircle(
            int textId,
            float centerX,
            float centerY,
            float radius,
            float startAngle,
            float warpRadiusOffset,
            @androidx.annotation.NonNull DrawTextOnCircle.Alignment alignment,
            @androidx.annotation.NonNull DrawTextOnCircle.Placement placement) {
        addOperation(
                new DrawTextOnCircle(
                        textId,
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        warpRadiusOffset,
                        alignment,
                        placement));
    }

    @Override
    public void addDrawTextRun(
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {
        addOperation(new DrawText(textId, start, end, contextStart, contextEnd, x, y, rtl));
    }

    @Override
    public void addDrawBitmapFontTextRun(
            int textId, int bitmapFontId, int start, int end, float x, float y,
            float glyphSpacing) {
        addOperation(new DrawBitmapFontText(textId, bitmapFontId, start, end, x, y, glyphSpacing));
    }

    @Override
    public void addDrawBitmapFontTextRunOnPath(
            int textId, int bitmapFontId, int pathId, int start, int end, float yAdj,
            float glyphSpacing) {
        addOperation(
                new DrawBitmapFontTextOnPath(
                        textId, bitmapFontId, pathId, start, end, yAdj, glyphSpacing));
    }

    @Override
    public void drawBitmapTextAnchored(
            int textId,
            int bitmapFontId,
            float start,
            float end,
            float x,
            float y,
            float panX,
            float panY,
            float glyphSpacing) {
        addOperation(
                new DrawBitmapTextAnchored(
                        textId, bitmapFontId, start, end, x, y, panX, panY, glyphSpacing));
    }

    @Override
    public int textMerge(int textId, int id1, int id2) {
        addOperation(new TextMerge(textId, id1, id2), textId);
        return textId;
    }

    @Override
    public int createTextFromFloat(
            int id, float value, short digitsBefore, short digitsAfter, int flags) {
        addOperation(new TextFromFloat(id, value, digitsBefore, digitsAfter, flags), id);
        return id;
    }

    @Override
    public void drawTextAnchored(int textId, float x, float y, float panX, float panY, int flags) {
        addOperation(new DrawTextAnchored(textId, x, y, panX, panY, flags));
    }

    @Override
    public void addDrawTweenPath(int path1Id, int path2Id, float tween, float start, float stop) {
        addOperation(new DrawTweenPath(path1Id, path2Id, tween, start, stop));
    }

    @Override
    public int addPathData(int id, float @NonNull [] pathData) {
        int winding = id >> 24;
        addOperation(new PathData(id, pathData, winding), id);
        return id;
    }

    @Override
    public int addPathData(int id, float @NonNull [] pathData, int winding) {
        addOperation(new PathData(id, pathData, winding), id);
        return id;
    }

    @Override
    public void addPaint(@NonNull PaintBundle paint) {
        addOperation(new PaintData(paint));
    }

    @Override
    public void setTheme(int theme) {
        addOperation(new Theme(theme));
    }

    @Override
    public void addMatrixSkew(float skewX, float skewY) {
        addOperation(new MatrixSkew(skewX, skewY));
    }

    @Override
    public void addMatrixRestore() {
        addOperation(new MatrixRestore());
    }

    @Override
    public void addMatrixSave() {
        addOperation(new MatrixSave());
    }

    @Override
    public void addMatrixRotate(float angle, float centerX, float centerY) {
        addOperation(new MatrixRotate(angle, centerX, centerY));
    }

    @Override
    public void addMatrixTranslate(float dx, float dy) {
        addOperation(new MatrixTranslate(dx, dy));
    }

    @Override
    public void addMatrixScale(float scaleX, float scaleY) {
        addOperation(new MatrixScale(scaleX, scaleY, Float.NaN, Float.NaN));
    }

    @Override
    public void addMatrixScale(float scaleX, float scaleY, float centerX, float centerY) {
        addOperation(new MatrixScale(scaleX, scaleY, centerX, centerY));
    }

    @Override
    public void addClipPath(int pathId) {
        int id = pathId & 0xFFFFF;
        int regionOp = pathId >> 24;
        addOperation(new ClipPath(id, regionOp));
    }

    @Override
    public void addClipRect(float left, float top, float right, float bottom) {
        addOperation(new ClipRect(left, top, right, bottom));
    }

    @Override
    public float addFloat(int id, float value) {
        addOperation(new FloatConstant(id, value), id);
        return Utils.asNan(id);
    }

    @Override
    public void addInteger(int id, int value) {
        addOperation(new IntegerConstant(id, value), id);
    }

    @Override
    public void addLong(int id, long value) {
        addOperation(new LongConstant(id, value), id);
    }

    @Override
    public void addBoolean(int id, boolean value) {
        addOperation(new BooleanConstant(id, value), id);
    }

    @Override
    public void addAnimatedFloat(int id, float @NonNull ... value) {
        addOperation(new FloatExpression(id, value, null), id);
    }

    @Override
    public void addAnimatedFloat(int id, float @NonNull [] value, float @Nullable [] animation) {
        addOperation(new FloatExpression(id, value, animation), id);
    }

    @Override
    public void addTouchExpression(
            int id,
            float value,
            float min,
            float max,
            float velocityId,
            int touchEffects,
            float @NonNull [] exp,
            int touchMode,
            float @Nullable [] touchSpec,
            float @Nullable [] easingSpec) {
        addOperation(
                id,
                (buffer, operations) -> {
                    TouchExpression.apply(
                            buffer,
                            id,
                            value,
                            min,
                            max,
                            velocityId,
                            touchEffects,
                            exp,
                            touchMode,
                            touchSpec,
                            easingSpec);
                    buffer.setIndex(0);
                    TouchExpression.read(buffer, operations);
                });
    }

    @Override
    public void textMeasure(int id, int textId, int mode) {
        addOperation(new TextMeasure(id, textId, mode), id);
    }

    @Override
    public void textLength(int id, int textId) {
        addOperation(new TextLength(id, textId), id);
    }

    @Override
    public void addFloatArray(int id, float @NonNull [] values) {
        addOperation(new DataListFloat(id, values), id);
    }

    @Override
    public void addDynamicFloatArray(int id, float size) {
        addOperation(new DataDynamicListFloat(id, size), id);
    }

    @Override
    public void setArrayValue(int id, float index, float value) {
        addOperation(new UpdateDynamicFloatList(id, index, value), id);
    }

    @Override
    public void addList(int id, int @NonNull [] listId) {
        // Force the DataListIds and dependents to be in the mSpanTreeRoot because the rust player
        // doesn't properly support them inside conditional nodes. See b/465085573.
        SpanOp spanOp = new SpanOp(new DataListIds(id, listId), mSpanTreeRoot);
        mOperationMap.put(id, spanOp);

        // Override mInsertPoint to ensure recordDependency books the deps as being needed by the
        // root span.
        Span previousInsertPoint = mInsertPoint;
        mInsertPoint = mSpanTreeRoot;

        mDependencyExtractingRemoteContext.setSpanOp(spanOp);
        for (int id2 : listId) {
            mDependencyExtractingRemoteContext.recordDependency(id2);
        }

        mInsertPoint = previousInsertPoint;

        mSpanTreeRoot.mOperations.add(spanOp);
    }

    @Override
    public void addMap(
            int id, @NonNull String[] keys, byte @Nullable [] types, int @NonNull [] listId) {
        addOperation(new DataMapIds(id, keys, types, listId), id);
    }

    @Override
    public void textLookup(int id, float dataSet, float index) {
        addOperation(new TextLookup(id, Utils.idFromNan(dataSet), index), id);
    }

    @Override
    public void idLookup(int id, float dataSet, float index) {
        addOperation(new IdLookup(id, Utils.idFromNan(dataSet), index), id);
    }

    @Override
    public void textLookup(int id, float dataSet, int index) {
        addOperation(new TextLookupInt(id, Utils.idFromNan(dataSet), index), id);
    }

    @Override
    public void addIntegerExpression(int id, int mask, int @NonNull [] value) {
        addOperation(new IntegerExpression(id, mask, value), id);
    }

    @Override
    public void addColor(int id, int color) {
        addOperation(new ColorConstant(id, color), id);
    }

    @Override
    public void addColorExpression(int id, int color1, int color2, float tween) {
        addOperation(new ColorExpression(id, 0, color1, color2, tween), id);
    }

    @Override
    public void addColorExpression(int id, short color1, int color2, float tween) {
        addOperation(new ColorExpression(id, 1, color1, color2, tween), id);
    }

    @Override
    public void addColorExpression(int id, int color1, short color2, float tween) {
        addOperation(new ColorExpression(id, 2, color1, color2, tween), id);
    }

    @Override
    public void addColorExpression(int id, short color1, short color2, float tween) {
        addOperation(new ColorExpression(id, 3, color1, color2, tween), id);
    }

    @Override
    public void addColorExpression(int id, float hue, float sat, float value) {
        addOperation(new ColorExpression(id, hue, sat, value), id);
    }

    @Override
    public void addColorExpression(int id, int alpha, float hue, float sat, float value) {
        addOperation(new ColorExpression(id, ColorExpression.HSV_MODE, alpha, hue, sat, value), id);
    }

    @Override
    public void addColorExpression(int id, float alpha, float red, float green, float blue) {
        addOperation(
                new ColorExpression(id, ColorExpression.ARGB_MODE, alpha, red, green, blue), id);
    }

    @Override
    public void setNamedVariable(int id, @NonNull String name, int type) {
        addOperation(new NamedVariable(id, type, name), id);
    }

    @Override
    public void addComponentStart(int type, int id) {
        mLastComponentId = getComponentId(id);
        addOperation(new ComponentStart(type, mLastComponentId, 0f, 0f));
    }

    @Override
    public void addContainerEnd() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void addModifierScroll(int direction, float max) {
        addOperation(new ScrollModifierOperation(direction, 0f, max, 0f));
        addOperation(new ContainerEnd());
    }

    @Override
    public void addModifierBackground(int color, int shape) {
        float r = (color >> 16 & 0xff) / 255.0f;
        float g = (color >> 8 & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        float a = (color >> 24 & 0xff) / 255.0f;
        addOperation(new BackgroundModifierOperation(0, 0, 0, 0, r, g, b, a, shape));
    }

    @Override
    public void addModifierBackground(float r, float g, float b, float a, int shape) {
        addOperation(new BackgroundModifierOperation(0, 0, 0, 0, r, g, b, a, shape));
    }

    @Override
    public void addModifierAlignBy(float line) {
        addOperation(new AlignByModifierOperation(line, 0));
    }

    @Override
    public void addModifierBorder(
            float borderWidth, float borderRoundedCorner, int color, int shape) {
        float r = (color >> 16 & 0xff) / 255.0f;
        float g = (color >> 8 & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        float a = (color >> 24 & 0xff) / 255.0f;
        addOperation(
                new BorderModifierOperation(
                        0, 0, 0, 0, borderWidth, borderRoundedCorner, r, g, b, a, shape));
    }

    @Override
    public void addModifierPadding(float left, float top, float right, float bottom) {
        addOperation(new PaddingModifierOperation(left, top, right, bottom));
    }

    @Override
    public void addModifierOffset(float x, float y) {
        addOperation(new OffsetModifierOperation(x, y));
    }

    @Override
    public void addModifierZIndex(float value) {
        addOperation(new ZIndexModifierOperation(value));
    }

    @Override
    public void addModifierRipple() {
        addOperation(new RippleModifierOperation());
    }

    @Override
    public void addModifierMarquee(
            int iterations,
            int animationMode,
            float repeatDelayMillis,
            float initialDelayMillis,
            float spacing,
            float velocity) {
        addOperation(
                new MarqueeModifierOperation(
                        iterations,
                        animationMode,
                        repeatDelayMillis,
                        initialDelayMillis,
                        spacing,
                        velocity));
    }

    @Override
    public void addModifierGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {
        addOperation(
                (buffer, operations) -> {
                    GraphicsLayerModifierOperation.apply(buffer, attributes);
                    buffer.setIndex(0);
                    GraphicsLayerModifierOperation.read(buffer, operations);
                });
    }

    @Override
    public void addRoundClipRectModifier(
            float topStart, float topEnd, float bottomStart, float bottomEnd) {
        addOperation(
                new RoundedClipRectModifierOperation(topStart, topEnd, bottomStart, bottomEnd));
    }

    @Override
    public void addClipRectModifier() {
        addOperation(new ClipRectModifierOperation());
    }

    @Override
    public void addLoopStart(int indexId, float from, float step, float until) {
        addOperation(new LoopOperation(indexId, from, step, until));
    }

    @Override
    public void addLoopEnd() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void addStateLayout(
            int componentId, int animationId, int horizontal, int vertical, int indexId) {
        mLastComponentId = getComponentId(componentId);
        addOperation(new StateLayout(mLastComponentId, animationId, horizontal, vertical, indexId));
    }

    @Override
    public void addBoxStart(int componentId, int animationId, int horizontal, int vertical) {
        mLastComponentId = getComponentId(componentId);
        addOperation(new BoxLayout(null, mLastComponentId, animationId, horizontal, vertical));
    }

    @Override
    public void addFitBoxStart(int componentId, int animationId, int horizontal, int vertical) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new FitBoxLayout(null, mLastComponentId, animationId, horizontal, vertical));
    }

    @Override
    public void addImage(
            int componentId, int animationId, int bitmapId, int scaleType, float alpha) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new ImageLayout(null, componentId, animationId, bitmapId, scaleType, alpha));
    }

    @Override
    public void addRowStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new RowLayout(null, mLastComponentId, animationId, horizontal, vertical, spacedBy));
    }

    @Override
    public void addCollapsibleRowStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new CollapsibleRowLayout(
                        null, mLastComponentId, animationId, horizontal, vertical, spacedBy));
    }

    @Override
    public void addColumnStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new ColumnLayout(
                        null, mLastComponentId, animationId, horizontal, vertical, spacedBy));
    }

    @Override
    public void addCollapsibleColumnStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new CollapsibleColumnLayout(
                        null, mLastComponentId, animationId, horizontal, vertical, spacedBy));
    }

    @Override
    public void addCanvasStart(int componentId, int animationId) {
        mLastComponentId = getComponentId(componentId);
        addOperation(new CanvasLayout(null, mLastComponentId, animationId));
    }

    @Override
    public void addCanvasContentStart(int componentId) {
        mLastComponentId = getComponentId(componentId);
        addOperation(new CanvasContent(mLastComponentId));
    }

    @Override
    public void addRootStart() {
        mLastComponentId = getComponentId(-1);
        addOperation(new RootLayoutComponent(mLastComponentId));
    }

    @Override
    public void addContentStart() {
        mLastComponentId = getComponentId(-1);
        addOperation(new LayoutComponentContent(mLastComponentId));
    }

    @Override
    public void addCanvasOperationsStart() {
        addOperation(new CanvasOperations());
    }

    @Override
    public void addRunActionsStart() {
        addOperation(new RunActionOperation());
    }

    @Override
    public void addComponentWidthValue(int id) {
        addOperation(new ComponentValue(ComponentValue.WIDTH, mLastComponentId, id));
    }

    @Override
    public void addComponentHeightValue(int id) {
        addOperation(new ComponentValue(ComponentValue.HEIGHT, mLastComponentId, id));
    }

    @Override
    public void addTextComponentStart(
            int componentId,
            int animationId,
            int textId,
            int color,
            float fontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            short flags,
            short textAlign,
            int overflow,
            int maxLines) {
        mLastComponentId = getComponentId(componentId);
        int flagsAndTextAlign = (flags << 16) | (textAlign & 0xFFFF);

        addOperation(
                new TextLayout(
                        null,
                        mLastComponentId,
                        animationId,
                        textId,
                        color,
                        fontSize,
                        fontStyle,
                        fontWeight,
                        fontFamilyId,
                        flagsAndTextAlign,
                        overflow,
                        maxLines));
    }

    @Override
    public void addTextComponentStart(
            int componentId,
            int animationId,
            int textId,
            int color,
            int colorId,
            float fontSize,
            float minFontSize,
            float maxFontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            int textAlign,
            int overflow,
            int maxLines,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean underline,
            boolean strikethrough,
            int @NonNull [] fontAxis,
            float @NonNull [] fontAxisValues,
            boolean autosize,
            int flags) {
        mLastComponentId = getComponentId(componentId);
        addOperation(
                new CoreText(
                        null,
                        mLastComponentId,
                        animationId,
                        textId,
                        color,
                        colorId,
                        fontSize,
                        minFontSize,
                        maxFontSize,
                        fontStyle,
                        fontWeight,
                        fontFamilyId,
                        textAlign,
                        overflow,
                        maxLines,
                        letterSpacing,
                        lineHeightAdd,
                        lineHeightMultiplier,
                        lineBreakStrategy,
                        hyphenationFrequency,
                        justificationMode,
                        underline,
                        strikethrough,
                        fontAxis,
                        fontAxisValues,
                        autosize,
                        flags));
    }

    @Override
    public void addImpulse(float duration, float start) {
        addOperation(new ImpulseOperation(duration, start));
    }

    @Override
    public void addImpulseProcess() {
        addOperation(new ImpulseProcess());
    }

    @Override
    public void addImpulseEnd() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void addParticles(
            int id,
            int @NonNull [] varIds,
            float @NonNull [] @NonNull [] initialExpressions,
            int particleCount) {
        addOperation(new ParticlesCreate(id, varIds, initialExpressions, particleCount));
    }

    @Override
    public void addParticlesLoop(
            int id, float @Nullable [] restart, float @NonNull [][] expressions) {
        addOperation(new ParticlesLoop(id, restart, expressions));
    }

    @Override
    public void addParticlesComparison(
            int id,
            short flags,
            float min,
            float max,
            float @Nullable [] condition,
            float @Nullable [][] apply1,
            float @Nullable [][] apply2) {
        addOperation(new ParticlesCompare(id, flags, min, max, condition, apply1, apply2));
    }

    @Override
    public void addParticleLoopEnd() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void defineFloatFunction(int fid, int @NonNull [] args) {
        addOperation(new FloatFunctionDefine(fid, args), fid);
    }

    @Override
    public void addEndFloatFunctionDef() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void callFloatFunction(int id, float @Nullable [] args) {
        addOperation(new FloatFunctionCall(id, args), id);
    }

    @Override
    public void bitmapAttribute(int id, int bitmapId, short attribute) {
        addOperation(new ImageAttribute(id, bitmapId, attribute, null), id);
    }

    @Override
    public void textAttribute(int id, int textId, short attribute) {
        addOperation(new TextAttribute(id, textId, attribute), id);
    }

    @Override
    public void timeAttribute(int id, int timeId, short attribute, int @Nullable ... args) {
        addOperation(new TimeAttribute(id, timeId, attribute, args), id);
    }

    @Override
    public void drawComponentContent() {
        addOperation(new DrawContent());
    }

    @Override
    public int storeBitmap(int imageId, int imageWidth, int imageHeight, byte @NonNull [] data) {
        addOperation(new BitmapData(imageId, imageWidth, imageHeight, data));
        return imageId;
    }

    @Override
    public int createBitmap(int imageId, short imageWidth, short imageHeight) {
        addOperation(
                new BitmapData(
                        imageId,
                        BitmapData.TYPE_RAW8888,
                        imageWidth,
                        BitmapData.ENCODING_EMPTY,
                        imageHeight,
                        new byte[0]));
        return imageId;
    }

    @Override
    public void drawOnBitmap(int imageId, int mode, int color) {
        addOperation(new DrawToBitmap(imageId, mode, color));
    }

    @Override
    public int storeBitmapA8(int imageId, int imageWidth, int imageHeight, byte @NonNull [] data) {
        addOperation(
                new BitmapData(
                        imageId,
                        BitmapData.TYPE_PNG_ALPHA_8,
                        (short) imageWidth,
                        BitmapData.ENCODING_INLINE,
                        (short) imageHeight,
                        data));
        return imageId;
    }

    @Override
    public int storeBitmapUrl(int imageId, @NonNull String url) {
        addOperation(
                new BitmapData(
                        imageId,
                        BitmapData.TYPE_PNG,
                        (short) 1,
                        BitmapData.ENCODING_URL,
                        (short) 1,
                        url.getBytes(StandardCharsets.UTF_8)));
        return imageId;
    }

    @Override
    public void pathCombine(int id, int path1, int path2, byte op) {
        addOperation(new PathCombine(id, path1, path2, op), id);
    }

    @Override
    public void performHaptic(int feedbackConstant) {
        addOperation(new HapticFeedback(feedbackConstant));
    }

    @Override
    public void addConditionalOperations(byte type, float a, float b) {
        Span child = new Span(mInsertPoint, null, mInsertPoint.mDepth + 1, 0);
        mInsertPoint.mChild = child;
        mInsertPoint = child;
        addOperation(new ConditionalOperations(type, a, b));
    }

    @Override
    public void endConditionalOperations() {
        addOperation(new ContainerEnd());
        mInsertPoint = mInsertPoint.mParent;
        Span next =
                new Span(
                        mInsertPoint.mParent,
                        mInsertPoint,
                        mInsertPoint.mDepth,
                        mInsertPoint.mSiblingRank + 1);
        mInsertPoint.mNext = next;
        mInsertPoint = next;
    }

    @Override
    public void addDebugMessage(int textId, float value, int flags) {
        addOperation(new DebugMessage(textId, value, flags));
    }

    @Override
    public void getColorAttribute(int id, int baseColor, short type) {
        addOperation(new ColorAttribute(id, baseColor, type), id);
    }

    @Override
    public void setMatrixFromPath(int pathId, float fraction, float vOffset, int flags) {
        addOperation(new MatrixFromPath(pathId, fraction, vOffset, flags));
    }

    @Override
    public void textSubtext(int id, int txtId, float start, float len) {
        addOperation(new TextSubtext(id, txtId, start, len), id);
    }

    @Override
    public void bitmapTextMeasure(int id, int textId, int bmFontId, int type, float glyphSpacing) {
        addOperation(new BitmapTextMeasure(id, textId, bmFontId, type, glyphSpacing), id);
    }

    @Override
    public void rem(@NonNull String text) {
        addOperation(new Rem(text));
    }

    @Override
    public void setVersion(int documentApiLevel, int profiles) {
        throw new UnsupportedOperationException("setVersion is not supported");
    }

    @Override
    public void setVersion(int documentApiLevel, int operationsProfiles,
            @NonNull Set<Integer> supportedOperations) {
        throw new UnsupportedOperationException("setVersion is not supported");
    }

    @Override
    public void addMatrixConst(int id, float @NonNull [] values) {
        addOperation(new MatrixConstant(id, 0, values), id);
    }

    @Override
    public void addMatrixExpression(int id, float @NonNull [] exp) {
        addOperation(new MatrixExpression(id, 0, exp), id);
    }

    @Override
    public void addMatrixVectorMath(
            float matrixId, short type, float @NonNull [] from, int @NonNull [] outId) {
        addOperation(new MatrixVectorMath(type, outId, Utils.idFromNan(matrixId), from));
    }

    @Override
    public void addFont(int id, int type, byte @NonNull [] data) {
        addOperation(new FontData(id, type, data));
    }

    @Override
    public void wakeIn(float seconds) {
        addOperation(new WakeIn(seconds));
    }

    @Override
    public void addPathExpression(
            int id,
            float @NonNull [] expressionX,
            float @Nullable [] expressionY,
            float start,
            float end,
            float count,
            int flags) {
        addOperation(
                new PathExpression(id, expressionX, expressionY, start, end, count, flags), id);
    }

    @Override
    public void addComponentVisibilityOperation(int valueId) {
        addOperation(new ComponentVisibilityOperation(valueId));
    }

    @Override
    public void addWidthModifierOperation(int type, float value) {
        addOperation(
                new WidthModifierOperation(DimensionModifierOperation.Type.fromInt(type), value));
    }

    @Override
    public void addHeightModifierOperation(int type, float value) {
        addOperation(
                new HeightModifierOperation(DimensionModifierOperation.Type.fromInt(type), value));
    }

    @Override
    public void addHeightInModifierOperation(float min, float max) {
        addOperation(new HeightInModifierOperation(min, max));
    }

    @Override
    public void addTouchDownModifierOperation() {
        addOperation(new TouchDownModifierOperation());
    }

    @Override
    public void addTouchUpModifierOperation() {
        addOperation(new TouchUpModifierOperation());
    }

    @Override
    public void addTouchCancelModifierOperation() {
        addOperation(new TouchCancelModifierOperation());
    }

    @Override
    public void addWidthInModifierOperation(float min, float max) {
        addOperation(new WidthInModifierOperation(min, max));
    }

    @Override
    public void addDrawContentOperation() {
        addOperation(new DrawContentOperation());
    }

    @Override
    public void startLayoutCompute(int type, int boundsId, boolean animateChanges) {
        addOperation(new LayoutComputeOperation(type, boundsId, animateChanges));
    }

    @Override
    public void endLayoutCompute() {
        addOperation(new ContainerEnd());
    }

    @Override
    public void addSemanticsModifier(
            int contentDescriptionId,
            byte role,
            int textId,
            int stateDescriptionId,
            int mode,
            boolean enabled,
            boolean clickable) {
        addOperation(
                new CoreSemantics(
                        contentDescriptionId,
                        role,
                        textId,
                        stateDescriptionId,
                        mode,
                        enabled,
                        clickable));
    }

    @Override
    public void addClickModifierOperation() {
        addOperation(new ClickModifierOperation());
    }

    @Override
    public void addCollapsiblePriorityModifier(int orientation, float priority) {
        addOperation(new CollapsiblePriorityModifierOperation(orientation, priority));
    }

    @Override
    public void addAnimationSpecModifier(
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            int enterAnimation,
            int exitAnimation) {
        addOperation(
                new AnimationSpec(
                        animationId,
                        motionDuration,
                        motionEasingType,
                        visibilityDuration,
                        visibilityEasingType,
                        AnimationSpec.intToAnimation(enterAnimation),
                        AnimationSpec.intToAnimation(exitAnimation)));
    }

    @Override
    public void addValueStringChangeActionOperation(int destTextId, int srcTextId) {
        addOperation(new ValueStringChangeActionOperation(destTextId, srcTextId));
    }

    @Override
    public void addValueIntegerExpressionChangeActionOperation(
            long destIntegerId, long srcIntegerId) {
        addOperation(new ValueIntegerExpressionChangeActionOperation(destIntegerId, srcIntegerId));
    }

    @Override
    public void addValueFloatChangeActionOperation(int valueId, float value) {
        addOperation(new ValueFloatChangeActionOperation(valueId, value));
    }

    @Override
    public void addValueIntegerChangeActionOperation(int valueId, int value) {
        addOperation(new ValueIntegerChangeActionOperation(valueId, value));
    }

    @Override
    public void addValueFloatExpressionChangeActionOperation(int mValueId, int mValue) {
        addOperation(new ValueFloatExpressionChangeActionOperation(mValueId, mValue));
    }

    /** Used to extract the dependencies from {@link VariableSupport#registerListening}. */
    private class DependencyExtractingRemoteContext extends RemoteContext {
        private SpanOp mSpanOp;

        public void setSpanOp(SpanOp spanOp) {
            mSpanOp = spanOp;
        }

        public void recordDependency(int id) {
            // We can expect this lookup to fail for time related IDs.
            SpanOp dep = mOperationMap.get(id);
            if (dep == null) {
                return;
            }
            dep.recordUsageBySpan(mInsertPoint);
            mSpanOp.mDeps.add(dep);
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public float[] getPathData(int instanceId) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadColor(int id, int color) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedLong(@NonNull String name, long value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        @Nullable
        public DataMap getDataMap(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void runNamedAction(int id, @Nullable Object value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void putObject(int id, @NonNull Object value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        @Nullable
        public Object getObject(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void hapticEffect(int type) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadText(int id, @NonNull String text) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        @Nullable
        public String getText(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadFloat(int id, float value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void overrideFloat(int id, float value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadInteger(int id, int value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void overrideInteger(int id, int value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void overrideText(int id, int valueId) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public float getFloat(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public int getInteger(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public long getLong(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public int getColor(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {
            recordDependency(id);
        }

        @Override
        public int updateOps() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        @Nullable
        public ShaderData getShader(int id) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void addClickArea(
                int id,
                int contentDescriptionId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
