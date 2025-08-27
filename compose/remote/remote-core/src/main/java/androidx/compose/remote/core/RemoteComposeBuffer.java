/*
 * Copyright (C) 2023 The Android Open Source Project
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
import androidx.compose.remote.core.operations.DrawTextOnPath;
import androidx.compose.remote.core.operations.DrawToBitmap;
import androidx.compose.remote.core.operations.DrawTweenPath;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.FloatFunctionCall;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.FontData;
import androidx.compose.remote.core.operations.HapticFeedback;
import androidx.compose.remote.core.operations.Header;
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
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout;
import androidx.compose.remote.core.operations.layout.managers.ImageLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.layout.managers.StateLayout;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
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
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.types.BooleanConstant;
import androidx.compose.remote.core.types.IntegerConstant;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Provides an abstract buffer to encode/decode RemoteCompose operations */
public class RemoteComposeBuffer {
    public static final int EASING_CUBIC_STANDARD = FloatAnimation.CUBIC_STANDARD;
    public static final int EASING_CUBIC_ACCELERATE = FloatAnimation.CUBIC_ACCELERATE;
    public static final int EASING_CUBIC_DECELERATE = FloatAnimation.CUBIC_DECELERATE;
    public static final int EASING_CUBIC_LINEAR = FloatAnimation.CUBIC_LINEAR;
    public static final int EASING_CUBIC_ANTICIPATE = FloatAnimation.CUBIC_ANTICIPATE;
    public static final int EASING_CUBIC_OVERSHOOT = FloatAnimation.CUBIC_OVERSHOOT;
    public static final int EASING_CUBIC_CUSTOM = FloatAnimation.CUBIC_CUSTOM;
    public static final int EASING_SPLINE_CUSTOM = FloatAnimation.SPLINE_CUSTOM;
    public static final int EASING_EASE_OUT_BOUNCE = FloatAnimation.EASE_OUT_BOUNCE;
    public static final int EASING_EASE_OUT_ELASTIC = FloatAnimation.EASE_OUT_ELASTIC;
    private @NonNull WireBuffer mBuffer = new WireBuffer();
    private static final boolean DEBUG = false;

    private int mLastComponentId = 0;
    private int mGeneratedComponentId = -1;
    private int mApiLevel = CoreDocument.DOCUMENT_API_LEVEL;

    Operations.UniqueIntMap<CompanionOperation> mMap = new Operations.UniqueIntMap<>();

    public RemoteComposeBuffer() {
        // nothing
    }

    public RemoteComposeBuffer(int apiLevel) {
        mApiLevel = apiLevel;
    }

    /**
     * Reset the internal buffers
     *
     * @param expectedSize provided hint for the main buffer size
     */
    public void reset(int expectedSize) {
        mBuffer.reset(expectedSize);
        mLastComponentId = 0;
        mGeneratedComponentId = -1;
    }

    public int getLastComponentId() {
        return mLastComponentId;
    }

    public @NonNull WireBuffer getBuffer() {
        return mBuffer;
    }

    public void setBuffer(@NonNull WireBuffer buffer) {
        this.mBuffer = buffer;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Supported operations on the buffer
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /** Insert a header */
    public void addHeader(short @NonNull [] tags, Object @NonNull [] values) {
        Header.apply(mBuffer, mApiLevel, tags, values);
    }

    /**
     * Insert a header
     *
     * @param width the width of the document in pixels
     * @param height the height of the document in pixels
     * @param density the density of the document in pixels per device pixel
     * @param capabilities bitmask indicating needed capabilities (unused for now)
     */
    public void header(int width, int height, float density, long capabilities) {
        Header.apply(mBuffer, width, height, density, capabilities);
    }

    /**
     * Add a root content description
     *
     * @param contentDescriptionId the content description text
     */
    public void addRootContentDescription(int contentDescriptionId) {
        if (contentDescriptionId != 0) {
            RootContentDescription.apply(mBuffer, contentDescriptionId);
        }
    }

    /**
     * Insert a bitmap
     *
     * @param imageId an image that we'll add to the buffer
     * @param imageWidth the width of the image
     * @param imageHeight the height of the image
     * @param srcLeft left coordinate of the source area
     * @param srcTop top coordinate of the source area
     * @param srcRight right coordinate of the source area
     * @param srcBottom bottom coordinate of the source area
     * @param dstLeft left coordinate of the destination area
     * @param dstTop top coordinate of the destination area
     * @param dstRight right coordinate of the destination area
     * @param dstBottom bottom coordinate of the destination area
     * @param contentDescriptionId the content description of the image
     */
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
        DrawBitmapInt.apply(
                mBuffer,
                imageId,
                srcLeft,
                srcTop,
                srcRight,
                srcBottom,
                dstLeft,
                dstTop,
                dstRight,
                dstBottom,
                contentDescriptionId);
    }

    /**
     * look up map and return the id of the object looked up
     *
     * @param id the id of the data
     * @param mapId the map to access
     * @param strId the string to lookup
     */
    public void mapLookup(int id, int mapId, int strId) {
        DataMapLookup.apply(mBuffer, id, mapId, strId);
    }

    /**
     * Add a text data for the id
     *
     * @param id id of the data
     * @param text text saved as data
     */
    public void addText(int id, @NonNull String text) {
        TextData.apply(mBuffer, id, text);
    }

    /**
     * Add a click area to the document
     *
     * @param id the id of the click area, reported in the click listener callback
     * @param contentDescriptionId the content description of that click area (accessibility)
     * @param left left coordinate of the area bounds
     * @param top top coordinate of the area bounds
     * @param right right coordinate of the area bounds
     * @param bottom bottom coordinate of the area bounds
     * @param metadataId associated metadata, user-provided
     */
    public void addClickArea(
            int id,
            int contentDescriptionId,
            float left,
            float top,
            float right,
            float bottom,
            int metadataId) {
        ClickArea.apply(mBuffer, id, contentDescriptionId, left, top, right, bottom, metadataId);
    }

    /**
     * Sets the way the player handles the content
     *
     * @param scroll set the horizontal behavior (NONE|SCROLL_HORIZONTAL|SCROLL_VERTICAL)
     * @param alignment set the alignment of the content (TOP|CENTER|BOTTOM|START|END)
     * @param sizing set the type of sizing for the content (NONE|SIZING_LAYOUT|SIZING_SCALE)
     * @param mode set the mode of sizing, either LAYOUT modes or SCALE modes the LAYOUT modes are:
     *     - LAYOUT_MATCH_PARENT - LAYOUT_WRAP_CONTENT or adding an horizontal mode and a vertical
     *     mode: - LAYOUT_HORIZONTAL_MATCH_PARENT - LAYOUT_HORIZONTAL_WRAP_CONTENT -
     *     LAYOUT_HORIZONTAL_FIXED - LAYOUT_VERTICAL_MATCH_PARENT - LAYOUT_VERTICAL_WRAP_CONTENT -
     *     LAYOUT_VERTICAL_FIXED The LAYOUT_*_FIXED modes will use the intrinsic document size
     */
    public void setRootContentBehavior(int scroll, int alignment, int sizing, int mode) {
        RootContentBehavior.apply(mBuffer, scroll, alignment, sizing, mode);
    }

