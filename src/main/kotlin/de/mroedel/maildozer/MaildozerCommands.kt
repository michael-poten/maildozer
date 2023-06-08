package de.mroedel.maildozer

import de.mroedel.maildozer.model.RecipientSummary
import de.mroedel.maildozer.service.MailService
import org.jline.reader.LineReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import javax.annotation.PostConstruct


@ShellComponent
class MaildozerCommands {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    @Lazy
    private val lineReader: LineReader? = null

    @Autowired
    private val mailService: MailService? = null

    @Autowired
    private val backgroundTaskExecutor: ThreadPoolTaskExecutor? = null

    @PostConstruct
    fun init() {
//        CompletableFuture.supplyAsync(Supplier {
//            mailService!!.getFolderList()
//        }, backgroundTaskExecutor)
    }

    @ShellMethod("Starts interactive mode" )
    fun start() {

        val checkedSummaries: MutableList<String> = ArrayList()
        val recipientSummaries: MutableList<RecipientSummary> = ArrayList()

        mailService!!.refreshRecipientSummaries(recipientSummaries, checkedSummaries, 3)

        if (recipientSummaries.isEmpty()) {
            return
        }

        val completableFuture = CompletableFuture.supplyAsync(Supplier {
            mailService.refreshRecipientSummaries(recipientSummaries, checkedSummaries)
        })

        checkLoop@ while(true) {

            if (recipientSummaries.isEmpty()) break
            val recipientSummary = recipientSummaries.get(0)

            val fromAmount = recipientSummary.amountMails

            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            println("From:      ${recipientSummary.address}")

            for ((index, value) in recipientSummary.exampleMails.withIndex()) {
                if (index == 0) {
                    println("Examples:  ${value.sentDate} - ${value.subject}")
                }else{
                    println("          ${value.sentDate} - ${value.subject}")
                }
            }
            println("Amount:    " + fromAmount)
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            println()
            println("[n] Next")
//            println("[s] Show more infos")
            println("[d] Delete all")
            println("[m] Move all")
            println("[x] Abort")
            println()

            val answer = lineReader!!.readLine("Answer: ")

            when (answer) {
                "d" -> {
                    checkedSummaries.add(recipientSummary.address!!.toLowerCase())
                    recipientSummaries.remove(recipientSummaries.find { it.address.equals(recipientSummary.address) })

                    CompletableFuture.supplyAsync(Supplier {
                        mailService.deleteMailsByFrom(recipientSummary.address!!)
                    }, backgroundTaskExecutor)

                }
//                "s" -> mailService.getMailsByFrom(mail.from.address).forEach { println("${it.sentDate} - ${it.subject}") }
                "m" -> {
                    val folderList = mailService.getFolderList()
                    for ((index, folder) in folderList.withIndex()) {
                        println("[$index] ${folder.fullName}")
                    }

                    val folderSelected = lineReader.readLine("Answer: ")

                    checkedSummaries.add(recipientSummary.address!!.toLowerCase())
                    recipientSummaries.remove(recipientSummaries.find { it.address.equals(recipientSummary.address) })

                    CompletableFuture.supplyAsync(Supplier {
                        mailService.moveMailsToFolder(recipientSummary.address!!, folderList.get(folderSelected.toInt()))
                    }, backgroundTaskExecutor)

                }
                "x" -> {
                    val countThreads = backgroundTaskExecutor!!.getThreadPoolExecutor().activeCount
                    if (countThreads > 0) {
                        if( lineReader.readLine("$countThreads tasks in queue. Still abort? [y/n]").equals("y") ) {
                            completableFuture.cancel(true)
                            break@checkLoop
                        }
                    }else{
                        completableFuture.cancel(true)
                        break@checkLoop
                    }
                }
                else -> continue@checkLoop
            }
        }
    }

    @ShellMethod("Counts mails with given mail address." )
    fun check(address: String): String {

        val mailsAmount = mailService!!.amountMailsByFrom(address)

        return "$mailsAmount mails found"
    }

    @ShellMethod("Deletes mails with given mail address." )
    fun delete(address: String): String {

        val mailsAmount = mailService!!.deleteMailsByFrom(address)

        return "$mailsAmount deleted"
    }

    @ShellMethod("Lists all folders." )
    fun folder(@ShellOption("list") list: Boolean) {
        if (list) {
            mailService!!.getFolderList().forEach { println("$it") }
        }
    }

    @ShellMethod("Count mails older than 2 years." )
    fun countMails() {

        val amount = mailService!!.countMails()
        println("Amount mails older: " + amount)

    }


}