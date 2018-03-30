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

package androidx.webkit.internal;

import android.webkit.WebView;

import androidx.core.os.BuildCompat;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class for calling into the WebView APK.
 */
public class WebViewGlueCommunicator {
    private static final String GLUE_FACTORY_PROVIDER_FETCHER_CLASS =
            "org.chromium.support_lib_glue.SupportLibReflectionUtil";
    private static final String GLUE_FACTORY_PROVIDER_FETCHER_METHOD =
            "createWebViewProviderFactory";

    /**
     * Fetch the one global support library WebViewProviderFactory from the WebView glue layer.
     */
    public static WebViewProviderFactoryAdapter getFactory() {
        return LAZY_FACTORY_HOLDER.INSTANCE;
    }

    public static WebkitToCompatConverter getCompatConverter() {
        return LAZY_FACTORY_HOLDER.COMPAT_CONVERTER;
    }

    private static class LAZY_FACTORY_HOLDER {
        static final WebViewProviderFactoryAdapter INSTANCE =
                new WebViewProviderFactoryAdapter(
                        WebViewGlueCommunicator.createGlueProviderFactory());
        static final WebkitToCompatConverter COMPAT_CONVERTER =
                new WebkitToCompatConverter(
                        INSTANCE.getWebkitToCompatConverter());
    }

    private static InvocationHandler fetchGlueProviderFactoryImpl() {
        try {
            Class<?> glueFactoryProviderFetcherClass = Class.forName(
                    GLUE_FACTORY_PROVIDER_FETCHER_CLASS, false, getWebViewClassLoader());
            Method createProviderFactoryMethod = glueFactoryProviderFetcherClass.getDeclaredMethod(
                    GLUE_FACTORY_PROVIDER_FETCHER_METHOD, InvocationHandler.class);
            return (InvocationHandler) createProviderFactoryMethod.invoke(null,
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new SupportLibraryInfo()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        // TODO(gsennton) if the above happens we should avoid throwing an exception! And probably
        // declare that the list of features supported by the WebView APK is empty.
    }

    private static WebViewProviderFactoryBoundaryInterface createGlueProviderFactory() {
        InvocationHandler invocationHandler = fetchGlueProviderFactoryImpl();
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebViewProviderFactoryBoundaryInterface.class, invocationHandler);
    }

    /**
     * Load the WebView code from the WebView APK and return the classloader containing that code.
     */
    @SuppressWarnings("NewApi")
    public static ClassLoader getWebViewClassLoader() {
        if (BuildCompat.isAtLeastP()) {
            return WebView.getWebViewClassLoader();
        } else {
            return getWebViewProviderFactory().getClass().getClassLoader();
        }
    }

    private static Object getWebViewProviderFactory() {
        try {
            Method getFactoryMethod = WebView.class.getDeclaredMethod("getFactory");
            getFactoryMethod.setAccessible(true);
            return getFactoryMethod.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private WebViewGlueCommunicator() {
    }
}
