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
import android.os.Parcel;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.graphics.drawable.IconCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteActionCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final IconCompat ICON = IconCompat.createWithContentUri("content://test");
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final PendingIntent ACTION = PendingIntent.getBroadcast(
            InstrumentationRegistry.getContext(), 0, new Intent("TESTACTION"),
            PendingIntent.FLAG_IMMUTABLE);

    public RemoteActionCompatTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRemoteAction_shallowCopy() throws Throwable {
        RemoteActionCompat reference = createTestRemoteActionCompat();
        RemoteActionCompat result = new RemoteActionCompat(reference);
        assertEqualsToTestRemoteActionCompat(result);
    }

    @Test
    public void testRemoteAction_parcel() {
        RemoteActionCompat reference = createTestRemoteActionCompat();

        Parcel p = Parcel.obtain();
        p.writeParcelable(ParcelUtils.toParcelable(reference), 0);
        p.setDataPosition(0);
        RemoteActionCompat result = ParcelUtils.fromParcelable(
                p.readParcelable(getClass().getClassLoader()));

        assertEqualsToTestRemoteActionCompat(result);
    }

    private RemoteActionCompat createTestRemoteActionCompat() {
        RemoteActionCompat reference = new RemoteActionCompat(ICON, TITLE, DESCRIPTION, ACTION);
        reference.setEnabled(false);
        reference.setShouldShowIcon(false);
        return reference;
    }

    private void assertEqualsToTestRemoteActionCompat(RemoteActionCompat remoteAction) {
        assertEquals(ICON.getUri(), remoteAction.getIcon().getUri());
        assertEquals(TITLE, remoteAction.getTitle());
        assertEquals(DESCRIPTION, remoteAction.getContentDescription());
        assertEquals(ACTION.getTargetPackage(), remoteAction.getActionIntent().getTargetPackage());
        assertFalse(remoteAction.isEnabled());
        assertFalse(remoteAction.shouldShowIcon());
    }
}