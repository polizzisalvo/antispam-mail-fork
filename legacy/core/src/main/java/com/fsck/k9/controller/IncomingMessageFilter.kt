package com.fsck.k9.controller

/** Hook chiamato dal MessagingController per ogni nuovo messaggio scaricato in Inbox. */
fun interface IncomingMessageFilter {
    fun isSpam(accountUuid: String, subject: String, fromAddress: String, bodySnippet: String): Boolean

    companion object {
        @JvmField
        val NONE = IncomingMessageFilter { _, _, _, _ -> false }
    }
}
