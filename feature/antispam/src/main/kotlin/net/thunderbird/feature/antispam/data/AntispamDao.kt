package net.thunderbird.feature.antispam.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface AntispamDao {

    // --- Liste ---
    @Query("SELECT * FROM list_source ORDER BY origin, name")
    suspend fun getAllLists(): List<ListSourceEntity>

    @Query("SELECT * FROM list_source WHERE catalogId = :catalogId")
    suspend fun getByCatalogId(catalogId: String): ListSourceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertList(list: ListSourceEntity): Long

    @Update
    suspend fun updateList(list: ListSourceEntity)

    @Query("DELETE FROM list_source WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    /** Liste usate da almeno un account: sono le uniche che il worker settimanale deve aggiornare. */
    @Query("SELECT * FROM list_source WHERE id IN (SELECT DISTINCT listId FROM account_list_link)")
    suspend fun getListsInUse(): List<ListSourceEntity>

    // --- Regole ---
    @Insert
    suspend fun insertRules(rules: List<RuleEntity>)

    @Query("DELETE FROM rule WHERE listId = :listId")
    suspend fun deleteRules(listId: Long)

    /** Sostituisce in blocco le regole di una lista (aggiornamento atomico). */
    @Transaction
    suspend fun replaceRules(listId: Long, rules: List<RuleEntity>) {
        deleteRules(listId)
        rules.chunked(500).forEach { insertRules(it) }
    }

    /** Ricerca esatta, indicizzata su (type, value), limitata alle liste attive per l'account. */
    @Query(
        """
        SELECT r.* FROM rule r
        JOIN account_list_link l ON l.listId = r.listId
        WHERE l.accountUuid = :accountUuid AND r.type = :type AND r.value IN (:values)
        """,
    )
    suspend fun findExact(accountUuid: String, type: RuleType, values: List<String>): List<RuleEntity>

    /** Keyword (oggetto/corpo) dell'account: caricate in memoria e cacheate dal manager. */
    @Query(
        """
        SELECT r.* FROM rule r
        JOIN account_list_link l ON l.listId = r.listId
        WHERE l.accountUuid = :accountUuid AND r.type = :type
        """,
    )
    suspend fun getKeywordRules(accountUuid: String, type: RuleType): List<RuleEntity>

    // --- Collegamenti account <-> lista ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<AccountListLinkEntity>)

    @Query("DELETE FROM account_list_link WHERE accountUuid = :accountUuid AND listId = :listId")
    suspend fun removeLink(accountUuid: String, listId: Long)

    @Query("SELECT listId FROM account_list_link WHERE accountUuid = :accountUuid")
    suspend fun getListIdsForAccount(accountUuid: String): List<Long>

    @Query("DELETE FROM account_list_link WHERE accountUuid = :accountUuid")
    suspend fun removeAllLinksForAccount(accountUuid: String)

    /** "Applica a tutti / a questi account": copia le liste di un account su altri. */
    @Transaction
    suspend fun copyLinks(fromAccount: String, toAccounts: List<String>) {
        val listIds = getListIdsForAccount(fromAccount)
        insertLinks(toAccounts.flatMap { acc -> listIds.map { AccountListLinkEntity(acc, it) } })
    }
}
