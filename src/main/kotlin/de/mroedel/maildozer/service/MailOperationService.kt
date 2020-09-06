package de.mroedel.maildozer.service

import de.mroedel.maildozer.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Supplier
import javax.mail.*
import javax.mail.Folder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.search.FromTerm
import javax.mail.search.SearchTerm
import kotlin.collections.ArrayList

@Service
class MailOperationService {

    @Autowired
    private val settingsService: SettingsService? = null

    @Autowired
    private val mailFetchTaskExecutor: ThreadPoolTaskExecutor? = null

    @Throws(MessagingException::class, IOException::class)
    fun deleteMailsByFrom(fromAddress: String): Int {

        val operation = {messages: Array<Message>, folder: Folder ->
            for (message in messages) {
                message.setFlag(Flags.Flag.DELETED, true)
            }
        }

        val address: Address = InternetAddress(fromAddress)
        val searchTerm: SearchTerm = FromTerm(address)

        return executeMailOperation(searchTerm, false, operation).amount
     }

    @Throws(MessagingException::class)
    fun amountMailsByFrom(fromAddress: String): Int {

        val address: Address = InternetAddress(fromAddress)
        val searchTerm: SearchTerm = FromTerm(address)

        return executeMailOperation(searchTerm, false, null).amount
    }

    @Throws(MessagingException::class)
    fun getMailsByFrom(fromAddress: String): List<Mail> {

        val address: Address = InternetAddress(fromAddress)
        val searchTerm: SearchTerm = FromTerm(address)

        return executeMailOperation(searchTerm, true, null, 10).mails
    }

    @Throws(MessagingException::class)
    fun getMail(): Mail? {
        return executeMailOperation(null, true, null, 1).mails.getOrNull(0)
    }

    @Throws(MessagingException::class, IOException::class)
    fun moveAllFromAddressToFolder(fromAddress: String, toFolder: Folder): Int {

        val address: Address = InternetAddress(fromAddress)
        val searchTerm: SearchTerm = FromTerm(address)

        val operation = {messages: Array<Message>, folder: Folder ->
            try{
                folder.copyMessages(messages, toFolder)
            }catch(e: java.lang.Exception) {
                e.printStackTrace()
            }
            for (message in messages) {
                message.setFlag(Flags.Flag.DELETED, true)
            }
        }

        return executeMailOperation(searchTerm, false, operation).amount
    }

    @Throws(MessagingException::class)
    fun getFolderList(): List<Folder> {
        return executeFolderOperation()
    }

    @Throws(MessagingException::class)
    fun getFolderByPath(path: String): Folder? {
        return executeFolderOperation(path).getOrNull(0)
    }

    fun refreshRecipientSummaries(currentRecipients: MutableList<RecipientSummary>, checkedSummaries: MutableList<String>, amountEntries: Int) {

        if (amountEntries != -1 && currentRecipients.size >= amountEntries) {
            return
        }

        executeGeneralOperation { settings: Settings, folder: Folder ->

            val messages = folder.messages.toList().chunked(10)

            for (msgChunk in messages) {

                val completableFuturesList: MutableList<CompletableFuture<Unit>> = java.util.ArrayList()
                for (msg in msgChunk) {
                    val completableFuture = CompletableFuture.supplyAsync(Supplier {

                        if (amountEntries != -1 && currentRecipients.size >= amountEntries) {
                            return@Supplier
                        }

                        val mail: Mail = convertToMail(msg as MimeMessage) ?: return@Supplier

                        var fromAddress: RecipientSummary? = null
                        if (!currentRecipients.map { it.from.address }.contains(mail.from.address) && !checkedSummaries.contains(mail.from.address.toLowerCase())) {
                            fromAddress = getRecipientSummary(mail.from.address)
                        }

                        synchronized(currentRecipients) {

                            if (fromAddress != null &&
                                    !currentRecipients.map { it.from.address }.contains(mail.from.address) &&
                                    !checkedSummaries.contains(mail.from.address.toLowerCase())) {
                                currentRecipients.add(fromAddress)
                                currentRecipients.sortByDescending { it.amountMails }
                            }
                        }

                    }, mailFetchTaskExecutor)
                    completableFuturesList.add(completableFuture)
                }

                val combinedFuture = CompletableFuture.allOf(*completableFuturesList.toTypedArray())
                try {
                    combinedFuture.get()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                }

            }
        }
    }

