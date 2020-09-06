package de.mroedel.maildozer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class MaildozerApplication

fun main(args: Array<String>) {
	runApplication<MaildozerApplication>(*args)
}