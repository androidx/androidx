/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.emoji2.bundled;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BundledEmojiCompatConfigTest {
    @Test
    @SdkSuppress(maxSdkVersion = 34)
    public void whenPassedExecutor_usesIt() {
        Context context = ApplicationProvider.getApplicationContext();
        Executor executor = mock(Executor.class);
        BundledEmojiCompatConfig config = new BundledEmojiCompatConfig(context, executor);
        EmojiCompat.reset(config);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void testAllEmojiFirstCodepointsAreCandidates() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        AssetManager assetManager = context.getAssets();
        MetadataRepo repo = MetadataRepo.create(assetManager, "NotoColorEmojiCompat.ttf");
        MetadataList list = repo.getMetadataList();
        MetadataItem item = new MetadataItem();
        for (int i = 0; i < list.listLength(); i++) {
            list.list(item, i);
            if (item.codepointsLength() > 0) {
                int firstCodepoint = item.codepoints(0);
                assertTrue(
                        "Codepoint 0x" + Integer.toHexString(firstCodepoint)
                                + " must be classified as an emoji candidate",
                        MetadataRepo.isEmojiCandidate(firstCodepoint)
                );
            }
        }
    }
}
