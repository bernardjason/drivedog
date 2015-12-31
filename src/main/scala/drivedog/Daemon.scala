package drivedog

import com.typesafe.scalalogging.StrictLogging
import scala.actors.Actor
import gui.Main
import scala.actors.PoisonPill
import Google._

case object Run
case object Exit
case object Pause
case object Go
case object Stop 
case object FullRun

object Daemon extends Actor with StrictLogging {

  def stopNow = {
    Google.pause=true
  }
  def  goNow = {
    Google.pause=false
    Daemon .restart()
  }
  
   def act() {
     while (!Google.pause) {
       receive {
         case Run => 
             try {
             
               run
               if ( FinalRunReport.entries.size > 0 ) {
                 Main.showInfo(s"changes ${FinalRunReport.entries.size}")
               }
              
             } catch {
               case ex:Exception => logger.error(s"Run failed ${ex} ")
               Main.showError(s"Run failed ${ex}")
             }
             FinalRunReport.complete
             logger.info("final report")
             for(l <- FinalRunReport.produce ) {
              logger.info(s"final report $l")
             }
             Main.addRunData
             if ( Google.anyConflicts ) {
                  Main.showError("there are conflicts to resolve")
             }
             Daemon ! Pause
         case Exit => System.exit(0)
         case Pause => Thread.sleep(Google.PAUSE)
           Daemon ! Run
         case FullRun => 
           GoogleDriveList.makeSureNextRunIsAFullOne
           Daemon ! Run
       }
     }
   }
   

  def run = {
    logger.info("starting run now")
    
    if ( ! Google.ROOT.unixfile.exists ) {
      logger.info(s"Making ${Google.ROOT}")
      Google.ROOT.unixfile.mkdirs
    }
    
    if ( ! Google.ROOT.unixfile.exists ) {
      throw new RuntimeException(s"unable to create ${Google.ROOT}")
    }
    
    FinalRunReport.start
    Google.anyConflicts=false
    if ( !Google.pause) GoogleDriveList.refreshToLocal()
    if ( !Google.pause) LocalDriveList.startPushProcess
    if ( !Google.pause) LocalDeleteList.delete()
    logger.info("run complete now")
   
  }
}
