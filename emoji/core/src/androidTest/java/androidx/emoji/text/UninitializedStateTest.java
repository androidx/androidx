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
package androidx.emoji.text;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class UninitializedStateTest {

    private TestConfigBuilder.WaitingDataLoader mWaitingDataLoader;

    @Before
    public void setup() {
        mWaitingDataLoader = new TestConfigBuilder.WaitingDataLoader(true);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(mWaitingDataLoader);
        EmojiCompat.reset(config);
    }

    @After
    public void after() {
        mWaitingDataLoader.getLoaderLatch().countDown();
        mWaitingDataLoader.getTestLatch().countDown();
    }

    @Test(expected = IllegalStateException.class)
    public void testHasEmojiGlyph() {
        EmojiCompat.get().hasEmojiGlyph("anystring");
    }

    @Test(expected = IllegalStateException.class)
    public void testHasEmojiGlyph_withMetadataVersion() {
        EmojiCompat.get().hasEmojiGlyph("anystring", 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testProcess() {
        EmojiCompat.get().process("anystring");
    }

    @Test(expected = IllegalStateException.class)
    public void testProcess_withStartEnd() {
        EmojiCompat.get().process("anystring", 1, 2);
    }
}
