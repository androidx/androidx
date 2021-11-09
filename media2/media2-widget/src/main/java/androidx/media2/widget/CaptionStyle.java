/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.media2.widget;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.view.accessibility.CaptioningManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Specifies visual properties for video captions, including foreground and
 * background colors, edge properties, and typeface.
 *
 * Note: This class is copied and trimmed from framework code,
 * android.view.accessibility.CaptioningManager.CaptionStyle,
 * in order to support lower than API level 19 devices.
 */
final class CaptionStyle {
    /**
     * Packed value for a color of 'none' and a cached opacity of 100%.
     */
    private static final int COLOR_NONE_OPAQUE = 0x000000FF;

    /**
     * Packed value for a color of 'default' and opacity of 100%.
     */
    public static final int COLOR_UNSPECIFIED = 0x00FFFFFF;

    /** The default caption style used to fill in unspecified values. */
    public static final CaptionStyle DEFAULT;

    /** Unspecified edge type value. */
    public static final int EDGE_TYPE_UNSPECIFIED = -1;

    /** Edge type value specifying no character edges. */
    public static final int EDGE_TYPE_NONE = 0;

    /** Edge type value specifying uniformly outlined character edges. */
    public static final int EDGE_TYPE_OUTLINE = 1;

    /** Edge type value specifying drop-shadowed character edges. */
    public static final int EDGE_TYPE_DROP_SHADOW = 2;

    /** Edge type value specifying raised bevel character edges. */
    public static final int EDGE_TYPE_RAISED = 3;

    /** Edge type value specifying depressed bevel character edges. */
    public static final int EDGE_TYPE_DEPRESSED = 4;

    /** The preferred foreground color for video captions. */
    public final int foregroundColor;

    /** The preferred background color for video captions. */
    public final int backgroundColor;

    /**
     * The preferred edge type for video captions, one of:
     * <ul>
     * <li>{@link #EDGE_TYPE_UNSPECIFIED}
     * <li>{@link #EDGE_TYPE_NONE}
     * <li>{@link #EDGE_TYPE_OUTLINE}
     * <li>{@link #EDGE_TYPE_DROP_SHADOW}
     * <li>{@link #EDGE_TYPE_RAISED}
     * <li>{@link #EDGE_TYPE_DEPRESSED}
     * </ul>
     */
    public final int edgeType;

    /**
     * The preferred edge color for video captions, if using an edge type
     * other than {@link #EDGE_TYPE_NONE}.
     */
    public final int edgeColor;

    /** The preferred window color for video captions. */
    public final int windowColor;

    private final boolean mHasForegroundColor;
    private final boolean mHasBackgroundColor;
    private final boolean mHasEdgeType;
    private final boolean mHasEdgeColor;
    private final boolean mHasWindowColor;

    /** Lazily-created typeface based on the raw typeface string. */
    private Typeface mParsedTypeface;

    @RequiresApi(19)
    CaptionStyle(CaptioningManager.CaptionStyle captionStyle) {
        this(captionStyle.foregroundColor, captionStyle.backgroundColor, captionStyle.edgeType,
                captionStyle.edgeColor,
                VERSION.SDK_INT >= 21 ? captionStyle.windowColor : COLOR_NONE_OPAQUE,
                Api19Impl.getTypeface(captionStyle));
    }

    CaptionStyle(int foregroundColor, int backgroundColor, int edgeType, int edgeColor,
            int windowColor, @Nullable Typeface typeface) {
        mHasForegroundColor = hasColor(foregroundColor);
        mHasBackgroundColor = hasColor(backgroundColor);
        mHasEdgeType = edgeType != EDGE_TYPE_UNSPECIFIED;
        mHasEdgeColor = hasColor(edgeColor);
        mHasWindowColor = hasColor(windowColor);

        // Always use valid colors, even when no override is specified, to
        // ensure backwards compatibility with apps targeting KitKat MR2.
        this.foregroundColor = mHasForegroundColor ? foregroundColor : Color.WHITE;
        this.backgroundColor = mHasBackgroundColor ? backgroundColor : Color.BLACK;
        this.edgeType = mHasEdgeType ? edgeType : EDGE_TYPE_NONE;
        this.edgeColor = mHasEdgeColor ? edgeColor : Color.BLACK;
        this.windowColor = mHasWindowColor ? windowColor : COLOR_NONE_OPAQUE;

        mParsedTypeface = typeface;
    }

    /**
     * Returns whether a packed color indicates a non-default value.
     *
     * @param packedColor the packed color value
     * @return {@code true} if a non-default value is specified
     */
    static boolean hasColor(int packedColor) {
        // Matches the color packing code from Settings. "Default" packed
        // colors are indicated by zero alpha and non-zero red/blue. The
        // cached alpha value used by Settings is stored in green.
        return (packedColor >>> 24) != 0 || (packedColor & 0xFFFF00) == 0;
    }

    /**
     * @return {@code true} if the user has specified a background color
     *         that should override the application default, {@code false}
     *         otherwise
     */
    boolean hasBackgroundColor() {
        return mHasBackgroundColor;
    }

    /**
     * @return {@code true} if the user has specified a foreground color
     *         that should override the application default, {@code false}
     *         otherwise
     */
    boolean hasForegroundColor() {
        return mHasForegroundColor;
    }

    /**
     * @return {@code true} if the user has specified an edge type that
     *         should override the application default, {@code false}
     *         otherwise
     */
    boolean hasEdgeType() {
        return mHasEdgeType;
    }

    /**
     * @return {@code true} if the user has specified an edge color that
     *         should override the application default, {@code false}
     *         otherwise
     */
    boolean hasEdgeColor() {
        return mHasEdgeColor;
    }

    /**
     * @return {@code true} if the user has specified a window color that
     *         should override the application default, {@code false}
     *         otherwise
     */
    boolean hasWindowColor() {
        return mHasWindowColor;
    }

    /**
     * @return the preferred {@link Typeface} for video captions, or null if
     *         not specified
     */
    @Nullable
    public Typeface getTypeface() {
        return mParsedTypeface;
    }

    static {
        DEFAULT = new CaptionStyle(Color.WHITE, Color.BLACK, EDGE_TYPE_NONE,
                Color.BLACK, COLOR_NONE_OPAQUE, null);
    }

    @RequiresApi(19)
    static class Api19Impl {

        @DoNotInline
        static Typeface getTypeface(CaptioningManager.CaptionStyle captionStyle) {
            return captionStyle.getTypeface();
        }

        private Api19Impl() {}
    }
}
