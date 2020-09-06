package de.mroedel.maildozer.model

import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
class Mail(val messageId: String?,
           var imapFolder: String,
           var from: MailRecipient,
           var subject: String,
           var sentDate: Date) {

    override fun toString(): String {
        return "$sentDate ::: $from ::: $subject"
    }
}