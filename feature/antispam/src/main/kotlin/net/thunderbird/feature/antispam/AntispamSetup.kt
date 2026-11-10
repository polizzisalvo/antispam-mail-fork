package net.thunderbird.feature.antispam

import net.thunderbird.feature.antispam.data.AccountListLinkEntity
import net.thunderbird.feature.antispam.data.AntispamDao
import net.thunderbird.feature.antispam.data.ListOrigin
import net.thunderbird.feature.antispam.data.ListSourceEntity
import net.thunderbird.feature.antispam.update.AntispamListUpdater
import net.thunderbird.feature.antispam.update.UpdateOutcome

/** Operazioni di configurazione che userà anche la futura schermata "Filtri Antispam Avanzati". */
class AntispamSetup(
    private val dao: AntispamDao,
    private val updater: AntispamListUpdater,
    private val manager: AntispamFilterManager,
) {
    /** Aggiunge (o riusa) una lista personalizzata, la collega agli account indicati e la scarica subito. */
    suspend fun addCustomList(url: String, name: String, accountUuids: Collection<String>): UpdateOutcome {
        val existing = dao.getAllLists().firstOrNull { it.url == url }
        val listId = existing?.id ?: dao.insertList(
            ListSourceEntity(url = url, name = name, origin = ListOrigin.CUSTOM),
        )
        dao.insertLinks(accountUuids.map { AccountListLinkEntity(it, listId) })
        manager.invalidate()

        val list = dao.getAllLists().first { it.id == listId }
        return updater.updateList(list, force = true)
    }

    suspend fun hasList(url: String): Boolean = dao.getAllLists().any { it.url == url }
}
