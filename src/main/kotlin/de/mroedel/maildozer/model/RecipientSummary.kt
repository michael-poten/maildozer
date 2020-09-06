package de.mroedel.maildozer.model

import org.springframework.data.mongodb.core.mapping.Document

@Document
class RecipientSummary(var from: MailRecipient,
                       var exampleMails: List<Mail>,
                       var amountMails: Int = 0) {

    override fun toString(): String {
        return "$from - $amountMails"
    }
}