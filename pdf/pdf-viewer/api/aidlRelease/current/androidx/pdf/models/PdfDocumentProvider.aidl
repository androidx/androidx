/*
 * Copyright (C) 2024 The Android Open Source Project
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
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package androidx.pdf.models;
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface PdfDocumentProvider {
  int openPdfDocument(in ParcelFileDescriptor pfd, String password);
  int numPages();
  androidx.pdf.models.Dimensions getPageDimensions(int pageNum);
  android.graphics.Bitmap getPageBitmap(int pageNum, int width, int height);
  android.graphics.Bitmap getTileBitmap(int pageNum, int tilewidth, int tileHeight, int pageWidth, int pageHeight, int offsetX, int offsetY);
  List<android.graphics.pdf.content.PdfPageTextContent> getPageText(int pageNum);
  List<android.graphics.pdf.models.PageMatchBounds> searchPageText(int pageNum, String query);
  android.graphics.pdf.models.selection.PageSelection selectPageText(int pageNum, in android.graphics.pdf.models.selection.SelectionBoundary start, in android.graphics.pdf.models.selection.SelectionBoundary stop);
  List<android.graphics.pdf.content.PdfPageLinkContent> getPageExternalLinks(int pageNum);
  List<android.graphics.pdf.content.PdfPageGotoLinkContent> getPageGotoLinks(int pageNum);
  List<android.graphics.pdf.content.PdfPageImageContent> getPageImageContent(int pageNum);
  boolean isPdfLinearized();
  int getFormType();
  void releasePage(int pageNum);
  void closePdfDocument();
}
