package de.mroedel.maildozer.repositories

import de.mroedel.maildozer.model.Mail
import org.springframework.data.jpa.repository.JpaRepository


interface MailRepository : JpaRepository<Mail?, String?> {


}