package androidx.ui.engine.text

/** A linear decoration to draw near the text. */
data class TextDecoration internal constructor(val mask: Int) {

    companion object {
        val none: TextDecoration = TextDecoration(0x0)

        /** Draw a line underneath each line of text */
        val underline: TextDecoration = TextDecoration(0x1)

        // TODO(Migration/siyamed): We do not currently support this, either need custom span
        // implementation or we wont support it

        /** Draw a line above each line of text */
        val overline: TextDecoration = TextDecoration(0x2)

        /** Draw a line through each line of text */
        val lineThrough: TextDecoration = TextDecoration(0x4)

        /** Creates a decoration that paints the union of all the given decorations. */
        fun combine(decorations: List<TextDecoration>): TextDecoration {
            var mask = 0
            for (decoration in decorations) {
                mask = mask or decoration.mask
            }

            return TextDecoration(mask)
        }
    }

    /** Whether this decoration will paint at least as much decoration as the given decoration. */
    fun contains(other: TextDecoration): Boolean {
        return (mask or other.mask) == mask
    }

    override fun toString(): String {
        if (mask == 0) {
            return "TextDecoration.none"
        }

        var values: MutableList<String> = mutableListOf()
        if (!((mask and TextDecoration.underline.mask) == 0)) {
            values.add("underline")
        }
        if (!((mask and TextDecoration.overline.mask) == 0)) {
            values.add("overline")
        }
        if (!((mask and TextDecoration.lineThrough.mask) == 0)) {
            values.add("lineThrough")
        }
        if ((values.size == 1)) {
            return "TextDecoration.${values.get(0)}"
        }
        return "TextDecoration.combine([${values.joinToString(separator = ", ")}])"
    }

// TODO(Migration/siyamed): removed the following since converted into data class
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//        return mask == (other as TextDecoration).mask
//    }
//
//    override fun hashCode(): Int {
//        return mask.hashCode()
//    }
}