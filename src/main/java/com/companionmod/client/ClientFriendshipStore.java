package com.companionmod.client;

import com.companionmod.BehaviorMode;
import java.util.*;

public class ClientFriendshipStore {
    private static final Map<UUID,Boolean>      friends   = new HashMap<>();
    private static final Map<UUID,BehaviorMode> behaviors = new HashMap<>();
    private static final Map<UUID,String>       nicknames = new HashMap<>();

    public static boolean     isFriend(UUID id)  { return friends.getOrDefault(id, false); }
    public static BehaviorMode getBehavior(UUID id){ return behaviors.getOrDefault(id, BehaviorMode.FOLLOW); }
    public static String      getNickname(UUID id){ return nicknames.getOrDefault(id, ""); }

    public static void setFriend(UUID id, boolean f) {
        if (f) friends.put(id, true);
        else { friends.remove(id); behaviors.remove(id); nicknames.remove(id); }
    }
    public static void setBehavior(UUID id, BehaviorMode m) { behaviors.put(id, m); }
    public static void setNickname(UUID id, String n)       { nicknames.put(id, n); }
    public static void clear() { friends.clear(); behaviors.clear(); nicknames.clear(); }
}
