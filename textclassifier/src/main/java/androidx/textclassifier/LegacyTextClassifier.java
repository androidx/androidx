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
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.text.util.LinkifyCompat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Provides limited text classifier feature by using the legacy {@link LinkifyCompat} API.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class LegacyTextClassifier extends TextClassifier {
    private static final List<String> DEFAULT_ENTITIES =
            Arrays.asList(TextClassifier.TYPE_URL,
                    TextClassifier.TYPE_EMAIL,
                    TextClassifier.TYPE_PHONE);

    private static final int NOT_LINKIFY = 0;

    public static final LegacyTextClassifier INSTANCE = new LegacyTextClassifier();

    private LegacyTextClassifier() {
        super(SessionStrategy.NO_OP);
    }

    @WorkerThread
    @Override
    @NonNull
    /** @inheritDoc */
    public TextLinks generateLinks(@NonNull TextLinks.Request request) {
        final Collection<String> entities = request.getEntityConfig()
                .resolveEntityTypes(DEFAULT_ENTITIES);
        final String requestText = request.getText().toString();
        final TextLinks.Builder builder = new TextLinks.Builder(requestText);
        for (String entity : entities) {
            addLinks(builder, requestText, entity);
        }
        return builder.build();
    }

    private static void addLinks(
            TextLinks.Builder builder, String string, @EntityType String entityType) {
        final int linkifyMask = entityTypeToLinkifyMask(entityType);
        if (linkifyMask == NOT_LINKIFY) {
            return;
        }

        final Spannable spannable = new SpannableString(string);
        if (LinkifyCompat.addLinks(spannable, linkifyMask)) {
            final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            for (URLSpan urlSpan : spans) {
                builder.addLink(
                        spannable.getSpanStart(urlSpan),
                        spannable.getSpanEnd(urlSpan),
                        Collections.singletonMap(entityType, 1.0f),
                        urlSpan);
            }
        }
    }

    @LinkifyCompat.LinkifyMask
    private static int entityTypeToLinkifyMask(@EntityType String entityType) {
        switch (entityType) {
            case TextClassifier.TYPE_URL:
                return Linkify.WEB_URLS;
            case TextClassifier.TYPE_PHONE:
                return Linkify.PHONE_NUMBERS;
            case TextClassifier.TYPE_EMAIL:
                return Linkify.EMAIL_ADDRESSES;
            default:
                // NOTE: Do not support MAP_ADDRESSES. Legacy version does not work well.
                return NOT_LINKIFY;
        }
    }
}
