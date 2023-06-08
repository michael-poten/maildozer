package de.mroedel.maildozer.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Mail {

    @Id
    var id: String? = null
    var messageId: String? = null
    var imapFolder: String? = null
    var address: String? = null
    var name: String? = null
    var subject: String? = null
    var sentDate: Date? = null
    var content: String? = null
    var size: Int? = null

    override fun toString(): String {
        return "$sentDate ::: $address ::: $subject"
    }
}