package de.mroedel.maildozer.model

import jakarta.mail.Message


class MailData(var amount: Int, var mails: List<Mail>, var messages: List<Message>) {

}