package androidx.ui.rendering.box

// TODO(Migration/xbhatnag): const constructor
data class _IntrinsicDimensionsCacheEntry(
    val dimension: _IntrinsicDimension,
    val argument: Double
) {
//    override fun equals(other: Any?): Boolean {
//        if (other !is _IntrinsicDimensionsCacheEntry)
//            return false
//        return dimension == other.dimension && argument == other.argument
//    }
//    @override
//    int get hashCode => hashValues(dimension, argument);
}
