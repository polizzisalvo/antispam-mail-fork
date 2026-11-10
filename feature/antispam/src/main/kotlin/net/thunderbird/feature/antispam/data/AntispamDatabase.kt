package net.thunderbird.feature.antispam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ListSourceEntity::class, RuleEntity::class, AccountListLinkEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AntispamDatabase : RoomDatabase() {
    abstract fun dao(): AntispamDao

    companion object {
        fun create(context: Context): AntispamDatabase =
            Room.databaseBuilder(context, AntispamDatabase::class.java, "antispam.db").build()
    }
}
