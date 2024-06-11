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

package androidx.remotecallback.compiler;

import static androidx.remotecallback.compiler.RemoteCallbackProcessor.EXTERNAL_INPUT;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 *
 */
class CallableMethod {

    private static final String BYTE = "byte";
    private static final String CHAR = "char";
    private static final String SHORT = "short";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOLEAN = "boolean";

    private static final String BYTE_ARRAY = "byte[]";
    private static final String CHAR_ARRAY = "char[]";
    private static final String SHORT_ARRAY = "short[]";
    private static final String INT_ARRAY = "int[]";
    private static final String LONG_ARRAY = "long[]";
    private static final String FLOAT_ARRAY = "float[]";
    private static final String DOUBLE_ARRAY = "double[]";
    private static final String BOOLEAN_ARRAY = "boolean[]";
    private static final String STRING_ARRAY = "java.lang.String[]";

    private static final String CONTEXT = "android.content.Context";
    private static final String STRING = "java.lang.String";
    private static final String URI = "android.net.Uri";

    private static final String OBJ_BYTE = "java.lang.Byte";
    private static final String CHARACTER = "java.lang.Character";
    private static final String OBJ_SHORT = "java.lang.Short";
    private static final String INTEGER = "java.lang.Integer";
    private static final String OBJ_LONG = "java.lang.Long";
    private static final String OBJ_FLOAT = "java.lang.Float";
    private static final String OBJ_DOUBLE = "java.lang.Double";
    private static final String OBJ_BOOLEAN = "java.lang.Boolean";

    private final Element mElement;
    private final ArrayList<TypeMirror> mTypes = new ArrayList<>();
    private final ArrayList<String> mNames = new ArrayList<>();
    private final ArrayList<String> mExtInputKeys = new ArrayList<>();
    private final String mClsName;
    private final ProcessingEnvironment mEnv;
    private TypeMirror mReturnType;

    CallableMethod(String name, Element element, ProcessingEnvironment env) {
        mClsName = name;
        mElement = element;
        mEnv = env;
        init();
    }

    /**
     * Get the name of the method this class is representing/tracking.
     */
    String getName() {
        return mElement.getSimpleName().toString();
    }

    private void init() {
        ExecutableType type = (ExecutableType) mElement.asType();
        ExecutableElement element = (ExecutableElement) mElement;
        List<? extends TypeMirror> types = type.getParameterTypes();
        List<? extends VariableElement> vars = element.getParameters();
        mReturnType = element.getReturnType();
        for (int i = 0; i < types.size(); i++) {
            mTypes.add(types.get(i));
            AnnotationMirror mirror = findAnnotation(vars.get(i), EXTERNAL_INPUT);
            mExtInputKeys.add(mirror != null ? getValue(mirror, "value", null) : null);
            mNames.add(vars.get(i).getSimpleName().toString());
        }
    }

    private AnnotationMirror findAnnotation(VariableElement element, String cls) {
        for (AnnotationMirror mirror: element.getAnnotationMirrors()) {
            if (typeString(mirror.getAnnotationType()).equals(cls)) {
                return mirror;
            }
        }
        return null;
    }

    private String getValue(AnnotationMirror annotation, String name, String defValue) {
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
        mEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't find annotation value");
        return null;
    }

    /**
     * Generate code and add the methods to the specified class/method.
     */
    void addMethods(TypeSpec.Builder genClass, MethodSpec.Builder runBuilder,
            ProcessingEnvironment env, Messager messager) {
        // Validate types
        for (int i = 0; i < mTypes.size(); i++) {
            if (checkType(typeString(mTypes.get(i)), messager)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Invalid type " + mTypes.get(i));
                return;
            }
        }
        if (!"androidx.remotecallback.RemoteCallback".equals(typeString(mReturnType))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "RemoteCallable methods must return RemoteCallback.LOCAL.");
            return;
        }

        ClassName callbackHandlerRegistry = ClassName.get("androidx.remotecallback",
                "CallbackHandlerRegistry");
        ClassName callbackHandler = ClassName.get("androidx.remotecallback",
                "CallbackHandlerRegistry.CallbackHandler");
        @SuppressWarnings("unused")
        ClassName remoteInputHolder = ClassName.get("androidx.remotecallback",
                "RemoteInputHolder");
        ClassName bundle = ClassName.get("android.os", "Bundle");
        ClassName context = ClassName.get("android.content", "Context");
        CodeBlock.Builder code = CodeBlock.builder();

        String methodName = mElement.getSimpleName().toString();
        code.add("$L.registerCallbackHandler($L.class, $S, ", callbackHandlerRegistry, mClsName,
                methodName);
        code.beginControlFlow("new $L<$L>()", callbackHandler, mClsName);

        // Begin executeCallback implementation ------------------------------------------------
        code.beginControlFlow("  public void executeCallback($L context, $L receiver, $L args)",
                context, mClsName, bundle);
        StringBuilder methodCall = new StringBuilder();
        methodCall.append("receiver.");
        methodCall.append(mElement.getSimpleName());
        methodCall.append("(");
        for (int i = 0; i < mNames.size(); i++) {
            TypeMirror type = mTypes.get(i);
            String typeString = typeString(type);
            // Pass the parameter to the method call.
            if (i != 0) {
                methodCall.append(", ");
            }
            methodCall.append("p" + i);

            if (typeString.equals(context.toString())) {
                code.addStatement("$L p" + i + " = context", mTypes.get(i));
                continue;
            }
            code.addStatement("$L p" + i, type);
            String key = mExtInputKeys.get(i) != null ? mExtInputKeys.get(i) : getBundleKey(i);
            // Generate code to extract the value.
            code.addStatement("p$L = $L", i, getBundleParam(typeString, key));
        }
        methodCall.append(")");
        // Add the method call as the last thing.
        code.addStatement(methodCall.toString());
        code.endControlFlow();
        // End executeCallback implementation --------------------------------------------------

