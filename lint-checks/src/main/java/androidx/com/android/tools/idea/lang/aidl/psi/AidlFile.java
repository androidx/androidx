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

import androidx.com.android.tools.idea.lang.aidl.AidlFileType;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AidlFile extends PsiFileBase {
  private final FileType myFileType;

  public AidlFile(FileViewProvider viewProvider) {
    super(viewProvider, AidlFileType.INSTANCE.getLanguage());
    myFileType = viewProvider.getFileType();
  }

  @Override
  @NotNull
  public FileType getFileType() {
    return myFileType;
  }

  public String getPackageName() {
    AidlPackage packageStatement = PsiTreeUtil.findChildOfType(this, AidlPackage.class);
    if (packageStatement != null) {
      return packageStatement.getQualifiedName().getQualifiedName();
    }
    return "";
  }

  public Collection<AidlImport> getImportStatements() {
    return PsiTreeUtil.findChildrenOfType(this, AidlImport.class);
  }

  @NotNull
  public Collection<AidlDeclaration> getAllAidlDeclarations() {
    // "PsiTreeUtil.findChildrenOfType(this, AidlDeclaration.class)" could do all the following with one call
    // but it needs to traverse all the elements.
    List<AidlDeclaration> results = new ArrayList<>();
    AidlBody body = PsiTreeUtil.getChildOfType(this, AidlBody.class);
    List<AidlDeclaration> topLevels = PsiTreeUtil.getChildrenOfTypeAsList(body, AidlDeclaration.class);
    results.addAll(topLevels);
    for (AidlDeclaration topLevel : topLevels) {
      results.addAll(PsiTreeUtil.getChildrenOfTypeAsList(topLevel, AidlDeclaration.class));
    }
    return results;
  }
}
