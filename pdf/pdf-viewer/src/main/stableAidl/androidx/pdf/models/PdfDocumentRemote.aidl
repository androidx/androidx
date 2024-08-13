package androidx.pdf.models;

import android.graphics.Rect;
import android.graphics.Bitmap;

import android.os.ParcelFileDescriptor;

import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.models.LinkRects;

/** Remote interface around a PdfDocument. */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface PdfDocumentRemote {
    int create(in ParcelFileDescriptor pfd, String password);

    int numPages();
    Dimensions getPageDimensions(int pageNum);

    Bitmap renderPage(int pageNum, int pageWidth, int pageHeight, boolean hideTextAnnots);
    Bitmap renderTile(int pageNum, int tileWidth, int tileHeight, int scaledPageWidth,
      int scaledPageHeight, int left, int top, boolean hideTextAnnots);

    String getPageText(int pageNum);
    List<String> getPageAltText(int pageNum);

    MatchRects searchPageText(int pageNum, String query);
    PageSelection selectPageText(int pageNum, in SelectionBoundary start, in SelectionBoundary stop);

    LinkRects getPageLinks(int pageNum);

    List<GotoLink> getPageGotoLinks(int pageNum);

    boolean isPdfLinearized();
    int getFormType();

    boolean cloneWithoutSecurity(in ParcelFileDescriptor destination);

    boolean saveAs(in ParcelFileDescriptor destination);

    void releasePage(int pageNum);

    // The PdfDocument is destroyed when this service is destroyed.
}