package androidx.ui.foundation.diagnostics

/**
 * Property with an `Iterable<T>` [value] that can be displayed with
 * different [DiagnosticsTreeStyle] for custom rendering.
 *
 * If [style] is [DiagnosticsTreeStyle.singleLine], the iterable is described
 * as a comma separated list, otherwise the iterable is described as a line
 * break separated list.
 *
 * Ctor comment:
 * Create a diagnostics property for iterables (e.g. lists).
 *
 * The [ifEmpty] argument is used to indicate how an iterable [value] with 0
 * elements is displayed. If [ifEmpty] equals null that indicates that an
 * empty iterable [value] is not interesting to display similar to how
 * [defaultValue] is used to indicate that a specific concrete value is not
 * interesting to display.
 *
 * The [style], [showName], and [level] arguments must not be null.
 */
class IterableProperty<T>(
    name: String,
    value: Iterable<T>?,
    defaultValue: Any? = kNoDefaultValue,
    ifNull: String? = null,
    ifEmpty: String? = "[]",
    showName: Boolean = true,
    style: DiagnosticsTreeStyle = DiagnosticsTreeStyle.singleLine,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<Iterable<T>>(
        name = name,
        value = value,
        defaultValue = defaultValue,
        ifNull = ifNull,
        ifEmpty = ifEmpty,
        style = style,
        showName = showName,
        level = level
) {

    init {
        assert(style != null)
        assert(showName != null)
        assert(level != null)
    }

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        val v = getValue()
        if (v == null)
            return getValue().toString()

        if (v.none())
            return ifEmpty ?: "[]"

        if (parentConfiguration != null && !parentConfiguration.lineBreakProperties) {
            // Always display the value as a single line and enclose the iterable
            // value in brackets to avoid ambiguity.
            return "[${v.joinToString(separator = ", ")}]"
        }

        val separator = if (getStyle() == DiagnosticsTreeStyle.singleLine) ", " else "\n"
        return getValue()!!.joinToString(separator = separator)
    }

    /**
     * Priority level of the diagnostic used to control which diagnostics should
     * be shown and filtered.
     *
     * If [ifEmpty] is null and the [value] is an empty [Iterable] then level
     * [DiagnosticLevel.fine] is returned in a similar way to how an
     * [ObjectFlagProperty] handles when [ifNull] is null and the [value] is
     * null.
     */
    override fun getLevel(): DiagnosticLevel {
        val v = getValue()
        if (ifEmpty == null && v != null && v.none() && super.getLevel() != DiagnosticLevel.hidden)
            return DiagnosticLevel.fine
        return super.getLevel()
    }

    override fun toJsonMap(): Map<String, Any> {
        val json = super.toJsonMap().toMutableMap()
        val v = getValue()
        if (v != null) {
            json["values"] = v.map { it.toString() }.toList()
        }
        return json
    }
}