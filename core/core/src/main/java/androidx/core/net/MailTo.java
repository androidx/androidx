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

package androidx.core.net;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MailTo URI parser. Replacement for {@link android.net.MailTo}.
 *
 * <p>This class parses a mailto scheme URI and then can be queried for the parsed parameters.
 * This implements RFC 6068.</p>
 *
 * <p><em>Note: scheme name matching is case-sensitive, unlike the formal RFC. As a result,
 * you should always ensure that you write your URI with the scheme using lower case letters,
 * and normalize any URIs you receive from outside of Android to ensure the scheme is lower case.
 * </em></p>
 */
public final class MailTo {
    public static final String MAILTO_SCHEME = "mailto:";
    private static final String MAILTO = "mailto";

    // Well known headers
    private static final String TO = "to";
    private static final String BODY = "body";
    private static final String CC = "cc";
    private static final String BCC = "bcc";
    private static final String SUBJECT = "subject";

    // All the parsed content is added to the headers.
    private HashMap<String, String> mHeaders;

    /**
     * Private constructor. The only way to build a Mailto object is through
     * the parse() method.
     */
    private MailTo() {
        mHeaders = new HashMap<>();
    }

    /**
     * Test to see if the given string is a mailto URI
     *
     * <p><em>Note: scheme name matching is case-sensitive, unlike the formal RFC. As a result,
     * you should always ensure that you write your URI string with the scheme using lower case
     * letters, and normalize any URIs you receive from outside of Android to ensure the scheme is
     * lower case.</em></p>
     *
     * @param uri string to be tested
     * @return true if the string is a mailto URI
     */
    public static boolean isMailTo(@Nullable String uri) {
        return uri != null && uri.startsWith(MAILTO_SCHEME);
    }

    /**
     * Test to see if the given Uri is a mailto URI
     *
     * <p><em>Note: scheme name matching is case-sensitive, unlike the formal RFC. As a result,
     * you should always ensure that you write your Uri with the scheme using lower case letters,
     * and normalize any Uris you receive from outside of Android to ensure the scheme is lower
     * case.</em></p>
     *
     * @param uri Uri to be tested
     * @return true if the Uri is a mailto URI
     */
    public static boolean isMailTo(@Nullable Uri uri) {
        return uri != null && MAILTO.equals(uri.getScheme());
    }

    /**
     * Parse and decode a mailto scheme string. This parser implements
     * RFC 6068. The returned object can be queried for the parsed parameters.
     *
     * <p><em>Note: scheme name matching is case-sensitive, unlike the formal RFC. As a result,
     * you should always ensure that you write your URI string with the scheme using lower case
     * letters, and normalize any URIs you receive from outside of Android to ensure the scheme is
     * lower case.</em></p>
     *
     * @param uri String containing a mailto URI
     * @return MailTo object
     * @exception ParseException if the scheme is not a mailto URI
     */
    @NonNull
    public static MailTo parse(@NonNull String uri) throws ParseException {
        Preconditions.checkNotNull(uri);

        if (!isMailTo(uri)) {
            throw new ParseException("Not a mailto scheme");
        }

        // Drop fragment if present
        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex != -1) {
            uri = uri.substring(0, fragmentIndex);
        }

        String address;
        String query;
        int queryIndex = uri.indexOf('?');
        if (queryIndex == -1) {
            address = Uri.decode(uri.substring(MAILTO_SCHEME.length()));
            query = null;
        } else {
            address = Uri.decode(uri.substring(MAILTO_SCHEME.length(), queryIndex));
            query = uri.substring(queryIndex + 1);
        }

        MailTo mailTo = new MailTo();

        // Parse out the query parameters
        if (query != null) {
            @SuppressWarnings("StringSplitter")
            String[] queries = query.split("&");
            for (String queryParameter : queries) {
                String[] nameValueArray = queryParameter.split("=", 2);
                if (nameValueArray.length == 0) {
                    continue;
                }

                // insert the headers with the name in lowercase so that
                // we can easily find common headers
                String queryParameterKey = Uri.decode(nameValueArray[0]).toLowerCase(Locale.ROOT);
                String queryParameterValue = nameValueArray.length > 1
                        ? Uri.decode(nameValueArray[1]) : null;

                mailTo.mHeaders.put(queryParameterKey, queryParameterValue);
            }
        }

        // Address can be specified in both the headers and just after the
        // mailto line. Join the two together.
        String toParameter = mailTo.getTo();
        if (toParameter != null) {
            address += ", " + toParameter;
        }
        mailTo.mHeaders.put(TO, address);

        return mailTo;
    }

    /**
     * Parse and decode a mailto scheme Uri. This parser implements
     * RFC 6068. The returned object can be queried for the parsed parameters.
     *
     * <p><em>Note: scheme name matching is case-sensitive, unlike the formal RFC. As a result,
     * you should always ensure that you write your Uri with the scheme using lower case letters,
     * and normalize any Uris you receive from outside of Android to ensure the scheme is lower
     * case.</em></p>
     *
     * @param uri Uri containing a mailto URI
     * @return MailTo object
     * @exception ParseException if the scheme is not a mailto URI
     */
    @NonNull
    public static MailTo parse(@NonNull Uri uri) throws ParseException {
        return parse(uri.toString());
    }

    /**
     * Retrieve the To address line from the parsed mailto URI. This could be
     * several email address that are comma-space delimited.
     * If no To line was specified, then null is return
     * @return comma delimited email addresses or null
     */
    @Nullable
    public String getTo() {
        return mHeaders.get(TO);
    }

    /**
     * Retrieve the CC address line from the parsed mailto URI. This could be
     * several email address that are comma-space delimited.
     * If no CC line was specified, then null is return
     * @return comma delimited email addresses or null
     */
    @Nullable
    public String getCc() {
        return mHeaders.get(CC);
    }

    /**
     * Retrieve the BCC address line from the parsed mailto URI. This could be
     * several email address that are comma-space delimited.
     * If no BCC line was specified, then null is return
     * @return comma delimited email addresses or null
     */
    @Nullable
    public String getBcc() {
        return mHeaders.get(BCC);
    }

    /**
     * Retrieve the subject line from the parsed mailto URI.
     * If no subject line was specified, then null is return
     * @return subject or null
     */
    @Nullable
    public String getSubject() {
        return mHeaders.get(SUBJECT);
    }

    /**
     * Retrieve the body line from the parsed mailto URI.
     * If no body line was specified, then null is return
     * @return body or null
     */
    @Nullable
    public String getBody() {
        return mHeaders.get(BODY);
    }

    /**
     * Retrieve all the parsed email headers from the mailto URI
     * @return map containing all parsed values
     */
    @Nullable
    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(MAILTO_SCHEME);
        sb.append('?');
        for (Map.Entry<String, String> header : mHeaders.entrySet()) {
            sb.append(Uri.encode(header.getKey()));
            sb.append('=');
            sb.append(Uri.encode(header.getValue()));
            sb.append('&');
        }
        return sb.toString();
    }
}
