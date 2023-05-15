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
package androidx.com.android.tools.idea.lang.aidl;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NonNls;

/**
 * Android IDL Language.
 */
public class AidlLanguage extends Language {
  private static final Object INSTANCE_LOCK = new Object();

  public static final Language INSTANCE = getOrCreate();

  private static Language getOrCreate() {
    // The Language class is not thread-safe, so this is a best-effort to avoid a race condition
    // during our own access across multiple lint worker threads.
    synchronized (INSTANCE_LOCK) {
      Language lang = Language.findLanguageByID(ID);
      if (lang != null) {
        return lang;
      }
      return new AidlLanguage();
    }
  }

  @NonNls private static final String ID = "AIDL";

  private AidlLanguage() {
    super(ID);
  }
}
