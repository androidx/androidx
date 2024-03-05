package androidx.pdf.aidl;

import android.graphics.Rect;

import android.os.ParcelFileDescriptor;
import androidx.pdf.aidl.Dimensions;
import androidx.pdf.aidl.MatchRects;
import androidx.pdf.aidl.PageSelection;
import androidx.pdf.aidl.SelectionBoundary;
import androidx.pdf.aidl.LinkRects;

/** Remote interface around a PdfDocument. */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface PdfDocumentRemote {
    int create(in ParcelFileDescriptor pfd, String password);

    int numPages();
    Dimensions getPageDimensions(int pageNum);

    boolean renderPage(int pageNum, in Dimensions size, boolean hideTextAnnots,
      in ParcelFileDescriptor output);
    boolean renderTile(int pageNum, int pageWidth, int pageHeight, int left, int top,
      in Dimensions tileSize, boolean hideTextAnnots, in ParcelFileDescriptor output);

    String getPageText(int pageNum);
    List<String> getPageAltText(int pageNum);

    MatchRects searchPageText(int pageNum, String query);
    PageSelection selectPageText(int pageNum, in SelectionBoundary start, in SelectionBoundary stop);

    LinkRects getPageLinks(int pageNum);

    byte[] getPageGotoLinksByteArray(int pageNum);

    boolean isPdfLinearized();

    boolean cloneWithoutSecurity(in ParcelFileDescriptor destination);

    boolean saveAs(in ParcelFileDescriptor destination);

    // The PdfDocument is destroyed when this service is destroyed.
}