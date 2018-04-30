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

package androidx.core.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ParagraphStyle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/**
 * Backwards compatible version of {@link Html}.
 */
@SuppressLint("InlinedApi") // Intentionally aliasing platform constants to compat versions.
public final class HtmlCompat {
    /**
     * Option for {@link #fromHtml(String, int)}: Wrap consecutive lines of text delimited by '\n'
     * inside &lt;p&gt; elements. {@link BulletSpan}s are ignored.
     */
    public static final int TO_HTML_PARAGRAPH_LINES_CONSECUTIVE =
            Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE;
    /**
     * Option for {@link #fromHtml(String, int)}: Wrap each line of text delimited by '\n' inside a
     * &lt;p&gt; or a &lt;li&gt; element. This allows {@link ParagraphStyle}s attached to be
     * encoded as CSS styles within the corresponding &lt;p&gt; or &lt;li&gt; element.
     */
    public static final int TO_HTML_PARAGRAPH_LINES_INDIVIDUAL =
            Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL;
    /**
     * Flag indicating that texts inside &lt;p&gt; elements will be separated from other texts with
     * one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH;
    /**
     * Flag indicating that texts inside &lt;h1&gt;~&lt;h6&gt; elements will be separated from
     * other texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_HEADING =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING;
    /**
     * Flag indicating that texts inside &lt;li&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM;
    /**
     * Flag indicating that texts inside &lt;ul&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST;
    /**
     * Flag indicating that texts inside &lt;div&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_DIV =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV;
    /**
     * Flag indicating that texts inside &lt;blockquote&gt; elements will be separated from other
     * texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE =
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE;
    /**
     * Flag indicating that CSS color values should be used instead of those defined in
     * {@link Color}.
     */
    public static final int FROM_HTML_OPTION_USE_CSS_COLORS = Html.FROM_HTML_OPTION_USE_CSS_COLORS;
    /**
     * Flags for {@link #fromHtml(String, int, ImageGetter, TagHandler)}: Separate block-level
     * elements with blank lines (two newline characters) in between. This is the legacy behavior
     * prior to N.
     */
    public static final int FROM_HTML_MODE_LEGACY = Html.FROM_HTML_MODE_LEGACY;
    /**
     * Flags for {@link #fromHtml(String, int, ImageGetter, TagHandler)}: Separate block-level
     * elements with line breaks (single newline character) in between. This inverts the
     * {@link Spanned} to HTML string conversion done with the option
     * {@link #TO_HTML_PARAGRAPH_LINES_INDIVIDUAL}.
     */
    public static final int FROM_HTML_MODE_COMPACT = Html.FROM_HTML_MODE_COMPACT;

    /** @hide */
    @IntDef(value = {
            FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH,
            FROM_HTML_SEPARATOR_LINE_BREAK_HEADING,
            FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM,
            FROM_HTML_SEPARATOR_LINE_BREAK_LIST,
            FROM_HTML_SEPARATOR_LINE_BREAK_DIV,
            FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE,
            FROM_HTML_OPTION_USE_CSS_COLORS,
            FROM_HTML_MODE_COMPACT,
            FROM_HTML_MODE_LEGACY
    }, flag = true)
    @RestrictTo(LIBRARY)
    @Retention(SOURCE)
    @interface FromHtmlFlags {
    }

    /** @hide */
    @IntDef({
            TO_HTML_PARAGRAPH_LINES_CONSECUTIVE,
            TO_HTML_PARAGRAPH_LINES_INDIVIDUAL
    })
    @RestrictTo(LIBRARY)
    @Retention(SOURCE)
    @interface ToHtmlOptions {
    }

    /**
     * Invokes {@link Html#fromHtml(String, int)} on API 24 and newer, otherwise {@code flags} are
     * ignored and {@link Html#fromHtml(String)} is used.
     */
    @NonNull
    public static Spanned fromHtml(@NonNull String source, @FromHtmlFlags int flags) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(source, flags);
        }
        //noinspection deprecation
        return Html.fromHtml(source);
    }

    /**
     * Invokes {@link Html#fromHtml(String, int, ImageGetter, TagHandler)} on API 24 or newer,
     * otherwise {@code flags} are ignored and
     * {@link Html#fromHtml(String, ImageGetter, TagHandler)} is used.
     */
    @NonNull
    public static Spanned fromHtml(@NonNull String source, @FromHtmlFlags int flags,
            @Nullable ImageGetter imageGetter, @Nullable TagHandler tagHandler) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(source, flags, imageGetter, tagHandler);
        }
        //noinspection deprecation
        return Html.fromHtml(source, imageGetter, tagHandler);
    }

    /**
     * Invokes {@link Html#toHtml(Spanned, int)} on API 24 or newer, otherwise {@code options} are
     * ignored and {@link Html#toHtml(Spanned)} is used.
     */
    @NonNull
    public static String toHtml(@NonNull Spanned text, @ToHtmlOptions int options) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.toHtml(text, options);
        }
        //noinspection deprecation
        return Html.toHtml(text);
    }

    private HtmlCompat() {
    }
}
