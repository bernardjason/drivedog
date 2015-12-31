package drivedog

import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Paths
import java.nio.file.Files
import scala.collection.mutable.ListBuffer
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.ParentReference
import scala.collection.JavaConversions._
import com.google.api.client.http.ByteArrayContent
import com.typesafe.scalalogging.StrictLogging
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import Google._
object LocalDriveList extends StrictLogging {

  def main(argc: Array[String]): Unit = {
    FinalRunReport.start
    startPushProcess
    println("\nfinal report")
    for (l <- FinalRunReport.produce) {
      println(l)
    }

  }
  def startPushProcess: Unit = {
    val interestingFile = ListBuffer[Path]()

    val fileProcessor = TreeProcessor(interestingFile);

    Files.walkFileTree(Paths.get(Google.ROOT), fileProcessor);

    interestingFile -= Paths.get(Google.ROOT)
    interestingFile -= Paths.get(Google.ROOT + "/" + Google.MYDRIVE)

    for (p <- interestingFile) {
      logger.info(s"Interesting ${p}");
    }
    pushInterestingFiles(interestingFile)
  }

  def pushInterestingFiles(interestingFile: ListBuffer[Path]) {
    for (path <- interestingFile) {
      (path.getParent + "/.drivedog/").unixfile.mkdirs
      val metaData = Paths.get(path.getParent + "/.drivedog/." + path.getFileName)
      val conflict = (metaData + Google.CONFLICT).unixfile

      val metaDataFile = metaData.toFile()
      val pathFile = path.toFile()

      val metaParent = path.getParent.getParent.toString() + "/.drivedog/." + path.getParent.getFileName

      val parentLocal = MetaData.getLocalInformation(path.getParent.toString(), metaParent)

      val parentId = if (parentLocal.isDefined) {
        parentLocal.get.list("id")
      } else {
        null;
      }

      if ( !conflict.exists ) {
          if (metaDataFile.exists()) {
            try {
              updateFile(path, metaData)
            } catch {
              case ex: GoogleJsonResponseException => {
                if (ex.getStatusCode == 404) {
                  LocalDeleteList.deleteLocalFile(path, metaData)
                }
              }
            }
          } else {
            val newFile = insertFile(path, parentId)
            MetaData.writeLocalInformation(metaData.toString(), newFile)
            metaDataFile.setLastModified(pathFile.lastModified())
          }
      } else {
        Google.anyConflicts=true
        logger.info(s"skipped interesting file as in conflict ${path}");
      }
    }
  }

  def insertFile(path: Path, parentId: String) = {
    logger.info(s"insert file $path $parentId")

    val pathFile = path.toFile()
    val title = path.getFileName.toString()
    val mimeType = if (pathFile.isFile()) Google.FILE else Google.FOLDER
    val body = new File();
    body.setTitle(title);
    body.setDescription(title);
    body.setMimeType(mimeType);

    if (parentId != null && parentId.length() > 0) {
      val parentRef = new ParentReference()
      parentRef.setId(parentId)
      body.setParents(List(parentRef).toList)
    }

    val file = if (pathFile.isDirectory()) {
      Google.service.files().insert(body).execute
    } else {
      val mediaContent = new FileContent(mimeType, pathFile)
      Google.service.files().insert(body, mediaContent).execute();
    }
    FinalRunReport.add(FinalRunReport.PUSH, s"insert ${path}",path.toString())

    logger.debug(s"inserted file $file")

    file
  }

  def updateFile(path: Path, metaData: Path) {

    logger.info(s"updateFile $path $metaData")
    val metaDataInfo = MetaData.getLocalInformation(path.toString(), metaData.toString())

    if (metaDataInfo.isDefined) {
      val fileId = metaDataInfo.get.list("id")
      val pathFile = path.toFile()
      val mimeFile = metaData.toFile()
      val mimeType = if (pathFile.isFile()) Google.FILE else Google.FOLDER
      
      val file = Google.service.files().get(fileId).execute();

      logger.debug(s"google current file $file")

      val localMetaFileComparedToGoogle = MetaData.compareAsDateTime(metaDataInfo.get.list("modifiedDate"), file.getModifiedDate.toString())

      logger.debug(s"compared to google $localMetaFileComparedToGoogle")

      import MetaData._
      if (pathFile.lastModified() > mimeFile.lastModified() && (file.getLabels.getTrashed == true || localMetaFileComparedToGoogle == SAME || localMetaFileComparedToGoogle == NEWER)) {
        val fileContent = path.toFile()
        val mediaContent = new FileContent(mimeType, fileContent);

        file.getLabels.setTrashed(false)

        val updatedFile = if (pathFile.isFile()) {
          logger.info(s"updating file")
          Google.service.files().update(fileId, file, mediaContent).execute();
          FinalRunReport.add(FinalRunReport.PUSH, s"update file ${file.getTitle} ${path}",path.toString())
        } else {
          logger.info(s"updating directory")
          Google.service.files().update(fileId, file).execute();
          FinalRunReport.add(FinalRunReport.PUSH, s"update directory ${file.getTitle} ${path}",path.toString())
        }

        val newInfoOnfile = Google.service.files().get(fileId).execute();
        MetaData.writeLocalInformation(metaData.toString(), newInfoOnfile)
        mimeFile.setLastModified(pathFile.lastModified())

        logger.debug(s"new meta data $newInfoOnfile")
      } else {
        val conflict = (metaData + Google.CONFLICT).unixfile
        conflict.createNewFile()
        logger.warn(s"conlfict $conflict")
        FinalRunReport.add(FinalRunReport.CONFLICT, s"conlfict $conflict",path.toString())
        Google.anyConflicts=true
      }
    } else {
      throw new RuntimeException("update a file but not meta data")
    }
  }

  case class TreeProcessor(interestingFile: ListBuffer[Path]) extends SimpleFileVisitor[Path] {
    override def visitFile(path: Path, attributes: BasicFileAttributes): FileVisitResult = {
      logger.info(s"Processing file:$path");
      checkIfInteresting(path)
      return FileVisitResult.CONTINUE;
    }

    def checkIfInteresting(path: java.nio.file.Path) = {
      val metaData = Paths.get(path.getParent + "/.drivedog/." + path.getFileName)
      val metaDataFile = metaData.toFile()
      val pathFile = path.toFile()
      if (metaDataFile.exists()) {
        if (metaDataFile.lastModified() < pathFile.lastModified()) {
          interestingFile += path
        }
      } else {
        interestingFile += path
      }
    }

    override def preVisitDirectory(path: Path, attributes: BasicFileAttributes): FileVisitResult = {
      val fn = path.getFileName
      if (fn.endsWith(".drivedog")) return FileVisitResult.SKIP_SUBTREE

      logger.info(s"Processing directory:$path");

      checkIfInteresting(path)

      return FileVisitResult.CONTINUE;
    }
  }

}