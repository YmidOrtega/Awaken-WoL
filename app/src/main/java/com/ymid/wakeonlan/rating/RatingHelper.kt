package com.ymid.wakeonlan.rating

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

object RatingHelper {

    private const val PREFS = "rating_prefs"
    private const val KEY_WAKE_COUNT = "wake_count"
    private const val KEY_REVIEW_REQUESTED = "review_requested"
    private const val THRESHOLD = 3

    fun recordWakeAndMaybeRequestReview(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REVIEW_REQUESTED, false)) return

        val count = prefs.getInt(KEY_WAKE_COUNT, 0) + 1
        prefs.edit().putInt(KEY_WAKE_COUNT, count).apply()

        if (count >= THRESHOLD) {
            requestReview(activity)
            prefs.edit().putBoolean(KEY_REVIEW_REQUESTED, true).apply()
        }
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
