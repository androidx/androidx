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

import com.android.SdkConstants;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * File type for *.aidl files.
 */
public class AidlFileType extends LanguageFileType {
  public static final AidlFileType INSTANCE = new AidlFileType();

  @NonNls public static final String DEFAULT_ASSOCIATED_EXTENSION = SdkConstants.EXT_AIDL;

  private AidlFileType() {
    super(AidlLanguage.INSTANCE);
  }

  @Override
  @NotNull
  @NonNls
  public String getDefaultExtension() {
    return DEFAULT_ASSOCIATED_EXTENSION;
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Android IDL";
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return null;
  }

  @Override
  @NotNull
  @NonNls
  public String getName() {
    return "AIDL";
  }
}
