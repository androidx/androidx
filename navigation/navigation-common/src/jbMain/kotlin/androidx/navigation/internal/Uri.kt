/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.internal

/**
 * Immutable URI reference. A URI reference includes a URI and a fragment, the
 * component of the URI following a '#'. Builds and parses URI references
 * which conform to
 * <a href="http://www.faqs.org/rfcs/rfc2396.html">RFC 2396</a>.
 *
 * Partial KMP adoption of android.net.Url
 */
internal class Uri {
    companion object {

        /**
         * Encodes characters in the given string as '%'-escaped octets
         * using the UTF-8 scheme. Leaves letters ("A-Z", "a-z"), numbers
         * ("0-9"), and unreserved characters ("_-!.~'()*") intact. Encodes
         * all other characters.
         *
         * @param s string to encode
         * @param allow set of additional characters to allow in the encoded form,
         *  null if no characters should be skipped
         * @return an encoded version of s suitable for use as a URI component
         */
        fun encode(s: String, allow: String? = null): String {

            // Lazily-initialized buffers.
            var encoded: StringBuilder? = null

            val oldLength = s.length

            // This loop alternates between copying over allowed characters and
            // encoding in chunks. This results in fewer method calls and
            // allocations than encoding one character at a time.
            var current = 0
            while (current < oldLength) {
                // Start in "copying" mode where we copy over allowed chars.

                // Find the next character which needs to be encoded.

                var nextToEncode = current
                while (nextToEncode < oldLength
                    && isAllowed(s[nextToEncode], allow)
                ) {
                    nextToEncode++
                }

                // If there's nothing more to encode...
                if (nextToEncode == oldLength) {
                    if (current == 0) {
                        // We didn't need to encode anything!
                        return s
                    } else {
                        // Presumably, we've already done some encoding.
                        encoded!!.append(s, current, oldLength)
                        return encoded.toString()
                    }
                }

                if (encoded == null) {
                    encoded = StringBuilder()
                }

                if (nextToEncode > current) {
                    // Append allowed characters leading up to this point.
                    encoded.append(s, current, nextToEncode)
                } else {
                    // assert nextToEncode == current
                }

                // Switch to "encoding" mode.

                // Find the next allowed character.
                current = nextToEncode
                var nextAllowed = current + 1
                while (nextAllowed < oldLength
                    && !isAllowed(s[nextAllowed], allow)
                ) {
                    nextAllowed++
                }

                // Convert the substring to bytes and encode the bytes as
                // '%'-escaped octets.
                val bytes: ByteArray = s.encodeToByteArray(current, nextAllowed)
                val bytesLength = bytes.size
                for (i in 0 until bytesLength) {
                    encoded.append('%')
                    encoded.append(HEX_DIGITS[(bytes[i].toInt() and 0xf0) shr 4])
                    encoded.append(HEX_DIGITS[bytes[i].toInt() and 0xf])
                }

                current = nextAllowed
            }

            // Encoded could still be null at this point if s is empty.
            return encoded?.toString() ?: s
        }

        /**
         * Decodes '%'-escaped octets in the given string using the UTF-8 scheme.
         * Replaces invalid octets with the unicode replacement character
         * ("\\uFFFD").
         *
         * @param s encoded string to decode
         * @return the given string with escaped octets decoded, or null if
         *  s is null
         */
        fun decode(s: String): String =
            UriCodec.decode(s, convertPlus = false, throwOnFailure = false)

        /**
         * Returns true if the given character is allowed.
         *
         * @param c character to check
         * @param allow characters to allow
         * @return true if the character is allowed or false if it should be
         * encoded
         */
        private fun isAllowed(c: Char, allow: String?): Boolean {
            return (c in 'A'..'Z')
                || (c in 'a'..'z')
                || (c in '0'..'9')
                || "_-!.~'()*".indexOf(c) != -1
                || (allow != null && allow.indexOf(c) != -1)
        }

        private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()
    }
}
