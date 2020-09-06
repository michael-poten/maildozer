package de.mroedel.maildozer.repositories

import de.mroedel.maildozer.model.Folder
import org.springframework.data.mongodb.repository.MongoRepository


interface FolderRepository : MongoRepository<Folder?, String?> {


}