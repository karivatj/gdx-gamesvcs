/*! Copyright 2013 Amazon Digital Services, Inc. All rights reserved. */
LeaderboardHandlerRecognized=(function(){var a=function(h,g){var f="LeaderboardRequest";var c=h;var b=g;this["getHandledType"]=function(){return f};this["handleRequest"]=function(k){var q=$.Deferred();if(!(k instanceof Request)){console.log("LeaderboardHandlerRecognized: handleRequest: input was not of type Request");var t=new Result(constants.NativeCallResultCode.REQUEST_ERROR,{});q.resolve(t)}else{console.log("LeaderboardHandlerRecognized: handleRequest: message is: "+JSON.stringify(k.getType()));var m=k.getType();var p=k.getParams();var n=p.action;var j={};switch(n){case"getLeaderboardsForGame":var o=p.cacheOnly;if(o){promise=b.getLeaderboards("SELF",p.gameId);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: cached getLeaderboards promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)})}else{promise=c.getLeaderboardsForGame(p.gameId);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: getLeaderboardsForGame promise: "+JSON.stringify(u));if(v!=constants.NativeCallResultCode.SUCCESS){console.log("LeaderboardHandlerRecognized: getLeaderboardsForGame promise: SERVICE RESULT NOT OK");promise=b.getLeaderboards("SELF",p.gameId);promise.done(function(z,A){console.log("LeaderboardHandlerRecognized: cached getLeaderboards promise: "+JSON.stringify(z));var B=A;var C=z;var z=new Result(B,C);q.resolve(z)})}else{var x=constants.NativeCallResultCode.SUCCESS;var y=u;var w=b.deleteLeaderboards("SELF",p.gameId);w.done(function(z){var A=b.cacheLeaderboards("SELF",p.gameId,y);A.done(function(C){console.log("UPDATED LEADERBOARDS CACHE cacheLeaderboards");var B=y.LeaderboardList;var E=[];$.each(B,function(H,F){var G=F.LeaderboardInfo;E.push(G)});var D=b.cacheLeaderboardInfoItems(p.gameId,E)})});var u=y;u.serviceResult=true;u=new Result(x,u);q.resolve(u)}})}break;case"getHighScore":var o=p.cacheOnly;var l=p.playerId;if(o){promise=b.getLocalPlayerScore(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: cached getLocalPlayerScore promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)})}else{promise=c.getHighScore(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: getHighScore promise: "+JSON.stringify(u));var y=v==constants.NativeCallResultCode.SUCCESS&&e(u);if(!y){console.log("LeaderboardHandlerRecognized: getHighScore promise: SERVICE RESULT NOT OK");promise=b.getLocalPlayerScore(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(A,B){console.log("LeaderboardHandlerRecognized: cached getLocalPlayerScore promise: "+JSON.stringify(A));var C=B;var D=A;var A=new Result(C,D);q.resolve(A)})}else{var x=constants.NativeCallResultCode.SUCCESS;var z=u;var u=new Result(x,z);var w=b.cacheLocalPlayerScore(l,p.gameId,p.leaderboardId,p.scope,z);w.done(function(A){console.log("UPDATED LEADERBOARDS CACHE")});q.resolve(u)}})}break;case"getHighestScoreSubmission":b.getHighestScoreSubmissions("SELF").always(function(u,v){console.log("LeaderboardHandlerGuest: cached getHighestScoreSubmissions promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)});break;case"getLeaderboardRanks":var o=p.cacheOnly;if(o){promise=b.getScores("SELF",p.gameId,p.leaderboardId,p.scope,p.startRank,p.numRanks,false);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: cached getScores promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)})}else{promise=c.getLeaderboardRanks(p.gameId,p.leaderboardId,p.scope,p.startRank,p.numRanks);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: getLeaderboardRanks promise: "+JSON.stringify(u));var y=v==constants.NativeCallResultCode.SUCCESS&&e(u);if(!y){console.log("LeaderboardHandlerRecognized: getLeaderboardRanks promise: SERVICE RESULT NOT OK");promise=b.getScores("SELF",p.gameId,p.leaderboardId,p.scope,p.startRank,p.numRanks,false);promise.done(function(A,B){console.log("LeaderboardHandlerRecognized: cached getScores promise: "+JSON.stringify(A));hostinterface.logMessage("LeaderboardHandlerRecognized: getScores result came back: "+JSON.stringify(A));var C=B;var D=A;var A=new Result(C,D);q.resolve(A)})}else{var x=constants.NativeCallResultCode.SUCCESS;var z=u;var u=new Result(x,z);var w=b.cacheScoresForGame("SELF",p.gameId,p.leaderboardId,p.scope,p.startRank,p.numRanks,false,z);w.done(function(A){console.log("UPDATED LEADERBOARDS CACHE")});q.resolve(u)}})}break;case"getLeaderboardPercentiles":var o=p.cacheOnly;var l=p.playerId;if(o){promise=b.getPercentiles(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: cached getPercentiles promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)})}else{promise=c.getLeaderboardPercentiles(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(u,v){console.log("LeaderboardHandlerRecognized: getLeaderboardPercentiles promise: "+JSON.stringify(u));var y=v==constants.NativeCallResultCode.SUCCESS&&e(u);if(!y){console.log("LeaderboardHandlerRecognized: getLeaderboardPercentiles promise: SERVICE RESULT NOT OK");promise=b.getPercentiles(l,p.gameId,p.leaderboardId,p.scope);promise.done(function(A,B){console.log("LeaderboardHandlerRecognized: cached getPercentiles promise: "+JSON.stringify(A));var C=B;var D=A;var A=new Result(C,D);q.resolve(A)})}else{var x=constants.NativeCallResultCode.SUCCESS;var z=u;var u=new Result(x,z);var w=b.cachePercentiles(l,p.gameId,p.leaderboardId,p.scope,z);w.done(function(A){console.log("UPDATED LEADERBOARDS CACHE")});q.resolve(u)}})}break;case"submitScore":var s=false;var r;var i=$.Deferred();b.getHighestScoreSubmission("SELF",p.gameId,p.leaderboardId).always(function(w,v){var u=v==constants.NativeCallResultCode.SUCCESS&&e(w);if(!u){s=true}b.getLeaderboardInfo(p.gameId,p.leaderboardId).always(function(z,x){r=z;if(v==constants.NativeCallResultCode.SUCCESS&&x==constants.NativeCallResultCode.SUCCESS&&z&&z.LeaderboardInfo){var y=z.LeaderboardInfo["SortOrder"];if(y=="DESCENDING"){if(p.PlayerScore>w){s=true}}else{if(p.PlayerScore<w){s=true}}}i.resolve()});$.when(i).always(function(){c.submitScore(p.gameId,p.leaderboardId,p.PlayerScore).always(function(H,F){console.log("LeaderboardHandlerRecognized: submitScore promise: "+JSON.stringify(H)+", resultCode: "+F);if(e(H)){d(p.leaderboardId)}if(F!=constants.NativeCallResultCode.SUCCESS||!e(H)){var C={};var x={};x.type="leaderboardEvent";x.leaderboardId=p.leaderboardId;x.PlayerScore=p.PlayerScore;x.gameId=p.gameId;x.eventTime=new Date().getTime();C.eventJson=x;NativeTransport.callNative({nativeCall:constants.NativeCallTypes.QUEUE_OFFLINE_EVENT,args:C});var A=r;if(s&&A&&A.LeaderboardInfo["Name"]){console.log("LeaderboardHandlerRecognized: displaying toast even though we just queued");var y=A.LeaderboardInfo["Name"];var E=A.LeaderboardInfo["IconUrl"];var B=ServiceFactory.getToastFactory().createLeaderboardToastRequest(y,p.PlayerScore,A.LeaderboardInfo["DisplayText"],E);hostinterface.showToast(JSON.stringify(B))}else{console.log("LeaderboardHandlerRecognized: not enough info to display the toast:");console.log("    isLocalHighScore: "+s);console.log("    leaderboardInfo: "+JSON.stringify(A));console.log("    leaderboardInfo[Name]: "+A.LeaderboardInfo["Name"])}b.updateCachedScoreSubmission("SELF",p.gameId,p.leaderboardId,p.PlayerScore).always(function(I,J){console.log("LeaderboardHandlerRecognized: submitScore updateCachedScoreSubmission completed")});q.resolve(H)}else{var G=constants.NativeCallResultCode.SUCCESS;var z=H;var H=new Result(G,z);b.updateCachedScoreSubmission("SELF",p.gameId,p.leaderboardId,p.PlayerScore).always(function(I,J){console.log("LeaderboardHandlerRecognized: submitScore updateCachedScoreSubmission completed")});var D=H.getResultMap()["SubmitScoreResultsList"];var A=H.getResultMap()["LeaderboardInfo"];var y=A.Name;$.each(D,function(K,M){var J=M.IsImproved;var L=M.Scope;if(J){if(L=="GLOBAL_ALL_TIME"){var I=ServiceFactory.getToastFactory().createLeaderboardToastRequest(y,p.PlayerScore,A.DisplayText,A.IconUrl);hostinterface.showToast(JSON.stringify(I))}}});q.resolve(H)}})})});break;case"clearCache":b.clear().always(function(u,v){console.log("LeaderboardHandlerRecognized: clearCache promise: "+JSON.stringify(u));var w=v;var x=u;var u=new Result(w,x);q.resolve(u)});break;default:console.log("LeaderboardHandlerRecognized: handleRequest: actionCode not supported: "+n);var t=new Result(constants.NativeCallResultCode.REQUEST_ERROR,{});q.resolve(t)}}console.log("LeaderboardHandlerRecognized: handleRequest: end of call");return q.promise()};function e(i){return(i!==undefined&&i!==null&&i.ResultCodes!==undefined&&i.ResultCodes!==null&&i.ResultCodes.errorCode==="OK"&&i.ResultCodes.resultCode==="OK")}function d(i){var j=new GameCircleEvent("PostLeaderboardInfo");j.addAttribute(constants.MetricConstants.MetricStringValueAttributesKeys_TARGET_ID,i);j.close()}};return a}());console.log("LeaderboardHandlerRecognized loaded.");