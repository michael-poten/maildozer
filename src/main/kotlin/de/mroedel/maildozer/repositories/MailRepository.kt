package de.mroedel.maildozer.repositories

import de.mroedel.maildozer.model.Mail
import org.springframework.data.mongodb.repository.MongoRepository


interface MailRepository : MongoRepository<Mail?, String?> {


}