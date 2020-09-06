package de.mroedel.maildozer.service

import de.mroedel.maildozer.model.Folder
import de.mroedel.maildozer.model.Mail
import de.mroedel.maildozer.model.RecipientSummary
import de.mroedel.maildozer.repositories.FolderRepository
import microsoft.exchange.webservices.data.core.service.item.EmailMessage
import microsoft.exchange.webservices.data.core.service.item.Item
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import javax.mail.FetchProfile
import javax.mail.MessagingException

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
    fun deleteMailsByFrom(from: String): Int {
        return mailOperationService!!.deleteMailsByFrom(from)
    }

    @Throws(MessagingException::class)
    fun getFolderList(): List<javax.mail.Folder> {
        val folderList = mailOperationService!!.getFolderList()

        folderRepository!!.deleteAll()
        for (folder in folderList) {
            folderRepository!!.save(Folder(folder.fullName))
        }

        return folderList
    }

    @Throws(MessagingException::class)
    fun getFolder(path: String): javax.mail.Folder? {
        return mailOperationService!!.getFolderByPath(path)
    }

    @Throws(MessagingException::class)
    fun refreshRecipientSummaries(currentRecipients: MutableList<RecipientSummary>, checkedSummaries: MutableList<String>, amountEntires: Int = -1) {
        mailOperationService!!.refreshRecipientSummaries(currentRecipients, checkedSummaries, amountEntires)
    }

    @Throws(MessagingException::class)
    fun moveMailsToFolder(fromAddress: String, folder: javax.mail.Folder): Int {
        return mailOperationService!!.moveAllFromAddressToFolder(fromAddress, folder)
    }

}