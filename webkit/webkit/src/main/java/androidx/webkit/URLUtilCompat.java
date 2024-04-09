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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
@SuppressWarnings("AcronymName") // Compat class for similarly named URLUtil in Android SDK
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
     *  <li>If the filename guessed from {@code url} or {@code contentDisposition} already
     *  contains an extension, but this extension differs from the one expected from the
     *  {@code mimeType}, then this method will append the expected extension instead of
     *  replacing the one already present. This is done to preserve filenames that contain a
     *  {@code "."} as part of a filename but where the last part is not meant as an  extension.
     *  <li>If the filename guessed from {@code contentDisposition} contains a {@code "/"}
     *  character, it will be replaced with {@code "_"}, unlike
     *  {@link android.webkit.URLUtil#guessFileName(String, String, String)} which will only
     *  return the part after the last {@code "/" character}.
     * </ul>
     * <p>
     * This method will use {@link #getFilenameFromContentDisposition(String)} to parse the
     * passed {@code contentDisposition}.
     * <ul>
     * <li>If not file extension is present in the guessed file name, one will be added based on
     * the
     * {@code mimetype} (this will be {@code ".bin"} if {@code mimeType} is {@code null}).
     * <li>If the guessed file name already contains an extension, but this extension doesn't
     * match a provided {@code mimeType}, then a new file extension will be added that matches
     * the {@code mimeType}.
     * </ul>
     *
     * @param url                Url to the content. Must not be {@code null}
     * @param contentDisposition Content-Disposition HTTP header or {@code null}
     * @param mimeType           Mime-type of the content or {@code null}
     * @return suggested filename
     * @see android.webkit.URLUtil#guessFileName(String, String, String)
     * @see #getFilenameFromContentDisposition(String)
     */
    @NonNull
    public static String guessFileName(@NonNull String url, @Nullable String contentDisposition,
            @Nullable String mimeType) {
        String filename = getFilenameSuggestion(url, contentDisposition);
        // Split filename between base and extension
        // Add an extension if filename does not have one
        String extensionFromMimeType = suggestExtensionFromMimeType(mimeType);

        if (filename.indexOf('.') < 0) {
            // Filename does not have an extension, use the suggested one.
            return filename + extensionFromMimeType;
        }

        // Filename already contains at least one dot.
        // Compare the last segment of the extension against the mime type.
        // If there's a mismatch, add the suggested extension instead.
        if (mimeType != null && extensionDifferentFromMimeType(filename, mimeType)) {
            return filename + extensionFromMimeType;
        }
        return filename;
    }

    /**
     * Get the suggested file name from the {@code contentDisposition} or {@code url}. Will
     * ensure that the filename contains no path separators by replacing them with the {@code "_"}
     * character.
     */
    @NonNull
    private static String getFilenameSuggestion(@NonNull String url,
            @Nullable String contentDisposition) {
        // First attempt to parse the Content-Disposition header if available
        if (contentDisposition != null) {
            String filename = getFilenameFromContentDisposition(contentDisposition);
            if (filename != null) {
                return replacePathSeparators(filename);
            }
        }

        // Try to generate a filename based on the URL.
        Uri parsedUri = Uri.parse(url);
        if (parsedUri != null) {
            String lastPathSegment = parsedUri.getLastPathSegment();
            if (lastPathSegment != null) {
                return replacePathSeparators(lastPathSegment);
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename.
        return "downloadfile";
    }

    /**
     * Replace all instances of {@code "/"} with {@code "_"} to avoid filenames that navigate the
     * path.
     */
    @NonNull
    private static String replacePathSeparators(@NonNull String raw) {
        return raw.replaceAll("/", "_");
    }


    /**
     * Check if the {@code filename} has an extension that is different from the expected one based
     * on the {@code mimeType}.
     */
    private static boolean extensionDifferentFromMimeType(@NonNull String filename,
            @NonNull String mimeType) {
        int lastDotIndex = filename.lastIndexOf('.');
        String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                filename.substring(lastDotIndex + 1));
        return typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType);
    }

    /**
     * Get a candidate file extension (including the @{code .}) for the given mimeType.
     * will return {@code ".bin"} if {@code mimeType} is {@code null}
     *
     * @param mimeType Reported mimetype
     * @return A file extension, including the {@code .}
     */
    @NonNull
    private static String suggestExtensionFromMimeType(@Nullable String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }
        String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                mimeType);
        if (extensionFromMimeType != null) {
            return "." + extensionFromMimeType;
        }
        if (mimeType.equalsIgnoreCase("text/html")) {
            return ".html";
        } else if (mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
            return ".txt";
        } else {
            return ".bin";
        }
    }

    /**
     * Pattern for parsing individual content disposition key-value pairs.
     * <p>
     * The pattern will attempt to parse the value as either single- double- or unquoted.
     * For the single- and double-quoted options, the pattern allows escaped quotes as part of
     * the value, as per
     * <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-2.2">RFC 2616 section 2.2</a>
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
    @Nullable
    public static String getFilenameFromContentDisposition(@NonNull String contentDisposition) {
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
    @NonNull
    private static String encodePlusCharacters(@NonNull String valueChars,
            @NonNull String encoding) {
        Charset charset = Charset.forName(encoding);
        StringBuilder sb = new StringBuilder();
        for (byte b : charset.encode("+").array()) {
            sb.append(String.format("%02x", b));
        }
        return valueChars.replaceAll("\\+", sb.toString());
    }
}
