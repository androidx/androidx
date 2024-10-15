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

package androidx.car.app.serialization;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.utils.LogTags.TAG_BUNDLER;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.CarProtocol;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to serialize and deserialize objects to/from {@link Bundle}s.
 *
 * <p>Supported object types:
 *
 * <li>{@link Boolean}
 * <li>{@link Byte}
 * <li>{@link Character}
 * <li>{@link Short}
 * <li>{@link Integer}
 * <li>{@link Long}
 * <li>{@link Double}
 * <li>{@link Float}
 * <li>{@link String}
 * <li>{@link Parcelable} - parcelables will serialize using {@link Parcelable} logic.
 * <li>{@link IInterface}
 * <li>{@link IBinder}
 * <li>{@link Map} - maps will be deserialize into {@link HashMap}s, and need to hold objects that
 * are also serializable as per this list.
 * <li>{@link List} - lists will deserialize into {@link ArrayList}s, and need to hold objects that
 * are also serializable as per this list.
 * <li>{@link Set} - sets will deserialize into {@link HashSet}s, and need to hold objects that are
 * also serializable as per this list.
 * <li>Custom objects that hold only other objects as per defined in this list as fields.
 *
 */
@RestrictTo(LIBRARY)
public final class Bundler {
    @VisibleForTesting
    static final String ICON_COMPAT_ANDROIDX = "androidx.core.graphics.drawable.IconCompat";

    @VisibleForTesting
    static final String ICON_COMPAT_SUPPORT = "android.support.v4.graphics.drawable.IconCompat";

    /** Whether to redact the values of the bundled fields in the logs. */
    private static final boolean REDACT_LOG_VALUES = true;

    private static final int MAX_VALUE_LOG_LENGTH = 32;

    private static final Map<Class<?>, String> UNOBFUSCATED_TYPE_NAMES =
            initUnobfuscatedTypeNames();
    private static final Map<Integer, String> BUNDLED_TYPE_NAMES = initBundledTypeNames();

    private static final String TAG_CLASS_NAME = "tag_class_name";
    private static final String TAG_CLASS_TYPE = "tag_class_type";
    private static final String TAG_VALUE = "tag_value";
    private static final String TAG_1 = "tag_1";
    private static final String TAG_2 = "tag_2";

    private static final int PRIMITIVE = 0;
    private static final int IINTERFACE = 1;
    private static final int MAP = 2;
    private static final int SET = 3;
    private static final int LIST = 4;
    private static final int OBJECT = 5;
    private static final int IMAGE = 6;
    private static final int ENUM = 7;
    private static final int CLASS = 8;
    private static final int IBINDER = 9;
    private static final int PERSON = 10;

    /**
     * Serializes an object into a {@link Bundle} for sending over IPC.
     *
     * <p>Do not use arrays of objects, use {@link Collection}s.
     *
     * <p>All objects to serialize <strong>MUST</strong> have a default constructor, even if it is
     * marked as {@code private}, in order to deserialize.
     *
     * @throws BundlerException if any exception is encountered attempting to bundle the object
     */
    @NonNull
    public static Bundle toBundle(@NonNull Object obj) throws BundlerException {
        String className = getUnobfuscatedClassName(obj.getClass());
        if (Log.isLoggable(TAG_BUNDLER, Log.DEBUG)) {
            Log.d(TAG_BUNDLER, "Bundling " + className);
        }
        return toBundle(obj, className, Trace.create());
    }

