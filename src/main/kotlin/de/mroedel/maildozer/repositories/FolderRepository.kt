package de.mroedel.maildozer.repositories

import de.mroedel.maildozer.model.Folder
import org.springframework.data.jpa.repository.JpaRepository


interface FolderRepository : JpaRepository<Folder?, String?> {


}