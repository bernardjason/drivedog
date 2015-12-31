package delete

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import drivedog.Google
import scala.collection.JavaConversions._
import drivedog.GoogleDriveList
import java.util.Date
import base.BaseTest
import drivedog.LocalDeleteList
import drivedog.MetaData

class DeleteFromLocal extends BaseTest with ShouldMatchers {
  
  "Clean Local Directory" should "download one file" in {
      deleteMarkerFile(dir.getId)
    GoogleDriveList.refreshToLocal(dir.getId,"'"+dir.getId +"' in parents")
  } 

  it should "ignore file if it is in conflict" in {
     val conflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt.conflict")
     conflict.createNewFile()
     val localFile=new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt")
     
     localFile.delete()
     LocalDeleteList.delete()
     assert(Google.anyConflicts == true)
  } 
  it should "now delete the file from google as conflict removed" in {
     val metaFile = LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt"
     val realFile = LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/helloworldOld.txt"

     val conflict = new java.io.File(LocalUnitTestDirectory+Google.MYDRIVE+"/"+GoogleUnitTestDirectory+"/.drivedog/.helloworldOld.txt.conflict")
     conflict.delete
     
     val meta = MetaData.getLocalInformation(realFile, metaFile)
     println(meta.get.list("id"))
     
     assert(Google.service.files().get(meta.get.list("id")).execute().getLabels.getTrashed == false )
     LocalDeleteList.delete()
     assert(Google.service.files().get(meta.get.list("id")).execute().getLabels.getTrashed == true )

     
  }


}