    @SuppressWarnings("unchecked")
    private static Bundle toBundle(@Nullable Object obj, String display, Trace parentTrace)
            throws BundlerException {
        if (obj != null && parentTrace.find(obj)) {
            throw new CycleDetectedBundlerException(
                    "Found cycle while bundling type " + obj.getClass().getSimpleName(),
                    parentTrace);
        }

        try (Trace trace = Trace.fromParent(obj, display, parentTrace)) {
            if (obj == null) {
                throw new TracedBundlerException("Bundling of null object is not supported", trace);
            } else if (obj instanceof IconCompat) {
                return serializeImage((IconCompat) obj);
            } else if (isPrimitiveType(obj) || obj instanceof Parcelable) {
                return serializePrimitive(obj, trace);
            } else if (obj instanceof IInterface) {
                return serializeIInterface((IInterface) obj);
            } else if (obj instanceof IBinder) {
                return serializeIBinder((IBinder) obj);
            } else if (obj instanceof Map) {
                return serializeMap((Map<Object, Object>) obj, trace);
            } else if (obj instanceof List) {
                return serializeList((List<Object>) obj, trace);
            } else if (obj instanceof Set) {
                return serializeSet((Set<Object>) obj, trace);
            } else if (obj.getClass().isEnum()) {
                return serializeEnum(obj, trace);
            } else if (obj instanceof Class) {
                return serializeClass((Class<?>) obj);
            } else if (obj.getClass().isArray()) {
                throw new TracedBundlerException(
                        "Object serializing contains an array, use a list or a set instead",
                        trace);
            } else if (obj instanceof Person) {
                return serializePerson((Person) obj);
            } else {
                return serializeObject(obj, trace);
            }
        }
    }

    /**
     * Deserializes a {@link Bundle} into the source object it was bundled from {@link
     * #toBundle(Object)}.
     *
     * <p>Possible versioning scenarios this de-serialization works with, and how it handles the
     * case:
     *
     * <li>De-serializing an object of an unknown class: will throw a {@link BundlerException}.
     * <li>De-serializing an object which sent an unknown field: will store all other fields and
     * ignore the unknown field.
     * <li>De-serializing an object which did not send a field: will store all known fields and
     * leave default value for field not sent.
     *
     * @throws BundlerException if any exception is encountered attempting to reconstruct the
     *                          object
     */
    @NonNull
    public static Object fromBundle(@NonNull Bundle bundle) throws BundlerException {
        if (Log.isLoggable(TAG_BUNDLER, Log.DEBUG)) {
            Log.d(TAG_BUNDLER, "Unbundling " + getBundledTypeName(bundle.getInt(TAG_CLASS_TYPE)));
        }
        return fromBundle(bundle, Trace.create());
    }

    @NonNull
    private static Object fromBundle(@NonNull Bundle bundle, Trace parentTrace)
            throws BundlerException {
        bundle.setClassLoader(requireNonNull(Bundler.class.getClassLoader()));

        int classType = bundle.getInt(TAG_CLASS_TYPE);

        try (Trace trace = Trace.fromParent(bundle, Trace.bundleToString(bundle), parentTrace)) {
            switch (classType) {
                case PRIMITIVE:
                    return deserializePrimitive(bundle, trace);
                case IINTERFACE:
                    return deserializeIInterface(bundle, trace);
                case IBINDER:
                    return deserializeIBinder(bundle, trace);
                case MAP:
                    return deserializeMap(bundle, trace);
                case SET:
                    return deserializeSet(bundle, trace);
                case LIST:
                    return deserializeList(bundle, trace);
                case IMAGE:
                    return deserializeImage(bundle, trace);
                case PERSON:
                    return deserializePerson(bundle);
                case OBJECT:
                    return deserializeObject(bundle, trace);
                case ENUM:
                    return deserializeEnum(bundle, trace);
                case CLASS:
                    return deserializeClass(bundle, trace);
                default: // fall out
            }

            throw new TracedBundlerException("Unsupported class type in bundle: " + classType,
                    trace);
        }
    }

    private static Bundle serializePrimitive(Object obj, Trace trace) throws BundlerException {
        Bundle bundle = new Bundle(2);

        bundle.putInt(TAG_CLASS_TYPE, PRIMITIVE);
        if (obj instanceof Boolean) {
            bundle.putBoolean(TAG_VALUE, (Boolean) obj);
        } else if (obj instanceof Byte) {
            bundle.putByte(TAG_VALUE, (Byte) obj);
        } else if (obj instanceof Character) {
            bundle.putChar(TAG_VALUE, (Character) obj);
        } else if (obj instanceof Short) {
            bundle.putShort(TAG_VALUE, (Short) obj);
        } else if (obj instanceof Integer) {
            bundle.putInt(TAG_VALUE, (Integer) obj);
        } else if (obj instanceof Long) {
            bundle.putLong(TAG_VALUE, (Long) obj);
        } else if (obj instanceof Double) {
            bundle.putDouble(TAG_VALUE, (Double) obj);
        } else if (obj instanceof Float) {
            bundle.putFloat(TAG_VALUE, (Float) obj);
        } else if (obj instanceof String) {
            bundle.putString(TAG_VALUE, (String) obj);
        } else if (obj instanceof Parcelable) {
            bundle.putParcelable(TAG_VALUE, (Parcelable) obj);
        } else {
            throw new TracedBundlerException(
                    "Unsupported primitive type: " + obj.getClass().getName(), trace);
        }

        return bundle;
    }

