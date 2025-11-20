/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.tvprovider.media.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.tvprovider.media.tv.TvContractCompat.Channels;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that channels can be created using the Builder pattern and correctly obtain
 * values from them
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ChannelTest {
    @After
    public void tearDown() {
        if (!Utils.hasTvInputFramework(ApplicationProvider.getApplicationContext())) {
            return;
        }
        ContentResolver resolver = ApplicationProvider.getApplicationContext().getContentResolver();
        resolver.delete(Channels.CONTENT_URI, null, null);
    }

    @Test
    public void testEmptyChannel() {
        Channel emptyChannel = new Channel.Builder()
                .build();
        ContentValues contentValues = emptyChannel.toContentValues(true);
        compareChannel(emptyChannel, Channel.fromCursor(getChannelCursor(contentValues)), true);
    }

    @Test
    public void testSampleChannel() {
        // Tests cloning and database I/O of a channel with some defined and some undefined
        // values.
        Channel sampleChannel = new Channel.Builder()
                .setDisplayName("Google")
                .setDisplayNumber("3")
                .setDescription("This is a sample channel")
                .setOriginalNetworkId(1)
                .setAppLinkIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setOriginalNetworkId(0)
                .build();
        ContentValues contentValues = sampleChannel.toContentValues(true);
        compareChannel(sampleChannel, Channel.fromCursor(getChannelCursor(contentValues)), true);

        Channel clonedSampleChannel = new Channel.Builder(sampleChannel).build();
        compareChannel(sampleChannel, clonedSampleChannel, true);
    }

    @Test
    public void testFullyPopulatedChannel() {
        Channel fullyPopulatedChannel = createFullyPopulatedChannel();
        ContentValues contentValues = fullyPopulatedChannel.toContentValues(true);
        compareChannel(fullyPopulatedChannel, Channel.fromCursor(getChannelCursor(contentValues)),
                true);

        Channel clonedFullyPopulatedChannel = new Channel.Builder(fullyPopulatedChannel).build();
        compareChannel(fullyPopulatedChannel, clonedFullyPopulatedChannel, true);
    }

    @Test
    public void testChannelWithSystemContentProvider() {
        if (!Utils.hasTvInputFramework(ApplicationProvider.getApplicationContext())) {
            return;
        }
        Channel fullyPopulatedChannel = createFullyPopulatedChannel();
        ContentValues contentValues = fullyPopulatedChannel.toContentValues();
        ContentResolver resolver = ApplicationProvider.getApplicationContext().getContentResolver();
        Uri channelUri = resolver.insert(Channels.CONTENT_URI, contentValues);
        assertNotNull(channelUri);

        Channel channelFromSystemDb = loadChannelFromContentProvider(resolver, channelUri);
        compareChannel(fullyPopulatedChannel, channelFromSystemDb, false);
    }

    @Test
    public void testChannelUpdateWithContentProvider() {
        if (!Utils.hasTvInputFramework(ApplicationProvider.getApplicationContext())) {
            return;
        }

        Channel fullyPopulatedChannel = createFullyPopulatedChannel();
        ContentValues contentValues = fullyPopulatedChannel.toContentValues();
        ContentResolver resolver = ApplicationProvider.getApplicationContext().getContentResolver();
        Uri channelUri = resolver.insert(Channels.CONTENT_URI, contentValues);
        assertNotNull(channelUri);

        Channel channelFromSystemDb = loadChannelFromContentProvider(resolver, channelUri);
        compareChannel(fullyPopulatedChannel, channelFromSystemDb, false);

        // Update a field from a fully loaded channel.
        Channel updatedChannel = new Channel.Builder(channelFromSystemDb)
                .setDescription("new description").build();
        assertEquals(1, resolver.update(channelUri, updatedChannel.toContentValues(), null, null));
        channelFromSystemDb = loadChannelFromContentProvider(resolver, channelUri);
        compareChannel(updatedChannel, channelFromSystemDb, false);

        // Update a field with null from a fully loaded channel.
        updatedChannel = new Channel.Builder(updatedChannel)
                .setAppLinkText(null).build();
        assertEquals(1, resolver.update(
                channelUri, updatedChannel.toContentValues(), null, null));
        channelFromSystemDb = loadChannelFromContentProvider(resolver, channelUri);
        compareChannel(updatedChannel, channelFromSystemDb, false);

        // Update a field without referencing fully channel.
        ContentValues values = new Channel.Builder().setDisplayName("abc").build()
                .toContentValues();
        assertEquals(1, values.size());
        assertEquals(1, resolver.update(channelUri, values, null, null));
        channelFromSystemDb = loadChannelFromContentProvider(resolver, channelUri);
        Channel expectedChannel = new Channel.Builder(channelFromSystemDb)
                .setDisplayName("abc").build();
        compareChannel(expectedChannel, channelFromSystemDb, false);
    }

    @Test
    public void testChannelEquals() {
        assertEquals(createFullyPopulatedChannel(), createFullyPopulatedChannel());
    }


    private static Channel loadChannelFromContentProvider(
            ContentResolver resolver, Uri channelUri) {
        try (Cursor cursor = resolver.query(channelUri, null, null, null, null)) {
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
            cursor.moveToNext();
            return Channel.fromCursor(cursor);
        }
    }

    private static Channel createFullyPopulatedChannel() {
        return new Channel.Builder()
                .setAppLinkColor(0x00FF0000)
                .setAppLinkIconUri(Uri.parse("http://example.com/icon.png"))
                .setAppLinkIntent(new Intent())
                .setAppLinkPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setAppLinkText("Open an intent")
                .setDescription("Channel description")
                .setDisplayName("Display Name")
                .setDisplayNumber("100")
                .setInputId("TestInputService")
                .setNetworkAffiliation("Network Affiliation")
                .setOriginalNetworkId(2)
                .setPackageName("androidx.tvprovider.test")
                .setSearchable(false)
                .setServiceId(3)
                .setTransportStreamId(4)
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setServiceType(TvContractCompat.Channels.SERVICE_TYPE_AUDIO_VIDEO)
                .setVideoFormat(TvContractCompat.Channels.VIDEO_FORMAT_240P)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setInternalProviderId("Internal Provider")
                .setTransient(true)
                .setBrowsable(true)
                .setLocked(true)
                .setSystemApproved(true)
                .build();
    }

    private static void compareChannel(Channel channelA, Channel channelB,
            boolean includeIdAndProtectedFields) {
        assertEquals(channelA.isSearchable(), channelB.isSearchable());
        assertEquals(channelA.getDescription(), channelB.getDescription());
        assertEquals(channelA.getDisplayName(), channelB.getDisplayName());
        assertEquals(channelA.getDisplayNumber(), channelB.getDisplayNumber());
        assertEquals(channelA.getInputId(), channelB.getInputId());
        assertEquals(channelA.getNetworkAffiliation(), channelB.getNetworkAffiliation());
        assertEquals(channelA.getOriginalNetworkId(), channelB.getOriginalNetworkId());
        assertEquals(channelA.getPackageName(), channelB.getPackageName());
        assertEquals(channelA.getServiceId(), channelB.getServiceId());
        assertEquals(channelA.getServiceType(), channelB.getServiceType());
        assertEquals(channelA.getTransportStreamId(), channelB.getTransportStreamId());
        assertEquals(channelA.getType(), channelB.getType());
        assertEquals(channelA.getVideoFormat(), channelB.getVideoFormat());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(channelA.getAppLinkColor(), channelB.getAppLinkColor());
            assertEquals(channelA.getAppLinkIconUri(), channelB.getAppLinkIconUri());
            assertEquals(channelA.getAppLinkIntentUri(), channelB.getAppLinkIntentUri());
            assertEquals(channelA.getAppLinkPosterArtUri(), channelB.getAppLinkPosterArtUri());
            assertEquals(channelA.getAppLinkText(), channelB.getAppLinkText());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(channelA.getInternalProviderId(), channelB.getInternalProviderId());
            assertEquals(channelA.isTransient(), channelB.isTransient());
        }
        if (includeIdAndProtectedFields) {
            // Skip row ID since the one from system DB has the valid ID while the other does not.
            assertEquals(channelA.getId(), channelB.getId());
            // When we insert a channel using toContentValues() to the system, we drop some
            // protected fields since they only can be modified by system apps.
            assertEquals(channelA.isBrowsable(), channelB.isBrowsable());
            assertEquals(channelA.isLocked(), channelB.isLocked());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertEquals(channelA.isSystemApproved(), channelB.isSystemApproved());
            }
            assertEquals(channelA.toContentValues(), channelB.toContentValues());
        }
    }

    private static MatrixCursor getChannelCursor(ContentValues contentValues) {
        String[] cols = Channel.PROJECTION;
        MatrixCursor cursor = new MatrixCursor(cols);
        MatrixCursor.RowBuilder builder = cursor.newRow();
        for (String col : cols) {
            if (col != null) {
                builder.add(col, contentValues.get(col));
            }
        }
        cursor.moveToFirst();
        return cursor;
    }
}
