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

private class URISyntaxException(
    input: String, reason: String, index: Int
) : Exception(
    "$reason at index $index: $input"
)

/**
 * Decodes “application/x-www-form-urlencoded” content.
 *
 * Copy of android.net.UriCodec
 */
internal object UriCodec {
    /**
     * Interprets a char as hex digits, returning a number from -1 (invalid char) to 15 ('f').
     */
    private fun hexCharToValue(c: Char): Int {
        if (c in '0'..'9') {
            return c.code - '0'.code
        }
        if (c in 'a'..'f') {
            return 10 + c.code - 'a'.code
        }
        if (c in 'A'..'F') {
            return 10 + c.code - 'A'.code
        }
        return -1
    }

    private fun unexpectedCharacterException(
        uri: String, name: String?, unexpected: Char, index: Int
    ): URISyntaxException {
        val nameString = if ((name == null)) "" else " in [$name]"
        return URISyntaxException(
            uri, "Unexpected character$nameString: $unexpected", index
        )
    }

    @Throws(URISyntaxException::class)
    private fun getNextCharacter(uri: String, index: Int, end: Int, name: String?): Char {
        if (index >= end) {
            val nameString = if ((name == null)) "" else " in [$name]"
            throw URISyntaxException(
                uri, "Unexpected end of string$nameString", index
            )
        }
        return uri[index]
    }

    /**
     * Decode a string according to the rules of this decoder.
     *
     * - if `convertPlus == true` all ‘+’ chars in the decoded output are converted to ‘ ‘
     * (white space)
     * - if `throwOnFailure == true`, an [IllegalArgumentException] is thrown for
     * invalid inputs. Else, U+FFFd is emitted to the output in place of invalid input octets.
     */
    fun decode(
        s: String,
        convertPlus: Boolean = false,
        throwOnFailure: Boolean = false
    ): String {
        val builder = StringBuilder(s.length)
        appendDecoded(builder, s, convertPlus, throwOnFailure)
        return builder.toString()
    }

    /**
     * Character to be output when there's an error decoding an input.
     */
    private const val INVALID_INPUT_CHARACTER = '\ufffd'

    private fun appendDecoded(
        builder: StringBuilder,
        s: String,
        convertPlus: Boolean,
        throwOnFailure: Boolean
    ) {
        // Holds the bytes corresponding to the escaped chars being read (empty if the last char
        // wasn't a escaped char).
        val byteBuffer = ByteArray(s.length) { 0 }
        var byteBufferPosition = 0
        fun put(byte: Byte) {
            byteBuffer[byteBufferPosition++] = byte
        }
        fun flush() {
            if (byteBufferPosition == 0) {
                return
            }
            builder.append(
                byteBuffer.decodeToString(
                    endIndex = byteBufferPosition,
                    throwOnInvalidSequence = throwOnFailure
                )
            )
            byteBufferPosition = 0
        }

        var i = 0
        while (i < s.length) {
            var c = s[i]
            i++
            when (c) {
                '+' -> {
                    flush()
                    builder.append(if (convertPlus) ' ' else '+')
                }

                '%' -> {
                    // Expect two characters representing a number in hex.
                    var hexValue: Byte = 0
                    var j = 0
                    while (j < 2) {
                        try {
                            c = getNextCharacter(s, i, s.length, null /* name */)
                        } catch (e: URISyntaxException) {
                            // Unexpected end of input.
                            if (throwOnFailure) {
                                throw IllegalArgumentException(e)
                            } else {
                                flush()
                                builder.append(INVALID_INPUT_CHARACTER)
                                return
                            }
                        }
                        i++
                        val newDigit = hexCharToValue(c)
                        if (newDigit < 0) {
                            if (throwOnFailure) {
                                throw IllegalArgumentException(
                                    unexpectedCharacterException(s, null,  /* name */c, i - 1)
                                )
                            } else {
                                flush()
                                builder.append(INVALID_INPUT_CHARACTER)
                                break
                            }
                        }
                        hexValue = (hexValue * 0x10 + newDigit).toByte()
                        j++
                    }
                    put(hexValue)
                }

                else -> {
                    flush()
                    builder.append(c)
                }
            }
        }
        flush()
    }
}
