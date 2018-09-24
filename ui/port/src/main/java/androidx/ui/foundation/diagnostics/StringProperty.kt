package androidx.ui.foundation.diagnostics

/**
 * Property which encloses its string [value] in quotes.
 *
 * See also:
 *
 *  * [MessageProperty], which is a better fit for showing a message
 *    instead of describing a property with a string value.
 *
 * Ctor comment:
 * Create a diagnostics property for strings.
 *
 * The [showName], [quoted], and [level] arguments must not be null.
 */
class StringProperty(
    name: String,
    value: String?,
    description: String? = null,
    ifEmpty: String? = null,
    showName: Boolean = true,
    defaultValue: Any? = kNoDefaultValue,
    /** Whether the value is enclosed in double quotes. */
    val quoted: Boolean = true,
    tooltip: String? = null,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<String>(
        name = name,
        value = value,
        description = description,
        defaultValue = defaultValue,
        tooltip = tooltip,
        showName = showName,
        ifEmpty = ifEmpty,
        level = level
) {

    init {
        assert(showName != null)
        assert(quoted != null)
        assert(level != null)
    }

    override fun toJsonMap(): Map<String, Any> {
        val json = super.toJsonMap().toMutableMap()
        json["quoted"] = quoted
        return json
    }

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        var text = _description ?: getValue()
        if (parentConfiguration != null &&
                !parentConfiguration.lineBreakProperties &&
                text != null) {
            // Escape linebreaks in multiline strings to avoid confusing output when
            // the parent of this node is trying to display all properties on the same
            // line.
            text = text.replace("\n", "\\n")
        }

        if (quoted && text != null) {
            // An empty value would not appear empty after being surrounded with
            // quotes so we have to handle this case separately.
            if (ifEmpty != null && text.isEmpty())
                return ifEmpty
            return "\"$text\""
        }
        return text.toString()
    }
}