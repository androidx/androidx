package androidx.ui.foundation.diagnostics

import java.util.regex.Pattern
import kotlin.coroutines.experimental.buildIterator

val _indentPattern = Pattern.compile("^ *(?:[-+*] |[0-9]+[.):] )?")

internal enum class _WordWrapParseMode {
    inSpace, inWord, atBreak
}

// / Wraps the given string at the given width.
// /
// / Wrapping occurs at space characters (U+0020). Lines that start with an
// / octothorpe ("#", U+0023) are not wrapped (so for example, Dart stack traces
// / won't be wrapped).
// /
// / Subsequent lines attempt to duplicate the indentation of the first line, for
// / example if the first line starts with multiple spaces. In addition, if a
// / `wrapIndent` argument is provided, each line after the first is prefixed by
// / that string.
// /
// / This is not suitable for use with arbitrary Unicode text. For example, it
// / doesn't implement UAX #14, can't handle ideographic text, doesn't hyphenate,
// / and so forth. It is only intended for formatting error messages.
// /
// / The default [debugPrint] implementation uses this for its line wrapping.
// TODO(Migration/Filip): Sync*?
fun debugWordWrap(message: String, width: Int, wrapIndent: String = ""): Iterable<String> {
    return Iterable {
        buildIterator {
            if (message.length < width || message.trimEnd()[0] == '#') {
                yield(message)
                return@buildIterator
            }

            // TODO(Filip): Changed mathching, not sure if it still works
            val matcher = _indentPattern.matcher(message)
            val prefixMatch = matcher.matches()
            val prefix = wrapIndent + " ".repeat(matcher.group(0).length)
            var start = 0
            var startForLengthCalculations = 0
            var addPrefix = false
            var index = prefix.length
            var mode = _WordWrapParseMode.inSpace
            var lastWordStart = 0
            var lastWordEnd: Int? = 0
            while (true) {
                when (mode) {
                    _WordWrapParseMode.inSpace -> { // at start of break point (or start of line); can't break until next break
                        while ((index < message.length) && (message[index] == ' '))
                            index += 1
                        lastWordStart = index
                        mode = _WordWrapParseMode.inWord
                    }
                    _WordWrapParseMode.inWord -> {
                        while ((index < message.length) && (message[index] != ' '))
                            index += 1
                        mode = _WordWrapParseMode.atBreak
                    }
                    _WordWrapParseMode.atBreak -> { // looking for a good break point
                        if ((index - startForLengthCalculations > width) || (index == message.length)) {
                            // we are over the width line, so break
                            if ((index - startForLengthCalculations <= width) || (lastWordEnd == null)) {
                                // we should use this point, before either it doesn't actually go over the end (last line), or it does, but there was no earlier break point
                                lastWordEnd = index
                            }
                            if (addPrefix) {
                                yield(prefix + message.substring(start, lastWordEnd))
                            } else {
                                yield(message.substring(start, lastWordEnd))
                                addPrefix = true
                            }
                            if (lastWordEnd >= message.length)
                                return@buildIterator
                            // just yielded a line
                            if (lastWordEnd == index) {
                                // we broke at current position
                                // eat all the spaces, then set our start point
                                while ((index < message.length) && (message[index] == ' '))
                                    index += 1
                                start = index
                                mode = _WordWrapParseMode.inWord
                            } else {
                                // we broke at the previous break point, and we're at the start of a new one
                                assert(lastWordStart > lastWordEnd)
                                start = lastWordStart
                                mode = _WordWrapParseMode.atBreak
                            }
                            startForLengthCalculations = start - prefix.length
                            assert(addPrefix)
                            lastWordEnd = null
                        } else {
                            // save this break point, we're not yet over the line width
                            lastWordEnd = index
                            // skip to the end of this break point
                            mode = _WordWrapParseMode.inSpace
                        }
                    }
                }
            }
        }
    }
}