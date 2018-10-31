package androidx.ui.foundation.diagnostics

import androidx.ui.describeEnum

/**
 * An property than displays enum values tersely.
 *
 * The enum value is displayed with the class name stripped. For example:
 * [HitTestBehavior.deferToChild] is shown as `deferToChild`.
 *
 * See also:
 *
 *  * [DiagnosticsProperty] which documents named parameters common to all
 *    [DiagnosticsProperty]
 */
class EnumProperty<T : Enum<*>>(
    name: String,
    value: T?,
    defaultValue: Any? = kNoDefaultValue,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<T>(
        name = name,
        value = value,
        defaultValue = defaultValue,
        level = level
) {
    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (getValue() == null)
            return getValue().toString()
        return describeEnum(getValue()!!)
    }
}