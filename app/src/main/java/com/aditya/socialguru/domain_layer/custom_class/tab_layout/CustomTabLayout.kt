package com.aditya.socialguru.domain_layer.custom_class.tab_layout

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.SampleCustomTabLayoutBinding

class CustomTabLayout(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    private var binding: SampleCustomTabLayoutBinding
    private lateinit var listTabName: List<String>
    private lateinit var listTabTv: List<TextView>
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        binding = SampleCustomTabLayoutBinding.inflate(LayoutInflater.from(context), this, true)
        setupAttrs(attrs)
        setupUI()
        setTextBackground(0)
    }

    fun attachTo(viewPager: ViewPager2) {
        onTabSelectedListener = {
            viewPager.setCurrentItem(it, true)
        }

        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onTabSelected(position)
                setTextBackground(position)
            }
        })
    }

    private fun setupAttrs(attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.CustomTabBar,
            0, 0
        )

        listTabName =
            typedArray.getTextArray(R.styleable.CustomTabBar_android_entries).toList().map {
                it.toString()
            }

        typedArray.recycle()
    }

    private fun setupUI() {
        // Initialize tab TextViews
        listTabTv = listTabName.mapIndexed { index, tabName ->
            initTabTv(tabName, index)
        }

        // Add tab TextViews to view_tabs_wrapper
        binding.viewTabsWrapper.apply {
            orientation = HORIZONTAL
            weightSum = listTabTv.size.toFloat()
            listTabTv.forEach {
                this.addView(it)
            }
        }


    }

    private fun initTabTv(tabName: String, index: Int) = TextView(context).apply {
        text = tabName
        layoutParams = LinearLayout.LayoutParams(
            0,
            LayoutParams.MATCH_PARENT,
            1f
        )
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(this.context, R.color.white))
        textSize = resources.getDimension(com.intuit.ssp.R.dimen._6ssp)
        typeface = ResourcesCompat.getFont(context, R.font.poppin_medium)
        setOnClickListener {
            onTabSelected(index)
            setTextBackground(index)
            background = ResourcesCompat.getDrawable(resources, R.drawable.shape_indicator, null)
        }
    }

    private fun setTextBackground(index: Int) {
        listTabTv.forEachIndexed { _index, textView ->
            if (_index == index) {
                textView.apply {
                    background =
                        ResourcesCompat.getDrawable(resources, R.drawable.shape_indicator, null)
                    setTextColor(ContextCompat.getColor(this.context, R.color.black))
                }
            } else {
                textView.apply {
                    background =
                        ResourcesCompat.getDrawable(resources, R.drawable.shape_tab_bar_bg, null)
                    setTextColor(ContextCompat.getColor(this.context, R.color.white))
                }
            }
        }
    }

    private fun onTabSelected(index: Int) {
        onTabSelectedListener?.invoke(index)
    }
}