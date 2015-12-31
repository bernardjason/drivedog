package drivedog

import java.nio.file.Files
import java.nio.file.Paths
import com.google.api.client.http.GenericUrl
import com.google.api.services.drive.model.File
import scala.collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging
import Google._

object GoogleFile extends StrictLogging {
    import MetaData._

  
  def getFile(file: File, path: StringBuilder) {
    
    logger.info(s"getFile file=${file.getTitle}, path=$path")
    
    Paths.get(path.mkString).toFile().mkdirs()
    val driveDogDir = s"${path}/.drivedog/".unixfile // new java.io.File(path + "/.drivedog/")
    driveDogDir.mkdirs()
    val metaFileName = driveDogDir+"/."+file.getTitle()
    
    val fileSystemPath = Paths.get(path + "/" + file.getTitle())
    val fileOnfilesystem = fileSystemPath.toFile()

    val local = MetaData.getLocalInformation(fileSystemPath.toString(),metaFileName)
    
    val r = local match {
      case Some(local) =>     MetaData.compareToGoogle(local, file)
      case None => OLDER 
    }
    logger.debug(s"getFile local is ${r} ${local}")
    if ( r == OLDER ) {
      if ( Google.isConflicted(metaFileName) == false ) {
        if ( isLocalNewer(fileOnfilesystem,metaFileName,file,local) == false) {
         downloadFileToLocalDisk(fileOnfilesystem, file, driveDogDir, fileSystemPath, metaFileName)
        }
      } else {
        FinalRunReport.add(FinalRunReport.CONFLICT, s"conflicted ${file.getTitle} see https://drive.google.com/drive/folders/${file.getId}",fileOnfilesystem.toString())
      }
    } else if ( r == NEWER ) {
      Google.anyConflicts=true
      logger.info(s"conflict, getFile local ${local} is ${r}")
      FinalRunReport.add(FinalRunReport.CONFLICT, s"conflicted mismatch between ${file.getTitle} see https://drive.google.com/drive/folders/${file.getId} ",fileOnfilesystem.toString())
    }

  }

  def downloadFileToLocalDisk(fileOnfilesystem: java.io.File, file: com.google.api.services.drive.model.File, 
      driveDogDir: java.io.File, fileSystemPath: java.nio.file.Path, metaFileName: String):Boolean = {
          
      logger.info(s"downloadFileToLocalDisk $file")
      if ( file.getMimeType == "application/vnd.google-apps.folder" ) {
        logger.info(s"mkdirs $fileOnfilesystem")
        MetaData.writeLocalInformation(metaFileName, file)
        fileOnfilesystem.mkdirs()
        FinalRunReport.add(FinalRunReport.PULL, s"directory ${file.getTitle}",fileOnfilesystem.toString())

      } else {

        val metaFile = metaFileName.unixfile // new java.io.File(metaFileName)
        if ( fileSystemPath.toFile().lastModified() >  metaFile.lastModified() ) {
          val conflict = new java.io.File(metaFileName+Google.CONFLICT)
          conflict.createNewFile()
          logger.warn(s"conflict $conflict")
          
          FinalRunReport.add(FinalRunReport.CONFLICT, s"conflict $conflict",fileOnfilesystem.toString())
          return false;
        }
        
        fileSystemPath.toFile().delete()
        
        downloadFileStream(file) match {
          case Some(content) => Files.copy(content, fileSystemPath)
          case None          => fileSystemPath.toFile().createNewFile()
        }
        MetaData.writeLocalInformation(metaFileName, file)
        FinalRunReport.add(FinalRunReport.PULL, s"file ${file.getTitle}",fileOnfilesystem.toString())

      }
      fileOnfilesystem.setLastModified(file.getModifiedDate.getValue )
      //new java.io.File(metaFileName).setLastModified(file.getModifiedDate.getValue )
      metaFileName.unixfile.setLastModified(file.getModifiedDate.getValue )
      true
  }
  

  
  def isLocalNewer(fileOnfilesystem:java.io.File, metaFileName:String, file:File, local:Option[Local]): Boolean = {
    val meta = metaFileName.unixfile // new java.io.File(metaFileName)
    val conflict = (metaFileName+Google.CONFLICT).unixfile // new java.io.File(metaFileName+Google.CONFLICT)
     
    if ( local.isDefined ) {
      val l = local.get
      if ( meta.lastModified() < fileOnfilesystem.lastModified() ) { // local file newer than meta data
        val googleDrive = file.toMap
        val r = MetaData.compareAsDateTime(l.list("modifiedDate"),    googleDrive("modifiedDate").asInstanceOf[com.google.api.client.util.DateTime].toString() )
        if ( r == OLDER ) { // google newer than meta data and local file is newer too
          logger.info(s"google newer than meta data and local file is newer too ${meta}")
          conflict.createNewFile()
          Google.anyConflicts=true
          return true
        }
      }       
    }

    false 
  }
  
  def downloadFileStream(file: File) = {
    if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
      val resp =
        Google.service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
          .execute();
      Some(resp.getContent())

    } else {
      None
    }
  }
}
