package androidx.ui.foundation.diagnostics

import androidx.ui.toStringAsFixed

/// Property describing a [double] [value] with an optional [unit] of measurement.
///
/// Numeric formatting is optimized for debug message readability.
open class DoubleProperty protected constructor(
        name: String,
        value: Double? = null,
        computeValue: ComputePropertyValueCallback<Double>? = null,
        ifNull: String,
        unit: String?,
        showName: Boolean = true,
        defaultValue: Any = kNoDefaultValue,
        tooltip: String,
        level: DiagnosticLevel = DiagnosticLevel.info
): _NumProperty<Double>(
        name = name,
        value = value,
        computeValue = computeValue,
        ifNull = ifNull,
        unit = unit,
        tooltip = tooltip,
        defaultValue = defaultValue,
        showName = showName,
        level = level
)  {

    companion object {

        /// If specified, [unit] describes the unit for the [value] (e.g. px).
        ///
        /// The [showName] and [level] arguments must not be null.
        fun create(
                name: String,
                value: Double,
                ifNull: String,
                unit: String?,
                showName: Boolean = true,
                defaultValue: Any = kNoDefaultValue,
                tooltip: String,
                level: DiagnosticLevel = DiagnosticLevel.info
        ): DoubleProperty {
            return DoubleProperty(
                    name = name,
                    value = value,
                    ifNull = ifNull,
                    unit = unit,
                    showName = showName,
                    defaultValue = defaultValue,
                    tooltip = tooltip,
                    level = level
            )
        }

        /// Property with a [value] that is computed only when needed.
        ///
        /// Use if computing the property [value] may throw an exception or is
        /// expensive.
        ///
        /// The [showName] and [level] arguments must not be null.
        fun createLazy(
                name: String,
                computeValue: ComputePropertyValueCallback<Double>,
                ifNull: String,
                unit: String?,
                showName: Boolean = true,
                defaultValue: Any = kNoDefaultValue,
                tooltip: String,
                level: DiagnosticLevel = DiagnosticLevel.info
        ): DoubleProperty {
            return DoubleProperty(
                    name = name,
                    computeValue = computeValue,
                    ifNull = ifNull,
                    unit = unit,
                    showName = showName,
                    defaultValue = defaultValue,
                    tooltip = tooltip,
                    level = level
            )
        }
    }

    override fun numberToString() = getValue()?.toStringAsFixed(1).orEmpty();
}