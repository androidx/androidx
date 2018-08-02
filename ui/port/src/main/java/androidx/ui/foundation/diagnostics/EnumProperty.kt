package androidx.ui.foundation.diagnostics

/// An property than displays enum values tersely.
///
/// The enum value is displayed with the class name stripped. For example:
/// [HitTestBehavior.deferToChild] is shown as `deferToChild`.
///
/// See also:
///
///  * [DiagnosticsProperty] which documents named parameters common to all
///    [DiagnosticsProperty]
class EnumProperty<T: Any>(
        name: String,
        value: T,
        defaultValue: Any = kNoDefaultValue,
        level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<T>(
        name = name,
        value = value,
        defaultValue = defaultValue,
        level = level
) {

    override fun toString(): String {
        return getValue().toString()
    }

    override fun toStringParametrized(
            parentConfiguration: TextTreeConfiguration?,
            minLevel: DiagnosticLevel
    ): String {
        return toString()
    }
}