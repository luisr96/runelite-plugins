package com.f2pstarhunt;

import net.runelite.api.FriendsChatRank;

public enum FriendsChatRankRequirement
{
    ANYONE("Anyone", -1),
    RECRUIT("Recruit", FriendsChatRank.RECRUIT.getValue()),
    CORPORAL("Corporal", FriendsChatRank.CORPORAL.getValue()),
    SERGEANT("Sergeant", FriendsChatRank.SERGEANT.getValue()),
    LIEUTENANT("Lieutenant", FriendsChatRank.LIEUTENANT.getValue()),
    CAPTAIN("Captain", FriendsChatRank.CAPTAIN.getValue()),
    GENERAL("General", FriendsChatRank.GENERAL.getValue()),
    OWNER("Owner", FriendsChatRank.OWNER.getValue());

    private final String name;
    private final int value;

    FriendsChatRankRequirement(String name, int value)
    {
        this.name = name;
        this.value = value;
    }

    public boolean meetsRequirement(FriendsChatRank playerRank)
    {
        if (this == ANYONE)
        {
            return true;
        }

        if (playerRank == null)
        {
            return false;
        }

        return playerRank.getValue() >= this.value;
    }

    @Override
    public String toString()
    {
        return name;
    }
}