/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import androidx.compose.remote.core.RcResolver;
import androidx.compose.remote.integration.view.demos.dsl.embedded.DemoEmbeddedRcControlKt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Host-side {@link RcResolver} implementation for {@code player-view-demos} that resolves embedded
 * {@code .rc} document URLs from the host application's {@code res/raw/} resources or {@code
 * assets/} folder into a {@code byte[]}.
 */
@SuppressLint("RestrictedApiAndroidX")
public final class DemoRcResolver {
    private DemoRcResolver() {}

    /**
     * Create an {@link RcResolver} backed by the given Android {@link Context}.
     *
     * @param context the host Android context
     * @return an {@link RcResolver} resolving packaged raw and asset {@code .rc} resources
     */
    public static @NonNull RcResolver create(@NonNull Context context) {
        Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext()
                : context;
        return url -> resolveFromContext(appContext, url);
    }

    @SuppressWarnings("DiscouragedApi")
    private static byte @Nullable [] resolveFromContext(
            @NonNull Context context, @NonNull String url) {
        String clean = url.trim();
        if (clean.startsWith("res/raw/")) {
            clean = clean.substring("res/raw/".length());
        } else if (clean.startsWith("raw://")) {
            clean = clean.substring("raw://".length());
        } else if (clean.startsWith("raw:")) {
            clean = clean.substring("raw:".length());
        }
        byte[] controlDoc = DemoEmbeddedRcControlKt.resolveEmbeddedControlDoc(clean);
        if (controlDoc != null) {
            return controlDoc;
        }
        int lastSlash = clean.lastIndexOf('/');
        String rawName = lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
        if (rawName.endsWith(".rc")) {
            rawName = rawName.substring(0, rawName.length() - 3);
        }
        Resources res = context.getResources();
        String pkg = context.getPackageName();
        int resId = res.getIdentifier(rawName, "raw", pkg);
        if (resId == 0 && "cube32".equals(rawName)) {
            resId = res.getIdentifier("cube3d", "raw", pkg);
        }
        if (resId != 0) {
            try (InputStream is = res.openRawResource(resId)) {
                return readAllBytes(is);
            } catch (IOException ignored) {
                // Fall through to assets lookup
            }
        }
        try (InputStream is = context.getAssets().open(clean)) {
            return readAllBytes(is);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static byte @NonNull [] readAllBytes(@NonNull InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        return bos.toByteArray();
    }
}