    /**
     * add Drawing the specified arc, which will be scaled to fit inside the specified oval. <br>
     * If the start angle is negative or >= 360, the start angle is treated as start angle modulo
     * 360. <br>
     * If the sweep angle is >= 360, then the oval is drawn completely. Note that this differs
     * slightly from SkPath::arcTo, which treats the sweep angle modulo 360. If the sweep angle is
     * negative, the sweep angle is treated as sweep angle modulo 360 <br>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the geometric angle of 0
     * degrees (3 o'clock on a watch.) <br>
     *
     * @param left left coordinate of oval used to define the shape and size of the arc
     * @param top top coordinate of oval used to define the shape and size of the arc
     * @param right right coordinate of oval used to define the shape and size of the arc
     * @param bottom bottom coordinate of oval used to define the shape and size of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public void addDrawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        DrawArc.apply(mBuffer, left, top, right, bottom, startAngle, sweepAngle);
    }

    /**
     * add Drawing the specified sector, which will be scaled to fit inside the specified oval. <br>
     * If the start angle is negative or >= 360, the start angle is treated as start angle modulo
     * 360. <br>
     * If the sweep angle is >= 360, then the oval is drawn completely. Note that this differs
     * slightly from SkPath::arcTo, which treats the sweep angle modulo 360. If the sweep angle is
     * negative, the sweep angle is treated as sweep angle modulo 360 <br>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the geometric angle of 0
     * degrees (3 o'clock on a watch.) <br>
     *
     * @param left left coordinate of oval used to define the shape and size of the arc
     * @param top top coordinate of oval used to define the shape and size of the arc
     * @param right right coordinate of oval used to define the shape and size of the arc
     * @param bottom bottom coordinate of oval used to define the shape and size of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public void addDrawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        DrawSector.apply(mBuffer, left, top, right, bottom, startAngle, sweepAngle);
    }

    /**
     * @param imageId The Id bitmap to be drawn
     * @param left left coordinate of rectangle that the bitmap will be to fit into
     * @param top top coordinate of rectangle that the bitmap will be to fit into
     * @param right right coordinate of rectangle that the bitmap will be to fit into
     * @param bottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param contentDescriptionId content description of the image
     */
    public void addDrawBitmap(
            int imageId,
            float left,
            float top,
            float right,
            float bottom,
            int contentDescriptionId) {
        DrawBitmap.apply(mBuffer, imageId, left, top, right, bottom, contentDescriptionId);
    }

