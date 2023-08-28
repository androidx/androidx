/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.icons.generator

import androidx.compose.material.icons.generator.vector.FillType
import androidx.compose.material.icons.generator.vector.Vector
import androidx.compose.material.icons.generator.vector.VectorNode
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.buildCodeBlock
import java.util.Locale

/**
 * Generator for creating a Kotlin source file with an ImageVector property for the given [vector],
 * with name [iconName] and theme [iconTheme].
 *
 * @param iconName the name for the generated property, which is also used for the generated file.
 * I.e if the name is `Menu`, the property will be `Menu` (inside a theme receiver object) and
 * the file will be `Menu.kt` (under the theme package name).
 * @param iconTheme the theme that this vector belongs to. Used to scope the property to the
 * correct receiver object, and also for the package name of the generated file.
 * @param vector the parsed vector to generate ImageVector.Builder commands for
 */
class ImageVectorGenerator(
    private val iconName: String,
    private val iconTheme: IconTheme,
    private val vector: Vector
) {
    /**
     * @return a [FileSpec] representing a Kotlin source file containing the property for this
     * programmatic [vector] representation.
     *
     * The package name and hence file location of the generated file is:
     * [PackageNames.MaterialIconsPackage] + [IconTheme.themePackageName].
     */
    fun createFileSpec(): FileSpec {
        val builder = createFileSpecBuilder(themePackageName = iconTheme.themePackageName)
        val backingProperty = getBackingProperty()
        // Create a property with a getter. The autoMirror is always false in this case.
        val propertySpecBuilder =
            PropertySpec.builder(name = iconName, type = ClassNames.ImageVector)
                .receiver(iconTheme.className)
                .getter(
                    iconGetter(
                        backingProperty = backingProperty,
                        iconName = iconName,
                        iconTheme = iconTheme,
                        autoMirror = false
                    )
                )
        // Add a deprecation warning with a suggestion to replace this icon's usage with its
        // equivalent that was generated under the automirrored package.
        if (vector.autoMirrored) {
            val autoMirroredPackage = "${PackageNames.MaterialIconsPackage.packageName}." +
                "$AutoMirroredPackageName.${iconTheme.themePackageName}"
            propertySpecBuilder.addAnnotation(
                AnnotationSpec.builder(Deprecated::class)
                    .addMember(
                        "\"Use the AutoMirrored version at %N.%N.%N.%N\"",
                        ClassNames.Icons.simpleName,
                        AutoMirroredName,
                        iconTheme.name,
                        iconName
                    )
                    .addMember(
                        "ReplaceWith( \"%N.%N.%N.%N\", \"$autoMirroredPackage.%N\")",
                        ClassNames.Icons.simpleName,
                        AutoMirroredName,
                        iconTheme.name,
                        iconName,
                        iconName
                    )
                    .build()
            )
        }
        builder.addProperty(propertySpecBuilder.build())
        builder.addProperty(backingProperty)
        return builder.setIndent().build()
    }

    /**
     * @return a [FileSpec] representing a Kotlin source file containing the property for this
     * programmatic, auto-mirrored, [vector] representation.
     *
     * The package name and hence file location of the generated file is:
     * [PackageNames.MaterialIconsPackage] + [AutoMirroredPackageName] +
     * [IconTheme.themePackageName].
     */
    fun createAutoMirroredFileSpec(): FileSpec {
        // Prepend the AutoMirroredName package name to the IconTheme package name.
        val builder = createFileSpecBuilder(
            themePackageName = "$AutoMirroredPackageName.${iconTheme.themePackageName}"
        )
        val backingProperty = getBackingProperty()
        // Create a property with a getter. The autoMirror is always false in this case.
        builder.addProperty(
            PropertySpec.builder(name = iconName, type = ClassNames.ImageVector)
                .receiver(iconTheme.autoMirroredClassName)
                .getter(
                    iconGetter(
                        backingProperty = backingProperty,
                        iconName = iconName,
                        iconTheme = iconTheme,
                        autoMirror = true
                    )
                )
                .build()
        )
        builder.addProperty(backingProperty)
        return builder.setIndent().build()
    }

    private fun createFileSpecBuilder(themePackageName: String): FileSpec.Builder {
        val iconsPackage = PackageNames.MaterialIconsPackage.packageName
        val combinedPackageName = "$iconsPackage.$themePackageName"
        return FileSpec.builder(
            packageName = combinedPackageName,
            fileName = iconName
        )
    }

    private fun getBackingProperty(): PropertySpec {
        // Use a unique property name for the private backing property. This is because (as of
        // Kotlin 1.4) each property with the same name will be considered as a possible candidate
        // for resolution, regardless of the access modifier, so by using unique names we reduce
        // the size from ~6000 to 1, and speed up compilation time for these icons.
        val backingPropertyName = "_" + iconName.replaceFirstChar { it.lowercase(Locale.ROOT) }
        return backingProperty(name = backingPropertyName)
    }

    /**
     * @return the body of the getter for the icon property. This getter returns the backing
     * property if it is not null, otherwise creates the icon and 'caches' it in the backing
     * property, and then returns the backing property.
     */
    private fun iconGetter(
        backingProperty: PropertySpec,
        iconName: String,
        iconTheme: IconTheme,
        autoMirror: Boolean
    ): FunSpec {
        return FunSpec.getterBuilder()
            .addCode(
                buildCodeBlock {
                    beginControlFlow("if (%N != null)", backingProperty)
                    addStatement("return %N!!", backingProperty)
                    endControlFlow()
                }
            )
            .addCode(
                buildCodeBlock {
                    val controlFlow = if (autoMirror) {
                        "%N = %M(name = \"$AutoMirroredName.%N.%N\", autoMirror = true)"
                    } else {
                        "%N = %M(name = \"%N.%N\")"
                    }
                    beginControlFlow(
                        controlFlow,
                        backingProperty,
                        MemberNames.MaterialIcon,
                        iconTheme.name,
                        iconName
                    )
                    vector.nodes.forEach { node -> addRecursively(node) }
                    endControlFlow()
                }
            )
            .addStatement("return %N!!", backingProperty)
            .build()
    }

    /**
     * @return The private backing property that is used to cache the ImageVector for a given
     * icon once created.
     *
     * @param name the name of this property
     */
    private fun backingProperty(name: String): PropertySpec {
        val nullableImageVector = ClassNames.ImageVector.copy(nullable = true)
        return PropertySpec.builder(name = name, type = nullableImageVector)
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
    }
}

