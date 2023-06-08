package de.mroedel.maildozer.service

import de.mroedel.maildozer.model.Folder
import de.mroedel.maildozer.model.Mail
import de.mroedel.maildozer.model.RecipientSummary
import de.mroedel.maildozer.repositories.FolderRepository
import jakarta.mail.MessagingException
import jakarta.mail.search.ReceivedDateTerm
import jakarta.mail.search.SearchTerm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
class MailService {

    @Autowired
    private val mailOperationService: MailOperationService? = null

    @Autowired
    val folderRepository: FolderRepository? = null

    @Throws(MessagingException::class, IOException::class)
    fun getMail(): Mail? {
        return mailOperationService!!.getMail()
     }

    @Throws(MessagingException::class)
    fun getMailsByFrom(from: String): List<Mail> {
        return mailOperationService!!.getMailsByFrom(from)
    }

    @Throws(MessagingException::class)
    fun amountMailsByFrom(from: String): Int {
        return mailOperationService!!.amountMailsByFrom(from)
    }

    @Throws(MessagingException::class)
    fun countMails(): Int {
        return mailOperationService!!.countMails()
    }

    @Throws(MessagingException::class)
    fun deleteMailsByFrom(from: String): Int {
        return mailOperationService!!.deleteMailsByFrom(from)
    }

    @Throws(MessagingException::class)
    fun getFolderList(): List<jakarta.mail.Folder> {
        val folderList = mailOperationService!!.getFolderList()

        folderRepository!!.deleteAll()
        for (folder in folderList) {
            val folderEntity = Folder()
            folderEntity.id = UUID.randomUUID().toString()
            folderEntity.folderPath = folder.fullName

            folderRepository!!.save(folderEntity)
        }

        return folderList
    }

    @Throws(MessagingException::class)
    fun getFolder(path: String): jakarta.mail.Folder? {
        return mailOperationService!!.getFolderByPath(path)
    }

    @Throws(MessagingException::class)
    fun refreshRecipientSummaries(currentRecipients: MutableList<RecipientSummary>, checkedSummaries: MutableList<String>, amountEntires: Int = -1) {
        mailOperationService!!.refreshRecipientSummaries(currentRecipients, checkedSummaries, amountEntires)
    }

    @Throws(MessagingException::class)
    fun moveMailsToFolder(fromAddress: String, folder: jakarta.mail.Folder): Int {
        return mailOperationService!!.moveAllFromAddressToFolder(fromAddress, folder)
    }

}