/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.versionedparcelable.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Processes annotations from VersionedParcelables.
 */
@SupportedAnnotationTypes({VersionedParcelProcessor.VERSIONED_PARCELIZE,
        VersionedParcelProcessor.PARCEL_FIELD,
        VersionedParcelProcessor.NON_PARCEL_FIELD})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class VersionedParcelProcessor extends AbstractProcessor {

    public static final String VERSIONED_PARCELIZE =
            "androidx.versionedparcelable.VersionedParcelize";
    public static final String PARCEL_FIELD = "androidx.versionedparcelable.ParcelField";
    public static final String NON_PARCEL_FIELD = "androidx.versionedparcelable.NonParcelField";

    public static final String GEN_SUFFIX = "Parcelizer";
    private static final String READ = "read";
    private static final String WRITE = "write";

    private Messager mMessager;
    private ProcessingEnvironment mEnv;
    private Map<Pattern, String> mMethodLookup = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mEnv = processingEnvironment;
        mMessager = processingEnvironment.getMessager();
        mMethodLookup.put(Pattern.compile("^boolean$"), "Boolean");
        mMethodLookup.put(Pattern.compile("^int$"), "Int");
        mMethodLookup.put(Pattern.compile("^long$"), "Long");
        mMethodLookup.put(Pattern.compile("^float$"), "Float");
        mMethodLookup.put(Pattern.compile("^double$"), "Double");
        mMethodLookup.put(Pattern.compile("^java.lang.String$"), "String");
        mMethodLookup.put(Pattern.compile("^android.os.IBinder$"), "StrongBinder");
        mMethodLookup.put(Pattern.compile("^byte\\[\\]$"), "ByteArray");
        mMethodLookup.put(Pattern.compile("^android.os.Bundle$"), "Bundle");
        mMethodLookup.put(Pattern.compile("^android.os.PersistableBundle$"), "PersistableBundle");
        mMethodLookup.put(Pattern.compile("^boolean\\[\\]$"), "BooleanArray");
        mMethodLookup.put(Pattern.compile("^char\\[\\]$"), "CharArray");
        mMethodLookup.put(Pattern.compile("^int\\[\\]$"), "IntArray");
        mMethodLookup.put(Pattern.compile("^long\\[\\]$"), "LongArray");
        mMethodLookup.put(Pattern.compile("^float\\[\\]$"), "FloatArray");
        mMethodLookup.put(Pattern.compile("^double\\[\\]$"), "DoubleArray");
        mMethodLookup.put(Pattern.compile("^java.lang.Exception$"), "Exception");
        mMethodLookup.put(Pattern.compile("^byte$"), "Byte");
        mMethodLookup.put(Pattern.compile("^android.util.Size$"), "Size");
        mMethodLookup.put(Pattern.compile("^android.util.SizeF$"), "SizeF");
        mMethodLookup.put(Pattern.compile("^android.util.SparseBooleanArray$"),
                "SparseBooleanArray");
        mMethodLookup.put(Pattern.compile("^android.os.Parcelable$"), "Parcelable");
        mMethodLookup.put(Pattern.compile("^java.util.List<.*>$"), "List");
        mMethodLookup.put(Pattern.compile("^androidx.versionedparcelable.VersionedParcelable$"),
                "VersionedParcelable");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) return true;
        TypeElement cls = findAnnotation(set, VERSIONED_PARCELIZE);
        TypeElement field = findAnnotation(set, PARCEL_FIELD);
        TypeElement nonField = findAnnotation(set, NON_PARCEL_FIELD);
        List<Element> versionedParcelables = new ArrayList<>();
        Map<Element, Set<Element>> fields = new HashMap<>();
        Set<Element> nonFields = new HashSet<>();

        if (cls == null) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "Can't find class annotation");
            return true;
        }
        if (field == null) {
            error("Can't find field annotation, no fields?");
            return true;
        }
        for (Element element : roundEnvironment.getElementsAnnotatedWith(cls)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(cls + " can only be applied to class.");
                return true;
            }
            versionedParcelables.add(element);
        }
        for (Element element : roundEnvironment.getElementsAnnotatedWith(field)) {
            if (element.getKind() != ElementKind.FIELD) {
                error(field + " can only be applied to field.");
                return true;
            }
            Element clsElement = findClass(element);
            if (!versionedParcelables.contains(clsElement)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR,
                        cls + " must be added to classes containing " + field);
            } else {
                if (!fields.containsKey(clsElement)) {
                    fields.put(clsElement, new HashSet<>());
                }
                fields.get(clsElement).add(element);
            }
        }
        if (nonField != null) {
            for (Element element : roundEnvironment.getElementsAnnotatedWith(nonField)) {
                if (element.getKind() != ElementKind.FIELD) {
                    error(nonField + " can only be applied to field.");
                    return true;
                }
                Element clsElement = findClass(element);
                if (!versionedParcelables.contains(clsElement)) {
                    error(cls + " must be added to classes containing " + nonField);
                } else {
                    nonFields.add(element);
                }
            }
        }
        if (versionedParcelables.isEmpty()) {
            error("No VersionedParcels found");
            return true;
        }
        for (Element versionedParcelable : versionedParcelables) {
            ArrayList<String> takenIds = new ArrayList<>();
            AnnotationMirror annotation = findAnnotationMirror(
                    versionedParcelable.getAnnotationMirrors(), VERSIONED_PARCELIZE);
            String allowSerialization = getValue(annotation, "allowSerialization", "false");
            String ignoreParcelables = getValue(annotation, "ignoreParcelables", "false");
            String isCustom = getValue(annotation, "isCustom", "false");
            String deprecatedIds = getValue(annotation, "deprecatedIds", "");
            parseDeprecated(takenIds, deprecatedIds);
            checkClass(versionedParcelable.asType().toString(), versionedParcelable, takenIds);
            generateSerialization(versionedParcelable, fields.get(versionedParcelable),
                    allowSerialization, ignoreParcelables, isCustom);
        }

        return true;
    }

    private void parseDeprecated(ArrayList<String> takenIds, String deprecatedIds) {
        deprecatedIds = deprecatedIds.replace("{", "").replace("}", "");
        String[] ids = deprecatedIds.split(",");
        for (String id : ids) {
            takenIds.add(id.trim());
        }
    }

    private void generateSerialization(Element versionedParcelable, Set<Element> fields,
            String allowSerialization, String ignoreParcelables, String isCustom) {
        boolean custom = "true".equals(isCustom);
        AnnotationSpec restrictTo = AnnotationSpec.builder(
                ClassName.get("androidx.annotation", "RestrictTo"))
                .addMember("value", "androidx.annotation.RestrictTo.Scope.LIBRARY").build();
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(versionedParcelable.getSimpleName() + GEN_SUFFIX)
                .addJavadoc("@hide")
                .addAnnotation(restrictTo)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        Set<VariableElement> parcelFields = new HashSet<>();
        findFields(fields, parcelFields);

        MethodSpec.Builder readBuilder = MethodSpec
                .methodBuilder(READ)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(versionedParcelable.asType()), "obj")
                .addParameter(ClassName.get("androidx.versionedparcelable", "VersionedParcel"),
                        "parcel");

        MethodSpec.Builder writeBuilder = MethodSpec
                .methodBuilder(WRITE)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(versionedParcelable.asType()), "obj")
                .addParameter(ClassName.get("androidx.versionedparcelable", "VersionedParcel"),
                        "parcel")
                .addStatement("parcel.setSerializationFlags($L, $L)", allowSerialization,
                        ignoreParcelables);
        if (custom) {
            writeBuilder.addStatement("obj.onPreParceling(parcel.isStream())");
        }
        for (VariableElement e : parcelFields) {
            String id = getValue(e);
            String method = getMethod(e);
            readBuilder.addStatement("obj.$L = parcel.$L(obj.$L, $L)", e.getSimpleName(),
                    "read" + method, e.getSimpleName(), id);
            writeBuilder.addStatement("parcel.$L(obj.$L, $L)", "write" + method,
                    e.getSimpleName(), id);
        }
        if (custom) {
            readBuilder.addStatement("obj.onPostParceling()");
        }
        genClass.addMethod(readBuilder.build());
        genClass.addMethod(writeBuilder.build());
        try {
            JavaFile.builder(getPkg(versionedParcelable),
                    genClass.build()).build().writeTo(mEnv.getFiler());
        } catch (IOException e) {
            error("Exception writing " + e);
        }
    }

    private String getPkg(Element s) {
        String pkg = mEnv.getElementUtils().getPackageOf(s).toString();
        return pkg;
    }

    private String getMethod(VariableElement e) {
        TypeMirror type = e.asType();
        String m = getMethod(type);
        if (m != null) return m;
        TypeElement te = (TypeElement) mEnv.getTypeUtils().asElement(type);
        if (te != null) {
            for (TypeMirror t : te.getInterfaces()) {
                m = getMethod(t);
                if (m != null) return m;
            }
        }
        // Manual handling for generic arrays to go last.
        if (type.toString().contains("[]")) {
            return "Array";
        }
        mMessager.printMessage(Diagnostic.Kind.ERROR, "Can't find type for " + e);
        return null;
    }

    private String getMethod(TypeMirror typeMirror) {
        for (Pattern p : mMethodLookup.keySet()) {
            if (p.matcher(typeMirror.toString()).find()) {
                return mMethodLookup.get(p);
            }
        }
        return null;
    }

    private void findFields(Collection<? extends Element> fields,
            Set<VariableElement> parcelFields) {
        for (Element element : fields) {
            if (element.getKind() == ElementKind.FIELD) {
                if (!element.getModifiers().contains(Modifier.STATIC)) {
                    if (fields.contains(element)) {
                        parcelFields.add((VariableElement) element);
                    }
                }
            } else {
                findFields(element.getEnclosedElements(), parcelFields);
            }
        }
    }

    private void checkClass(String clsName, Element element, ArrayList<String> takenIds) {
        if (element.getKind() == ElementKind.FIELD) {
            if (!element.getModifiers().contains(Modifier.STATIC)) {
                int i;
                List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
                for (i = 0; i < annotations.size(); i++) {
                    AnnotationMirror annotation = annotations.get(i);
                    if (annotation.getAnnotationType().toString().equals(PARCEL_FIELD)) {
                        String valStr = getValue(annotation, "value", null);
                        if (valStr == null) {
                            return;
                        }
                        if (takenIds.contains(valStr)) {
                            error("Id " + valStr + " already taken on " + element);
                            return;
                        }
                        takenIds.add(valStr);
                        break;
                    }
                    if (annotation.getAnnotationType().toString().equals(NON_PARCEL_FIELD)) {
                        break;
                    }
                }
                if (i == annotations.size()) {
                    mMessager.printMessage(Diagnostic.Kind.ERROR,
                            clsName + "." + element.getSimpleName() + " is not annotated with "
                                    + "@ParcelField or @NonParcelField");
                    return;
                }
            }
        }
        for (Element e : element.getEnclosedElements()) {
            if (e.getKind() != ElementKind.CLASS) {
                checkClass(clsName, e, takenIds);
            }
        }
    }

    private String getValue(Element e) {
        List<? extends AnnotationMirror> annotations = e.getAnnotationMirrors();
        for (int i = 0; i < annotations.size(); i++) {
            AnnotationMirror annotation = annotations.get(i);
            if (annotation.getAnnotationType().toString().equals(PARCEL_FIELD)) {
                return getValue(annotation, "value", null);
            }
        }
        return null;
    }

    private String getValue(AnnotationMirror annotation, String name, String defValue) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
                annotation.getElementValues();
        for (ExecutableElement av : elementValues.keySet()) {
            if (Objects.equals(av.getSimpleName().toString(), name)) {
                AnnotationValue v = elementValues.get(av);
                return v != null ? v.toString() : av.getDefaultValue().getValue().toString();
            }
        }
        if (defValue != null) {
            return defValue;
        }
        error("Can't find annotation value");
        return null;
    }

    private Element findClass(Element element) {
        if (element != null && element.getKind() != ElementKind.CLASS) {
            return findClass(element.getEnclosingElement());
        }
        return element;
    }

    private AnnotationMirror findAnnotationMirror(List<? extends AnnotationMirror> set,
            String name) {
        for (AnnotationMirror annotation : set) {
            if (String.valueOf(annotation.getAnnotationType()).equals(name)) {
                return annotation;
            }
        }
        return null;
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> set, String name) {
        for (TypeElement typeElement : set) {
            if (String.valueOf(typeElement).equals(name)) {
                return typeElement;
            }
        }
        return null;
    }

    private void error(String error) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, error);
    }
}

