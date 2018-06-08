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
package androidx.emoji.widget;

import android.annotation.SuppressLint;
import android.text.Editable;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * EditableFactory used to improve editing operations on an EditText.
 * <p>
 * EditText uses DynamicLayout, which attaches to the Spannable instance that is being edited using
 * ChangeWatcher. ChangeWatcher implements SpanWatcher and Textwatcher. Currently every delete/add
 * operation is reported to DynamicLayout, for every span that has changed. For each change,
 * DynamicLayout performs some expensive computations. i.e. if there is 100 EmojiSpans and the first
 * span is deleted, DynamicLayout gets 99 calls about the change of position occurred in the
 * remaining spans. This causes a huge delay in response time.
 * <p>
 * Since "android.text.DynamicLayout$ChangeWatcher" class is not a public class,
 * EmojiEditableFactory checks if the watcher is in the classpath, and if so uses the modified
 * Spannable which reduces the total number of calls to DynamicLayout for operations that affect
 * EmojiSpans.
 *
 * @see SpannableBuilder
 */
final class EmojiEditableFactory extends Editable.Factory {
    private static final Object sInstanceLock = new Object();
    @GuardedBy("sInstanceLock")
    private static volatile Editable.Factory sInstance;

    @Nullable private static Class<?> sWatcherClass;

    @SuppressLint("PrivateApi")
    private EmojiEditableFactory() {
        try {
            String className = "android.text.DynamicLayout$ChangeWatcher";
            sWatcherClass = getClass().getClassLoader().loadClass(className);
        } catch (Throwable t) {
            // ignore
        }
    }

    @SuppressWarnings("GuardedBy")
    public static Editable.Factory getInstance() {
        if (sInstance == null) {
            synchronized (sInstanceLock) {
                if (sInstance == null) {
                    sInstance = new EmojiEditableFactory();
                }
            }
        }
        return sInstance;
    }

    @Override
    public Editable newEditable(@NonNull final CharSequence source) {
        if (sWatcherClass != null) {
            return SpannableBuilder.create(sWatcherClass, source);
        }
        return super.newEditable(source);
    }
}
