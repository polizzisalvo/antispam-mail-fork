package net.thunderbird.feature.antispam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Chiamato all'avvio dell'app (da K9JobManager): pianifica l'aggiornamento e, se serve, aggiunge la lista di test. */
class AntispamStartup(
    private val scheduler: AntispamUpdateScheduler,
    private val setup: AntispamSetup,
    private val config: AntispamConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onStart(accountUuids: List<String>) {
        scheduler.schedule()

        val testListUrl = config.testListUrl ?: return
        scope.launch {
            try {
                if (!setup.hasList(testListUrl)) {
                    setup.addCustomList(testListUrl, "Lista di test", accountUuids)
                }
            } catch (_: Exception) {
                // Non deve mai impedire l'avvio dell'app.
            }
        }
    }
}
