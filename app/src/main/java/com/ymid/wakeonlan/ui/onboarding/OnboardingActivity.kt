package com.ymid.wakeonlan.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.databinding.ActivityOnboardingBinding
import com.ymid.wakeonlan.ui.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    data class Slide(val iconRes: Int, val titleRes: Int, val descRes: Int)

    private val slides = listOf(
        Slide(R.drawable.ic_launcher_foreground_on, R.string.onboarding_slide1_title, R.string.onboarding_slide1_description),
        Slide(R.drawable.ic_power_24, R.string.onboarding_slide2_title, R.string.onboarding_slide2_description),
        Slide(R.drawable.ic_notification, R.string.onboarding_slide3_title, R.string.onboarding_slide3_description)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = SlideAdapter(slides)
        binding.onboardingPager.adapter = adapter

        TabLayoutMediator(binding.onboardingIndicator, binding.onboardingPager) { _, _ -> }.attach()

        binding.onboardingSkip.setOnClickListener { finish() }

        binding.onboardingNext.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current < slides.size - 1) {
                binding.onboardingPager.currentItem = current + 1
            } else {
                finish()
            }
        }

        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isLast = position == slides.size - 1
                binding.onboardingNext.text = getString(
                    if (isLast) R.string.onboarding_finish else R.string.onboarding_next
                )
                binding.onboardingSkip.visibility = if (isLast) View.GONE else View.VISIBLE
            }
        })
    }

    override fun finish() {
        markOnboardingComplete(this)
        startActivity(Intent(this, MainActivity::class.java))
        super.finish()
    }

    inner class SlideAdapter(private val items: List<Slide>) :
        RecyclerView.Adapter<SlideAdapter.SlideViewHolder>() {

        inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.onboarding_icon)
            val title: TextView = view.findViewById(R.id.onboarding_title)
            val desc: TextView = view.findViewById(R.id.onboarding_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_slide, parent, false)
            return SlideViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            val slide = items[position]
            holder.icon.setImageResource(slide.iconRes)
            holder.title.setText(slide.titleRes)
            holder.desc.setText(slide.descRes)
        }
    }

    companion object {
        private const val PREFS = "onboarding_prefs"
        private const val KEY_DONE = "done"

        fun isComplete(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DONE, false)

        private fun markOnboardingComplete(context: Context) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
        }
    }
}
