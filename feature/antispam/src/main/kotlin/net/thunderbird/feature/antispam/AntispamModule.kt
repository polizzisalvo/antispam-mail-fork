package net.thunderbird.feature.antispam

import android.content.Context
import net.thunderbird.feature.antispam.data.AntispamDao
import net.thunderbird.feature.antispam.data.AntispamDatabase
import net.thunderbird.feature.antispam.update.AntispamCatalogSync
import net.thunderbird.feature.antispam.update.AntispamListDownloader
import net.thunderbird.feature.antispam.update.AntispamListUpdater
import org.koin.dsl.module

/** Lista di prova ospitata nel fork: modificala da GitHub per cambiare le regole senza ricompilare. */
private const val TEST_LIST_URL =
    "https://raw.githubusercontent.com/polizzisalvo/antispam-mail-fork/antispam-filter/feature/antispam/lists/test-list.txt"

val antispamModule = module {
    single { AntispamConfig(catalogUrl = null, testListUrl = TEST_LIST_URL) }
    single<AntispamDatabase> { AntispamDatabase.create(get<Context>()) }
    single<AntispamDao> { get<AntispamDatabase>().dao() }
    single { AntispamListDownloader() }
    single { AntispamFilterManager(dao = get()) }
    single {
        AntispamListUpdater(
            dao = get(),
            downloader = get(),
            onRulesChanged = { get<AntispamFilterManager>().invalidate() },
        )
    }
    single {
        AntispamCatalogSync(
            dao = get(),
            downloader = get(),
            catalogUrl = get<AntispamConfig>().catalogUrl.orEmpty(),
        )
    }
    single { AntispamSetup(dao = get(), updater = get(), manager = get()) }
    single { AntispamUpdateScheduler(workManager = get()) }
    single { AntispamStartup(scheduler = get(), setup = get(), config = get()) }
}
