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

package com.example.android.support.text.emoji;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class EmojiRepo {
    private static final String TAG = "EmojiData";
    private static List<EmojiData> sEmojis = new ArrayList<>();

    private EmojiRepo() {
    }

    static List<EmojiData> getEmojis() {
        return sEmojis;
    }

    static synchronized void load(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    read(context);
                } catch (Throwable t) {
                    Log.e(TAG, "Cannot load emojis", t);
                }
            }
        }).run();
    }

    private static void read(Context context) throws IOException {
        final InputStream inputStream = context.getAssets().open("emojis.txt");
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder stringBuilder = new StringBuilder();
            final StringBuilder codepointBuilder = new StringBuilder();
            List<Integer> codepointsList;

            String s;
            while ((s = reader.readLine()) != null) {
                s = s.trim();
                // skip comments
                if (s.isEmpty() || s.startsWith("#")) continue;

                stringBuilder.setLength(0);
                codepointBuilder.setLength(0);
                codepointsList = new ArrayList<>();

                // emoji codepoints are space separated: i.e. 0x1f1e6 0x1f1e8
                final String[] split = s.split(" ");

                for (int index = 0; index < split.length; index++) {
                    final String part = split[index].trim();
                    int codepoint = Integer.parseInt(part, 16);
                    codepointsList.add(codepoint);
                    codepointBuilder.append(String.format("u+%04x", codepoint));
                    codepointBuilder.append(" ");
                    stringBuilder.append(Character.toChars(codepoint));
                }
                final EmojiData emojiData = new EmojiData(stringBuilder.toString(), codepointsList,
                        codepointBuilder.toString());
                sEmojis.add(emojiData);
            }
        } finally {
            inputStream.close();
        }
    }

    static class EmojiData {
        private String mEmoji;
        private List<Integer> mCodepoints;
        private String mCodepointString;

        EmojiData(String emoji, List<Integer> codepoints, String codepointString) {
            mEmoji = emoji;
            mCodepoints = codepoints;
            mCodepointString = codepointString;
        }

        public String getEmoji() {
            return mEmoji;
        }

        public List<Integer> getCodepoints() {
            return mCodepoints;
        }

        public String getCodepointString() {
            return mCodepointString;
        }
    }
}