    fun getRecipientSummary(fromAddress: String): RecipientSummary? {

        return executeGeneralOperation { settings: Settings, folder: Folder ->

            val address: Address = InternetAddress(fromAddress)
            val searchTerm: SearchTerm = FromTerm(address)
            var msgs = folder.search(searchTerm)

            val amountMails: Int = msgs.size

            msgs = msgs.take(5).toTypedArray()

            if (msgs.isNotEmpty()) {
                val exampleMails: MutableList<Mail> = ArrayList()
                for (msg in msgs) {
                    exampleMails.add(convertToMail(msg as MimeMessage) ?: continue)
                }
                val recipient: MailRecipient = exampleMails[0].from
                RecipientSummary(recipient, exampleMails, amountMails)
            }else{
                null
            }
        }
    }

    private fun executeFolderOperation(folderListPattern: String? = null): List<Folder> {

        val generalOperation = {settings: Settings, folderInput: Folder ->
            val folders: MutableList<Folder> = ArrayList()

            if (folderListPattern != null) {

                var currentFolder: Folder? = null
                for (folderPart in folderListPattern.split("/")) {
                    if (currentFolder == null) {
                        currentFolder = folderInput.getFolder(folderPart)
                    }else{
                        currentFolder = currentFolder.getFolder(folderPart)
                    }
                }

                if (currentFolder != null) {
                    folders.add(currentFolder)
                }
            }else{
                for (folder in folderInput.list("*")) {
                    if (folder.type and Folder.HOLDS_MESSAGES != 0) {
                        folders.add(folder)
                    }
                }
            }

            folders
        }

        return executeGeneralOperation(generalOperation)
    }

    private fun executeMailOperation(searchTerm: SearchTerm?,
                                     convert: Boolean,
                                     operation: ((Array<Message>, Folder) -> Unit)?,
                                     limit: Int = 100000 ): MailData {

        return executeGeneralOperation { settings: Settings, folder: Folder ->
            var msgs: Array<Message>
            if (searchTerm != null) {
                msgs = folder.search(searchTerm)
            }else{
                msgs = folder.messages
            }

            msgs = msgs.take(limit).toTypedArray()

            operation?.invoke(msgs, folder)

            if (convert) {
                val mails: MutableList<Mail> = ArrayList()
                for (message in msgs) {
                    val tmpMsg = message as MimeMessage
                    val tmpMail = convertToMail(tmpMsg) ?: continue

                    mails.add(tmpMail)
                }

                MailData(msgs.size, mails, msgs.toList())
            }else{
                MailData(msgs.size, ArrayList(), msgs.toList())
            }
        }
    }

    private fun <T> executeGeneralOperation(generalOperation: (settings: Settings, folder: Folder) -> T ): T {
        val session = Session.getDefaultInstance(Properties())
        var folder: Folder? = null
        var store: Store? = null
        try {
            val settings: Settings = settingsService!!.getCurrentSettings()
            store = session.getStore("imaps")
            store.connect(settings.imapServer, settings.imapUser, settings.imapPassword)
            folder = store.getFolder(settings.imapPath)
            folder!!.open(Folder.READ_WRITE)

            return generalOperation(settings, folder)
        } catch (e: MessagingException) {
            e.printStackTrace()
            throw MessagingException(e.message)
        } finally {
            try {
                folder!!.close(true)
                store!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun convertToMail(message: MimeMessage): Mail? {

        try{
            var messageId: String? = null
            val msgID = message.getHeader("Message-ID")
            if (msgID != null && msgID.isNotEmpty()) {
                messageId = msgID[0]
            }

            val subject: String = message.subject
            val sentDate: Date = message.sentDate

            val imapFolder: String =  message.folder.fullName
            var tmpFromAddress = ""
            var tmpFromName: String? = null

            val fromAddress: Array<Address?> = message.from
            if (fromAddress.isNotEmpty()) {
                val addrFrom = fromAddress[0] as InternetAddress

                val addressFrom: String? = addrFrom.address
                if (addressFrom != null && addressFrom !== "") {
                    tmpFromAddress = addressFrom
                }

                val nameSender: String? = addrFrom.personal
                if (nameSender != null && nameSender !== "") {
                    tmpFromName = nameSender
                }
            }

            val senderAddress: Address? = message.sender
            if (senderAddress != null && tmpFromAddress == "") {
                val addrSender = senderAddress as InternetAddress

                val addressSender: String? = addrSender.address
                if (addressSender != null && addressSender !== "") {
                    tmpFromAddress = addressSender
                }

                val nameSender: String? = addrSender.personal
                if (nameSender != null && nameSender !== "") {
                    tmpFromName = nameSender
                }
            }

            return Mail(messageId, imapFolder, MailRecipient(tmpFromAddress, tmpFromName), subject, sentDate)
        }catch(e: java.lang.Exception) {
            return null
        }
    }

}