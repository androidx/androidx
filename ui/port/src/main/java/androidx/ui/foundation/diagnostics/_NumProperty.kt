package androidx.ui.foundation.diagnostics

abstract class _NumProperty<T : Number>(
    name: String,
    value: T?,
    computeValue: ComputePropertyValueCallback<T>? = null,
    ifNull: String,
        // / Optional unit the [value] is measured in.
        // /
        // / Unit must be acceptable to display immediately after a number with no
        // / spaces. For example: 'physical pixels per logical pixel' should be a
        // / [tooltip] not a [unit].
    val unit: String?,
    showName: Boolean = true,
    defaultValue: Any = kNoDefaultValue,
    tooltip: String? = null,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<T>(
        name,
        value,
        computeValue = computeValue,
        ifNull = ifNull,
        showName = showName,
        defaultValue = defaultValue,
        tooltip = tooltip,
        level = level
 ) {

    override fun toJsonMap(): Map<String, Any> {
        var json = super.toJsonMap().toMutableMap()
        if (unit != null)
            json["unit"] = unit

        json["numberToString"] = numberToString()
        return json
    }

    // / String describing just the numeric [value] without a unit suffix.
    abstract fun numberToString(): String

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (getValue() == null)
            return getValue().toString()

        return if (unit != null) "${numberToString()}$unit" else numberToString()
    }
}