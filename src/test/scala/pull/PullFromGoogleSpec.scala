package pull

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import drivedog.Google
import scala.collection.JavaConversions._
import drivedog.GoogleDriveList
import java.util.Date
import base.BaseTest

class PullFromGoogleSpec extends BaseTest with ShouldMatchers {
  
    "Clean Local Directory" should "download one file" in {
      deleteMarkerFile(dir.getId)
    GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents")
  }
  it should "be an old file dated approx 31 Jan 1980" in {
    val localFile=LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt"
    val jan1980 = new Date(1980-1900, 0, 31);    
    val fileDate = new java.io.File(localFile).lastModified()
    assert(jan1980.getTime == fileDate)
  }
  
  
  "Now remove meta data" should "cause a conflict as Google has the same file" in {
    deleteMarkerFile(dir.getId)
    new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt").delete()
    GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents")
  }
  it should "have created a conflict file" in {
    val conflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt.conflict")
    assert(conflict.exists() == true)
  }
  
  "Now remove the conflicted file" should "still fail to download" in {
    deleteMarkerFile(dir.getId)
    
      val localFile=new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt")
      val conflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt.conflict")

      localFile.delete()
      
      val localFileExists=new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt")

      GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents")

      assert(localFileExists.exists() == false)
  }
  
  "Now remove the conflict file and conflicted file" should "download" in {
      
      deleteMarkerFile(dir.getId)
      
      val localFile=new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt")
      val conflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt.conflict")

      localFile.delete
      conflict.delete
      GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents")

      val jan1980 = new Date(1980-1900, 0, 31);    
      val fileDate = localFile.lastModified()
      assert(jan1980.getTime == fileDate)
  }
  
  
}
