/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.text.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.PatternsCompat;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinkifyCompat brings in {@code Linkify} improvements for URLs and email addresses to older API
 * levels.
 */
public final class LinkifyCompat {
    private static final String[] EMPTY_STRING = new String[0];

    private static final Comparator<LinkSpec>  COMPARATOR = (a, b) -> {
        if (a.start < b.start) {
            return -1;
        }

        if (a.start > b.start) {
            return 1;
        }

        return Integer.compare(b.end, a.end);
    };

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true, value = { Linkify.WEB_URLS, Linkify.EMAIL_ADDRESSES, Linkify.PHONE_NUMBERS,
            Linkify.MAP_ADDRESSES, Linkify.ALL })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LinkifyMask {}

    /**
     *  Scans the text of the provided Spannable and turns all occurrences
     *  of the link types indicated in the mask into clickable links.
     *  If the mask is nonzero, it also removes any existing URLSpans
     *  attached to the Spannable, to avoid problems if you call it
     *  repeatedly on the same text.
     *
     *  @param text Spannable whose text is to be marked-up with links
     *  @param mask Mask to define which kinds of links will be searched.
     *
     *  @return True if at least one link is found and applied.
     */
    @SuppressWarnings("deprecation")
    public static boolean addLinks(@NonNull Spannable text, @LinkifyMask int mask) {
        if (shouldAddLinksFallbackToFramework()) {
            return Linkify.addLinks(text, mask);
        }
        if (mask == 0) {
            return false;
        }

        URLSpan[] old = text.getSpans(0, text.length(), URLSpan.class);

        for (int i = old.length - 1; i >= 0; i--) {
            text.removeSpan(old[i]);
        }

        if ((mask & Linkify.PHONE_NUMBERS) != 0) {
            Linkify.addLinks(text, Linkify.PHONE_NUMBERS);
        }

        final ArrayList<LinkSpec> links = new ArrayList<>();

        if ((mask & Linkify.WEB_URLS) != 0) {
            gatherLinks(links, text, PatternsCompat.AUTOLINK_WEB_URL,
                    new String[] { "http://", "https://", "rtsp://" },
                    Linkify.sUrlMatchFilter, null);
        }

        if ((mask & Linkify.EMAIL_ADDRESSES) != 0) {
            gatherLinks(links, text, PatternsCompat.AUTOLINK_EMAIL_ADDRESS,
                    new String[] { "mailto:" },
                    null, null);
        }

        if ((mask & Linkify.MAP_ADDRESSES) != 0) {
            gatherMapLinks(links, text);
        }

        pruneOverlaps(links, text);

        if (links.size() == 0) {
            return false;
        }

        for (LinkSpec link: links) {
            if (link.frameworkAddedSpan == null) {
                applyLink(link.url, link.start, link.end, text);
            }
        }

        return true;
    }

    /**
     *  Scans the text of the provided TextView and turns all occurrences of
     *  the link types indicated in the mask into clickable links.  If matches
     *  are found the movement method for the TextView is set to
     *  LinkMovementMethod.
     *
     *  @param text TextView whose text is to be marked-up with links
     *  @param mask Mask to define which kinds of links will be searched.
     *
     *  @return True if at least one link is found and applied.
     */
    public static boolean addLinks(@NonNull TextView text, @LinkifyMask int mask) {
        if (shouldAddLinksFallbackToFramework()) {
            return Linkify.addLinks(text, mask);
        }
        if (mask == 0) {
            return false;
        }

        CharSequence t = text.getText();

        if (t instanceof Spannable) {
            if (addLinks((Spannable) t, mask)) {
                addLinkMovementMethod(text);
                return true;
            }

        } else {
            SpannableString s = SpannableString.valueOf(t);

            if (addLinks(s, mask)) {
                addLinkMovementMethod(text);
                text.setText(s);

                return true;
            }

        }
        return false;
    }

    /**
     *  Applies a regex to the text of a TextView turning the matches into
     *  links.  If links are found then UrlSpans are applied to the link
     *  text match areas, and the movement method for the text is changed
     *  to LinkMovementMethod.
     *
     *  @param text         TextView whose text is to be marked-up with links
     *  @param pattern      Regex pattern to be used for finding links
     *  @param scheme       URL scheme string (eg <code>http://</code>) to be
     *                      prepended to the links that do not start with this scheme.
     */
    public static void addLinks(@NonNull TextView text, @NonNull Pattern pattern,
            @Nullable String scheme) {
        if (shouldAddLinksFallbackToFramework()) {
            Linkify.addLinks(text, pattern, scheme);
            return;
        }
        addLinks(text, pattern, scheme, null, null, null);
    }

    /**
     *  Applies a regex to the text of a TextView turning the matches into
     *  links.  If links are found then UrlSpans are applied to the link
     *  text match areas, and the movement method for the text is changed
     *  to LinkMovementMethod.
     *
     *  @param text         TextView whose text is to be marked-up with links
     *  @param pattern      Regex pattern to be used for finding links
     *  @param scheme       URL scheme string (eg <code>http://</code>) to be
     *                      prepended to the links that do not start with this scheme.
     *  @param matchFilter  The filter that is used to allow the client code
     *                      additional control over which pattern matches are
     *                      to be converted into links.
     */
    public static void addLinks(@NonNull TextView text, @NonNull Pattern pattern,
            @Nullable String scheme, @Nullable MatchFilter matchFilter,
            @Nullable TransformFilter transformFilter) {
        if (shouldAddLinksFallbackToFramework()) {
            Linkify.addLinks(text, pattern, scheme, matchFilter, transformFilter);
            return;
        }
        addLinks(text, pattern, scheme, null, matchFilter, transformFilter);
    }

    /**
     *  Applies a regex to the text of a TextView turning the matches into
     *  links.  If links are found then UrlSpans are applied to the link
     *  text match areas, and the movement method for the text is changed
     *  to LinkMovementMethod.
     *
     *  @param text TextView whose text is to be marked-up with links.
     *  @param pattern Regex pattern to be used for finding links.
     *  @param defaultScheme The default scheme to be prepended to links if the link does not
     *                       start with one of the <code>schemes</code> given.
     *  @param schemes Array of schemes (eg <code>http://</code>) to check if the link found
     *                 contains a scheme. Passing a null or empty value means prepend defaultScheme
     *                 to all links.
     *  @param matchFilter  The filter that is used to allow the client code additional control
     *                      over which pattern matches are to be converted into links.
     *  @param transformFilter Filter to allow the client code to update the link found.
     */
    public static void addLinks(@NonNull TextView text, @NonNull Pattern pattern,
            @Nullable String defaultScheme, @Nullable String[] schemes,
            @Nullable MatchFilter matchFilter, @Nullable TransformFilter transformFilter) {
        if (shouldAddLinksFallbackToFramework()) {
            Api24Impl.addLinks(text, pattern, defaultScheme, schemes, matchFilter, transformFilter);
            return;
        }
        SpannableString spannable = SpannableString.valueOf(text.getText());

        boolean linksAdded = addLinks(spannable, pattern, defaultScheme, schemes, matchFilter,
                transformFilter);
        if (linksAdded) {
            text.setText(spannable);
            addLinkMovementMethod(text);
        }
    }

    /**
     *  Applies a regex to a Spannable turning the matches into
     *  links.
     *
     *  @param text         Spannable whose text is to be marked-up with links
     *  @param pattern      Regex pattern to be used for finding links
     *  @param scheme       URL scheme string (eg <code>http://</code>) to be
     *                      prepended to the links that do not start with this scheme.
     */
    public static boolean addLinks(@NonNull Spannable text, @NonNull Pattern pattern,
            @Nullable String scheme) {
        if (shouldAddLinksFallbackToFramework()) {
            return Linkify.addLinks(text, pattern, scheme);
        }
        return addLinks(text, pattern, scheme, null, null, null);
    }

    /**
     * Applies a regex to a Spannable turning the matches into
     * links.
     *
     * @param spannable    Spannable whose text is to be marked-up with links
     * @param pattern      Regex pattern to be used for finding links
     * @param scheme       URL scheme string (eg <code>http://</code>) to be
     *                     prepended to the links that do not start with this scheme.
     * @param matchFilter  The filter that is used to allow the client code
     *                     additional control over which pattern matches are
     *                     to be converted into links.
     * @param transformFilter Filter to allow the client code to update the link found.
     *
     * @return True if at least one link is found and applied.
     */
    public static boolean addLinks(@NonNull Spannable spannable, @NonNull Pattern pattern,
            @Nullable String scheme, @Nullable MatchFilter matchFilter,
            @Nullable TransformFilter transformFilter) {
        if (shouldAddLinksFallbackToFramework()) {
            return Linkify.addLinks(spannable, pattern, scheme, matchFilter, transformFilter);
        }
        return addLinks(spannable, pattern, scheme, null, matchFilter,
                transformFilter);
    }

    /**
     * Applies a regex to a Spannable turning the matches into links.
     *
     * @param spannable Spannable whose text is to be marked-up with links.
     * @param pattern Regex pattern to be used for finding links.
     * @param defaultScheme The default scheme to be prepended to links if the link does not
     *                      start with one of the <code>schemes</code> given.
     * @param schemes Array of schemes (eg <code>http://</code>) to check if the link found
     *                contains a scheme. Passing a null or empty value means prepend defaultScheme
     *                to all links.
     * @param matchFilter  The filter that is used to allow the client code additional control
     *                     over which pattern matches are to be converted into links.
     * @param transformFilter Filter to allow the client code to update the link found.
     *
     * @return True if at least one link is found and applied.
     */
    public static boolean addLinks(@NonNull Spannable spannable, @NonNull Pattern pattern,
            @Nullable  String defaultScheme, @Nullable String[] schemes,
            @Nullable MatchFilter matchFilter, @Nullable TransformFilter transformFilter) {
        if (shouldAddLinksFallbackToFramework()) {
            return Api24Impl.addLinks(spannable, pattern, defaultScheme, schemes, matchFilter,
                    transformFilter);
        }
        final String[] schemesCopy;
        if (defaultScheme == null) defaultScheme = "";
        if (schemes == null || schemes.length < 1) {
            schemes = EMPTY_STRING;
        }

        schemesCopy = new String[schemes.length + 1];
        schemesCopy[0] = defaultScheme.toLowerCase(Locale.ROOT);
        for (int index = 0; index < schemes.length; index++) {
            String scheme = schemes[index];
            schemesCopy[index + 1] = (scheme == null) ? "" : scheme.toLowerCase(Locale.ROOT);
        }

        boolean hasMatches = false;
        Matcher m = pattern.matcher(spannable);

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String match = m.group(0);
            boolean allowed = true;

            if (matchFilter != null) {
                allowed = matchFilter.acceptMatch(spannable, start, end);
            }

            if (allowed && match != null) {
                String url = makeUrl(match, schemesCopy, m, transformFilter);

                applyLink(url, start, end, spannable);
                hasMatches = true;
            }
        }

        return hasMatches;
    }

    private static boolean shouldAddLinksFallbackToFramework() {
        return Build.VERSION.SDK_INT >= 28;
    }

    private static void addLinkMovementMethod(@NonNull TextView t) {
        MovementMethod m = t.getMovementMethod();

        if (!(m instanceof LinkMovementMethod)) {
            if (t.getLinksClickable()) {
                t.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static String makeUrl(@NonNull String url, @NonNull String[] prefixes,
            Matcher matcher, @Nullable TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(matcher, url);
        }

        boolean hasPrefix = false;

        for (String prefix : prefixes) {
            if (url.regionMatches(true, 0, prefix, 0, prefix.length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefix, 0, prefix.length())) {
                    url = prefix + url.substring(prefix.length());
                }

                break;
            }
        }

        if (!hasPrefix && prefixes.length > 0) {
            url = prefixes[0] + url;
        }

        return url;
    }

    @SuppressWarnings("SameParameterValue")
    private static void gatherLinks(ArrayList<LinkSpec> links,
            Spannable s, Pattern pattern, String[] schemes,
            MatchFilter matchFilter, TransformFilter transformFilter) {
        Matcher m = pattern.matcher(s);

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String match = m.group(0);

            if ((matchFilter == null || matchFilter.acceptMatch(s, start, end)) && match != null) {
                LinkSpec spec = new LinkSpec();
                spec.url = makeUrl(match, schemes, m, transformFilter);
                spec.start = start;
                spec.end = end;

                links.add(spec);
            }
        }
    }

    private static void applyLink(String url, int start, int end, Spannable text) {
        URLSpan span = new URLSpan(url);

        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void gatherMapLinks(ArrayList<LinkSpec> links, Spannable s) {
        String string = s.toString();
        String address;
        int base = 0;

        try {
            while ((address = findAddress(string)) != null) {
                int start = string.indexOf(address);

                if (start < 0) {
                    break;
                }

                LinkSpec spec = new LinkSpec();
                int length = address.length();
                int end = start + length;

                spec.start = base + start;
                spec.end = base + end;
                string = string.substring(end);
                base += end;

                String encodedAddress;

                try {
                    encodedAddress = URLEncoder.encode(address,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    continue;
                }

                spec.url = "geo:0,0?q=" + encodedAddress;
                links.add(spec);
            }
        } catch (UnsupportedOperationException e) {
            // findAddress may fail with an unsupported exception on platforms without a WebView.
            // In this case, we will not append anything to the links variable: it would have died
            // in WebView.findAddress.
        }
    }

    @SuppressWarnings("deprecation")
    private static String findAddress(String addr) {
        if (Build.VERSION.SDK_INT >= 28) {
            return WebView.findAddress(addr);
        }
        return FindAddress.findAddress(addr);
    }

    private static void pruneOverlaps(ArrayList<LinkSpec> links, Spannable text) {
        // Append spans added by framework
        URLSpan[] urlSpans = text.getSpans(0, text.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            LinkSpec spec = new LinkSpec();
            spec.frameworkAddedSpan = urlSpan;
            spec.start = text.getSpanStart(urlSpan);
            spec.end = text.getSpanEnd(urlSpan);
            links.add(spec);
        }

        Collections.sort(links, COMPARATOR);

        int len = links.size();
        int i = 0;

        while (i < len - 1) {
            LinkSpec a = links.get(i);
            LinkSpec b = links.get(i + 1);
            int remove = -1;

            if ((a.start <= b.start) && (a.end > b.start)) {
                if (b.end <= a.end) {
                    remove = i + 1;
                } else if ((a.end - a.start) > (b.end - b.start)) {
                    remove = i + 1;
                } else if ((a.end - a.start) < (b.end - b.start)) {
                    remove = i;
                }

                if (remove != -1) {
                    URLSpan span = links.get(remove).frameworkAddedSpan;
                    if (span != null) {
                        text.removeSpan(span);
                    }
                    links.remove(remove);
                    len--;
                    continue;
                }

            }

            i++;
        }
    }

    /**
     * Do not create this static utility class.
     */
    private LinkifyCompat() {}

    private static class LinkSpec {
        URLSpan frameworkAddedSpan;
        String url;
        int start;
        int end;

        LinkSpec() {
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void addLinks(TextView text, Pattern pattern, String defaultScheme, String[] schemes,
                MatchFilter matchFilter, TransformFilter transformFilter) {
            Linkify.addLinks(text, pattern, defaultScheme, schemes, matchFilter, transformFilter);
        }

        @DoNotInline
        static boolean addLinks(Spannable spannable, Pattern pattern, String defaultScheme,
                String[] schemes, MatchFilter matchFilter, TransformFilter transformFilter) {
            return Linkify.addLinks(spannable, pattern, defaultScheme, schemes, matchFilter,
                    transformFilter);
        }
    }
}