    /**
     * @param imageId The bitmap to be drawn
     * @param srcLeft left coordinate in the source bitmap will be to extracted
     * @param srcTop top coordinate in the source bitmap will be to extracted
     * @param srcRight right coordinate in the source bitmap will be to extracted
     * @param srcBottom bottom coordinate in the source bitmap will be to extracted
     * @param dstLeft left coordinate of rectangle that the bitmap will be to fit into
     * @param dstTop top coordinate of rectangle that the bitmap will be to fit into
     * @param dstRight right coordinate of rectangle that the bitmap will be to fit into
     * @param dstBottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param scaleType The type of scaling to allow the image to fit.
     * @param scaleFactor the scale factor when scale type is FIXED_SCALE (type = 7)
     * @param contentDescriptionId associate a string with image for accessibility
     */
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
        DrawBitmapScaled.apply(
                mBuffer,
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
                contentDescriptionId);
    }

    /**
     * Records a bitmap font and returns an ID.
     *
     * @param id the id to use
     * @param glyphs The glyphs that define the bitmap font
     * @return id of the BitmapFont
     */
    public int addBitmapFont(int id, BitmapFontData.Glyph @NonNull [] glyphs) {
        BitmapFontData.apply(mBuffer, id, glyphs, null);
        return id;
    }

    /**
     * Records a bitmap font and returns an ID.
     *
     * @param id the id to use
     * @param glyphs The glyphs that define the bitmap font
     * @param kerningTable The kerning table, where the key is pairs of glyphs (literally $1$2) and
     *     the value is the horizontal adjustment in pixels for that glyph pair. Can be empty.
     * @return id of the BitmapFont
     */
    public int addBitmapFont(
            int id,
            BitmapFontData.Glyph @NonNull [] glyphs,
            @NonNull Map<String, Short> kerningTable) {
        BitmapFontData.apply(mBuffer, id, glyphs, kerningTable);
        return id;
    }

    /**
     * This defines the name of the bitmap given the id.
     *
     * @param id of the Bitmap
     * @param name Name of the color
     */
    public void setBitmapName(int id, @NonNull String name) {
        NamedVariable.apply(mBuffer, id, NamedVariable.IMAGE_TYPE, name);
    }

    /**
     * Draw the specified circle using the specified paint. If radius is <= 0, then nothing will be
     * drawn.
     *
     * @param centerX The x-coordinate of the center of the circle to be drawn
     * @param centerY The y-coordinate of the center of the circle to be drawn
     * @param radius The radius of the circle to be drawn
     */
    public void addDrawCircle(float centerX, float centerY, float radius) {
        DrawCircle.apply(mBuffer, centerX, centerY, radius);
    }

    /**
     * Draw a line segment with the specified start and stop x,y coordinates, using the specified
     * paint.
     *
     * @param x1 The x-coordinate of the start point of the line
     * @param y1 The y-coordinate of the start point of the line
     * @param x2 The x-coordinate of the end point of the line
     * @param y2 The y-coordinate of the end point of the line
     */
    public void addDrawLine(float x1, float y1, float x2, float y2) {
        DrawLine.apply(mBuffer, x1, y1, x2, y2);
    }

    /**
     * Draw the specified oval using the specified paint.
     *
     * @param left left coordinate of oval
     * @param top top coordinate of oval
     * @param right right coordinate of oval
     * @param bottom bottom coordinate of oval
     */
    public void addDrawOval(float left, float top, float right, float bottom) {
        DrawOval.apply(mBuffer, left, top, right, bottom);
    }

    /**
     * interpolate the two paths to produce a 3rd
     *
     * @param out the id to use
     * @param pid1 the first path
     * @param pid2 the second path
     * @param tween path is the path1+(pat2-path1)*tween
     * @return id of the tweened path
     */
    public int pathTween(int out, int pid1, int pid2, float tween) {
        PathTween.apply(mBuffer, out, pid1, pid2, tween);
        return out;
    }

    /**
     * Create a path with an initial moveTo
     *
     * @param out the id to use
     * @param x x coordinate of the moveto
     * @param y y coordinate of the moveto
     * @return id of the created path
     */
    public int pathCreate(int out, float x, float y) {
        PathCreate.apply(mBuffer, out, x, y);
        return out;
    }

    /**
     * Append a path to an existing path
     *
     * @param id id of the path to append to
     * @param path the path to append
     */
    public void pathAppend(int id, float @NonNull ... path) {
        PathAppend.apply(mBuffer, id, path);
    }

    /**
     * Draw the specified path
     *
     * @param pathId
     */
    public void addDrawPath(int pathId) {
        DrawPath.apply(mBuffer, pathId);
    }

    /**
     * Draw the specified Rect
     *
     * @param left left coordinate of rectangle to be drawn
     * @param top top coordinate of rectangle to be drawn
     * @param right right coordinate of rectangle to be drawn
     * @param bottom bottom coordinate of rectangle to be drawn
     */
    public void addDrawRect(float left, float top, float right, float bottom) {
        DrawRect.apply(mBuffer, left, top, right, bottom);
    }

    /**
     * Draw the specified round-rect
     *
     * @param left left coordinate of rectangle to be drawn
     * @param top left coordinate of rectangle to be drawn
     * @param right left coordinate of rectangle to be drawn
     * @param bottom left coordinate of rectangle to be drawn
     * @param radiusX The x-radius of the oval used to round the corners
     * @param radiusY The y-radius of the oval used to round the corners
     */
    public void addDrawRoundRect(
            float left, float top, float right, float bottom, float radiusX, float radiusY) {
        DrawRoundRect.apply(mBuffer, left, top, right, bottom, radiusX, radiusY);
    }

    /**
     * Draw the text, with origin at (x,y) along the specified path.
     *
     * @param textId The text to be drawn
     * @param pathId The path the text should follow for its baseline
     * @param hOffset The distance along the path to add to the text's starting position
     * @param vOffset The distance above(-) or below(+) the path to position the text
     */
    public void addDrawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
        DrawTextOnPath.apply(mBuffer, textId, pathId, hOffset, vOffset);
    }

    /**
     * Draw the text, with origin at (x,y). The origin is interpreted based on the Align setting in
     * the paint.
     *
     * @param textId The text to be drawn
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param contextStart the context start
     * @param contextEnd the context end
     * @param x The x-coordinate of the origin of the text being drawn
     * @param y The y-coordinate of the baseline of the text being drawn
     * @param rtl Draw RTTL
     */
    public void addDrawTextRun(
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {
        DrawText.apply(mBuffer, textId, start, end, contextStart, contextEnd, x, y, rtl);
    }

    /**
     * Draw the text with a bitmap font, with origin at (x,y). The origin is interpreted based on
     * the Align setting in the paint.
     *
     * @param textId The text to be drawn
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param x The x-coordinate of the origin of the text being drawn
     * @param y The y-coordinate of the baseline of the text being drawn
     */
    public void addDrawBitmapFontTextRun(
            int textId, int bitmapFontId, int start, int end, float x, float y) {
        DrawBitmapFontText.apply(mBuffer, textId, bitmapFontId, start, end, x, y);
    }

    /**
     * Draw the text with a bitmap font along the path.
     *
     * @param textId The text to be drawn
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param pathId The id of the path to draw along
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param yAdj Adjustment away from the path along the normal at that point
     */
    public void addDrawBitmapFontTextRunOnPath(
            int textId, int bitmapFontId, int pathId, int start, int end, float yAdj) {
        DrawBitmapFontTextOnPath.apply(mBuffer, textId, bitmapFontId, pathId, start, end, yAdj);
    }

    /**
     * Draw a text on canvas at relative to position (x, y), offset panX and panY. <br>
     * The panning factors (panX, panY) mapped to the resulting bounding box of the text, in such a
     * way that a panning factor of (0.0, 0.0) would center the text at (x, y)
     *
     * <ul>
     *   <li>Panning of -1.0, -1.0 - the text above & right of x,y.
     *   <li>Panning of 1.0, 1.0 - the text is below and to the left
     *   <li>Panning of 1.0, 0.0 - the test is centered & to the right of x,y
     * </ul>
     *
     * <p>Setting panY to NaN results in y being the baseline of the text.
     *
     * @param textId id of text to draw
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param x Coordinate of the Anchor
     * @param y Coordinate of the Anchor
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param panX justifies text -1.0=right, 0.0=center, 1.0=left
     * @param panY position text -1.0=above, 0.0=center, 1.0=below, Nan=baseline
     */
    public void drawBitmapTextAnchored(
            int textId,
            int bitmapFontId,
            float start,
            float end,
            float x,
            float y,
            float panX,
            float panY) {
        DrawBitmapTextAnchored.apply(mBuffer, textId, bitmapFontId, start, end, x, y, panX, panY);
    }

    /**
     * Merge two text (from id's) output one id
     *
     * @param textId text id
     * @param id1 left id
     * @param id2 right id
     * @return new id that merges the two text
     */
    public int textMerge(int textId, int id1, int id2) {
        TextMerge.apply(mBuffer, textId, id1, id2);
        return textId;
    }

    public static final int PAD_AFTER_SPACE = TextFromFloat.PAD_AFTER_SPACE;
    public static final int PAD_AFTER_NONE = TextFromFloat.PAD_AFTER_NONE;
    public static final int PAD_AFTER_ZERO = TextFromFloat.PAD_AFTER_ZERO;
    public static final int PAD_PRE_SPACE = TextFromFloat.PAD_PRE_SPACE;
    public static final int PAD_PRE_NONE = TextFromFloat.PAD_PRE_NONE;
    public static final int PAD_PRE_ZERO = TextFromFloat.PAD_PRE_ZERO;

    /**
     * Create a TextFromFloat command which creates text from a Float.
     *
     * @param id The id of the text to create
     * @param value The value to convert
     * @param digitsBefore the digits before the decimal point
     * @param digitsAfter the digits after the decimal point
     * @param flags configure the behaviour using PAD_PRE_* and PAD_AFTER* flags
     * @return id of the string that can be passed to drawTextAnchored
     */
    public int createTextFromFloat(
            int id, float value, short digitsBefore, short digitsAfter, int flags) {
        TextFromFloat.apply(mBuffer, id, value, digitsBefore, digitsAfter, flags);
        return id;
    }

    /**
     * Draw a text on canvas at relative to position (x, y), offset panX and panY. <br>
     * The panning factors (panX, panY) mapped to the resulting bounding box of the text, in such a
     * way that a panning factor of (0.0, 0.0) would center the text at (x, y)
     *
     * <ul>
     *   <li>Panning of -1.0, -1.0 - the text above & right of x,y.
     *   <li>Panning of 1.0, 1.0 - the text is below and to the left
     *   <li>Panning of 1.0, 0.0 - the test is centered & to the right of x,y
     * </ul>
     *
     * <p>Setting panY to NaN results in y being the baseline of the text.
     *
     * @param textId text to draw
     * @param x Coordinate of the Anchor
     * @param y Coordinate of the Anchor
     * @param panX justifies text -1.0=right, 0.0=center, 1.0=left
     * @param panY position text -1.0=above, 0.0=center, 1.0=below, Nan=baseline
     * @param flags 1 = RTL
     */
    public void drawTextAnchored(int textId, float x, float y, float panX, float panY, int flags) {
        DrawTextAnchored.apply(mBuffer, textId, x, y, panX, panY, flags);
    }

    /**
     * draw an interpolation between two paths that have the same pattern
     *
     * @param path1Id The path1 to be drawn between
     * @param path2Id The path2 to be drawn between
     * @param tween The ratio of path1 and path2 to 0 = all path 1, 1 = all path2
     * @param start The start of the subrange of paths to draw 0 = start form start .5 is 1/2 way
     * @param stop The end of the subrange of paths to draw 1 = end at the end .5 is end 1/2 way
     */
    public void addDrawTweenPath(int path1Id, int path2Id, float tween, float start, float stop) {
        DrawTweenPath.apply(mBuffer, path1Id, path2Id, tween, start, stop);
    }

    /**
     * Add a path object
     *
     * @param id the path id
     * @param pathData the path data
     * @return the id of the path on the wire
     */
    public int addPathData(int id, float @NonNull [] pathData) {
        PathData.apply(mBuffer, id, pathData);
        return id;
    }

    /**
     * Add a path object
     *
     * @param id the path id
     * @param pathData the path data
     * @return the id of the path on the wire
     */
    public int addPathData(int id, float @NonNull [] pathData, int winding) {
        if (mApiLevel < 7 && winding != 0) {
            throw new RuntimeException("winding not supported in API level < 7");
        }
        PathData.apply(mBuffer, id | (winding << 24), pathData);
        return id;
    }
    /**
     * Adds a paint Bundle to the doc
     *
     * @param paint
     */
    public void addPaint(@NonNull PaintBundle paint) {
        PaintData.apply(mBuffer, paint);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * inflate the buffer into a list of operations
     *
     * @param operations the operations list to add to
     */
    public void inflateFromBuffer(@NonNull ArrayList<Operation> operations) {
        mApiLevel = Header.readApiLevel(mBuffer);
        int profiles = 0;
        if (mApiLevel >= 7) {
            try {
                Header header = Header.readDirect(mBuffer);
                profiles = header.getProfiles();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        mBuffer.setIndex(0);
        if (mApiLevel == -1) {
            // Invalid API level (or invalid Header)
            return;
        }
        Operations.UniqueIntMap<CompanionOperation> map =
                Operations.getOperations(mApiLevel, profiles);
        if (map == null) {
            // Invalid operations map
            return;
        }
        mMap = map;
        while (mBuffer.available()) {
            int opId = mBuffer.readByte();
            if (DEBUG) {
                Utils.log(">> " + opId);
            }
            CompanionOperation operation = mMap.get(opId);
            if (operation == null) {
                throw new RuntimeException("Unknown operation encountered " + opId);
            }
            operation.read(mBuffer, operations);
        }
    }

    /**
     * copy the current buffer to a new one
     *
     * @return A new RemoteComposeBuffer
     */
    @NonNull RemoteComposeBuffer copy() {
        ArrayList<Operation> operations = new ArrayList<>();
        inflateFromBuffer(operations);
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        return copyFromOperations(operations, buffer);
    }

    /**
     * add a set theme
     *
     * @param theme The theme to set
     */
    public void setTheme(int theme) {
        Theme.apply(mBuffer, theme);
    }

    @NonNull
    static String version() {
        return "v1.0";
    }

    /**
     * Initialize a buffer from a file
     *
     * @param path the file path
     * @return the RemoteComposeBuffer
     * @throws IOException
     */
    @NonNull
    public static RemoteComposeBuffer fromFile(@NonNull String path) throws IOException {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        read(new File(path), buffer);
        return buffer;
    }

    /**
     * Create a RemoteComposeBuffer from a file
     *
     * @param file A file
     * @return A RemoteComposeBuffer
     * @throws IOException if the file cannot be read
     */
    @NonNull
    public RemoteComposeBuffer fromFile(@NonNull File file) throws IOException {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        read(file, buffer);
        return buffer;
    }

    /**
     * Create a RemoteComposeBuffer from an InputStream
     *
     * @param inputStream An InputStream
     * @return A RemoteComposeBuffer
     */
    @NonNull
    public static RemoteComposeBuffer fromInputStream(@NonNull InputStream inputStream) {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        read(inputStream, buffer);
        return buffer;
    }

    /**
     * Create a RemoteComposeBuffer from an array of operations
     *
     * @param operations An array of operations
     * @param buffer A RemoteComposeBuffer
     * @return A RemoteComposeBuffer
     */
    @NonNull RemoteComposeBuffer copyFromOperations(
            @NonNull ArrayList<Operation> operations, @NonNull RemoteComposeBuffer buffer) {

        for (Operation operation : operations) {
            operation.write(buffer.mBuffer);
        }
        return buffer;
    }

    /**
     * Write the given RemoteComposeBuffer to the given file
     *
     * @param buffer a RemoteComposeBuffer
     * @param file a target file
     */
    public void write(@NonNull RemoteComposeBuffer buffer, @NonNull File file) {
        try {
            FileOutputStream fd = new FileOutputStream(file);
            fd.write(buffer.mBuffer.getBuffer(), 0, buffer.mBuffer.getSize());
            fd.flush();
            fd.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Read the content of the file into the buffer
     *
     * @param file a target file
     * @param buffer a RemoteComposeBuffer
     * @throws IOException
     */
    static void read(@NonNull File file, @NonNull RemoteComposeBuffer buffer) throws IOException {
        FileInputStream fd = new FileInputStream(file);
        read(fd, buffer);
    }

    /**
     * Initialize a buffer from an input stream
     *
     * @param fd the input stream
     * @param buffer a RemoteComposeBuffer
     */
    public static void read(@NonNull InputStream fd, @NonNull RemoteComposeBuffer buffer) {
        try {
            byte[] bytes = readAllBytes(fd);
            buffer.reset(bytes.length);
            System.arraycopy(bytes, 0, buffer.mBuffer.mBuffer, 0, bytes.length);
            buffer.mBuffer.mSize = bytes.length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load a byte buffer from the input stream
     *
     * @param is the input stream
     * @return a byte buffer containing the input stream content
     * @throws IOException
     */
    private static byte[] readAllBytes(@NonNull InputStream is) throws IOException {
        byte[] buff = new byte[32 * 1024]; // moderate size buff to start
        int red = 0;
        while (true) {
            int ret = is.read(buff, red, buff.length - red);
            if (ret == -1) {
                is.close();
                return Arrays.copyOf(buff, red);
            }
            red += ret;
            if (red == buff.length) {
                buff = Arrays.copyOf(buff, buff.length * 2);
            }
        }
    }

    /**
     * add a Pre-concat the current matrix with the specified skew.
     *
     * @param skewX The amount to skew in X
     * @param skewY The amount to skew in Y
     */
    public void addMatrixSkew(float skewX, float skewY) {
        MatrixSkew.apply(mBuffer, skewX, skewY);
    }

    /**
     * This call balances a previous call to save(), and is used to remove all modifications to the
     * matrix/clip state since the last save call. Do not call restore() more times than save() was
     * called.
     */
    public void addMatrixRestore() {
        MatrixRestore.apply(mBuffer);
    }

    /**
     * Add a saves the current matrix and clip onto a private stack.
     *
     * <p>Subsequent calls to translate,scale,rotate,skew,concat or clipRect, clipPath will all
     * operate as usual, but when the balancing call to restore() is made, those calls will be
     * forgotten, and the settings that existed before the save() will be reinstated.
     */
    public void addMatrixSave() {
        MatrixSave.apply(mBuffer);
    }

    /**
     * add a pre-concat the current matrix with the specified rotation.
     *
     * @param angle The amount to rotate, in degrees
     * @param centerX The x-coord for the pivot point (unchanged by the rotation)
     * @param centerY The y-coord for the pivot point (unchanged by the rotation)
     */
    public void addMatrixRotate(float angle, float centerX, float centerY) {
        MatrixRotate.apply(mBuffer, angle, centerX, centerY);
    }

    /**
     * add a Pre-concat to the current matrix with the specified translation
     *
     * @param dx The distance to translate in X
     * @param dy The distance to translate in Y
     */
    public void addMatrixTranslate(float dx, float dy) {
        MatrixTranslate.apply(mBuffer, dx, dy);
    }

    /**
     * Add a pre-concat of the current matrix with the specified scale.
     *
     * @param scaleX The amount to scale in X
     * @param scaleY The amount to scale in Y
     */
    public void addMatrixScale(float scaleX, float scaleY) {
        MatrixScale.apply(mBuffer, scaleX, scaleY, Float.NaN, Float.NaN);
    }

    /**
     * Add a pre-concat of the current matrix with the specified scale.
     *
     * @param scaleX The amount to scale in X
     * @param scaleY The amount to scale in Y
     * @param centerX The x-coord for the pivot point (unchanged by the scale)
     * @param centerY The y-coord for the pivot point (unchanged by the scale)
     */
    public void addMatrixScale(float scaleX, float scaleY, float centerX, float centerY) {
        MatrixScale.apply(mBuffer, scaleX, scaleY, centerX, centerY);
    }

    /**
     * sets the clip based on clip id
     *
     * @param pathId 0 clears the clip
     */
    public void addClipPath(int pathId) {
        ClipPath.apply(mBuffer, pathId);
    }

    /**
     * Sets the clip based on clip rec
     *
     * @param left left coordinate of the clip rectangle
     * @param top top coordinate of the clip rectangle
     * @param right right coordinate of the clip rectangle
     * @param bottom bottom coordinate of the clip rectangle
     */
    public void addClipRect(float left, float top, float right, float bottom) {
        ClipRect.apply(mBuffer, left, top, right, bottom);
    }

    /**
     * Add a float return a NaN number pointing to that float
     *
     * @param id the id of the constant
     * @param value the value of the float
     * @return the NaN id of float
     */
    public float addFloat(int id, float value) {
        FloatConstant.apply(mBuffer, id, value);
        return Utils.asNan(id);
    }

    /**
     * Add a Integer return an id number pointing to that float.
     *
     * @param id the value id
     * @param value adds an integer and assigns it an id
     */
    public void addInteger(int id, int value) {
        IntegerConstant.apply(mBuffer, id, value);
    }

    /**
     * Add a long constant return a id. They can be used as parameters to Custom Attributes.
     *
     * @param id the value id
     * @param value the value of the long
     */
    public void addLong(int id, long value) {
        LongConstant.apply(mBuffer, id, value);
    }

    /**
     * Add a boolean constant return a id. They can be used as parameters to Custom Attributes.
     *
     * @param id the value id
     * @param value the value of the boolean
     */
    public void addBoolean(int id, boolean value) {
        BooleanConstant.apply(mBuffer, id, value);
    }

    /**
     * Add a IntegerId as float ID.
     *
     * @param id id to be converted
     * @return the id wrapped in a NaN
     */
    public float asFloatId(int id) {
        return Utils.asNan(id);
    }

    /**
     * Add a float that is a computation based on variables
     *
     * @param id the id of the float
     * @param value A RPN style float operation i.e. "4, 3, ADD" outputs 7
     */
    public void addAnimatedFloat(int id, float @NonNull ... value) {
        FloatExpression.apply(mBuffer, id, value, null);
    }

    /**
     * Add a float that is a computation based on variables. see packAnimation
     *
     * @param id the id of the float
     * @param value A RPN style float operation i.e. "4, 3, ADD" outputs 7
     * @param animation Array of floats that represents animation
     */
    public void addAnimatedFloat(int id, float @NonNull [] value, float @Nullable [] animation) {
        FloatExpression.apply(mBuffer, id, value, animation);
    }

    /**
     * Add a touch handle system
     *
     * @param id the id used for the returned position
     * @param value the default value
     * @param min the minimum value
     * @param max the maximum value
     * @param velocityId the id for the velocity TODO support in v2
     * @param touchEffects the touch effects
     * @param exp The Float Expression
     * @param touchMode the touch up handling behaviour
     * @param touchSpec the touch up handling parameters
     * @param easingSpec the easing parameter TODO support in v2
     */
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
        TouchExpression.apply(
                mBuffer,
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
    }

    /**
     * measure the text and return a measure as a float
     *
     * @param id id of the measure
     * @param textId id of the text
     * @param mode the mode 0 is the width
     */
    public void textMeasure(int id, int textId, int mode) {
        TextMeasure.apply(mBuffer, id, textId, mode);
    }

    /**
     * measure the text and return the length of the text as float
     *
     * @param id id of the length op
     * @param textId id of the text
     */
    public void textLength(int id, int textId) {
        TextLength.apply(mBuffer, id, textId);
    }

    /**
     * add a float array
     *
     * @param id id of the array
     * @param values
     */
    public void addFloatArray(int id, float @NonNull [] values) {
        DataListFloat.apply(mBuffer, id, values);
    }

    /**
     * This creates a list of individual floats
     *
     * @param id list id
     * @param listId array id to be stored
     */
    public void addList(int id, int @NonNull [] listId) {
        DataListIds.apply(mBuffer, id, listId);
    }

    /**
     * add an int map
     *
     * @param id int map id
     * @param keys
     * @param types
     * @param listId
     */
    public void addMap(
            int id, @NonNull String[] keys, byte @Nullable [] types, int @NonNull [] listId) {
        DataMapIds.apply(mBuffer, id, keys, types, listId);
    }

    /**
     * This provides access to text in RemoteList
     *
     * <p>TODO: do we want both a float and an int index version of this method? bbade@ TODO
     * for @hoford - add a unit test for this method
     *
     * @param id id of the text
     * @param dataSet
     * @param index index as a float variable
     */
    public void textLookup(int id, float dataSet, float index) {
        TextLookup.apply(mBuffer, id, Utils.idFromNan(dataSet), index);
    }

    /**
     * This provides access to text in RemoteList
     *
     * @param id id of integer to write
     * @param dataSet the array
     * @param index index as a float variable
     */
    public void idLookup(int id, float dataSet, float index) {
        IdLookup.apply(mBuffer, id, Utils.idFromNan(dataSet), index);
    }

    /**
     * This provides access to text in RemoteList
     *
     * <p>TODO for hoford - add a unit test for this method
     *
     * @param id id of the text
     * @param dataSet
     * @param index index as an int variable
     */
    public void textLookup(int id, float dataSet, int index) {
        TextLookupInt.apply(mBuffer, id, Utils.idFromNan(dataSet), index);
    }

    /**
     * Add and integer expression
     *
     * @param id the id of the expression
     * @param mask defines which elements are operators or variables
     * @param value array of values to calculate maximum 32
     */
    public void addIntegerExpression(int id, int mask, int @NonNull [] value) {
        IntegerExpression.apply(mBuffer, id, mask, value);
    }

    /**
     * Add a simple color
     *
     * @param id the color id
     * @param color the RGB color value
     */
    public void addColor(int id, int color) {
        ColorConstant c = new ColorConstant(id, color);
        c.write(mBuffer);
    }

    /**
     * Add a color that represents the tween between two colors
     *
     * @param id the color id
     * @param color1 the ARGB value of the first color
     * @param color2 the ARGB value of the second color
     * @param tween the interpolation bet
     */
    public void addColorExpression(int id, int color1, int color2, float tween) {
        ColorExpression c = new ColorExpression(id, 0, color1, color2, tween);
        c.write(mBuffer);
    }

    /**
     * Add a color that represents the tween between two colors where color1 is the id of a color
     *
     * @param id the color id
     * @param color1 id of color
     * @param color2 rgb color value
     * @param tween the tween between color1 and color2 (1 = color2)
     */
    public void addColorExpression(int id, short color1, int color2, float tween) {
        ColorExpression c = new ColorExpression(id, 1, color1, color2, tween);
        c.write(mBuffer);
    }

    /**
     * Add a color that represents the tween between two colors where color2 is the id of a color
     *
     * @param id the color id
     * @param color1 the ARGB value of the first color
     * @param color2 id of the second color
     * @param tween the tween between color1 and color2 (1 = color2)
     */
    public void addColorExpression(int id, int color1, short color2, float tween) {
        ColorExpression c = new ColorExpression(id, 2, color1, color2, tween);
        c.write(mBuffer);
    }

    /**
     * Add a color that represents the tween between two colors where color1 & color2 are the ids of
     * colors
     *
     * @param id the color id
     * @param color1 id of the first color
     * @param color2 id of the second color
     * @param tween the tween between color1 and color2 (1 = color2)
     */
    public void addColorExpression(int id, short color1, short color2, float tween) {
        ColorExpression c = new ColorExpression(id, 3, color1, color2, tween);
        c.write(mBuffer);
    }

    /**
     * Color calculated by Hue saturation and value. (as floats they can be variables used to create
     * color transitions)
     *
     * @param id the color id
     * @param hue the Hue
     * @param sat the saturation
     * @param value the value
     */
    public void addColorExpression(int id, float hue, float sat, float value) {
        ColorExpression c = new ColorExpression(id, hue, sat, value);
        c.write(mBuffer);
    }

    /**
     * Color calculated by Alpha, Hue saturation and value. (as floats they can be variables used to
     * create color transitions)
     *
     * @param id the color id
     * @param alpha the Alpha
     * @param hue the hue
     * @param sat the saturation
     * @param value the value
     */
    public void addColorExpression(int id, int alpha, float hue, float sat, float value) {
        ColorExpression c =
                new ColorExpression(id, ColorExpression.HSV_MODE, alpha, hue, sat, value);
        c.write(mBuffer);
    }

    /**
     * Color calculated by Alpha, Red, Green and Blue. (as floats they can be variables used to
     * create color transitions)
     *
     * @param id the color id
     * @param alpha the alpha value of the color
     * @param red the red component of the color
     * @param green the green component of the color
     * @param blue the blue component of the color
     */
    public void addColorExpression(int id, float alpha, float red, float green, float blue) {
        ColorExpression c =
                new ColorExpression(id, ColorExpression.ARGB_MODE, alpha, red, green, blue);
        c.write(mBuffer);
    }

    /**
     * create and animation based on description and return as an array of floats. see
     * addAnimatedFloat
     *
     * @param duration the duration of the animation in seconds
     * @param type the type of animation
     * @param spec the parameters of the animation if any
     * @param initialValue the initial value if it animates to a start
     * @param wrap the wraps value so (e.g 360 so angles 355 would animate to 5)
     * @return
     */
    public static float @NonNull [] packAnimation(
            float duration, int type, float @Nullable [] spec, float initialValue, float wrap) {

        return FloatAnimation.packToFloatArray(duration, type, spec, initialValue, wrap);
    }

    /**
     * This defines the name of a type of given object
     *
     * @param id of the float
     * @param name name of the float
     * @param type the type of variable NamedVariable.COLOR_TYPE, STRING_TYPE, etc
     */
    public void setNamedVariable(int id, @NonNull String name, int type) {
        NamedVariable.apply(mBuffer, id, type, name);
    }

    /**
     * Returns a usable component id -- either the one passed in parameter if not -1 or a generated
     * one.
     *
     * @param id the current component id (if -1, we'll generate a new one)
     * @return a usable component id
     */
    private int getComponentId(int id) {
        int resolvedId = 0;
        if (id != -1) {
            resolvedId = id;
        } else {
            mGeneratedComponentId--;
            resolvedId = mGeneratedComponentId;
        }
        return resolvedId;
    }

    /**
     * Add a component start tag
     *
     * @param type type of component
     * @param id component id
     */
    public void addComponentStart(int type, int id) {
        mLastComponentId = getComponentId(id);
        ComponentStart.apply(mBuffer, type, mLastComponentId, 0f, 0f);
    }

    /**
     * Add a component start tag
     *
     * @param type type of component
     */
    public void addComponentStart(int type) {
        addComponentStart(type, -1);
    }

    /** Add a component end tag */
    public void addContainerEnd() {
        ContainerEnd.apply(mBuffer);
    }

    /**
     * Add a scroll modifier
     *
     * @param direction HORIZONTAL(0) or VERTICAL(1)
     * @param max max scroll amount
     */
    public void addModifierScroll(int direction, float max) {
        ScrollModifierOperation.apply(mBuffer, direction, 0f, max, 0f);
        ContainerEnd.apply(mBuffer);
    }

    /**
     * Add a background modifier of provided color
     *
     * @param color the color of the background
     * @param shape the background shape -- SHAPE_RECTANGLE, SHAPE_CIRCLE
     */
    public void addModifierBackground(int color, int shape) {
        float r = (color >> 16 & 0xff) / 255.0f;
        float g = (color >> 8 & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        float a = (color >> 24 & 0xff) / 255.0f;
        BackgroundModifierOperation.apply(mBuffer, 0f, 0f, 0f, 0f, r, g, b, a, shape);
    }

    /**
     * Add a border modifier
     *
     * @param borderWidth the border width
     * @param borderRoundedCorner the rounded corner radius if the shape is ROUNDED_RECT
     * @param color the color of the border
     * @param shape the shape of the border
     */
    public void addModifierBorder(
            float borderWidth, float borderRoundedCorner, int color, int shape) {
        float r = (color >> 16 & 0xff) / 255.0f;
        float g = (color >> 8 & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        float a = (color >> 24 & 0xff) / 255.0f;
        BorderModifierOperation.apply(
                mBuffer, 0f, 0f, 0f, 0f, borderWidth, borderRoundedCorner, r, g, b, a, shape);
    }

    /**
     * Add a padding modifier
     *
     * @param left left padding
     * @param top top padding
     * @param right right padding
     * @param bottom bottom padding
     */
    public void addModifierPadding(float left, float top, float right, float bottom) {
        PaddingModifierOperation.apply(mBuffer, left, top, right, bottom);
    }

    /**
     * Add an offset modifier
     *
     * @param x x offset
     * @param y y offset
     */
    public void addModifierOffset(float x, float y) {
        OffsetModifierOperation.apply(mBuffer, x, y);
    }

    /**
     * Add a zIndex modifier
     *
     * @param value z-Index value
     */
    public void addModifierZIndex(float value) {
        ZIndexModifierOperation.apply(mBuffer, value);
    }

    /** Add a ripple effect on touch down as a modifier */
    public void addModifierRipple() {
        RippleModifierOperation.apply(mBuffer);
    }

    /**
     * Add a marquee modifier
     *
     * @param iterations number of iterations
     * @param animationMode animation mode
     * @param repeatDelayMillis repeat delay
     * @param initialDelayMillis initial delay
     * @param spacing spacing between items
     * @param velocity velocity of the marquee
     */
    public void addModifierMarquee(
            int iterations,
            int animationMode,
            float repeatDelayMillis,
            float initialDelayMillis,
            float spacing,
            float velocity) {
        MarqueeModifierOperation.apply(
                mBuffer,
                iterations,
                animationMode,
                repeatDelayMillis,
                initialDelayMillis,
                spacing,
                velocity);
    }

    /**
     * Add a graphics layer
     *
     * @param attributes
     */
    public void addModifierGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {
        GraphicsLayerModifierOperation.apply(mBuffer, attributes);
    }

    /**
     * Sets the clip based on rounded clip rect
     *
     * @param topStart
     * @param topEnd
     * @param bottomStart
     * @param bottomEnd
     */
    public void addRoundClipRectModifier(
            float topStart, float topEnd, float bottomStart, float bottomEnd) {
        RoundedClipRectModifierOperation.apply(mBuffer, topStart, topEnd, bottomStart, bottomEnd);
    }

    /** Add a clip rect modifier */
    public void addClipRectModifier() {
        ClipRectModifierOperation.apply(mBuffer);
    }

    /**
     * add start of loop
     *
     * @param indexId id of the variable
     * @param from start value
     * @param step step value
     * @param until stop value
     */
    public void addLoopStart(int indexId, float from, float step, float until) {
        LoopOperation.apply(mBuffer, indexId, from, step, until);
    }

    /** Add a loop end */
    public void addLoopEnd() {
        ContainerEnd.apply(mBuffer);
    }

    /**
     * add a state layout
     *
     * @param componentId id of the state
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     * @param indexId index of the state
     */
    public void addStateLayout(
            int componentId, int animationId, int horizontal, int vertical, int indexId) {
        mLastComponentId = getComponentId(componentId);
        StateLayout.apply(mBuffer, mLastComponentId, animationId, horizontal, vertical, indexId);
    }

    /**
     * Add a box start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     */
    public void addBoxStart(int componentId, int animationId, int horizontal, int vertical) {
        mLastComponentId = getComponentId(componentId);
        BoxLayout.apply(mBuffer, mLastComponentId, animationId, horizontal, vertical);
    }

    /**
     * Add a fitbox start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     */
    public void addFitBoxStart(int componentId, int animationId, int horizontal, int vertical) {
        mLastComponentId = getComponentId(componentId);
        FitBoxLayout.apply(mBuffer, mLastComponentId, animationId, horizontal, vertical);
    }

    /**
     * Add an imagelayout command
     *
     * @param componentId component id
     * @param animationId animation id
     * @param bitmapId bitmap id
     * @param scaleType scale type
     * @param alpha alpha value
     */
    public void addImage(
            int componentId, int animationId, int bitmapId, int scaleType, float alpha) {
        mLastComponentId = getComponentId(componentId);
        ImageLayout.apply(mBuffer, componentId, animationId, bitmapId, scaleType, alpha);
    }

    /**
     * Add a row start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     * @param spacedBy spacing between items
     */
    public void addRowStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        RowLayout.apply(mBuffer, mLastComponentId, animationId, horizontal, vertical, spacedBy);
    }

    /**
     * Add a row start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     * @param spacedBy spacing between items
     */
    public void addCollapsibleRowStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        CollapsibleRowLayout.apply(
                mBuffer, mLastComponentId, animationId, horizontal, vertical, spacedBy);
    }

    /**
     * Add a column start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     * @param spacedBy spacing between items
     */
    public void addColumnStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        ColumnLayout.apply(mBuffer, mLastComponentId, animationId, horizontal, vertical, spacedBy);
    }

    /**
     * Add a column start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param horizontal horizontal alignment
     * @param vertical vertical alignment
     * @param spacedBy spacing between items
     */
    public void addCollapsibleColumnStart(
            int componentId, int animationId, int horizontal, int vertical, float spacedBy) {
        mLastComponentId = getComponentId(componentId);
        CollapsibleColumnLayout.apply(
                mBuffer, mLastComponentId, animationId, horizontal, vertical, spacedBy);
    }

    /**
     * Add a canvas start tag
     *
     * @param componentId component id
     * @param animationId animation id
     */
    public void addCanvasStart(int componentId, int animationId) {
        mLastComponentId = getComponentId(componentId);
        CanvasLayout.apply(mBuffer, mLastComponentId, animationId);
    }

    /**
     * Add a canvas content start tag
     *
     * @param componentId component id
     */
    public void addCanvasContentStart(int componentId) {
        mLastComponentId = getComponentId(componentId);
        CanvasContent.apply(mBuffer, mLastComponentId);
    }

    /** Add a root start tag */
    public void addRootStart() {
        mLastComponentId = getComponentId(-1);
        RootLayoutComponent.apply(mBuffer, mLastComponentId);
    }

    /** Add a content start tag */
    public void addContentStart() {
        mLastComponentId = getComponentId(-1);
        LayoutComponentContent.apply(mBuffer, mLastComponentId);
    }

    /** Add a canvas operations start tag */
    public void addCanvasOperationsStart() {
        CanvasOperations.apply(mBuffer);
    }

    /** Add container hosting actions */
    public void addRunActionsStart() {
        RunActionOperation.apply(mBuffer);
    }

    /**
     * Add a component width value
     *
     * @param id id of the value
     */
    public void addComponentWidthValue(int id) {
        ComponentValue.apply(mBuffer, ComponentValue.WIDTH, mLastComponentId, id);
    }

    /**
     * Add a component height value
     *
     * @param id id of the value
     */
    public void addComponentHeightValue(int id) {
        ComponentValue.apply(mBuffer, ComponentValue.HEIGHT, mLastComponentId, id);
    }

    /**
     * Add a text component start tag
     *
     * @param componentId component id
     * @param animationId animation id
     * @param textId id of the text
     * @param color color of the text
     * @param fontSize font size
     * @param fontStyle font style (0 : Normal, 1 : Italic)
     * @param fontWeight font weight (1 to 1000, normal is 400)
     * @param fontFamilyId font family or null
     * @param textAlign text alignment (0 : Center, 1 : Left, 2 : Right)
     * @param overflow
     * @param maxLines
     */
    public void addTextComponentStart(
            int componentId,
            int animationId,
            int textId,
            int color,
            float fontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            int textAlign,
            int overflow,
            int maxLines) {
        mLastComponentId = getComponentId(componentId);
        TextLayout.apply(
                mBuffer,
                mLastComponentId,
                animationId,
                textId,
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamilyId,
                textAlign,
                overflow,
                maxLines);
    }

    /**
     * add an impulse. (must be followed by impulse end)
     *
     * @param duration duration of the impulse
     * @param start the start time
     */
    public void addImpulse(float duration, float start) {
        ImpulseOperation.apply(mBuffer, duration, start);
    }

    /** add an impulse process */
    public void addImpulseProcess() {
        ImpulseProcess.apply(mBuffer);
    }

    /** Add an impulse end */
    public void addImpulseEnd() {
        ContainerEnd.apply(mBuffer);
    }

    /**
     * Start a particle engine container & initial setup
     *
     * @param id the particle engine id
     * @param varIds list of variable ids
     * @param initialExpressions the expressions used to initialize the variables
     * @param particleCount the number of particles to draw
     */
    public void addParticles(
            int id,
            int @NonNull [] varIds,
            float @NonNull [][] initialExpressions,
            int particleCount) {
        ParticlesCreate.apply(mBuffer, id, varIds, initialExpressions, particleCount);
    }

    /**
     * Setup the particle engine loop
     *
     * @param id the particle engine id
     * @param restart value on restart
     * @param expressions the expressions used to update the variables during the particles run
     */
    public void addParticlesLoop(
            int id, float @Nullable [] restart, float @NonNull [][] expressions) {
        ParticlesLoop.apply(mBuffer, id, restart, expressions);
    }

    /** Closes the particle engine container */
    public void addParticleLoopEnd() {
        ContainerEnd.apply(mBuffer);
    }

    /**
     * @param fid The id of the function
     * @param args The arguments of the function
     */
    public void defineFloatFunction(int fid, int @NonNull [] args) {
        FloatFunctionDefine.apply(mBuffer, fid, args);
    }

    /** end the definition of the function */
    public void addEndFloatFunctionDef() {
        ContainerEnd.apply(mBuffer);
    }

    /**
     * add a function call
     *
     * @param id the id of the function to call
     * @param args the arguments of the function
     */
    public void callFloatFunction(int id, float @Nullable [] args) {
        FloatFunctionCall.apply(mBuffer, id, args);
    }

    /**
     * @param id attribute id
     * @param bitmapId the id of the bitmap
     * @param attribute the attribute to get
     */
    public void bitmapAttribute(int id, int bitmapId, short attribute) {
        ImageAttribute.apply(mBuffer, id, bitmapId, attribute, null);
    }

    /**
     * @param id the text attribute id
     * @param textId the id of the bitmap
     * @param attribute the attribute to get
     */
    public void textAttribute(int id, int textId, short attribute) {
        TextAttribute.apply(mBuffer, id, textId, attribute);
    }

    /**
     * @param id the time attribute id
     * @param timeId the id of the long
     * @param attribute the attribute to get
     * @param args the arguments of the function
     */
    public void timeAttribute(int id, int timeId, short attribute, int @Nullable ... args) {
        TimeAttribute.apply(mBuffer, id, timeId, attribute, args);
    }

    /** In the context of a component draw modifier, draw the content of the component */
    public void drawComponentContent() {
        DrawContent.apply(mBuffer);
    }

    /**
     * Store an image in the buffer
     *
     * @param imageId the image id
     * @param imageWidth the image width
     * @param imageHeight the image height
     * @param data the image data
     * @return the image id
     */
    public int storeBitmap(int imageId, int imageWidth, int imageHeight, byte @NonNull [] data) {
        BitmapData.apply(mBuffer, imageId, imageWidth, imageHeight, data); // todo: potential npe
        return imageId;
    }

    /**
     * Create a bitmap of given id, width and height Bitmap contains no data, It's only use is to
     * draw to
     *
     * @param imageId
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public int createBitmap(int imageId, short imageWidth, short imageHeight) {
        BitmapData.apply(
                mBuffer,
                imageId,
                BitmapData.TYPE_RAW8888,
                imageWidth,
                BitmapData.ENCODING_EMPTY,
                imageHeight,
                new byte[0]);
        return imageId;
    }

    /**
     * @param imageId
     * @param mode
     * @param color
     */
    public void drawOnBitmap(int imageId, int mode, int color) {
        DrawToBitmap.apply(mBuffer, imageId, mode, color);
    }

    /**
     * Store an alpha 8 image in the buffer
     *
     * @param imageId the image id
     * @param imageWidth the image width
     * @param imageHeight the image height
     * @param data the image data
     * @return the image id
     */
    public int storeBitmapA8(int imageId, int imageWidth, int imageHeight, byte @NonNull [] data) {
        BitmapData.apply(
                mBuffer,
                imageId,
                BitmapData.TYPE_PNG_ALPHA_8,
                (short) imageWidth,
                BitmapData.ENCODING_INLINE,
                (short) imageHeight,
                data); // todo: potential npe
        return imageId;
    }

    /**
     * Combine two paths
     *
     * @param id output id
     * @param path1 first path
     * @param path2 second path
     * @param op operation to perform OP_DIFFERENCE, OP_INTERSECT, OP_REVERSE_DIFFERENCE, OP_UNION,
     *     OP_XOR
     */
    public void pathCombine(int id, int path1, int path2, byte op) {

        PathCombine.apply(mBuffer, id, path1, path2, op);
    }

    /**
     * Perform a haptic feedback
     *
     * @param feedbackConstant
     */
    public void performHaptic(int feedbackConstant) {
        HapticFeedback.apply(mBuffer, feedbackConstant);
    }

    /**
     * Add a conditional operation
     *
     * @param type type of comparison
     * @param a first value
     * @param b second value
     */
    public void addConditionalOperations(byte type, float a, float b) {
        ConditionalOperations.apply(mBuffer, type, a, b);
    }

    /**
     * Add a debug message
     *
     * @param textId text id
     * @param value value
     * @param flags flags
     */
    public void addDebugMessage(int textId, float value, int flags) {
        DebugMessage.apply(mBuffer, textId, value, flags);
    }

    /**
     * Return a color attribute value on the given color
     *
     * @param id the color attribute id
     * @param baseColor
     * @param type type of attribute
     * @return
     */
    public void getColorAttribute(int id, int baseColor, short type) {
        ColorAttribute.apply(mBuffer, id, baseColor, type);
    }

    /**
     * set a matrix from path
     *
     * @param pathId id of path
     * @param fraction fractional position on path
     * @param vOffset vertical offset to add to matrix
     * @param flags flags to use 1 = Tangent
     */
    public void setMatrixFromPath(int pathId, float fraction, float vOffset, int flags) {
        MatrixFromPath.apply(mBuffer, pathId, fraction, vOffset, flags);
    }

    /**
     * Subtext of a text
     *
     * @param id the text subtext id
     * @param txtId the input text
     * @param start the start position 0 = first character
     * @param len the length of the subtext -1 = to the end
     */
    public void textSubtext(int id, int txtId, float start, float len) {
        TextSubtext.apply(mBuffer, id, txtId, start, len);
    }

    /**
     * Measure a text using a bitmap font
     *
     * @param id the id of the resulting measure
     * @param textId the input text
     * @param bmFontId the bitmap font
     * @param type
     * @return
     */
    public void bitmapTextMeasure(int id, int textId, int bmFontId, int type) {
        BitmapTextMeasure.apply(mBuffer, id, textId, bmFontId, type);
    }

    /**
     * Add a comment to the document This does not consume string space.
     *
     * @param text text inserted into the document.
     */
    public void rem(@NonNull String text) {
        Rem.apply(mBuffer, text);
    }

    /**
     * Set current version of the buffer (typically for writing)
     *
     * @param documentApiLevel
     * @param profiles
     */
    public void setVersion(int documentApiLevel, int profiles) {
        mApiLevel = documentApiLevel;
        mBuffer.setVersion(documentApiLevel, profiles);
    }

    /**
     * Set current version of the buffer (typically for writing)
     *
     * @param documentApiLevel
     * @param supportedOperations
     */
    public void setVersion(int documentApiLevel, @NonNull Set<Integer> supportedOperations) {
        mApiLevel = documentApiLevel;

        mBuffer.setValidOperations(supportedOperations);
    }

    /**
     * Add a matrix constant
     *
     * @param id the id of the resulting matrix
     * @param values
     * @return
     */
    public void addMatrixConst(int id, float @NonNull [] values) {
        MatrixConstant.apply(mBuffer, id, 0, values);
    }

    /**
     * Add a matrix expression
     *
     * @param id the matrix expression id
     * @param exp matrix expression
     */
    public void addMatrixExpression(int id, float @NonNull [] exp) {
        MatrixExpression.apply(mBuffer, id, 0, exp);
    }

    /**
     * Multiply a vector by a matrix
     *
     * @param matrixId id of the matrix
     * @param type type of operation (typically 0=normal, 1=projection)
     * @param from vector to multiply
     * @param outId output vector
     */
    public void addMatrixVectorMath(
            float matrixId, short type, float @NonNull [] from, int @NonNull [] outId) {
        MatrixVectorMath.apply(mBuffer, type, outId, Utils.idFromNan(matrixId), from);
    }

    /**
     * Add font data
     *
     * @param id of the font
     * @param type of the font ignored currently only 0 is supported
     * @param data font data
     */
    public void addFont(int id, int type, byte @NonNull [] data) {
        FontData.apply(mBuffer, id, type, data);
    }

    /**
     * Add a wake in command
     *
     * @param seconds time to start the render loop
     */
    public void wakeIn(float seconds) {
        WakeIn.apply(mBuffer, seconds);
    }

    /**
     * Add a path expression
     *
     * @param id output id
     * @param expressionX expression for x
     * @param expressionY expression for y
     * @param start start value
     * @param end end value
     * @param count count value
     * @param flags flags
     */
    public void addPathExpression(
            int id,
            float @NonNull [] expressionX,
            float @Nullable [] expressionY,
            float start,
            float end,
            float count,
            int flags) {
        PathExpression.apply(mBuffer, id, expressionX, expressionY, start, end, count, flags);
    }

    /**
     * Add a component visibility operation
     * @param valueId id of the value
     */
    public void addComponentVisibilityOperation(int valueId) {
        ComponentVisibilityOperation.apply(mBuffer, valueId);
    }

    /**
     * Add a width modifier operation
     * @param type type of operation
     * @param value value of the operation
     */
    public void addWidthModifierOperation(int type, float value) {
        WidthModifierOperation.apply(mBuffer,  type, value);
    }

    /**
     * Add a height modifier operation
     * @param type type of operation
     * @param value value of the operation
     */
    public void addHeightModifierOperation(int type, float value) {
        HeightModifierOperation.apply(mBuffer,  type, value);
    }

    /**
     * Add a height in modifier operation
     * @param min min value
     * @param max max value
     */
    public void addHeightInModifierOperation(float min, float max) {
        HeightInModifierOperation.apply(mBuffer,  min, max);
    }

    /**
     * Add a touch down modifier operation
     */
    public void addTouchDownModifierOperation() {
        TouchDownModifierOperation.apply(mBuffer);
    }

    /**
     * Add a touch up modifier operation
     */
    public void addTouchUpModifierOperation() {
        TouchUpModifierOperation.apply(mBuffer);
    }

    /**
     * Add a touch cancel modifier operation
     */
    public void addTouchCancelModifierOperation() {
        TouchCancelModifierOperation.apply(mBuffer);
    }

    /**
     * Add a width in modifier operation
     */
    public void addWidthInModifierOperation(float min, float max) {
        WidthInModifierOperation.apply(mBuffer,  min, max);
    }

    /**
     * Add a draw content operation
     */
    public void addDrawContentOperation() {
        DrawContentOperation.apply(mBuffer);
    }

    /**
     * Add a semantics modifier operation
     */
    public void addSemanticsModifier(int contentDescriptionId,
                                     byte role,
                                     int textId,
                                     int stateDescriptionId,
                                     int mode,
                                     boolean enabled,
                                     boolean clickable) {
        CoreSemantics.apply(
                mBuffer, contentDescriptionId,
                role,
                textId,
                stateDescriptionId,
                mode,
                enabled,
                clickable);
    }

    /**
     * Add a click modifier operation
     */
    public void addClickModifierOperation() {
        ClickModifierOperation.apply(mBuffer);
    }

    /**
     * Add a collapsible priority modifier operation
     * @param orientation orientation
     * @param priority priority
     */
    public void addCollapsiblePriorityModifier(int orientation, float priority) {
        CollapsiblePriorityModifierOperation.apply(mBuffer, orientation, priority);
    }

    /**
     * Add an animation spec modifier operation
     * @param animationId animation id
     * @param motionDuration duration of the motion
     * @param motionEasingType easing type
     * @param visibilityDuration duration of the visibility
     * @param visibilityEasingType easing type
     * @param enterAnimation enter animation
     * @param exitAnimation exit animation
     */
    public void addAnimationSpecModifier(int animationId,
                                         float motionDuration,
                                         int motionEasingType,
                                         float visibilityDuration,
                                         int visibilityEasingType,
                                         int enterAnimation,
                                         int exitAnimation) {
        AnimationSpec.apply(mBuffer,
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                enterAnimation,
                exitAnimation);
    }

    /**
     * Add a value string change action operation
     * @param destTextId dest text id
     * @param srcTextId src text id
     */
    public void addValueStringChangeActionOperation(int destTextId, int srcTextId) {
        ValueStringChangeActionOperation.apply(mBuffer, destTextId, srcTextId);
    }

    /**
     * Add a value integer expression change action operation
     * @param destIntegerId dest integer id
     * @param srcIntegerId src integer id
     */
    public void addValueIntegerExpressionChangeActionOperation(
            long destIntegerId,
            long srcIntegerId) {
        ValueIntegerExpressionChangeActionOperation.apply(mBuffer, destIntegerId, srcIntegerId);
    }

    /**
     * Add a value float change action operation
     * @param valueId dest value id
     * @param value value
     */
    public void addValueFloatChangeActionOperation(int valueId, float value) {
        ValueFloatChangeActionOperation.apply(mBuffer, valueId, value);
    }

    /**
     * Add a value integer change action operation
     * @param valueId dest value id
     * @param value value
     */
    public void addValueIntegerChangeActionOperation(int valueId, int value) {
        ValueIntegerChangeActionOperation.apply(mBuffer, valueId, value);
    }

    /**
     * Add a value float expression change action operation
     * @param mValueId dest value id
     * @param mValue value
     */
    public void addValueFloatExpressionChangeActionOperation(int mValueId, int mValue) {
        ValueFloatExpressionChangeActionOperation.apply(mBuffer, mValueId, mValue);
    }
}
