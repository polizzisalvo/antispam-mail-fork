package net.thunderbird.feature.antispam.update

import android.database.sqlite.SQLiteConstraintException
import net.thunderbird.feature.antispam.data.AntispamDao
import net.thunderbird.feature.antispam.data.ListOrigin
import net.thunderbird.feature.antispam.data.ListSourceEntity
import org.json.JSONException
import org.json.JSONObject

data class CatalogEntry(val id: String, val name: String, val url: String, val defaultEnabled: Boolean)

data class CatalogSyncResult(val added: List<ListSourceEntity>, val removedCount: Int)

/**
 * Catalogo remoto delle liste predefinite (JSON ospitato nel repo del fork):
 * { "version": 1, "lists": [ { "id": "it-promo", "name": "Promozioni IT", "url": "https://...", "defaultEnabled": true } ] }
 *
 * Tocca SOLO le liste BUILT_IN. Le liste CUSTOM dell'utente non vengono mai modificate né rimosse.
 */
class AntispamCatalogSync(
    private val dao: AntispamDao,
    private val downloader: AntispamListDownloader,
    private val catalogUrl: String,
) {
    suspend fun sync(): CatalogSyncResult? {
        val download = downloader.fetch(catalogUrl, etag = null, lastModified = null) as? FetchResult.Downloaded
            ?: return null
        val entries = parseCatalog(download.lines.joinToString("\n")) ?: return null
        if (entries.isEmpty()) return null // catalogo vuoto o rotto: non cancelliamo nulla

        val existing = dao.getAllLists().filter { it.origin == ListOrigin.BUILT_IN && it.catalogId != null }
            .associateBy { it.catalogId }
        val added = mutableListOf<ListSourceEntity>()

        for (entry in entries) {
            val current = existing[entry.id]
            if (current == null) {
                val newList = ListSourceEntity(
                    url = entry.url,
                    name = entry.name,
                    origin = ListOrigin.BUILT_IN,
                    catalogId = entry.id,
                )
                try {
                    added += newList.copy(id = dao.insertList(newList))
                } catch (_: SQLiteConstraintException) {
                    // URL già presente come lista personalizzata dell'utente: la lasciamo com'è.
                }
            } else if (current.url != entry.url || current.name != entry.name) {
                // URL cambiato: azzeriamo etag, così la lista viene riscaricata per intero.
                dao.updateList(current.copy(url = entry.url, name = entry.name, etag = null, lastModified = null))
            }
        }

        val catalogIds = entries.map { it.id }.toSet()
        val toRemove = existing.values.filter { it.catalogId !in catalogIds }
        toRemove.forEach { dao.deleteList(it.id) } // CASCADE su regole e collegamenti

        return CatalogSyncResult(added = added, removedCount = toRemove.size)
    }

    internal fun parseCatalog(json: String): List<CatalogEntry>? = try {
        val array = JSONObject(json).getJSONArray("lists")
        (0 until array.length()).mapNotNull { i ->
            val o = array.getJSONObject(i)
            val id = o.optString("id").trim()
            val url = o.optString("url").trim()
            if (id.isEmpty() || !url.startsWith("https://")) {
                null
            } else {
                CatalogEntry(id, o.optString("name", id), url, o.optBoolean("defaultEnabled", false))
            }
        }
    } catch (_: JSONException) {
        null
    }
}
