package de.mroedel.maildozer.model

class RecipientSummary(var address: String? = null,
                       var name: String? = null,
                       var exampleMails: List<Mail>,
                       var amountMails: Int = 0) {

    override fun toString(): String {
        return "$address - $amountMails"
    }
}