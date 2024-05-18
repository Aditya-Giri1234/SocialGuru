package com.aditya.socialguru.ui_layer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter



class HomeViewPagerAdapter(
    list: List<Fragment>, fm: FragmentManager, lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {
    // A custom ArrayList to hold the list of Fragments
    private val fragmentList = list

    // getItemCount determines the total number of pages in the adapter
    override fun getItemCount(): Int {
        return fragmentList.size
    }

    // createFragment creates a Fragment instance for a specific position
    override fun createFragment(position: Int): Fragment {
        // Returns the relevant Fragment for a specific position
        return fragmentList[position]
    }
}