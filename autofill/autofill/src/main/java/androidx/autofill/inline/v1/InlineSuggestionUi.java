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

package androidx.autofill.inline.v1;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.autofill.R;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.common.BundledStyle;
import androidx.autofill.inline.common.ImageViewStyle;
import androidx.autofill.inline.common.SlicedContent;
import androidx.autofill.inline.common.TextViewStyle;
import androidx.autofill.inline.common.ViewStyle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/**
 * The entry point for building the content or style for the V1 inline suggestion UI.
 *
 * <p>The V1 UI composes of four widgets, put in order in a horizontal linear layout: start icon,
 * title, subtitle, and end icon. Some of the widgets are optional, or conditionally optional
 * based on existence of other widgets. See {@link Content.Builder#build()} for the conditions.
 *
 * <p>A default theme will be applied on the UI. The client can use {@link Style} to customize
 * the style for individual widgets as well as the overall UI background.
 *
 * <p>For Autofill provider developer, to build a content {@link Slice} that can be used as input to
 * the {@link android.service.autofill.InlinePresentation}, you may use the
 * {@link InlineSuggestionUi.Content.Builder}. For example:
 *
 * <pre class="prettyprint">
 *   public Slice createSlice(
 *       InlinePresentationSpec imeSpec,
 *       CharSequence title,
 *       CharSequence subtitle,
 *       Icon startIcon,
 *       Icon endIcon,
 *       CharSequence contentDescription,
 *       PendingIntent attribution) {
 *     // Make sure that the IME spec claims support for v1 UI template.
 *     Bundle imeStyle = imeSpec.getStyle();
 *     if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1)) {
 *       return null;
 *     }
 *
 *     // Build the content for the v1 UI.
 *     Content.Builder builder =
 *         InlineSuggestionUi.newContentBuilder(attribution)
 *           .setContentDescription(contentDescription);
 *     if(!TextUtils.isEmpty(title)) {
 *       builder.setTitle(title);
 *     }
 *     if (!TextUtils.isEmpty(subtitle)) {
 *       builder.setSubtitle(subtitle);
 *     }
 *     if (startIcon != null) {
 *       startIcon.setTintBlendMode(BlendMode.DST)
 *       builder.setStartIcon(startIcon);
 *     }
 *     if (endIcon != null) {
 *       builder.setEndIcon(endIcon);
 *     }
 *     return builder.build().getSlice();
 *   }
 * </pre>
 *
 * <p>For IME developer, to build a styles {@link Bundle} that can be used as input to the
 * {@link android.widget.inline.InlinePresentationSpec}, you may use the
 * {@link UiVersions.StylesBuilder}. For example:
 *
 * <pre class="prettyprint">
 *   public Bundle createBundle(Bundle uiExtras) {
 *     // We have styles builder, because it's possible that the IME can support multiple UI
 *     // templates in the future.
 *     StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();
 *
 *     // Assuming we only want to support v1 UI template. If the provided uiExtras doesn't contain
 *     // v1, then return null.
 *     if (!UiVersions.getVersions(uiExtras).contains(UiVersions.INLINE_UI_VERSION_1)) {
 *       return null;
 *     }
 *
 *     // Create the style for v1 template.
 *     Style style = InlineSuggestionUi.newStyleBuilder()
 *         .setSingleIconChipStyle(
 *             new ViewStyle.Builder()
 *                 .setBackgroundColor(Color.TRANSPARENT)
 *                 .setPadding(0, 0, 0, 0)
 *                 .setLayoutMargin(0, 0, 0, 0)
 *                 .build())
 *         .setSingleIconChipIconStyle(
 *             new ImageViewStyle.Builder()
 *                 .setMaxWidth(actionIconSize)
 *                 .setMaxHeight(actionIconSize)
 *                 .setScaleType(ScaleType.FIT_CENTER)
 *                 .setLayoutMargin(0, 0, pinnedActionMarginEnd, 0)
 *                 .setTintList(actionIconColor)
 *                 .build())
 *         .setChipStyle(
 *             new ViewStyle.Builder()
 *                 .setBackground(
 *                     Icon.createWithResource(this, R.drawable.chip_background))
 *                 .setPadding(toPixel(13), 0, toPixel(13), 0)
 *                 .build())
 *         .setStartIconStyle(
 *             new ImageViewStyle.Builder()
 *                 .setLayoutMargin(0, 0, 0, 0)
 *                 .setTintList(chipIconColor)
 *                 .build())
 *         .setTitleStyle(
 *             new TextViewStyle.Builder()
 *                 .setLayoutMargin(toPixel(4), 0, toPixel(4), 0)
 *                 .setTextColor(Color.parseColor("#FF202124"))
 *                 .setTextSize(16)
 *                 .build())
 *         .setSubtitleStyle(
 *             new TextViewStyle.Builder()
 *                 .setLayoutMargin(0, 0, toPixel(4), 0)
 *                 .setTextColor(Color.parseColor("#99202124")) // 60% opacity
 *                 .setTextSize(14)
 *                 .build())
 *         .setEndIconStyle(
 *             new ImageViewStyle.Builder()
 *                 .setLayoutMargin(0, 0, 0, 0)
 *                 .setTintList(chipIconColor)
 *                 .build())
 *         .build();
 *
 *     // Add v1 UI style to the supported styles and return.
 *     stylesBuilder.addStyle(style);
 *     Bundle stylesBundle = stylesBuilder.build();
 *     return stylesBundle;
 *   }
 * </pre>
 *
 * <p>Alternatively, if the IME wants to use the default style, then:
 *
 * <pre class="prettyprint">
 *   public Bundle createBundle(Bundle uiExtras) {
 *     if (!UiVersions.getVersions(uiExtras).contains(UiVersions.INLINE_UI_VERSION_1)) {
 *       return null;
 *     }
 *     StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();
 *     stylesBuilder.addStyle(InlineSuggestionUi.newStyleBuilder().build());
 *     return stylesBuilder.build();
 *   }
 * </pre>
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public final class InlineSuggestionUi {
    private static final String TAG = "InlineSuggestionUi";

    /**
     * Returns a builder to build the content for V1 inline suggestion UI.
     *
     * <p><b>Important Note:</b> The
     * {@link android.service.autofill.AutofillService AutofillService} is responsible for keeping
     * track of the {@link PendingIntent} attribution intents it has used and cleaning them up
     * properly with {@link PendingIntent#cancel()}, or reusing them for the next set of
     * suggestions. Intents are safe to cleanup on receiving a new
     * {@link android.service.autofill.AutofillService#onFillRequest} call.
     * </p>
     *
     * @param attributionIntent invoked when the UI is long-pressed.
     * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
     */
    public static Content.@NonNull Builder newContentBuilder(
            @NonNull PendingIntent attributionIntent) {
        return new Content.Builder(attributionIntent);
    }

    /**
     * Returns a builder to build the style for V1 inline suggestion UI.
     */
    public static Style.@NonNull Builder newStyleBuilder() {
        return new Style.Builder();
    }

    /**
     * @param contentSlice the content slice for V1
     * @return the V1 content created from the slice, or null if the slice is invalid
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Content fromSlice(@NonNull Slice contentSlice) {
        Content content = new Content(contentSlice);
        if (!content.isValid()) {
            Log.w(TAG, "Invalid content for " + UiVersions.INLINE_UI_VERSION_1);
            return null;
        }
        return content;
    }

    /**
     * @param styleBundle the style bundle for V1
     * @return the V1 style created from the bundle, or null if the bundle is invalid
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Style fromBundle(@NonNull Bundle styleBundle) {
        Style style = new Style(styleBundle);
        if (!style.isValid()) {
            Log.w(TAG, "Invalid style for " + UiVersions.INLINE_UI_VERSION_1);
            return null;
        }
        return style;
    }

    /**
     * Renders the V1 inline suggestion view with the provided content and style.
     *
     */
    @RestrictTo(LIBRARY)
    public static @NonNull View render(@NonNull Context context, @NonNull Content content,
            @NonNull Style style) {
        context = getDefaultContextThemeWrapper(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        @LayoutRes final int layoutResId =
                style.isValid() && style.getLayoutTruncationPreference()
                        == Style.LAYOUT_TRUNCATION_PREFERENCE_BALANCING_TITLE_SUBTITLE
                        ? R.layout.autofill_inline_suggestion_balancing
                        : R.layout.autofill_inline_suggestion;
        final ViewGroup suggestionView = (ViewGroup) inflater.inflate(layoutResId, null);

        final ImageView startIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_start_icon);
        final TextView titleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_title);
        final TextView subtitleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_subtitle);
        final ImageView endIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_end_icon);

        final CharSequence title = content.getTitle();
        if (title != null) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        }
        final CharSequence subtitle = content.getSubtitle();
        if (subtitle != null) {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }
        final Icon startIcon = content.getStartIcon();
        if (startIcon != null) {
            startIconView.setImageIcon(startIcon);
            startIconView.setVisibility(View.VISIBLE);
        }
        final Icon endIcon = content.getEndIcon();
        if (endIcon != null) {
            endIconView.setImageIcon(endIcon);
            endIconView.setVisibility(View.VISIBLE);
        }
        final CharSequence contentDescription = content.getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            suggestionView.setContentDescription(contentDescription);
        }

        if (style.isValid()) {
            if (content.isSingleIconOnly()) {
                style.applyStyle(suggestionView, startIconView);
            } else {
                style.applyStyle(suggestionView, startIconView, titleView,
                        subtitleView, endIconView);
            }
        }
        return suggestionView;
    }

    /**
     * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
     * @see Content#getAttributionIntent()
     */
    @RestrictTo(LIBRARY)
    public static @Nullable PendingIntent getAttributionIntent(@NonNull Content content) {
        return content.getAttributionIntent();
    }

    private static Context getDefaultContextThemeWrapper(@NonNull Context context) {
        Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(R.style.Theme_AutofillInlineSuggestion, true);
        return new ContextThemeWrapper(context, theme);
    }

    private InlineSuggestionUi() {
    }

    /**
     * Style for the V1 inline suggestion UI.
     */
    @SuppressWarnings("HiddenSuperclass")
    public static final class Style extends BundledStyle implements UiVersions.Style {
        private static final String KEY_STYLE_V1 = "style_v1";
        private static final String KEY_CHIP_STYLE = "chip_style";
        private static final String KEY_TITLE_STYLE = "title_style";
        private static final String KEY_SUBTITLE_STYLE = "subtitle_style";
        private static final String KEY_START_ICON_STYLE = "start_icon_style";
        private static final String KEY_END_ICON_STYLE = "end_icon_style";
        private static final String KEY_SINGLE_ICON_CHIP_STYLE = "single_icon_chip_style";
        private static final String KEY_SINGLE_ICON_CHIP_ICON_STYLE = "single_icon_chip_icon_style";
        private static final String KEY_LAYOUT_DIRECTION = "layout_direction";
        private static final String KEY_LAYOUT_TRUNCATION_PREFERENCE =
                "layout_truncation_preference";

        /**
         * Use {@link InlineSuggestionUi#fromBundle(Bundle)} or {@link Builder#build()} to
         * instantiate the class.
         */
        Style(@NonNull Bundle bundle) {
            super(bundle);
        }

        /**
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Override
        protected @NonNull String getStyleKey() {
            return KEY_STYLE_V1;
        }

        /**
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void applyStyle(@NonNull View singleIconChipView,
                @NonNull ImageView singleIconView) {
            if (!isValid()) {
                return;
            }

            // layout direction
            singleIconChipView.setLayoutDirection(getLayoutDirection());

            // single icon
            if (singleIconView.getVisibility() != View.GONE) {
                ImageViewStyle singleIconViewStyle = getSingleIconChipIconStyle();
                if (singleIconViewStyle == null) {
                    singleIconViewStyle = getStartIconStyle();
                }
                if (singleIconViewStyle != null) {
                    singleIconViewStyle.applyStyleOnImageViewIfValid(singleIconView);
                }
            }
            // entire chip
            ViewStyle chipViewStyle = getSingleIconChipStyle();
            if (chipViewStyle == null) {
                chipViewStyle = getChipStyle();
            }
            if (chipViewStyle != null) {
                chipViewStyle.applyStyleOnViewIfValid(singleIconChipView);
            }
        }

        /**
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void applyStyle(@NonNull View chipView, @NonNull ImageView startIconView,
                @NonNull TextView titleView, @NonNull TextView subtitleView,
                @NonNull ImageView endIconView) {
            if (!isValid()) {
                return;
            }

            // layout direction
            chipView.setLayoutDirection(getLayoutDirection());

            // start icon
            if (startIconView.getVisibility() != View.GONE) {
                ImageViewStyle startIconViewStyle = getStartIconStyle();
                if (startIconViewStyle != null) {
                    startIconViewStyle.applyStyleOnImageViewIfValid(startIconView);
                }
            }
            // title
            if (titleView.getVisibility() != View.GONE) {
                TextViewStyle titleStyle = getTitleStyle();
                if (titleStyle != null) {
                    titleStyle.applyStyleOnTextViewIfValid(titleView);
                }
            }
            // subtitle
            if (subtitleView.getVisibility() != View.GONE) {
                TextViewStyle subtitleStyle = getSubtitleStyle();
                if (subtitleStyle != null) {
                    subtitleStyle.applyStyleOnTextViewIfValid(subtitleView);
                }
            }
            // end icon
            if (endIconView.getVisibility() != View.GONE) {
                ImageViewStyle endIconViewStyle = getEndIconStyle();
                if (endIconViewStyle != null) {
                    endIconViewStyle.applyStyleOnImageViewIfValid(endIconView);
                }
            }
            // entire chip
            ViewStyle chipViewStyle = getChipStyle();
            if (chipViewStyle != null) {
                chipViewStyle.applyStyleOnViewIfValid(chipView);
            }
        }

        /**
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Override
        public @NonNull String getVersion() {
            return UiVersions.INLINE_UI_VERSION_1;
        }

        /**
         * @see Builder#setLayoutDirection(int)
         */
        public int getLayoutDirection() {
            int layoutDirection = mBundle.getInt(KEY_LAYOUT_DIRECTION, View.LAYOUT_DIRECTION_LTR);
            if (layoutDirection != View.LAYOUT_DIRECTION_LTR
                    && layoutDirection != View.LAYOUT_DIRECTION_RTL) {
                layoutDirection = View.LAYOUT_DIRECTION_LTR;
            }
            return layoutDirection;
        }

        /**
         * @see Builder#setLayoutTruncationPreference(int)
         */
        public @LayoutTruncationPreference int getLayoutTruncationPreference() {
            return mBundle.getInt(KEY_LAYOUT_TRUNCATION_PREFERENCE,
                    LAYOUT_TRUNCATION_PREFERENCE_PRIORITIZE_TITLE);
        }

        /**
         * @see Builder#setChipStyle(ViewStyle)
         */
        public @Nullable ViewStyle getChipStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_CHIP_STYLE);
            return styleBundle == null ? null : new ViewStyle(styleBundle);
        }

        /**
         * @see Builder#setTitleStyle(TextViewStyle)
         */
        public @Nullable TextViewStyle getTitleStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_TITLE_STYLE);
            return styleBundle == null ? null : new TextViewStyle(styleBundle);
        }

        /**
         * @see Builder#setSubtitleStyle(TextViewStyle)
         */
        public @Nullable TextViewStyle getSubtitleStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_SUBTITLE_STYLE);
            return styleBundle == null ? null : new TextViewStyle(styleBundle);
        }

        /**
         * @see Builder#setStartIconStyle(ImageViewStyle)
         */
        public @Nullable ImageViewStyle getStartIconStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_START_ICON_STYLE);
            return styleBundle == null ? null : new ImageViewStyle(styleBundle);
        }

        /**
         * @see Builder#setEndIconStyle(ImageViewStyle)
         */
        public @Nullable ImageViewStyle getEndIconStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_END_ICON_STYLE);
            return styleBundle == null ? null : new ImageViewStyle(styleBundle);
        }

        /**
         * @see Builder#setSingleIconChipStyle(ViewStyle)
         */
        public @Nullable ViewStyle getSingleIconChipStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_SINGLE_ICON_CHIP_STYLE);
            return styleBundle == null ? null : new ViewStyle(styleBundle);
        }

        /**
         * @see Builder#setSingleIconChipIconStyle(ImageViewStyle)
         */
        public @Nullable ImageViewStyle getSingleIconChipIconStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_SINGLE_ICON_CHIP_ICON_STYLE);
            return styleBundle == null ? null : new ImageViewStyle(styleBundle);
        }

        /**
         * Prioritizes displaying the full title. The title is laid out first from left to right,
         * and the subtitle is only shown in the remaining space. If the title occupies the
         * entire width, the subtitle will be completely hidden or truncated.
         * This is the default value.
         */
        public static final int LAYOUT_TRUNCATION_PREFERENCE_PRIORITIZE_TITLE = 0;

        /**
         * Balances the truncation between the title and subtitle. Both elements may be truncated
         * to ensure that parts of each remain visible.
         */
        public static final int LAYOUT_TRUNCATION_PREFERENCE_BALANCING_TITLE_SUBTITLE = 1;

        /**
         * Describes the preferred behavior for truncating content in UI layout.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef({
                LAYOUT_TRUNCATION_PREFERENCE_PRIORITIZE_TITLE,
                LAYOUT_TRUNCATION_PREFERENCE_BALANCING_TITLE_SUBTITLE,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface LayoutTruncationPreference {
        }

        /**
         * Builder for the {@link Style}.
         */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder extends BundledStyle.Builder<Style> {

            /**
             * Use {@link InlineSuggestionUi#newStyleBuilder()} to instantiate this class.
             */
            Builder() {
                super(KEY_STYLE_V1);
            }

            /**
             * Sets the layout direction for the UI.
             *
             * <p>Note that the process that renders the UI needs to have
             * {@code android:supportsRtl="true"} for this to take effect.
             *
             * @param layoutDirection the layout direction to set. Should be one of:
             *                        {@link View#LAYOUT_DIRECTION_LTR},
             *                        {@link View#LAYOUT_DIRECTION_RTL}.
             *
             * @see View#setLayoutDirection(int)
             */
            public @NonNull Builder setLayoutDirection(int layoutDirection) {
                mBundle.putInt(KEY_LAYOUT_DIRECTION, layoutDirection);
                return this;
            }

            /**
             * Sets the layout truncation preference.
             *
             * <p>The default value is {@link #LAYOUT_TRUNCATION_PREFERENCE_PRIORITIZE_TITLE}.
             *
             * @see #LAYOUT_TRUNCATION_PREFERENCE_PRIORITIZE_TITLE
             * @see #LAYOUT_TRUNCATION_PREFERENCE_BALANCING_TITLE_SUBTITLE
             */
            public @NonNull Builder setLayoutTruncationPreference(
                    @LayoutTruncationPreference int layoutTruncationPreference) {
                mBundle.putInt(KEY_LAYOUT_TRUNCATION_PREFERENCE, layoutTruncationPreference);
                return this;
            }

            /**
             * Sets the chip style.
             *
             * <p>See {@link #setSingleIconChipStyle(ViewStyle)} for more information about setting
             * a special chip style for the case where the entire chip is a single icon.
             */
            public @NonNull Builder setChipStyle(@NonNull ViewStyle chipStyle) {
                chipStyle.assertIsValid();
                mBundle.putBundle(KEY_CHIP_STYLE, chipStyle.getBundle());
                return this;
            }

            /**
             * Sets the title style.
             */
            public @NonNull Builder setTitleStyle(@NonNull TextViewStyle titleStyle) {
                titleStyle.assertIsValid();
                mBundle.putBundle(KEY_TITLE_STYLE, titleStyle.getBundle());
                return this;
            }

            /**
             * Sets the subtitle style.
             */
            public @NonNull Builder setSubtitleStyle(@NonNull TextViewStyle subtitleStyle) {
                subtitleStyle.assertIsValid();
                mBundle.putBundle(KEY_SUBTITLE_STYLE, subtitleStyle.getBundle());
                return this;
            }

            /**
             * Sets the start icon style.
             *
             * <p>See {@link #setSingleIconChipIconStyle(ImageViewStyle)} for more information
             * about setting a special icon style for the case where the entire chip is a single
             * icon.
             */
            public @NonNull Builder setStartIconStyle(@NonNull ImageViewStyle startIconStyle) {
                startIconStyle.assertIsValid();
                mBundle.putBundle(KEY_START_ICON_STYLE, startIconStyle.getBundle());
                return this;
            }

            /**
             * Sets the end icon style.
             */
            public @NonNull Builder setEndIconStyle(@NonNull ImageViewStyle endIconStyle) {
                endIconStyle.assertIsValid();
                mBundle.putBundle(KEY_END_ICON_STYLE, endIconStyle.getBundle());
                return this;
            }

            /**
             * Sets the chip style for the case where there is a single icon and no text. If not
             * provided, will fallback to use the chip style provided by {@link #setChipStyle
             * (ViewStyle)}.
             */
            public @NonNull Builder setSingleIconChipStyle(@NonNull ViewStyle chipStyle) {
                chipStyle.assertIsValid();
                mBundle.putBundle(KEY_SINGLE_ICON_CHIP_STYLE, chipStyle.getBundle());
                return this;
            }

            /**
             * Sets the icon style for the case where there is a single icon and no text in the
             * chip. If not provided, will fallback to use the icon style provided by
             * {@link #setStartIconStyle(ImageViewStyle)}
             */
            public @NonNull Builder setSingleIconChipIconStyle(@NonNull ImageViewStyle iconStyle) {
                iconStyle.assertIsValid();
                mBundle.putBundle(KEY_SINGLE_ICON_CHIP_ICON_STYLE, iconStyle.getBundle());
                return this;
            }

            @Override
            public @NonNull Style build() {
                return new Style(mBundle);
            }
        }
    }

    /**
     * Content for the V1 inline suggestion UI.
     */
    @SuppressWarnings("HiddenSuperclass")
    public static final class Content extends SlicedContent {
        static final String HINT_INLINE_TITLE = "inline_title";
        static final String HINT_INLINE_SUBTITLE = "inline_subtitle";
        static final String HINT_INLINE_START_ICON = "inline_start_icon";
        static final String HINT_INLINE_END_ICON = "inline_end_icon";
        static final String HINT_INLINE_ATTRIBUTION_INTENT = "inline_attribution";
        static final String HINT_INLINE_CONTENT_DESCRIPTION = "inline_content_description";

        private @Nullable Icon mStartIcon;
        private @Nullable Icon mEndIcon;
        private @Nullable CharSequence mTitle;
        private @Nullable CharSequence mSubtitle;
        private @Nullable PendingIntent mAttributionIntent;
        private @Nullable CharSequence mContentDescription;

        /**
         * Use {@link InlineSuggestionUi#fromSlice(Slice)} or {@link Builder#build()} to
         * instantiate this class.
         */
        Content(@NonNull Slice slice) {
            super(slice);
            for (SliceItem sliceItem : slice.getItems()) {
                final String itemType = itemType(sliceItem);
                if (itemType == null) {
                    continue;
                }
                switch (itemType) {
                    case HINT_INLINE_TITLE:
                        mTitle = sliceItem.getText().toString();
                        break;
                    case HINT_INLINE_SUBTITLE:
                        mSubtitle = sliceItem.getText().toString();
                        break;
                    case HINT_INLINE_START_ICON:
                        mStartIcon = sliceItem.getIcon();
                        break;
                    case HINT_INLINE_END_ICON:
                        mEndIcon = sliceItem.getIcon();
                        break;
                    case HINT_INLINE_ATTRIBUTION_INTENT:
                        mAttributionIntent = sliceItem.getAction();
                        break;
                    case HINT_INLINE_CONTENT_DESCRIPTION:
                        mContentDescription = sliceItem.getText();
                        break;
                    default:
                        break;
                }
            }
        }

        boolean isSingleIconOnly() {
            return mStartIcon != null && mTitle == null && mSubtitle == null && mEndIcon == null;
        }

        /**
         * @see Builder#setTitle(CharSequence)
         */
        public @Nullable CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @see Builder#setSubtitle(CharSequence)
         */
        public @Nullable CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @see Builder#setStartIcon(Icon)
         */
        public @Nullable Icon getStartIcon() {
            return mStartIcon;
        }

        /**
         * @see Builder#setEndIcon(Icon)
         */
        public @Nullable Icon getEndIcon() {
            return mEndIcon;
        }

        /**
         * @see Builder#setContentDescription(CharSequence)
         */
        public @Nullable CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @see InlineSuggestionUi#newContentBuilder(PendingIntent)
         */
        @Override
        public @Nullable PendingIntent getAttributionIntent() {
            return mAttributionIntent;
        }

        /**
         */
        @RestrictTo(LIBRARY)
        @Override
        public boolean isValid() {
            return UiVersions.INLINE_UI_VERSION_1.equals(SlicedContent.getVersion(mSlice));
        }

        private static @Nullable String itemType(SliceItem sliceItem) {
            switch (sliceItem.getFormat()) {
                case FORMAT_IMAGE:
                    if (sliceItem.getIcon() == null) {
                        return null;
                    }
                    if (sliceItem.getHints().contains(HINT_INLINE_START_ICON)) {
                        return HINT_INLINE_START_ICON;
                    } else if (sliceItem.getHints().contains(HINT_INLINE_END_ICON)) {
                        return HINT_INLINE_END_ICON;
                    }
                    break;
                case FORMAT_TEXT:
                    if (TextUtils.isEmpty(sliceItem.getText())) {
                        return null;
                    }
                    if (sliceItem.getHints().contains(HINT_INLINE_TITLE)) {
                        return HINT_INLINE_TITLE;
                    } else if (sliceItem.getHints().contains(HINT_INLINE_SUBTITLE)) {
                        return HINT_INLINE_SUBTITLE;
                    } else if (sliceItem.getHints().contains(HINT_INLINE_CONTENT_DESCRIPTION)) {
                        return HINT_INLINE_CONTENT_DESCRIPTION;
                    }
                    break;
                case FORMAT_ACTION:
                    if (sliceItem.getAction() != null && sliceItem.getHints().contains(
                            HINT_INLINE_ATTRIBUTION_INTENT)) {
                        return HINT_INLINE_ATTRIBUTION_INTENT;
                    }
                    break;
                default:
                    return null;
            }
            return null;
        }

        /**
         * Builder for the {@link Content}.
         */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder extends SlicedContent.Builder<Content> {
            private final @NonNull PendingIntent mAttributionIntent;
            private @Nullable Icon mStartIcon;
            private @Nullable Icon mEndIcon;
            private @Nullable CharSequence mTitle;
            private @Nullable CharSequence mSubtitle;
            private @Nullable CharSequence mContentDescription;
            private @Nullable List<String> mHints;

            /**
             * Use {@link InlineSuggestionUi#newContentBuilder(PendingIntent)} to instantiate
             * this class.
             *
             * @param attributionIntent invoked when the UI is long-pressed.
             * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
             */
            Builder(@NonNull PendingIntent attributionIntent) {
                super(UiVersions.INLINE_UI_VERSION_1);
                mAttributionIntent = attributionIntent;
            }

            /**
             * Sets the title of the suggestion UI.
             *
             * @param title displayed as title of slice.
             */
            public @NonNull Builder setTitle(@NonNull CharSequence title) {
                mTitle = title;
                return this;
            }

            /**
             * Sets the subtitle of the suggestion UI.
             *
             * @param subtitle displayed as subtitle of slice.
             */
            public @NonNull Builder setSubtitle(@NonNull CharSequence subtitle) {
                mSubtitle = subtitle;
                return this;
            }

            /**
             * Sets the start icon of the suggestion UI.
             *
             * <p>Note that the {@link ImageViewStyle} style may specify the tint list to be
             * applied on the icon. If you don't want that, you may disable it by calling {@code
             * Icon#setTintBlendMode(BlendMode.DST)}.
             *
             * @param startIcon {@link Icon} resource displayed at start of slice.
             */
            public @NonNull Builder setStartIcon(@NonNull Icon startIcon) {
                mStartIcon = startIcon;
                return this;
            }

            /**
             * Sets the end icon of the suggestion UI.
             *
             * <p>Note that the {@link ImageViewStyle} style may specify the tint list to be
             * applied on the icon. If you don't want that, you may disable it by calling {@code
             * Icon#setTintBlendMode(BlendMode.DST)}.
             *
             * @param endIcon {@link Icon} resource displayed at end of slice.
             */
            public @NonNull Builder setEndIcon(@NonNull Icon endIcon) {
                mEndIcon = endIcon;
                return this;
            }

            /**
             * Sets the content description for the suggestion view.
             *
             * @param contentDescription the content description.
             * @see View#setContentDescription(CharSequence)
             */
            public @NonNull Builder setContentDescription(
                    @NonNull CharSequence contentDescription) {
                mContentDescription = contentDescription;
                return this;
            }

            /**
             * Sets hints to indicate the kind of data in the suggestion.
             *
             * @param hints defined in {@link androidx.autofill.inline.SuggestionHintConstants}
             */
            public @NonNull Builder setHints(@NonNull List<String> hints) {
                mHints = hints;
                return this;
            }

            @Override
            public @NonNull Content build() {
                if (mTitle == null && mStartIcon == null && mEndIcon == null && mSubtitle == null) {
                    throw new IllegalStateException(
                            "Title, subtitle, start icon, end icon are all null. "
                                    + "Please set value for at least one of them");
                }
                if (mTitle == null && mSubtitle != null) {
                    throw new IllegalStateException(
                            "Cannot set the subtitle without setting the title.");
                }
                if (mAttributionIntent == null) {
                    throw new IllegalStateException("Attribution intent cannot be null.");
                }
                if (mStartIcon != null) {
                    mSliceBuilder.addIcon(mStartIcon, null,
                            Collections.singletonList(HINT_INLINE_START_ICON));
                }
                if (mTitle != null) {
                    mSliceBuilder.addText(mTitle, null,
                            Collections.singletonList(HINT_INLINE_TITLE));
                }
                if (mSubtitle != null) {
                    mSliceBuilder.addText(mSubtitle, null,
                            Collections.singletonList(HINT_INLINE_SUBTITLE));
                }
                if (mEndIcon != null) {
                    mSliceBuilder.addIcon(mEndIcon, null,
                            Collections.singletonList(HINT_INLINE_END_ICON));
                }
                if (mAttributionIntent != null) {
                    mSliceBuilder.addAction(mAttributionIntent, new Slice.Builder(
                                    mSliceBuilder).addHints(
                            Collections.singletonList(HINT_INLINE_ATTRIBUTION_INTENT)).build(),
                            null);
                }
                if (mContentDescription != null) {
                    mSliceBuilder.addText(mContentDescription, null,
                            Collections.singletonList(HINT_INLINE_CONTENT_DESCRIPTION));
                }
                if (mHints != null) {
                    mSliceBuilder.addHints(mHints);
                }
                return new Content(mSliceBuilder.build());
            }
        }
    }
}
