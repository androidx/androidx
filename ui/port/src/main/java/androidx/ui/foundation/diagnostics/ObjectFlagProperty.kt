package androidx.ui.foundation.diagnostics

/**
 * A property where the important diagnostic information is primarily whether
 * the [value] is present (non-null) or absent (null), rather than the actual
 * value of the property itself.
 *
 * The [ifPresent] and [ifNull] strings describe the property [value] when it
 * is non-null and null respectively. If one of [ifPresent] or [ifNull] is
 * omitted, that is taken to mean that [level] should be
 * [DiagnosticsLevel.hidden] when [value] is non-null or null respectively.
 *
 * This kind of diagnostics property is typically used for values mostly opaque
 * values, like closures, where presenting the actual object is of dubious
 * value but where reporting the presence or absence of the value is much more
 * useful.
 *
 * See also:
 *
 *  * [FlagProperty], which provides similar functionality describing whether
 *    a [value] is true or false.
 *
 *
 * Create a diagnostics property for values that can be present (non-null) or
 * absent (null), but for which the exact value's [Object.toString]
 * representation is not very transparent (e.g. a callback).
 *
 * The [showName] and [level] arguments must not be null. Additionally, at
 * least one of [ifPresent] and [ifNull] must not be null.
 */
class ObjectFlagProperty<T : Any>(
    name: String,
    value: T?,
    /**
     * Description to use if the property [value] is not null.
     *
     * If the property [value] is not null and [ifPresent] is null, the
     * [level] for the property is [DiagnosticsLevel.hidden] and the description
     * from superclass is used.
     */
    val ifPresent: String? = null,
    ifNull: String? = null,
    showName: Boolean = false,
    level: DiagnosticLevel = DiagnosticLevel.info

) : DiagnosticsProperty<T>(
        name = name,
        value = value,
        showName = showName,
        ifNull = ifNull,
        level = level
) {

    init {
        assert(ifPresent != null || ifNull != null)
    }

    companion object {
        /**
         * Shorthand constructor to describe whether the property has a value.
         *
         * Only use if prefixing the property name with the word 'has' is a good
         * flag name.
         *
         * The [name] and [level] arguments must not be null.
         */
        fun <T : Any> has(
            name: String,
            value: T?,
            level: DiagnosticLevel = DiagnosticLevel.info
        ): ObjectFlagProperty<T>
        {
            return ObjectFlagProperty(
                    name = name,
                    value = value,
                    ifPresent = "has $name",
                    showName = false,
                    level = level)
        }
    }

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (getValue() != null) {
            if (ifPresent != null)
                return ifPresent
        } else {
            if (ifNull != null)
                return ifNull!!
        }
        return super.valueToString(parentConfiguration = parentConfiguration)
    }

    override fun getShowName(): Boolean {
        if ((getValue() != null && ifPresent == null) || (getValue() == null && ifNull == null)) {
            // We are missing a description for the flag value so we need to show the
            // flag name. The property will have DiagnosticLevel.hidden for this case
            // so users will not see this the property in this case unless they are
            // displaying hidden properties.
            return true
        }
        return super.getShowName()
    }

    override fun getLevel(): DiagnosticLevel {
        if (getValue() != null) {
            if (ifPresent == null)
                return DiagnosticLevel.hidden
        } else {
            if (ifNull == null)
                return DiagnosticLevel.hidden
        }

        return super.getLevel()
    }

    override fun toJsonMap(): Map<String, Any> {
        val json = super.toJsonMap().toMutableMap()
        if (ifPresent != null)
            json["ifPresent"] = ifPresent
        return json
    }
}