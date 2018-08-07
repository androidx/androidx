package androidx.ui.foundation.diagnostics

import androidx.ui.Type

typealias ComputePropertyValueCallback<T> = () -> T

// / Property with a [value] of type [T].
// /
// / If the default `value.toString()` does not provide an adequate description
// / of the value, specify `description` defining a custom description.
// /
// / The [showSeparator] property indicates whether a separator should be placed
// / between the property [name] and its [value].
open class DiagnosticsProperty<T : Any> internal constructor(
    name: String,
    value: T? = null,
    private val computeValue: ComputePropertyValueCallback<T>? = null,
    val description: String? = null,
        // / Description if the property [value] is null.
    var ifNull: String? = null,
        // / Description if the property description would otherwise be empty.
    val ifEmpty: String? = null,
    showName: Boolean = true,
    showSeparator: Boolean = true,
        // / If the [value] of the property equals [defaultValue] the priority [level]
        // / of the property is downgraded to [DiagnosticLevel.fine] as the property
        // / value is uninteresting.
        // /
        // / [defaultValue] has type [T] or is [kNoDefaultValue].
    val defaultValue: Any? = kNoDefaultValue,
        // / Optional tooltip typically describing the property.
        // /
        // / Example tooltip: 'physical pixels per logical pixel'
        // /
        // / If present, the tooltip is added in parenthesis after the raw value when
        // / generating the string description.
    val tooltip: String? = null,
        // / Whether a [value] of null causes the property to have [level]
        // / [DiagnosticLevel.warning] warning that the property is missing a [value].
    val missingIfNull: Boolean = false,
    style: DiagnosticsTreeStyle = DiagnosticsTreeStyle.singleLine,
    private val level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsNode(
        name = name,
        showName = showName,
        showSeparator = showSeparator,
        style = style) {

    protected val _description: String? = description

    private var _value: T? = value

    private var _valueComputed: Boolean = true

    private var _exception: Throwable? = null

    private val _defaultLevel: DiagnosticLevel = level

    init {
        assert(showName != null)
        assert(showSeparator != null)
        assert(style != null)
        assert(level != null)

        // TODO(Filip): Type erasure prevents this:
        // assert(defaultValue == kNoDefaultValue || defaultValue is T)

        if (ifNull == null) {
            ifNull = if (missingIfNull) "MISSING" else null
        }

        if (computeValue != null) {
            _valueComputed = false
        }
    }

    companion object {
        fun <T : Any> create(
            name: String,
            value: T?,
            description: String? = null,
            ifNull: String? = null,
            ifEmpty: String? = null,
            showName: Boolean = true,
            showSeparator: Boolean = true,
            defaultValue: Any? = kNoDefaultValue,
            tooltip: String? = null,
            missingIfNull: Boolean = false,
            style: DiagnosticsTreeStyle = DiagnosticsTreeStyle.singleLine,
            level: DiagnosticLevel = DiagnosticLevel.info
        ): DiagnosticsProperty<T> {
            return DiagnosticsProperty(
                    name = name,
                    value = value,
                    description = description,
                    ifNull = ifNull,
                    ifEmpty = ifEmpty,
                    showName = showName,
                    showSeparator = showSeparator,
                    defaultValue = defaultValue,
                    tooltip = tooltip,
                    missingIfNull = missingIfNull,
                    style = style,
                    level = level
            )
        }

        // / Property with a [value] that is computed only when needed.
        // /
        // / Use if computing the property [value] may throw an exception or is
        // / expensive.
        // /
        // / The [showName], [showSeparator], [style], [missingIfNull], and [level]
        // / arguments must not be null.
        // /
        // / The [level] argument is just a suggestion and can be overridden if
        // / if something else about the property causes it to have a lower or higher
        // / level. For example, if calling `computeValue` throws an exception, [level]
        // / will always return [DiagnosticLevel.error].
        fun <T : Any> createLazy(
            name: String,
            computeValue: ComputePropertyValueCallback<T>,
            description: String? = null,
            ifNull: String,
            ifEmpty: String? = null,
            showName: Boolean = true,
            showSeparator: Boolean = true,
            defaultValue: Any = kNoDefaultValue,
            tooltip: String,
            missingIfNull: Boolean = false,
            style: DiagnosticsTreeStyle = DiagnosticsTreeStyle.singleLine,
            level: DiagnosticLevel = DiagnosticLevel.info
        ): DiagnosticsProperty<T> {
            return DiagnosticsProperty(
                    name = name,
                    computeValue = computeValue,
                    description = description,
                    ifNull = ifNull,
                    ifEmpty = ifEmpty,
                    showName = showName,
                    showSeparator = showSeparator,
                    defaultValue = defaultValue,
                    tooltip = tooltip,
                    missingIfNull = missingIfNull,
                    style = style,
                    level = level
            )
        }
    }

    override fun toJsonMap(): Map<String, Any> {
        val json = super.toJsonMap().toMutableMap()

        if (defaultValue != kNoDefaultValue)
            json["defaultValue"] = defaultValue.toString()
        if (ifEmpty != null)
            json["ifEmpty"] = ifEmpty
        if (ifNull != null)
            json["ifNull"] = ifNull!!
        if (tooltip != null)
            json["tooltip"] = tooltip
        json["missingIfNull"] = missingIfNull
        if (getException() != null)
            json["exception"] = getException().toString()
        json["propertyType"] = propertyType().toString()
        json["valueToString"] = valueToString()
        json["defaultLevel"] = _defaultLevel.toString()
        // TODO(Migration/Filip): Cannot do this
        // if (T is Diagnosticable)
        //    json["isDiagnosticableValue"] = true;
        return json
    }

    // / Returns a string representation of the property value.
    // /
    // / Subclasses should override this method instead of [toDescription] to
    // / customize how property values are converted to strings.
    // /
    // / Overriding this method ensures that behavior controlling how property
    // / values are decorated to generate a nice [toDescription] are consistent
    // / across all implementations. Debugging tools may also choose to use
    // / [valueToString] directly instead of [toDescription].
    // /
    // / `parentConfiguration` specifies how the parent is rendered as text art.
    // / For example, if the parent places all properties on one line, the value
    // / of the property should be displayed without line breaks if possible.
    open fun valueToString(parentConfiguration: TextTreeConfiguration? = null): String {
        val v = getValue()
        // DiagnosticableTree values are shown using the shorter toStringShort()
        // instead of the longer toString() because the toString() for a
        // DiagnosticableTree value is likely too large to be useful.
        return if (v is DiagnosticableTree) v.toStringShort() else v.toString()
    }

    override fun toDescription(parentConfiguration: TextTreeConfiguration?): String {
        if (_description != null)
            return addTooltip(_description)

        if (_exception != null)
            return "EXCEPTION (${getException()})"; // TODO(Filip): is toString enough?

        if (ifNull != null && getValue() == null)
            return addTooltip(ifNull!!)

        var result = valueToString(parentConfiguration = parentConfiguration)
        if (result.isEmpty() && ifEmpty != null)
            result = ifEmpty
        return addTooltip(result)
    }

    // / If a [tooltip] is specified, add the tooltip it to the end of `text`
    // / enclosing it parenthesis to disambiguate the tooltip from the rest of
    // / the text.
    // /
    // / `text` must not be null.
    private fun addTooltip(text: String): String {
        tooltip ?: return text
        return "$text ($tooltip)"
    }

    // / The type of the property [value].
    // /
    // / This is determined from the type argument `T` used to instantiate the
    // / [DiagnosticsProperty] class. This means that the type is available even if
    // / [value] is null, but it also means that the [propertyType] is only as
    // / accurate as the type provided when invoking the constructor.
    // /
    // / Generally, this is only useful for diagnostic tools that should display
    // / null values in a manner consistent with the property type. For example, a
    // / tool might display a null [Color] value as an empty rectangle instead of
    // / the word "null".
    // TODO(Migration/Filip): We can't do T::class.java in Kotlin. Need to revisit this.
    fun propertyType() =
            Type.fromObject(if (getValue() == null) Any::class.java else getValue()!!::class.java)

    // / Returns the value of the property either from cache or by invoking a
    // / [ComputePropertyValueCallback].
    // /
    // / If an exception is thrown invoking the [ComputePropertyValueCallback],
    // / [value] returns null and the exception thrown can be found via the
    // / [exception] property.
    // /
    // / See also:
    // /
    // /  * [valueToString], which converts the property value to a string.
    override fun getValue(): T? {
        maybeCacheValue()
        return _value
    }

    // / Exception thrown if accessing the property [value] threw an exception.
    // /
    // / Returns null if computing the property value did not throw an exception.
    fun getException(): Throwable? {
        maybeCacheValue()
        return _exception
    }

    private fun maybeCacheValue() {
        if (_valueComputed) {
            return
        }

        _valueComputed = true
        assert(computeValue != null)
        try {
            _value = computeValue?.invoke()
        } catch (e: Throwable) {
            _exception = e
            _value = null
        }
    }

    // / Priority level of the diagnostic used to control which diagnostics should
    // / be shown and filtered.
    // /
    // / The property level defaults to the value specified by the `level`
    // / constructor argument. The level is raised to [DiagnosticLevel.error] if
    // / an [exception] was thrown getting the property [value]. The level is
    // / raised to [DiagnosticLevel.warning] if the property [value] is null and
    // / the property is not allowed to be null due to [missingIfNull]. The
    // / priority level is lowered to [DiagnosticLevel.fine] if the property
    // / [value] equals [defaultValue].
    override fun getLevel(): DiagnosticLevel {
        if (_defaultLevel == DiagnosticLevel.hidden)
            return _defaultLevel

        if (_exception != null)
            return DiagnosticLevel.error

        if (getValue() == null && missingIfNull)
            return DiagnosticLevel.warning

        // Use a low level when the value matches the default value.
        if (defaultValue != kNoDefaultValue && getValue() == defaultValue)
            return DiagnosticLevel.fine

        return _defaultLevel
    }

    override fun getProperties(): List<DiagnosticsNode> = emptyList()

    override fun getChildren(): List<DiagnosticsNode> = emptyList()
}
