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

/**
 * @author jasonb
 * look for files deleted on local disk so that it can then delete them from google drive. Relies on the 
 * .drivedog meta data to spot that the file was there but isn't now
 */
object LocalDeleteList extends StrictLogging {

  def main(argc: Array[String]): Unit = {

    FinalRunReport.start

    delete
    println("\nfinal report")
    for (l <- FinalRunReport.produce) {
      println(l)
    }

  }

  def delete() {

    val interestingFile = ListBuffer[Path]()

    val fileProcessor = DeletedTreeProcessor(interestingFile);

    Files.walkFileTree(Paths.get(Google.ROOT), fileProcessor);

    for (p <- interestingFile) {
      val metaDataInfo = MetaData.getLocalInformation("", p.toString())
      if (metaDataInfo.isDefined) {
        logger.info(s"Interesting meta data only file ${p}");
        logger.debug(s"Interesting meta data only file ${p} , ${metaDataInfo.get}");

        if ( Google.isConflicted(p.toString()) == false ) {
          try {
            logger.info(s"delete ${p} ${metaDataInfo.get.list("id")}");

            Google.service.files().trash(metaDataInfo.get.list("id")).execute
  
            FinalRunReport.add(FinalRunReport.DELETE, 
                s"deleted ${p.getParent.getParent}/${metaDataInfo.get.list("title")}",p.toString())
  
          } catch {
            case ex: com.google.api.client.googleapis.json.GoogleJsonResponseException =>
              logger.warn(s"${p} already missing from google")
          }
          p.toFile().delete()
        } else {
          FinalRunReport.add(FinalRunReport.DELETE, 
              s"conflicted file, investigate ${p.getParent.getParent}/${metaDataInfo.get.list("title")}",p.toString())
        }
      }
    }
  }

  def deleteLocalFile(path: Path, meta: Path) {

    val renameTo = (meta.toString() + ".deleted").unixfile
    meta.toFile().renameTo(renameTo)

    val renameFile = s"${path.getParent.toString()}/.drivedog/${path.getFileName}".unixfile 
      //new java.io.File(path.getParent.toString() + "/.drivedog/" + path.getFileName)

    path.toFile().renameTo(renameFile)

    FinalRunReport.add(FinalRunReport.DELETE, s"deleted $path to ${renameFile}",renameFile.toString())
    logger.warn(s"$path missing, deleted on Google")
  }

  case class DeletedTreeProcessor(interestingFile: ListBuffer[Path]) extends SimpleFileVisitor[Path] {
    override def visitFile(path: Path, attributes: BasicFileAttributes): FileVisitResult = {
      checkIfInteresting(path)
      return FileVisitResult.CONTINUE;
    }

    def checkIfInteresting(metaFile: java.nio.file.Path) = {
      if (metaFile.toString().contains(".drivedog/.")) {
        val pathFile =(metaFile.toString().replace(".drivedog/.", "")).unixfile

        if (metaFile.toString.endsWith(".deleted") == false && metaFile.toString.endsWith(".conflict") == false && pathFile.exists() == false && metaFile.toFile().isFile()) {
          interestingFile += metaFile
        }
      }

    }

    override def preVisitDirectory(path: Path, attributes: BasicFileAttributes): FileVisitResult = {
      val fn = path.getFileName

      logger.info(s"Processing directory:$path");

      checkIfInteresting(path)

      return FileVisitResult.CONTINUE;
    }
  }
}