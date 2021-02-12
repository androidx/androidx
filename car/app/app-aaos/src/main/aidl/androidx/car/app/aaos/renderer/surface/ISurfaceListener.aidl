package androidx.car.app.aaos.renderer.surface;

import androidx.car.app.aaos.renderer.surface.SurfaceWrapper;

/**
 * A surface event listener interface.
 *
 * @hide
 */
interface ISurfaceListener {
  /**
   * Notifies that the surface has become available.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains information on the surface that
   * has become available.
   */
  void onSurfaceAvailable(in SurfaceWrapper surfaceWrapper) = 1;

  /**
   * Notifies that the surface size has changed.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains the updated information on the
   * surface.
   */
  void onSurfaceChanged(in SurfaceWrapper surfaceWrapper) = 2;
}
