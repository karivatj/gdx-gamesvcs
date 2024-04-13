package de.golfgl.gdxgamesvcs;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingConfig;
import com.android.billingclient.api.BillingConfigResponseListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.GetBillingConfigParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.FriendsResolutionRequiredException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.golfgl.gdxgamesvcs.achievement.IAchievement;
import de.golfgl.gdxgamesvcs.achievement.IFetchAchievementsResponseListener;
import de.golfgl.gdxgamesvcs.country.ICountryCodeResponseListener;
import de.golfgl.gdxgamesvcs.friend.IFriendsDataResponseListener;
import de.golfgl.gdxgamesvcs.gamestate.IFetchGameStatesListResponseListener;
import de.golfgl.gdxgamesvcs.gamestate.ILoadGameStateResponseListener;
import de.golfgl.gdxgamesvcs.gamestate.ISaveGameStateResponseListener;
import de.golfgl.gdxgamesvcs.leaderboard.IFetchLeaderBoardEntriesResponseListener;
import de.golfgl.gdxgamesvcs.leaderboard.ILeaderBoardEntry;
import de.golfgl.gdxgamesvcs.player.IPlayerDataResponseListener;
import de.golfgl.gdxgamesvcs.player.PlayerData;

/**
 * Client for Google Play Games
 * <p>
 * Refactored by Kari Vatjus-Anttila on 27.11.2021
 * Based on code made by Benjamin Schulte on 26.03.2017.
 */

