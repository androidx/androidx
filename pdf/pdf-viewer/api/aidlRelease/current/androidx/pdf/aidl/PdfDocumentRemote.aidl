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

package androidx.pdf.aidl;
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface PdfDocumentRemote {
  int create(in ParcelFileDescriptor pfd, String password);
  int numPages();
  androidx.pdf.aidl.Dimensions getPageDimensions(int pageNum);
  boolean renderPage(int pageNum, in androidx.pdf.aidl.Dimensions size, boolean hideTextAnnots, in ParcelFileDescriptor output);
  boolean renderTile(int pageNum, int pageWidth, int pageHeight, int left, int top, in androidx.pdf.aidl.Dimensions tileSize, boolean hideTextAnnots, in ParcelFileDescriptor output);
  String getPageText(int pageNum);
  List<String> getPageAltText(int pageNum);
  androidx.pdf.aidl.MatchRects searchPageText(int pageNum, String query);
  androidx.pdf.aidl.PageSelection selectPageText(int pageNum, in androidx.pdf.aidl.SelectionBoundary start, in androidx.pdf.aidl.SelectionBoundary stop);
  androidx.pdf.aidl.LinkRects getPageLinks(int pageNum);
  byte[] getPageGotoLinksByteArray(int pageNum);
  boolean isPdfLinearized();
  boolean cloneWithoutSecurity(in ParcelFileDescriptor destination);
  boolean saveAs(in ParcelFileDescriptor destination);
}
