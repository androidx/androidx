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

import static androidx.core.util.Preconditions.checkNotNull;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.wear.tiles.TileProviderService;
import androidx.wear.tiles.proto.ActionProto.Action;
import androidx.wear.tiles.proto.ActionProto.AndroidActivity;
import androidx.wear.tiles.proto.ActionProto.AndroidExtra;
import androidx.wear.tiles.proto.ActionProto.LaunchAction;
import androidx.wear.tiles.proto.ActionProto.LoadAction;
import androidx.wear.tiles.proto.DimensionProto.ContainerDimension;
import androidx.wear.tiles.proto.DimensionProto.ContainerDimension.InnerCase;
import androidx.wear.tiles.proto.DimensionProto.DpProp;
import androidx.wear.tiles.proto.DimensionProto.ExpandedDimensionProp;
import androidx.wear.tiles.proto.DimensionProto.ImageDimension;
import androidx.wear.tiles.proto.DimensionProto.ProportionalDimensionProp;
import androidx.wear.tiles.proto.DimensionProto.SpProp;
import androidx.wear.tiles.proto.DimensionProto.SpacerDimension;
import androidx.wear.tiles.proto.DimensionProto.WrappedDimensionProp;
import androidx.wear.tiles.proto.LayoutElementProto.Arc;
import androidx.wear.tiles.proto.LayoutElementProto.ArcAnchorTypeProp;
import androidx.wear.tiles.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto.ArcLine;
import androidx.wear.tiles.proto.LayoutElementProto.ArcSpacer;
import androidx.wear.tiles.proto.LayoutElementProto.ArcText;
import androidx.wear.tiles.proto.LayoutElementProto.Box;
import androidx.wear.tiles.proto.LayoutElementProto.Column;
import androidx.wear.tiles.proto.LayoutElementProto.ContentScaleMode;
import androidx.wear.tiles.proto.LayoutElementProto.FontStyle;
import androidx.wear.tiles.proto.LayoutElementProto.FontVariant;
import androidx.wear.tiles.proto.LayoutElementProto.HorizontalAlignmentProp;
import androidx.wear.tiles.proto.LayoutElementProto.Image;
import androidx.wear.tiles.proto.LayoutElementProto.Layout;
import androidx.wear.tiles.proto.LayoutElementProto.LayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto.Row;
import androidx.wear.tiles.proto.LayoutElementProto.Spacer;
import androidx.wear.tiles.proto.LayoutElementProto.Span;
import androidx.wear.tiles.proto.LayoutElementProto.SpanImage;
import androidx.wear.tiles.proto.LayoutElementProto.SpanText;
import androidx.wear.tiles.proto.LayoutElementProto.SpanVerticalAlignmentProp;
import androidx.wear.tiles.proto.LayoutElementProto.Spannable;
import androidx.wear.tiles.proto.LayoutElementProto.Text;
import androidx.wear.tiles.proto.LayoutElementProto.TextAlignmentProp;
import androidx.wear.tiles.proto.LayoutElementProto.TextOverflowProp;
import androidx.wear.tiles.proto.LayoutElementProto.VerticalAlignmentProp;
import androidx.wear.tiles.proto.ModifiersProto.ArcModifiers;
import androidx.wear.tiles.proto.ModifiersProto.Background;
import androidx.wear.tiles.proto.ModifiersProto.Border;
import androidx.wear.tiles.proto.ModifiersProto.Clickable;
import androidx.wear.tiles.proto.ModifiersProto.Modifiers;
import androidx.wear.tiles.proto.ModifiersProto.Padding;
import androidx.wear.tiles.proto.ModifiersProto.SpanModifiers;
import androidx.wear.tiles.proto.StateProto.State;
import androidx.wear.tiles.renderer.R;
import androidx.wear.tiles.renderer.internal.ResourceResolvers.ResourceAccessException;
import androidx.wear.tiles.renderer.internal.WearArcLayout.ArcLayoutWidget;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Renderer for Tiles.
 *
 * <p>This variant uses Android views to represent the contents of the Tile.
 */
public final class TileRendererInternal {

    private static final String TAG = "TileRendererInternal";

    private static final int HALIGN_DEFAULT_GRAVITY = Gravity.CENTER_HORIZONTAL;
    private static final int VALIGN_DEFAULT_GRAVITY = Gravity.CENTER_VERTICAL;
    private static final int TEXT_ALIGN_DEFAULT = Gravity.CENTER_HORIZONTAL;
    private static final ScaleType IMAGE_DEFAULT_SCALE_TYPE = ScaleType.FIT_CENTER;

    @WearArcLayout.LayoutParams.VerticalAlignment
    private static final int ARC_VALIGN_DEFAULT = WearArcLayout.LayoutParams.VALIGN_CENTER;

    private static final int SPAN_VALIGN_DEFAULT = ImageSpan.ALIGN_BOTTOM;

    // This is pretty badly named; TruncateAt specifies where to place the ellipsis (or whether to
    // marquee). Disabling truncation with null actually disables the _ellipsis_, but text will
    // still be truncated.
    @Nullable private static final TruncateAt TEXT_OVERFLOW_DEFAULT = null;

    private static final int TEXT_COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int TEXT_MAX_LINES_DEFAULT = 1;
    private static final int TEXT_MIN_LINES = 1;

    private static final ContainerDimension CONTAINER_DIMENSION_DEFAULT =
            ContainerDimension.newBuilder()
                    .setWrappedDimension(WrappedDimensionProp.getDefaultInstance())
                    .build();

    @WearArcLayout.AnchorType
    private static final int ARC_ANCHOR_DEFAULT = WearArcLayout.ANCHOR_CENTER;

    // White
    private static final int LINE_COLOR_DEFAULT = 0xFFFFFFFF;

    final Context mAppContext;
    private final Layout mLayoutProto;
    private final ResourceResolvers mResourceResolvers;

    private final FontSet mTitleFontSet;
    private final FontSet mBodyFontSet;

    final Executor mLoadActionExecutor;
    final LoadActionListener mLoadActionListener;

    /**
     * Listener for clicks on Clickable objects that have an Action to (re)load the contents of a
     * tile.
     */
    public interface LoadActionListener {

        /**
         * Called when a Clickable that has a LoadAction is clicked.
         *
         * @param nextState The state that the next tile should be in.
         */
        void onClick(@NonNull State nextState);
    }

