/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.webkit;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibility versions of methods in {@link android.webkit.URLUtil}.
 *
 * @see android.webkit.URLUtil
 */
public final class URLUtilCompat {

    private URLUtilCompat() {} // Class should not be instantiated

    /**
     * Guesses canonical filename that a download would have, using the URL and contentDisposition.
     * <p>
     * This method differs from
     * {@link android.webkit.URLUtil#guessFileName(String, String, String)} in the following
     * ways:
     * <ul>
     *  <li>This method uses an updated parsing of {@code contentDisposition}, making this
     *  available on older Android versions. See {@link #getFilenameFromContentDisposition(String)}.
     *  <li>If the {@code contentDisposition} parameter is valid and contains a filename, that
     *  filename will be returned. The {@code url} and {@code mimeType} parameters will not be
     *  used in this case.
     *  <li>Otherwise the filename will be deduced from the path of the {@code url} and the
     *  {@code mimeType}. If a {@code mimeType} is provided and the canonical extension for that
     *  filetype does not match the filename derived from the {@code url}, then the canonical
     *  extension will be appended.
     *  <li>If the {@code mimeType} is {@code null}, {@code "application/octet-stream"}, or not
     *  known by the system, no extension will be appended to the filename.
     * </ul>
     *
     * @param url                Url to the content. Must not be {@code null}
     * @param contentDisposition Content-Disposition HTTP header or {@code null}
     * @param mimeType           Mime-type of the content or {@code null}
     * @return suggested filename
     * @see android.webkit.URLUtil#guessFileName(String, String, String)
     * @see #getFilenameFromContentDisposition(String)
     */
    public static @NonNull String guessFileName(@NonNull String url,
            @Nullable String contentDisposition, @Nullable String mimeType) {

        // First attempt to parse the Content-Disposition header if available.
        if (contentDisposition != null) {
            String filename = getFilenameFromContentDisposition(contentDisposition);
            if (filename != null) {
                return replacePathSeparators(filename);
            }
        }

        // Fallback filename.
        String filename = "downloadfile";

        Uri parsedUri = Uri.parse(url);
        if (parsedUri != null) {
            String lastPathSegment = parsedUri.getLastPathSegment();
            if (lastPathSegment != null) {
                filename = replacePathSeparators(lastPathSegment);
            }
        }

        // Ensure that filenames guessed from the URL path has an extension matching the mimeType.
        if (filename.indexOf('.') < 0 || extensionDifferentFromMimeType(filename, mimeType)) {
            String extensionFromMimeType = suggestExtensionFromMimeType(mimeType);
            return filename + extensionFromMimeType;
        }
        return filename;
    }

    /**
     * Replace all instances of {@code "/"} with {@code "_"} to avoid filenames that navigate the
     * path.
     */
    private static @NonNull String replacePathSeparators(@NonNull String raw) {
        return raw.replaceAll("/", "_");
    }