        code.endControlFlow();
        code.add(");\n");
        runBuilder.addCode(code.build());

        // Start assembleArguments implementation ----------------------------------------------
        code = CodeBlock.builder();
        ClassName remoteCallback = ClassName.get("androidx.remotecallback", "RemoteCallback");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .returns(remoteCallback)
                .addAnnotation(AnnotationSpec.builder(ClassName.bestGuess("Override")).build())
                .addModifiers(Modifier.PUBLIC);

        code.addStatement("$L b = new $L()", bundle, bundle);
        for (int i = 0; i < mNames.size(); i++) {
            TypeMirror type = mTypes.get(i);
            String typeString = typeString(type);
            builder.addParameter(TypeName.get(type), "p" + i);
            if (typeString.equals(context.toString())) {
                continue;
            }
            boolean isNative = isNative(typeString);
            // Only fill in value if the argument has a value.
            if (!isNative) code.beginControlFlow("if (p$L != null)", i);

            // Otherwise just need to place the arg value.
            code.addStatement("b.put$L($L, ($L) p$L)",
                    getTypeMethod(typeString),
                    getBundleKey(i), type, i);

            // No value present, need an explicit null for security.
            if (!isNative) code.nextControlFlow("else");
            if (!isNative) code.addStatement("b.putString($L, null)", getBundleKey(i));
            if (!isNative) code.endControlFlow();
        }
        code.addStatement(
                "return androidx.remotecallback.CallbackHandlerRegistry.stubToRemoteCallback("
                        + "this, $L.class, b, $S)",
                mClsName, mElement.getSimpleName());
        builder.addCode(code.build());

        genClass.addMethod(builder.build());
    }

    @SuppressWarnings("unused")
    private int countArgs(ClassName context) {
        int ct = 0;
        for (int i = 0; i < mTypes.size(); i++) {
            if (typeString(mTypes.get(i)).equals(context.toString())) {
                continue;
            }
            ct++;
        }
        return ct;
    }

    @SuppressWarnings("unused")
    private String getBundleParam(String type, int index) {
        String key = getBundleKey(index);
        return getBundleParam(type, key);
    }

    private boolean isNative(String type) {
        switch (type) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                return true;
        }
        return false;
    }

    private String getBundleParam(String type, String key) {
        switch (type) {
            case BYTE:
                return "args.getByte(" + key + ", (byte) 0)";
            case CHAR:
                return "args.getChar(" + key + ", (char) 0)";
            case SHORT:
                return "args.getShort(" + key + ", (short) 0)";
            case INT:
                return "args.getInt(" + key + ", 0)";
            case LONG:
                return "args.getLong(" + key + ", 0)";
            case FLOAT:
                return "args.getFloat(" + key + ", 0f)";
            case DOUBLE:
                return "args.getDouble(" + key + ", 0.0)";
            case BOOLEAN:
                return "args.getBoolean(" + key + ", false)";
        }
        return "(" + type + ") args.get(" + key + ")";
    }

    private String getTypeMethod(String type) {
        switch (type) {
            case BYTE:
                return "Byte";
            case CHAR:
                return "Char";
            case SHORT:
                return "Short";
            case INT:
                return "Int";
            case LONG:
                return "Long";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case BOOLEAN:
                return "Boolean";
            case STRING:
                return "String";
            case URI:
                return "Parcelable";
            case BYTE_ARRAY:
                return "ByteArray";
            case CHAR_ARRAY:
                return "CharArray";
            case SHORT_ARRAY:
                return "ShortArray";
            case INT_ARRAY:
                return "IntArray";
            case LONG_ARRAY:
                return "LongArray";
            case FLOAT_ARRAY:
                return "FloatArray";
            case DOUBLE_ARRAY:
                return "DoubleArray";
            case BOOLEAN_ARRAY:
                return "BooleanArray";
            case STRING_ARRAY:
                return "StringArray";
            case OBJ_BYTE:
                return "Byte";
            case CHARACTER:
                return "Char";
            case OBJ_SHORT:
                return "Short";
            case INTEGER:
                return "Int";
            case OBJ_LONG:
                return "Long";
            case OBJ_FLOAT:
                return "Float";
            case OBJ_DOUBLE:
                return "Double";
            case OBJ_BOOLEAN:
                return "Boolean";
        }
        throw new RuntimeException("Invalid type " + type);
    }

    private String getBundleKey(int index) {
        return "\"p" + index + "\"";
    }

    @SuppressWarnings("unused")
    private boolean checkType(String type, Messager messager) {
        switch (type) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case CONTEXT:
            case BYTE_ARRAY:
            case CHAR_ARRAY:
            case SHORT_ARRAY:
            case INT_ARRAY:
            case LONG_ARRAY:
            case FLOAT_ARRAY:
            case DOUBLE_ARRAY:
            case BOOLEAN_ARRAY:
            case STRING_ARRAY:
            case URI:
            case OBJ_BYTE:
            case CHARACTER:
            case OBJ_SHORT:
            case INTEGER:
            case OBJ_LONG:
            case OBJ_FLOAT:
            case OBJ_DOUBLE:
            case OBJ_BOOLEAN:
                return false;
            default:
                return true;
        }
    }

    /** Returns a simple string version of the type, with no annotations. */
    private String typeString(TypeMirror type) {
        return TypeName.get(type).toString();
    }
}
