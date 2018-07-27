/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.text.Spannable;
import android.text.style.ClickableSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextLinks.SpanFactory;
import androidx.textclassifier.TextLinks.TextLink;
import androidx.textclassifier.TextLinks.TextLinkSpan;

/**
 * Parameters for generating and applying links.
 */
public final class TextLinksParams {

    /**
     * A function to create spans from TextLinks.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SpanFactory DEFAULT_SPAN_FACTORY = new SpanFactory() {
        @Override
        public TextLinkSpan createSpan(TextLink textLink) {
            return new TextLinkSpan(textLink);
        }
    };

    @TextLinks.ApplyStrategy
    private final int mApplyStrategy;
    private final SpanFactory mSpanFactory;
    @Nullable private final TextClassifier.EntityConfig mEntityConfig;
    @Nullable private final LocaleListCompat mDefaultLocales;

    TextLinksParams(
            @TextLinks.ApplyStrategy int applyStrategy,
            SpanFactory spanFactory,
            @Nullable TextClassifier.EntityConfig entityConfig,
            @Nullable LocaleListCompat defaultLocales) {
        mApplyStrategy = applyStrategy;
        mSpanFactory = spanFactory;
        mEntityConfig = entityConfig;
        mDefaultLocales = defaultLocales;
    }

    /**
     * Returns the entity config used to determine what entity types to generate.
     */
    @Nullable
    TextClassifier.EntityConfig getEntityConfig() {
        return mEntityConfig;
    }

    /**
     * Returns an ordered list of locale preferences that can be used to disambiguate
     * the provided text
     */
    @Nullable
    LocaleListCompat getDefaultLocales() {
        return mDefaultLocales;
    }

    /**
     * Annotates the given text with the generated links. It will fail if the provided text doesn't
     * match the original text used to crete the TextLinks.
     *
     * @param text the text to apply the links to. Must match the original text
     * @param textLinks the links to apply to the text
     *
     * @return a status code indicating whether or not the links were successfully applied
     */
    @TextLinks.Status
    int apply(@NonNull Spannable text, @NonNull TextLinks textLinks) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(textLinks);

        if (!canApply(text, textLinks)) {
            return TextLinks.STATUS_DIFFERENT_TEXT;
        }
        if (textLinks.getLinks().isEmpty()) {
            return TextLinks.STATUS_NO_LINKS_FOUND;
        }

        int applyCount = 0;
        for (TextLink link : textLinks.getLinks()) {
            final TextLinkSpan span = mSpanFactory.createSpan(link);
            if (span != null) {
                final ClickableSpan[] existingSpans = text.getSpans(
                        link.getStart(), link.getEnd(), ClickableSpan.class);
                if (existingSpans.length > 0) {
                    if (mApplyStrategy == TextLinks.APPLY_STRATEGY_REPLACE) {
                        for (ClickableSpan existingSpan : existingSpans) {
                            text.removeSpan(existingSpan);
                        }
                        text.setSpan(span, link.getStart(), link.getEnd(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        applyCount++;
                    }
                } else {
                    text.setSpan(span, link.getStart(), link.getEnd(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    applyCount++;
                }
            }
        }
        if (applyCount == 0) {
            return TextLinks.STATUS_NO_LINKS_APPLIED;
        }
        return TextLinks.STATUS_LINKS_APPLIED;
    }

    /**
     * Returns true if it is possible to apply the specified textLinks to the specified text.
     * Otherwise, returns false.
     */
    boolean canApply(@NonNull Spannable text, @NonNull TextLinks textLinks) {
        return text.toString().startsWith(textLinks.getText());
    }

    /**
     * A builder for building TextLinksParams.
     */
    public static final class Builder {

        @TextLinks.ApplyStrategy
        private int mApplyStrategy = TextLinks.APPLY_STRATEGY_IGNORE;
        private SpanFactory mSpanFactory = DEFAULT_SPAN_FACTORY;
        @Nullable private TextClassifier.EntityConfig mEntityConfig;
        @Nullable private LocaleListCompat mDefaultLocales;

        /**
         * Sets the apply strategy used to determine how to apply links to text.
         *      e.g {@link TextLinks#APPLY_STRATEGY_IGNORE}
         *
         * @return this builder
         */
        public Builder setApplyStrategy(@TextLinks.ApplyStrategy int applyStrategy) {
            mApplyStrategy = checkApplyStrategy(applyStrategy);
            return this;
        }

        /**
         * Sets a custom span factory for converting TextLinks to TextLinkSpans.
         * Set to {@code null} to use the default span factory.
         *
         * @return this builder
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public Builder setSpanFactory(@Nullable SpanFactory spanFactory) {
            mSpanFactory = spanFactory == null ? DEFAULT_SPAN_FACTORY : spanFactory;
            return this;
        }

        /**
         * Sets the entity configuration to use. This determines what types of entities the
         * TextClassifier will look for.
         * Set to {@code null} for the default entity config and the TextClassifier will
         * automatically determine what links to generate.
         *
         * @return this builder
         */
        @NonNull
        public Builder setEntityConfig(@Nullable TextClassifier.EntityConfig entityConfig) {
            mEntityConfig = entityConfig;
            return this;
        }

        /**
         * @param defaultLocales ordered list of locale preferences that may be used to
         *                       disambiguate the provided text. If no locale preferences exist,
         *                       set this to null or an empty locale list.
         * @return this builder
         */
        @NonNull
        public Builder setDefaultLocales(@Nullable LocaleListCompat defaultLocales) {
            mDefaultLocales = defaultLocales;
            return this;
        }

        /**
         * Builds and returns a TextLinksParams object.
         */
        public TextLinksParams build() {
            return new TextLinksParams(
                    mApplyStrategy, mSpanFactory, mEntityConfig, mDefaultLocales);
        }
    }

    /** @throws IllegalArgumentException if the value is invalid */
    @TextLinks.ApplyStrategy
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static int checkApplyStrategy(int applyStrategy) {
        if (applyStrategy != TextLinks.APPLY_STRATEGY_IGNORE
                && applyStrategy != TextLinks.APPLY_STRATEGY_REPLACE) {
            throw new IllegalArgumentException(
                    "Invalid apply strategy. See TextLinksParams.ApplyStrategy for options.");
        }
        return applyStrategy;
    }
}
