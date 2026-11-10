package net.thunderbird.feature.antispam.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ListOrigin { BUILT_IN, CUSTOM }

enum class RuleType { SENDER_ADDRESS, SENDER_DOMAIN, SUBJECT_KEYWORD, BODY_KEYWORD }

/**
 * BUILT_IN: arriva dal catalogo remoto, viene aggiornata e può essere rimossa se esce dal catalogo.
 * CUSTOM: aggiunta dall'utente, fissa: il catalogo non la tocca mai.
 */
@Entity(tableName = "list_source", indices = [Index(value = ["url"], unique = true)])
data class ListSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val name: String,
    val origin: ListOrigin,
    val catalogId: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastUpdatedAt: Long? = null,
    val lastError: String? = null,
    val ruleCount: Int = 0,
)

@Entity(
    tableName = "rule",
    foreignKeys = [
        ForeignKey(
            entity = ListSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId"), Index(value = ["type", "value"])],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val type: RuleType,
    val value: String, // sempre minuscolo
    val isException: Boolean = false, // regole @@ (allowlist)
)

/** Associa una lista a un account: le liste sono scaricate una volta sola, l'attivazione è per account. */
@Entity(
    tableName = "account_list_link",
    primaryKeys = ["accountUuid", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = ListSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId")],
)
data class AccountListLinkEntity(
    val accountUuid: String,
    val listId: Long,
)
