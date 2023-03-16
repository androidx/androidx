
package androidx.appactions.builtintypes.properties

class CallFormat private constructor(
    val asText: String?,
    val asCanonicalValue: CanonicalValue?,
) {

    constructor(text: String) : this(asText = text, asCanonicalValue = null)

    constructor(canonicalValue: CanonicalValue) : this(
        asText = null,
        asCanonicalValue = canonicalValue
    )

    abstract class CanonicalValue internal constructor(val textValue: String)
}