    private static Bundle serializeIInterface(IInterface iInterface) {
        Bundle bundle = new Bundle(3);

        String className = iInterface.getClass().getName();

        bundle.putInt(TAG_CLASS_TYPE, IINTERFACE);
        bundle.putBinder(TAG_VALUE, iInterface.asBinder());
        bundle.putString(TAG_CLASS_NAME, className);

        return bundle;
    }

    private static Bundle serializeIBinder(IBinder binder) {
        Bundle bundle = new Bundle(2);

        bundle.putInt(TAG_CLASS_TYPE, IBINDER);
        bundle.putBinder(TAG_VALUE, binder);

        return bundle;
    }

    private static Bundle serializeMap(Map<Object, Object> map, Trace trace)
            throws BundlerException {
        Bundle bundle = new Bundle(2);

        ArrayList<Bundle> list = new ArrayList<>();

        int i = 0;
        for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
            Bundle keyValue = new Bundle(2);
            keyValue.putBundle(TAG_1, toBundle(mapEntry.getKey(), "<key " + i + ">", trace));
            if (mapEntry.getValue() != null) {
                keyValue.putBundle(TAG_2,
                        toBundle(mapEntry.getValue(), "<value " + i + ">", trace));
            }
            i++;
            list.add(keyValue);
        }
        bundle.putInt(TAG_CLASS_TYPE, MAP);
        bundle.putParcelableArrayList(TAG_VALUE, list);

