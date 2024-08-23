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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
@SupportedAnnotationTypes({
        VersionedParcelProcessor.VERSIONED_PARCELIZE,
        VersionedParcelProcessor.PARCEL_FIELD,
        VersionedParcelProcessor.NON_PARCEL_FIELD
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class VersionedParcelProcessor extends AbstractProcessor {

    static final String VERSIONED_PARCELIZE = "androidx.versionedparcelable.VersionedParcelize";
    static final String PARCEL_FIELD = "androidx.versionedparcelable.ParcelField";
    static final String NON_PARCEL_FIELD = "androidx.versionedparcelable.NonParcelField";

    private static final ClassName RESTRICT_TO = ClassName.get("androidx.annotation", "RestrictTo");
    private static final ClassName RESTRICT_TO_SCOPE = RESTRICT_TO.nestedClass("Scope");
    private static final ClassName VERSIONED_PARCEL =
            ClassName.get("androidx.versionedparcelable", "VersionedParcel");

    private static final String GEN_SUFFIX = "Parcelizer";
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
        mMethodLookup.put(Pattern.compile("^java.lang.CharSequence$"), "CharSequence");
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
        mMethodLookup.put(Pattern.compile("^java.util.Set<.*>$"), "Set");
        mMethodLookup.put(Pattern.compile("^java.util.Map<.*>$"), "Map");
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
        Map<String, Set<Element>> fields = new HashMap<>();

        if (cls == null) {
            error("Can't find class annotation");
            return true;
        }
        if (field == null) {
            error("Can't find field annotation, no fields?");
            return true;
        }
        for (Element element: roundEnvironment.getElementsAnnotatedWith(cls)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(cls + " can only be applied to class.");
                return true;
            }
            versionedParcelables.add(element);
        }
        for (Element element: roundEnvironment.getElementsAnnotatedWith(field)) {
            if (element.getKind() != ElementKind.FIELD) {
                error(field + " can only be applied to field.");
                return true;
            }
            Element clsElement = findClass(element);
            if (!versionedParcelables.contains(clsElement)) {
                error(cls + " must be added to classes containing " + field);
            } else {
                fields.computeIfAbsent(clsElement.toString(), (s) -> new HashSet<Element>())
                        .add(element);
            }
        }
        if (nonField != null) {
            for (Element element: roundEnvironment.getElementsAnnotatedWith(nonField)) {
                if (element.getKind() != ElementKind.FIELD) {
                    error(nonField + " can only be applied to field.");
                    return true;
                }
                Element clsElement = findClass(element);
                if (!versionedParcelables.contains(clsElement)) {
                    error(cls + " must be added to classes containing " + nonField);
                }
            }
        }
        if (versionedParcelables.isEmpty()) {
            error("No VersionedParcels found");
            return true;
        }
        for (Element versionedParcelable: versionedParcelables) {
            ArrayList<String> takenIds = new ArrayList<>();
            AnnotationMirror annotation = findAnnotationMirror(
                    versionedParcelable.getAnnotationMirrors(), VERSIONED_PARCELIZE);
            String allowSerialization = getValue(annotation, "allowSerialization", "false");
            String ignoreParcelables = getValue(annotation, "ignoreParcelables", "false");
            String isCustom = getValue(annotation, "isCustom", "false");
            String deprecatedIds = getValue(annotation, "deprecatedIds", "");
            String jetifyAs = getValue(annotation, "jetifyAs", "");
            String factoryClass = getValue(annotation, "factory", "");
            parseDeprecated(takenIds, deprecatedIds);
            checkClass(typeString(versionedParcelable.asType()), versionedParcelable, takenIds);

            ArrayList<Element> f = new ArrayList<>();
            TypeElement te = (TypeElement) mEnv.getTypeUtils().asElement(
                    versionedParcelable.asType());
            while (te != null) {
                Set<Element> collection = fields.get(te.getQualifiedName().toString());
                if (collection != null) {
                    f.addAll(collection);
                }
                te = (TypeElement) mEnv.getTypeUtils().asElement(te.getSuperclass());
            }
            generateSerialization(versionedParcelable, f,
                    allowSerialization, ignoreParcelables, isCustom, jetifyAs, factoryClass);
        }

        return true;
    }

    @SuppressWarnings("StringSplitter")
    private void parseDeprecated(ArrayList<String> takenIds, String deprecatedIds) {
        deprecatedIds = deprecatedIds.replace("{", "").replace("}", "");
        String[] ids = deprecatedIds.split(",");
        for (String id: ids) {
            takenIds.add(id.trim());
        }
    }

    private void generateSerialization(Element versionedParcelable, List<Element> fields,
            String allowSerialization, String ignoreParcelables, String isCustom,
            String jetifyAs, String factoryClass) {
        boolean custom = "true".equals(isCustom);
        AnnotationSpec restrictTo = AnnotationSpec.builder(RESTRICT_TO)
                .addMember("value", "$T.LIBRARY", RESTRICT_TO_SCOPE)
                .build();
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(versionedParcelable.getSimpleName() + GEN_SUFFIX)
                .addOriginatingElement(versionedParcelable)
                .addAnnotation(restrictTo)
                .addModifiers(Modifier.PUBLIC);
        if (jetifyAs == null || jetifyAs.length() == 0) {
            genClass.addModifiers(Modifier.FINAL);
        }

        ArrayList<VariableElement> parcelFields = new ArrayList<>();
        findFields(fields, parcelFields);

        TypeName type = ClassName.get((TypeElement) versionedParcelable);
        AnnotationSpec suppressUncheckedWarning = AnnotationSpec.builder(
                ClassName.get("java.lang", "SuppressWarnings"))
                .addMember("value", "$S", "unchecked")
                .build();
        MethodSpec.Builder readBuilder = MethodSpec
                .methodBuilder(READ)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(suppressUncheckedWarning)
                .returns(type)
                .addParameter(VERSIONED_PARCEL, "parcel");
        if (factoryClass != null && factoryClass.length() != 0) {
            // Strip the .class
            factoryClass = factoryClass.substring(0, factoryClass.lastIndexOf('.'));
            ClassName cls = ClassName.bestGuess(factoryClass);
            genClass.addField(FieldSpec.builder(cls, "sBuilder")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .initializer("new $T()", cls)
                    .build());
            readBuilder.addStatement("$T obj = sBuilder.get()", type);
        } else {
            readBuilder.addStatement("$1T obj = new $1T()", type);
        }

        MethodSpec.Builder writeBuilder = MethodSpec
                .methodBuilder(WRITE)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(suppressUncheckedWarning)
                .addParameter(type, "obj")
                .addParameter(VERSIONED_PARCEL, "parcel")
                .addStatement("parcel.setSerializationFlags($L, $L)", allowSerialization,
                        ignoreParcelables);
        if (custom) {
            writeBuilder.addStatement("obj.onPreParceling(parcel.isStream())");
        }
        parcelFields.sort(Comparator.comparing(e -> getValue(getAnnotation(e), "value", null)));
        for (VariableElement e: parcelFields) {
            genClass.addOriginatingElement(e);
            AnnotationMirror annotation = getAnnotation(e);
            String id = getValue(annotation, "value", null);
            String defaultValue = getValue(annotation, "defaultValue", null, false);
            String method = getMethod(e);
            readBuilder.addStatement("obj.$L = parcel.$L(obj.$L, $L)", e.getSimpleName(),
                    "read" + method, e.getSimpleName(), id);

            if (defaultValue != null && defaultValue.length() != 0) {
                if (defaultValue.equals("\"null\"")) {
                    writeBuilder.beginControlFlow("if (obj.$L != null)", e.getSimpleName());
                } else {
                    if (isNative(e)) {
                        writeBuilder.beginControlFlow("if ($L != obj.$L)", strip(defaultValue),
                                e.getSimpleName());
                    } else if (isArray(e)) {
                        writeBuilder.beginControlFlow("if (!$T.equals($L, obj.$L))",
                                Arrays.class, strip(defaultValue), e.getSimpleName());
                    } else {
                        String v = "java.lang.String".equals(typeString(e.asType())) ? defaultValue
                                : strip(defaultValue);
                        writeBuilder.beginControlFlow("if (!$L.equals(obj.$L))",
                                v, e.getSimpleName());
                    }
                }
            }
            writeBuilder.addStatement("parcel.$L(obj.$L, $L)", "write" + method,
                    e.getSimpleName(), id);
            if (defaultValue != null && defaultValue.length() != 0) {
                writeBuilder.endControlFlow();
            }
        }

        if (custom) {
            readBuilder.addStatement("obj.onPostParceling()");
        }
        readBuilder.addStatement("return obj");
        genClass.addMethod(readBuilder.build());
        genClass.addMethod(writeBuilder.build());
        try {
            TypeSpec typeSpec = genClass.build();
            String pkg = getPkg(versionedParcelable);
            JavaFile.builder(pkg,
                    typeSpec).build().writeTo(mEnv.getFiler());
            if (jetifyAs != null && jetifyAs.length() > 0) {
                int index = jetifyAs.lastIndexOf('.');
                String jetPkg = jetifyAs.substring(1, index);
                String superCls = pkg + "." + versionedParcelable.getSimpleName() + GEN_SUFFIX;
                TypeSpec.Builder jetifyClass = TypeSpec
                        .classBuilder(jetifyAs.substring(index + 1, jetifyAs.length() - 1)
                                + GEN_SUFFIX)
                        .addAnnotation(restrictTo)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        // The empty package here is a hack to avoid an import,
                        // since the classes have the same name.
                        .superclass(ClassName.get("", superCls));
                jetifyClass.addMethod(MethodSpec
                        .methodBuilder(READ)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(type)
                        .addParameter(VERSIONED_PARCEL, "parcel")
                        .addStatement("return $L.read(parcel)", superCls)
                        .build());
                jetifyClass.addMethod(MethodSpec
                        .methodBuilder(WRITE)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(type, "obj")
                        .addParameter(VERSIONED_PARCEL, "parcel")
                        .addStatement("$L.write(obj, parcel)", superCls)
                        .build());
                TypeSpec jetified = jetifyClass.build();
                JavaFile.builder(jetPkg, jetified).build().writeTo(mEnv.getFiler());
            }
        } catch (IOException e) {
            error("Exception writing " + e);
        }
    }

    private String strip(String s) {
        if (!s.startsWith("\"")) return s;
        return s.substring(1, s.length() - 1);
    }

    private String getPkg(Element s) {
        String pkg = mEnv.getElementUtils().getPackageOf(s).toString();
        return pkg;
    }

    /** Returns a simple string version of the type, with no annotations. */
    private String typeString(TypeMirror type) {
        return TypeName.get(type).toString();
    }

    private String getMethod(VariableElement e) {
        TypeMirror type = e.asType();
        String m = getMethod(type);
        if (m != null) return m;
        TypeElement te = (TypeElement) mEnv.getTypeUtils().asElement(type);
        while (te != null) {
            for (TypeMirror t: te.getInterfaces()) {
                m = getMethod(t);
                if (m != null) return m;
            }
            te = te.getSuperclass() != null ? (TypeElement) mEnv.getTypeUtils()
                    .asElement(te.getSuperclass()) : null;
        }
        // Manual handling for generic arrays to go last.
        if (typeString(type).contains("[]")) {
            return "Array";
        }
        error("Can't find type for " + e + " (type: " + type + ")");
        return null;
    }

    private boolean isArray(VariableElement e) {
        return typeString(e.asType()).endsWith("[]");
    }

    private boolean isNative(VariableElement e) {
        String type = typeString(e.asType());
        return "int".equals(type)
                || "byte".equals(type)
                || "char".equals(type)
                || "long".equals(type)
                || "double".equals(type)
                || "float".equals(type)
                || "boolean".equals(type);
    }

    private String getMethod(TypeMirror typeMirror) {
        // Get an annotation-free version of the type string through TypeName
        String typeString = typeString(typeMirror);
        for (Pattern p: mMethodLookup.keySet()) {
            if (p.matcher(typeString).find()) {
                return mMethodLookup.get(p);
            }
        }
        return null;
    }

    private void findFields(Collection<? extends Element> fields,
            ArrayList<VariableElement> parcelFields) {
        for (Element element: fields) {
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
                    if (typeString(annotation.getAnnotationType()).equals(PARCEL_FIELD)) {
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
                    if (typeString(annotation.getAnnotationType()).equals(NON_PARCEL_FIELD)) {
                        break;
                    }
                }
                if (i == annotations.size()) {
                    error(clsName + "." + element.getSimpleName() + " is not annotated with "
                                    + "@ParcelField or @NonParcelField");
                    return;
                }
            }
        } else if (element.getKind() == ElementKind.CLASS) {
            TypeElement te = (TypeElement) mEnv.getTypeUtils().asElement(
                    element.asType());
            if (te != null && te.getSuperclass() != null) {
                Element e = (TypeElement) mEnv.getTypeUtils().asElement(te.getSuperclass());
                if (e != null) {
                    checkClass(clsName, e, takenIds);
                }
            }
        }
        for (Element e: element.getEnclosedElements()) {
            if (e.getKind() != ElementKind.CLASS) {
                checkClass(clsName, e, takenIds);
            }
        }
    }

    private AnnotationMirror getAnnotation(Element e) {
        List<? extends AnnotationMirror> annotations = e.getAnnotationMirrors();
        for (int i = 0; i < annotations.size(); i++) {
            AnnotationMirror annotation = annotations.get(i);
            if (typeString(annotation.getAnnotationType()).equals(PARCEL_FIELD)) {
                return annotation;
            }
        }
        return null;
    }

    private String getValue(AnnotationMirror annotation, String name, String defValue) {
        return getValue(annotation, name, defValue, true);
    }

    private String getValue(AnnotationMirror annotation, String name, String defValue,
            boolean required) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
                annotation.getElementValues();
        for (ExecutableElement av: elementValues.keySet()) {
            if (Objects.equals(av.getSimpleName().toString(), name)) {
                AnnotationValue v = elementValues.get(av);
                return v != null ? v.toString() : av.getDefaultValue().getValue().toString();
            }
        }
        if (defValue != null) {
            return defValue;
        }
        if (required) {
            error("Can't find annotation value");
        }
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
        for (AnnotationMirror annotation: set) {
            if (String.valueOf(annotation.getAnnotationType()).equals(name)) {
                return annotation;
            }
        }
        return null;
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> set, String name) {
        for (TypeElement typeElement: set) {
            if (String.valueOf(typeElement).equals(name)) {
                return typeElement;
            }
        }
        return null;
    }

    private void error(String error) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, "VersionedParcelProcessor - " + error);
    }
}

