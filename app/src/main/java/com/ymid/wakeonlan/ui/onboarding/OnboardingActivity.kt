package com.ymid.wakeonlan.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.databinding.ActivityOnboardingBinding
import com.ymid.wakeonlan.ui.MainActivity
import java.util.Locale

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    data class Slide(val iconRes: Int, val titleRes: Int, val descRes: Int, val tintColor: Int? = null)

    private val slides = listOf(
        Slide(R.drawable.ic_launcher_foreground_on, R.string.onboarding_slide1_title, R.string.onboarding_slide1_description),
        Slide(R.drawable.ic_power_24, R.string.onboarding_slide2_title, R.string.onboarding_slide2_description, R.color.ledOnlineColor),
        Slide(R.drawable.ic_notification, R.string.onboarding_slide3_title, R.string.onboarding_slide3_description, R.color.ledOnlineColor)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val adapter = SlideAdapter(slides)
        binding.onboardingPager.adapter = adapter

        binding.onboardingIndicator.setSegmentCount(slides.size)
        binding.onboardingSkip.text = "› " + getString(R.string.onboarding_skip).lowercase()
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
                binding.onboardingStep.text = String.format(Locale.ROOT, "%02d/%02d", position + 1, slides.size)
                binding.onboardingIndicator.setProgress((position + 1) / slides.size.toFloat())
                binding.onboardingNext.text = "› " + getString(
                    if (isLast) R.string.onboarding_finish else R.string.onboarding_next
                ).lowercase()
                binding.onboardingSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
                if (isLast) requestNotificationPermissionIfNeeded()
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
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
            // The launcher glyph only fills the middle of its 108dp canvas; plain icons fill theirs
            val launcherGlyph = slide.iconRes == R.drawable.ic_launcher_foreground_on
            val padding = if (launcherGlyph) 0 else (14 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.icon.setPadding(padding, padding, padding, padding)
            holder.icon.scaleX = if (launcherGlyph) 1.25f else 1f
            holder.icon.scaleY = holder.icon.scaleX
            if (slide.tintColor != null) {
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, slide.tintColor))
            } else {
                holder.icon.clearColorFilter()
            }
            holder.title.setText(slide.titleRes)
            holder.desc.setText(slide.descRes)
        }
    }

    companion object {
        private const val PREFS = "onboarding_prefs"
        private const val KEY_DONE = "done"

        @JvmStatic
        fun isComplete(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DONE, false)

        private fun markOnboardingComplete(context: Context) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
        }
    }
}
