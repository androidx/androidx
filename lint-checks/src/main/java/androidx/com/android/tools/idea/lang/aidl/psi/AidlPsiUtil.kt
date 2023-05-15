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
@file:JvmName("AidlPsiUtil")

package androidx.com.android.tools.idea.lang.aidl.psi

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

// Various method implementations called from generated methods referenced in Aidl.bnf
fun getNameIdentifier(element: AidlQualifiedName): PsiElement = element.lastChild
fun getNameIdentifier(element: AidlDottedName): PsiElement = element.lastChild

fun getQualifiedName(element: AidlDottedName): String {
  var declaration = element.getParentOfType<AidlDeclaration>(true) ?: return ""
  var name = declaration.name
  while (true) {
    declaration = declaration.getParentOfType(true) ?: break
    name = "${declaration.name}.$name"
  }
  val pkg = element.containingFile.packageName
  if (pkg.isNotEmpty()) {
    name = "$pkg.$name"
  }
  return name
}

fun getQualifiedName(element: AidlQualifiedName): String = element.text

fun getReference(element: AidlQualifiedName): PsiReference {
  return object : PsiReferenceBase<AidlQualifiedName?>(element) {
    override fun resolve(): PsiElement? {
      return (myElement as AidlQualifiedName).resolve()
    }
  }
}

fun resolve(element: AidlImport): PsiElement? {
  val project = element.project
  val facade = JavaPsiFacade.getInstance(project)
  val scope = GlobalSearchScope.allScope(project)
  return facade.findClass(element.qualifiedName.text, scope)
}

fun resolve(element: AidlQualifiedName): PsiElement? {
  val text = element.qualifiedName
  val project = element.project
  val facade = JavaPsiFacade.getInstance(project)
  val scope = GlobalSearchScope.allScope(project)
  if (text.contains(".")) {
    // Already qualified
    return facade.findClass(text, scope)
  }
  val imports = element.containingFile.importStatements
  for (importStatement in imports) {
    val importedFqn = importStatement.qualifiedName
    val imported = importedFqn.name
    if (text == imported) {
      return importedFqn.resolve()
    }
  }

  // See if it's in the same package
  val typeClass = facade.findClass(element.containingFile.packageName + "." + text, scope)
  if (typeClass != null) {
    return typeClass
  }

  val fqn = when (text) {
    "String" -> "java.lang.String"
    "List" -> "java.util.List"
    "Map" -> "java.util.Map"
    "IBinder" -> "android.os.IBinder"
    "FileDescriptor" -> "java.io.FileDescriptor"
    "CharSequence" -> "java.lang.CharSequence"
    "ParcelFileDescriptor" -> "android.os.ParcelFileDescriptor"
    "ParcelableHolder" -> "android.os.ParcelableHolder"
    else -> return null
  }
  return facade.findClass(fqn, scope)
}