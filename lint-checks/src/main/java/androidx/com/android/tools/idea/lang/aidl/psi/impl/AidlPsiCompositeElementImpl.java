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
package androidx.com.android.tools.idea.lang.aidl.psi.impl;

import androidx.com.android.tools.idea.lang.aidl.psi.AidlDottedName;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlFile;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlNamedElement;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlPsiCompositeElement;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlQualifiedName;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class AidlPsiCompositeElementImpl extends ASTWrapperPsiElement implements AidlPsiCompositeElement {
  public AidlPsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public IElementType getTokenType() {
    return null;
  }

  @Override
  public AidlFile getContainingFile() {
    return (AidlFile)super.getContainingFile();
  }

  @Override
  public String toString() {
    AidlNamedElement nameElement = findChildByClass(AidlNamedElement.class);
    if (nameElement != null) {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName());
      sb.append("(\"");
      if (nameElement instanceof AidlQualifiedName) {
        sb.append(((AidlQualifiedName)nameElement).getQualifiedName());
      } else if (nameElement instanceof AidlDottedName) {
        sb.append(nameElement.getName()).append("\" as in \"");
        sb.append(((AidlDottedName)nameElement).getQualifiedName());
      } else {
        sb.append(nameElement.getName());
      }
      sb.append("\")");
      return sb.toString();
    }
    return super.toString();
  }
}
