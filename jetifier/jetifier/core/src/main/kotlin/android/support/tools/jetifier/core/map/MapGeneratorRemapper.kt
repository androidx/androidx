package android.support.tools.jetifier.core.map

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaField
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.bytecode.CoreRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomClassRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
import org.objectweb.asm.ClassVisitor
import java.util.regex.Pattern

/**
 * Hooks to asm remapping to collect data for [TypesMap] by applying all the [RewriteRule]s from the
 * given [config] on all discovered and eligible types and fields.
 */
class MapGeneratorRemapper(private val config: Config) : CoreRemapper {

    companion object {
        private const val TAG : String = "MapGeneratorRemapper"
    }

    private val typesRewritesMap = hashMapOf<JavaType, JavaType>()
    private val fieldsRewritesMap = hashMapOf<JavaField, JavaField>()

    var isMapNotComplete = false
        private set

    /**
     * Ignore mPrefix types and anything that contains $ as these are internal fields that won't be
     * ever referenced.
     */
    private val ignoredFields = Pattern.compile("(^m[A-Z]+.*$)|(.*\\$.*)")

    fun createClassRemapper(visitor: ClassVisitor): CustomClassRemapper {
        return CustomClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        if (!isTypeSupported(type)) {
            return type
        }

        if (typesRewritesMap.contains(type)) {
            return type
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedTypeName = rule.apply(type) ?: continue
            typesRewritesMap.put(type, mappedTypeName)

            Log.i(TAG, "  map: %s -> %s", type, mappedTypeName)
            return mappedTypeName
        }

        isMapNotComplete = true
        Log.e(TAG, "No rule for: " + type)
        typesRewritesMap.put(type, type) // Identity
        return type
    }

    override fun rewriteField(field : JavaField): JavaField {
        if (!isTypeSupported(field.owner)) {
            return field
        }

        if (ignoredFields.matcher(field.name).matches()) {
            return field
        }

        if (fieldsRewritesMap.contains(field)) {
            return field
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedFieldName = rule.apply(field) ?: continue
            fieldsRewritesMap.put(field, mappedFieldName)

            Log.i(TAG, "  map: %s -> %s", field, mappedFieldName)
            return mappedFieldName
        }

        isMapNotComplete = true
        Log.e(TAG, "No rule for: " + field)
        fieldsRewritesMap.put(field, field) // Identity
        return field
    }

    fun createTypesMap() : TypesMap {
        return TypesMap(typesRewritesMap, fieldsRewritesMap)
    }

    private fun isTypeSupported(type: JavaType) : Boolean {
        return config.restrictToPackagePrefixes.any{ type.fullName.startsWith(it) }
    }
}