    /**
     * Default constructor.
     *
     * @param appContext The application context.
     * @param layout The portion of the Tile to render.
     * @param resourceResolvers Resolvers for the resources used for rendering this Prototile.
     * @param loadActionExecutor Executor to dispatch loadActionListener on.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRendererInternal(
            @NonNull Context appContext,
            @NonNull Layout layout,
            @NonNull ResourceResolvers resourceResolvers,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this(
                appContext,
                layout,
                resourceResolvers,
                /* tilesTheme= */ 0,
                loadActionExecutor,
                loadActionListener);
    }

    /**
     * Default constructor.
     *
     * @param appContext The application context.
     * @param layout The portion of the Tile to render.
     * @param resourceResolvers Resolvers for the resources used for rendering this Prototile.
     * @param tilesTheme The theme to use for this Tiles instance. This can be used to customise
     *     things like the default font family. Pass 0 to use the default theme.
     * @param loadActionExecutor Executor to dispatch loadActionListener on.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRendererInternal(
            @NonNull Context appContext,
            @NonNull Layout layout,
            @NonNull ResourceResolvers resourceResolvers,
            @StyleRes int tilesTheme,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        if (tilesTheme == 0) {
            tilesTheme = R.style.TilesBaseTheme;
        }

        TypedArray a = appContext.obtainStyledAttributes(tilesTheme, R.styleable.TilesTheme);

        this.mTitleFontSet =
                new FontSet(appContext, a.getResourceId(R.styleable.TilesTheme_tilesTitleFont, -1));
        this.mBodyFontSet =
                new FontSet(appContext, a.getResourceId(R.styleable.TilesTheme_tilesBodyFont, -1));
        a.recycle();

        this.mAppContext = new ContextThemeWrapper(appContext, tilesTheme);
        this.mLayoutProto = layout;
        this.mResourceResolvers = resourceResolvers;
        this.mLoadActionExecutor = loadActionExecutor;
        this.mLoadActionListener = loadActionListener;
    }

    private int safeDpToPx(DpProp dpProp) {
        return round(
                max(0, dpProp.getValue()) * mAppContext.getResources().getDisplayMetrics().density);
    }

    @Nullable
    private static Float safeAspectRatioOrNull(
            ProportionalDimensionProp proportionalDimensionProp) {
        final int dividend = proportionalDimensionProp.getAspectRatioWidth();
        final int divisor = proportionalDimensionProp.getAspectRatioHeight();

        if (dividend <= 0 || divisor <= 0) {
            return null;
        }
        return (float) dividend / divisor;
    }

    private static Rect getSourceBounds(View v) {
        final int[] pos = new int[2];
        v.getLocationOnScreen(pos);

        return new Rect(
                /* left= */ pos[0],
                /* top= */ pos[1],
                /* right= */ pos[0] + v.getWidth(),
                /* bottom= */ pos[1] + v.getHeight());
    }

    /**
     * Generates a generic LayoutParameters for use by all components. This just defaults to setting
     * the width/height to WRAP_CONTENT.
     *
     * @return The default layout parameters.
     */
    private static ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams updateLayoutParamsInLinearLayout(
            LinearLayout parent,
            LayoutParams layoutParams,
            ContainerDimension width,
            ContainerDimension height) {
        // This is a little bit fun. Tiles' semantics is that dimension = expand should eat all
        // remaining space in that dimension, but not grow the parent. This is easy for standard
        // containers, but a little trickier in rows and columns on Android.
        //
        // A Row (LinearLayout) supports this with width=0 and weight>0. After doing a layout pass,
        // it will assign all remaining space to elements with width=0 and weight>0, biased by the
        // weight. This causes problems if there are two (or more) "expand" elements in a row,
        // which is itself set to WRAP_CONTENTS, and one of those elements has a measured width
        // (e.g. Text). In that case, the LinearLayout will measure the text, then ensure that
        // all elements with a weight set have their widths set according to the weight. For us,
        // that means that _all_ elements with expand=true will size themselves to the same width
        // as the Text, pushing out the bounds of the parent row. This happens on columns too,
        // but of course regarding height.
        //
        // To get around this, if an element with expand=true is added to a row that is WRAP_CONTENT
        // (e.g. a row with no explicit width, that is not expanded), we ignore the expand=true, and
        // set the inner element's width to WRAP_CONTENT too.

        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(layoutParams);

        // Handle the width
        if (parent.getOrientation() == LinearLayout.HORIZONTAL
                && width.getInnerCase() == InnerCase.EXPANDED_DIMENSION) {
            // If the parent container would not normally have "remaining space", ignore the
            // expand=true.
            if (parent.getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
                linearLayoutParams.width = LayoutParams.WRAP_CONTENT;
            } else {
                linearLayoutParams.width = 0;
                linearLayoutParams.weight = 1;
            }
        } else {
            linearLayoutParams.width = dimensionToPx(width);
        }

        // And the height
        if (parent.getOrientation() == LinearLayout.VERTICAL
                && height.getInnerCase() == InnerCase.EXPANDED_DIMENSION) {
            // If the parent container would not normally have "remaining space", ignore the
            // expand=true.
            if (parent.getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                linearLayoutParams.height = LayoutParams.WRAP_CONTENT;
            } else {
                linearLayoutParams.height = 0;
                linearLayoutParams.weight = 1;
            }
        } else {
            linearLayoutParams.height = dimensionToPx(height);
        }

        return linearLayoutParams;
    }

    private LayoutParams updateLayoutParams(
            ViewGroup parent,
            LayoutParams layoutParams,
            ContainerDimension width,
            ContainerDimension height) {
        if (parent instanceof LinearLayout) {
            // LinearLayouts have a bunch of messy caveats in Tiles when their children can be
            // expanded; factor that case out to keep this clean.
            return updateLayoutParamsInLinearLayout(
                    (LinearLayout) parent, layoutParams, width, height);
        } else {
            layoutParams.width = dimensionToPx(width);
            layoutParams.height = dimensionToPx(height);
        }

        return layoutParams;
    }

    private static int horizontalAlignmentToGravity(HorizontalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case HALIGN_START:
                return Gravity.START;
            case HALIGN_CENTER:
                return Gravity.CENTER_HORIZONTAL;
            case HALIGN_END:
                return Gravity.END;
            case HALIGN_LEFT:
                return Gravity.LEFT;
            case HALIGN_RIGHT:
                return Gravity.RIGHT;
            case UNRECOGNIZED:
            case HALIGN_UNDEFINED:
                return HALIGN_DEFAULT_GRAVITY;
        }

        return HALIGN_DEFAULT_GRAVITY;
    }

    private static int verticalAlignmentToGravity(VerticalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case VALIGN_TOP:
                return Gravity.TOP;
            case VALIGN_CENTER:
                return Gravity.CENTER_VERTICAL;
            case VALIGN_BOTTOM:
                return Gravity.BOTTOM;
            case UNRECOGNIZED:
            case VALIGN_UNDEFINED:
                return VALIGN_DEFAULT_GRAVITY;
        }

        return VALIGN_DEFAULT_GRAVITY;
    }

    @WearArcLayout.LayoutParams.VerticalAlignment
    private static int verticalAlignmentToArcVAlign(VerticalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case VALIGN_TOP:
                return WearArcLayout.LayoutParams.VALIGN_OUTER;
            case VALIGN_CENTER:
                return WearArcLayout.LayoutParams.VALIGN_CENTER;
            case VALIGN_BOTTOM:
                return WearArcLayout.LayoutParams.VALIGN_INNER;
            case UNRECOGNIZED:
            case VALIGN_UNDEFINED:
                return ARC_VALIGN_DEFAULT;
        }

        return ARC_VALIGN_DEFAULT;
    }

    private static ScaleType contentScaleModeToScaleType(ContentScaleMode contentScaleMode) {
        switch (contentScaleMode) {
            case CONTENT_SCALE_MODE_FIT:
                return ScaleType.FIT_CENTER;
            case CONTENT_SCALE_MODE_CROP:
                return ScaleType.CENTER_CROP;
            case CONTENT_SCALE_MODE_FILL_BOUNDS:
                return ScaleType.FIT_XY;
            case CONTENT_SCALE_MODE_UNDEFINED:
            case UNRECOGNIZED:
                return IMAGE_DEFAULT_SCALE_TYPE;
        }

        return IMAGE_DEFAULT_SCALE_TYPE;
    }

    private static int spanVerticalAlignmentToImgSpanAlignment(
            SpanVerticalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case SPAN_VALIGN_TEXT_BASELINE:
                return ImageSpan.ALIGN_BASELINE;
            case SPAN_VALIGN_BOTTOM:
                return ImageSpan.ALIGN_BOTTOM;
            case SPAN_VALIGN_UNDEFINED:
            case UNRECOGNIZED:
                return SPAN_VALIGN_DEFAULT;
        }

        return SPAN_VALIGN_DEFAULT;
    }

    /**
     * Whether a font style is bold or not (has weight > 700). Note that this check is required,
     * even if you are using an explicitly bold font (e.g. Roboto-Bold), as Typeface still needs to
     * bold bit set to render properly.
     */
    private static boolean isBold(FontStyle fontStyle) {
        // Although this method could be a simple equality check against FONT_WEIGHT_BOLD, we list
        // all current cases here so that this will become a compile time error as soon as a new
        // FontWeight value is added to the schema. If this fails to build, then this means that
        // an int typeface style is no longer enough to represent all FontWeight values and a
        // customizable, per-weight text style must be introduced to TileRendererInternal to
        // handle this. See b/176980535
        switch (fontStyle.getWeight().getValue()) {
            case FONT_WEIGHT_BOLD:
                return true;
            case FONT_WEIGHT_NORMAL:
            case FONT_WEIGHT_MEDIUM:
            case FONT_WEIGHT_UNDEFINED:
            case UNRECOGNIZED:
                return false;
        }

        return false;
    }

    private Typeface fontStyleToTypeface(FontStyle fontStyle) {
        FontSet fonts = mBodyFontSet;

        if (fontStyle.getVariant().getValue() == FontVariant.FONT_VARIANT_TITLE) {
            fonts = mTitleFontSet;
        }

        switch (fontStyle.getWeight().getValue()) {
            case FONT_WEIGHT_BOLD:
                return fonts.mBoldFont;
            case FONT_WEIGHT_MEDIUM:
                return fonts.mMediumFont;
            case FONT_WEIGHT_NORMAL:
            case FONT_WEIGHT_UNDEFINED:
            case UNRECOGNIZED:
                return fonts.mNormalFont;
        }

        return fonts.mNormalFont;
    }

    private static int fontStyleToTypefaceStyle(FontStyle fontStyle) {
        final boolean isBold = isBold(fontStyle);
        final boolean isItalic = fontStyle.getItalic().getValue();

        if (isBold && isItalic) {
            return Typeface.BOLD_ITALIC;
        } else if (isBold) {
            return Typeface.BOLD;
        } else if (isItalic) {
            return Typeface.ITALIC;
        } else {
            return Typeface.NORMAL;
        }
    }

    @SuppressWarnings("nullness")
    private Typeface createTypeface(FontStyle fontStyle) {
        return Typeface.create(fontStyleToTypeface(fontStyle), fontStyleToTypefaceStyle(fontStyle));
    }

    private static MetricAffectingSpan createTypefaceSpan(FontStyle fontStyle) {
        return new StyleSpan(fontStyleToTypefaceStyle(fontStyle));
    }

    /**
     * Returns whether or not the default style bits in Typeface can be used, or if we need to add
     * bold/italic flags there.
     */
    private static boolean hasDefaultTypefaceStyle(FontStyle fontStyle) {
        return !fontStyle.getItalic().getValue() && !isBold(fontStyle);
    }

    private float toPx(SpProp spField) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spField.getValue(),
                mAppContext.getResources().getDisplayMetrics());
    }

    private void applyFontStyle(FontStyle style, TextView textView) {
        // Need to supply typefaceStyle when creating the typeface (will select specialist
        // bold/italic typefaces), *and* when setting the typeface (will set synthetic
        // bold/italic flags in Paint if they're not supported by the given typeface).
        textView.setTypeface(createTypeface(style), fontStyleToTypefaceStyle(style));

        int currentPaintFlags = textView.getPaintFlags();

        // Remove the bits we're setting
        currentPaintFlags &= ~Paint.UNDERLINE_TEXT_FLAG;

        if (style.hasUnderline() && style.getUnderline().getValue()) {
            currentPaintFlags |= Paint.UNDERLINE_TEXT_FLAG;
        }

        textView.setPaintFlags(currentPaintFlags);

        if (style.hasSize()) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.getSize().getValue());
        }

        if (style.hasLetterSpacing()) {
            textView.setLetterSpacing(style.getLetterSpacing().getValue());
        }

        textView.setTextColor(extractTextColorArgb(style));
    }

    private void applyFontStyle(FontStyle style, WearCurvedTextView textView) {
        // Need to supply typefaceStyle when creating the typeface (will select specialist
        // bold/italic typefaces), *and* when setting the typeface (will set synthetic
        // bold/italic flags in Paint if they're not supported by the given typeface).
        textView.setTypeface(createTypeface(style), fontStyleToTypefaceStyle(style));

        int currentPaintFlags = textView.getPaintFlags();

        // Remove the bits we're setting
        currentPaintFlags &= ~Paint.UNDERLINE_TEXT_FLAG;

        if (style.hasUnderline() && style.getUnderline().getValue()) {
            currentPaintFlags |= Paint.UNDERLINE_TEXT_FLAG;
        }

        textView.setPaintFlags(currentPaintFlags);

        if (style.hasSize()) {
            textView.setTextSize(toPx(style.getSize()));
        }
    }

    private void applyClickable(View view, Clickable clickable) {
        view.setTag(clickable.getId());

        boolean hasAction = false;
        switch (clickable.getOnClick().getValueCase()) {
            case LAUNCH_ACTION:
                Intent i =
                        buildLaunchActionIntent(
                                clickable.getOnClick().getLaunchAction(), clickable.getId());
                if (i != null) {
                    hasAction = true;
                    view.setOnClickListener(
                            v -> {
                                i.setSourceBounds(getSourceBounds(view));
                                ActivityInfo ai =
                                        i.resolveActivityInfo(
                                                mAppContext.getPackageManager(), /* flags= */ 0);

                                if (ai != null && ai.exported) {
                                    mAppContext.startActivity(i);
                                }
                            });
                }
                break;
            case LOAD_ACTION:
                hasAction = true;
                view.setOnClickListener(
                        v ->
                                mLoadActionExecutor.execute(
                                        () ->
                                                mLoadActionListener.onClick(
                                                        buildState(
                                                                clickable
                                                                        .getOnClick()
                                                                        .getLoadAction(),
                                                                clickable.getId()))));
                break;
            case VALUE_NOT_SET:
                break;
        }

        if (hasAction) {
            // Apply ripple effect
            TypedValue outValue = new TypedValue();
            mAppContext
                    .getTheme()
                    .resolveAttribute(
                            android.R.attr.selectableItemBackground,
                            outValue,
                            /* resolveRefs= */ true);
            view.setForeground(mAppContext.getDrawable(outValue.resourceId));
        }
    }

    private void applyPadding(View view, Padding padding) {
        if (padding.getRtlAware().getValue()) {
            view.setPaddingRelative(
                    safeDpToPx(padding.getStart()),
                    safeDpToPx(padding.getTop()),
                    safeDpToPx(padding.getEnd()),
                    safeDpToPx(padding.getBottom()));
        } else {
            view.setPadding(
                    safeDpToPx(padding.getStart()),
                    safeDpToPx(padding.getTop()),
                    safeDpToPx(padding.getEnd()),
                    safeDpToPx(padding.getBottom()));
        }
    }

    private GradientDrawable applyBackground(
            View view, Background background, @Nullable GradientDrawable drawable) {
        if (drawable == null) {
            drawable = new GradientDrawable();
        }

        if (background.hasColor()) {
            drawable.setColor(background.getColor().getArgb());
        }

        if (background.hasCorner()) {
            drawable.setCornerRadius(safeDpToPx(background.getCorner().getRadius()));
            view.setClipToOutline(true);
            view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }

        return drawable;
    }

    private GradientDrawable applyBorder(Border border, @Nullable GradientDrawable drawable) {
        if (drawable == null) {
            drawable = new GradientDrawable();
        }

        drawable.setStroke(safeDpToPx(border.getWidth()), border.getColor().getArgb());

        return drawable;
    }

    private View applyModifiers(View view, Modifiers modifiers) {
        if (modifiers.hasClickable()) {
            applyClickable(view, modifiers.getClickable());
        }

        if (modifiers.hasSemantics()) {
            applyAudibleParams(view, modifiers.getSemantics().getContentDescription());
        }

        if (modifiers.hasPadding()) {
            applyPadding(view, modifiers.getPadding());
        }

        GradientDrawable backgroundDrawable = null;

        if (modifiers.hasBackground()) {
            backgroundDrawable =
                    applyBackground(view, modifiers.getBackground(), backgroundDrawable);
        }

        if (modifiers.hasBorder()) {
            backgroundDrawable = applyBorder(modifiers.getBorder(), backgroundDrawable);
        }

        if (backgroundDrawable != null) {
            view.setBackground(backgroundDrawable);
        }

        return view;
    }

    // This is a little nasty; ArcLayoutWidget is just an interface, so we have no guarantee that
    // the instance also extends View (as it should). Instead, just take a View in and rename
    // this, and check that it's an ArcLayoutWidget internally.
    private View applyModifiersToArcLayoutView(View view, ArcModifiers modifiers) {
        if (!(view instanceof ArcLayoutWidget)) {
            Log.e(
                    TAG,
                    "applyModifiersToArcLayoutView should only be called with an ArcLayoutWidget");
            return view;
        }

        if (modifiers.hasClickable()) {
            applyClickable(view, modifiers.getClickable());
        }

        if (modifiers.hasSemantics()) {
            applyAudibleParams(view, modifiers.getSemantics().getContentDescription());
        }

        return view;
    }

    private static int textAlignToAndroidGravity(TextAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case TEXT_ALIGN_START:
                return Gravity.START;
            case TEXT_ALIGN_CENTER:
                return Gravity.CENTER_HORIZONTAL;
            case TEXT_ALIGN_END:
                return Gravity.END;
            case TEXT_ALIGN_UNDEFINED:
            case UNRECOGNIZED:
                return TEXT_ALIGN_DEFAULT;
        }

        return TEXT_ALIGN_DEFAULT;
    }

    @Nullable
    private static TruncateAt textTruncationToEllipsize(TextOverflowProp type) {
        switch (type.getValue()) {
            case TEXT_OVERFLOW_TRUNCATE:
                // A null TruncateAt disables adding an ellipsis.
                return null;
            case TEXT_OVERFLOW_ELLIPSIZE_END:
                return TruncateAt.END;
            case TEXT_OVERFLOW_UNDEFINED:
            case UNRECOGNIZED:
                return TEXT_OVERFLOW_DEFAULT;
        }

        return TEXT_OVERFLOW_DEFAULT;
    }

    @WearArcLayout.AnchorType
    private static int anchorTypeToAnchorPos(ArcAnchorTypeProp type) {
        switch (type.getValue()) {
            case ARC_ANCHOR_START:
                return WearArcLayout.ANCHOR_START;
            case ARC_ANCHOR_CENTER:
                return WearArcLayout.ANCHOR_CENTER;
            case ARC_ANCHOR_END:
                return WearArcLayout.ANCHOR_END;
            case ARC_ANCHOR_UNDEFINED:
            case UNRECOGNIZED:
                return ARC_ANCHOR_DEFAULT;
        }

        return ARC_ANCHOR_DEFAULT;
    }

    private int dimensionToPx(ContainerDimension containerDimension) {
        switch (containerDimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return safeDpToPx(containerDimension.getLinearDimension());
            case EXPANDED_DIMENSION:
                return LayoutParams.MATCH_PARENT;
            case WRAPPED_DIMENSION:
                return LayoutParams.WRAP_CONTENT;
            case INNER_NOT_SET:
                return dimensionToPx(CONTAINER_DIMENSION_DEFAULT);
        }

        return dimensionToPx(CONTAINER_DIMENSION_DEFAULT);
    }

    private static int extractTextColorArgb(FontStyle fontStyle) {
        if (fontStyle.hasColor()) {
            return fontStyle.getColor().getArgb();
        } else {
            return TEXT_COLOR_DEFAULT;
        }
    }

    /**
     * Returns an Android {@link Intent} that can perform the action defined in the given tile
     * {@link LaunchAction}.
     */
    @Nullable
    public static Intent buildLaunchActionIntent(
            @NonNull LaunchAction launchAction, @NonNull String clickableId) {
        if (launchAction.hasAndroidActivity()) {
            AndroidActivity activity = launchAction.getAndroidActivity();
            Intent i =
                    new Intent().setClassName(activity.getPackageName(), activity.getClassName());
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (!clickableId.isEmpty()) {
                i.putExtra(TileProviderService.EXTRA_CLICKABLE_ID, clickableId);
            }

            for (Map.Entry<String, AndroidExtra> entry : activity.getKeyToExtraMap().entrySet()) {
                if (entry.getValue().hasStringVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getStringVal().getValue());
                } else if (entry.getValue().hasIntVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getIntVal().getValue());
                } else if (entry.getValue().hasLongVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getLongVal().getValue());
                } else if (entry.getValue().hasDoubleVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getDoubleVal().getValue());
                } else if (entry.getValue().hasBooleanVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getBooleanVal().getValue());
                }
            }

            return i;
        }

        return null;
    }

    static State buildState(LoadAction loadAction, String clickableId) {
        // Get the state specified by the provider and add the last clicked clickable's ID to it.
        return loadAction.getRequestState().toBuilder().setLastClickableId(clickableId).build();
    }

    @Nullable
    private View inflateColumn(ViewGroup parent, Column column) {
        ContainerDimension width =
                column.hasWidth() ? column.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height =
                column.hasHeight() ? column.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, column.getContentsList())) {
            Log.w(TAG, "Column set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        LinearLayout linearLayout = new LinearLayout(mAppContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        linearLayout.setGravity(horizontalAlignmentToGravity(column.getHorizontalAlignment()));

        layoutParams = updateLayoutParams(parent, layoutParams, width, height);

        View wrappedView = applyModifiers(linearLayout, column.getModifiers());

        parent.addView(wrappedView, layoutParams);
        inflateLayoutElements(linearLayout, column.getContentsList());

        return wrappedView;
    }

    @Nullable
    private View inflateRow(ViewGroup parent, Row row) {
        ContainerDimension width = row.hasWidth() ? row.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height = row.hasHeight() ? row.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, row.getContentsList())) {
            Log.w(TAG, "Row set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        LinearLayout linearLayout = new LinearLayout(mAppContext);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        linearLayout.setGravity(verticalAlignmentToGravity(row.getVerticalAlignment()));

        layoutParams = updateLayoutParams(parent, layoutParams, width, height);

        View wrappedView = applyModifiers(linearLayout, row.getModifiers());

        parent.addView(wrappedView, layoutParams);
        inflateLayoutElements(linearLayout, row.getContentsList());

        return wrappedView;
    }

    @Nullable
    private View inflateBox(ViewGroup parent, Box box) {
        ContainerDimension width = box.hasWidth() ? box.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height = box.hasHeight() ? box.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, box.getContentsList())) {
            Log.w(TAG, "Box set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        FrameLayout frame = new FrameLayout(mAppContext);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        layoutParams = updateLayoutParams(parent, layoutParams, width, height);

        int gravity =
                horizontalAlignmentToGravity(box.getHorizontalAlignment())
                        | verticalAlignmentToGravity(box.getVerticalAlignment());

        View wrappedView = applyModifiers(frame, box.getModifiers());

        parent.addView(wrappedView, layoutParams);
        inflateLayoutElements(frame, box.getContentsList());

        // We can't set layout gravity to a FrameLayout ahead of time (and foregroundGravity only
        // sets the gravity of the foreground Drawable). Go and apply gravity to the child.
        applyGravityToFrameLayoutChildren(frame, gravity);

        // HACK: FrameLayout has a bug in it. If we add one WRAP_CONTENT child, and one MATCH_PARENT
        // child, the expected behaviour is that the FrameLayout sizes itself to fit the
        // WRAP_CONTENT child (e.g. a TextView), then the MATCH_PARENT child is forced to the
        // same size as the outer FrameLayout (and hence, the size of the TextView, after
        // accounting for padding etc). Because of a bug though, this doesn't happen; instead,
        // the MATCH_PARENT child will just keep its intrinsic size. This is because FrameLayout
        // only forces MATCH_PARENT children to a given size if there are _more than one_ of them
        // (see the bottom of FrameLayout#onMeasure).
        //
        // To work around this (without copying the whole of FrameLayout just to change a "1" to
        // "0"), we add a Space element in if there is one MATCH_PARENT child. This has a tiny
        // cost to the measure pass, and negligible cost to layout/draw (since it doesn't take
        // part in those passes).
        int numMatchParentChildren = 0;
        for (int i = 0; i < frame.getChildCount(); i++) {
            LayoutParams lp = frame.getChildAt(i).getLayoutParams();
            if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
                numMatchParentChildren++;
            }
        }

        if (numMatchParentChildren == 1) {
            Space hackSpace = new Space(mAppContext);
            LayoutParams hackSpaceLp =
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            frame.addView(hackSpace, hackSpaceLp);
        }

        return wrappedView;
    }

    @Nullable
    private View inflateSpacer(ViewGroup parent, Spacer spacer) {
        int widthPx = safeDpToPx(spacer.getWidth().getLinearDimension());
        int heightPx = safeDpToPx(spacer.getHeight().getLinearDimension());

        if (widthPx == 0 && heightPx == 0) {
            return null;
        }

        LayoutParams layoutParams = generateDefaultLayoutParams();

        // Modifiers cannot be applied to android's Space, so use a plain View if this Spacer has
        // modifiers.
        View view;
        if (spacer.hasModifiers()) {
            view = applyModifiers(new View(mAppContext), spacer.getModifiers());
            layoutParams =
                    updateLayoutParams(
                            parent,
                            layoutParams,
                            spacerDimensionToContainerDimension(spacer.getWidth()),
                            spacerDimensionToContainerDimension(spacer.getHeight()));
        } else {
            view = new Space(mAppContext);
            view.setMinimumWidth(widthPx);
            view.setMinimumHeight(heightPx);
        }

        parent.addView(view, layoutParams);

        return view;
    }

    @Nullable
    private View inflateArcSpacer(ViewGroup parent, ArcSpacer spacer) {
        float lengthDegrees = max(0, spacer.getLength().getValue());
        int thicknessPx = safeDpToPx(spacer.getThickness());

        if (lengthDegrees == 0 && thicknessPx == 0) {
            return null;
        }

        WearCurvedSpacer space = new WearCurvedSpacer(mAppContext);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        space.setSweepAngleDegrees(lengthDegrees);
        space.setThicknessPx(thicknessPx);

        View wrappedView = applyModifiersToArcLayoutView(space, spacer.getModifiers());
        parent.addView(wrappedView, layoutParams);

        return wrappedView;
    }

    private View inflateText(ViewGroup parent, Text text) {
        TextView textView =
                new TextView(mAppContext, /* attrs= */ null, R.attr.tilesFallbackTextAppearance);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        textView.setText(text.getText().getValue());

        textView.setEllipsize(textTruncationToEllipsize(text.getOverflow()));
        textView.setGravity(textAlignToAndroidGravity(text.getMultilineAlignment()));

        if (text.hasMaxLines()) {
            textView.setMaxLines(max(TEXT_MIN_LINES, text.getMaxLines().getValue()));
        } else {
            textView.setMaxLines(TEXT_MAX_LINES_DEFAULT);
        }

        // Setting colours **must** go after setting the Text Appearance, otherwise it will get
        // immediately overridden.
        if (text.hasFontStyle()) {
            applyFontStyle(text.getFontStyle(), textView);
        } else {
            applyFontStyle(FontStyle.getDefaultInstance(), textView);
        }

        if (text.hasLineHeight()) {
            float lineHeightPx = toPx(text.getLineHeight());
            final float fontHeightPx = textView.getPaint().getFontSpacing();
            if (lineHeightPx != fontHeightPx) {
                textView.setLineSpacing(lineHeightPx - fontHeightPx, 1f);
            }
        }

        View wrappedView = applyModifiers(textView, text.getModifiers());
        parent.addView(wrappedView, layoutParams);

        // We don't want the text to be screen-reader focusable, unless wrapped in a Audible. This
        // prevents automatically reading out partial text (e.g. text in a row) etc.
        textView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        return wrappedView;
    }

    private View inflateArcText(ViewGroup parent, ArcText text) {
        WearCurvedTextView textView =
                new WearCurvedTextView(
                        mAppContext, /* attrs= */ null, R.attr.tilesFallbackTextAppearance);

        LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        textView.setText(text.getText().getValue());

        if (text.hasFontStyle()) {
            applyFontStyle(text.getFontStyle(), textView);
        }

        textView.setTextColor(extractTextColorArgb(text.getFontStyle()));

        View wrappedView = applyModifiersToArcLayoutView(textView, text.getModifiers());
        parent.addView(wrappedView, layoutParams);

        return wrappedView;
    }

    private static boolean isZeroLengthImageDimension(ImageDimension dimension) {
        return dimension.getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION
                && dimension.getLinearDimension().getValue() == 0;
    }

    private static ContainerDimension imageDimensionToContainerDimension(ImageDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return ContainerDimension.newBuilder()
                        .setLinearDimension(dimension.getLinearDimension())
                        .build();
            case EXPANDED_DIMENSION:
                return ContainerDimension.newBuilder()
                        .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance())
                        .build();
            case PROPORTIONAL_DIMENSION:
                // A ratio size should be translated to a WRAP_CONTENT; the RatioViewWrapper will
                // deal with the sizing of that.
                return ContainerDimension.newBuilder()
                        .setWrappedDimension(WrappedDimensionProp.getDefaultInstance())
                        .build();
            case INNER_NOT_SET:
                break;
        }
        // Caller should have already checked for this.
        throw new IllegalArgumentException(
                "ImageDimension has an unknown dimension type: " + dimension.getInnerCase().name());
    }

    private static ContainerDimension spacerDimensionToContainerDimension(
            SpacerDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return ContainerDimension.newBuilder()
                        .setLinearDimension(dimension.getLinearDimension())
                        .build();
            case INNER_NOT_SET:
                // A spacer is allowed to have missing dimension and this should be considered as
                // 0dp.
                return ContainerDimension.newBuilder()
                        .setLinearDimension(DpProp.getDefaultInstance())
                        .build();
        }
        // Caller should have already checked for this.
        throw new IllegalArgumentException(
                "SpacerDimension has an unknown dimension type: "
                        + dimension.getInnerCase().name());
    }

    @SuppressWarnings("ExecutorTaskName")
    @Nullable
    private View inflateImage(ViewGroup parent, Image image) {
        String protoResId = image.getResourceId().getValue();

        // If either width or height isn't set, abort.
        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.INNER_NOT_SET
                || image.getHeight().getInnerCase() == ImageDimension.InnerCase.INNER_NOT_SET) {
            Log.w(TAG, "One of width and height not set on image " + protoResId);
            return null;
        }

        // The image must occupy _some_ space.
        if (isZeroLengthImageDimension(image.getWidth())
                || isZeroLengthImageDimension(image.getHeight())) {
            Log.w(TAG, "One of width and height was zero on image " + protoResId);
            return null;
        }

        // Both dimensions can't be ratios.
        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION
                && image.getHeight().getInnerCase()
                        == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            Log.w(TAG, "Both width and height were proportional for image " + protoResId);
            return null;
        }

        // Pull the ratio for the RatioViewWrapper. Was either argument a proportional dimension?
        @Nullable Float ratio = RatioViewWrapper.UNDEFINED_ASPECT_RATIO;

        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            ratio = safeAspectRatioOrNull(image.getWidth().getProportionalDimension());
        }

        if (image.getHeight().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            ratio = safeAspectRatioOrNull(image.getHeight().getProportionalDimension());
        }

        if (ratio == null) {
            Log.w(TAG, "Invalid aspect ratio for image " + protoResId);
            return null;
        }

        ImageViewWithoutIntrinsicSizes imageView = new ImageViewWithoutIntrinsicSizes(mAppContext);

        if (image.hasContentScaleMode()) {
            imageView.setScaleType(
                    contentScaleModeToScaleType(image.getContentScaleMode().getValue()));
        }

        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION) {
            imageView.setMinimumWidth(safeDpToPx(image.getWidth().getLinearDimension()));
        }

        if (image.getHeight().getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION) {
            imageView.setMinimumHeight(safeDpToPx(image.getHeight().getLinearDimension()));
        }

        // We need to sort out the sizing of the widget now, so we can pass the correct params to
        // RatioViewWrapper. First, translate the ImageSize to a ContainerSize. A ratio size should
        // be translated to a WRAP_CONTENT; the RatioViewWrapper will deal with the sizing of that.
        LayoutParams ratioWrapperLayoutParams = generateDefaultLayoutParams();
        ratioWrapperLayoutParams =
                updateLayoutParams(
                        parent,
                        ratioWrapperLayoutParams,
                        imageDimensionToContainerDimension(image.getWidth()),
                        imageDimensionToContainerDimension(image.getHeight()));

        RatioViewWrapper ratioViewWrapper = new RatioViewWrapper(mAppContext);
        ratioViewWrapper.setAspectRatio(ratio);
        ratioViewWrapper.addView(imageView);

        // Finally, wrap the image in any modifiers...
        View wrappedView = applyModifiers(ratioViewWrapper, image.getModifiers());

        parent.addView(wrappedView, ratioWrapperLayoutParams);

        ListenableFuture<Drawable> drawableFuture = mResourceResolvers.getDrawable(protoResId);
        if (drawableFuture.isDone()) {
            // If the future is done, immediately draw.
            setImageDrawable(imageView, drawableFuture, protoResId);
        } else {
            // Is there a placeholder to use in the meantime?
            try {
                if (mResourceResolvers.hasPlaceholderDrawable(protoResId)) {
                    imageView.setImageDrawable(
                            mResourceResolvers.getPlaceholderDrawableOrThrow(protoResId));
                }
            } catch (ResourceAccessException ex) {
                Log.e(TAG, "Exception loading placeholder for resource " + protoResId, ex);
            }

            // Otherwise, handle the result on the UI thread.
            drawableFuture.addListener(
                    () -> setImageDrawable(imageView, drawableFuture, protoResId),
                    ContextCompat.getMainExecutor(mAppContext));
        }

        boolean canImageBeTinted = false;

        try {
            canImageBeTinted = mResourceResolvers.canImageBeTinted(protoResId);
        } catch (IllegalArgumentException ex) {
            // This implies that the image doesn't exist, but in that case, the above statement
            // should have thrown.
            Log.wtf(TAG, "Exception tinting image " + protoResId, ex);
        }

        if (image.hasFilter()) {
            if (image.getFilter().hasTint() && canImageBeTinted) {
                // Only allow tinting for Android images.
                ColorStateList tint = ColorStateList.valueOf(image.getFilter().getTint().getArgb());
                imageView.setImageTintList(tint);

                // SRC_IN throws away the colours in the drawable that we're tinting. Effectively,
                // the drawable being tinted is only a mask to apply the colour to.
                imageView.setImageTintMode(Mode.SRC_IN);
            }
        }

        return wrappedView;
    }

    private static void setImageDrawable(
            ImageView imageView, Future<Drawable> drawableFuture, String protoResId) {
        try {
            imageView.setImageDrawable(drawableFuture.get());
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "Could not get drawable for image " + protoResId);
        }
    }

    @Nullable
    private View inflateArcLine(ViewGroup parent, ArcLine line) {
        float lengthDegrees = max(0, line.getLength().getValue());
        int thicknessPx = safeDpToPx(line.getThickness());

        if (lengthDegrees == 0 && thicknessPx == 0) {
            return null;
        }

        WearCurvedLineView lineView = new WearCurvedLineView(mAppContext);

        // A ArcLineView must always be the same width/height as its parent, so it can draw the line
        // properly inside of those bounds.
        LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        int lineColor = LINE_COLOR_DEFAULT;
        if (line.hasColor()) {
            lineColor = line.getColor().getArgb();
        }

        lineView.setThicknessPx(thicknessPx);
        lineView.setSweepAngleDegrees(lengthDegrees);
        lineView.setColor(lineColor);

        View wrappedView = applyModifiersToArcLayoutView(lineView, line.getModifiers());
        parent.addView(wrappedView, layoutParams);

        return wrappedView;
    }

    @Nullable
    private View inflateArc(ViewGroup parent, Arc arc) {
        WearArcLayout arcLayout = new WearArcLayout(mAppContext);

        LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        arcLayout.setAnchorAngleDegrees(arc.getAnchorAngle().getValue());
        arcLayout.setAnchorType(anchorTypeToAnchorPos(arc.getAnchorType()));

        // Add all children.
        for (ArcLayoutElement child : arc.getContentsList()) {
            @Nullable View childView = inflateArcLayoutElement(arcLayout, child);
            if (childView != null) {
                WearArcLayout.LayoutParams childLayoutParams =
                        (WearArcLayout.LayoutParams) childView.getLayoutParams();
                boolean rotate = false;
                if (child.hasAdapter()) {
                    rotate = child.getAdapter().getRotateContents().getValue();
                }

                // Apply rotation and gravity.
                childLayoutParams.setRotate(rotate);
                childLayoutParams.setVerticalAlignment(
                        verticalAlignmentToArcVAlign(arc.getVerticalAlign()));
            }
        }

        View wrappedView = applyModifiers(arcLayout, arc.getModifiers());
        parent.addView(wrappedView, layoutParams);

        return wrappedView;
    }

    private void applyStylesToSpan(
            SpannableStringBuilder builder, int start, int end, FontStyle fontStyle) {
        if (fontStyle.hasSize()) {
            AbsoluteSizeSpan span = new AbsoluteSizeSpan(round(toPx(fontStyle.getSize())));
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.hasWeight() || fontStyle.hasVariant()) {
            CustomTypefaceSpan span = new CustomTypefaceSpan(fontStyleToTypeface(fontStyle));
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (!hasDefaultTypefaceStyle(fontStyle)) {
            MetricAffectingSpan span = createTypefaceSpan(fontStyle);
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.getUnderline().getValue()) {
            UnderlineSpan span = new UnderlineSpan();
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.hasLetterSpacing()) {
            LetterSpacingSpan span = new LetterSpacingSpan(fontStyle.getLetterSpacing().getValue());
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        ForegroundColorSpan colorSpan;

        colorSpan = new ForegroundColorSpan(extractTextColorArgb(fontStyle));

        builder.setSpan(colorSpan, start, end, Spanned.SPAN_MARK_MARK);
    }

    private void applyModifiersToSpan(
            SpannableStringBuilder builder, int start, int end, SpanModifiers modifiers) {
        if (modifiers.hasClickable()) {
            ClickableSpan clickableSpan = new TilesClickableSpan(modifiers.getClickable());

            builder.setSpan(clickableSpan, start, end, Spanned.SPAN_MARK_MARK);
        }
    }

    private SpannableStringBuilder inflateTextInSpannable(
            SpannableStringBuilder builder, SpanText text) {
        int currentPos = builder.length();
        int lastPos = currentPos + text.getText().getValue().length();

        builder.append(text.getText().getValue());

        applyStylesToSpan(builder, currentPos, lastPos, text.getFontStyle());
        applyModifiersToSpan(builder, currentPos, lastPos, text.getModifiers());

        return builder;
    }

    @SuppressWarnings("ExecutorTaskName")
    private SpannableStringBuilder inflateImageInSpannable(
            SpannableStringBuilder builder, SpanImage protoImage, TextView textView) {
        String protoResId = protoImage.getResourceId().getValue();

        if (protoImage.getWidth().getValue() == 0 || protoImage.getHeight().getValue() == 0) {
            Log.w(TAG, "One of width and height was zero on image " + protoResId);
            return builder;
        }

        ListenableFuture<Drawable> drawableFuture = mResourceResolvers.getDrawable(protoResId);
        if (drawableFuture.isDone()) {
            // If the future is done, immediately add drawable to builder.
            try {
                Drawable drawable = drawableFuture.get();
                appendSpanDrawable(builder, drawable, protoImage);
            } catch (ExecutionException | InterruptedException e) {
                Log.w(
                        TAG,
                        "Could not get drawable for image "
                                + protoImage.getResourceId().getValue());
            }
        } else {
            // If the future is not done, add an empty drawable to builder as a placeholder.
            @Nullable Drawable placeholderDrawable = null;

            try {
                if (mResourceResolvers.hasPlaceholderDrawable(protoResId)) {
                    placeholderDrawable =
                            mResourceResolvers.getPlaceholderDrawableOrThrow(protoResId);
                }
            } catch (ResourceAccessException ex) {
                Log.e(TAG, "Could not get placeholder for image " + protoResId, ex);
            }

            if (placeholderDrawable == null) {
                placeholderDrawable = new ColorDrawable(Color.TRANSPARENT);
            }

            int startInclusive = builder.length();
            FixedImageSpan placeholderDrawableSpan =
                    appendSpanDrawable(builder, placeholderDrawable, protoImage);
            int endExclusive = builder.length();

            // When the future is done, replace the empty drawable with the received one.
            drawableFuture.addListener(
                    () -> {
                        // Remove the placeholder. This should be safe, even with other modifiers
                        // applied. This just removes the single drawable span, and should leave
                        // other spans in place.
                        builder.removeSpan(placeholderDrawableSpan);
                        // Add the new drawable to the same range.
                        setSpanDrawable(
                                builder, drawableFuture, startInclusive, endExclusive, protoImage);
                        // Update the TextView.
                        textView.setText(builder);
                    },
                    ContextCompat.getMainExecutor(mAppContext));
        }

        return builder;
    }

    private FixedImageSpan appendSpanDrawable(
            SpannableStringBuilder builder, Drawable drawable, SpanImage protoImage) {
        drawable.setBounds(
                0, 0, safeDpToPx(protoImage.getWidth()), safeDpToPx(protoImage.getHeight()));
        FixedImageSpan imgSpan =
                new FixedImageSpan(
                        drawable,
                        spanVerticalAlignmentToImgSpanAlignment(protoImage.getAlignment()));

        int startPos = builder.length();

        // Note, we need to replace a real character, not a space. Spaces at the end of a line are
        // trimmed, which means that some image spans can be removed from the Span if they end up
        // being located at the end of a line.
        builder.append("A", imgSpan, Spanned.SPAN_MARK_MARK);
        int endPos = builder.length();

        applyModifiersToSpan(builder, startPos, endPos, protoImage.getModifiers());

        return imgSpan;
    }

    private void setSpanDrawable(
            SpannableStringBuilder builder,
            ListenableFuture<Drawable> drawableFuture,
            int startInclusive,
            int endExclusive,
            SpanImage protoImage) {
        final String protoResourceId = protoImage.getResourceId().getValue();

        try {
            // Add the image span to the same range occupied by the placeholder.
            Drawable drawable = drawableFuture.get();
            drawable.setBounds(
                    0, 0, safeDpToPx(protoImage.getWidth()), safeDpToPx(protoImage.getHeight()));
            FixedImageSpan imgSpan =
                    new FixedImageSpan(
                            drawable,
                            spanVerticalAlignmentToImgSpanAlignment(protoImage.getAlignment()));
            builder.setSpan(
                    imgSpan,
                    startInclusive,
                    endExclusive,
                    android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "Could not get drawable for image " + protoResourceId);
        }
    }

    private View inflateSpannable(ViewGroup parent, Spannable spannable) {
        TextView tv =
                new TextView(mAppContext, /* attrs= */ null, R.attr.tilesFallbackTextAppearance);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (Span element : spannable.getSpansList()) {
            switch (element.getInnerCase()) {
                case IMAGE:
                    SpanImage protoImage = element.getImage();
                    builder = inflateImageInSpannable(builder, protoImage, tv);
                    break;
                case TEXT:
                    SpanText protoText = element.getText();
                    builder = inflateTextInSpannable(builder, protoText);
                    break;
                default:
                    Log.w(TAG, "Unknown Span child type.");
                    break;
            }
        }

        tv.setEllipsize(textTruncationToEllipsize(spannable.getOverflow()));
        tv.setGravity(horizontalAlignmentToGravity(spannable.getMultilineAlignment()));

        if (spannable.hasMaxLines()) {
            tv.setMaxLines(max(TEXT_MIN_LINES, spannable.getMaxLines().getValue()));
        } else {
            tv.setMaxLines(TEXT_MAX_LINES_DEFAULT);
        }

        if (spannable.hasLineSpacing()) {
            tv.setLineSpacing(toPx(spannable.getLineSpacing()), 1f);
        }

        tv.setText(builder);

        View wrappedView = applyModifiers(tv, spannable.getModifiers());
        parent.addView(applyModifiers(tv, spannable.getModifiers()), layoutParams);

        return wrappedView;
    }

    @Nullable
    private View inflateArcLayoutElement(ViewGroup parent, ArcLayoutElement element) {
        View inflatedView = null;

        switch (element.getInnerCase()) {
            case ADAPTER:
                // Fall back to the normal inflater.
                inflatedView = inflateLayoutElement(parent, element.getAdapter().getContent());
                break;

            case SPACER:
                inflatedView = inflateArcSpacer(parent, element.getSpacer());
                break;

            case LINE:
                inflatedView = inflateArcLine(parent, element.getLine());
                break;

            case TEXT:
                inflatedView = inflateArcText(parent, element.getText());
                break;

            case INNER_NOT_SET:
                break;
        }

        if (inflatedView == null) {
            // Covers null (returned when the childCase in the proto isn't known). Sadly, ProtoLite
            // doesn't give us a way to access childCase's underlying tag, so we can't give any
            // smarter error message here.
            Log.w(TAG, "Unknown child type");
        }

        return inflatedView;
    }

    @Nullable
    private View inflateLayoutElement(ViewGroup parent, LayoutElement element) {
        // What is it?
        View inflatedView = null;
        switch (element.getInnerCase()) {
            case COLUMN:
                inflatedView = inflateColumn(parent, element.getColumn());
                break;
            case ROW:
                inflatedView = inflateRow(parent, element.getRow());
                break;
            case BOX:
                inflatedView = inflateBox(parent, element.getBox());
                break;
            case SPACER:
                inflatedView = inflateSpacer(parent, element.getSpacer());
                break;
            case TEXT:
                inflatedView = inflateText(parent, element.getText());
                break;
            case IMAGE:
                inflatedView = inflateImage(parent, element.getImage());
                break;
            case ARC:
                inflatedView = inflateArc(parent, element.getArc());
                break;
            case SPANNABLE:
                inflatedView = inflateSpannable(parent, element.getSpannable());
                break;
            case INNER_NOT_SET:
            default: // TODO(b/178359365): Remove default case
                Log.w(TAG, "Unknown child type: " + element.getInnerCase().name());
                break;
        }

        return inflatedView;
    }

    private boolean canMeasureContainer(
            ContainerDimension containerWidth,
            ContainerDimension containerHeight,
            List<LayoutElement> elements) {
        // We can't measure a container if it's set to wrap-contents but all of its contents are set
        // to expand-to-parent. Such containers must not be displayed.
        if (containerWidth.hasWrappedDimension() && !containsMeasurableWidth(elements)) {
            return false;
        }
        if (containerHeight.hasWrappedDimension() && !containsMeasurableHeight(elements)) {
            return false;
        }
        return true;
    }

    private boolean containsMeasurableWidth(List<LayoutElement> elements) {
        for (LayoutElement element : elements) {
            if (isWidthMeasurable(element)) {
                // Enough to find a single element that is measurable.
                return true;
            }
        }
        return false;
    }

    private boolean containsMeasurableHeight(List<LayoutElement> elements) {
        for (LayoutElement element : elements) {
            if (isHeightMeasurable(element)) {
                // Enough to find a single element that is measurable.
                return true;
            }
        }
        return false;
    }

    private boolean isWidthMeasurable(LayoutElement element) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return isMeasurable(element.getColumn().getWidth());
            case ROW:
                return isMeasurable(element.getRow().getWidth());
            case BOX:
                return isMeasurable(element.getBox().getWidth());
            case SPACER:
                return isMeasurable(element.getSpacer().getWidth());
            case IMAGE:
                return isMeasurable(element.getImage().getWidth());
            case ARC:
            case TEXT:
            case SPANNABLE:
                return true;
            case INNER_NOT_SET:
            default: // TODO(b/178359365): Remove default case
                return false;
        }
    }

    private boolean isHeightMeasurable(LayoutElement element) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return isMeasurable(element.getColumn().getHeight());
            case ROW:
                return isMeasurable(element.getRow().getHeight());
            case BOX:
                return isMeasurable(element.getBox().getHeight());
            case SPACER:
                return isMeasurable(element.getSpacer().getHeight());
            case IMAGE:
                return isMeasurable(element.getImage().getHeight());
            case ARC:
            case TEXT:
            case SPANNABLE:
                return true;
            case INNER_NOT_SET:
            default: // TODO(b/178359365): Remove default case
                return false;
        }
    }

    private boolean isMeasurable(ContainerDimension dimension) {
        return dimensionToPx(dimension) != LayoutParams.MATCH_PARENT;
    }

    private static boolean isMeasurable(ImageDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
            case PROPORTIONAL_DIMENSION:
                return true;
            case EXPANDED_DIMENSION:
            case INNER_NOT_SET:
                return false;
        }
        return false;
    }

    private static boolean isMeasurable(SpacerDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return true;
            case INNER_NOT_SET:
                return false;
        }
        return false;
    }

    private void inflateLayoutElements(ViewGroup parent, List<LayoutElement> elements) {
        for (LayoutElement element : elements) {
            inflateLayoutElement(parent, element);
        }
    }

    /**
     * Inflates a Tile into {@code parent}.
     *
     * @param parent The view to attach the tile into.
     * @return The first child that was inflated. This may be null if the proto is empty the
     *     top-level LayoutElement has no inner set, or the top-level LayoutElement contains an
     *     unsupported inner type.
     */
    @Nullable
    public View inflate(@NonNull ViewGroup parent) {
        // Go!
        return inflateLayoutElement(parent, mLayoutProto.getRoot());
    }

    private static void applyGravityToFrameLayoutChildren(FrameLayout parent, int gravity) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // All children should have a LayoutParams already set...
            if (!(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) {
                // This...shouldn't happen.
                throw new IllegalStateException(
                        "Layout params of child is not a descendant of FrameLayout.LayoutParams.");
            }

            // Children should grow out from the middle of the layout.
            ((FrameLayout.LayoutParams) child.getLayoutParams()).gravity = gravity;
        }
    }

    private static void applyAudibleParams(View view, String accessibilityLabel) {
        view.setContentDescription(accessibilityLabel);
        view.setFocusable(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** Implementation of ClickableSpan for Tiles' Clickables. */
    private class TilesClickableSpan extends ClickableSpan {
        private final Clickable mClickable;

        TilesClickableSpan(@NonNull Clickable clickable) {
            this.mClickable = clickable;
        }

        @Override
        public void onClick(@NonNull View widget) {
            Action action = mClickable.getOnClick();

            switch (action.getValueCase()) {
                case LAUNCH_ACTION:
                    Intent i =
                            buildLaunchActionIntent(action.getLaunchAction(), mClickable.getId());
                    if (i != null) {
                        if (i.resolveActivity(mAppContext.getPackageManager()) != null) {
                            mAppContext.startActivity(i);
                        }
                    }
                    break;
                case LOAD_ACTION:
                    mLoadActionExecutor.execute(
                            () ->
                                    mLoadActionListener.onClick(
                                            buildState(
                                                    action.getLoadAction(), mClickable.getId())));
                    break;
                case VALUE_NOT_SET:
                    break;
            }
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            // Don't change the underlying text appearance.
        }
    }

    // Android's normal ImageSpan (well, DynamicDrawableSpan) applies baseline alignment incorrectly
    // in some cases. It incorrectly assumes that the difference between the bottom (as passed to
    // draw) and baseline of the text is always equal to the font descent, when that doesn't always
    // hold. Instead, the "y" parameter is the Y coordinate of the baseline, so base the baseline
    // alignment on that rather than "bottom".
    private static class FixedImageSpan extends ImageSpan {
        private WeakReference<Drawable> mDrawableRef;

        FixedImageSpan(@NonNull Drawable drawable) {
            super(drawable);
        }

        FixedImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
            super(drawable, verticalAlignment);
        }

        @Override
        public void draw(
                @androidx.annotation.NonNull Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                @androidx.annotation.NonNull Paint paint) {
            Drawable b = getCachedDrawable();
            canvas.save();

            int transY = bottom - b.getBounds().bottom;
            if (mVerticalAlignment == ALIGN_BASELINE) {
                transY = y - b.getBounds().bottom;
            } else if (mVerticalAlignment == ALIGN_CENTER) {
                transY = (bottom - top) / 2 - b.getBounds().height() / 2;
            }

            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }

        private Drawable getCachedDrawable() {
            WeakReference<Drawable> wr = mDrawableRef;
            Drawable d = null;

            if (wr != null) {
                d = wr.get();
            }

            if (d == null) {
                d = getDrawable();
                mDrawableRef = new WeakReference<>(d);
            }

            return d;
        }
    }

    /** Holder for different weights of the same font variant. */
    private static class FontSet {
        final Typeface mNormalFont;
        final Typeface mMediumFont;
        final Typeface mBoldFont;

        FontSet(@NonNull Context appContext, int style) {
            TypedArray a = appContext.obtainStyledAttributes(style, R.styleable.TilesFontSet);

            this.mNormalFont = loadTypeface(a, R.styleable.TilesFontSet_tilesNormalFont);
            this.mMediumFont = loadTypeface(a, R.styleable.TilesFontSet_tilesMediumFont);
            this.mBoldFont = loadTypeface(a, R.styleable.TilesFontSet_tilesBoldFont);

            a.recycle();
        }

        @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
        private static Typeface loadTypeface(TypedArray array, int styleableResId) {
            // Resources are a little nasty; we can't just check if resType =
            // TypedValue.TYPE_REFERENCE, because it never is (if you use @font/foo inside of
            // styles.xml, the value will be a string of the form res/font/foo.ttf). Instead, see
            // if there's a resource ID at all, and use that, otherwise assume it's a well known
            // font family.
            int resType = array.getType(styleableResId);

            if (array.getResourceId(styleableResId, -1) != -1
                    && array.getFont(styleableResId) != null) {
                return checkNotNull(array.getFont(styleableResId));
            } else if (resType == TypedValue.TYPE_STRING
                    && array.getString(styleableResId) != null) {
                // Load the normal typeface; we customise this into BOLD/ITALIC later on.
                return Typeface.create(
                        checkNotNull(array.getString(styleableResId)), Typeface.NORMAL);
            } else {
                throw new IllegalArgumentException("Unknown resource value type " + resType);
            }
        }
    }
}