/**
 * Recursively adds function calls to construct the given [vectorNode] and its children.
 */
private fun CodeBlock.Builder.addRecursively(vectorNode: VectorNode) {
    when (vectorNode) {
        // TODO: b/147418351 - add clip-paths once they are supported
        is VectorNode.Group -> {
            beginControlFlow("%M", MemberNames.Group)
            vectorNode.paths.forEach { path ->
                addRecursively(path)
            }
            endControlFlow()
        }

        is VectorNode.Path -> {
            addPath(vectorNode) {
                vectorNode.nodes.forEach { pathNode ->
                    addStatement(pathNode.asFunctionCall())
                }
            }
        }
    }
}

/**
 * Adds a function call to create the given [path], with [pathBody] containing the commands for
 * the path.
 */
private fun CodeBlock.Builder.addPath(
    path: VectorNode.Path,
    pathBody: CodeBlock.Builder.() -> Unit
) {
    // Only set the fill type if it is EvenOdd - otherwise it will just be the default.
    val setFillType = path.fillType == FillType.EvenOdd

    val parameterList = with(path) {
        listOfNotNull(
            "fillAlpha = ${fillAlpha}f".takeIf { fillAlpha != 1f },
            "strokeAlpha = ${strokeAlpha}f".takeIf { strokeAlpha != 1f },
            "pathFillType = %M".takeIf { setFillType }
        )
    }

    val parameters = if (parameterList.isNotEmpty()) {
        parameterList.joinToString(prefix = "(", postfix = ")")
    } else {
        ""
    }

    if (setFillType) {
        beginControlFlow("%M$parameters", MemberNames.MaterialPath, MemberNames.EvenOdd)
    } else {
        beginControlFlow("%M$parameters", MemberNames.MaterialPath)
    }
    pathBody()
    endControlFlow()
}
