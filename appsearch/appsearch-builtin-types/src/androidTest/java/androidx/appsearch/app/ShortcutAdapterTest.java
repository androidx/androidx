/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.appsearch.builtintypes.Timer;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ShortcutAdapterTest {

    private static final String EXPECTED_URI = "appsearch://__shortcut_adapter_db__/"
            + "__shortcut_adapter_ns__/id%201";

    @Test
    @SmallTest
    public void createShortcutFromDocument_WorksCorrectly() throws AppSearchException {
        final Context context = ApplicationProvider.getApplicationContext();
        final String name = "Timer 1";
        final String id = "id 1";
        final Timer timer = timer(id, name);
        final ShortcutInfoCompat si = ShortcutAdapter.createShortcutBuilderFromDocument(
                context, timer).build();
        assertThat(si.getId()).isEqualTo(id);
        assertThat(si.getShortLabel()).isEqualTo(name);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            assertThat(si.getIntent().toUri(0)).isEqualTo(
                    EXPECTED_URI + "#Intent;action=android.intent.action.VIEW;end");
        } else {
            assertThat(si.getIntent().toUri(0)).isEqualTo(EXPECTED_URI);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void createShortcutFromDocumentWithCustomNamespace_ThrowsException()
            throws AppSearchException {
        final Context context = ApplicationProvider.getApplicationContext();
        final Timer timer = new Timer.Builder("custom_ns", "my_id").build();
        ShortcutAdapter.createShortcutBuilderFromDocument(context, timer).build();
    }

    @Test
    @SmallTest
    public void extractDocumentFromShortcut_WorksCorrectly()
            throws AppSearchException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final Timer timer = timer("id 1");
        final ShortcutInfoCompat si = ShortcutAdapter.createShortcutBuilderFromDocument(
                context, timer).build();
        GenericDocument genericDocument = ShortcutAdapter.extractDocument(si);
        assertThat(genericDocument.getId()).isEqualTo("id 1");
    }

    @Test
    @SmallTest
    public void getDocumentUriFromDocument_WorksCorrectly() throws AppSearchException {
        final Timer timer = timer("id 1");
        assertThat(ShortcutAdapter.getDocumentUri(timer))
                .isEqualTo(Uri.parse(EXPECTED_URI));
    }

    @Test
    @SmallTest
    public void getDocumentUriFromShortcutId_WorksCorrectly() throws AppSearchException {
        assertThat(ShortcutAdapter.getDocumentUri("id 1"))
                .isEqualTo(Uri.parse(EXPECTED_URI));
    }

    private static Timer timer(@NonNull final String id) {
        return timer(id, "my timer");
    }

    private static Timer timer(@NonNull final String id, @NonNull final String name) {
        return new Timer.Builder(ShortcutAdapter.DEFAULT_NAMESPACE, id)
                .setDocumentScore(1)
                .setDocumentTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName(name)
                .setDurationMillis(1000)
                .setRemainingDurationMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .build();
    }
}
