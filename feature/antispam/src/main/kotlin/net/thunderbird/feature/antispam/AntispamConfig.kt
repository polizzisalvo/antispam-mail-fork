package net.thunderbird.feature.antispam

/**
 * @param catalogUrl JSON con le liste predefinite (vedi AntispamCatalogSync). null = nessun catalogo.
 * @param testListUrl lista personalizzata aggiunta automaticamente a tutti gli account (solo per i test
 *   iniziali, finché non c'è la schermata di configurazione). null = disattivata.
 */
data class AntispamConfig(
    val catalogUrl: String? = null,
    val testListUrl: String? = null,
)
