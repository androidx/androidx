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
package androidx.com.android.tools.idea.lang.aidl.psi.impl

import androidx.com.android.tools.idea.lang.aidl.psi.AidlConstantDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlEnumDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlEnumeratorDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlInterfaceDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlMethodDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlNamedElement
import androidx.com.android.tools.idea.lang.aidl.psi.AidlParcelableDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlUnionDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlVariableDeclaration
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

abstract class AbstractAidlDeclarationImpl(node: ASTNode) : AidlPsiCompositeElementImpl(node),
  AidlDeclaration {
  override fun getName(): String {
    return declarationName.text
  }

  override fun getQualifiedName(): String {
    val prefix = when (this) {
      is AidlMethodDeclaration,
      is AidlVariableDeclaration,
      is AidlEnumeratorDeclaration,
      is AidlConstantDeclaration -> {
        (this.parent as AidlDeclaration).qualifiedName
      }
      else -> {
        containingFile.packageName
      }
    }
    return if (prefix.isEmpty()) name else "$prefix.$name"
  }

  override fun getGeneratedPsiElements(): Array<out PsiElement> {
    val facade = JavaPsiFacade.getInstance(project)
    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return EMPTY_ARRAY
    val moduleScope = GlobalSearchScope.moduleScope(module)
    if (this is AidlInterfaceDeclaration ||
      this is AidlParcelableDeclaration ||
      this is AidlEnumDeclaration ||
      this is AidlUnionDeclaration
    ) {
      val declarationClass = facade.findClass(qualifiedName, moduleScope)
      return declarationClass?.let { arrayOf(it) } ?: EMPTY_ARRAY
    } else if (this is AidlMethodDeclaration) {
      val methodDeclaration: AidlMethodDeclaration = this
      val containingClass = methodDeclaration.parent as AidlDeclaration
      val psiClass = facade.findClass(containingClass.qualifiedName, moduleScope)
      if (psiClass != null) {
        val name = methodDeclaration.name
        // AIDL doesn't support method overloading, so the generated method can be found using only
        // interface name and method name.
        return psiClass.findMethodsByName(name, false)
      }
    } else if (
      this is AidlEnumeratorDeclaration ||
      this is AidlVariableDeclaration ||
      this is AidlConstantDeclaration
    ) {
      val containingClass = this.parent as AidlDeclaration
      val psiClass = facade.findClass(containingClass.qualifiedName, moduleScope)
      if (psiClass != null) {
        if (containingClass is AidlUnionDeclaration) {
          val combined = ArrayList<PsiElement>(3)
          combined.addAll(psiClass.findMethodsByName(name.getter(), false))
          combined.addAll(psiClass.findMethodsByName(name.setter(), false))
          psiClass.findFieldByName(name, false)?.let(combined::add)
          return combined.toTypedArray()
        }
        val field = psiClass.findFieldByName(name, false)
        return field?.let { arrayOf(it) } ?: EMPTY_ARRAY
      }
    } else {
      assert(false) { this.javaClass }
    }
    return EMPTY_ARRAY
  }

  private fun String.getter(): String = "get" + this[0].uppercaseChar() + substring(1)
  private fun String.setter(): String = "set" + this[0].uppercaseChar() + substring(1)

  private fun getNameIdentifier(): PsiElement {
    return declarationName
  }

  override fun getPresentation(): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String {
        return name
      }

      override fun getIcon(unused: Boolean): Icon {
        return object : Icon {
          override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            // Nothing.
          }

          override fun getIconWidth(): Int = 0

          override fun getIconHeight(): Int = 0
        }
      }
    }
  }

  override fun getDeclarationName(): AidlNamedElement {
    return findChildByClass(AidlNamedElement::class.java)!!
  }
}