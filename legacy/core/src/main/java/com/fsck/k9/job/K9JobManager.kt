package com.fsck.k9.job

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.antispam.AntispamStartup
import net.thunderbird.legacy.logging.Log

private const val TAG = "K9JobManager"

class K9JobManager(
    private val workManager: WorkManager,
    private val accountManager: LegacyAccountManager,
    private val mailSyncWorkerManager: MailSyncWorkerManager,
    private val syncDebugFileLogManager: FileLogLimitWorkManager,
    private val antispamStartup: AntispamStartup? = null,
) {
    fun scheduleDebugLogLimit(contentUriString: String): Flow<WorkInfo?> {
        return syncDebugFileLogManager.scheduleFileLogTimeLimit(contentUriString)
    }

    fun cancelDebugLogLimit() {
        syncDebugFileLogManager.cancelFileLogTimeLimit()
    }

    fun scheduleAllMailJobs() {
        Log.v(TAG, "scheduling all jobs")
        scheduleMailSync()
        antispamStartup?.onStart(accountManager.findAll().map { it.id.toString() })
    }

    fun scheduleMailSync(account: LegacyAccount) {
        mailSyncWorkerManager.cancelMailSync(account)
        mailSyncWorkerManager.scheduleMailSync(account)
    }

    private fun scheduleMailSync() {
        cancelAllMailSyncJobs()

        accountManager.findAll().forEach { account ->
            mailSyncWorkerManager.scheduleMailSync(account)
        }
    }

    private fun cancelAllMailSyncJobs() {
        Log.v(TAG, "canceling mail sync job")
        workManager.cancelAllWorkByTag(MailSyncWorkerManager.MAIL_SYNC_TAG)
    }
}
