package de.mroedel.maildozer.model

import org.springframework.data.mongodb.core.mapping.Document

@Document
class Folder(val folderPath: String) {

}