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

package androidx.core.view;

import static androidx.core.view.ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.view.ContentInfoCompat.SOURCE_APP;
import static androidx.core.view.ContentInfoCompat.SOURCE_CLIPBOARD;

import static com.google.common.truth.Truth.assertThat;

import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.ContentInfo;

import androidx.core.util.Predicate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ContentInfoCompatTest {

    @Test
    public void testPartition_multipleItems() throws Exception {
        Uri sampleUri = Uri.parse("content://com.example/path");
        ClipData clip = ClipData.newPlainText("", "Hello");
        clip.addItem(new ClipData.Item("Hi"));
        clip.addItem(new ClipData.Item(sampleUri));
        ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        // Test splitting when some items match and some don't.
        Pair<ContentInfoCompat, ContentInfoCompat> split;
        split = payload.partition(new Predicate<ClipData.Item>() {
            @Override
            public boolean test(ClipData.Item item) {
                return item.getUri() != null;
            }
        });
        assertThat(split.first.getClip().getItemCount()).isEqualTo(1);
        assertThat(split.second.getClip().getItemCount()).isEqualTo(2);
        assertThat(split.first.getClip().getItemAt(0).getUri()).isEqualTo(sampleUri);
        assertThat(split.first.getClip().getDescription()).isNotSameInstanceAs(
                payload.getClip().getDescription());
        assertThat(split.second.getClip().getDescription()).isNotSameInstanceAs(
                payload.getClip().getDescription());
        assertThat(split.first.getSource()).isEqualTo(SOURCE_CLIPBOARD);
        assertThat(split.first.getLinkUri()).isNotNull();
        assertThat(split.first.getExtras()).isNotNull();
        assertThat(split.second.getSource()).isEqualTo(SOURCE_CLIPBOARD);
        assertThat(split.second.getLinkUri()).isNotNull();
        assertThat(split.second.getExtras()).isNotNull();

        // Test splitting when none of the items match.
        split = payload.partition(new Predicate<ClipData.Item>() {
            @Override
            public boolean test(ClipData.Item item) {
                return false;
            }
        });
        assertThat(split.first).isNull();
        assertThat(split.second).isSameInstanceAs(payload);

        // Test splitting when all of the items match.
        split = payload.partition(new Predicate<ClipData.Item>() {
            @Override
            public boolean test(ClipData.Item item) {
                return true;
            }
        });
        assertThat(split.first).isSameInstanceAs(payload);
        assertThat(split.second).isNull();
    }

    @Test
    public void testPartition_singleItem() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");
        ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        Pair<ContentInfoCompat, ContentInfoCompat> split;
        split = payload.partition(new Predicate<ClipData.Item>() {
            @Override
            public boolean test(ClipData.Item item) {
                return false;
            }
        });
        assertThat(split.first).isNull();
        assertThat(split.second).isSameInstanceAs(payload);

        split = payload.partition(new Predicate<ClipData.Item>() {
            @Override
            public boolean test(ClipData.Item item) {
                return true;
            }
        });
        assertThat(split.first).isSameInstanceAs(payload);
        assertThat(split.second).isNull();
    }

    @Test
    public void testBuilder_validation() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");

        // Test validation of source.
        ContentInfoCompat.Builder builder = new ContentInfoCompat.Builder(clip, 6);
        try {
            ContentInfoCompat payload = builder.build();
            Assert.fail("Expected exception but got: " + payload);
        } catch (IllegalArgumentException expected) {
        }

        // Test validation of flags.
        builder = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD).setFlags(1 << 1);
        try {
            ContentInfoCompat payload = builder.build();
            Assert.fail("Expected exception but got: " + payload);
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testBuilder_copy() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");
        ContentInfoCompat original = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        // Verify that that calling the builder with a ContentInfoCompat instance creates a
        // shallow copy.
        ContentInfoCompat copy = new ContentInfoCompat.Builder(original).build();
        assertThat(copy).isNotSameInstanceAs(original);
        assertThat(copy.getClip()).isSameInstanceAs(original.getClip());
        assertThat(copy.getSource()).isEqualTo(original.getSource());
        assertThat(copy.getFlags()).isEqualTo(original.getFlags());
        assertThat(copy.getLinkUri()).isSameInstanceAs(original.getLinkUri());
        assertThat(copy.getExtras()).isSameInstanceAs(original.getExtras());
    }

    @Test
    public void testBuilder_copyAndUpdate() throws Exception {
        ClipData clip1 = ClipData.newPlainText("", "Hello");
        ContentInfoCompat original = new ContentInfoCompat.Builder(clip1, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        // Verify that calling setters after initializing the builder with a ContentInfoCompat
        // instance updates the fields.
        ClipData clip2 = ClipData.newPlainText("", "Bye");
        ContentInfoCompat copy = new ContentInfoCompat.Builder(original)
                .setClip(clip2)
                .setSource(SOURCE_APP)
                .setFlags(0)
                .setLinkUri(null)
                .setExtras(null)
                .build();
        assertThat(copy.getClip().getItemAt(0).getText()).isEqualTo("Bye");
        assertThat(copy.getSource()).isEqualTo(SOURCE_APP);
        assertThat(copy.getFlags()).isEqualTo(0);
        assertThat(copy.getLinkUri()).isEqualTo(null);
        assertThat(copy.getExtras()).isEqualTo(null);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testBuilder_copyAndUpdate_platformContentInfo() throws Exception {
        ClipData clip1 = ClipData.newPlainText("", "Hello");
        ContentInfoCompat original = new ContentInfoCompat.Builder(clip1, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        // Verify that calling setters after initializing the builder with a ContentInfoCompat
        // instance updates the wrapped platform object also.
        ClipData clip2 = ClipData.newPlainText("", "Bye");
        ContentInfoCompat copy = new ContentInfoCompat.Builder(original)
                .setClip(clip2)
                .setSource(SOURCE_APP)
                .setFlags(0)
                .setLinkUri(null)
                .setExtras(null)
                .build();
        ContentInfo platContentInfo = copy.toContentInfo();
        assertThat(platContentInfo).isNotSameInstanceAs(original.toContentInfo());
        assertThat(platContentInfo.getClip().getItemAt(0).getText()).isEqualTo("Bye");
        assertThat(platContentInfo.getSource()).isEqualTo(SOURCE_APP);
        assertThat(platContentInfo.getFlags()).isEqualTo(0);
        assertThat(platContentInfo.getLinkUri()).isEqualTo(null);
        assertThat(platContentInfo.getExtras()).isEqualTo(null);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCompatToPlatform() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");
        Bundle extras = new Bundle();
        extras.putString("sampleExtrasKey", "sampleExtrasValue");
        ContentInfoCompat contentInfo = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD)
                .setFlags(FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(extras)
                .build();

        // Verify that retrieving the platform object returns a shallow copy.
        ContentInfo platContentInfo = contentInfo.toContentInfo();
        assertThat(platContentInfo.getClip()).isSameInstanceAs(contentInfo.getClip());
        assertThat(platContentInfo.getSource()).isEqualTo(contentInfo.getSource());
        assertThat(platContentInfo.getFlags()).isEqualTo(contentInfo.getFlags());
        assertThat(platContentInfo.getLinkUri()).isSameInstanceAs(contentInfo.getLinkUri());
        assertThat(platContentInfo.getExtras()).isSameInstanceAs(contentInfo.getExtras());

        // Verify that retrieving the platform object multiple times returns the same instance.
        ContentInfo platContentInfo2 = contentInfo.toContentInfo();
        assertThat(platContentInfo2).isSameInstanceAs(platContentInfo);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testPlatformToCompat() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");
        Bundle extras = new Bundle();
        extras.putString("sampleExtrasKey", "sampleExtrasValue");
        ContentInfo platContentInfo = new ContentInfo.Builder(clip, ContentInfo.SOURCE_CLIPBOARD)
                .setFlags(ContentInfo.FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(extras)
                .build();

        // Verify that converting to the compat object returns a shallow copy.
        ContentInfoCompat contentInfo = ContentInfoCompat.toContentInfoCompat(platContentInfo);
        assertThat(contentInfo.getClip()).isSameInstanceAs(platContentInfo.getClip());
        assertThat(contentInfo.getSource()).isEqualTo(platContentInfo.getSource());
        assertThat(contentInfo.getFlags()).isEqualTo(platContentInfo.getFlags());
        assertThat(contentInfo.getLinkUri()).isSameInstanceAs(platContentInfo.getLinkUri());
        assertThat(contentInfo.getExtras()).isSameInstanceAs(platContentInfo.getExtras());

        // Verify that converting to the compat object multiple times returns a new instance each
        // time.
        ContentInfoCompat contentInfo2 = ContentInfoCompat.toContentInfoCompat(platContentInfo);
        assertThat(contentInfo2).isNotSameInstanceAs(contentInfo);

        // Verify that converting back from the compat object returns the original platform
        // instance.
        assertThat(contentInfo.toContentInfo()).isSameInstanceAs(platContentInfo);
        assertThat(contentInfo2.toContentInfo()).isSameInstanceAs(platContentInfo);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testPartitionPlatformContentInfo_multipleItems() throws Exception {
        Uri sampleUri = Uri.parse("content://com.example/path");
        ClipData clip = ClipData.newPlainText("", "Hello");
        clip.addItem(new ClipData.Item("Hi", "<b>Salut</b>"));
        clip.addItem(new ClipData.Item(sampleUri));
        ContentInfo payload = new ContentInfo.Builder(clip, ContentInfo.SOURCE_CLIPBOARD)
                .setFlags(ContentInfo.FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        // Test splitting when some items match and some don't.
        Pair<ContentInfo, ContentInfo> split;
        split = ContentInfoCompat.partition(payload,
                new java.util.function.Predicate<ClipData.Item>() {
                    @Override
                    public boolean test(ClipData.Item item) {
                        return item.getUri() != null;
                    }
                });
        assertThat(split.first.getClip().getItemCount()).isEqualTo(1);
        assertThat(split.second.getClip().getItemCount()).isEqualTo(2);
        assertThat(split.first.getClip().getItemAt(0).getUri()).isEqualTo(sampleUri);
        assertThat(split.first.getClip().getDescription()).isNotSameInstanceAs(
                payload.getClip().getDescription());
        assertThat(split.second.getClip().getDescription()).isNotSameInstanceAs(
                payload.getClip().getDescription());
        assertThat(split.first.getSource()).isEqualTo(ContentInfo.SOURCE_CLIPBOARD);
        assertThat(split.first.getLinkUri()).isNotNull();
        assertThat(split.first.getExtras()).isNotNull();
        assertThat(split.second.getSource()).isEqualTo(ContentInfo.SOURCE_CLIPBOARD);
        assertThat(split.second.getLinkUri()).isNotNull();
        assertThat(split.second.getExtras()).isNotNull();

        // Test splitting when none of the items match.
        split = ContentInfoCompat.partition(payload,
                new java.util.function.Predicate<ClipData.Item>() {
                    @Override
                    public boolean test(ClipData.Item item) {
                        return false;
                    }
                });
        assertThat(split.first).isNull();
        assertThat(split.second).isSameInstanceAs(payload);

        // Test splitting when all of the items match.
        split = ContentInfoCompat.partition(payload,
                new java.util.function.Predicate<ClipData.Item>() {
                    @Override
                    public boolean test(ClipData.Item item) {
                        return true;
                    }
                });
        assertThat(split.first).isSameInstanceAs(payload);
        assertThat(split.second).isNull();
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testPartitionPlatformContentInfo_singleItem() throws Exception {
        ClipData clip = ClipData.newPlainText("", "Hello");
        ContentInfo payload = new ContentInfo.Builder(clip, ContentInfo.SOURCE_CLIPBOARD)
                .setFlags(ContentInfo.FLAG_CONVERT_TO_PLAIN_TEXT)
                .setLinkUri(Uri.parse("http://example.com"))
                .setExtras(new Bundle())
                .build();

        Pair<ContentInfo, ContentInfo> split;
        split = ContentInfoCompat.partition(payload,
                new java.util.function.Predicate<ClipData.Item>() {
                    @Override
                    public boolean test(ClipData.Item item) {
                        return false;
                    }
                });
        assertThat(split.first).isNull();
        assertThat(split.second).isSameInstanceAs(payload);

        split = ContentInfoCompat.partition(payload,
                new java.util.function.Predicate<ClipData.Item>() {
                    @Override
                    public boolean test(ClipData.Item item) {
                        return true;
                    }
                });
        assertThat(split.first).isSameInstanceAs(payload);
        assertThat(split.second).isNull();
    }
}
