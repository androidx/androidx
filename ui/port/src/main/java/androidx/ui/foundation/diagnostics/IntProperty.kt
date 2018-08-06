package androidx.ui.foundation.diagnostics

// / An int valued property with an optional unit the value is measured in.
// /
// / Examples of units include 'px' and 'ms'.
// /
// / Ctor comment:
// / Create a diagnostics property for integers.
// /
// / The [showName] and [level] arguments must not be null.
class IntProperty(
    name: String,
    value: Int,
    ifNull: String,
    unit: String?,
    showName: Boolean = true,
    defaultValue: Any = kNoDefaultValue,
    level: DiagnosticLevel = DiagnosticLevel.info
) : _NumProperty<Int>(
        name = name,
        value = value,
        ifNull = ifNull,
        showName = showName,
        unit = unit,
        defaultValue = defaultValue,
        level = level
) {

    override fun numberToString() = getValue().toString()
}