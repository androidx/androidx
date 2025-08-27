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
package androidx.compose.remote.creation;

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.toMathName;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_ABS;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_ADD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_AND;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_CLAMP;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_COPY_SIGN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_DECR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_DIV;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_IFELSE;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_INCR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MAD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MAX;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MIN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MOD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MUL;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_NEG;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_NOT;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_OR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SHL;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SHR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SIGN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SUB;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_USHR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_VAR1;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_VAR2;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_XOR;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteComposeState;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.BitmapFontData;
import androidx.compose.remote.core.operations.DataMapIds;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.PathAppend;
import androidx.compose.remote.core.operations.PathCombine;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.TextLength;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.ImageScaling;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.types.IntegerConstant;
import androidx.compose.remote.core.types.LongConstant;
import androidx.compose.remote.creation.actions.Action;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.profile.Profile;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RemoteComposeWriter {
    protected @NonNull RemoteComposeBuffer mBuffer;
    protected @NonNull RemoteComposeState mState = new RemoteComposeState();
    protected @NonNull Platform mPlatform;
    private int mOriginalWidth = 0;
    private int mOriginalHeight = 0;
    private @NonNull String mContentDescription = "";
    private boolean mHasForceSendingNewPaint = false;

    public static final float TIME_IN_CONTINUOUS_SEC = RemoteContext.FLOAT_CONTINUOUS_SEC;
    public static final int FONT_TYPE_DEFAULT = PaintBundle.FONT_TYPE_DEFAULT;
    public static final int FONT_TYPE_SANS_SERIF = PaintBundle.FONT_TYPE_SANS_SERIF;
    public static final int FONT_TYPE_SERIF = PaintBundle.FONT_TYPE_SERIF;
    public static final int FONT_TYPE_MONOSPACE = PaintBundle.FONT_TYPE_MONOSPACE;

    /**
     * Factory to obtain a RemoteComposeWriter
     *
     * @param width original size of the document
     * @param height original height of the document
     * @param contentDescription content description
     * @param profile the profile used for writing the document
     * @return a RemoteComposeWriter instance
     */
    public static @NonNull RemoteComposeWriter obtain(
            int width, int height, @NonNull String contentDescription, @NonNull Profile profile) {
        return profile.create(width, height, contentDescription);
    }

    /**
     * Factory to obtain a RemoteComposeWriter
     *
     * @param width original size of the document
     * @param height original height of the document
     * @param profile the profile used for writing the document
     * @return a RemoteComposeWriter instance
     */
    public static @NonNull RemoteComposeWriter obtain(
            int width, int height, @NonNull Profile profile) {
        return profile.create(width, height, "");
    }

    /**
     * Create a RemoteComposeWriter
     *
     * @param width original document width
     * @param height original document height
     * @param contentDescription content description
     * @param platform the platform to use
     */
    public RemoteComposeWriter(
            int width, int height, @NonNull String contentDescription, @NonNull Platform platform) {
        this.mPlatform = platform;
        mBuffer = new RemoteComposeBuffer();
        header(width, height, contentDescription, 1f, 0);
        mOriginalWidth = width;
        mOriginalHeight = height;
        mContentDescription = contentDescription;
    }

    /**
     * Create a RemoteComposeWriter
     *
     * @param width original document width
     * @param height original document height
     * @param contentDescription content description
     * @param apilLevel document api level
     * @param profiles bitmap for the profiles
     * @param platform the platform to use
     */
    public RemoteComposeWriter(
            int width,
            int height,
            @NonNull String contentDescription,
            int apilLevel,
            int profiles,
            @NonNull Platform platform) {
        this(
                platform,
                apilLevel,
                hTag(Header.DOC_WIDTH, width),
                hTag(Header.DOC_HEIGHT, height),
                hTag(Header.DOC_CONTENT_DESCRIPTION, contentDescription),
                hTag(Header.DOC_PROFILES, profiles));
    }

    /**
     * create a new RemoteComposeWriter
     *
     * @param platform the platform to use
     * @param apiLevel document api level
     * @param tags properties of the document
     */
    public RemoteComposeWriter(@NonNull Platform platform, int apiLevel, HTag @NonNull ... tags) {
        this.mPlatform = platform;
        mBuffer = new RemoteComposeBuffer(apiLevel);

        Object w = HTag.getValue(tags, Header.DOC_WIDTH);
        Object h = HTag.getValue(tags, Header.DOC_HEIGHT);
        Object d = HTag.getValue(tags, Header.DOC_CONTENT_DESCRIPTION);
        int profiles = HTag.getProfiles(tags);

        if (w instanceof Integer) {
            mOriginalWidth = (int) w;
        }
        if (h instanceof Integer) {
            mOriginalHeight = (int) h;
        }
        if (d instanceof String) {
            mContentDescription = (String) d;
        }

        mBuffer.setVersion(apiLevel, profiles);

        mBuffer.addHeader(HTag.getTags(tags), HTag.getValues(tags));
        if (apiLevel == 6 && profiles == 0) {
            int contentDescriptionId = 0;
            if (mContentDescription != null) {
                contentDescriptionId = addText(mContentDescription);
                mBuffer.addRootContentDescription(contentDescriptionId);
            }
        }
    }

    /**
     * create a new RemoteComposeWriter
     *
     * @param platform the platform to use
     * @param tags properties of the document
     */
    public RemoteComposeWriter(@NonNull Platform platform, HTag @NonNull ... tags) {
        this(platform, CoreDocument.DOCUMENT_API_LEVEL, tags);
    }

    /**
     * create a new RemoteComposeWriter
     *
     * @param profile the profile to use
     * @param buffer the buffer to use
     * @param tags properties of the document
     */
    protected RemoteComposeWriter(
            @NonNull Profile profile, @NonNull RemoteComposeBuffer buffer, HTag @NonNull ... tags) {
        this.mPlatform = profile.getPlatform();
        mBuffer = buffer;

        Object w = HTag.getValue(tags, Header.DOC_WIDTH);
        Object h = HTag.getValue(tags, Header.DOC_HEIGHT);
        Object d = HTag.getValue(tags, Header.DOC_CONTENT_DESCRIPTION);

        if (w instanceof Integer) {
            mOriginalWidth = (int) w;
        }
        if (h instanceof Integer) {
            mOriginalHeight = (int) h;
        }
        if (d instanceof String) {
            mContentDescription = (String) d;
        }

        mBuffer.addHeader(HTag.getTags(tags), HTag.getValues(tags));
    }

    protected int mMaxValidFloatExpressionOperation =
            AnimatedFloatExpression.getMaxOpForLevel(CoreDocument.DOCUMENT_API_LEVEL);

    protected boolean areFloatExpressionOperationsValid(float f) {
        if (Float.isNaN(f)) {
            int id = Utils.idFromNan(f);
            return id != 0 && id < mMaxValidFloatExpressionOperation;
        }
        return true;
    }

    /**
     * Validate the float operations also throw exception if NaN
     *
     * @param ops
     */
    protected void validateOps(float @NonNull [] ops) {
        for (int i = 0; i < ops.length; i++) {
            if (!areFloatExpressionOperationsValid(ops[i])) {
                String str = toMathName(ops[i]);
                str = "Invalid operation: " + Utils.idFromNan(ops[i]) + "(" + str + ")";
                throw new IllegalArgumentException(str);
            }
        }
    }

    /** Reset the writer */
    public void reset() {
        mCacheComponentWidthValues.clear();
        mCacheComponentHeightValues.clear();
        mBuffer.reset(1000000);
        mState.reset();
        header(mOriginalWidth, mOriginalHeight, mContentDescription, 1f, 0);
    }

    /**
     * Insert a header
     *
     * @param width the width of the document in pixels
     * @param height the height of the document in pixels
     * @param contentDescription content description of the document
     * @param density the density of the document in pixels per device pixel
     * @param capabilities bitmask indicating needed capabilities (unused for now)
     */
    public void header(
            int width,
            int height,
            @Nullable String contentDescription,
            float density,
            long capabilities) {
        mBuffer.header(width, height, density, capabilities);
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
            mBuffer.addRootContentDescription(contentDescriptionId);
        }
    }

    /**
     * @param tag the tag to use
     * @param value the value to use
     * @return a tag
     */
    public static @NonNull HTag hTag(short tag, @NonNull Object value) {
        return new HTag(tag, value);
    }

    /** Subtract the second path from the first path. */
    public static final byte COMBINE_DIFFERENCE = PathCombine.OP_DIFFERENCE;

    /** Intersect the second path with the first path. */
    public static final byte COMBINE_INTERSECT = PathCombine.OP_INTERSECT;

    /** Subtract the first path from the second path. */
    public static final byte COMBINE_REVERSE_DIFFERENCE = PathCombine.OP_REVERSE_DIFFERENCE;

    /** Union (inclusive-or) the two paths. */
    public static final byte COMBINE_UNION = PathCombine.OP_UNION;

    /** Exclusive-or the two paths. */
    public static final byte COMBINE_XOR = PathCombine.OP_XOR;

    /**
     * Combine two paths.
     *
     * @param path1 first path
     * @param path2 second path
     * @param op operation to apply
     * @return id of path
     */
    public int pathCombine(int path1, int path2, byte op) {
        int id = nextId();
        mBuffer.pathCombine(id, path1, path2, op);
        return id;
    }

    /**
     * Perform a haptic feedback
     *
     * @param feedbackConstant the vibration type
     */
    public void performHaptic(int feedbackConstant) {
        mBuffer.performHaptic(feedbackConstant);
    }

    /**
     * Returns the color attribute
     *
     * @param baseColor
     * @param type
     * @return
     */
    public float getColorAttribute(int baseColor, short type) {
        int id = mState.createNextAvailableId();
        mBuffer.getColorAttribute(id, baseColor, type);
        return Utils.asNan(id);
    }

    /**
     * Explicitly encode actions in the buffer
     *
     * @param actions
     */
    public void addAction(Action @NonNull ... actions) {
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            action.write(this);
        }
    }

    /**
     * Returns a substring
     *
     * @param txtId
     * @param start
     * @param len
     * @return
     */
    public int textSubtext(int txtId, float start, float len) {
        int id = mState.createNextAvailableId();
        mBuffer.textSubtext(id, txtId, start, len);
        return id;
    }

    /**
     * Measure bitmap font text dimensions.
     *
     * @param textId
     * @param bmFontId
     * @param measureWidth
     * @return float id of the property
     */
    public float bitmapTextMeasure(int textId, int bmFontId, int measureWidth) {
        int id = mState.createNextAvailableId();
        mBuffer.bitmapTextMeasure(id, textId, bmFontId, measureWidth);
        return Utils.asNan(id);
    }

    /**
     * Matrix multiply operation
     *
     * @param matrixId id of the matrix
     * @param from input vector
     * @param out output vector
     */
    public void addMatrixMultiply(float matrixId, float @Nullable [] from, float @Nullable [] out) {
        addMatrixMultiply(matrixId, (short) 0, from, out);
    }

    /**
     * Matrix multiply operation (with additional flags) supports 1 = perspective (2 will be
     * multiply with the inverse)
     *
     * @param matrixId id of the matrix
     * @param type 0 = normal multiply , 1 is multiply with perspective
     * @param from input vector
     * @param out output vector
     */
    public void addMatrixMultiply(
            float matrixId, short type, float @Nullable [] from, float @NonNull [] out) {
        int[] outId = new int[out.length];
        for (int i = 0; i < out.length; i++) {
            outId[i] = mState.createNextAvailableId();
            out[i] = Utils.asNan(outId[i]);
        }
        mBuffer.addMatrixVectorMath(matrixId, type, from, outId);
    }

    /**
     * Returns the value of force sending new paint flag and clears it.
     *
     * @return value of the force new paint flag
     */
    public boolean checkAndClearForceSendingNewPaint() {
        boolean result = mHasForceSendingNewPaint;
        mHasForceSendingNewPaint = false;
        return result;
    }

    /**
     * Tell the system to wake up in a given number of seconds or sooner
     *
     * @param seconds number of seconds to wake up
     */
    public void wakeIn(float seconds) {
        mBuffer.wakeIn(seconds);
    }

    /**
     * Add a path expression
     *
     * @param expressionX the x expression
     * @param expressionY the y expression
     * @param start the start value
     * @param end the end value
     * @param count the number of points
     * @param flags the flags
     * @return the id of the path
     */
    public int addPathExpression(
            float @NonNull [] expressionX,
            float @NonNull [] expressionY,
            float start,
            float end,
            float count,
            int flags) {

        int id = mState.createNextAvailableId();
        mBuffer.addPathExpression(id, expressionX, expressionY, start, end, count, flags);
        return id;
    }

    /**
     * Add a polar path expression
     *
     * @param expressionR the radius expression
     * @param start the start value
     * @param end the end value
     * @param count the number of points
     * @param centerX the center x
     * @param centerY the center y
     * @param flags the flags
     * @return the id of the path
     */
    public int addPolarPathExpression(
            float @NonNull [] expressionR,
            float start,
            float end,
            float count,
            float centerX,
            float centerY,
            int flags) {

        int id = mState.createNextAvailableId();
        mBuffer.addPathExpression(
                id,
                expressionR,
                new float[] {centerX, centerY},
                start,
                end,
                count,
                Rc.PathExpression.POLAR_PATH | flags);
        return id;
    }

    /** Used to create the tag values in the header */
    public static class HTag {
        @NonNull Short mTag;
        @NonNull Object mValue;

        /**
         * Create a tag
         *
         * @param tag the tag to use
         * @param value the value to use
         */
        public HTag(@NonNull Short tag, @NonNull Object value) {
            mTag = tag;
            mValue = value;
        }

        /**
         * Returns the profiles bitmask
         *
         * @param tags list of tags
         * @return profiles bitmask
         */
        static int getProfiles(HTag @NonNull [] tags) {
            int profiles = 0;
            for (int i = 0; i < tags.length; i++) {
                if (tags[i].mTag == Header.DOC_PROFILES) {
                    return (Integer) tags[i].mValue;
                }
            }
            return profiles;
        }

        /**
         * Returns the integer value
         *
         * @param tags list of tags
         * @param tag the specific tag to look for
         * @return found value, null if not
         */
        static @Nullable Object getValue(HTag @NonNull [] tags, int tag) {
            for (int i = 0; i < tags.length; i++) {
                if (tags[i].mTag == tag) {
                    return tags[i].mValue;
                }
            }
            return null;
        }

        static short @NonNull [] getTags(HTag @NonNull [] tags) {
            short[] ret = new short[tags.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = tags[i].mTag;
            }
            return ret;
        }

        static Object @NonNull [] getValues(HTag @NonNull [] tags) {
            Object[] ret = new Object[tags.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = tags[i].mValue;
            }
            return ret;
        }
    }

    /** Returns the internal byte buffer. This should be used along with bufferSize(). */
    public byte @NonNull [] buffer() {
        return mBuffer.getBuffer().getBuffer();
    }

    /** Returns the length of the internal byte buffer */
    public int bufferSize() {
        return mBuffer.getBuffer().getSize();
    }

    public @NonNull RemoteComposeBuffer getBuffer() {
        return mBuffer;
    }

    /**
     * Create a shader from a string
     *
     * @param shaderString the text representation of the shader
     * @return a RemoteComposeShader
     */
    public @NonNull RemoteComposeShader createShader(@NonNull String shaderString) {
        return new RemoteComposeShader(shaderString, this);
    }

    /**
     * Set a current theme, applied to the following operations in the document. This can be used to
     * "tag" the following operations to a given theme. On playback, we can then filter operations
     * depending on the chosen theme.
     *
     * @param theme the theme we are interested in: - Theme.UNSPECIFIED - Theme.DARK - Theme.LIGHT
     */
    public void setTheme(int theme) {
        mBuffer.setTheme(theme);
    }

    /**
     * Add a bitmap to the document
     *
     * @param image a Bitmap object
     * @param width bitmap width
     * @param height bitmap height
     * @param contentDescription a description for the image
     */
    public void drawBitmap(@NonNull Object image, int width, int height,
            @Nullable String contentDescription) {
        int imageId = storeBitmap(image);
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        mBuffer.drawBitmap(
                imageId,
                width,
                height,
                0,
                0,
                width,
                height,
                0,
                0,
                width,
                height,
                contentDescriptionId);
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
        mBuffer.setRootContentBehavior(scroll, alignment, sizing, mode);
    }

    /**
     * Add a click area to the document, in root coordinates. We are not doing any specific sorting
     * through the declared areas on click detections, which means that the first one containing the
     * click coordinates will be the one reported; the order of addition of those click areas is
     * therefore meaningful.
     *
     * @param id the id of the area, which will be reported on click
     * @param contentDescription the content description of that click area (accessibility)
     * @param left the left coordinate of the click area (in pixels)
     * @param top the top coordinate of the click area (in pixels)
     * @param right the right coordinate of the click area (in pixels)
     * @param bottom the bottom coordinate of the click area (in pixels)
     * @param metadata arbitrary metadata associated with the are, also reported on click
     */
    public void addClickArea(
            int id,
            @Nullable String contentDescription,
            float left,
            float top,
            float right,
            float bottom,
            @Nullable String metadata) {
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        int metadataId = 0;
        if (metadata != null) {
            metadataId = addText(metadata);
        }
        mBuffer.addClickArea(id, contentDescriptionId, left, top, right, bottom, metadataId);
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
    public void drawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        mBuffer.addDrawArc(left, top, right, bottom, startAngle, sweepAngle);
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
    public void drawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        mBuffer.addDrawSector(left, top, right, bottom, startAngle, sweepAngle);
    }

    /**
     * @param image The bitmap to be drawn
     * @param left left coordinate of rectangle that the bitmap will be to fit into
     * @param top top coordinate of rectangle that the bitmap will be to fit into
     * @param right right coordinate of rectangle that the bitmap will be to fit into
     * @param bottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param contentDescription content description of the image
     */
    public void drawBitmap(
            @NonNull Object image,
            float left,
            float top,
            float right,
            float bottom,
            @Nullable String contentDescription) {
        int imageId = storeBitmap(image);
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        mBuffer.addDrawBitmap(imageId, left, top, right, bottom, contentDescriptionId);
    }

    /**
     * @param imageId The id of the bitmap to be drawn
     * @param left left coordinate of rectangle that the bitmap will be to fit into
     * @param top top coordinate of rectangle that the bitmap will be to fit into
     * @param right right coordinate of rectangle that the bitmap will be to fit into
     * @param bottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param contentDescription content description of the image
     */
    public void drawBitmap(
            int imageId,
            float left,
            float top,
            float right,
            float bottom,
            @Nullable String contentDescription) {
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        mBuffer.addDrawBitmap(imageId, left, top, right, bottom, contentDescriptionId);
    }

    /**
     * @param imageId The id of the bitmap to be drawn
     * @param left left coordinate of rectangle that the bitmap will be to fit into
     * @param top top coordinate of rectangle that the bitmap will be to fit into
     * @param contentDescription content description of the image
     */
    public void drawBitmap(
            int imageId, float left, float top, @Nullable String contentDescription) {
        int imageWidth = mPlatform.getImageWidth(imageId);
        int imageHeight = mPlatform.getImageHeight(imageId);
        drawBitmap(imageId, left, top, imageWidth, imageHeight, contentDescription);
    }

    /** No scaling is applied */
    public static final int IMAGE_SCALE_NONE = ImageScaling.SCALE_NONE;

    /** reduce images uniformly and contained in the destination */
    public static final int IMAGE_SCALE_INSIDE = ImageScaling.SCALE_INSIDE;

    /** reduce images uniformly such that the width fits the destination */
    public static final int IMAGE_SCALE_FILL_WIDTH = ImageScaling.SCALE_FILL_WIDTH;

    /** reduce images uniformly such that the height fits the destination */
    public static final int IMAGE_SCALE_FILL_HEIGHT = ImageScaling.SCALE_FILL_HEIGHT;

    /** Scale images uniformly such that the height & height fits the destination */
    public static final int IMAGE_SCALE_FIT = ImageScaling.SCALE_FIT;

    /** crop images uniformly such that the height & height fits the destination */
    public static final int IMAGE_SCALE_CROP = ImageScaling.SCALE_CROP;

    /** crop images uniformly such that the height & height fits the destination */
    public static final int IMAGE_SCALE_FILL_BOUNDS = ImageScaling.SCALE_FILL_BOUNDS;

    /** scale images by scaleFacto destination */
    public static final int IMAGE_SCALE_FIXED_SCALE = ImageScaling.SCALE_FIXED_SCALE;

    public static final int IMAGE_REFERENCE = 1 << 8;

    /**
     * @param image The bitmap to be drawn
     * @param srcLeft left coordinate of rectangle that the bitmap will be to fit into
     * @param srcTop top coordinate of rectangle that the bitmap will be to fit into
     * @param srcRight right coordinate of rectangle that the bitmap will be to fit into
     * @param srcBottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param dstLeft left coordinate of rectangle that the bitmap will be to fit into
     * @param dstTop top coordinate of rectangle that the bitmap will be to fit into
     * @param dstRight right coordinate of rectangle that the bitmap will be to fit into
     * @param dstBottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param scaleType Scale TYPE IMAGE_SCALE_NONE, IMAGE_SCALE_INSIDE etc.
     * @param scaleFactor scale image when ScaleType is IMAGE_SCALE_FIXED_SCALE
     * @param contentDescription content description of the image
     */
    public void drawScaledBitmap(
            @NonNull Object image,
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
            @Nullable String contentDescription) {
        int imageId = storeBitmap(image);
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        mBuffer.drawScaledBitmap(
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
     * @param imageId The id of the bitmap to be drawn
     * @param srcLeft left coordinate of rectangle that the bitmap will be to fit into
     * @param srcTop top coordinate of rectangle that the bitmap will be to fit into
     * @param srcRight right coordinate of rectangle that the bitmap will be to fit into
     * @param srcBottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param dstLeft left coordinate of rectangle that the bitmap will be to fit into
     * @param dstTop top coordinate of rectangle that the bitmap will be to fit into
     * @param dstRight right coordinate of rectangle that the bitmap will be to fit into
     * @param dstBottom bottom coordinate of rectangle that the bitmap will be to fit into
     * @param scaleType Scale TYPE
     * @param scaleFactor scale image when ScaleType is IMAGE_SCALE_FIXED_SCALE
     * @param contentDescription content description of the image
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
            @Nullable String contentDescription) {
        int contentDescriptionId = 0;
        if (contentDescription != null) {
            contentDescriptionId = addText(contentDescription);
        }
        mBuffer.drawScaledBitmap(
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
     * Draw the specified circle using the specified paint. If radius is <= 0, then nothing will be
     * drawn.
     *
     * @param centerX The x-coordinate of the center of the circle to be drawn
     * @param centerY The y-coordinate of the center of the circle to be drawn
     * @param radius The radius of the circle to be drawn
     */
    public void drawCircle(float centerX, float centerY, float radius) {
        mBuffer.addDrawCircle(centerX, centerY, radius);
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
    public void drawLine(float x1, float y1, float x2, float y2) {
        mBuffer.addDrawLine(x1, y1, x2, y2);
    }

    /**
     * Draw the specified oval using the specified paint.
     *
     * @param left left coordinate of oval
     * @param top top coordinate of oval
     * @param right right coordinate of oval
     * @param bottom bottom coordinate of oval
     */
    public void drawOval(float left, float top, float right, float bottom) {
        mBuffer.addDrawOval(left, top, right, bottom);
    }

    /**
     * Draw the specified path
     *
     * <p>Note: path objects are not immutable modifying them and calling this will not change the
     * drawing
     *
     * @param path The path to be drawn
     */
    public void drawPath(@NonNull Object path) {
        int id = mState.dataGetId(path);
        if (id == -1) { // never been seen before
            id = addPathData(path);
        }
        mBuffer.addDrawPath(id);
    }

    /**
     * Draw the specified path
     *
     * @param pathId pathId of path to be drawn
     */
    public void drawPath(int pathId) {
        mBuffer.addDrawPath(pathId);
    }

    /**
     * Draw the specified Rect
     *
     * @param left left coordinate of rectangle to be drawn
     * @param top top coordinate of rectangle to be drawn
     * @param right right coordinate of rectangle to be drawn
     * @param bottom bottom coordinate of rectangle to be drawn
     */
    public void drawRect(float left, float top, float right, float bottom) {
        mBuffer.addDrawRect(left, top, right, bottom);
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
    public void drawRoundRect(
            float left, float top, float right, float bottom, float radiusX, float radiusY) {
        mBuffer.addDrawRoundRect(left, top, right, bottom, radiusX, radiusY);
    }

    /**
     * This creates text id from text. It can be used with methods that take String or textId.
     *
     * @param text string
     * @return id
     */
    public int textCreateId(@NonNull String text) {
        return addText(text);
    }

    /**
     * Merge two text defined by ids
     *
     * @param id1 Id of first text
     * @param id2 Id of second text
     * @return the id of the merged text
     */
    public int textMerge(int id1, int id2) {
        int textId = nextId();
        return mBuffer.textMerge(textId, id1, id2);
    }

    /**
     * Draw the text, with origin at (x,y) along the specified path.
     *
     * @param text The text to be drawn
     * @param path The path the text should follow for its baseline
     * @param hOffset The distance along the path to add to the text's starting position
     * @param vOffset The distance above(-) or below(+) the path to position the text
     */
    public void drawTextOnPath(
            @NonNull String text, @NonNull Object path, float hOffset, float vOffset) {
        int textId = addText(text);
        drawTextOnPath(textId, path, hOffset, vOffset);
    }

    /**
     * Draw the text, with origin at (x,y) along the specified path.
     *
     * @param textId The text to be drawn
     * @param path The path the text should follow for its baseline
     * @param hOffset The distance along the path to add to the text's starting position
     * @param vOffset The distance above(-) or below(+) the path to position the text
     */
    public void drawTextOnPath(int textId, @NonNull Object path, float hOffset, float vOffset) {
        int pathId = mState.dataGetId(path);
        if (pathId == -1) { // never been seen before
            pathId = addPathData(path);
        }
        mBuffer.addDrawTextOnPath(textId, pathId, hOffset, vOffset);
    }

    /**
     * Draw the text, with origin at (x,y). The origin is interpreted based on the Align setting in
     * the paint.
     *
     * @param text The text to be drawn
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param contextStart the start of the context
     * @param contextEnd the end of the context
     * @param x The x-coordinate of the origin of the text being drawn
     * @param y The y-coordinate of the baseline of the text being drawn
     * @param rtl Draw RTTL
     */
    public void drawTextRun(
            @NonNull String text,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            @NonNull Boolean rtl) {
        int textId = addText(text);
        mBuffer.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl);
    }

    /**
     * Draw the text, with origin at (x,y). The origin is interpreted based on the Align setting in
     * the paint.
     *
     * @param textId The id of the text to be drawn
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param contextStart the start of the context
     * @param contextEnd the end of the context
     * @param x The x-coordinate of the origin of the text being drawn
     * @param y The y-coordinate of the baseline of the text being drawn
     * @param rtl Draw RTTL
     */
    public void drawTextRun(
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            @NonNull Boolean rtl) {
        mBuffer.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl);
    }

    /**
     * Draw the text, with origin at (x,y). The origin is interpreted based on the Align setting in
     * the paint.
     *
     * @param textId The id of the text to be drawn
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param x The x-coordinate of the origin of the text being drawn
     * @param y The y-coordinate of the baseline of the text being drawn
     */
    public void drawBitmapFontTextRun(
            int textId, int bitmapFontId, int start, int end, float x, float y) {
        mBuffer.addDrawBitmapFontTextRun(textId, bitmapFontId, start, end, x, y);
    }

    /**
     * Draw the text along the path.
     *
     * @param textId The id of the text to be drawn
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param path The path to draw along
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param yAdj Adjustment away from the path along the normal at that point
     */
    public void drawBitmapFontTextRunOnPath(
            int textId, int bitmapFontId, @NonNull Object path, int start, int end, float yAdj) {
        int pathId = mState.dataGetId(path);
        if (pathId == -1) { // never been seen before
            pathId = addPathData(path);
        }
        mBuffer.addDrawBitmapFontTextRunOnPath(textId, bitmapFontId, pathId, start, end, yAdj);
    }

    /**
     * Draw a text on canvas at relative to position (x, y), offset panX and panY.
     *
     * <p>The panning factors (panX, panY) mapped to the resulting bounding box of the text, in such
     * a way that a panning factor of (0.0, 0.0) would center the text at (x, y) * Panning of -1.0,
     * -1.0 - the text above & right of x,y. * Panning of 1.0, 1.0 - the text is below and to the
     * left * Panning of 1.0, 0.0 - the test is centered & to the right of x,y
     *
     * <p>Setting panY to NaN results in y being the baseline of the text.
     *
     * @param str text to draw
     * @param x Coordinate of the Anchor
     * @param y Coordinate of the Anchor
     * @param panX justifies the text -1.0=right, 0.0=center, 1.0=left
     * @param panY position text -1.0=above, 0.0=center, 1.0=below, Nan=baseline
     * @param flags 1 = RTL
     */
    public void drawTextAnchored(
            @NonNull String str, float x, float y, float panX, float panY, int flags) {
        int textId = addText(str);
        mBuffer.drawTextAnchored(textId, x, y, panX, panY, flags);
    }

    /**
     * Draw a text on canvas at relative to position (x, y), offset panX and panY.
     *
     * <p>The panning factors (panX, panY) mapped to the resulting bounding box of the text, in such
     * a way that a panning factor of (0.0, 0.0) would center the text at (x, y) * Panning of -1.0,
     * -1.0 - the text above & right of x,y. * Panning of 1.0, 1.0 - the text is below and to the
     * left * Panning of 1.0, 0.0 - the test is centered & to the right of x,y
     *
     * <p>Setting panY to NaN results in y being the baseline of the text.
     *
     * @param strId text to draw
     * @param x Coordinate of the Anchor
     * @param y Coordinate of the Anchor
     * @param panX justifies the text -1.0=right, 0.0=center, 1.0=left
     * @param panY position text -1.0=above, 0.0=center, 1.0=below, Nan=baseline
     * @param flags 1 = RTL 2 = MONOSPACE_MEASURE
     */
    public void drawTextAnchored(int strId, float x, float y, float panX, float panY, int flags) {
        mBuffer.drawTextAnchored(strId, x, y, panX, panY, flags);
    }

    /**
     * draw an interpolation between two paths that have the same pattern
     *
     * <p>Warning paths objects are not immutable and this is not taken into consideration
     *
     * @param path1 The path1 to be drawn between
     * @param path2 The path2 to be drawn between
     * @param tween The ratio of path1 and path2 to 0 = all path 1, 1 = all path2
     * @param start The start of the subrange of paths to draw 0 = start form start 0.5 is half way
     * @param stop The end of the subrange of paths to draw 1 = end at the end 0.5 is end half way
     */
    public void drawTweenPath(
            @NonNull Object path1, @NonNull Object path2, float tween, float start, float stop) {
        int path1Id = mState.dataGetId(path1);
        if (path1Id == -1) { // never been seen before
            path1Id = addPathData(path1);
        }
        int path2Id = mState.dataGetId(path2);
        if (path2Id == -1) { // never been seen before
            path2Id = addPathData(path2);
        }
        mBuffer.addDrawTweenPath(path1Id, path2Id, tween, start, stop);
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
     * @param text text to draw
     * @param bitmapFontId The id of the bitmap font to draw with
     * @param x Coordinate of the Anchor
     * @param y Coordinate of the Anchor
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param panX justifies text -1.0=right, 0.0=center, 1.0=left
     * @param panY position text -1.0=above, 0.0=center, 1.0=below, Nan=baseline
     */
    public void drawBitmapTextAnchored(
            @NonNull String text,
            int bitmapFontId,
            float start,
            float end,
            float x,
            float y,
            float panX,
            float panY) {
        int textId = addText(text);
        mBuffer.drawBitmapTextAnchored(textId, bitmapFontId, start, end, x, y, panX, panY);
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
        mBuffer.drawBitmapTextAnchored(textId, bitmapFontId, start, end, x, y, panX, panY);
    }

    /**
     * draw an interpolation between two paths that have the same pattern
     *
     * @param path1Id The path1 to be drawn between
     * @param path2Id The path2 to be drawn between
     * @param tween The ratio of path1 and path2 to 0 = all path 1, 1 = all path2
     * @param start The start of the subrange of paths to draw 0 = start form start 0.5 is half way
     * @param stop The end of the subrange of paths to draw 1 = end at the end 0.5 is end half way
     */
    public void drawTweenPath(int path1Id, int path2Id, float tween, float start, float stop) {
        mBuffer.addDrawTweenPath(path1Id, path2Id, tween, start, stop);
    }

    /**
     * Add an android Path object. (It is converted to internal path)
     *
     * @param path Android Path object
     * @return id of the path object to be used by drawPath, etc.
     */
    public int addPathData(@NonNull Object path) {
        float[] pathData = mPlatform.pathToFloatArray(path);
        int id = mState.cacheData(path);
        if (pathData == null) {
            throw new IllegalArgumentException("Invalid path data");
        }
        return mBuffer.addPathData(id, pathData);
    }

    /**
     * Add an android Path object. (It is converted to internal path)
     *
     * @param path Android Path object
     * @return id of the path object to be used by drawPath, etc.
     */
    public int addPathData(@NonNull Object path, int winding) {
        float[] pathData = mPlatform.pathToFloatArray(path);
        int id = mState.cacheData(path);
        if (pathData == null) {
            throw new IllegalArgumentException("Invalid path data");
        }
        return mBuffer.addPathData(id, pathData, winding);
    }

    /**
     * interpolate the two paths to produce a 3rd
     *
     * @param pid1 the first path
     * @param pid2 the second path
     * @param tween path is the path1+(pat2-path1)*tween
     * @return id of the tweened path
     */
    public int pathTween(int pid1, int pid2, float tween) {
        int out = mState.createNextAvailableId();
        return mBuffer.pathTween(out, pid1, pid2, tween);
    }

    /**
     * create a path object.
     *
     * @param x the initial p
     * @param y
     * @return
     */
    public int pathCreate(float x, float y) {
        int out = mState.createNextAvailableId();
        return mBuffer.pathCreate(out, x, y);
    }

    /**
     * append to the path object
     *
     * @param pathId
     * @param path
     */
    public void pathAppend(int pathId, float @NonNull ... path) {
        mBuffer.pathAppend(pathId, path);
    }

    /**
     * @param pathId
     * @param x
     * @param y
     */
    public void pathAppendLineTo(int pathId, float x, float y) {
        mBuffer.pathAppend(pathId, PathAppend.LINE_NAN, 0, 0, x, y);
    }

    /**
     * append quadratic bezier from the last point, approaching control point (x1,y1), and ending at
     * (x2,y2).
     *
     * @param pathId the path id
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void pathAppendQuadTo(int pathId, float x1, float y1, float x2, float y2) {
        mBuffer.pathAppend(pathId, PathAppend.QUADRATIC_NAN, 0, 0, x1, y1, x2, y2);
    }

    /**
     * add a MoveTo to the path
     *
     * @param pathId the path id
     * @param x The amount to add to the x-coordinate of the previous point on this contour, to
     *     specify a line
     * @param y The amount to add to the x-coordinate of the previous point on this contour, to
     *     specify a line
     */
    public void pathAppendMoveTo(int pathId, float x, float y) {
        mBuffer.pathAppend(pathId, PathAppend.MOVE_NAN, x, y);
    }

    /**
     * @param pathId
     */
    public void pathAppendClose(int pathId) {
        mBuffer.pathAppend(pathId, PathAppend.CLOSE_NAN);
    }

    /**
     * @param pathId
     */
    public void pathAppendReset(int pathId) {
        mBuffer.pathAppend(pathId, PathAppend.RESET_NAN);
    }

    /**
     * Add an Svg Path descriptions string. (It is converted to internal path)
     *
     * @param path SVG style Path String
     * @return id of the path object to be used by drawPath, etc.
     */
    public int addPathString(@NonNull String path) {
        return addPathData(mPlatform.parsePath(path));
    }

    /**
     * Pre-concat the current matrix with the specified skew.
     *
     * @param skewX The amount to skew in X
     * @param skewY The amount to skew in Y
     */
    public void skew(float skewX, float skewY) {
        mBuffer.addMatrixSkew(skewX, skewY);
    }

    /**
     * Pre-concat the current matrix with the specified rotation.
     *
     * @param angle The amount to rotate, in degrees
     * @param centerX The x-coord for the pivot point (unchanged by the rotation)
     * @param centerY The y-coord for the pivot point (unchanged by the rotation)
     */
    public void rotate(float angle, float centerX, float centerY) {
        mBuffer.addMatrixRotate(angle, centerX, centerY);
    }

    /**
     * Pre-concat the current matrix with the specified rotation.
     *
     * @param angle The amount to rotate, in degrees
     */
    public void rotate(float angle) {
        mBuffer.addMatrixRotate(angle, Float.NaN, Float.NaN);
    }

    /**
     * set the Matrix relative to the path
     *
     * @param pathId the id of the path object
     * @param fraction the position on path
     * @param vOffset the vertical offset to position the string
     * @param flags flags to set path 1=position only , 2 = Tangent
     */
    public void matrixFromPath(int pathId, float fraction, float vOffset, int flags) {
        mBuffer.setMatrixFromPath(pathId, fraction, vOffset, flags);
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     *
     * <p>Subsequent calls to translate,scale,rotate,skew,concat or clipRect, clipPath will all
     * operate as usual, but when the balancing call to restore() is made, those calls will be
     * forgotten, and the settings that existed before the save() will be reinstated.
     */
    public void save() {
        mBuffer.addMatrixSave();
    }

    /**
     * This call balances a previous call to save(), and is used to remove all modifications to the
     * matrix/clip state since the last save call. Do not call restore() more times than save() was
     * called.
     */
    public void restore() {
        mBuffer.addMatrixRestore();
    }

    /**
     * Preconcat the current matrix with the specified translation
     *
     * @param dx The distance to translate in X
     * @param dy The distance to translate in Y
     */
    public void translate(float dx, float dy) {
        mBuffer.addMatrixTranslate(dx, dy);
    }

    /**
     * Preconcat the current matrix with the specified scale.
     *
     * @param scaleX The amount to scale in X = 1.0f
     * @param scaleY The amount to scale in Y = 1.0f
     * @param centerX The x-coord for the pivot point (unchanged by the scale) = Float.NaN
     * @param centerY The y-coord for the pivot point (unchanged by the scale) = Float.NaN
     */
    public void scale(float scaleX, float scaleY, float centerX, float centerY) {
        mBuffer.addMatrixScale(scaleX, scaleY, centerX, centerY);
    }

    /**
     * Preconcat the current matrix with the specified scale.
     *
     * @param scaleX The amount to scale in X
     * @param scaleY The amount to scale in Y
     */
    public void scale(float scaleX, float scaleY) {
        mBuffer.addMatrixScale(scaleX, scaleY);
    }

    /**
     * Sets the clip path. Subsequent draw calls will be clipped by this path clip cpath is removed
     * by matrixClear
     *
     * @param pathId the id of the path object
     */
    public void addClipPath(int pathId) {
        mBuffer.addClipPath(pathId);
    }

    /**
     * Sets the clip. Subsequent draw calls will be clipped by this rect clip is removed by
     * matrixClear
     *
     * @param left the left of the rectangle
     * @param top the top of the rectangle
     * @param right the right of the rectangle
     * @param bottom the bottom of the rectangle
     */
    public void clipRect(float left, float top, float right, float bottom) {
        mBuffer.addClipRect(left, top, right, bottom);
    }

    /**
     * Add a float constant that can be referenced The main use would be to create a named float for
     * latter access
     *
     * @param value the value of the float
     * @return the id of a float as a Nan
     */
    public @NonNull Float addFloatConstant(float value) {
        int id = mState.cacheFloat(value);
        return mBuffer.addFloat(id, value);
    }

    /**
     * Reserve a float and returns a NaN number pointing to that float
     *
     * @return the nan id of float
     */
    public float reserveFloatVariable() {
        int id = mState.createNextAvailableId();
        return Utils.asNan(id);
    }

    @NonNull HashMap<@NonNull Integer, @NonNull Float> mCacheComponentWidthValues =
            new HashMap<Integer, Float>();
    @NonNull HashMap<@NonNull Integer, @NonNull Float> mCacheComponentHeightValues =
            new HashMap<Integer, Float>();

    /**
     * Add a float constant representing the current component width
     *
     * @return float NaN containing the id
     */
    public @NonNull Float addComponentWidthValue() {
        if (mCacheComponentWidthValues.containsKey(mBuffer.getLastComponentId())) {
            return mCacheComponentWidthValues.get(mBuffer.getLastComponentId());
        }
        float id = reserveFloatVariable();
        mBuffer.addComponentWidthValue(Utils.idFromNan(id));
        mCacheComponentWidthValues.put(mBuffer.getLastComponentId(), id);
        return id;
    }

    /**
     * Add a float constant representing the current component height
     *
     * @return float NaN containing the id
     */
    public @NonNull Float addComponentHeightValue() {
        if (mCacheComponentHeightValues.containsKey(mBuffer.getLastComponentId())) {
            return mCacheComponentHeightValues.get(mBuffer.getLastComponentId());
        }
        float id = reserveFloatVariable();
        mBuffer.addComponentHeightValue(Utils.idFromNan(id));
        mCacheComponentHeightValues.put(mBuffer.getLastComponentId(), id);
        return id;
    }

    /**
     * Add a color constant. This can be overridden to be used in theming and named using
     * setColorName()
     *
     * @param color the ARGB int of the color
     * @return the id of the color
     */
    public int addColor(int color) {
        int id = mState.createNextAvailableId();
        mBuffer.addColor(id, color);
        return id;
    }

    /**
     * @param name The String representing the name of the color
     * @param color the ARGB int of the color
     * @return the id of the Color
     */
    public int addNamedColor(@NonNull String name, int color) {
        int id = addColor(color);
        mBuffer.setNamedVariable(id, name, NamedVariable.COLOR_TYPE);
        return id;
    }

    /**
     * set the name of a color associated with an ID
     *
     * @param id the id to name
     * @param name the name of the color
     */
    public void setColorName(int id, @NonNull String name) {
        mBuffer.setNamedVariable(id, name, NamedVariable.COLOR_TYPE);
    }

    /**
     * Set the name of the string associated with the id
     *
     * @param id of the string
     * @param name name of the string
     */
    public void setStringName(int id, @NonNull String name) {
        mBuffer.setNamedVariable(id, name, NamedVariable.STRING_TYPE);
    }

    /**
     * Adds a named string with an initial value.
     *
     * @param name The String representing the name of the String
     * @param initialValue The initial value of the String
     * @return a float encoding the id
     */
    public int addNamedString(@NonNull String name, @NonNull String initialValue) {
        int id = mState.createNextAvailableId();
        mBuffer.setNamedVariable(id, name, NamedVariable.STRING_TYPE);
        TextData.apply(mBuffer.getBuffer(), id, initialValue);
        return id;
    }

    /**
     * Adds a named int with an initial value.
     *
     * @param name The String representing the name of the float
     * @param initialValue The initial value of the float
     * @return a float encoding the id
     */
    public long addNamedInt(@NonNull String name, int initialValue) {
        int id = mState.createNextAvailableId();
        mBuffer.setNamedVariable(id, name, NamedVariable.INT_TYPE);
        IntegerConstant.apply(mBuffer.getBuffer(), id, initialValue);
        mState.updateInteger(id, initialValue);
        return (long) id + 0x100000000L;
    }

    /**
     * Adds a named float with an initial value.
     *
     * @param name The String representing the name of the float
     * @param initialValue The initial value of the float
     * @return a float encoding the id
     */
    public float addNamedFloat(@NonNull String name, float initialValue) {
        int id = mState.createNextAvailableId();
        mBuffer.setNamedVariable(id, name, NamedVariable.FLOAT_TYPE);
        FloatConstant.apply(mBuffer.getBuffer(), id, initialValue);
        mState.updateFloat(id, initialValue);
        return Utils.asNan(id);
    }

    /**
     * @param name The String representing the name of the Bitmap.
     * @param initialValue the initial Bitmap
     * @return the id of the Bitmap
     */
    public int addNamedBitmap(@NonNull String name, @NonNull Object initialValue) {
        int id = storeBitmap(initialValue);
        mBuffer.setNamedVariable(id, name, NamedVariable.IMAGE_TYPE);
        mState.updateObject(id, initialValue);
        return id;
    }

    /**
     * Set the name of the long associated with the id
     *
     * @param name the name of the long
     * @param initialValue The initial value of the named long
     * @return the id of the named long
     */
    public int addNamedLong(@NonNull String name, long initialValue) {
        int id = mState.createNextAvailableId();
        mBuffer.setNamedVariable(id, name, NamedVariable.LONG_TYPE);
        LongConstant.apply(mBuffer.getBuffer(), id, initialValue);
        return id;
    }

    /**
     * Add a color expression that is an interpolation between two colors
     *
     * @param color1 First color
     * @param color2 Second color
     * @param tween the ratio between the first and the second color
     * @return id of a color
     */
    public short addColorExpression(int color1, int color2, float tween) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, color1, color2, tween);
        return (short) id;
    }

    /**
     * Add a color expression that is an interpolation between two colors
     *
     * @param colorId1 First color as an id
     * @param color2 Second color
     * @param tween the ratio between the first and the second color
     * @return id of a color
     */
    public short addColorExpression(short colorId1, int color2, float tween) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, colorId1, color2, tween);
        return (short) id;
    }

    /**
     * Add a color expression that is an interpolation between two colors
     *
     * @param color1 First color
     * @param colorId2 Second color as an id
     * @param tween interpolate between the two colors
     * @return The id of the color
     */
    public short addColorExpression(int color1, short colorId2, float tween) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, color1, colorId2, tween);
        return (short) id;
    }

    /**
     * Add a color expression that is an interpolation between two colors
     *
     * @param colorId1 First color as an id
     * @param colorId2 Second color as an id
     * @param tween the ratio between the first and the second color
     * @return the id of the color
     */
    public short addColorExpression(short colorId1, short colorId2, float tween) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, colorId1, colorId2, tween);
        return (short) id;
    }

    /**
     * Add a color expression that is based on HSV
     *
     * @param hue the color hue
     * @param sat the color saturation
     * @param value the color value
     * @return the id of the color
     */
    public short addColorExpression(float hue, float sat, float value) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, hue, sat, value);
        return (short) id;
    }

    /**
     * Add a color expression from HSV and Alpha
     *
     * @param alpha the transparency of the color 0 is transparent
     * @param hue the color hue
     * @param sat the color saturation
     * @param value the color value
     * @return the id of the color
     */
    public short addColorExpression(int alpha, float hue, float sat, float value) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, alpha, hue, sat, value);
        return (short) id;
    }

    /**
     * Add a color expression from RGB and Alpha
     *
     * @param alpha the transparency of the color 0 is transparent
     * @param red the color hue
     * @param green the color saturation
     * @param blue the color value
     * @return the id of the color
     */
    public short addColorExpression(float alpha, float red, float green, float blue) {
        int id = mState.createNextAvailableId();
        mBuffer.addColorExpression(id, alpha, red, green, blue);
        return (short) id;
    }

    /**
     * Create an animated float based on a reverse-Polish notation expression
     *
     * @param value Combination
     * @return the id of the expression as a Nan float
     */
    public @NonNull Float floatExpression(float @NonNull ... value) {
        int id = mState.cacheData(value);
        mBuffer.addAnimatedFloat(id, value);
        return Utils.asNan(id);
    }

    /**
     * Add a float expression that is a computation based on variables. see packAnimation
     *
     * @param value A RPN style float operation i.e. "4, 3, ADD" outputs 7
     * @param animation Array of floats that represents animation
     * @return NaN id of the result of the calculation
     */
    public float floatExpression(float @NonNull [] value, float @Nullable [] animation) {
        int id = mState.cacheData(value);
        mBuffer.addAnimatedFloat(id, value, animation);
        return Utils.asNan(id);
    }

    /**
     * Add and integer constant
     *
     * @param value
     * @return id of integer as a long to prevent accidental miss use
     */
    public long addInteger(int value) {
        int id = mState.cacheInteger(value);
        mBuffer.addInteger(id, value);
        return id + 0x100000000L;
    }

    /**
     * Add a long constant return a id. They can be used as parameters to Custom Attributes.
     *
     * @param value the value of the long
     * @return the id of the command representing long
     */
    public int addLong(long value) {
        int id = mState.createNextAvailableId();
        mBuffer.addLong(id, value);
        return id;
    }

    /**
     * Add a boolean constant return a id. They can be used as parameters to Custom Attributes.
     *
     * @param value the value of the boolean
     * @return the id
     */
    public int addBoolean(boolean value) {
        int id = mState.createNextAvailableId();
        mBuffer.addBoolean(id, value);
        return id;
    }

    /**
     * look up map and return the id of the object looked up
     *
     * @param mapId the map to access
     * @param str the string to lookup
     * @return id containing the result of the lookup
     */
    public int mapLookup(int mapId, @NonNull String str) {
        int strId = addText(str);
        return mapLookup(mapId, strId);
    }

    /**
     * look up map and return the id of the object looked up
     *
     * @param mapId the map to access
     * @param strId the string to lookup
     * @return id containing the result of the lookup
     */
    public int mapLookup(int mapId, int strId) {
        int hash = mapId + strId * 33;
        int id = mState.dataGetId(hash);
        if (id == -1) {
            id = mState.cacheData(hash);
            mBuffer.mapLookup(id, mapId, strId);
        }
        return id;
    }

    /**
     * Adds a text string data to the stream and returns its id Will be used to insert string with
     * bitmaps etc.
     *
     * @param text the string to inject in the buffer
     */
    public int addText(@NonNull String text) {
        int id = mState.dataGetId(text);
        if (id == -1) {
            id = mState.cacheData(text);
            mBuffer.addText(id, text);
        }
        return id;
    }

    /**
     * measure the text and return a measure as a float
     *
     * @param textId id of the text
     * @param mode the mode 0 is the width
     * @return
     */
    public float textMeasure(int textId, int mode) {
        int id = mState.cacheData(textId + mode * 31);
        mBuffer.textMeasure(id, textId, mode);
        return Utils.asNan(id);
    }

    /**
     * Encode a text length operation
     *
     * @param textId the text id we are measuring
     * @return an id encoded as a float NaN
     */
    public float textLength(int textId) {
        // The cache id is computed buy merging the two values together
        // to create a relatively unique value
        int id = mState.cacheData(textId + (TextLength.id() << 16));
        mBuffer.textLength(id, textId);
        return Utils.asNan(id);
    }

    public static final byte STOP_GENTLY = TouchExpression.STOP_GENTLY;
    public static final byte STOP_ENDS = TouchExpression.STOP_ENDS;
    public static final byte STOP_INSTANTLY = TouchExpression.STOP_INSTANTLY;
    public static final byte STOP_NOTCHES_EVEN = TouchExpression.STOP_NOTCHES_EVEN;
    public static final byte STOP_NOTCHES_PERCENTS = TouchExpression.STOP_NOTCHES_PERCENTS;
    public static final byte STOP_NOTCHES_ABSOLUTE = TouchExpression.STOP_NOTCHES_ABSOLUTE;
    public static final byte STOP_ABSOLUTE_POS = TouchExpression.STOP_ABSOLUTE_POS;

    /**
     * Encode an easing as a float array
     *
     * @param maxTime
     * @param maxAcceleration
     * @param maxVelocity
     * @return
     */
    public float @NonNull [] easing(float maxTime, float maxAcceleration, float maxVelocity) {
        return new float[] {Float.intBitsToFloat(0), maxTime, maxAcceleration, maxVelocity};
    }

    public static final int ID_REFERENCE = 1 << 15;

    /**
     * Add touch handling on canvas
     *
     * @param defValue the default value
     * @param min the minimum value if set to NaN it wraps around max
     * @param max the maximum value
     * @param touchMode the touch Mode STOP_*
     * @param velocityId This indicates that it is moving
     * @param touchEffects This contains effect currently only haptic feedback
     * @param touchSpec with touchMode configure types of stopping behaviour
     * @param easingSpec the configuration how it will slow down on touch up
     * @param exp the float expression that maps the touches to the path
     * @return touch
     */
    public float addTouch(
            float defValue,
            float min,
            float max,
            int touchMode,
            float velocityId,
            int touchEffects,
            float @Nullable [] touchSpec,
            float @Nullable [] easingSpec,
            float @NonNull ... exp) {
        int id = mState.createNextAvailableId();
        mBuffer.addTouchExpression(
                id,
                defValue,
                min,
                max,
                velocityId,
                touchEffects,
                exp,
                touchMode,
                touchSpec,
                easingSpec);
        return Utils.asNan(id);
    }

    /**
     * Create a spring encoded as a float array
     *
     * @param stiffness
     * @param damping
     * @param stopThreshold
     * @param boundaryMode
     * @return
     */
    public float @NonNull [] spring(
            float stiffness, float damping, float stopThreshold, int boundaryMode) {
        return new float[] {
            0, stiffness, damping, stopThreshold, Float.intBitsToFloat(boundaryMode)
        };
    }

    /**
     * Represent a map of data ids
     *
     * @param bitmapId the id of the bitmap
     * @param attribute the attribute to get
     * @return the value of the attribute as a NaN float
     */
    public float bitmapAttribute(int bitmapId, short attribute) {
        int id = mState.createNextAvailableId();
        mBuffer.bitmapAttribute(id, bitmapId, attribute);
        return Utils.asNan(id);
    }

    /**
     * get an attribute of a text
     *
     * @param textId the id of the bitmap
     * @param attribute the attribute to get
     * @return the value of the attribute as a NaN float
     */
    public float textAttribute(int textId, short attribute) {
        int id = mState.createNextAvailableId();
        mBuffer.textAttribute(id, textId, attribute);
        return Utils.asNan(id);
    }

    public static class DataMap {
        @NonNull String mName;

        enum Types {
            STRING(DataMapIds.TYPE_STRING),
            INT(DataMapIds.TYPE_INT),
            FLOAT(DataMapIds.TYPE_FLOAT),
            LONG(DataMapIds.TYPE_LONG),
            BOOLEAN(DataMapIds.TYPE_BOOLEAN);

            private final byte mValue;

            Types(final byte newValue) {
                mValue = newValue;
            }

            public byte getValue() {
                return mValue;
            }
        }

        @NonNull Types mType;
        @NonNull String mTextValue;
        float mFloatValue;
        boolean mBooleanValue;
        long mLongValue;
        int mIntValue;

        DataMap(@NonNull String name, @NonNull String value) {
            mName = name;
            mType = Types.STRING;
            mTextValue = value;
        }

        DataMap(@NonNull String name, float value) {
            mName = name;
            mType = Types.FLOAT;
            mFloatValue = value;
        }

        DataMap(@NonNull String name, long value) {
            mName = name;
            mType = Types.LONG;
            mLongValue = value;
        }

        DataMap(@NonNull String name, int value) {
            mName = name;
            mType = Types.INT;
            mIntValue = value;
        }

        DataMap(@NonNull String name, boolean value) {
            mName = name;
            mType = Types.BOOLEAN;
            mBooleanValue = value;
        }
    }

    /**
     * Create a map entry for a float
     *
     * @param name name of the value
     * @param value
     * @return
     */
    public static @NonNull DataMap map(@NonNull String name, float value) {
        return new DataMap(name, value);
    }

    /**
     * Create a map entry for an int
     *
     * @param name name of the value
     * @param value
     * @return
     */
    public static @NonNull DataMap map(@NonNull String name, int value) {
        return new DataMap(name, value);
    }

    /**
     * Create a map entry for a long
     *
     * @param name name of the value
     * @param value
     * @return
     */
    public static @NonNull DataMap map(@NonNull String name, long value) {
        return new DataMap(name, value);
    }

    /**
     * Create a map entry for a String
     *
     * @param name name of the value
     * @param value
     * @return
     */
    public static @NonNull DataMap map(@NonNull String name, @NonNull String value) {
        return new DataMap(name, value);
    }

    /**
     * Create a map entry for a boolean
     *
     * @param name name of the value
     * @param value
     * @return
     */
    public static @NonNull DataMap map(@NonNull String name, boolean value) {
        return new DataMap(name, value);
    }

    private int encodeData(DataMap @NonNull ... data) {
        String[] names = new String[data.length];
        int[] ids = new int[data.length];
        byte[] types = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            DataMap item = data[i];
            int id = 0;
            switch (item.mType) {
                case STRING:
                    id = addText(item.mTextValue);
                    break;
                case INT:
                    id = mState.cacheInteger(item.mIntValue);
                    mBuffer.addInteger(id, item.mIntValue);
                    break;
                case FLOAT:
                    id = Utils.idFromNan(addFloatConstant(item.mFloatValue));
                    break;
                case LONG:
                    id = addLong(item.mLongValue);
                    break;
                case BOOLEAN:
                    id = addBoolean(item.mBooleanValue);
                    break;
            }
            ids[i] = id;
            types[i] = item.mType.mValue;
            names[i] = item.mName;
        }
        int id = mState.cacheData(ids, NanMap.TYPE_ARRAY);
        mBuffer.addMap(id, names, types, ids);
        return id;
    }

    /**
     * Add map of ids
     *
     * @param data
     * @return
     */
    public int addDataMap(DataMap @NonNull ... data) {
        return encodeData(data);
    }

    /**
     * Add a map of ids
     *
     * @param keys
     * @param ids
     * @return
     */
    public float addDataMap(String @NonNull [] keys, int @NonNull [] ids) {
        int id = mState.cacheData(ids, NanMap.TYPE_ARRAY);
        mBuffer.addMap(id, keys, null, ids);
        return id;
    }

    /**
     * Create an integer expression
     *
     * @param mask sets the bits the are expressions
     * @param value values in the expression maximum 32
     * @return
     */
    public long integerExpression(int mask, int @NonNull ... value) {
        int id = mState.cacheData(value);
        mBuffer.addIntegerExpression(id, mask, value);
        return id + 0x100000000L;
    }

    /**
     * Create an integer expression
     *
     * @param v values in the expression as long ids
     * @return
     */
    public long integerExpression(long @NonNull ... v) {
        int mask = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > Integer.MAX_VALUE) {
                mask |= 1 << i;
            }
        }
        int[] vint = new int[v.length];
        for (int i = 0; i < vint.length; i++) {
            vint[i] = (int) v[i];
        }

        return integerExpression(mask, vint);
    }

    /**
     * Converts a integer ID to a float ID.
     *
     * @param id
     * @return
     */
    public float asFloatId(long id) {
        return mBuffer.asFloatId((int) (id & 0xFFFFFFFL));
    }

    /**
     * Create a time attribute
     *
     * @param longID the id of the time to be compared
     * @param type the type of comparison
     * @param args the additional arguments for the comparison
     * @return float value
     */
    public float timeAttribute(int longID, short type, int @NonNull ... args) {
        int id = mState.createNextAvailableId();
        mBuffer.timeAttribute(id, longID, type, args);
        return Utils.asNan(id);
    }

    /**
     * Bundle a float RPN expression as an expression
     *
     * @param value Series of floats that represents the expression
     * @return the series of floats that represent the expression
     */
    public float @NonNull [] exp(float @NonNull ... value) {
        return value;
    }

    /** Or'ed with type Animation only happens in positive direction */
    public static final int SNAP_WHEN_LESS = 1 << 10;

    /** Or'ed with type Animation only happens in negative direction */
    public static final int SNAP_WHEN_MORE = 2 << 10;

    /** propagate animation to derived expressions */
    public static final int PROPAGATE_ANIMATION = 4 << 10;

    /**
     * Bundle Animation Expressions as a compacted array of floats
     *
     * @param duration The duration of the animation
     * @param type The type of the animation
     * @param spec The parameters of the animation if it is custom spec
     * @param initialValue the initial value if animation is on first use
     * @param wrap The value about witch it wraps e.g. 360 and rotation
     * @return Float array representing the animation
     */
    public float @NonNull [] anim(
            float duration, int type, float @Nullable [] spec, float initialValue, float wrap) {
        return RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, wrap);
    }

    /**
     * Bundle Animation Expressions as a compacted array of floats
     *
     * @param duration The duration of the animation
     * @param type The type of the animation
     * @param spec The parameters of the animation if it is custom spec
     * @param initialValue the initial value if animation is on first use
     * @return Float array representing the animation
     */
    public float @NonNull [] anim(
            float duration, int type, float @Nullable [] spec, float initialValue) {
        return RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, Float.NaN);
    }

    /**
     * Bundle Animation Expressions as a compacted array of floats
     *
     * @param duration The duration of the animation
     * @param type The type of the animation
     * @param spec The parameters of the animation if it is custom spec
     * @return Float array representing the animation
     */
    public float @NonNull [] anim(float duration, int type, float @Nullable [] spec) {
        return RemoteComposeBuffer.packAnimation(duration, type, spec, Float.NaN, Float.NaN);
    }

    /**
     * Bundle Animation Expressions as a compacted array of floats
     *
     * @param duration The duration of the animation
     * @param type The type of the animation
     * @return Float array representing the animation
     */
    public float @NonNull [] anim(float duration, int type) {
        return RemoteComposeBuffer.packAnimation(duration, type, null, Float.NaN, Float.NaN);
    }

    /**
     * Bundle Animation Expressions as a compacted array of floats
     *
     * @param duration The duration of the animation
     * @return Float array representing the animation
     */
    public float @NonNull [] anim(float duration) {
        return RemoteComposeBuffer.packAnimation(
                duration, RemoteComposeBuffer.EASING_CUBIC_STANDARD, null, Float.NaN, Float.NaN);
    }

    /**
     * This looks up an entry in a id array
     *
     * @param arrayId
     * @param index
     * @return
     */
    public int idLookup(float arrayId, float index) {
        int id = mState.createNextAvailableId();
        mBuffer.idLookup(id, arrayId, index);
        return id;
    }

    /**
     * This looks up a entry in a text array
     *
     * @param arrayId the Nan encoded id of the array
     * @param index the index Passed as a float for integration into float system
     * @return id of text object that can be use in drawText operations
     */
    public int textLookup(float arrayId, float index) {
        long hash =
                (((long) Float.floatToRawIntBits(arrayId)) << 32)
                        + Float.floatToRawIntBits(
                                index); // TODO: is this the correct ()s? -- bbade@
        int id = mState.cacheData(hash);
        mBuffer.textLookup(id, arrayId, index);
        return id;
    }

    /**
     * Looks up a entry in a text array
     *
     * @param arrayId the Nan encoded id of the array
     * @param indexId the index id
     * @return id of text object that can be use in drawText operations
     */
    public int textLookup(float arrayId, int indexId) {
        long hash =
                (((long) Float.floatToRawIntBits(arrayId)) << 32)
                        + Float.floatToRawIntBits(indexId); // TODO: is this the correct ()s?
        int id = mState.cacheData(hash);
        mBuffer.textLookup(id, arrayId, indexId);
        return id;
    }

    /**
     * Convert a float to a string for drawing
     *
     * @param value The value to convert (typically a Nan ID)
     * @param before digits before the decimal point
     * @param after digits after the decimal point
     * @param flags configure the behaviour using PAD_PRE_* and PAD_AFTER* flags
     * @return id of string
     */
    public int createTextFromFloat(float value, int before, int after, int flags) {
        String placeHolder =
                Utils.floatToString(value) + "(" + before + "," + after + "," + flags + ")";
        int id = mState.dataGetId(placeHolder);
        if (id == -1) {
            id = mState.cacheData(placeHolder);
            //   TextData.apply(mBuffer, id, text);
        }
        return mBuffer.createTextFromFloat(id, value, (short) before, (short) after, flags);
    }

    /**
     * Returns the next available id for the given type
     *
     * @param type the type of the value
     * @return a unique id
     */
    public int createID(int type) {
        return mState.createNextAvailableId(type);
    }

    /**
     * Returns the next available id
     *
     * @return a unique id
     */
    public int nextId() {
        return mState.createNextAvailableId();
    }

    public static final long L_ADD = 0x100000000L + I_ADD;
    public static final long L_SUB = 0x100000000L + I_SUB;
    public static final long L_MUL = 0x100000000L + I_MUL;
    public static final long L_DIV = 0x100000000L + I_DIV;
    public static final long L_MOD = 0x100000000L + I_MOD;
    public static final long L_SHL = 0x100000000L + I_SHL;
    public static final long L_SHR = 0x100000000L + I_SHR;
    public static final long L_USHR = 0x100000000L + I_USHR;
    public static final long L_OR = 0x100000000L + I_OR;
    public static final long L_AND = 0x100000000L + I_AND;
    public static final long L_XOR = 0x100000000L + I_XOR;
    public static final long L_COPY_SIGN = 0x100000000L + I_COPY_SIGN;
    public static final long L_MIN = 0x100000000L + I_MIN;
    public static final long L_MAX = 0x100000000L + I_MAX;

    public static final long L_NEG = 0x100000000L + I_NEG;
    public static final long L_ABS = 0x100000000L + I_ABS;
    public static final long L_INCR = 0x100000000L + I_INCR;
    public static final long L_DECR = 0x100000000L + I_DECR;
    public static final long L_NOT = 0x100000000L + I_NOT;
    public static final long L_SIGN = 0x100000000L + I_SIGN;

    public static final long L_CLAMP = 0x100000000L + I_CLAMP;
    public static final long L_IFELSE = 0x100000000L + I_IFELSE;
    public static final long L_MAD = 0x100000000L + I_MAD;

    public static final long L_VAR1 = 0x100000000L + I_VAR1;
    public static final long L_VAR2 = 0x100000000L + I_VAR2;

    /**
     * Add a root component
     *
     * @param content content of the layout
     */
    public void root(@NonNull RemoteComposeWriterInterface content) {
        mBuffer.addRootStart();
        content.run();
        mBuffer.addContainerEnd();
    }

    /**
     * Start a loop component
     *
     * @param indexId the index id
     * @param from from index
     * @param step step increment of the index
     * @param until loop till
     */
    public void startLoop(int indexId, float from, float step, float until) {
        mBuffer.addLoopStart(indexId, from, step, until);
    }

    /**
     * Start a loop component
     *
     * @param from from index
     * @param step step increment of the index
     * @param until loop till
     * @return
     */
    public float startLoopVar(float from, float step, float until) {
        int indexId = createID(0);
        mBuffer.addLoopStart(indexId, from, step, until);
        return asFloatId(indexId);
    }

    /**
     * Start a loop component
     *
     * @param count loop count
     */
    public float startLoop(float count) {
        int indexId = createID(0);
        startLoop(indexId, 0, 1, count);
        return asFloatId(indexId);
    }

    /** End a loop component */
    public void endLoop() {
        mBuffer.addLoopEnd();
    }

    /**
     * Add a Loop
     *
     * @param indexId the index id
     * @param from the index starting value as a float
     * @param step step increment of the index as a float
     * @param until loop until the index hit this value
     * @param content the content to loop
     */
    public void loop(
            int indexId,
            float from,
            float step,
            float until,
            @NonNull RemoteComposeWriterInterface content) {
        startLoop(indexId, from, step, until);
        content.run();
        endLoop();
    }

    /**
     * Add a Loop
     *
     * @param until loop until the index hit this value
     * @param from the index starting value
     * @param step step increment of the index
     * @param indexId the index id
     * @param content the content to loop
     */
    public void loop(
            int indexId,
            int from,
            int step,
            int until,
            @NonNull RemoteComposeWriterInterface content) {
        startLoop(indexId, from, step, until);
        content.run();
        endLoop();
    }

    /**
     * Add a conditional block
     *
     * @param type type of conditional block
     * @param a the first variable
     * @param b the second variable
     * @param content content of the conditional block
     */
    public void conditionalOperations(
            byte type, float a, float b, @NonNull RemoteComposeWriterInterface content) {
        mBuffer.addConditionalOperations(type, a, b);
        content.run();
        endConditionalOperations();
    }

    /**
     * Add a conditional block
     *
     * @param type type of conditional block
     * @param a the first variable
     * @param b the second variable
     */
    public void conditionalOperations(byte type, float a, float b) {
        mBuffer.addConditionalOperations(type, a, b);
    }

    /** End a conditional block */
    public void endConditionalOperations() {
        mBuffer.addContainerEnd();
    }

    /** Call addContentStart on buffer */
    private void addContentStart() {
        mBuffer.addContentStart();
        mHasForceSendingNewPaint = true;
    }

    /**
     * Add a column layout
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     * @param content content of the layout
     */
    public void column(
            @NonNull RecordingModifier modifier,
            int horizontal,
            int vertical,
            @NonNull RemoteComposeWriterInterface content) {
        startColumn(modifier, horizontal, vertical);
        content.run();
        endColumn();
    }

    /**
     * Start a column layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startColumn(@NonNull RecordingModifier modifier, int horizontal, int vertical) {
        int componentId = modifier.getComponentId();
        float spacedBy = modifier.getSpacedBy();
        mBuffer.addColumnStart(componentId, -1, horizontal, vertical, spacedBy);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a column layout */
    public void endColumn() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a collapsible column layout
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     * @param content content of the layout
     */
    public void collapsibleColumn(
            @NonNull RecordingModifier modifier,
            int horizontal,
            int vertical,
            @NonNull RemoteComposeWriterInterface content) {
        startCollapsibleColumn(modifier, horizontal, vertical);
        content.run();
        endCollapsibleColumn();
    }

    /**
     * Start a collapsible column layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startCollapsibleColumn(
            @NonNull RecordingModifier modifier, int horizontal, int vertical) {
        int componentId = modifier.getComponentId();
        float spacedBy = modifier.getSpacedBy();
        mBuffer.addCollapsibleColumnStart(componentId, -1, horizontal, vertical, spacedBy);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a collapsible column layout */
    public void endCollapsibleColumn() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a row layout
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     * @param content content of the layout
     */
    public void row(
            @NonNull RecordingModifier modifier,
            int horizontal,
            int vertical,
            @NonNull RemoteComposeWriterInterface content) {
        startRow(modifier, horizontal, vertical);
        content.run();
        endRow();
    }

    /**
     * Start a row layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startRow(@NonNull RecordingModifier modifier, int horizontal, int vertical) {
        int componentId = modifier.getComponentId();
        float spacedBy = modifier.getSpacedBy();
        mBuffer.addRowStart(componentId, -1, horizontal, vertical, spacedBy);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a row layout */
    public void endRow() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a collapsible row layout
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     * @param content content of the layout
     */
    public void collapsibleRow(
            @NonNull RecordingModifier modifier,
            int horizontal,
            int vertical,
            @NonNull RemoteComposeWriterInterface content) {
        startCollapsibleRow(modifier, horizontal, vertical);
        content.run();
        endCollapsibleRow();
    }

    /**
     * Start a collapsible row layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startCollapsibleRow(
            @NonNull RecordingModifier modifier, int horizontal, int vertical) {
        int componentId = modifier.getComponentId();
        float spacedBy = modifier.getSpacedBy();
        mBuffer.addCollapsibleRowStart(componentId, -1, horizontal, vertical, spacedBy);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a collapsible row layout */
    public void endCollapsibleRow() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a Canvas
     *
     * @param modifier list of modifiers for the layout
     * @param content content of the layout
     */
    public void canvas(
            @NonNull RecordingModifier modifier, @NonNull RemoteComposeWriterInterface content) {
        startCanvas(modifier);
        content.run();
        endCanvas();
    }

    /** In the context of a draw modifier, draw the component content */
    public void drawComponentContent() {
        mBuffer.drawComponentContent();
    }

    /**
     * Start a canvas
     *
     * @param modifier
     */
    public void startCanvas(@NonNull RecordingModifier modifier) {
        mBuffer.addCanvasStart(modifier.getComponentId(), -1);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
        mBuffer.addCanvasContentStart(-1);
    }

    /** End a canvas */
    public void endCanvas() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /** Start a list of canvas operations */
    public void startCanvasOperations() {
        mBuffer.addCanvasOperationsStart();
    }

    /** End a list of canvas operations */
    public void endCanvasOperations() {
        mBuffer.addContainerEnd();
    }

    /** Start a list of actions */
    public void startRunActions() {
        mBuffer.addRunActionsStart();
    }

    /** End a list of actions */
    public void endRunActions() {
        mBuffer.addContainerEnd();
    }

    /**
     * Add a box layout
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     * @param content content of the layout
     */
    public void box(
            @NonNull RecordingModifier modifier,
            int horizontal,
            int vertical,
            @NonNull RemoteComposeWriterInterface content) {
        startBox(modifier, horizontal, vertical);
        content.run();
        endBox();
    }

    /**
     * Start a box layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startBox(@NonNull RecordingModifier modifier, int horizontal, int vertical) {
        mBuffer.addBoxStart(modifier.getComponentId(), -1, horizontal, vertical);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }


    /**
     * Start a box layout
     *
     * @param modifier
     */
    public void startBox(@NonNull RecordingModifier modifier) {
        startBox(modifier, BoxLayout.START, BoxLayout.TOP);
    }

    /** End a box layout */
    public void endBox() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Start a fitbox layout
     *
     * @param modifier
     * @param horizontal
     * @param vertical
     */
    public void startFitBox(@NonNull RecordingModifier modifier, int horizontal, int vertical) {
        mBuffer.addFitBoxStart(modifier.getComponentId(), -1, horizontal, vertical);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a fitbox layout */
    public void endFitBox() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /** Add an image component */
    public void image(
            @NonNull RecordingModifier modifier, int imageId, int scaleType, float alpha) {
        mBuffer.addImage(modifier.getComponentId(), -1, imageId, scaleType, alpha);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        mBuffer.addContainerEnd();
    }

    /** Add a state layout */
    public void stateLayout(
            @NonNull RecordingModifier modifier,
            int indexId,
            @NonNull RemoteComposeWriterInterface content) {
        startStateLayout(modifier, indexId);
        content.run();
        endStateLayout();
    }

    /**
     * Start a state layout
     *
     * @param modifier
     * @param indexId
     */
    public void startStateLayout(@NonNull RecordingModifier modifier, int indexId) {
        mBuffer.addStateLayout(modifier.getComponentId(), -1, 0, 0, indexId);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a state layout */
    public void endStateLayout() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a scroll modifier
     *
     * @param direction HORIZONTAL(0) or VERTICAL(1)
     * @param positionId the position id as a NaN
     */
    public void addModifierScroll(int direction, float positionId) {
        float max = this.reserveFloatVariable();
        float notchMax = this.reserveFloatVariable();
        float touchExpressionDirection =
                direction != 0 ? RemoteContext.FLOAT_TOUCH_POS_X : RemoteContext.FLOAT_TOUCH_POS_Y;

        ScrollModifierOperation.apply(mBuffer.getBuffer(), direction, positionId, max, notchMax);
        mBuffer.addTouchExpression(
                Utils.idFromNan(positionId),
                0f,
                0f,
                max,
                0f,
                3,
                new float[] {
                    touchExpressionDirection, -1, MUL,
                },
                TouchExpression.STOP_GENTLY,
                null,
                null);
        mBuffer.addContainerEnd();
    }

    /**
     * Add a scroll modifier
     *
     * @param direction HORIZONTAL(0) or VERTICAL(1)
     * @param positionId the position id as a NaN
     * @param notches
     */
    public void addModifierScroll(int direction, float positionId, int notches) {
        // TODO: add support for non-notch behaviors etc.
        float max = this.reserveFloatVariable();
        float notchMax = this.reserveFloatVariable();
        float touchExpressionDirection =
                direction != 0 ? RemoteContext.FLOAT_TOUCH_POS_X : RemoteContext.FLOAT_TOUCH_POS_Y;

        ScrollModifierOperation.apply(mBuffer.getBuffer(), direction, positionId, max, notchMax);

        mBuffer.addTouchExpression(
                Utils.idFromNan(positionId),
                0f,
                0f,
                max,
                0f,
                3,
                new float[] {
                    touchExpressionDirection, -1, MUL,
                },
                TouchExpression.STOP_NOTCHES_EVEN,
                new float[] {notches, notchMax},
                null);

        mBuffer.addContainerEnd();
    }

    /**
     * Add a scroll modifier
     *
     * @param direction HORIZONTAL(0) or VERTICAL(1)
     */
    public void addModifierScroll(int direction) {
        float max = this.reserveFloatVariable();
        mBuffer.addModifierScroll(direction, max);
    }

    /** Add a text component */
    public void textComponent(
            @NonNull RecordingModifier modifier,
            int textId,
            int color,
            float fontSize,
            int fontStyle,
            float fontWeight,
            @Nullable String fontFamily,
            int textAlign,
            int overflow,
            int maxLines,
            @NonNull RemoteComposeWriterInterface content) {
        startTextComponent(
                modifier,
                textId,
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamily,
                textAlign,
                overflow,
                maxLines);
        content.run();
        endTextComponent();
    }

    /**
     * Start a text component
     *
     * @param modifier
     * @param textId
     * @param color
     * @param fontSize
     * @param fontStyle
     * @param fontWeight
     * @param fontFamily
     * @param textAlign
     * @param overflow
     * @param maxLines
     */
    public void startTextComponent(
            @NonNull RecordingModifier modifier,
            int textId,
            int color,
            float fontSize,
            int fontStyle,
            float fontWeight,
            @Nullable String fontFamily,
            int textAlign,
            int overflow,
            int maxLines) {
        int fontFamilyId = -1;
        if (fontFamily != null) {
            fontFamilyId = addText(fontFamily);
        }
        mBuffer.addTextComponentStart(
                modifier.getComponentId(),
                -1,
                textId,
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamilyId,
                textAlign,
                overflow,
                maxLines);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        addContentStart();
    }

    /** End a text component */
    public void endTextComponent() {
        mBuffer.addContainerEnd();
        mBuffer.addContainerEnd();
    }

    /**
     * Add a box layout with no content
     *
     * @param modifier list of modifiers for the layout
     * @param horizontal horizontal positioning
     * @param vertical vertical positioning
     */
    public void box(@NonNull RecordingModifier modifier, int horizontal, int vertical) {
        mBuffer.addBoxStart(modifier.getComponentId(), -1, horizontal, vertical);
        for (RecordingModifier.Element m : modifier.getList()) {
            m.write(this);
        }
        mBuffer.addContainerEnd();
    }

    /**
     * Add a box layout with no content
     *
     * @param modifier list of modifiers for the layout
     */
    public void box(@NonNull RecordingModifier modifier) {
        box(modifier, BoxLayout.CENTER, BoxLayout.CENTER);
    }

    /**
     * Creates and id for each string and makes a list of those ids
     *
     * @param strs varargs of strings
     * @return
     */
    public float addStringList(String @NonNull ... strs) {
        int[] ids = new int[strs.length];
        for (int i = 0; i < strs.length; i++) {
            ids[i] = textCreateId(strs[i]);
        }
        return addList(ids);
    }

    /**
     * Creates a list of id's
     *
     * @param strIds id's of the strings
     * @return
     */
    public float addStringList(int @NonNull ... strIds) {
        int id = mState.cacheData(strIds, NanMap.TYPE_ARRAY);
        mBuffer.addList(id, strIds);
        return Utils.asNan(id);
    }

    /**
     * Creates a list of id's
     *
     * @param listId id's of the strings
     */
    public float addList(int @NonNull [] listId) {
        int id = mState.cacheData(listId, NanMap.TYPE_ARRAY);
        mBuffer.addList(id, listId);
        return Utils.asNan(id);
    }

    /**
     * add an array of float
     *
     * @param values
     * @return
     */
    public float addFloatArray(float @NonNull [] values) {
        int id = mState.cacheData(values, NanMap.TYPE_ARRAY);
        mBuffer.addFloatArray(id, values);
        return Utils.asNan(id);
    }

    /**
     * Add a list of float
     *
     * @param values
     * @return
     */
    public float addFloatList(float @NonNull [] values) {
        // return mBuffer.addFloatList(values);
        int[] listId = new int[values.length];
        for (int i = 0; i < listId.length; i++) {
            listId[i] = mState.cacheFloat(values[i]);
            mBuffer.addFloat(listId[i], values[i]);
        }
        return addList(listId);
    }

    /**
     * Add a map of float values
     *
     * @param keys
     * @param values
     * @return
     */
    public float addFloatMap(String @NonNull [] keys, float @NonNull [] values) {
        int[] listId = new int[values.length];
        byte[] type = new byte[values.length];
        for (int i = 0; i < listId.length; i++) {
            listId[i] = mState.cacheFloat(values[i]);
            mBuffer.addFloat(listId[i], values[i]);
            type[i] = DataMapIds.TYPE_FLOAT;
        }
        int id = mState.cacheData(listId, NanMap.TYPE_ARRAY);
        mBuffer.addMap(id, keys, type, listId);
        return Utils.asNan(id);
    }

    /**
     * Ensures the bitmap is stored.
     *
     * @param image the bitbap to store
     * @return the id of the bitmap
     */
    public int storeBitmap(@NonNull Object image) {
        int imageId = mState.dataGetId(image);
        if (imageId == -1) {
            imageId = mState.cacheData(image);
            byte[] data = mPlatform.imageToByteArray(image); // todo: potential npe
            short imageWidth = (short) mPlatform.getImageWidth(image);
            short imageHeight = (short) mPlatform.getImageHeight(image);
            if (mPlatform.isAlpha8Image(image)) {
                mBuffer.storeBitmapA8(imageId, imageWidth, imageHeight, data);
            } else {
                mBuffer.storeBitmap(imageId, imageWidth, imageHeight, data);
            }
        }
        return imageId;
    }

    /**
     * Add a bitmap to the buffer
     *
     * @param image the bitmap
     * @return
     */
    public int addBitmap(@NonNull Object image) {
        return storeBitmap(image);
    }

    /**
     * Add a named bitmap to the buffer
     *
     * @param image the bitmap
     * @param name the bitmap name
     * @return
     */
    public int addBitmap(@NonNull Object image, @NonNull String name) {
        int id = storeBitmap(image);
        nameBitmapId(id, name);
        return id;
    }

    /**
     * Add a bitmap font
     *
     * @param glyphs array of glyphs
     * @return
     */
    public int addBitmapFont(BitmapFontData.Glyph @NonNull [] glyphs) {
        int id = mState.createNextAvailableId();
        return mBuffer.addBitmapFont(id, glyphs);
    }

    /**
     * Add a bitmap font
     *
     * @param glyphs array of glyphs
     * @param kerningTable The kerning table, where the key is pairs of glyphs (literally $1$2) and
     *     the value is the horizontal adjustment in pixels for that glyph pair. Can be empty.
     * @return
     */
    public int addBitmapFont(
            BitmapFontData.Glyph @NonNull [] glyphs, @NonNull Map<String, Short> kerningTable) {
        int id = mState.createNextAvailableId();
        return mBuffer.addBitmapFont(id, glyphs, kerningTable);
    }

    /**
     * Associate a name to a bitmap
     *
     * @param id the bitmap id
     * @param omicron the bitmap name
     */
    public void nameBitmapId(int id, @NonNull String omicron) {
        mBuffer.setBitmapName(id, omicron);
    }

    /**
     * Create a slot for an id but do not fill it. TODO do we need this? + it does not waste an
     * command - you can have unfilled slots
     *
     * @return
     */
    public float createFloatId() {
        return asFloatId(createID(0));
    }

    /**
     * Add an impulse container
     *
     * @param duration
     * @param start
     */
    public void impulse(float duration, float start) {
        mBuffer.addImpulse(duration, start);
    }

    /**
     * Add an impulse container
     *
     * @param duration
     * @param start
     * @param run
     */
    public void impulse(float duration, float start, @NonNull Runnable run) {
        mBuffer.addImpulse(duration, start);
        run.run();
        mBuffer.addImpulseEnd();
    }

    /**
     * Add an impulse process container
     *
     * @param run
     */
    public void impulseProcess(@NonNull Runnable run) {
        mBuffer.addImpulseProcess();
        run.run();
        mBuffer.addImpulseEnd();
    }

    /** add an impulse process */
    public void impulseProcess() {
        mBuffer.addImpulseProcess();
    }

    /** Close an impulse container */
    public void impulseEnd() {
        mBuffer.addImpulseEnd();
    }

    /**
     * Add a particle system definition
     *
     * @param variables
     * @param initialExpressions
     * @param particleCount
     * @return
     */
    public float createParticles(
            float @NonNull [] variables,
            float @Nullable [] @NonNull [] initialExpressions,
            int particleCount) {
        int id = createID(0);
        float index = asFloatId(id);
        int[] varId = new int[variables.length];
        for (int i = 0; i < varId.length; i++) {
            varId[i] = createID(0);
            variables[i] = asFloatId(varId[i]);
        }
        mBuffer.addParticles(id, varId, initialExpressions, particleCount);
        return index;
    }

    /**
     * Add a particle loop
     *
     * @param id id of the particle system (encoded as NaN)
     * @param restart
     * @param expressions
     * @param r
     */
    public void particlesLoop(
            float id,
            float @Nullable [] restart,
            float @NonNull [] @NonNull [] expressions,
            @NonNull Runnable r) {
        mBuffer.addParticlesLoop(Utils.idFromNan(id), restart, expressions);
        r.run();
        mBuffer.addParticleLoopEnd();
    }

    /**
     * Define a function to be called later
     *
     * @param args the arguments of the function to be filled in
     * @return the id of the function
     */
    public int createFloatFunction(float @NonNull [] args) {
        int fid = mState.createNextAvailableId();
        int[] intArgs = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            intArgs[i] = createID(0);
            args[i] = asFloatId(intArgs[i]);
        }
        mBuffer.defineFloatFunction(fid, intArgs);
        return fid;
    }

    /** End the function */
    public void endFloatFunction() {
        mBuffer.addEndFloatFunctionDef();
    }

    /**
     * add a function call
     *
     * @param id The id of the function to call
     * @param args The arguments of the function
     */
    public void callFloatFunction(int id, float @NonNull ... args) {
        mBuffer.callFloatFunction(id, args);
    }

    /**
     * Add a time in ms
     *
     * @param time the time in ms from epoch
     * @return id of time constant
     */
    public int addTimeLong(long time) {
        return addLong(time);
    }

    /**
     * Add a debug message. Outputs to standard out. Remove this in production.
     *
     * @param message the message to add
     */
    public void addDebugMessage(@NonNull String message) {
        int textId = addText(message);
        mBuffer.addDebugMessage(textId, 0f, 0);
    }

    /**
     * Add a debug message. Outputs to standard out. Remove this in production.
     *
     * @param message the message to add
     * @param value the value to add
     */
    public void addDebugMessage(@NonNull String message, float value) {
        int textId = addText(message);
        mBuffer.addDebugMessage(textId, value, 0);
    }

    /**
     * Add a debug message. Outputs to standard out. Remove this in production.
     *
     * @param message the message to add
     * @param value the value to add
     * @param flag the flag to add
     */
    public void addDebugMessage(@NonNull String message, float value, int flag) {
        int textId = addText(message);
        mBuffer.addDebugMessage(textId, value, flag);
    }

    /**
     * Add a debug message. Outputs to standard out. Remove this in production.
     *
     * @param textId the message to add
     * @param value the value to add
     * @param flag the flag to add
     */
    public void addDebugMessage(int textId, float value, int flag) {
        mBuffer.addDebugMessage(textId, value, flag);
    }

    /**
     * Add a matrix expression
     *
     * @param exp the matrix expression
     * @return the id of the matrix expression
     */
    public float matrixExpression(float @NonNull ... exp) {
        int id = mState.createNextAvailableId();
        mBuffer.addMatrixExpression(id, exp);
        return Utils.asNan(id);
    }

    /**
     * Add a inline font to the buffer. The font is a byte array loaded from a true type font.
     * Typically it would be a sub font of a larger font restricted to a specific range of
     * characters.
     *
     * @param data the font data
     * @return the id of the font use in painter.setTypeface(id)
     */
    public int addFont(byte @NonNull [] data) {
        int id = mState.createNextAvailableId();
        mBuffer.addFont(id, 0, data);
        return id;
    }

    /**
     * Create a bitmap
     *
     * @param width the width of the bitmap
     * @param height the height of the bitmap
     * @return id of bitmap
     */
    public int createBitmap(int width, int height) {
        int id = mState.createNextAvailableId();
        return mBuffer.createBitmap(id, (short) width, (short) height);
    }

    /**
     * Draw on a bitmap, all subsequent operations will be applied to the bitmap
     *
     * @param bitmapId if 0 draw on main canvas
     * @param mode to support various mode if 1 do not erase the bitmap
     * @param color the color to fill the bitmap
     */
    public void drawOnBitmap(int bitmapId, int mode, int color) {
        mBuffer.drawOnBitmap(bitmapId, mode, color);
    }

    /**
     * Draw on a bitmap, all subsequent operations will be applied to the bitmap
     *
     * @param bitmapId if 0 draw on main canvas
     */
    public void drawOnBitmap(int bitmapId) {
        mBuffer.drawOnBitmap(bitmapId, 0, 0);
    }

    /**
     * Add a component visibility operation
     *
     * @param valueId the value to set the visibility to
     */
    public void addComponentVisibilityOperation(int valueId) {
        mBuffer.addComponentVisibilityOperation(valueId);
    }

    /**
     * Add a width modifier operation
     *
     * @param type    the type the width
     * @param valueId the value to set the width to
     */
    public void addWidthModifierOperation(int type, float valueId) {
        mBuffer.addWidthModifierOperation(type, valueId);
    }

    /**
     * Add a height modifier operation
     *
     * @param type    the type the height
     * @param valueId the value to set the height to
     */
    public void addHeightModifierOperation(int type, float valueId) {
        mBuffer.addHeightModifierOperation(type, valueId);
    }

    /**
     * Add a modifier ripple
     */
    public void addModifierRipple() {
        mBuffer.addModifierRipple();
    }

    /**
     * Add a modifier zIndex
     *
     * @param value the zIndex to set
     */
    public void addModifierZIndex(float value) {
        mBuffer.addModifierZIndex(value);
    }

    /**
     * Add a modifier marquee
     *
     * @param iterations         the number of iterations
     * @param animationMode      the animation mode
     * @param repeatDelayMillis  the repeat delay
     * @param initialDelayMillis the initial delay
     * @param spacing            the spacing
     * @param velocity           the velocity
     */
    public void addModifierMarquee(int iterations,
                                   int animationMode,
                                   float repeatDelayMillis,
                                   float initialDelayMillis,
                                   float spacing,
                                   float velocity) {
        mBuffer.addModifierMarquee(iterations,
                animationMode,
                repeatDelayMillis,
                initialDelayMillis,
                spacing,
                velocity);
    }

    /**
     * Add a height in modifier operation
     *
     * @param min the minimum height
     * @param max the maximum height
     */
    public void addHeightInModifierOperation(float min, float max) {
        mBuffer.addHeightInModifierOperation(min, max);
    }

    /**
     * Add a modifier graphics layer
     *
     * @param attributes the attributes to set
     */
    public void addModifierGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {
        mBuffer.addModifierGraphicsLayer(attributes);
    }

    /**
     * Add a touch down modifier operation
     */
    public void addTouchDownModifierOperation() {
        mBuffer.addTouchDownModifierOperation();
    }

    /**
     * Add a touch up modifier operation
     */
    public void addTouchUpModifierOperation() {
        mBuffer.addTouchUpModifierOperation();
    }

    /**
     * Add a touch cancel modifier operation
     */
    public void addTouchCancelModifierOperation() {
        mBuffer.addTouchCancelModifierOperation();
    }

    /**
     * Add a container start
     */
    public void addContainerEnd() {
        mBuffer.addContainerEnd();
    }

    /**
     * Add a modifier offset
     *
     * @param x offset
     * @param y offset
     */
    public void addModifierOffset(float x, float y) {
        mBuffer.addModifierOffset(x, y);
    }

    /**
     * Add a modifier background
     *
     * @param color the color to set
     * @param shape the shape to set
     */
    public void addModifierBackground(int color, int shape) {
        mBuffer.addModifierBackground(color, shape);
    }

    /**
     * Add a clip rect modifier
     */
    public void addClipRectModifier() {
        mBuffer.addClipRectModifier();
    }

    /**
     * Add a round clip rect modifier
     *
     * @param topStart    the top start value
     * @param topEnd      the top end value
     * @param bottomStart the bottom start value
     * @param bottomEnd   the bottom end value
     */
    public void addRoundClipRectModifier(float topStart,
                                         float topEnd,
                                         float bottomStart,
                                         float bottomEnd) {
        mBuffer.addRoundClipRectModifier(topStart, topEnd, bottomStart, bottomEnd);
    }

    /**
     * Add a width in modifier operation
     *
     * @param min the minimum width
     * @param max the maximum width
     */
    public void addWidthInModifierOperation(float min, float max) {
        mBuffer.addWidthInModifierOperation(min, max);
    }

    /**
     * Add a modifier padding
     *
     * @param left   the left padding
     * @param top    the top padding
     * @param right  the right padding
     * @param bottom the bottom padding
     */
    public void addModifierPadding(float left, float top, float right, float bottom) {
        mBuffer.addModifierPadding(left, top, right, bottom);
    }

    /**
     * Add a draw content operation
     */
    public void addDrawContentOperation() {
        mBuffer.addDrawContentOperation();
    }

    /**
     * Add a semantics modifier
     *
     * @param contentDescriptionId the content description id
     * @param role                 the role
     * @param textId               the text id
     * @param stateDescriptionId   the state description id
     * @param mode                 the mode
     * @param enabled              the enabled
     * @param clickable            the clickable
     */
    public void addSemanticsModifier(int contentDescriptionId,
                                     byte role,
                                     int textId,
                                     int stateDescriptionId,
                                     int mode,
                                     boolean enabled,
                                     boolean clickable) {

        mBuffer.addSemanticsModifier(contentDescriptionId,
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
        mBuffer.addClickModifierOperation();
    }

    /**
     * Add a collapsible priority modifier
     *
     * @param orientation the orientation
     * @param priority    the priority
     */
    public void addCollapsiblePriorityModifier(int orientation, float priority) {
        mBuffer.addCollapsiblePriorityModifier(orientation, priority);
    }

    /**
     * Add an animation spec modifier
     *
     * @param animationId          the animation id
     * @param motionDuration       the motion duration
     * @param motionEasingType     the motion easing type
     * @param visibilityDuration   the visibility duration
     * @param visibilityEasingType the visibility easing type
     * @param enterAnimation       the enter animation
     * @param exitAnimation        the exit animation
     */
    public void addAnimationSpecModifier(int animationId,
                                         float motionDuration,
                                         int motionEasingType,
                                         float visibilityDuration,
                                         int visibilityEasingType,
                                         int enterAnimation,
                                         int exitAnimation) {
        mBuffer.addAnimationSpecModifier(
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                enterAnimation,
                exitAnimation);
    }

    /**
     * Add a modifier border
     *
     * @param width         the width
     * @param roundedCorner the rounded corner
     * @param color         the color
     * @param shapeType     the shape type
     */
    public void addModifierBorder(float width, float roundedCorner, int color, int shapeType) {
        mBuffer.addModifierBorder(width, roundedCorner, color, shapeType);
    }

    /**
     * Add a value string change action operation
     *
     * @param destTextId the text id to change
     * @param srcTextId  the text id to copy from
     */
    public void addValueStringChangeActionOperation(int destTextId, int srcTextId) {
        mBuffer.addValueStringChangeActionOperation(destTextId, srcTextId);
    }

    /**
     * Add a value float expression change action operation
     *
     * @param destIntegerId the integer id to change
     * @param srcIntegerId  the integer id to copy from
     */
    public void addValueIntegerExpressionChangeActionOperation(
            long destIntegerId,
            long srcIntegerId) {
        mBuffer.addValueIntegerExpressionChangeActionOperation(destIntegerId, srcIntegerId);
    }

    /**
     * Add a value float change action operation
     *
     * @param mValueId the value id to change
     * @param mValue   the value to change to
     */
    public void addValueFloatChangeActionOperation(int mValueId, float mValue) {
        mBuffer.addValueFloatChangeActionOperation(mValueId, mValue);
    }

    /**
     * Add a value integer change action operation
     *
     * @param valueId the value id to change
     * @param value   the value to change to
     */
    public void addValueIntegerChangeActionOperation(int valueId, int value) {
        mBuffer.addValueIntegerChangeActionOperation(valueId, value);
    }

    /**
     * Add a value float expression change action operation
     *
     * @param mValueId the value id to change
     * @param mValue   the value to change to
     */
    public void addValueFloatExpressionChangeActionOperation(int mValueId, int mValue) {
        mBuffer.addValueFloatExpressionChangeActionOperation(mValueId, mValue);
    }
}
