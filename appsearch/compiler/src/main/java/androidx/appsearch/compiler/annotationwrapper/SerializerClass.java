/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.compiler.annotationwrapper;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.ProcessingException;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Represents a class that can convert between some custom type and a property's actual type.
 *
 * @see androidx.appsearch.app.StringSerializer
 * @see androidx.appsearch.app.LongSerializer
 */
@AutoValue
public abstract class SerializerClass {
    public enum Kind {
        STRING_SERIALIZER(/* actualTypeInGenericDoc= */ClassName.get(String.class)),
        LONG_SERIALIZER(/* actualTypeInGenericDoc= */TypeName.LONG);

        /**
         * The actual type of the corresponding property within a {@code GenericDocument}.
         *
         * <p>For example, a {@link #STRING_SERIALIZER} may only be used with a
         * {@code @Document.StringProperty} which, in turn, boils down to a {@link String} within
         * a {@code GenericDocument}.
         */
        @NonNull
        @SuppressWarnings("ImmutableEnumChecker") // TypeName is an immutable 3P type
        final TypeName mActualTypeInGenericDoc;

        Kind(@NonNull TypeName actualTypeInGenericDoc) {
            mActualTypeInGenericDoc = actualTypeInGenericDoc;
        }
    }

    /**
     * The kind of serializer.
     */
    @NonNull
    public abstract Kind getKind();

    /**
     * The serializer class element.
     */
    @NonNull
    public abstract TypeElement getElement();

    /**
     * The zero-param constructor. Present on every serializer class.
     */
    @NonNull
    public abstract ExecutableElement getDefaultConstructor();

    /**
     * The custom type that can be serialized using the serializer class.
     */
    @NonNull
    public abstract TypeMirror getCustomType();

    /**
     * Creates a serializer class given its {@link TypeElement}.
     *
     * @throws ProcessingException If the {@code clazz} does not have a zero-param constructor.
     */
    @NonNull
    public static SerializerClass create(
            @NonNull TypeElement clazz, @NonNull Kind kind) throws ProcessingException {
        ExecutableElement deserializeMethod = findDeserializeMethod(clazz, kind);
        return new AutoValue_SerializerClass(
                kind,
                clazz,
                findDefaultConstructor(clazz),
                /* customType= */deserializeMethod.getReturnType());
    }

    /**
     * Returns the zero-param constructor in the {@code clazz}.
     *
     * @throws ProcessingException If no such constructor exists or it's private.
     */
    private static ExecutableElement findDefaultConstructor(
            @NonNull TypeElement clazz) throws ProcessingException {
        ExecutableElement constructor = clazz.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(element -> (ExecutableElement) element)
                .filter(ctor -> ctor.getParameters().isEmpty())
                .findFirst()
                .orElseThrow(() -> new ProcessingException(
                        "Serializer %s must have a zero-param constructor"
                                .formatted(clazz.getQualifiedName()),
                        clazz));
        if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ProcessingException(
                    "The zero-param constructor of serializer %s must not be private"
                            .formatted(clazz.getQualifiedName()),
                    constructor);
        }
        return constructor;
    }

    /**
     * Returns the {@code T deserialize(PropertyType)} method.
     *
     */
    private static ExecutableElement findDeserializeMethod(
            @NonNull TypeElement clazz, @NonNull Kind kind) {
        //noinspection OptionalGetWithoutIsPresent
        return clazz.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element)
                // The type-system enforces there is one method satisfying these constraints
                .filter(method -> method.getSimpleName().contentEquals("deserialize")
                        && !method.getModifiers().contains(Modifier.STATIC)
                        // Direct equality check with the param's type should be sufficient.
                        // Don't need to allow for subtypes because mActualTypeInGenericDoc can
                        // only be a primitive type or String which is a final class.
                        && hasSingleParamOfExactType(method, kind.mActualTypeInGenericDoc))
                .findFirst()
                // Should never throw because param type is enforced by the type-system
                .get();
    }

    private static boolean hasSingleParamOfExactType(
            @NonNull ExecutableElement method, @NonNull TypeName expectedType) {
        if (method.getParameters().size() != 1) {
            return false;
        }
        TypeName firstParamType = TypeName.get(method.getParameters().get(0).asType());
        return firstParamType.equals(expectedType);
    }
}
