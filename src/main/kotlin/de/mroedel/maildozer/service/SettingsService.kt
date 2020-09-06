package de.mroedel.maildozer.service

import de.mroedel.maildozer.model.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SettingsService {

    @Value("\${mail.imap.path}")
    val mailImapPath: String? = null

    @Value("\${mail.imap.server}")
    val mailImapServer: String? = null

    @Value("\${mail.imap.email}")
    val mailImapEmail: String? = null

    @Value("\${mail.imap.user}")
    val mailImapUser: String? = null

    @Value("\${mail.imap.password}")
    val mailImapPassword: String? = null

    fun getCurrentSettings(): Settings {
        return createSettings()
    }

    fun createSettings(): Settings {
        val settings = Settings()
        settings.imapPath = mailImapPath
        settings.imapServer = mailImapServer
        settings.imapEmail = mailImapEmail
        settings.imapUser = mailImapUser
        settings.imapPassword = mailImapPassword

        return settings
    }

}