public class GpgsClient implements
        IGameServiceClient,
        AndroidEventListener,
        BillingClientStateListener,
        PurchasesUpdatedListener {

    public static final String GAMESERVICE_ID = IGameServiceClient.GS_GOOGLEPLAYGAMES_ID;

    public static final int RC_LEADERBOARD = 9002;
    public static final int RC_ACHIEVEMENTS = 9003;
    public static final int RC_SHOW_SHARING_FRIENDS_CONSENT = 9004;
    public static final int RC_SHOW_PROFILE = 9005;

    private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 10;

    protected Activity myContext;

    private BillingClient mBillingClient;

    protected boolean forceReload;

    private boolean billingClientReady;
    private boolean isConnected;
    private boolean isInitialized;

    protected IGameServiceListener gameListener;
    protected IGameServiceIdMapper<String> gpgsLeaderboardIdMapper;
    protected IGameServiceIdMapper<String> gpgsAchievementIdMapper;
    private IFriendsDataResponseListener friendsDataResponseListener;

    private String mPlayerDisplayName;
    private String mServerAuthCode;
    private String mWebClientId;

    /**
     * sets up the mapper for leader board ids
     *
     * @param gpgsLeaderboardIdMapper Id mapper
     * @return this for method chaining
     */
    public GpgsClient setGpgsLeaderboardIdMapper(IGameServiceIdMapper<String> gpgsLeaderboardIdMapper) {
        this.gpgsLeaderboardIdMapper = gpgsLeaderboardIdMapper;
        return this;
    }

    /**
     * sets up the mapper for leader achievement ids
     *
     * @param gpgsAchievementIdMapper Id mapper
     * @return this for method chaining
     */
    public GpgsClient setGpgsAchievementIdMapper(IGameServiceIdMapper<String> gpgsAchievementIdMapper) {
        this.gpgsAchievementIdMapper = gpgsAchievementIdMapper;
        return this;
    }

    /**
     * set to true if you want to force refreshes when fetching data
     */
    public void setFetchForceRefresh(boolean forceRefresh) {
        this.forceReload = forceRefresh;
    }

    /**
     * Initializes the GoogleApiClient. Give your main AndroidLauncher as context.
     * <p>
     *
     * @param context         your AndroidLauncher class
     * @param webClientId     optional client id if one wants to authenticate to Firebase later on with Google Play account
     *                        See: https://firebase.google.com/docs/auth/android/play-games#integrate_play_games_sign-in_into_your_game
     * @return this for method chaining
     */
    public GpgsClient initialize(AndroidApplication context, String webClientId) {

        if (isInitialized)
            throw new IllegalStateException("Already initialized.");

        myContext = context;

        // We need to receive onActivityResult
        context.addAndroidEventListener(this);

        billingClientReady = false;
        mBillingClient = BillingClient.newBuilder(myContext).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(this);

        isConnected = false;
        PlayGamesSdk.initialize(myContext);

        mWebClientId = webClientId;

        isInitialized = true;

        return this;
    }

    /**
     * Initializes the GoogleApiClient. Give your main AndroidLauncher as context.
     * <p>
     *
     * @param context -your AndroidLauncher class
     * @return this for method chaining
     */
    public GpgsClient initialize(AndroidApplication context) {

        if (isInitialized)
            throw new IllegalStateException("Already initialized.");

        myContext = context;

        // We need to receive onActivityResult
        context.addAndroidEventListener(this);

        billingClientReady = false;
        mBillingClient = BillingClient.newBuilder(myContext).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(this);

        isConnected = false;
        PlayGamesSdk.initialize(myContext);

        mWebClientId = null;

        isInitialized = true;

        return this;
    }

    private GoogleSignInAccount getSignInAccount() {
        return GoogleSignIn.getLastSignedInAccount(myContext);
    }

    @Override
    public void setListener(IGameServiceListener gameListener) {
        this.gameListener = gameListener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED &&
                (requestCode == RC_LEADERBOARD || requestCode == RC_ACHIEVEMENTS)) {
            Gdx.app.error(GAMESERVICE_ID, "Reconnect required. User needs to sign in again.");
        } else if (requestCode == RC_SHOW_SHARING_FRIENDS_CONSENT) {
            if (resultCode == Activity.RESULT_OK) {
                showFriends(friendsDataResponseListener);
            } else {
                Gdx.app.error(GAMESERVICE_ID, "User did not give consent to access friends.");
            }
        }
    }

    @Override
    public String getGameServiceId() {
        return GAMESERVICE_ID;
    }

    @Override
    public String getServerAuthCode() {
        return mServerAuthCode;
    }


    private final OnCompleteListener<AuthenticationResult> checkAuthentication = new OnCompleteListener<AuthenticationResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthenticationResult> task) {
            if (task.isSuccessful() && task.getResult().isAuthenticated()) {
                // Continue with Play Games Services
                Gdx.app.log(GAMESERVICE_ID, "Successfully signed in to Google Play Services.");
                isConnected = true;
                PlayGames.getPlayersClient(myContext).getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
                    @Override
                    public void onComplete(@NonNull Task<Player> task) {
                        if (task.isSuccessful()) {
                            Gdx.app.log(GAMESERVICE_ID, "Successfully fetched player id: " + task.getResult().getDisplayName());
                            mPlayerDisplayName = task.getResult().getDisplayName();
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "There was an issue communicating with the server while fetching player id. Exception: " + task.getException());
                            mPlayerDisplayName = null;
                        }
                    }
                });

                PlayGames.getGamesSignInClient(myContext).requestServerSideAccess(mWebClientId, false).addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            Gdx.app.log(GAMESERVICE_ID, "Successfully fetched server auth code.");
                            mServerAuthCode = task.getResult();
                            gameListener.gsOnSessionActive();
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "There was an issue communicating with the server while fetching server auth code. Exception: " + task.getException());
                            mServerAuthCode = null;
                            gameListener.gsOnSessionInactive();
                        }
                    }
                });
            } else {
                Gdx.app.error(GAMESERVICE_ID, "Unable to sign in. User must sign in manually. Exception: " + task.getException());
                mPlayerDisplayName = null;
                mServerAuthCode =  null;
                isConnected = false;
            }
        }
    };

    @Override
    public boolean resumeSession() {
        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        PlayGames.getGamesSignInClient(myContext).isAuthenticated()
                .addOnCompleteListener(checkAuthentication);

        return true;
    }

    @Override
    public boolean logIn() {
        if (isSessionActive()) {
            return true;
        }

        PlayGames.getGamesSignInClient(myContext).signIn()
                .addOnCompleteListener(checkAuthentication);

        return true;
    }

    @Override
    public void logOff() {
        // Google Play Games V2 API has no sign out method
    }

    @Override
    public void pauseSession() {
    }

    @Override
    public String getPlayerDisplayName() {
        return null;
    }

    @Override
    public boolean getPlayerData(final IPlayerDataResponseListener callback) {
        if (isSessionActive()) {
            PlayGames.getPlayersClient(myContext).getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
                @Override
                public void onComplete(@NonNull Task<Player> task) {
                    if (task.isSuccessful()) {
                        PlayerData playerData = new PlayerData();
                        Player player = task.getResult();

                        playerData.playerId = player.getPlayerId();
                        playerData.displayName = player.getDisplayName();
                        playerData.title = player.getTitle();
                        playerData.name = "";

                        if (callback != null)
                            callback.onPlayerDataResponse(playerData);
                    } else {
                        Gdx.app.error(GAMESERVICE_ID, "Failed to get player display name: " + task.getException().getMessage());
                    }
                }
            });
            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not get player data. Session is not active");
            return false;
        }
    }

    @Override
    public boolean isSessionActive() {
        return isConnected;
    }

    @Override
    public boolean isConnectionPending() {
        //Connections are handled behind the scenes. No need for this anymore. Return false by default.
        return false;
    }

    @Override
    public void showLeaderboards(String leaderBoardId) throws GameServiceException {
        if (isSessionActive()) {
            if (gpgsLeaderboardIdMapper != null)
                leaderBoardId = gpgsLeaderboardIdMapper.mapToGsId(leaderBoardId);

            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(myContext);

            if (leaderBoardId != null) {
                leaderboardsClient.getLeaderboardIntent(leaderBoardId).addOnCompleteListener(new OnCompleteListener<Intent>() {
                    @Override
                    public void onComplete(@NonNull Task<Intent> task) {
                        if (task.isSuccessful()) {
                            myContext.startActivityForResult(task.getResult(), RC_LEADERBOARD);
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to startup leaderboards activity");
                        }
                    }
                });
            } else {
                leaderboardsClient.getAllLeaderboardsIntent().addOnCompleteListener(new OnCompleteListener<Intent>() {
                    @Override
                    public void onComplete(@NonNull Task<Intent> task) {
                        if (task.isSuccessful()) {
                            myContext.startActivityForResult(task.getResult(), RC_LEADERBOARD);
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to startup leaderboards activity");
                        }
                    }
                });
            }
        } else {
            throw new GameServiceException.NoSessionException();
        }
    }

    @Override
    public void showAchievements() throws GameServiceException {
        if (isSessionActive()) {
            PlayGames.getAchievementsClient(myContext).getAchievementsIntent().addOnCompleteListener(new OnCompleteListener<Intent>() {
                @Override
                public void onComplete(@NonNull Task<Intent> task) {
                    if (task.isSuccessful()) {
                        myContext.startActivityForResult(task.getResult(), RC_ACHIEVEMENTS);
                    } else {
                        Gdx.app.error(GAMESERVICE_ID, "Failed to startup achievements activity");
                    }
                }
            });
        } else {
            throw new GameServiceException.NoSessionException();
        }
    }

    @Override
    public boolean fetchAchievements(final IFetchAchievementsResponseListener callback) {
        if(isSessionActive()) {
            PlayGames.getAchievementsClient(myContext).load(forceReload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<AchievementBuffer>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<AchievementBuffer>> task) {
                    if (task.isSuccessful()) {
                        AchievementBuffer achievementsResult = task.getResult().get();

                        if (achievementsResult != null) {
                            Array<IAchievement> gpgsAchs = new Array<>(achievementsResult.getCount());

                            for (Achievement ach : achievementsResult) {
                                GpgsAchievement gpgsAchievement = new GpgsAchievement();
                                gpgsAchievement.achievementId = ach.getAchievementId();
                                gpgsAchievement.achievementMapper = gpgsAchievementIdMapper;
                                gpgsAchievement.description = ach.getDescription();
                                gpgsAchievement.title = ach.getName();

                                if (ach.getState() == Achievement.STATE_UNLOCKED)
                                    gpgsAchievement.percCompl = 1f;
                                else if (ach.getType() == Achievement.TYPE_INCREMENTAL)
                                    gpgsAchievement.percCompl = (float) ach.getCurrentSteps() / ach.getTotalSteps();

                                gpgsAchs.add(gpgsAchievement);
                            }

                            achievementsResult.release();
                            if (callback != null)
                                callback.onFetchAchievementsResponse(gpgsAchs);
                        }
                    } else {
                        Gdx.app.log(GAMESERVICE_ID, "Failed to fetch achievements: " + (task.getException() == null ? "Unknown error" : task.getException().getMessage()));
                        if (callback != null)
                            callback.onFetchAchievementsResponse(null);
                    }
                }
            });

            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Cannot fetch achievements. Session is not active");
            if (callback != null)
                callback.onFetchAchievementsResponse(null);
            return false;
        }
    }

    @Override
    public boolean submitToLeaderboard(String leaderboardId, long score, String tag) {
        if(isSessionActive()) {
            if (gpgsLeaderboardIdMapper != null)
                leaderboardId = gpgsLeaderboardIdMapper.mapToGsId(leaderboardId);

            if (leaderboardId == null)
                return false;

            if (tag != null) {
                PlayGames.getLeaderboardsClient(myContext).submitScore(leaderboardId, score, tag);
            } else {
                PlayGames.getLeaderboardsClient(myContext).submitScore(leaderboardId, score);
            }

            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not submit scores to leaderboard. Session is not active");
            return false;
        }
    }

    @Override
    public boolean incrementLeaderboard(final String leaderboardId, final long increment) {
        if (isSessionActive()) {
            final LeaderboardsClient mLeaderboardsClient = PlayGames.getLeaderboardsClient(myContext);

            mLeaderboardsClient.loadCurrentPlayerLeaderboardScore(
                    leaderboardId,
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC
            ).addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                @Override
                public void onSuccess(AnnotatedData<LeaderboardScore> leaderboardScoreAnnotatedData) {
                    if (leaderboardScoreAnnotatedData.get() == null)
                        mLeaderboardsClient.submitScore(leaderboardId, increment);
                    else {
                        long currentScore = leaderboardScoreAnnotatedData.get().getRawScore();
                        mLeaderboardsClient.submitScore(leaderboardId, currentScore + increment);
                    }
                }
            });

            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not increment scores to leaderboard. Session is not active");
            return false;
        }
    }

    @Override
    public boolean fetchLeaderboardEntries(String leaderBoardId, int limit, boolean relatedToPlayer, IFetchLeaderBoardEntriesResponseListener callback) {
        return false;
    }

    @Override
    public boolean fetchLeaderboardEntries(final String origLeaderboardId, int limit,
                                           boolean relatedToPlayer,
                                           final IFetchLeaderBoardEntriesResponseListener callback,
                                           int timespan, int collection) {
        if (isSessionActive()) {
            String resolvedLeaderboardId;

            if (gpgsLeaderboardIdMapper != null)
                resolvedLeaderboardId = gpgsLeaderboardIdMapper.mapToGsId(origLeaderboardId);
            else
                resolvedLeaderboardId = origLeaderboardId;

            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(myContext);

            //Validate timespan and collection
            timespan = timespan == 0 ? LeaderboardVariant.TIME_SPAN_DAILY : timespan == 1 ? LeaderboardVariant.TIME_SPAN_WEEKLY : LeaderboardVariant.TIME_SPAN_ALL_TIME;
            collection = collection == 0 ? LeaderboardVariant.COLLECTION_PUBLIC : LeaderboardVariant.COLLECTION_FRIENDS;

            final String leaderboardId = resolvedLeaderboardId;

            if (relatedToPlayer) {
                leaderboardsClient.loadPlayerCenteredScores(leaderboardId,
                        timespan,
                        collection,
                        MathUtils.clamp(limit, 1, 25), forceReload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
                    @Override
                    public void onComplete(@NonNull Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> task) {
                        if (task.isSuccessful()) {
                            Gdx.app.log(GAMESERVICE_ID, "Leaderboard entries retrieved successfully");
                            processLeaderboardResults(origLeaderboardId, task.getResult().get().getScores(), callback);
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to get leaderboard entries: " + task.getException().getMessage());
                            processLeaderboardResults(origLeaderboardId, null, callback);
                        }
                    }
                });
            } else {
                leaderboardsClient.loadTopScores(leaderboardId,
                        timespan,
                        collection,
                        MathUtils.clamp(limit, 1, 25), forceReload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
                    @Override
                    public void onComplete(@NonNull Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> task) {
                        if (task.isSuccessful()) {
                            Gdx.app.error(GAMESERVICE_ID, "Leaderboard entries retrieved successfully");
                            processLeaderboardResults(origLeaderboardId, task.getResult().get().getScores(), callback);
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to get leaderboard entries: " + task.getException().getMessage());
                            processLeaderboardResults(origLeaderboardId, null, callback);
                        }
                    }
                });
            }
            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not fetch scores from leaderboards. Session is not active");
            callback.onLeaderBoardResponse(origLeaderboardId, null);
            return false;
        }
    }

    private void processLeaderboardResults(String leaderboardId, LeaderboardScoreBuffer scores, IFetchLeaderBoardEntriesResponseListener callback) {
        if(callback != null) {
            if (scores != null && scores.getCount() > 0) {
                Array<ILeaderBoardEntry> gpgsLbEs = new Array<>(scores.getCount());
                Gdx.app.log(GAMESERVICE_ID, "Leaderboard entries size: " + scores.getCount());
                for (LeaderboardScore score : scores) {
                    GpgsLeaderBoardEntry gpgsLbE = new GpgsLeaderBoardEntry();
                    Gdx.app.log(GAMESERVICE_ID, "UserDisplayName: " + gpgsLbE.userDisplayName + " player: " + mPlayerDisplayName);
                    gpgsLbE.userDisplayName = score.getScoreHolderDisplayName();
                    gpgsLbE.currentPlayer = gpgsLbE.userDisplayName.equalsIgnoreCase(mPlayerDisplayName);
                    gpgsLbE.formattedValue = score.getDisplayScore();
                    gpgsLbE.scoreRank = score.getDisplayRank();
                    gpgsLbE.userId = score.getScoreHolder().getPlayerId();
                    gpgsLbE.sortValue = score.getRawScore();
                    gpgsLbE.scoreTag = score.getScoreTag();
                    gpgsLbEs.add(gpgsLbE);
                }
                scores.release();
                callback.onLeaderBoardResponse(leaderboardId, gpgsLbEs);
            } else {
                callback.onLeaderBoardResponse(leaderboardId,  null);
            }
        }
    }

    @Override
    public boolean submitEvent(String eventId, int increment) {
        if (isSessionActive()) {
            PlayGames.getEventsClient(myContext).increment(eventId, increment);
            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not submit event. Session is not active");
            return false;
        }
    }

    @Override
    public boolean unlockAchievement(String achievementId) {
        if (isSessionActive()) {
            if (gpgsAchievementIdMapper != null)
                achievementId = gpgsAchievementIdMapper.mapToGsId(achievementId);

            if (achievementId != null) {
                PlayGames.getAchievementsClient(myContext).unlock(achievementId);
                return true;
            } else {
                return false;
            }
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not unlock achievement. Session is not active");
            return false;
        }
    }

    @Override
    public boolean incrementAchievement(String achievementId, int incNum, float completionPercentage) {
        if (isSessionActive()) {
            if (gpgsAchievementIdMapper != null)
                achievementId = gpgsAchievementIdMapper.mapToGsId(achievementId);

            if (achievementId != null) {
                PlayGames.getAchievementsClient(myContext).increment(achievementId, incNum);
                return true;
            } else {
                return false;
            }
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not increment achievement. Session is not active");
            return false;
        }
    }

    /**
     * Get list of friends from the Friends API. Display a Intent if permission
     * is needed to access the API.
     */
    public void showFriends(final IFriendsDataResponseListener callback) {
        if (isSessionActive()) {
            getFriendsList(callback);
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not show friends. Session is not active");
            if (callback != null)
                callback.onFriendsDataResponse(null);
        }
    }

    private void getFriendsList(final IFriendsDataResponseListener callback) {
        int PAGE_SIZE = 20;
        this.friendsDataResponseListener = callback;
        PlayGames.getPlayersClient(myContext)
                .loadFriends(PAGE_SIZE, /* forceReload= */ false)
                .addOnSuccessListener(
                        new OnSuccessListener<AnnotatedData<PlayerBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<PlayerBuffer> data) {
                                PlayerBuffer playerBuffer = data.get();
                                HashMap<String, String> friendsData = new HashMap();
                                for (Player player : playerBuffer) {
                                    String displayName = player.getDisplayName();
                                    String playerId = player.getPlayerId();
                                    friendsData.put(playerId, displayName);
                                }
                                playerBuffer.release();
                                callback.onFriendsDataResponse(friendsData);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                if (exception instanceof FriendsResolutionRequiredException) {
                                    PendingIntent pendingIntent =
                                            ((FriendsResolutionRequiredException) exception)
                                                    .getResolution();
                                    try {
                                        myContext.startIntentSenderForResult(
                                                pendingIntent.getIntentSender(),
                                                /* requestCode */ RC_SHOW_SHARING_FRIENDS_CONSENT, // replace with your request code
                                                /* fillInIntent */ null,
                                                /* flagsMask */ 0,
                                                /* flagsValues */ 0,
                                                /* extraFlags */ 0,
                                                /* options */ null);
                                    } catch (IntentSender.SendIntentException e) {
                                        Gdx.app.error(GAMESERVICE_ID, "Failed to start intent sender: " + e.getMessage());
                                    }
                                }
                            }
                        });
    }


    /**
     * Retrieve and launch an Intent to show a player profile within the game.
     */
    @Override
    public void showPlayerProfile(String playerId) {
        PlayGames.getPlayersClient(myContext)
                .getCompareProfileIntent(playerId)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent  intent) {
                        startActivityForResult(myContext, intent, RC_SHOW_PROFILE, null);
                    }});
    }

    /**
     * Show a player profile within the game, with additional hints containing the
     * game-specific names for both players.
     *
     * @param otherPlayerId The Play Games playerId of the player to view.
     * @param otherPlayerInGameName The game-specific name of the player being viewed.
     * @param currentPlayerInGameName The game-specific name of the player who is signed
     *                                in. Hence if the player sends an invitation to the profile they are viewing,
     *                                their game-specific name can be included.
     */
    @Override
    public void showPlayerProfileWithHints(String otherPlayerId, String otherPlayerInGameName, String currentPlayerInGameName) {
        PlayGames.getPlayersClient(myContext)
                .getCompareProfileIntentWithAlternativeNameHints(otherPlayerId, otherPlayerInGameName, currentPlayerInGameName)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent  intent) {
                        startActivityForResult(myContext, intent, RC_SHOW_PROFILE, null);
                    }});
    }

    /**
     * Override this method if you need to set some meta data, for example the description which is displayed
     * in the Play Games app
     *
     * @param metaDataBuilder builder for savegame metadata
     * @param id              snapshot id
     * @param gameState       gamestate data
     * @param progressValue   gamestate progress value
     * @return changed meta data builder
     */
    protected SnapshotMetadataChange.Builder setSaveGameMetaData(SnapshotMetadataChange.Builder metaDataBuilder,
                                                                 String id, byte[] gameState, long progressValue) {
        return metaDataBuilder.setProgressValue(progressValue);
    }

    @Override
    public void saveGameState(final String fileId, final byte[] gameState, final long progressValue,
                              final ISaveGameStateResponseListener callback) {
        if (isSessionActive()) {

            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(myContext);

            // Open the snapshot, creating if necessary
            snapshotsClient.open(fileId, true).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                    if (task.isSuccessful()) {
                        try {
                            processSnapshotOpenResult(task.getResult(), 0).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<Snapshot> task) {
                                    if (task.isSuccessful()) {
                                        Snapshot snapshot = task.getResult();
                                        if (snapshot == null) {
                                            Gdx.app.log(GAMESERVICE_ID, "Could not open snapshot due to conflicts");
                                            if (callback != null)
                                                callback.onGameStateSaved(false, "Could not open snapshot due to conflicts");
                                        } else {
                                            saveSnapshotProgress(snapshot, fileId, gameState, progressValue, callback);
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to open game state: " + task.getException().getMessage());
                        }
                    } else {
                        Gdx.app.error(GAMESERVICE_ID, "Failed to open game state: " + task.getException().getMessage());
                    }
                }
            });
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not save game state. Session is not active");
            if (callback != null)
                callback.onGameStateSaved(false, "Session not active");
        }
    }

    public void saveSnapshotProgress(Snapshot snapshot, String id, final byte[] gameState, long progressValue, final ISaveGameStateResponseListener callback) {
        if (progressValue < snapshot.getMetadata().getProgressValue()) {
            Gdx.app.error(GAMESERVICE_ID, "Progress of saved game state higher than current one. Did not save.");
            if (callback != null)
                callback.onGameStateSaved(true, null);
            return;
        }

        // Write the new data to the snapshot
        snapshot.getSnapshotContents().writeBytes(gameState);

        // Change metadata
        SnapshotMetadataChange.Builder metaDataBuilder = new SnapshotMetadataChange.Builder()
                .fromMetadata(snapshot.getMetadata());
        metaDataBuilder = setSaveGameMetaData(metaDataBuilder, id, gameState, progressValue);
        SnapshotMetadataChange metadataChange = metaDataBuilder.build();

        PlayGames.getSnapshotsClient(myContext).commitAndClose(snapshot, metadataChange).addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
            @Override
            public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                if (task.isSuccessful()) {
                    Gdx.app.log(GAMESERVICE_ID, "Successfully saved game state with " + gameState.length + "B");
                    if (callback != null)
                        callback.onGameStateSaved(true, null);
                } else {
                    Gdx.app.error(GAMESERVICE_ID, "Error while saving game state: " + task.getException().getMessage());
                    throw new RuntimeException(task.getException().getMessage());
                }
            }
        });
    }

    @Override
    public void loadGameState(final String id, final ILoadGameStateResponseListener callback) {
        if (isSessionActive()) {

            final SnapshotsClient client = PlayGames.getSnapshotsClient(myContext);

            // Open the snapshot, creating if necessary
            client.open(id, true).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask) {
                    if (openTask.isSuccessful()) {
                        try {
                            processSnapshotOpenResult(openTask.getResult(), 0).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<Snapshot> task) {
                                    if (task.isSuccessful()) {
                                        Snapshot snapshot = task.getResult();
                                        if (snapshot == null) {
                                            Gdx.app.log(GAMESERVICE_ID, "Could not open snapshot due to conflicts!");
                                            if (callback != null)
                                                callback.gsGameStateLoaded(null);
                                        } else {
                                            byte[] mSaveGameData = new byte[0];
                                            try {
                                                mSaveGameData = snapshot.getSnapshotContents().readFully();
                                            } catch (IOException e) {
                                                Gdx.app.error(GAMESERVICE_ID, "Failed to read game state: " + e.getMessage());
                                            }
                                            if (callback != null)
                                                callback.gsGameStateLoaded(mSaveGameData);
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to load game state: " + openTask.getException().getMessage());
                        }
                    } else {
                        Gdx.app.error(GAMESERVICE_ID, "Failed to load game state: " + openTask.getException().getMessage());
                        if (callback != null)
                            callback.gsGameStateLoaded(null);
                    }
                }
            });
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not load game state. Session is not active");
            if (callback != null)
                callback.gsGameStateLoaded(null);
        }
    }

    @Override
    public boolean deleteGameState(final String id, final ISaveGameStateResponseListener callback) {
        if (isSessionActive()) {

            final SnapshotsClient client = PlayGames.getSnapshotsClient(myContext);

            // Open the snapshot, creating if necessary
            client.open(id, false).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask) {
                    if (openTask.isSuccessful()) {
                        try {
                            Snapshot snapshot = Tasks.await(processSnapshotOpenResult(openTask.getResult(), 0));
                            if (snapshot == null) {
                                Gdx.app.log(GAMESERVICE_ID, "Could not open snapshot due to conflicts");
                                if (callback != null)
                                    callback.onGameStateSaved(false, "Could not open snapshot due to conflicts");
                            } else {
                                client.delete(snapshot.getMetadata()).addOnCompleteListener(new OnCompleteListener<String>() {
                                    @Override
                                    public void onComplete(@NonNull Task<String> deleteTask) {
                                        if (deleteTask.isSuccessful()) {
                                            Gdx.app.error(GAMESERVICE_ID, "Snapshot deleted successfully: " + deleteTask.getException().getMessage());
                                            if (callback != null)
                                                callback.onGameStateSaved(true, "Snapshot deleted successfully");
                                        } else {
                                            Gdx.app.error(GAMESERVICE_ID, "Failed to delete the snapshot: " + deleteTask.getException().getMessage());
                                            if (callback != null)
                                                callback.onGameStateSaved(false,
                                                        deleteTask.getException().getMessage());
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to delete game state: " + openTask.getException().getMessage());
                        }
                    } else {
                        Gdx.app.log(GAMESERVICE_ID, "Could not delete game state " + id + ": " + openTask.getException().getMessage());
                        if (callback != null)
                            callback.onGameStateSaved(false, openTask.getException().getMessage());
                    }
                }
            });

            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not delete game state. Session is not active");
            if (callback != null)
                callback.onGameStateSaved(false, "Session not active");
            return false;
        }
    }

    @Override
    public boolean fetchGameStates(final IFetchGameStatesListResponseListener callback) {
        if (isSessionActive()) {

            PlayGames.getSnapshotsClient(myContext).load(forceReload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<SnapshotMetadataBuffer>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<SnapshotMetadataBuffer>> task) {
                    if (task.isSuccessful()) {
                        SnapshotMetadataBuffer snapshots = task.getResult().get();
                        Array<String> gameStates = new Array<>(snapshots.getCount());

                        for (SnapshotMetadata snapshot : snapshots) {
                            gameStates.add(snapshot.getUniqueName());
                        }

                        snapshots.release();

                        if (callback != null)
                            callback.onFetchGameStatesListResponse(gameStates);
                    } else {
                        Gdx.app.log(GAMESERVICE_ID, "Failed to fetch game states:" +
                                task.getException().getMessage());
                        if (callback != null)
                            callback.onFetchGameStatesListResponse(null);
                    }
                }
            });

            return true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Could not fetch game states. Session is not active");
            if (callback != null)
                callback.onFetchGameStatesListResponse(null);
            return false;
        }
    }

    @Override
    public void fetchCountryCode(ICountryCodeResponseListener callback) {
        if (!isSessionActive()) {
            Gdx.app.error(GAMESERVICE_ID, "Could not fetch country code. Session is not active");
            if (callback != null)
                callback.countryCodeRequestFailed("Session not active");
            return;
        }

        if (!billingClientReady) {
            Gdx.app.error(GAMESERVICE_ID, "Billing client not ready. Cannot fetch country code");
            if (callback != null)
                callback.countryCodeRequestFailed("Billing client not ready");
            return;
        }

        GetBillingConfigParams getBillingConfigParams = GetBillingConfigParams.newBuilder().build();
        mBillingClient.getBillingConfigAsync(getBillingConfigParams,
                new BillingConfigResponseListener() {
                    @Override
                    public void onBillingConfigResponse(@NonNull BillingResult billingResult, @Nullable BillingConfig billingConfig) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && billingConfig != null) {
                            String countryCode = billingConfig.getCountryCode();
                            if (callback != null)
                                callback.countryCodeReceived(countryCode);
                        } else {
                            Gdx.app.error(GAMESERVICE_ID, "Failed to fetch country code: " + billingResult.getDebugMessage());
                            if (callback != null)
                                callback.countryCodeRequestFailed(billingResult.getDebugMessage());
                        }
                    }
                });
    }

    /**
     * Conflict resolution for when Snapshots are opened. Returns a Task.
     */
    private Task<Snapshot> processSnapshotOpenResult(SnapshotsClient.DataOrConflict<Snapshot> result,
                                                     final int retryCount) {
        if (!result.isConflict()) {
            // There was no conflict, so return the result of the source.
            TaskCompletionSource<Snapshot> source = new TaskCompletionSource<>();
            source.setResult(result.getData());
            return source.getTask();
        }

        // There was a conflict.  Try resolving it by selecting the newest of the conflicting snapshots.
        // This is the same as using RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED as a conflict resolution
        // policy, but we are implementing it as an example of a manual resolution.
        // One option is to present a UI to the user to choose which snapshot to resolve.
        SnapshotsClient.SnapshotConflict conflict = result.getConflict();

        Snapshot snapshot = conflict.getSnapshot();
        Snapshot conflictSnapshot = conflict.getConflictingSnapshot();

        // Resolve between conflicts by selecting the newest of the conflicting snapshots.
        Snapshot resolvedSnapshot = snapshot;

        if (snapshot.getMetadata().getLastModifiedTimestamp() <
                conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
            resolvedSnapshot = conflictSnapshot;
        }

        return PlayGames.getSnapshotsClient(myContext)
                .resolveConflict(conflict.getConflictId(), resolvedSnapshot)
                .continueWithTask(
                        new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<Snapshot>>() {
                            @Override
                            public Task<Snapshot> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                                // Resolving the conflict may cause another conflict,
                                // so recurse and try another resolution.
                                if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES) {
                                    return processSnapshotOpenResult(task.getResult(), retryCount + 1);
                                } else {
                                    //Fail, return null;
                                    return null;
                                }
                            }
                        });
    }

    @Override
    public boolean isFeatureSupported(GameServiceFeature feature) {
        switch (feature) {
            case GameStateStorage:
            case GameStateMultipleFiles:
            case FetchGameStates:
            case GameStateDelete:
            case ShowAchievementsUI:
            case ShowAllLeaderboardsUI:
            case ShowLeaderboardUI:
            case SubmitEvents:
            case FetchAchievements:
            case FetchLeaderBoardEntries:
            case LeaderboardTimeSpans:
            case LeaderboardCollections:
            case PlayerLogOut:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Gdx.app.error(GAMESERVICE_ID, "Billing service disconnected");
        billingClientReady = false;
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            Gdx.app.log(GAMESERVICE_ID, "Billing service connected");
            billingClientReady = true;
        } else {
            Gdx.app.error(GAMESERVICE_ID, "Billing service setup failed: " + billingResult.getDebugMessage());
            billingClientReady = false;
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        Gdx.app.log(GAMESERVICE_ID, "Purchases updated: " + billingResult.getResponseCode());
    }
}
