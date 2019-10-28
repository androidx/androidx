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

import static androidx.textclassifier.ConvertUtils.toPlatformEntityConfig;
import static androidx.textclassifier.ConvertUtils.unwrapLocalListCompat;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.RemoteActionCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityConfig;
import androidx.textclassifier.TextClassifier.EntityType;
import androidx.textclassifier.widget.ToolbarController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A collection of links, representing subsequences of text and the entity types (phone number,
 * address, url, etc) they may be.
 */
public final class TextLinks {

    private static final String LOG_TAG = "TextLinks";
    private static final String EXTRA_FULL_TEXT = "text";
    private static final String EXTRA_LINKS = "links";
    private static final String EXTRA_EXTRAS = "extras";

    private final CharSequence mFullText;
    private final List<TextLink> mLinks;
    private final Bundle mExtras;

    static final Executor sWorkerExecutor = Executors.newFixedThreadPool(1);
    static final MainThreadExecutor sMainThreadExecutor = new MainThreadExecutor();

    /** Status unknown. */
    public static final int STATUS_UNKNOWN = -1;
    /** Links were successfully applied to the text. */
    public static final int STATUS_LINKS_APPLIED = 0;
    /** No links exist to apply to text. Links count is zero. */
    public static final int STATUS_NO_LINKS_FOUND = 1;
    /** No links applied to text. The links were filtered out. */
    public static final int STATUS_NO_LINKS_APPLIED = 2;
    /** The specified text does not match the text used to generate the links. */
    public static final int STATUS_DIFFERENT_TEXT = 3;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            STATUS_UNKNOWN,
            STATUS_LINKS_APPLIED,
            STATUS_NO_LINKS_FOUND,
            STATUS_NO_LINKS_APPLIED,
            STATUS_DIFFERENT_TEXT
    })
    public @interface Status {}

    /** Do not replace {@link ClickableSpan}s that exist where the {@link TextLinkSpan} needs to
     * be applied to. Do not apply the TextLinkSpan. **/
    public static final int APPLY_STRATEGY_IGNORE = 0;
    /** Replace any {@link ClickableSpan}s that exist where the {@link TextLinkSpan} needs to be
     * applied to. **/
    public static final int APPLY_STRATEGY_REPLACE = 1;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({APPLY_STRATEGY_IGNORE, APPLY_STRATEGY_REPLACE})
    public @interface ApplyStrategy {}

    TextLinks(CharSequence fullText, List<TextLink> links, Bundle extras) {
        mFullText = fullText;
        mLinks = Collections.unmodifiableList(links);
        mExtras = extras;
    }

    /**
     * Returns the text that was used to generate these links.
     */
    @NonNull
    public CharSequence getText() {
        return mFullText;
    }

    /**
     * Returns an unmodifiable Collection of the links.
     */
    @NonNull
    public Collection<TextLink> getLinks() {
        return mLinks;
    }

    /**
     * Returns the extended, vendor specific data.
     *
     * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
     * prefer to hold a reference to the returned bundle rather than frequently calling this
     * method. Avoid updating the content of this bundle. On pre-O devices, the values in the
     * Bundle are not deep copied.
     */
    @NonNull
    public Bundle getExtras() {
        return BundleUtils.deepCopy(mExtras);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.US, "TextLinks{fullText=%s, links=%s}", mFullText, mLinks);
    }

    /**
     * Adds a TextLinks object to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_FULL_TEXT, mFullText.toString());
        BundleUtils.putTextLinkList(bundle, EXTRA_LINKS, mLinks);
        bundle.putBundle(EXTRA_EXTRAS, mExtras);
        return bundle;
    }

    /**
     * Extracts an TextLinks object from a bundle that was added using {@link #toBundle()}.
     *
     * @throws IllegalArgumentException if the bundle is malformed.
     */
    @NonNull
    public static TextLinks createFromBundle(@NonNull Bundle bundle) {
        Bundle extras = bundle.getBundle(EXTRA_EXTRAS);
        return new TextLinks(
                bundle.getString(EXTRA_FULL_TEXT),
                BundleUtils.getTextLinkListOrThrow(bundle, EXTRA_LINKS),
                extras == null ? Bundle.EMPTY : extras);
    }

    /**
     * Annotates the given text with the generated links.
     *
     * <p><strong>NOTE: </strong>It may be necessary to set a LinkMovementMethod on the TextView
     * widget to properly handle links. See {@link TextView#setMovementMethod(MovementMethod)}.
     * It is also necessary that the TextView be focusable.
     * See {@link TextView#setFocusable(boolean)}} and
     * {@link TextView#setFocusableInTouchMode(boolean)}.
     *
     * @param text the text to apply the links to. Must match the original text
     * @param textClassifier the TextClassifier to use to classify a clicked link. Should usually
     *                       be the one used to generate the links
     * @param textLinksParams the param that specifies how the links should be applied
     *
     * @return the status code which indicates the operation is success or not.
     */
    @Status
    public int apply(
            @NonNull Spannable text,
            @NonNull TextClassifier textClassifier,
            @NonNull TextLinksParams textLinksParams) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(textClassifier);
        Preconditions.checkNotNull(textLinksParams);

        return textLinksParams.apply(text, this, textClassifier);
    }

    /**
     * A link, identifying a substring of text and possible entity types for it.
     */
    public static final class TextLink {

        private static final String EXTRA_ENTITY_SCORES = "scores";
        private static final String EXTRA_START = "start";
        private static final String EXTRA_END = "end";

        private final EntityConfidence mEntityScores;
        private final int mStart;
        private final int mEnd;

        /**
         * Create a new TextLink.
         *
         * @throws IllegalArgumentException if entityScores is null or empty.
         * @hide
         */
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        TextLink(int start, int end, @NonNull Map<String, Float> entityScores) {
            Preconditions.checkNotNull(entityScores);
            Preconditions.checkArgument(!entityScores.isEmpty());
            Preconditions.checkArgument(start <= end);
            mStart = start;
            mEnd = end;
            mEntityScores = new EntityConfidence(entityScores);
        }

        /**
         * Returns the start index of this link in the original text.
         *
         * @return the start index.
         */
        public int getStart() {
            return mStart;
        }

        /**
         * Returns the end index of this link in the original text.
         *
         * @return the end index.
         */
        public int getEnd() {
            return mEnd;
        }

        /**
         * Returns the number of entity types that have confidence scores.
         *
         * @return the entity type count.
         */
        public int getEntityTypeCount() {
            return mEntityScores.getEntities().size();
        }

        /**
         * Returns the entity type at a given index. Entity types are sorted by confidence.
         *
         * @return the entity type at the provided index.
         */
        @NonNull public @EntityType String getEntityType(int index) {
            return mEntityScores.getEntities().get(index);
        }

        /**
         * Returns the confidence score for a particular entity type.
         *
         * @param entityType the entity type.
         */
        public @FloatRange(from = 0.0, to = 1.0) float getConfidenceScore(
                @EntityType String entityType) {
            return mEntityScores.getConfidenceScore(entityType);
        }

        @Override
        @NonNull
        public String toString() {
            return String.format(Locale.US,
                    "TextLink{start=%s, end=%s, entityScores=%s}",
                    mStart, mEnd, mEntityScores);
        }

        /**
         * Adds this TextLink to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            BundleUtils.putMap(bundle, EXTRA_ENTITY_SCORES, mEntityScores.getConfidenceMap());
            bundle.putInt(EXTRA_START, mStart);
            bundle.putInt(EXTRA_END, mEnd);
            return bundle;
        }

        @NonNull
        EntityConfidence getEntityScores() {
            return mEntityScores;
        }

        /**
         * Extracts a TextLink from a bundle that was added using {@link #toBundle()}.
         */
        @NonNull
        public static TextLink createFromBundle(@NonNull Bundle bundle) {
            return new TextLink(
                    bundle.getInt(EXTRA_START),
                    bundle.getInt(EXTRA_END),
                    BundleUtils.getFloatStringMapOrThrow(bundle, EXTRA_ENTITY_SCORES));
        }
    }

    /**
     * A request object for generating TextLinks.
     */
    public static final class Request {

        private static final String EXTRA_TEXT = "text";
        private static final String EXTRA_DEFAULT_LOCALES = "locales";
        private static final String EXTRA_ENTITY_CONFIG = "entity_config";
        private static final String EXTRA_REFERENCE_TIME = "reference_time";

        private final CharSequence mText;
        @Nullable private final LocaleListCompat mDefaultLocales;
        @NonNull private final EntityConfig mEntityConfig;
        @Nullable private Long mReferenceTime = null;
        @NonNull private final Bundle mExtras;

        Request(
                @NonNull CharSequence text,
                @Nullable LocaleListCompat defaultLocales,
                @Nullable EntityConfig entityConfig,
                @Nullable Long referenceTime,
                @NonNull Bundle extras) {
            mText = SpannedString.valueOf(text);
            mDefaultLocales = defaultLocales;
            mEntityConfig = entityConfig == null
                    ? new TextClassifier.EntityConfig.Builder().build()
                    : entityConfig;
            mReferenceTime = referenceTime;
            mExtras = extras;
        }

        /**
         * Returns the text to generate links for.
         */
        @NonNull
        public CharSequence getText() {
            return mText;
        }

        /**
         * @return ordered list of locale preferences that can be used to disambiguate
         *      the provided text
         */
        @Nullable
        public LocaleListCompat getDefaultLocales() {
            return mDefaultLocales;
        }

        /**
         * @return The config representing the set of entities to look for
         * @see Builder#setEntityConfig(EntityConfig)
         */
        @NonNull
        public EntityConfig getEntityConfig() {
            return mEntityConfig;
        }

        /**
         * @return reference time based on which relative dates (e.g. "tomorrow") should be
         *      interpreted. This should be milliseconds from the epoch of
         *      1970-01-01T00:00:00Z(UTC timezone).
         */
        @Nullable
        public Long getReferenceTime() {
            return mReferenceTime;
        }

        /**
         * Returns the extended, vendor specific data.
         *
         * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
         * prefer to hold a reference to the returned bundle rather than frequently calling this
         * method. Avoid updating the content of this bundle. On pre-O devices, the values in the
         * Bundle are not deep copied.
         */
        @NonNull
        public Bundle getExtras() {
            return BundleUtils.deepCopy(mExtras);
        }

        /**
         * A builder for building TextLinks requests.
         */
        public static final class Builder {

            private final CharSequence mText;

            @Nullable private LocaleListCompat mDefaultLocales;
            @Nullable private EntityConfig mEntityConfig;
            @Nullable private Long mReferenceTime = null;
            @Nullable private Bundle mExtras;

            public Builder(@NonNull CharSequence text) {
                mText = Preconditions.checkNotNull(text);
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
             * Sets the entity configuration to use. This determines what types of entities the
             * TextClassifier will look for.
             * Set to {@code null} for the default entity config and the TextClassifier will
             * automatically determine what links to generate.
             *
             * @return this builder
             */
            @NonNull
            public Builder setEntityConfig(@Nullable EntityConfig entityConfig) {
                mEntityConfig = entityConfig;
                return this;
            }

            /**
             * @param referenceTime reference time based on which relative dates (e.g. "tomorrow")
             *      should be interpreted. This should usually be the time when the text was
             *      originally composed and should be milliseconds from the epoch of
             *      1970-01-01T00:00:00Z(UTC timezone). For example, if there is a message saying
             *      "see you 10 days later", and the message was composed yesterday, text classifier
             *      will then realize it is indeed means 9 days later from now and generate a link
             *      accordingly. If no reference time or {@code null} is set, now is used.
             *
             * @return this builder
             */
            @NonNull
            public TextLinks.Request.Builder setReferenceTime(
                    @Nullable Long referenceTime) {
                mReferenceTime = referenceTime;
                return this;
            }

            /**
             * Sets the extended, vendor specific data.
             */
            @NonNull
            public Builder setExtras(@Nullable Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Builds and returns the request object.
             */
            @NonNull
            public Request build() {
                return new Request(mText, mDefaultLocales, mEntityConfig, mReferenceTime,
                        mExtras == null ? Bundle.EMPTY : mExtras);
            }

        }

        /**
         * Adds this Request to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            bundle.putCharSequence(EXTRA_TEXT, mText);
            bundle.putBundle(EXTRA_ENTITY_CONFIG, mEntityConfig.toBundle());
            BundleUtils.putLocaleList(bundle, EXTRA_DEFAULT_LOCALES, mDefaultLocales);
            BundleUtils.putLong(bundle, EXTRA_REFERENCE_TIME, mReferenceTime);
            bundle.putBundle(EXTRA_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Extracts a Request from a bundle that was added using {@link #toBundle()}.
         */
        @NonNull
        public static Request createFromBundle(@NonNull Bundle bundle) {
            return new Builder(bundle.getCharSequence(EXTRA_TEXT))
                    .setDefaultLocales(BundleUtils.getLocaleList(bundle, EXTRA_DEFAULT_LOCALES))
                    .setEntityConfig(
                            EntityConfig.createFromBundle(bundle.getBundle(EXTRA_ENTITY_CONFIG)))
                    .setReferenceTime(BundleUtils.getLong(bundle, EXTRA_REFERENCE_TIME))
                    .setExtras(bundle.getBundle(EXTRA_EXTRAS))
                    .build();
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @NonNull
        android.view.textclassifier.TextLinks.Request toPlatform() {
            return new android.view.textclassifier.TextLinks.Request.Builder(getText())
                    .setDefaultLocales(unwrapLocalListCompat(getDefaultLocales()))
                    .setEntityConfig(toPlatformEntityConfig(getEntityConfig()))
                    .build();
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @NonNull
        static TextLinks.Request fromPlatform(
                @NonNull android.view.textclassifier.TextLinks.Request request) {
            return new TextLinks.Request.Builder(request.getText())
                    .setDefaultLocales(ConvertUtils.wrapLocalList(request.getDefaultLocales()))
                    .setEntityConfig(
                            TextClassifier.EntityConfig.fromPlatform(request.getEntityConfig()))
                    .build();
        }
    }

    /**
     * A factory to create spans from TextLinks.
     */
    public interface SpanFactory {

        /** Creates a span from a text link. */
        TextLinkSpan createSpan(@NonNull TextLinkSpanData textLinkSpanData);
    }

    /**
     * Contains necessary data for {@link TextLinkSpan}.
     */
    public static class TextLinkSpanData {
        @NonNull
        private final TextLink mTextLink;
        @NonNull
        private final TextClassifier mTextClassifier;
        @Nullable
        private final Long mReferenceTime;

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        TextLinkSpanData(
                @NonNull TextLink textLink,
                @NonNull TextClassifier textClassifier,
                @Nullable Long referenceTime) {
            mTextLink = Preconditions.checkNotNull(textLink);
            mTextClassifier = Preconditions.checkNotNull(textClassifier);
            mReferenceTime = referenceTime;
        }

        @NonNull
        public TextLink getTextLink() {
            return mTextLink;
        }

        /**
         * TODO: Make it public once we confirm how should we represent a datetime.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Nullable
        public Long getReferenceTime() {
            return mReferenceTime;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        TextClassifier getTextClassifier() {
            return mTextClassifier;
        }
    }

    /**
     * A ClickableSpan for a TextLink.
     * <p>
     * You can implement the {@link #onClick(View)} function to specify the on click behavior of
     * the span.
     */
    public abstract static class TextLinkSpan extends ClickableSpan {
        private TextLinkSpanData mTextLinkSpanData;

        public TextLinkSpan(@NonNull TextLinkSpanData textLinkSpanData) {
            mTextLinkSpanData = Preconditions.checkNotNull(textLinkSpanData);
        }

        /**
         * Returns the data that is relevant to this span.
         */
        @NonNull
        public final TextLinkSpanData getTextLinkSpanData() {
            return mTextLinkSpanData;
        }
    }

    /**
     * The default implementation of {@link TextLinkSpan}.
     * <p>
     * When this span is clicked, text classifier will be used to classify the text in the span and
     * suggest possible actions. The suggested actions will be presented to users eventually.
     * You can change the way how the suggested actions are presented to user by overriding
     * {@link #onTextClassificationResult(TextView, TextClassification)}.
     */
    public static class DefaultTextLinkSpan extends TextLinkSpan {

        /**
         * Constructs a DefaultTextLinkSpan.
         *
         * @param textLinkSpanData The data object that contains data of this span, like the text
         *                         link.
         */
        public DefaultTextLinkSpan(@NonNull TextLinkSpanData textLinkSpanData) {
            super(textLinkSpanData);
        }

        /**
         * Performs the click action associated with this span.
         * <p>
         * Subclass implementations should always call through to the superclass implementation.
         */
        @Override
        @CallSuper
        @SuppressLint("SyntheticAccessor")
        public void onClick(@NonNull View widget) {
            if (!(widget instanceof TextView)) {
                return;
            }

            final TextView textView = (TextView) widget;
            final CharSequence text = textView.getText();

            if (!(text instanceof Spanned)) {
                return;
            }

            final Spanned spanned = (Spanned) text;
            final int start = spanned.getSpanStart(this);
            final int end = spanned.getSpanEnd(this);
            if (start < 0 || start >= end || end > text.length()) {
                Log.d(LOG_TAG, "Cannot show link toolbar. Invalid text indices");
                return;
            }

            final TextClassification.Request request =
                    new TextClassification.Request.Builder(text, start, end)
                            .setReferenceTime(getTextLinkSpanData().getReferenceTime())
                            .setDefaultLocales(getLocales(textView))
                            .build();
            // TODO: Truncate the text.
            sWorkerExecutor.execute(new ClassifyTextRunnable(textView, this, request, spanned));
        }

        private static class ClassifyTextRunnable implements Runnable {
            private WeakReference<TextView> mTextView;
            private DefaultTextLinkSpan mTextLinkSpan;
            private TextClassification.Request mRequest;
            private Spanned mClassifiedSpan;

            private ClassifyTextRunnable(
                    TextView textView,
                    DefaultTextLinkSpan textLinkSpan,
                    TextClassification.Request request, Spanned classifiedSpan) {
                mTextView = new WeakReference<>(textView);
                mTextLinkSpan = textLinkSpan;
                mRequest = request;
                mClassifiedSpan = classifiedSpan;
            }

            @Override
            @SuppressLint("SyntheticAccessor")
            public void run() {
                final TextClassifier classifier =
                        mTextLinkSpan.getTextLinkSpanData().getTextClassifier();
                final TextClassification textClassification = classifier.classifyText(mRequest);
                sMainThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = mTextView.get();
                        if (textView == null) {
                            return;
                        }
                        if (textView.getText() != mClassifiedSpan) {
                            Log.d(LOG_TAG, "Text has changed from the classified text. "
                                    + "Ignoring.");
                            return;
                        }
                        mTextLinkSpan.onTextClassificationResult(textView, textClassification);
                    }
                });
            }
        }

        /**
         * Invoked when the classification of the text of this span is available.
         * <p>
         * When user clicks on this span, text classifier will be used to classify the text of
         * this span. Once text classifier has the result, this callback will be invoked. If the
         * text in the textview is changed during the text classification, this won't be invoked.
         * <p>
         * You can change the way how the suggested actions are presented to user by overriding
         * this function.
         *
         * @param textView the textview that this span is attached to
         * @param textClassification the text classification result of the text in this span.
         */
        @UiThread
        public void onTextClassificationResult(
                @NonNull TextView textView, @NonNull TextClassification textClassification) {
            List<RemoteActionCompat> actions = textClassification.getActions();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final Spanned spanned = SpannableString.valueOf(textView.getText());
                final int start = spanned.getSpanStart(this);
                final int end = spanned.getSpanEnd(this);
                ToolbarController.getInstance(textView).show(actions, start, end);
                return;
            }

            if (!actions.isEmpty()) {
                try {
                    actions.get(0).getActionIntent().send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(LOG_TAG, "Error handling TextLinkSpan click", e);
                }
                return;
            }
        }

        private LocaleListCompat getLocales(TextView textView) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                return ConvertUtils.wrapLocalList(textView.getTextLocales());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return LocaleListCompat.create(textView.getTextLocale());
            }
            return LocaleListCompat.create(Locale.getDefault());
        }
    }

    /**
     * A builder to construct a TextLinks instance.
     */
    public static final class Builder {
        private final CharSequence mFullText;
        private final ArrayList<TextLink> mLinks;
        @Nullable private Bundle mExtras;

        /**
         * Create a new TextLinks.Builder.
         *
         * @param fullText The full text to annotate with links.
         */
        public Builder(@NonNull CharSequence fullText) {
            mFullText = Preconditions.checkNotNull(fullText);
            mLinks = new ArrayList<>();
        }

        /**
         * Adds a TextLink.
         *
         * @return this instance.
         *
         * @throws IllegalArgumentException if entityScores is null or empty.
         */
        @NonNull
        public Builder addLink(int start, int end, @NonNull Map<String, Float> entityScores) {
            mLinks.add(new TextLink(start, end, Preconditions.checkNotNull(entityScores)));
            return this;
        }

        /**
         * @hide
         */
        @NonNull
        Builder addLink(TextLink link) {
            mLinks.add(Preconditions.checkNotNull(link));
            return this;
        }

        /**
         * Sets the extended, vendor specific data.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Removes all {@link TextLink}s.
         */
        // TODO: Hide.
        @NonNull
        public Builder clearTextLinks() {
            mLinks.clear();
            return this;
        }

        /**
         * Constructs a TextLinks instance.
         *
         * @return the constructed TextLinks.
         */
        @NonNull
        public TextLinks build() {
            return new TextLinks(mFullText, mLinks,
                    mExtras == null ? Bundle.EMPTY : BundleUtils.deepCopy(mExtras));
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(28)
    @NonNull
    // TODO: In Q, we should make getText public and use it here.
    static TextLinks fromPlatform(
            @NonNull android.view.textclassifier.TextLinks textLinks,
            @NonNull CharSequence requestText) {
        Preconditions.checkNotNull(textLinks);
        Preconditions.checkNotNull(requestText);

        Collection<android.view.textclassifier.TextLinks.TextLink> links = textLinks.getLinks();
        TextLinks.Builder builder = new TextLinks.Builder(requestText.toString());
        for (android.view.textclassifier.TextLinks.TextLink link : links) {
            builder.addLink(link.getStart(), link.getEnd(),
                    ConvertUtils.createFloatMapFromTextLinks(link));
        }
        return builder.build();
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(28)
    @NonNull
    android.view.textclassifier.TextLinks toPlatform() {
        android.view.textclassifier.TextLinks.Builder builder =
                new android.view.textclassifier.TextLinks.Builder((String) getText());
        for (TextLink textLink : getLinks()) {
            builder.addLink(
                    textLink.getStart(),
                    textLink.getEnd(),
                    textLink.getEntityScores().getConfidenceMap());
        }
        return builder.build();
    }
}
