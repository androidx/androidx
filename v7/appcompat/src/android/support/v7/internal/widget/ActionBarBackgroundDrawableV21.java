package android.support.v7.internal.widget;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

class ActionBarBackgroundDrawableV21 extends ActionBarBackgroundDrawable {

    public ActionBarBackgroundDrawableV21(ActionBarContainer container) {
        super(container);
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.getOutline(outline);
        }
    }
}
