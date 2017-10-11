package android.support.tools.jetifier.core.transform

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaType
import java.util.regex.Pattern

/**
 * Context to share the transformation state between individual [Transformer]s.
 */
class TransformationContext(val config: Config) {

    // Merges all packages prefixes into one regEx pattern
    private val packagePrefixPattern = Pattern.compile(
        "^(" + config.restrictToPackagePrefixes.map { "($it)" }.joinToString("|") + ").*$")

    /** Counter for [reportNoMappingFoundFailure] calls. */
    var mappingNotFoundFailuresCount = 0
        private set

    /** Returns whether any errors were found during the transformation process */
    fun wasErrorFound() = mappingNotFoundFailuresCount > 0

    /**
     * Returns whether the given type is eligible for rewrite.
     *
     * If not, the transformers should ignore it.
     */
    fun isEligibleForRewrite(type: JavaType) : Boolean {
        if (config.restrictToPackagePrefixes.isEmpty()) {
            return false
        }
        return packagePrefixPattern.matcher(type.fullName).matches()
    }

    /**
     * Used to report that there was a reference found that satisfies [isEligibleForRewrite] but no
     * mapping was found to rewrite it.
     */
    fun reportNoMappingFoundFailure() {
        mappingNotFoundFailuresCount++
    }
}