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

package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteActionCompatTest extends BaseInstrumentationTestCase<TestActivity> {

    public RemoteActionCompatTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRemoteAction_bundle() throws Throwable {
        IconCompat icon = IconCompat.createWithContentUri("content://test");
        String title = "title";
        String description = "description";
        PendingIntent action = PendingIntent.getBroadcast(InstrumentationRegistry.getContext(), 0,
                new Intent("TESTACTION"), 0);
        RemoteActionCompat reference = new RemoteActionCompat(icon, title, description, action);
        reference.setEnabled(false);
        reference.setShouldShowIcon(false);

        RemoteActionCompat result = RemoteActionCompat.createFromBundle(reference.toBundle());

        assertEquals(icon.getUri(), result.getIcon().getUri());
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getContentDescription());
        assertEquals(action.getCreatorPackage(), result.getActionIntent().getCreatorPackage());
        assertFalse(result.isEnabled());
        assertFalse(result.shouldShowIcon());
    }

    @Test
    public void testRemoteAction_shallowCopy() throws Throwable {
        IconCompat icon = IconCompat.createWithContentUri("content://test");
        String title = "title";
        String description = "description";
        PendingIntent action = PendingIntent.getBroadcast(InstrumentationRegistry.getContext(), 0,
                new Intent("TESTACTION"), 0);
        RemoteActionCompat reference = new RemoteActionCompat(icon, title, description, action);
        reference.setEnabled(false);
        reference.setShouldShowIcon(false);

        RemoteActionCompat result = new RemoteActionCompat(reference);

        assertEquals(icon.getUri(), result.getIcon().getUri());
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getContentDescription());
        assertEquals(action.getCreatorPackage(), result.getActionIntent().getCreatorPackage());
        assertFalse(result.isEnabled());
        assertFalse(result.shouldShowIcon());
    }
}
