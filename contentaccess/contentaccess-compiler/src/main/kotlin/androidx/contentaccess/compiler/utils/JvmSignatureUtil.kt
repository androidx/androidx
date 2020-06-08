package androidx.contentaccess.compiler.utils

import com.google.auto.common.MoreElements
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import java.io.Writer
import java.util.ArrayList
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Name
import javax.lang.model.element.NestingKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.IntersectionType
import javax.lang.model.type.NoType
import javax.lang.model.type.NullType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.UnionType
import javax.lang.model.type.WildcardType
import javax.lang.model.util.AbstractTypeVisitor8
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class JvmSignatureUtil(
    elements: Elements,
    types: Types
) : Elements {
    internal val elements: Elements
    private val types: Types

    constructor(processingEnv: ProcessingEnvironment) : this(
        processingEnv.elementUtils,
        processingEnv.typeUtils
    ) {
    }

    override fun getTypeElement(name: CharSequence): TypeElement {
        return elements.getTypeElement(name)
    }

    /**
     * Invokes [Elements.getTypeElement], throwing [TypeNotPresentException]
     * if it is not accessible in the current compilation.
     */
    fun checkTypePresent(typeName: String?): TypeElement {
        return elements.getTypeElement(typeName) ?: throw TypeNotPresentException(typeName, null)
    }

    override fun getPackageElement(name: CharSequence): PackageElement {
        return elements.getPackageElement(name)
    }

    override fun getElementValuesWithDefaults(
        a: AnnotationMirror
    ): Map<out ExecutableElement, AnnotationValue> {
        return elements.getElementValuesWithDefaults(a)
    }

    /** Returns a map of annotation values keyed by attribute name.  */
    fun getElementValuesWithDefaultsByName(
        a: AnnotationMirror
    ): Map<String, AnnotationValue?> {
        val builder =
            ImmutableMap.builder<String, AnnotationValue?>()
        val map =
            getElementValuesWithDefaults(a)
        for (e in map.keys) {
            builder.put(e.simpleName.toString(), map[e]!!)
        }
        return builder.build()
    }

    override fun getDocComment(e: Element): String {
        return elements.getDocComment(e)
    }

    override fun isDeprecated(e: Element): Boolean {
        return elements.isDeprecated(e)
    }

    override fun getBinaryName(type: TypeElement): Name {
        return elements.getBinaryName(type)
    }

    override fun getPackageOf(type: Element): PackageElement {
        return elements.getPackageOf(type)
    }

    override fun getAllMembers(type: TypeElement): List<Element> {
        return elements.getAllMembers(type)
    }

    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
        return elements.getAllAnnotationMirrors(e)
    }

    override fun hides(
        hider: Element,
        hidden: Element
    ): Boolean {
        return elements.hides(hider, hidden)
    }

    override fun overrides(
        overrider: ExecutableElement,
        overridden: ExecutableElement,
        type: TypeElement
    ): Boolean {
        return elements.overrides(overrider, overridden, type)
    }

    override fun getConstantExpression(value: Any): String {
        return elements.getConstantExpression(value)
    }

    override fun printElements(
        w: Writer,
        vararg elements: Element
    ) {
        this.elements.printElements(w, *elements)
    }

    override fun getName(cs: CharSequence): Name {
        return elements.getName(cs)
    }

    override fun isFunctionalInterface(type: TypeElement): Boolean {
        return elements.isFunctionalInterface(type)
    }

    companion object {
        fun getMethodDescriptor(element: ExecutableElement): String {
            return element.simpleName.toString() + getDescriptor(
                element.asType()
            )
        }

        private fun getDescriptor(t: TypeMirror): String {
            return t.accept(
                JVM_DESCRIPTOR_TYPE_VISITOR,
                null
            )
        }

        private val JVM_DESCRIPTOR_TYPE_VISITOR =
            object : AbstractTypeVisitor8<String, Void>() {
                override fun visitArray(
                    arrayType: ArrayType,
                    v: Void
                ): String {
                    return "[" + getDescriptor(arrayType.componentType)
                }

                override fun visitDeclared(
                    declaredType: DeclaredType,
                    v: Void?
                ): String {
                    return "L" + getInternalName(declaredType.asElement()) + ";"
                }

                override fun visitError(
                    errorType: ErrorType,
                    v: Void?
                ): String {
                    return visitUnknown(errorType, null)
                }

                override fun visitExecutable(
                    executableType: ExecutableType,
                    v: Void?
                ): String {
                    val descriptors =
                        ArrayList<String>()
                    for (tm in executableType.parameterTypes) {
                        descriptors.add(getDescriptor(tm))
                    }
                    val parameterDescriptors = java.lang.String.join("", descriptors)
                    val returnDescriptor =
                        getDescriptor(executableType.returnType)
                    return "($parameterDescriptors)$returnDescriptor"
                }

                override fun visitIntersection(
                    intersectionType: IntersectionType,
                    v: Void?
                ): String {
                    return getDescriptor(
                        intersectionType.bounds[0]
                    )
                }

                override fun visitNoType(noType: NoType, v: Void): String {
                    return "V"
                }

                override fun visitNull(
                    nullType: NullType,
                    v: Void?
                ): String {
                    return visitUnknown(nullType, null)
                }

                override fun visitPrimitive(
                    primitiveType: PrimitiveType,
                    v: Void?
                ): String {
                    return when (primitiveType.kind) {
                        TypeKind.BOOLEAN -> "Z"
                        TypeKind.BYTE -> "B"
                        TypeKind.SHORT -> "S"
                        TypeKind.INT -> "I"
                        TypeKind.LONG -> "J"
                        TypeKind.CHAR -> "C"
                        TypeKind.FLOAT -> "F"
                        TypeKind.DOUBLE -> "D"
                        else -> throw IllegalArgumentException("Unknown primitive type.")
                    }
                }

                override fun visitTypeVariable(
                    typeVariable: TypeVariable,
                    v: Void?
                ): String {
                    return getDescriptor(typeVariable.upperBound)
                }

                override fun visitUnion(
                    unionType: UnionType,
                    v: Void?
                ): String {
                    return visitUnknown(unionType, null)
                }

                override fun visitUnknown(
                    typeMirror: TypeMirror,
                    v: Void?
                ): String {
                    throw IllegalArgumentException("Unsupported type: " + typeMirror)
                }

                override fun visitWildcard(
                    wildcardType: WildcardType,
                    v: Void?
                ): String {
                    return ""
                }

                private fun getInternalName(element: Element): String {
                    try {
                        val typeElement =
                            MoreElements.asType(element)
                        return when (typeElement.getNestingKind()) {
                            NestingKind.TOP_LEVEL -> typeElement.getQualifiedName()
                                .toString().replace('.', '/')
                            NestingKind.MEMBER -> getInternalName(typeElement.getEnclosingElement
                                ()) + "$" + typeElement.getSimpleName()
                            else -> throw IllegalArgumentException("Unsupported nesting kind.")
                        }
                    } catch (e: IllegalArgumentException) {
                        // Not a TypeElement, try something else...
                    }
                    if (element is QualifiedNameable) {
                        val qualifiedNameElement = element
                        return qualifiedNameElement.getQualifiedName()
                            .toString().replace('.', '/')
                    }
                    return element.getSimpleName().toString()
                }
            }
    }

    init {
        this.elements =
            Preconditions.checkNotNull(
                elements
            )
        this.types =
            Preconditions.checkNotNull(types)
    }
}