package de.golfgl.gdxgamesvcs;

import com.badlogic.gdx.Gdx;

/**
 * Convinience wrapper for {@link IGameServiceListener} implementations: all calls are made on UI main render thread
 * when using this wrapper
 * <p>
 * Created by Benjamin Schulte on 12.08.2017.
 */

public class GameServiceRenderThreadListener implements IGameServiceListener {

    IGameServiceListener realListener;

    public GameServiceRenderThreadListener(IGameServiceListener listener) {
        realListener = listener;
    }

    @Override
    public void gsOnSessionActive(final GsResultCode resultCode) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                realListener.gsOnSessionActive(resultCode);
            }
        });
    }

    @Override
    public void gsOnSessionInactive(final GsResultCode resultCode) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                realListener.gsOnSessionInactive(resultCode);
            }
        });
    }

    @Override
    public void gsShowErrorToUser(final GsResultCode et, final String msg, final Throwable t) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                realListener.gsShowErrorToUser(et, msg, t);
            }
        });
    }
}
