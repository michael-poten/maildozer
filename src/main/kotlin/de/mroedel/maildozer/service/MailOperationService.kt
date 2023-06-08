package de.mroedel.maildozer.service

import com.sun.mail.imap.OlderTerm
import de.mroedel.maildozer.model.Mail
import de.mroedel.maildozer.model.MailData
import de.mroedel.maildozer.model.RecipientSummary
import de.mroedel.maildozer.model.Settings
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.search.FromTerm
import jakarta.mail.search.SearchTerm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Supplier


@Service
class MailOperationService {

    @Autowired
    private val settingsService: SettingsService? = null

    @Autowired
    private val mailFetchTaskExecutor: ThreadPoolTaskExecutor? = null

    @Throws(MessagingException::class, IOException::class)
    fun deleteMailsByFrom(fromAddress: String): Int {

        val operation = {messages: Array<Message>, folder: jakarta.mail.Folder ->
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
    fun countMails(): Int {

//        val localDate: LocalDate = LocalDate.now().minusYears(1)
//        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val searchTerm: SearchTerm = OlderTerm(60 * 60 * 24 * 364)

        return executeMailOperation(searchTerm, true, null, 10000).amount
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
    fun moveAllFromAddressToFolder(fromAddress: String, toFolder: jakarta.mail.Folder): Int {

        val address: Address = InternetAddress(fromAddress)
        val searchTerm: SearchTerm = FromTerm(address)

        val operation = { messages: Array<Message>, folder: jakarta.mail.Folder ->
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
    fun getFolderList(): List<jakarta.mail.Folder> {
        return executeFolderOperation()
    }

    @Throws(MessagingException::class)
    fun getFolderByPath(path: String): jakarta.mail.Folder? {
        return executeFolderOperation(path).getOrNull(0)
    }

    fun refreshRecipientSummaries(currentRecipients: MutableList<RecipientSummary>, checkedSummaries: MutableList<String>, amountEntries: Int) {

        if (amountEntries != -1 && currentRecipients.size >= amountEntries) {
            return
        }

        executeGeneralOperation { settings: Settings, folder: jakarta.mail.Folder ->

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
                        if (!currentRecipients.map { it.address }.contains(mail.address) && !checkedSummaries.contains(mail.address!!.toLowerCase())) {
                            fromAddress = getRecipientSummary(mail.address)
                        }

                        synchronized(currentRecipients) {

                            if (fromAddress != null &&
                                    !currentRecipients.map { it.address }.contains(mail.address) &&
                                    !checkedSummaries.contains(mail.address!!.toLowerCase())) {
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

    fun getRecipientSummary(fromAddress: String?): RecipientSummary? {

        return executeGeneralOperation { settings: Settings, folder: jakarta.mail.Folder ->

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

                RecipientSummary(exampleMails[0].address, exampleMails[0].name, exampleMails, amountMails)
            }else{
                null
            }
        }
    }

    private fun executeFolderOperation(folderListPattern: String? = null): List<jakarta.mail.Folder> {

        val generalOperation = {settings: Settings, folderInput: jakarta.mail.Folder ->
            val folders: MutableList<jakarta.mail.Folder> = ArrayList()

            if (folderListPattern != null) {

                var currentFolder: jakarta.mail.Folder? = null
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
                    if (folder.type and jakarta.mail.Folder.HOLDS_MESSAGES != 0) {
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
                                     operation: ((Array<Message>, jakarta.mail.Folder) -> Unit)?,
                                     limit: Int = 100000): MailData {

        return executeGeneralOperation { settings: Settings, folder: jakarta.mail.Folder ->
            var msgs: Array<Message>
            if (searchTerm != null) {
                msgs = folder.search(searchTerm)
            }else{
                msgs = folder.getMessages(1, limit)
            }

            msgs = msgs.take(limit).toTypedArray()

            operation?.invoke(msgs, folder)

            if (convert) {
                val mails: MutableList<Mail> = ArrayList()

                for ((index, message) in msgs.withIndex()) {
                    val tmpMsg = message as MimeMessage
                    val tmpMail = convertToMail(tmpMsg) ?: continue
                    mails.add(tmpMail)

                    println("Convert mail " + (index + 1) + "/" + msgs.size)
                }

                MailData(msgs.size, mails, msgs.toList())
            }else{
                MailData(msgs.size, ArrayList(), msgs.toList())
            }
        }
    }

    private fun <T> executeGeneralOperation(generalOperation: (settings: Settings, folder: jakarta.mail.Folder) -> T ): T {
        val props = Properties()
// props.setProperty("mail.imaps.ssl.trust", host);
        props.setProperty("mail.store.protocol", "imap")
        props.setProperty("mail.mime.base64.ignoreerrors", "true")
        props.setProperty("mail.imap.partialfetch", "false")
        props.setProperty("mail.imaps.partialfetch", "false")
        props.setProperty("mail.imap.fetchsize", "10000k")
        props.setProperty("mail.imaps.fetchsize", "10000k")

        val session = Session.getInstance(props)
        var folder: jakarta.mail.Folder? = null
        var store: Store? = null
        try {
            val settings: Settings = settingsService!!.getCurrentSettings()
            store = session.getStore("imaps")
            store.connect(settings.imapServer, settings.imapUser, settings.imapPassword)
            folder = store.getFolder(settings.imapPath)
            folder!!.open(jakarta.mail.Folder.READ_WRITE)

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

            val messageId: String? = message.messageID

            val subject: String? = message.subject
            val sentDate: Date? = message.receivedDate
            val content: String? = getTextFromMessage(message)
            val size: Int = message.size

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

            val mail = Mail()
            mail.id = UUID.randomUUID().toString()
            mail.messageId = messageId
            mail.imapFolder = imapFolder
            mail.address = tmpFromAddress
            mail.name = tmpFromName
            mail.subject = subject
            mail.sentDate = sentDate
            mail.size = size
            mail.content = content

            return mail
        }catch(e: java.lang.Exception) {
            return null
        }
    }

    private fun getTextFromMessage(message: Message): String? {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        }
        if (message.isMimeType("multipart/*")) {
            val mimeMultipart: MimeMultipart = message.content as MimeMultipart
            return getTextFromMimeMultipart(mimeMultipart)
        }
        return ""
    }

    private fun getTextFromMimeMultipart(
        mimeMultipart: MimeMultipart
    ): String {
        var result = ""
        for (i in 0 until mimeMultipart.getCount()) {
            val bodyPart: BodyPart = mimeMultipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/plain")) {
                return """
                $result
                ${bodyPart.content}
                """.trimIndent() // without return, same text appears twice in my tests
            }
            result += parseBodyPart(bodyPart)
        }
        return result
    }

    private fun parseBodyPart(bodyPart: BodyPart): String {
        if (bodyPart.isMimeType("text/html")) {
            return "\n" + org.jsoup.Jsoup
                .parse(bodyPart.content.toString())
                .text()
        }
        return if (bodyPart.content is MimeMultipart) {
            getTextFromMimeMultipart(bodyPart.content as MimeMultipart)
        } else ""
    }

}