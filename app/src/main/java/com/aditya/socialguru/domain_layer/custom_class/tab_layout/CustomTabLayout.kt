package com.aditya.socialguru.domain_layer.custom_class.tab_layout

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
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
import com.aditya.socialguru.domain_layer.helper.sspToPx
import com.aditya.socialguru.domain_layer.manager.MyLogger

class CustomTabLayout(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    private var binding: SampleCustomTabLayoutBinding
    private lateinit var listTabName: List<String>
    private lateinit var listTabTv: List<TextView>
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var textSize: Float = 0f
    private var fontFamily: String? = null

    init {
        //Doing this inflated view automatically add to parent where i pass to parent context and don't make attachToParent false because view not attach and don't see.
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
        textSize = typedArray.getDimension(R.styleable.CustomTabBar_android_textSize, resources.getDimension(
            com.intuit.ssp.R.dimen._16ssp))
        MyLogger.w(msg = "Text Size is $textSize")
        fontFamily = typedArray.getString(R.styleable.CustomTabBar_android_fontFamily)

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
        val params=LinearLayout.LayoutParams(
            0,
            LayoutParams.MATCH_PARENT,
            1f
        )
        params.setMargins(0 ,0,2 ,0)
        layoutParams = params
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(this.context, R.color.white))
        setTextSize(TypedValue.COMPLEX_UNIT_PX,this@CustomTabLayout.textSize)

        fontFamily?.let {
            typeface = ResourcesCompat.getFont(context, resources.getIdentifier(it, "font", context.packageName))
        } ?: run {
            typeface = ResourcesCompat.getFont(context, R.font.poppin_medium)
        }
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