package drivedog

import scala.collection.JavaConversions._
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.File
import com.google.api.client.http.GenericUrl
import java.nio.file.Paths
import java.nio.file.Files
import com.typesafe.scalalogging.StrictLogging
import java.text.SimpleDateFormat
import java.util.Calendar
import java.nio.charset.StandardCharsets
import com.google.api.services.drive.model.ParentReference
import com.google.api.services.drive.model.File.Labels
import com.google.api.client.util.DateTime

object GoogleDriveList extends StrictLogging {

  val directories:scala.collection.mutable.Map[String,File] = scala.collection.mutable.Map()
  
  
  def main(args: Array[String]): Unit = {
    FinalRunReport.start
    refreshToLocal()//"modifiedDate >=  '2015-12-05T00:00:00.000'")
    
    println("\nfinal report")
    for(l <- FinalRunReport.produce ) {
      println(l)
    }
  }
  def refreshToLocal(parentId:String=null,parentFilter:String = null) {

    val (nextTimeFileToUsed, useThisFilter) = getLastTime(parentId,parentFilter)//, modFilter)    
     
    logger.info(s"search criteria on google is $useThisFilter")
    
    var result:FileList=null;
    var nextPageToken:String = null

    directories.clear()
    do {
      logger.info("get google listing")
      result = Google.service.files().list().setQ(useThisFilter)
        .setPageToken(nextPageToken)
        .setMaxResults(Google.FILE_LIST_PAGE_SIZE)
        .execute();

      nextPageToken=result.getNextPageToken
      val files = result.getItems();
      
      if (files == null || files.size() == 0) {
        logger.info("No files found.");
      } else {
        logger.info(s"Found files ${files.size}")
        for (file <- files) {
          logger.info(s"Process file ${file.getTitle}")
          logger.debug(s"Process file ${file}")
          if ( file.getLabels().getTrashed() ==false && file.getShared == false && file.getTitle.startsWith(".drivedog") == false ) {
            val parent = file.getParents();
            
            val path =  new StringBuilder(Google.ROOT )
            
            if (parent.size() > 0) {
              val pid = parent.get(0).getId();
              val dirs = directoryTree(pid)
              val toPath = for( d<- dirs) yield (d.getTitle)
              toPath.addString(path, "/")
            }
            GoogleFile.getFile(file, path)
          }
        }
      }
    } while (nextPageToken != null && nextPageToken.length() > 0);
    
    val myHostName = Google.getLocalHostNameForDriveDog

    if ( Google.anyConflicts == false ) {
      val filterForMarkerFile = if ( parentId != null ) {
        s"'${parentId}' in parents and title = '${myHostName}' and trashed != true" 
      } else {
        s"title = '${myHostName}' and trashed != true" 
      }
      for( d <- Google.service.files().list().setQ(filterForMarkerFile).execute().getItems) {
        Google.service.files().delete(d  .getId).execute()
      }
      nextTimeFileToUsed.setTitle(myHostName)
      Google.service.files.patch(nextTimeFileToUsed.getId, nextTimeFileToUsed).execute()    
    }
  }

  def getLastTime(parentId:String,parentFilter: String) = {
    val modFilter = new StringBuilder
    
    val myHostName = Google.getLocalHostNameForDriveDog
    
    for( d <- Google.service.files().list().setQ("title = '"+myHostName+".next' and trashed != true").execute().getItems) {
      Google.service.files().delete(d.getId).execute()
    }
    
    val body = new File();
    body.setLabels( (new Labels).setRestricted(true))
    body.setTitle(myHostName+".next");
    body.setDescription("drivedog work file");
    body.setMimeType(Google.FILE);
    if ( parentId != null ) {
      val parentRef = new ParentReference()
      parentRef.setId(parentId)
      body.setParents(List( parentRef).toList)
    }
    val nextTime = Google.service.files().insert(body).execute
    
    val hostFile = if ( parentFilter != null ) 
     Google.service.files().list().setQ(parentFilter + " and title = '"+myHostName+"' and trashed != true").execute();
    else
     Google.service.files().list().setQ("title = '"+myHostName+"' and trashed != true").execute(); 
    
    val hostFileList = hostFile.getItems

    val lastTimeWeQueriedDrive = if (hostFileList.size == 1 ) {
        val lastTime = hostFileList.get(0)
        val md = new DateTime(lastTime.getModifiedDate.getValue - Google.BACK_IN_TIME)   
        md.toString()        
    } else "" 
    
    if ( parentFilter != null ) 
      modFilter.append(parentFilter)
      
    if ( lastTimeWeQueriedDrive.length() > 0 ) {
      if ( parentFilter != null  )  
        modFilter.append(" and modifiedDate >= '").append(lastTimeWeQueriedDrive).append("'")
      else  
        modFilter.append("modifiedDate >= '").append(lastTimeWeQueriedDrive).append("'")
    }
    
    (nextTime,modFilter.toString())
  }
  

  def makeSureNextRunIsAFullOne {
    val myHostName = Google.getLocalHostNameForDriveDog

    for( d <- Google.service.files().list().setQ(s"(title = '${myHostName}.next' or  title = '${myHostName}') and trashed != true").execute().getItems) {
      logger.info(s"for full run, trashing file ${d}")
      Google.service.files().trash(d.getId).execute()
    }
    logger.info("****************** NEXT RUN A FULL RUN *****************")
  }
  
  def directoryTree(pid:String):List[File] = {
   if ( ! directories.contains(pid) ) {
       directories += (pid -> Google.service.files().get(pid).execute())
    }
    val currentDirectory =directories(pid)
    
    if ( currentDirectory.getParents.size() > 0 ) {
      directoryTree(currentDirectory.getParents.get(0).getId) ++ List(currentDirectory)
    } else {
      List(currentDirectory)
    }
  }
}