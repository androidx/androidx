package androidx.ui.foundation.diagnostics

// / Builder that builds a String with specified prefixes for the first and
// / subsequent lines.
// /
// / Allows for the incremental building of strings using `write*()` methods.
// / The strings are concatenated into a single string with the first line
// / prefixed by [prefixLineOne] and subsequent lines prefixed by
// / [prefixOtherLines].
class _PrefixedStringBuilder(
        // / Prefix to add to the first line.
    val prefixLineOne: String,
        // / Prefix to add to subsequent lines.
        // /
        // / The prefix can be modified while the string is being built in which case
        // / subsequent lines will be added with the modified prefix.
    var prefixOtherLines: String
) {

    private val _buffer = StringBuffer()
    private var _atLineStart = true

    // / Whether the string being built already has more than 1 line.
    var hasMultipleLines = false

    // / Write text ensuring the specified prefixes for the first and subsequent
    // / lines.
    fun write(inputString: String) {
        var s = inputString

        if (s.isEmpty())
            return

        if (s == "\n") {
            // Edge case to avoid adding trailing whitespace when the caller did
            // not explicitly add trailing whitespace.
            if (_buffer.isEmpty()) {
                _buffer.append(prefixLineOne.trimEnd())
            } else if (_atLineStart) {
                _buffer.append(prefixOtherLines.trimEnd())
                hasMultipleLines = true
            }
            _buffer.append("\n")
            _atLineStart = true
            return
        }

        if (_buffer.isEmpty()) {
            _buffer.append(prefixLineOne)
        } else if (_atLineStart) {
            _buffer.append(prefixOtherLines)
            hasMultipleLines = true
        }
        var lineTerminated = false

        if (s.endsWith("\n")) {
            s = s.substring(0, s.length - 1)
            lineTerminated = true
        }
        val parts = s.split('\n')
        _buffer.append(parts[0])
        for (i in 1 until parts.size) {
            _buffer.apply {
                append("\n")
                append(prefixOtherLines)
                append(parts[i])
            }
        }

        if (lineTerminated)
            _buffer.append("\n")

        _atLineStart = lineTerminated
    }

    // / Write text assuming the text already obeys the specified prefixes for the
    // / first and subsequent lines.
    fun writeRaw(text: String) {
        if (text.isEmpty())
            return
        _buffer.append(text)
        _atLineStart = text.endsWith("\n")
    }

    // / Write a line assuming the line obeys the specified prefixes. Ensures that
    // / a newline is added if one is not present.
    // / The same as [writeRaw] except a newline is added at the end of [line] if
    // / one is not already present.
    // /
    // / A new line is not added if the input string already contains a newline.
    fun writeRawLine(line: String) {
        if (line.isEmpty())
            return
        _buffer.append(line)
        if (!line.endsWith("\n"))
            _buffer.append("\n")
        _atLineStart = true
    }

    override fun toString() = _buffer.toString()
}