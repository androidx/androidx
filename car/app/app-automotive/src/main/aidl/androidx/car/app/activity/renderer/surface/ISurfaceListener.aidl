package androidx.car.app.activity.renderer.surface;

import androidx.car.app.serialization.Bundleable;

/**
 * A surface event listener interface.
 *
 * @hide
 */
oneway interface ISurfaceListener {
  /**
   * Notifies that the surface has become available.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains information on the surface that
   * has become available.
   */
  void onSurfaceAvailable(in Bundleable surfaceWrapper) = 1;

  /**
   * Notifies that the surface size has changed.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains the updated information on the
   * surface.
   */
  void onSurfaceChanged(in Bundleable surfaceWrapper) = 2;
}
