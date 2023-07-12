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
package androidx.com.android.tools.idea.lang.aidl.psi;

import com.android.annotations.NonNull;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * AidlDeclaration is either a {@code AidlParcelableDeclaration}, {@code AidlInterfaceDeclaration},
 * {@link AidlEnumDeclaration}, {@link AidlUnionDeclaration}, or a {@code AidlMethodDeclaration}.
 */
public interface AidlDeclaration extends AidlPsiCompositeElement {
  @NotNull
  AidlNamedElement getDeclarationName();

  @NotNull
  @Override
  String getName();

  @NotNull
  String getQualifiedName();

  /**
   * Get the PSI elements of the generated Java code for this AIDL declaration.
   */
  @NonNull
  PsiElement[] getGeneratedPsiElements();
}
