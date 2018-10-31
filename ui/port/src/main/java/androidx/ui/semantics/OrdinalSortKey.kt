package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DoubleProperty

/**
 * A [SemanticsSortKey] that sorts simply based on the `Double` value it is
 * given.
 *
 * The [OrdinalSortKey] compares itself with other [OrdinalSortKey]s
 * to sort based on the order it is given.
 *
 * The ordinal value `order` is typically a whole number, though it can be
 * fractional, e.g. in order to fit between two other consecutive whole
 * numbers. The value must be finite (it cannot be [Double.nan],
 * [Double.infinity], or [Double.negativeInfinity]).
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
    val order: Double,
    name: String? = null
) : SemanticsSortKey(name) {

    init {
        assert(order != null)
        assert(order != Double.NaN)
        assert(order > Double.NEGATIVE_INFINITY)
        assert(order < Double.POSITIVE_INFINITY)
    }

    override fun doCompare(other: SemanticsSortKey): Int {
        other as OrdinalSortKey // guaranteed by super.compareTo
        if (other.order == null || order == null || other.order == order)
            return 0
        return order.compareTo(other.order)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DoubleProperty.create("order", order, defaultValue = null))
    }
}