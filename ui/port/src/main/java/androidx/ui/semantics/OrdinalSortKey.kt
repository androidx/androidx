package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.FloatProperty

/**
 * A [SemanticsSortKey] that sorts simply based on the `Float` value it is
 * given.
 *
 * The [OrdinalSortKey] compares itself with other [OrdinalSortKey]s
 * to sort based on the order it is given.
 *
 * The ordinal value `order` is typically a whole number, though it can be
 * fractional, e.g. in order to fit between two other consecutive whole
 * numbers. The value must be finite (it cannot be [Float.NaN],
 * [Float.POSITIVE_INFINITY], or [Float.negativeInfinity]).
 *
 * See also:
 *
 *  * [SemanticsSortOrder] which manages a list of sort keys.
 */
open class OrdinalSortKey(
    /**
     * Determines the placement of this key in a sequence of keys that defines
     * the order in which this node is traversed by the platform's accessibility
     * services.
     *
     * Lower values will be traversed first.
     */
    val order: Float,
    name: String? = null
) : SemanticsSortKey(name) {

    init {
        assert(order != null)
        assert(order != Float.NaN)
        assert(order > Float.NEGATIVE_INFINITY)
        assert(order < Float.POSITIVE_INFINITY)
    }

    override fun doCompare(other: SemanticsSortKey): Int {
        other as OrdinalSortKey // guaranteed by super.compareTo
        if (other.order == null || order == null || other.order == order)
            return 0
        return order.compareTo(other.order)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(FloatProperty.create("order", order, defaultValue = null))
    }
}