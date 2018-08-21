package androidx.ui.foundation.diagnostics

import androidx.ui.clamp
import androidx.ui.toStringAsFixed

// / Property which clamps a [double] to between 0 and 1 and formats it as a
// / percentage.
// /
// / Ctor comment:
// / Create a diagnostics property for doubles that represent percentages or
// / fractions.
// /
// / Setting [showName] to false is often reasonable for [PercentProperty]
// / objects, as the fact that the property is shown as a percentage tends to
// / be sufficient to disambiguate its meaning.
// /
// / The [showName] and [level] arguments must not be null.
class PercentProperty(
    name: String,
    fraction: Double?,
    ifNull: String? = null,
    unit: String? = null,
    showName: Boolean = true,
    tooltip: String? = null,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DoubleProperty(
        name = name,
        value = fraction,
        ifNull = ifNull,
        showName = showName,
        tooltip = tooltip,
        unit = unit,
        level = level
) {

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (getValue() == null)
            return getValue().toString()
        return if (unit != null) "${numberToString()} $unit" else numberToString()
    }

    override fun numberToString(): String {
        if (getValue() == null)
            return getValue().toString()
        return "${(getValue()!!.clamp(0.0, 1.0) * 100.0).toStringAsFixed(1)}%"
    }
}