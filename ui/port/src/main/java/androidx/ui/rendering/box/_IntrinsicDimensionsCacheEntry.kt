package androidx.ui.rendering.box

// TODO(Migration/xbhatnag): @immutable and const constructor
data class _IntrinsicDimensionsCacheEntry(
    val dimension: _IntrinsicDimension,
    val argument: Double
) {
    override fun equals(other: Any?): Boolean {
        if (other !is _IntrinsicDimensionsCacheEntry)
            return false
        return dimension == other.dimension && argument == other.argument
    }

// TODO(Migration/xbhatnag): How do we combine hash code values?
//    @override
//    int get hashCode => hashValues(dimension, argument);
}
