/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.inspection;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.inspection.ArtTooling.EntryHook;
import androidx.inspection.ArtTooling.ExitHook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation details for DefaultArtTooling.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings({"rawtypes", "unused", "unchecked"})
public class ArtToolingImpl {

    private static ArtToolingImpl sInstance;

    /**
     * Create a singleton of this class.
     *
     * Implementation note: this class has to be a singleton.
     * We don't have a way to undo bytecode manipulations, so to avoid duplicating doing the same
     * transformations multiple times, this object lives forever.
     */
    @NonNull
    public static ArtToolingImpl instance() {
        if (sInstance == null) {
            System.loadLibrary("art_tooling");
            sInstance = new ArtToolingImpl(createNativeArtTooling());
        }
        return sInstance;
    }

    // will be passed to jni method to call methods on the instance
    @SuppressWarnings("FieldCanBeLocal")
    // Currently ArtToolingImpl is singleton and it is never destroyed, so we don't clean
    // this reference.
    private final long mNativePtr;

    // mExitHooks and mEntryHooks are both maps from:
    //   methodLabel -> method hook info
    @SuppressLint("BanConcurrentHashMap")
    private final Map<String, List<HookInfo<ExitHook>>> mExitHooks =
            new java.util.concurrent.ConcurrentHashMap<>();
    @SuppressLint("BanConcurrentHashMap")
    private final Map<String, List<HookInfo<EntryHook>>> mEntryHooks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Construct an instance referencing some native (JVMTI) resources.
     */
    private ArtToolingImpl(long nativePtr) {
        mNativePtr = nativePtr;
    }

    private static String createLabel(Class origin, String method) {
        return origin.getName() + "->" + method;
    }

    /**
     * Called from DefaultArtTooling
     */
    @NonNull
    public static <T> List<T> findInstances(@NonNull Class<T> clazz) {
        return Arrays.asList(nativeFindInstances(instance().mNativePtr, clazz));
    }

    /**
     * Called from DefaultArtTooling
     */
    public static void addEntryHook(@NonNull String inspectorId, @NonNull Class origin,
            @NonNull String method,
            @NonNull EntryHook hook) {
        nativeRegisterEntryHook(instance().mNativePtr, origin, method);
        List<HookInfo<EntryHook>> hooks = instance().mEntryHooks.computeIfAbsent(
                createLabel(origin, method),
                key -> new CopyOnWriteArrayList<>()
        );
        hooks.add(new HookInfo<>(inspectorId, hook));
    }

    /**
     * Called from DefaultArtTooling
     */
    public static void addExitHook(@NonNull String inspectorId, @NonNull Class origin,
            @NonNull String method,
            @NonNull ExitHook<?> hook) {
        nativeRegisterExitHook(instance().mNativePtr, origin, method);
        List<HookInfo<ExitHook>> hooks = instance().mExitHooks.computeIfAbsent(
                createLabel(origin, method),
                key -> new CopyOnWriteArrayList<>()
        );
        hooks.add(new HookInfo<>(inspectorId, hook));
    }

    /**
     * Called from DefaultArtTooling
     */
    public static void unregisterHooks(@NonNull String inspectorId) {
        removeHooks(inspectorId, instance().mEntryHooks);
        removeHooks(inspectorId, instance().mExitHooks);
    }

    private static void removeHooks(
            String inspectorId, Map<String, ? extends List<? extends HookInfo<?>>> hooks) {
        for (List<? extends HookInfo<?>> list : hooks.values()) {
            for (HookInfo<?> info : list) {
                if (info.mInspectorId.equals(inspectorId)) {
                    list.remove(info);
                }
            }
        }
    }

    private static <T> T onExitInternal(String label, T returnObject) {
        ArtToolingImpl instance = ArtToolingImpl.instance();
        List<HookInfo<ExitHook>> hooks = instance.mExitHooks.get(label);
        if (hooks == null) {
            return returnObject;
        }

        for (HookInfo<ExitHook> hook : hooks) {
            returnObject = (T) hook.mHook.onExit(returnObject);
        }
        return returnObject;
    }

    /** Callback from native */
    @Nullable
    public static Object onExit(@NonNull String methodSignature, @Nullable Object returnObject) {
        return onExitInternal(methodSignature, returnObject);
    }

    /** Callback from native */
    public static void onExit(@NonNull String methodSignature) {
        onExitInternal(methodSignature, null);
    }

    /** Callback from native */
    public static boolean onExit(@NonNull String methodSignature, boolean result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static byte onExit(@NonNull String methodSignature, byte result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static char onExit(@NonNull String methodSignature, char result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static short onExit(@NonNull String methodSignature, short result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static int onExit(@NonNull String methodSignature, int result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static float onExit(@NonNull String methodSignature, float result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static long onExit(@NonNull String methodSignature, long result) {
        return onExitInternal(methodSignature, result);
    }

    /** Callback from native */
    public static double onExit(@NonNull String methodSignature, double result) {
        return onExitInternal(methodSignature, result);
    }

    /**
     * Receives an array where:
     *
     * <ol>
     *   <li>the first parameter is the method signature,
     *   <li>the second parameter is the "this" reference,
     *   <li>all remaining arguments are the function's parameters.
     * </ol>
     *
     * <p>For example, the function {@code Client#sendMessage(Receiver r, String message)} will
     * receive the array: ["(Lcom/example/Receiver;Ljava/lang/String;)Lcom/example/Client;", this,
     * r, message]
     */
    public static void onEntry(@NonNull Object[] signatureThisParams) {
        // Should always at least contain signature and "this"
        assert (signatureThisParams.length >= 2);
        String signature = (String) signatureThisParams[0];
        List<HookInfo<EntryHook>> hooks = ArtToolingImpl.instance().mEntryHooks.get(signature);

        if (hooks == null) {
            return;
        }

        Object thisObject = signatureThisParams[1];
        List<Object> params = Collections.emptyList();
        if (signatureThisParams.length > 2) {
            params =
                    Arrays.asList(
                            Arrays.copyOfRange(signatureThisParams, 2, signatureThisParams.length));
        }

        for (HookInfo<EntryHook> hook : hooks) {
            hook.mHook.onEntry(thisObject, params);
        }
    }

    private static final class HookInfo<T> {
        final String mInspectorId;
        final T mHook;

        HookInfo(String inspectorId, T hook) {
            this.mInspectorId = inspectorId;
            this.mHook = hook;
        }
    }

    private static native long createNativeArtTooling();

    private static native <T> T[] nativeFindInstances(long servicePtr, Class<T> clazz);

    private static native void nativeRegisterEntryHook(
            long servicePtr, Class<?> originClass, String originMethod);

    private static native void nativeRegisterExitHook(
            long servicePtr, Class<?> originClass, String originMethod);
}
