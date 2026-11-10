package net.thunderbird.feature.antispam

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AntispamUpdateScheduler(private val workManager: WorkManager) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Aggiornamento automatico ogni 7 giorni. KEEP: non azzera il timer a ogni avvio dell'app. */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<AntispamUpdateWorker>(UPDATE_INTERVAL_DAYS, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Per il pulsante "Aggiorna ora". */
    fun updateNow() {
        val request = OneTimeWorkRequestBuilder<AntispamUpdateWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueue(request)
    }

    private companion object {
        const val UPDATE_INTERVAL_DAYS = 7L
        const val PERIODIC_WORK_NAME = "antispam_list_update"
    }
}
