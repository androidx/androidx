package androidx.wear.protolayout.renderer.inflater;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.lang.ref.WeakReference;

// Android's normal ImageSpan (well, DynamicDrawableSpan) applies baseline alignment incorrectly
// in some cases. It incorrectly assumes that the difference between the bottom (as passed to
// draw) and baseline of the text is always equal to the font descent, when that doesn't always
// hold. Instead, the "y" parameter is the Y coordinate of the baseline, so base the baseline
// alignment on that rather than "bottom".
class FixedImageSpan extends ImageSpan {
  @Nullable private WeakReference<Drawable> mDrawableRef;

  FixedImageSpan(@NonNull Drawable drawable) {
    super(drawable);
  }

  FixedImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
    super(drawable, verticalAlignment);
  }

  @Override
  public void draw(
      @NonNull Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      @NonNull Paint paint) {
    Drawable b = getCachedDrawable();
    canvas.save();

    int transY = bottom - b.getBounds().bottom;
    if (mVerticalAlignment == ALIGN_BASELINE) {
      transY = y - b.getBounds().bottom;
    } else if (mVerticalAlignment == ALIGN_CENTER) {
      transY = (bottom - top) / 2 - b.getBounds().height() / 2;
    }

    canvas.translate(x, transY);
    b.draw(canvas);
    canvas.restore();
  }

  @VisibleForTesting
  Drawable getCachedDrawable() {
    WeakReference<Drawable> wr = mDrawableRef;
    Drawable d = null;

    if (wr != null) {
      d = wr.get();
    }

    if (d == null) {
      d = getDrawable();
      mDrawableRef = new WeakReference<>(d);
    }

    return d;
  }
}
