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
package androidx.com.android.tools.idea.lang.aidl.lexer;

import androidx.com.android.tools.idea.lang.aidl.AidlLanguage;

import com.intellij.psi.tree.IElementType;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AidlTokenType extends IElementType {
  public AidlTokenType(@NotNull @NonNls String debugName) {
    super(debugName, AidlLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    String s = super.toString();
    for (Field field : AidlTokenTypes.class.getDeclaredFields()) {
      try {
        if (field.get(null) == this) {
          return "AidlTokenTypes." + field.getName();
        }
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    return "AidlTokenType." + s;
  }
}
