package de.mroedel.maildozer.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Folder {

    @Id
    var id: String? = null
    var folderPath: String? = null

}