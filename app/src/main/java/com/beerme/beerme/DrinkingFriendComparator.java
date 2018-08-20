package com.beerme.beerme;

import java.util.Comparator;

public class DrinkingFriendComparator implements Comparator<DrinkingFriend> {
    @Override
    public int compare(DrinkingFriend d1, DrinkingFriend d2) {
            if(d1.isDrinking()&&d2.isDrinking()||!d1.isDrinking()&&d2.isDrinking()) return 0;
            else if(d1.isDrinking()) return 1;
            return -1;
    }
}
