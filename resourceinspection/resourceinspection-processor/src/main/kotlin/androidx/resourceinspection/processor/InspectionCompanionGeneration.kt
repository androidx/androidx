/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.resourceinspection.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.Locale
import javax.lang.model.element.Modifier

/** Generates an inspection companion from a view using JavaPoet. */
internal fun generateInspectionCompanion(
    view: View,
    generatedAnnotation: AnnotationSpec?
): JavaFile {
    val typeSpec = TypeSpec.classBuilder(
        view.className.simpleNames().joinToString(
            separator = "\$",
            postfix = "\$InspectionCompanion"
        )
    ).apply {
        addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        addSuperinterface(INSPECTION_COMPANION.parameterized(view.className))
        addAnnotation(REQUIRES_API)
        addAnnotation(RESTRICT_TO)

        generatedAnnotation?.let { addAnnotation(it) }

        addOriginatingElement(view.type)

        addJavadoc("Inspection companion for {@link \$T}.\n\n@hide", view.className)

        addField(
            FieldSpec.builder(TypeName.BOOLEAN, "mPropertiesMapped", Modifier.PRIVATE).run {
                initializer("false")
                build()
            }
        )

        val attributeIdNames = NameAllocator().apply {
            for (attribute in view.attributes) {
                val attributeName = attribute.name.replaceFirstChar { it.uppercase() }
                newName("m${attributeName}Id", attribute)
            }
        }

        for (attribute in view.attributes) {
            addField(TypeName.INT, attributeIdNames[attribute], Modifier.PRIVATE)
        }

        addMethod(
            MethodSpec.methodBuilder("mapProperties").apply {
                addAnnotation(OVERRIDE)
                addModifiers(Modifier.PUBLIC)
                addParameter(PROPERTY_MAPPER.annotated(NON_NULL), "propertyMapper")

                for (attribute in view.attributes) {
                    when (attribute.type) {
                        AttributeType.INT_ENUM -> addStatement(
                            "\$N = propertyMapper.mapIntEnum(\$S, \$L, \$L)",
                            attributeIdNames[attribute],
                            attribute.name,
                            attribute.attrReference,
                            intEnumLambda(attribute)
                        )
                        AttributeType.INT_FLAG -> addStatement(
                            "\$N = propertyMapper.mapIntFlag(\$S, \$L, \$L)",
                            attributeIdNames[attribute],
                            attribute.name,
                            attribute.attrReference,
                            intFlagLambda(attribute)
                        )
                        else -> addStatement(
                            "\$N = propertyMapper.map\$L(\$S, \$L)",
                            attributeIdNames[attribute],
                            attribute.type.apiSuffix,
                            attribute.name,
                            attribute.attrReference
                        )
                    }
                }
            }.build()
        )

        addMethod(
            MethodSpec.methodBuilder("readProperties").apply {
                // Make sure the view parameter name doesn't conflict with anything
                val decapitalizedClassName = view.className.simpleName()
                    .replaceFirstChar { it.lowercase(Locale.US) }
                val viewParameter = attributeIdNames.clone()
                    .apply { newName("propertyReader") }
                    .newName(decapitalizedClassName)

                addAnnotation(OVERRIDE)
                addModifiers(Modifier.PUBLIC)
                addParameter(view.className.annotated(NON_NULL), viewParameter)
                addParameter(PROPERTY_READER.annotated(NON_NULL), "propertyReader")

                beginControlFlow("if (!mPropertiesMapped)")
                addStatement("throw new \$T()", UNINITIALIZED_EXCEPTION)
                endControlFlow()

                for (attribute in view.attributes) {
                    addStatement(
                        "propertyReader.read\$L(\$N, \$N.\$L)",
                        attribute.type.apiSuffix,
                        attributeIdNames[attribute],
                        viewParameter,
                        attribute.invocation
                    )
                }
            }.build()
        )
    }.build()

    return JavaFile.builder(view.className.packageName(), typeSpec)
        .indent(" ".repeat(4))
        .build()
}

