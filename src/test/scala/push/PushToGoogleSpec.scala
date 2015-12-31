package pull

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import drivedog.Google
import scala.collection.JavaConversions._
import drivedog.GoogleDriveList
import java.util.Date
import java.io.PrintWriter
import drivedog.LocalDriveList
import drivedog.MetaData
import com.google.api.client.http.ByteArrayContent
import base.BaseTest

class PushToGoogleSpec extends BaseTest with ShouldMatchers {
  
 
  "Clean Local Directory" should "download one file" in {
    GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents or title = '"+GoogleUnitTestDirectory+"'")
  }
  it should "be an old file dated approx 31 Jan 1980" in {
    val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt"
    val jan1980 = new Date(1980-1900, 0, 31);    
    val fileDate = new java.io.File(localFile).lastModified()
    assert(jan1980.getTime == fileDate)
    val meta = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/.drivedog/."+GoogleUnitTestDirectory)
    assert(meta.exists() == true)
  }
  
  it should "be updated and sent back to google" in {
     val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt"
     val pw = new PrintWriter(new java.io.File(localFile ))
     pw.write("Hello, world")
     pw.close  
     LocalDriveList.startPushProcess
  }
  it should "now be able to download back to local if I delete it" in {
     val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt"
     val local = new java.io.File(localFile)
     local.delete
     assert(local.exists == false)
     
     GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents or title = '"+GoogleUnitTestDirectory+"'")
     assert(local.exists == true)
     assert(scala.io.Source.fromFile(localFile).mkString == "Hello, world")
  }
  it should "now be able to create a new file to upload" in {
     val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/braveNewWorld.txt"
     val pw = new PrintWriter(new java.io.File(localFile ))
     pw.write("brave new world")
     pw.close  
     LocalDriveList.startPushProcess
     GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents or title = '"+GoogleUnitTestDirectory+"'")

     val meta = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.braveNewWorld.txt")
     assert(meta.exists() == true)
  }
  it should "be able to handle updated google fle and updated local file creating a conflict" in {
      val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/braveNewWorld.txt"
      val metaFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.braveNewWorld.txt"
      val googleInfo = MetaData.getLocalInformation(localFile, metaFile)
      val fileId=googleInfo.get.list("id")

      val file = Google.service.files().get(fileId).execute();
      val byteContent = new ByteArrayContent("application/text","this is some text I am putting on gdrive by hand".getBytes)
      Google.service.files().update(fileId, file, byteContent).execute();

      val pw = new PrintWriter(new java.io.File(localFile ))
      pw.write("a little edit on local disk")
      pw.close  
      LocalDriveList.startPushProcess

      val metaConflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.braveNewWorld.txt.conflict")
     assert(metaConflict.exists() == true)
  }
}
