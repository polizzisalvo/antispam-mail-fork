package net.thunderbird.feature.antispam

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.thunderbird.feature.antispam.update.AntispamCatalogSync
import net.thunderbird.feature.antispam.update.AntispamListUpdater
import net.thunderbird.feature.antispam.update.UpdateOutcome
import org.koin.core.context.GlobalContext

/**
 * Aggiorna catalogo e liste. Il nome del package è fuori da "com.fsck.k9", quindi K9WorkerFactory restituisce null
 * e WorkManager usa la factory standard: le dipendenze si prendono da Koin qui dentro.
 */
class AntispamUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.getOrNull() ?: return Result.retry()
        val config = koin.get<AntispamConfig>()

        if (config.catalogUrl != null) {
            koin.get<AntispamCatalogSync>().sync()
        }
        val outcomes = koin.get<AntispamListUpdater>().updateAllInUse()
        val allFailed = outcomes.isNotEmpty() && outcomes.values.all { it is UpdateOutcome.Error }
        return if (allFailed) Result.retry() else Result.success()
    }
}
