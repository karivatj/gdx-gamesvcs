package de.golfgl.gdxgamesvcs.leaderboard;

import com.badlogic.gdx.utils.Array;

/**
 * Callback for
 * {@link de.golfgl.gdxgamesvcs.IGameServiceClient#fetchLeaderboardEntries(String, int, boolean, IFetchLeaderBoardEntriesResponseListener)}
 *
 * @author mgsx
 */
public interface IFetchLeaderBoardEntriesResponseListener {
    /**
     * Called when leaderBoard entries are received.
     *
     * @param leaderboardId leaderboardId of the leaderboard that was attempted to fetch
     * @param leaderBoard null if leaderBoard couldn't be fetched.
     */
    void onLeaderBoardResponse(String leaderboardId, Array<ILeaderBoardEntry> leaderBoard);
}
