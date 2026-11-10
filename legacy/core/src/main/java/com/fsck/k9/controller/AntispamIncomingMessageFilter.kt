package com.fsck.k9.controller

import net.thunderbird.feature.antispam.AntispamFilterManager

/** Collega il motore antispam al MessagingController. */
class AntispamIncomingMessageFilter(
    private val manager: AntispamFilterManager,
) : IncomingMessageFilter {
    override fun isSpam(accountUuid: String, subject: String, fromAddress: String, bodySnippet: String): Boolean =
        manager.isSpam(accountUuid, subject, fromAddress, bodySnippet)
}
