package androidx.ui.engine.text

// / A visual position in a string of text.
// / Creates an object representing a particular position in a string.
// /
// / The arguments must not be null (so the [offset] argument is required).
// /
// / The index of the character that immediately follows the position.
// /
// / For example, given the string `'Hello'`, offset 0 represents the cursor
// / being before the `H`, while offset 5 represents the cursor being just
// / after the `o`.
// /
// / If the offset has more than one visual location (e.g., occurs at a line
// / break), which of the two locations is represented by this position.
// /
// / For example, if the text `'AB'` had a forced line break between the `A`
// / and the `B`, then the downstream affinity at offset 1 represents the
// / cursor being just after the `A` on the first line, while the upstream
// / affinity at offset 1 represents the cursor being just before the `B` on
// / the first line.
data class TextPosition(val offset: Int, val affinity: TextAffinity) {
    override fun toString(): String {
        return "TextPosition(offset: $offset, affinity: $affinity)"
    }
}

// TODO(Migration/siyamed): removed the following since converted into data class
//    @override
//    int get hashCode => hashValues(offset, affinity);

//    @override
//    bool operator ==(dynamic other) {
//        if (other.runtimeType != runtimeType)
//            return false;
//        final TextPosition typedOther = other;
//        return typedOther.offset == offset
//                && typedOther.affinity == affinity;
//    }