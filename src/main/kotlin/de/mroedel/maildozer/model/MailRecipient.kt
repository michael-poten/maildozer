package de.mroedel.maildozer.model

class MailRecipient(var address: String,
                    val name: String?) {

    override fun toString(): String {
        return "$name<$address>"
    }

}