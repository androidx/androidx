package androidx.ui.foundation.diagnostics

/**
 * Property where the description is either [ifTrue] or [ifFalse] depending on
 * whether [value] is true or false.
 *
 * Using [FlagProperty] instead of [DiagnosticsProperty<bool>] can make
 * diagnostics display more polished. For example, given a property named
 * `visible` that is typically true, the following code will return 'hidden'
 * when `visible` is false and nothing when visible is true, in contrast to
 * `visible: true` or `visible: false`.
 *
 * ## Sample code
 *
 * ```dart
 * new FlagProperty(
 *   'visible',
 *   value: true,
 *   ifFalse: 'hidden',
 * )
 * ```
 *
 * [FlagProperty] should also be used instead of [DiagnosticsProperty<bool>]
 * if showing the bool value would not clearly indicate the meaning of the
 * property value.
 *
 * ```dart
 * new FlagProperty(
 *   'inherit',
 *   value: inherit,
 *   ifTrue: '<all styles inherited>',
 *   ifFalse: '<no style specified>',
 * )
 * ```
 *
 * See also:
 *
 *  * [ObjectFlagProperty], which provides similar behavior describing whether
 *    a [value] is null.
 *
 *
 * Ctor comment:
 * Constructs a FlagProperty with the given descriptions with the specified descriptions.
 *
 * [showName] defaults to false as typically [ifTrue] and [ifFalse] should
 * be descriptions that make the property name redundant.
 *
 * The [showName] and [level] arguments must not be null.
 */
class FlagProperty(
    name: String,
    value: Boolean,
    /**
     * Description to use if the property [value] is true.
     *
     * If not specified and [value] equals true the property's priority [level]
     * will be [DiagnosticLevel.hidden].
     */
    val ifTrue: String? = null,
    /**
     * Description to use if the property value is false.
     *
     * If not specified and [value] equals false, the property's priority [level]
     * will be [DiagnosticLevel.hidden].
     */
    val ifFalse: String? = null,
    showName: Boolean = false,
    defaultValue: Any = kNoDefaultValue,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<Boolean>(
        name = name,
        value = value,
        showName = showName,
        defaultValue = defaultValue,
        level = level
) {
    init {
        assert(ifTrue != null || ifFalse != null)
    }

    override fun toJsonMap(): Map<String, Any> {
        val json = super.toJsonMap().toMutableMap()
        if (ifTrue != null)
            json["ifTrue"] = ifTrue
        if (ifFalse != null)
            json["ifFalse"] = ifFalse

        return json
    }

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (getValue() == true) {
            if (ifTrue != null)
                return ifTrue
        } else if (getValue() == false) {
            if (ifFalse != null)
                return ifFalse
        }
        return super.valueToString(parentConfiguration = parentConfiguration)
    }

    override fun getShowName(): Boolean {
        val v = getValue()
        if (v == null || (v == true && ifTrue == null) || (v == false && ifFalse == null)) {
            // We are missing a description for the flag value so we need to show the
            // flag name. The property will have DiagnosticLevel.hidden for this case
            // so users will not see this the property in this case unless they are
            // displaying hidden properties.
            return true
        }
        return super.getShowName()
    }

    override fun getLevel(): DiagnosticLevel {
        val v = getValue()
        if (v == true) {
            if (ifTrue == null)
                return DiagnosticLevel.hidden
        }
        if (v == false) {
            if (ifFalse == null)
                return DiagnosticLevel.hidden
        }
        return super.getLevel()
    }
}