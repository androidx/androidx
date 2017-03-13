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
package android.support.media.tv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.support.media.tv.TvContractCompat.Channels;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.v4.os.BuildCompat;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests that channels can be created using the Builder pattern and correctly obtain
 * values from them
 */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public class ChannelTest extends TestCase {
    private static final String KEY_SPLASHSCREEN = "splashscreen";
    private static final String KEY_PREMIUM_CHANNEL = "premium";
    private static final String SPLASHSCREEN_URL = "http://example.com/splashscreen.jpg";

    @Override
    protected void tearDown() {
        ContentResolver resolver = InstrumentationRegistry.getContext().getContentResolver();
        resolver.delete(Channels.CONTENT_URI, null, null);
    }

    @Test
    public void testEmptyChannel() {
        Channel emptyChannel = new Channel.Builder()
                .build();
        ContentValues contentValues = emptyChannel.toContentValues(true);
        compareChannel(emptyChannel, Channel.fromCursor(getChannelCursor(contentValues)), false);
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
        compareChannel(sampleChannel, Channel.fromCursor(getChannelCursor(contentValues)), false);

        Channel clonedSampleChannel = new Channel.Builder(sampleChannel).build();
        compareChannel(sampleChannel, clonedSampleChannel, false);
    }

    @Test
    public void testFullyPopulatedChannel() {
        Channel fullyPopulatedChannel = createFullyPopulatedChannel();
        ContentValues contentValues = fullyPopulatedChannel.toContentValues(true);
        compareChannel(fullyPopulatedChannel, Channel.fromCursor(getChannelCursor(contentValues)),
                false);

        Channel clonedFullyPopulatedChannel = new Channel.Builder(fullyPopulatedChannel).build();
        compareChannel(fullyPopulatedChannel, clonedFullyPopulatedChannel, false);
    }

    @Test
    public void testChannelWithSystemContentProvider() {
        Channel fullyPopulatedChannel = createFullyPopulatedChannel();
        ContentValues contentValues = fullyPopulatedChannel.toContentValues();
        ContentResolver resolver = InstrumentationRegistry.getContext().getContentResolver();
        Uri channelUri = resolver.insert(Channels.CONTENT_URI, contentValues);
        assertNotNull(channelUri);

        Channel channelFromSystemDb;
        try (Cursor cursor = resolver.query(channelUri, Channel.PROJECTION, null, null, null)) {
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
            cursor.moveToNext();
            channelFromSystemDb = Channel.fromCursor(cursor);
        }
        compareChannel(fullyPopulatedChannel, channelFromSystemDb, true);
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
                .setPackageName("android.support.media.tv.test")
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
                .setTransient(true)
                .setBrowsable(true)
                .setSystemApproved(true)
                .build();
    }

    private static void compareChannel(Channel channelA, Channel channelB, boolean fromSystemDb) {
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
        if (BuildCompat.isAtLeastO()) {
            assertEquals(channelA.isTransient(), channelB.isTransient());
        }
        if (!fromSystemDb) {
            // Skip row ID since the one from system DB has the valid ID while the other does not.
            assertEquals(channelA.getId(), channelB.getId());
            // When we insert a channel using toContentValues() to the system, we drop some
            // protected fields since they only can be modified by system apps.
            assertEquals(channelA.isBrowsable(), channelB.isBrowsable());
            if (BuildCompat.isAtLeastO()) {
                assertEquals(channelA.isSystemApproved(), channelB.isSystemApproved());
                assertEquals(channelA.toContentValues(), channelB.toContentValues());
            }
            assertEquals(channelA.toString(), channelB.toString());
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