/** The `(Int) -> String` lambda for int enums, as an anonymous class for Java 7 compatibility. */
private fun intEnumLambda(attribute: Attribute): TypeSpec {
    return TypeSpec.anonymousClassBuilder("").apply {
        addSuperinterface(INT_FUNCTION.parameterized(STRING))

        addMethod(
            MethodSpec.methodBuilder("apply").apply {
                addAnnotation(OVERRIDE)
                addModifiers(Modifier.PUBLIC)
                returns(STRING)
                addParameter(TypeName.INT, "value")

                beginControlFlow("switch (value)")

                attribute.intMapping.forEach { (name, value, _) ->
                    addCode("case \$L:\n\$>return \$S;\n\$<", value, name)
                }

                addCode("default:\n\$>return \$T.valueOf(value);\n\$<", STRING)
                endControlFlow()
            }.build()
        )
    }.build()
}

/** The `(Int) -> Set<String>` lambda for int flags, as an anonymous class. */
private fun intFlagLambda(attribute: Attribute): TypeSpec {
    val stringSet = SET.parameterized(STRING)
    val stringHashSet = HASH_SET.parameterized(STRING)

    return TypeSpec.anonymousClassBuilder("").apply {
        addSuperinterface(INT_FUNCTION.parameterized(stringSet))

        addMethod(
            MethodSpec.methodBuilder("apply").apply {
                addAnnotation(OVERRIDE)
                addModifiers(Modifier.PUBLIC)
                returns(stringSet)
                addParameter(TypeName.INT, "value")

                addStatement("final \$T flags = new \$T()", stringSet, stringHashSet)

                attribute.intMapping.forEach { (name, value, mask) ->
                    if (mask == 0) {
                        beginControlFlow("if (value == \$L)", value)
                    } else {
                        beginControlFlow("if ((value & \$L) == \$L)", mask, value)
                    }
                    addStatement("flags.add(\$S)", name)
                    endControlFlow()
                }

                addStatement("return flags")
            }.build()
        )
    }.build()
}

/** A [CodeBlock] of the `$namespace.R.attr.$name` attribute ID reference. */
private val Attribute.attrReference: CodeBlock
    get() = CodeBlock.of("\$T.attr.\$N", ClassName.get(namespace, "R"), name)

/** Kotlin wrapper for [ClassName.get] to avoid platform types. */
private fun className(packageName: String, simpleName: String): ClassName {
    return ClassName.get(packageName, simpleName)
}

/** Idiomatic wrapper for [ParameterizedTypeName.get]. */
private fun ClassName.parameterized(vararg types: TypeName): ParameterizedTypeName {
    return ParameterizedTypeName.get(this, *types)
}

private val INSPECTION_COMPANION = className("android.view.inspector", "InspectionCompanion")
private val PROPERTY_MAPPER = className("android.view.inspector", "PropertyMapper")
private val PROPERTY_READER = className("android.view.inspector", "PropertyReader")
private val UNINITIALIZED_EXCEPTION: ClassName =
    INSPECTION_COMPANION.nestedClass("UninitializedPropertyMapException")

private val INT_FUNCTION = className("java.util.function", "IntFunction")
private val STRING = className("java.lang", "String")
private val SET = className("java.util", "Set")
private val HASH_SET = className("java.util", "HashSet")

private val NON_NULL: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "NonNull")).build()

private val OVERRIDE: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("java.lang", "Override")).build()

/** Minimum SDK version that supports the view inspection API. */
private const val MIN_SDK = 29

private val REQUIRES_API: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "RequiresApi"))
        .addMember("value", "\$L", MIN_SDK)
        .build()

private val RESTRICT_TO: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "RestrictTo"))
        .addMember(
            "value",
            "\$T.LIBRARY",
            ClassName.get("androidx.annotation", "RestrictTo", "Scope")
        )
        .build()