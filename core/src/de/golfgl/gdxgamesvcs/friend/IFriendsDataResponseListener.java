package de.golfgl.gdxgamesvcs.friend;

import java.util.ArrayList;
import java.util.HashMap;

import de.golfgl.gdxgamesvcs.IGameServiceClient;
import de.golfgl.gdxgamesvcs.player.IPlayerDataResponseListener;

/**
 * Callback for
 * {@link IGameServiceClient#showFriends(IFriendsDataResponseListener)}
 *
 * @author Kari Vatjus-Anttila
 */
public interface IFriendsDataResponseListener {
    void onFriendsDataResponse(HashMap<String, String> friendsData);
}