    /**
     * Get a candidate file extension (including the @{code .}) for the given mimeType.
     * Will return empty string for {@code null}, {@code "application/octet-stream"}, and any
     * unknown mime types.
     *
     * @param mimeType Reported mimetype
     * @return A file extension, including the {@code .}, or empty string
     */
    private static @NonNull String suggestExtensionFromMimeType(@Nullable String mimeType) {
        if (mimeType == null) {
            return "";
        }
        mimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        if (mimeType.equals("application/octet-stream")) {
            // Octet-stream is a generic extension that really doesn't map to anything, despite the
            // MimeTypeMap insisting that it maps to ".bin".
            // Handle this as a special case and return an empty guess.
            return "";
        }
        String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                mimeType);
        if (extensionFromMimeType != null) {
            return "." + extensionFromMimeType;
        }
        return "";
    }

    /**
     * Check if the {@code filename} has an extension that is different from the expected one based
     * on the {@code mimeType}.
     */
    private static boolean extensionDifferentFromMimeType(@NonNull String filename,
            @Nullable String mimeType) {
        if (mimeType == null) {
            return false;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                filename.substring(lastDotIndex + 1));
        return typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType);
    }

    /**
     * Pattern for parsing individual content disposition key-value pairs.
     * <p>
     * The pattern will attempt to parse the value as either single- double- or unquoted.
     * For the single- and double-quoted options, the pattern allows escaped quotes as part of
     * the value, as per
     * <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-2.2">RFC 2616 section 2.2</a>
     *
     * @noinspection RegExpRepeatedSpace Spaces are ignored by parser, there for readability.
     */
    private static final Pattern DISPOSITION_PATTERN = Pattern.compile(
            "\\s*"
                    + "(\\S+?) # Group 1: parameter name\n"
                    + "\\s*=\\s* # Match equals sign\n"
                    + "(?: # non-capturing group of options\n"
                    + "   '( (?: [^'\\\\] | \\\\. )* )' # Group 2: single-quoted\n"
                    + " | \"( (?: [^\"\\\\] | \\\\. )*  )\" # Group 3: double-quoted\n"
                    + " | ( [^'\"][^;\\s]* ) # Group 4: un-quoted parameter\n"
                    + ")\\s*;? # Optional end semicolon",
            Pattern.COMMENTS);

    /**
     * Extract filename from a  {@code Content-Disposition} header value.
     * <p>
     * This method implements the parsing defined in
     * <a href="https://datatracker.ietf.org/doc/html/rfc6266">RFC 6266</a>,
     * supporting both the {@code filename} and {@code filename*} disposition parameters.
     * If the passed header value has the {@code "inline"} disposition type, this method will
     * return {@code null} to indicate that a download was not intended.
     * <p>
     * If both {@code filename*} and {@code filename} is present, the former will be returned, as
     * per the RFC. Invalid encoded values will be ignored.
     *
     * @param contentDisposition Value of {@code Content-Disposition} header.
     * @return The filename suggested by the header or {@code null} if no filename could be
     * parsed from the header value.
     */
    public static @Nullable String getFilenameFromContentDisposition(
            @NonNull String contentDisposition) {
        String[] parts = contentDisposition.trim().split(";", 2);
        if (parts.length < 2) {
            // Need at least 2 parts, the `disposition-type` and at least one `disposition-parm`.
            return null;
        }
        String dispositionType = parts[0].trim();
        if ("inline".equalsIgnoreCase(dispositionType)) {
            // "inline" should not result in a download.
            // Unknown disposition types should be handles as "attachment"
            // https://datatracker.ietf.org/doc/html/rfc6266#section-4.2
            return null;
        }
        String dispositionParameters = parts[1];
        Matcher matcher = DISPOSITION_PATTERN.matcher(dispositionParameters);
        String filename = null;
        String filenameExt = null;
        while (matcher.find()) {
            String parameter = matcher.group(1);
            String value;
            if (matcher.group(2) != null) {
                value = removeSlashEscapes(matcher.group(2)); // Value was single-quoted
            } else if (matcher.group(3) != null) {
                value = removeSlashEscapes(matcher.group(3)); // Value was double-quoted
            } else {
                value = matcher.group(4); // Value was un-quoted
            }

            if (parameter == null || value == null) {
                continue;
            }

            if ("filename*".equalsIgnoreCase(parameter)) {
                filenameExt = parseExtValueString(value);
            } else if ("filename".equalsIgnoreCase(parameter)) {
                filename = value;
            }
        }

        // RFC 6266 dictates the filenameExt should be preferred if present.
        if (filenameExt != null) {
            return filenameExt;
        }
        return filename;
    }

    /**
     * Replace escapes of the \X form with X.
     */
    private static String removeSlashEscapes(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("\\\\(.)", "$1");
    }

    /**
     * Parse an extended value string which can be percent-encoded. Return {@code} null if unable
     * to parse the string.
     */
    private static String parseExtValueString(String raw) {
        String[] parts = raw.split("'", 3);
        if (parts.length < 3) {
            return null;
        }

        String encoding = parts[0];
        // Intentionally ignore parts[1] (language).
        String valueChars = parts[2];

        try {
            // The URLDecoder force-decodes + as " "
            // so preemptively replace all values with the encoded value to preserve them.
            String valueWithEncodedPlus = encodePlusCharacters(valueChars, encoding);
            // Use the decode(String, String) version since the Charset version is not available
            // at the current language level for the library.
            return URLDecoder.decode(valueWithEncodedPlus, encoding);
        } catch (RuntimeException | UnsupportedEncodingException ignored) {
            return null; // Ignoring an un-parsable value is within spec.
        }
    }


    /**
     * Replace all instances of {@code "+"} with the percent-encoded equivalent for the given
     * {@code encoding}.
     */
    private static @NonNull String encodePlusCharacters(@NonNull String valueChars,
            @NonNull String encoding) {
        Charset charset = Charset.forName(encoding);
        StringBuilder sb = new StringBuilder();
        for (byte b : charset.encode("+").array()) {
            sb.append(String.format("%02x", b));
        }
        return valueChars.replaceAll("\\+", sb.toString());
    }
}
