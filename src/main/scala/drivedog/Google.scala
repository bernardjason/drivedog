package drivedog

import java.net._

object Google  {
    System.setProperty("user.timezone", "UTC");

    val service = QuickSetup.getDriveService();
    val DATE_FORMAT="YYYY-MM-dd'T'HH:mm:ss.SSS'Z'"
    val CONFLICT = ".conflict"
    val FOLDER = "application/vnd.google-apps.folder"
    val FILE = "text/plain"
    val MYDRIVE = "My Drive"
    val BACK_IN_TIME = 60000
    var ROOT= sys.env("HOME")+"/drivedog/"
    val FILE_LIST_PAGE_SIZE = 100
    val PAUSE=10000
    val STOPPAUSE=2000
    var pause=false
    var anyConflicts=false

    
   def isConflicted(metaFileName:String) = {
    val meta = new java.io.File(metaFileName)
    val conflict = new java.io.File(metaFileName+Google.CONFLICT)
    
    if ( conflict.exists() ) {
      Google.anyConflicts=true
      true
    }
    else false
  }
    
  def getLocalHostNameForDriveDog = {
    val info = InetAddress.getLocalHost
    ".drivedog_"+info.getCanonicalHostName
  }
  
  implicit class UnixFile(name:String) {
    def unixfile =  new java.io.File(name)  
    
  }
}
