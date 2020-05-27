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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.autofill.R;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.common.BundledStyle;
import androidx.autofill.inline.common.ImageViewStyle;
import androidx.autofill.inline.common.SlicedContent;
import androidx.autofill.inline.common.TextViewStyle;
import androidx.autofill.inline.common.ViewStyle;

import java.util.Collections;

/**
 * The entry point for building the content or style for the V1 inline suggestion UI.
 *
 * <p>The V1 UI composes of four widgets, put in order in a horizontal linear layout: start icon,
 * title, subtitle, and end icon. Some of the widgets are optional, or conditionally optional
 * based on existence of other widgets. See {@link Content.Builder#build()} for the conditions.
 *
 * <p>A default theme will be applied on the UI. The client can use {@link Style} to customize
 * the style for individual widgets as well as the overall UI background.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public final class InlineSuggestionUi {
    private static final String TAG = "InlineSuggestionUi";

    /**
     * Returns a builder to build the content for V1 inline suggestion UI.
     *
     * @param attributionIntent invoked when the UI is long-pressed.
     * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
     */
    @NonNull
    public static Content.Builder newContentBuilder(@NonNull PendingIntent attributionIntent) {
        return new Content.Builder(attributionIntent);
    }

    /**
     * Returns a builder to build the style for V1 inline suggestion UI.
     */
    @NonNull
    public static Style.Builder newStyleBuilder() {
        return new Style.Builder();
    }

    /**
     * @param contentSlice the content slice for V1
     * @return the V1 content created from the slice, or null if the slice is invalid
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static Content fromSlice(@NonNull Slice contentSlice) {
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
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static Style fromBundle(@NonNull Bundle styleBundle) {
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
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public static View render(@NonNull Context context, @NonNull Content content,
            @Nullable Style style) {
        context = getDefaultContextThemeWrapper(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final ViewGroup suggestionView =
                (ViewGroup) inflater.inflate(R.layout.autofill_inline_suggestion, null);

        final ImageView startIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_start_icon);
        final TextView titleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_title);
        final TextView subtitleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_subtitle);
        final ImageView endIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_end_icon);

        CharSequence title = content.getTitle();
        if (title != null) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        }
        CharSequence subtitle = content.getSubtitle();
        if (subtitle != null) {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }
        Icon startIcon = content.getStartIcon();
        if (startIcon != null) {
            startIconView.setImageIcon(startIcon);
            startIconView.setVisibility(View.VISIBLE);
        }
        Icon endIcon = content.getEndIcon();
        if (endIcon != null) {
            endIconView.setImageIcon(endIcon);
            endIconView.setVisibility(View.VISIBLE);
        }
        CharSequence contentDescription = content.getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            suggestionView.setContentDescription(contentDescription);
        }

        if (style != null && style.isValid()) {
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
     * @hide
     * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
     * @see Content#getAttributionIntent()
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static PendingIntent getAttributionIntent(@NonNull Content content) {
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
    public static final class Style extends BundledStyle implements UiVersions.Style {
        private static final String KEY_STYLE_V1 = "style_v1";
        private static final String KEY_CHIP_STYLE = "chip_style";
        private static final String KEY_SINGLE_ICON_CHIP_STYLE = "single_icon_chip_style";
        private static final String KEY_TITLE_STYLE = "title_style";
        private static final String KEY_SUBTITLE_STYLE = "subtitle_style";
        private static final String KEY_START_ICON_STYLE = "start_icon_style";
        private static final String KEY_END_ICON_STYLE = "end_icon_style";

        /**
         * Use {@link InlineSuggestionUi#fromBundle(Bundle)} or {@link Builder#build()} to
         * instantiate the class.
         */
        Style(@NonNull Bundle bundle) {
            super(bundle);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @Override
        protected String getStyleKey() {
            return KEY_STYLE_V1;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void applyStyle(@NonNull View singleIconChipView, @NonNull ImageView startIconView) {
            if (!isValid()) {
                return;
            }
            // start icon
            // start icon
            if (startIconView.getVisibility() != View.GONE) {
                ImageViewStyle startIconViewStyle = getStartIconStyle();
                if (startIconViewStyle != null) {
                    startIconViewStyle.applyStyleOnImageViewIfValid(startIconView);
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
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void applyStyle(@NonNull View chipView, @NonNull ImageView startIconView,
                @NonNull TextView titleView, @NonNull TextView subtitleView,
                @NonNull ImageView endIconView) {
            if (!isValid()) {
                return;
            }
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
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @Override
        public String getVersion() {
            return UiVersions.INLINE_UI_VERSION_1;
        }


        /**
         * @see Builder#setChipStyle(ViewStyle)
         */
        @Nullable
        public ViewStyle getChipStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_CHIP_STYLE);
            return styleBundle == null ? null : new ViewStyle(styleBundle);
        }

        /**
         * @see Builder#setSingleIconChipStyle(ViewStyle)
         */
        @Nullable
        public ViewStyle getSingleIconChipStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_SINGLE_ICON_CHIP_STYLE);
            return styleBundle == null ? null : new ViewStyle(styleBundle);
        }

        /**
         * @see Builder#setTitleStyle(TextViewStyle)
         */
        @Nullable
        public TextViewStyle getTitleStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_TITLE_STYLE);
            return styleBundle == null ? null : new TextViewStyle(styleBundle);
        }

        /**
         * @see Builder#setSubtitleStyle(TextViewStyle)
         */
        @Nullable
        public TextViewStyle getSubtitleStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_SUBTITLE_STYLE);
            return styleBundle == null ? null : new TextViewStyle(styleBundle);
        }

        /**
         * @see Builder#setStartIconStyle(ImageViewStyle)
         */
        @Nullable
        public ImageViewStyle getStartIconStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_START_ICON_STYLE);
            return styleBundle == null ? null : new ImageViewStyle(styleBundle);
        }

        /**
         * @see Builder#setEndIconStyle(ImageViewStyle)
         */
        @Nullable
        public ImageViewStyle getEndIconStyle() {
            Bundle styleBundle = mBundle.getBundle(KEY_END_ICON_STYLE);
            return styleBundle == null ? null : new ImageViewStyle(styleBundle);
        }

        /**
         * Builder for the {@link Style}.
         */
        public static final class Builder extends BundledStyle.Builder<Style> {

            /**
             * Use {@link InlineSuggestionUi#newStyleBuilder()} to instantiate this class.
             */
            Builder() {
                super(KEY_STYLE_V1);
            }

            /**
             * Sets the chip style.
             */
            @NonNull
            public Builder setChipStyle(@NonNull ViewStyle chipStyle) {
                chipStyle.assertIsValid();
                mBundle.putBundle(KEY_CHIP_STYLE, chipStyle.getBundle());
                return this;
            }

            /**
             * Sets the chip style for the case where there is a single icon and no text.
             */
            @NonNull
            public Builder setSingleIconChipStyle(@NonNull ViewStyle iconOnlyChipStyle) {
                iconOnlyChipStyle.assertIsValid();
                mBundle.putBundle(KEY_SINGLE_ICON_CHIP_STYLE, iconOnlyChipStyle.getBundle());
                return this;
            }

            /**
             * Sets the title style.
             */
            @NonNull
            public Builder setTitleStyle(@NonNull TextViewStyle titleStyle) {
                titleStyle.assertIsValid();
                mBundle.putBundle(KEY_TITLE_STYLE, titleStyle.getBundle());
                return this;
            }

            /**
             * Sets the subtitle style.
             */
            @NonNull
            public Builder setSubtitleStyle(@NonNull TextViewStyle subtitleStyle) {
                subtitleStyle.assertIsValid();
                mBundle.putBundle(KEY_SUBTITLE_STYLE, subtitleStyle.getBundle());
                return this;
            }

            /**
             * Sets the start icon style.
             */
            @NonNull
            public Builder setStartIconStyle(@NonNull ImageViewStyle startIconStyle) {
                startIconStyle.assertIsValid();
                mBundle.putBundle(KEY_START_ICON_STYLE, startIconStyle.getBundle());
                return this;
            }

            /**
             * Sets the end icon style.
             */
            @NonNull
            public Builder setEndIconStyle(@NonNull ImageViewStyle endIconStyle) {
                endIconStyle.assertIsValid();
                mBundle.putBundle(KEY_END_ICON_STYLE, endIconStyle.getBundle());
                return this;
            }

            @NonNull
            @Override
            public Style build() {
                return new Style(mBundle);
            }
        }
    }

    /**
     * Content for the V1 inline suggestion UI.
     */
    public static final class Content extends SlicedContent {
        static final String HINT_INLINE_TITLE = "inline_title";
        static final String HINT_INLINE_SUBTITLE = "inline_subtitle";
        static final String HINT_INLINE_START_ICON = "inline_start_icon";
        static final String HINT_INLINE_END_ICON = "inline_end_icon";
        static final String HINT_INLINE_ATTRIBUTION_INTENT = "inline_attribution";
        static final String HINT_INLINE_CONTENT_DESCRIPTION = "inline_content_description";

        @Nullable
        private Icon mStartIcon;
        @Nullable
        private Icon mEndIcon;
        @Nullable
        private CharSequence mTitle;
        @Nullable
        private CharSequence mSubtitle;
        @Nullable
        private PendingIntent mAttributionIntent;
        @Nullable
        private CharSequence mContentDescription;

        /**
         * Use {@link InlineSuggestionUi#fromSlice(Slice)} or {@link Builder#build()} to
         * instantiate this class.
         */
        Content(@NonNull Slice slice) {
            super(slice);
            for (SliceItem sliceItem : slice.getItems()) {
                String itemType = itemType(sliceItem);
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
        @Nullable
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @see Builder#setSubtitle(CharSequence)
         */
        @Nullable
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @see Builder#setStartIcon(Icon)
         */
        @Nullable
        public Icon getStartIcon() {
            return mStartIcon;
        }

        /**
         * @see Builder#setEndIcon(Icon)
         */
        @Nullable
        public Icon getEndIcon() {
            return mEndIcon;
        }

        /**
         * @see Builder#setContentDescription(CharSequence)
         */
        @Nullable
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @see InlineSuggestionUi#newContentBuilder(PendingIntent)
         */
        @Nullable
        @Override
        public PendingIntent getAttributionIntent() {
            return mAttributionIntent;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public boolean isValid() {
            return UiVersions.INLINE_UI_VERSION_1.equals(SlicedContent.getVersion(mSlice));
        }

        @Nullable
        private static String itemType(SliceItem sliceItem) {
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
        public static final class Builder extends SlicedContent.Builder<Content> {
            @NonNull
            private final PendingIntent mAttributionIntent;
            @Nullable
            private Icon mStartIcon;
            @Nullable
            private Icon mEndIcon;
            @Nullable
            private CharSequence mTitle;
            @Nullable
            private CharSequence mSubtitle;
            @Nullable
            private CharSequence mContentDescription;

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
            @NonNull
            public Builder setTitle(@NonNull CharSequence title) {
                mTitle = title;
                return this;
            }

            /**
             * Sets the subtitle of the suggestion UI.
             *
             * @param subtitle displayed as subtitle of slice.
             */
            @NonNull
            public Builder setSubtitle(@NonNull CharSequence subtitle) {
                mSubtitle = subtitle;
                return this;
            }

            /**
             * Sets the start icon of the suggestion UI.
             *
             * @param startIcon {@link Icon} resource displayed at start of slice.
             */
            @NonNull
            public Builder setStartIcon(@NonNull Icon startIcon) {
                mStartIcon = startIcon;
                return this;
            }

            /**
             * Sets the end icon of the suggestion UI.
             *
             * @param endIcon {@link Icon} resource displayed at end of slice.
             */
            @NonNull
            public Builder setEndIcon(@NonNull Icon endIcon) {
                mEndIcon = endIcon;
                return this;
            }

            /**
             * Sets the content description for the suggestion view.
             *
             * @param contentDescription the content description.
             * @see View#setContentDescription(CharSequence)
             */
            @NonNull
            public Builder setContentDescription(@NonNull CharSequence contentDescription) {
                mContentDescription = contentDescription;
                return this;
            }

            @NonNull
            @Override
            public Content build() {
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
                return new Content(mSliceBuilder.build());
            }
        }
    }
}
