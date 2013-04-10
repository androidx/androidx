package android.support.v4.media;

public class TransportStateListener {
    /**
     * The play state of the transport changed.  Use
     * {@link android.support.v4.media.TransportController#isPlaying()
     * TransportController.isPlaying()} to determine the new state.
     */
    public void onPlayingChanged(TransportController controller) {
    }

    /**
     * The available controls of the transport changed.  Use
     * {@link TransportController#getTransportControlFlags()}
     * TransportController.getTransportControlFlags()} to determine the new state.
     */
    public void onTransportControlsChanged(TransportController controller) {
    }
}