        return bundle;
    }

    private static Bundle serializeList(List<Object> list, Trace trace) throws BundlerException {
        Bundle bundle = serializeCollection(list, trace);
        bundle.putInt(TAG_CLASS_TYPE, LIST);
        return bundle;
    }

    private static Bundle serializeSet(Set<Object> set, Trace trace) throws BundlerException {
        Bundle bundle = serializeCollection(set, trace);
        bundle.putInt(TAG_CLASS_TYPE, SET);
        return bundle;
    }

    private static Bundle serializeCollection(Collection<Object> collection, Trace trace)
            throws BundlerException {
        Bundle bundle = new Bundle(2);
        ArrayList<Bundle> list = new ArrayList<>();

        int i = 0;
        for (Object entry : collection) {
            list.add(toBundle(entry, "<item " + i + ">", trace));
            i++;
        }
        bundle.putParcelableArrayList(TAG_VALUE, list);

        return bundle;
    }

    private static Bundle serializeEnum(Object obj, Trace trace) throws BundlerException {
        Bundle bundle = new Bundle(3);
        bundle.putInt(TAG_CLASS_TYPE, ENUM);

        Method nameMethod = getClassOrSuperclassMethod(obj.getClass(), "name", trace);
        String enumName;
        try {
            enumName = (String) nameMethod.invoke(obj);
        } catch (ReflectiveOperationException e) {
            // Should not happen since it always exists.
            throw new TracedBundlerException("Enum missing name method", trace, e);
        }

        bundle.putString(TAG_VALUE, enumName);
        bundle.putString(TAG_CLASS_NAME, obj.getClass().getName());
        return bundle;
    }

    private static Bundle serializeClass(Class<?> obj) {
        Bundle bundle = new Bundle(2);
        bundle.putInt(TAG_CLASS_TYPE, CLASS);
        bundle.putString(TAG_VALUE, obj.getName());
        return bundle;
    }

    private static Bundle serializeImage(IconCompat image) {
        Bundle bundle = new Bundle(2);
        bundle.putInt(TAG_CLASS_TYPE, IMAGE);
        bundle.putBundle(TAG_VALUE, image.toBundle());
        return bundle;
    }

    private static Bundle serializePerson(Person person) {
        Bundle bundle = person.toBundle();
        bundle.putInt(TAG_CLASS_TYPE, PERSON);
        return bundle;
    }

    private static Bundle serializeObject(Object obj, Trace trace) throws BundlerException {
        String className = obj.getClass().getName();
        if (!obj.getClass().isAnnotationPresent(CarProtocol.class)) {
            throw new TracedBundlerException(
                    "Invalid class not marked as CarProtocol: " + className, trace);
        }
        try {
            obj.getClass().getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new TracedBundlerException(
                    "Class to deserialize is missing a no args constructor: " + className, trace,
                    e);
        }
        List<Field> fields = getFields(obj.getClass());
        Bundle bundle = new Bundle(fields.size() + 2);

        bundle.putInt(TAG_CLASS_TYPE, OBJECT);
        bundle.putString(TAG_CLASS_NAME, className);
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = getFieldName(field);

            Object value = null;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                throw new TracedBundlerException("Field is not accessible: " + fieldName, trace, e);
            }

            if (value != null) {
                bundle.putParcelable(fieldName, toBundle(value, field.getName(), trace));
            }
        }

        return bundle;
    }

    @SuppressWarnings("deprecation")
    private static Object deserializePrimitive(Bundle bundle, Trace trace) throws BundlerException {
        Object primitive = bundle.get(TAG_VALUE);
        if (primitive == null) {
            throw new TracedBundlerException("Bundle is missing the primitive value", trace);
        }
        return primitive;
    }

    @SuppressWarnings("argument.type.incompatible") // so that we can invoke static asInterface
    private static Object deserializeIInterface(Bundle bundle, Trace trace)
            throws BundlerException {
        IBinder binder = bundle.getBinder(TAG_VALUE);
        if (binder == null) {
            throw new TracedBundlerException("Bundle is missing the binder", trace);
        }

        String interfaceClassName = bundle.getString(TAG_CLASS_NAME);
        if (interfaceClassName == null) {
            throw new TracedBundlerException("Bundle is missing IInterface class name", trace);
        }

        try {
            Class<?> clazz = Class.forName(interfaceClassName);
            Method converter = getClassOrSuperclassMethod(clazz, "asInterface", trace);

            // null obj because the asInterface is static.
            Object obj = converter.invoke(null, binder);
            if (obj == null) {
                throw new TracedBundlerException("Failed to get interface from binder", trace);
            }
            return obj;
        } catch (ClassNotFoundException e) {
            throw new TracedBundlerException(
                    "Binder for unknown IInterface: " + interfaceClassName, trace, e);
        } catch (ReflectiveOperationException e) {
            throw new TracedBundlerException(
                    // Should not happen since we set it as accessible.
                    "Method to create IInterface from a Binder is not accessible for interface: "
                            + interfaceClassName,
                    trace,
                    e);
        }
    }

    private static Object deserializeIBinder(Bundle bundle, Trace trace) throws BundlerException {
        IBinder binder = bundle.getBinder(TAG_VALUE);
        if (binder == null) {
            throw new TracedBundlerException("Bundle is missing the binder", trace);
        }

        return binder;
    }

    // so that we can put null values in the map
    @SuppressWarnings({"argument.type.incompatible", "deprecation"})
    private static Object deserializeMap(Bundle bundle, Trace trace) throws BundlerException {
        ArrayList<Parcelable> list = bundle.getParcelableArrayList(TAG_VALUE);
        if (list == null) {
            throw new TracedBundlerException("Bundle is missing the map", trace);
        }

        Map<Object, Object> map = new HashMap<>();
        for (Parcelable entry : list) {
            Bundle key = ((Bundle) entry).getBundle(TAG_1);
            Bundle value = ((Bundle) entry).getBundle(TAG_2);

            if (key == null) {
                throw new TracedBundlerException("Bundle is missing key", trace);
            }

            map.put(fromBundle(key, trace), value == null ? null : fromBundle(value, trace));
        }

        return map;
    }

    private static Object deserializeSet(Bundle bundle, Trace trace) throws BundlerException {
        return deserializeCollection(bundle, new HashSet<>(), trace);
    }

    private static Object deserializeList(Bundle bundle, Trace trace) throws BundlerException {
        return deserializeCollection(bundle, new ArrayList<>(), trace);
    }

    @SuppressWarnings("deprecation")
    private static Object deserializeCollection(
            Bundle bundle, Collection<Object> collection, Trace trace) throws BundlerException {
        ArrayList<Parcelable> list = bundle.getParcelableArrayList(TAG_VALUE);
        if (list == null) {
            throw new TracedBundlerException("Bundle is missing the collection", trace);
        }

        for (Parcelable value : list) {
            collection.add(fromBundle((Bundle) value, trace));
        }
        return collection;
    }

    // Method#invoke takes a nullable but the check complains.
    @SuppressWarnings({"argument.type.incompatible", "return.type.incompatible"})
    private static Object deserializeEnum(Bundle bundle, Trace trace) throws BundlerException {
        String enumName = bundle.getString(TAG_VALUE);
        if (enumName == null) {
            throw new TracedBundlerException("Missing enum name [" + enumName + "]", trace);
        }

        String enumClassName = bundle.getString(TAG_CLASS_NAME);
        if (enumClassName == null) {
            throw new TracedBundlerException("Missing enum className [" + enumClassName + "]",
                    trace);
        }

        try {
            Method nameMethod =
                    getClassOrSuperclassMethod(Class.forName(enumClassName), "valueOf", trace);
            return nameMethod.invoke(null, enumName);
        } catch (IllegalArgumentException e) {
            throw new TracedBundlerException(
                    "Enum value [" + enumName + "] does not exist in enum class [" + enumClassName
                            + "]",
                    trace,
                    e);
        } catch (ClassNotFoundException e) {
            throw new TracedBundlerException("Enum class [" + enumClassName + "] not found", trace,
                    e);
        } catch (ReflectiveOperationException e) {
            // Should not happen since it always exists.
            throw new TracedBundlerException(
                    "Enum of class [" + enumClassName + "] missing valueOf method", trace, e);
        }
    }

    private static Object deserializeClass(Bundle bundle, Trace trace) throws BundlerException {
        String className = bundle.getString(TAG_VALUE);
        if (className == null) {
            throw new TracedBundlerException("Class is missing the class name", trace);
        }

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new TracedBundlerException("Class name is unknown: " + className, trace, e);
        }
    }

    private static Object deserializeImage(Bundle bundle, Trace trace) throws BundlerException {
        Bundle iconCompatBundle = bundle.getBundle(TAG_VALUE);
        if (iconCompatBundle == null) {
            throw new TracedBundlerException("IconCompat bundle is null", trace);
        }
        IconCompat iconCompat = IconCompat.createFromBundle(iconCompatBundle);
        if (iconCompat == null) {
            throw new TracedBundlerException("Failed to create IconCompat from bundle", trace);
        }
        return iconCompat;
    }

    private static Object deserializePerson(Bundle bundle) {
        return Person.fromBundle(bundle);
    }

    @SuppressWarnings("deprecation")
    private static Object deserializeObject(Bundle bundle, Trace trace) throws BundlerException {
        String className = bundle.getString(TAG_CLASS_NAME);
        if (className == null) {
            throw new TracedBundlerException("Bundle is missing the class name", trace);
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isAnnotationPresent(CarProtocol.class)) {
                throw new TracedBundlerException(
                        "Invalid class not marked as CarProtocol: " + className, trace);
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object obj = constructor.newInstance();

            for (Field field : getFields(clazz)) {
                field.setAccessible(true);
                String fieldName = getFieldName(field);

                Object value = bundle.get(fieldName);
                if (value == null) {
                    // If we don't find the field in the bundle, try dejetifying it.
                    value = bundle.get(
                            fieldName.replaceAll(ICON_COMPAT_ANDROIDX, ICON_COMPAT_SUPPORT));
                }

                if (value instanceof Bundle) {
                    field.set(obj, fromBundle((Bundle) value, trace));
                } else if (value == null) {
                    if (Log.isLoggable(TAG_BUNDLER, Log.DEBUG)) {
                        Log.d(TAG_BUNDLER, "Value is null for field: " + field);
                    }
                }
            }
            return obj;
        } catch (ClassNotFoundException e) {
            throw new TracedBundlerException("Object for unknown class: " + className, trace, e);
        } catch (NoSuchMethodException e) {
            throw new TracedBundlerException(
                    "Object missing no args constructor: " + className, trace, e);
        } catch (ReflectiveOperationException e) {
            // Should not happen since we set it as accessible.
            throw new TracedBundlerException(
                    "Constructor or field is not accessible: " + className, trace, e);
        } catch (IllegalArgumentException e) {
            throw new TracedBundlerException("Failed to deserialize class: " + className, trace, e);
        }
    }

    @VisibleForTesting
    static String getFieldName(Field field) {
        return getFieldName(field.getDeclaringClass().getName(), field.getName());
    }

    @VisibleForTesting
    static String getFieldName(String className, String fieldName) {
        return className + fieldName;
    }

    private static List<Field> getFields(@Nullable Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        if (clazz == null || clazz == Object.class) {
            return fields;
        }

        Field[] classFields = clazz.getDeclaredFields();
        for (Field field : classFields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                fields.add(field);
            }
        }

        fields.addAll(getFields(clazz.getSuperclass()));
        return fields;
    }

    private static Method getClassOrSuperclassMethod(
            @Nullable Class<?> clazz, String methodName, Trace trace)
            throws TracedBundlerException {
        if (clazz == null || clazz == Object.class) {
            throw new TracedBundlerException("No method " + methodName + " in class " + clazz,
                    trace);
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                method.setAccessible(true);
                return method;
            }
        }
        return getClassOrSuperclassMethod(clazz.getSuperclass(), methodName, trace);
    }

    static String getUnobfuscatedClassName(Class<?> clazz) {
        String name = UNOBFUSCATED_TYPE_NAMES.get(clazz);
        if (name == null) {
            // Special case for subclasses of certain collections.
            if (List.class.isAssignableFrom(clazz)) {
                return "<List>";
            } else if (Map.class.isAssignableFrom(clazz)) {
                return "<Map>";
            } else if (Set.class.isAssignableFrom(clazz)) {
                return "<Set>";
            }
        }
        return name == null ? clazz.getSimpleName() : name;
    }

    static String getBundledTypeName(int type) {
        String name = BUNDLED_TYPE_NAMES.get(type);
        return name == null ? "unknown" : name;
    }

    private static Map<Integer, String> initBundledTypeNames() {
        ArrayMap<Integer, String> map = new ArrayMap<>();
        map.put(PRIMITIVE, "primitive");
        map.put(IINTERFACE, "iInterface");
        map.put(IBINDER, "iBinder");
        map.put(MAP, "map");
        map.put(SET, "set");
        map.put(LIST, "list");
        map.put(OBJECT, "object");
        map.put(IMAGE, "image");
        return map;
    }

    private static Map<Class<?>, String> initUnobfuscatedTypeNames() {
        // Init LUT for know type names, used for logging without proguard obfuscation.
        ArrayMap<Class<?>, String> map = new ArrayMap<>();
        map.put(Boolean.class, "bool");
        map.put(Byte.class, "byte");
        map.put(Short.class, "short");
        map.put(Integer.class, "int");
        map.put(Long.class, "long");
        map.put(Double.class, "double");
        map.put(Float.class, "float");
        map.put(String.class, "string");
        map.put(Parcelable.class, "parcelable");
        map.put(Map.class, "map");
        map.put(List.class, "list");
        map.put(IconCompat.class, "image");
        return map;
    }

    static String ellipsize(String s) {
        if (s.length() < MAX_VALUE_LOG_LENGTH) {
            return s;
        } else {
            return s.substring(0, MAX_VALUE_LOG_LENGTH - 1) + "...";
        }
    }

    static boolean isPrimitiveType(Object obj) {
        return obj instanceof Boolean
                || obj instanceof Byte
                || obj instanceof Character
                || obj instanceof Short
                || obj instanceof Integer
                || obj instanceof Long
                || obj instanceof Double
                || obj instanceof Float
                || obj instanceof String;
    }

    /** Represents a named frame in the serialization stack tracked by a {@link Trace} instance. */
    private static class Frame {
        private final Object mObj;
        private final String mDisplay;

        Frame(Object obj, String display) {
            mObj = obj;
            mDisplay = display;
        }

        public Object getObj() {
            return mObj;
        }

        @Override
        public String toString() {
            return toFlatString();
        }

        String toFlatString() {
            return "[" + mDisplay + ", " + getUnobfuscatedClassName(mObj.getClass()) + "]";
        }

        String toTraceString() {
            String s = getUnobfuscatedClassName(mObj.getClass()) + " " + mDisplay;
            if (!REDACT_LOG_VALUES && isPrimitiveType(mObj)) {
                s += ": " + ellipsize(mObj.toString());
            }
            return s;
        }
    }

    /**
     * Keeps a stack trace of a bundling or unbundling operation.
     *
     * <p>Used for detecting cycles, logging, and including a trace of the objects in exceptions
     * thrown during bundling and unbundling operations down the stack.
     *
     * <p>This type is {@link AutoCloseable} so that it can pop the frame it owns with its {@link
     * #close()} method once the operation is complete for the object.
     */
    private static class Trace implements AutoCloseable {
        private static final int MAX_LOG_INDENT = 12;
        private static final int MAX_FLAT_FRAMES = 8;

        @Nullable
        private String[] mIndents; // memoized blank lines used for indentation
        private final ArrayDeque<Frame> mFrames;

        static Trace create() {
            return new Trace(null, "", new ArrayDeque<>());
        }

        static Trace fromParent(@Nullable Object obj, String display, Trace parent) {
            return new Trace(obj, display, parent.mFrames);
        }

        @SuppressWarnings("deprecation")
        static String bundleToString(Bundle bundle) {
            int classType = bundle.getInt(TAG_CLASS_TYPE);
            String s = getBundledTypeName(classType);
            if (!REDACT_LOG_VALUES && classType == PRIMITIVE) {
                Object primitive = bundle.get(TAG_VALUE);
                s += ": " + (primitive != null ? ellipsize(primitive.toString()) : "<null>");
            }
            return s;
        }

        @Override
        public void close() {
            mFrames.removeFirst();
        }

        boolean find(Object obj) {
            for (Frame frame : mFrames) {
                if (frame.getObj() == obj) {
                    return true;
                }
            }
            return false;
        }

        /** Returns a flattened representation of the, apt for including in exception messages. */
        String toFlatString() {
            StringBuilder s = new StringBuilder();
            int count = Math.min(mFrames.size(), MAX_FLAT_FRAMES);
            Iterator<Frame> it = mFrames.descendingIterator();
            while (it.hasNext() && count-- > 0) {
                s.append(it.next().toFlatString());
            }
            if (it.hasNext()) {
                s.append("[...]");
            }
            return s.toString();
        }

        private String getIndent(int level) {
            level = Math.min(level, MAX_LOG_INDENT - 1);
            if (mIndents == null) {
                mIndents = new String[MAX_LOG_INDENT];
            }
            String indent = mIndents[level];
            if (indent == null) {
                indent = repeatChar(' ', level);
                if (level == MAX_LOG_INDENT - 1) {
                    indent += "...";
                }
                mIndents[level] = indent;
            }
            return indent;
        }

        private static String repeatChar(char c, int length) {
            char[] chars = new char[length];
            Arrays.fill(chars, c);
            return new String(chars);
        }

        @SuppressWarnings("method.invocation.invalid")
        private Trace(@Nullable Object obj, String display, ArrayDeque<Frame> frames) {
            mFrames = frames;
            if (obj != null) { // not the root
                Frame frame = new Frame(obj, display);
                frames.addFirst(frame);
                if (Log.isLoggable(TAG_BUNDLER, Log.VERBOSE)) {
                    Log.v(TAG_BUNDLER, getIndent(frames.size()) + frame.toTraceString());
                }
            }
        }
    }

    /**
     * A decorator on a {@link BundlerException} that tacks the frame information on its message.
     */
    static class TracedBundlerException extends BundlerException {
        TracedBundlerException(String msg, Trace trace) {
            super(msg + ", frames: " + trace.toFlatString());
        }

        TracedBundlerException(String msg, Trace trace, Throwable e) {
            super(msg + ", frames: " + trace.toFlatString(), e);
        }
    }

    /** A {@link BundlerException} thrown when a cycle is detected during bundling. */
    static class CycleDetectedBundlerException extends TracedBundlerException {
        CycleDetectedBundlerException(String msg, Trace trace) {
            super(msg, trace);
        }
    }

    private Bundler() {
    }
}
