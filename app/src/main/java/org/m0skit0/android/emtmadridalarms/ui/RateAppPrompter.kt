package org.m0skit0.android.emtmadridalarms.ui

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.LastRatePromptShown
import org.m0skit0.android.emtmadridalarms.data.MarkRatePromptShown
import org.m0skit0.android.emtmadridalarms.data.RecordAppOpen
import org.m0skit0.android.emtmadridalarms.data.nextRatePromptThreshold
import timber.log.Timber

private const val TAG = "RateAppPrompter"

fun interface RequestInAppReview : (Activity) -> Unit
fun interface RateAppPrompter : (Activity) -> Unit

internal fun requestInAppReview(): RequestInAppReview = RequestInAppReview { activity ->
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            val flow = manager.launchReviewFlow(activity, reviewInfo)
            flow.addOnCompleteListener { Timber.d("In-app review flow finished") }
        } else {
            Timber.w("In-app review request failed: ${task.exception?.message}")
        }
    }
}

internal fun rateAppPrompter(
    recordAppOpen: RecordAppOpen,
    lastRatePromptShown: LastRatePromptShown,
    markRatePromptShown: MarkRatePromptShown,
    requestInAppReview: RequestInAppReview,
    scope: CoroutineScope,
): RateAppPrompter = RateAppPrompter { activity ->
    scope.launch {
        val count = recordAppOpen()
        val lastShown = lastRatePromptShown()
        val threshold = nextRatePromptThreshold(count, lastShown)
        if (threshold == null) {
            Timber.d("No rate prompt due (opens=$count, lastShown=$lastShown)")
            return@launch
        }
        markRatePromptShown(threshold)
        Timber.d("Requesting in-app review at threshold=$threshold")
        requestInAppReview(activity)
    }
}
