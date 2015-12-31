package drivedog

import java.io.PrintWriter
import java.nio.file.Path
import scala.collection.JavaConversions._
import com.google.api.services.drive.model.File
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.StrictLogging

case class Local(real:String,metaPath:String,unsortedlist:java.util.List[String]) {
  val actualNativeFile = new java.io.File(real)
  val lastModified=actualNativeFile.lastModified()
  
  
  val list:scala.collection.mutable.Map[ String,String ] = scala.collection.mutable.Map()
  for( l <- unsortedlist ) {
    val a = l.split(",",2)
    val tt = new Tuple2( a(0),a(1) )
    list +=  tt 
  } 
}



object MetaData extends StrictLogging {

  def getLocalInformation(real:String,path:String):Option[Local] = {
    if ( new java.io.File(path).exists ) {
      try {
        Some(Local(real,path,Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)))
      } catch {
        case ex:java.nio.charset.MalformedInputException => logger.warn(s"$path not a meta file, ignored")
        None
      }
    } else {
      None
    }
  }
  
  def writeLocalInformation(path:String,file:File) {
    
    val pw = new PrintWriter(path)
    for( l <- file.toList.sortBy(_._1) ) {
      pw.printf("%s,%s\n",l._1.toString(),l._2.toString())      
    }
    pw.close();
  }
  
 
  def compareToGoogle(local:Local,file:File):DateCompare = {
    val googleDrive = file.toMap
    if ( local.actualNativeFile.exists() == false ) return OLDER

    logger.debug(s"conflict check ${file}"+local.list("modifiedDate") +"  "+googleDrive("modifiedDate").asInstanceOf[com.google.api.client.util.DateTime].toString())
    compareAsDateTime(local.list("modifiedDate"),    googleDrive("modifiedDate").asInstanceOf[com.google.api.client.util.DateTime].toString() )
  }
  
  
  def compareAsDateTime(local:String,google:String):DateCompare = {
    val df = new SimpleDateFormat(Google.DATE_FORMAT)
    val localDate = df.parse(local)
    val googleDate = df.parse(google)
    localDate.compareTo(googleDate) match {
      case -1 => OLDER
      case 0 => SAME
      case 1 => NEWER
    }
  }
  
  trait DateCompare { val compare:Int }
  case object OLDER extends DateCompare { val compare = -1 }
  case object SAME extends DateCompare { val compare = 0 }
  case object NEWER extends DateCompare { val compare = 1 }

}
