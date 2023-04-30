package com.example.socialguru.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.socialguru.fragment.freind.FollowerListFragment;
import com.example.socialguru.fragment.freind.FriendListFragment;
import com.example.socialguru.fragment.freind.FriendRequestFragment;

public class FreindViewPagerAdapter extends FragmentPagerAdapter {

    public FreindViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){

            case 0:
                return new FollowerListFragment();
            case 1:
                return new FriendRequestFragment();

            case 2:
                return new FriendListFragment();
            default:
                return new FollowerListFragment();

        }
    }

    @Override
    public int getCount() {
        return 3;
    }
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {

        String title="";

        if(position==0){
            title = "Follower";
        }
        else {

            if (position == 1) {
                title = "FriendRequest";

            } else {
                if (position == 2) {
                    title = "FriendList";
                }
            }
        }

        return title;
    }

}
