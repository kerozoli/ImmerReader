package com.keroleap.immerreader.Service;

/**
 * Thrown when a fresh frame cannot be obtained from a camera.
 *
 * <p>The {@link #isTimedOut()} flag lets schedulers distinguish a camera that
 * did not answer in time ({@code ErrorType.TIMEOUT}) from a camera that answered
 * with an error or produced an undecodable payload ({@code ErrorType.FETCH_ERROR}).
 */
public class CameraUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean timedOut;

    public CameraUnavailableException(String message, boolean timedOut) {
        super(message);
        this.timedOut = timedOut;
    }

    public CameraUnavailableException(String message, boolean timedOut, Throwable cause) {
        super(message, cause);
        this.timedOut = timedOut;
    }

    public boolean isTimedOut() {
        return timedOut;
    }
}
