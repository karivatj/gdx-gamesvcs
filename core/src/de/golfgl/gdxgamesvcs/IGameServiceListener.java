package de.golfgl.gdxgamesvcs;

/**
 * Listener interface for Game Services
 * <p>
 * There is no guarantee that these methods are called on the render thread! Use Gdx.app.postRunnable when necessary!
 * <p>
 * Created by Benjamin Schulte on 26.03.2017.
 */

public interface IGameServiceListener {

    /**
     * Called when game service user session is sucessfully connected
     *
     * @param resultCode GsResultCode which represents the reason of the status change. May be null.
     */
    public void gsOnSessionActive(GsResultCode resultCode);

    /**
     * Called when game service user session has disconnected or a connection attempt failed
     *
     * @param resultCode GsResultCode which represents the reason of the status change. May be null.
     */
    public void gsOnSessionInactive(GsResultCode resultCode);

    /**
     * Called from GameServiceClient to show a message to the user.
     *
     * @param et  error type for your own message
     * @param msg further information, may be null
     * @param t   Throwable causing the problem, may be null
     */
    public void gsShowErrorToUser(GsResultCode et, String msg, Throwable t);

    public enum GsResultCode {
        connectionCancelled,
        connectionExplicitlySignedOut,
        connectionPaused,
        connectionResumed,
        errorLoginFailed,
        errorLogoutFailed,
        errorServiceUnreachable,
        errorInitFailed,
        errorUnknown,
        signedIn,
        signedOut
    }
}
