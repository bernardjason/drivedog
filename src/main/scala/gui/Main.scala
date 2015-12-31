package gui

import java.awt.CheckboxMenuItem
import java.awt.Dimension
import java.awt.Frame
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import drivedog.Daemon
import drivedog.FinalRunReport
import drivedog.GoogleDriveList
import javax.swing.ImageIcon
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import java.awt.event.ItemEvent
import drivedog.Google._
import drivedog.FullRun
import java.io.FileInputStream
import drivedog.Google._
import java.io.RandomAccessFile
object Main {
  val minuteFormat = new SimpleDateFormat("HH:mm:ss")

  private var guiRunning=false;
  def main(args: Array[String]) {

    val  lockFile = sys.env("HOME")+"/.drivedog.daemon.lock"
    var locked=false
    try {
     
      val lock = new RandomAccessFile(lockFile,"rw")
      if ( lock.getChannel.tryLock() != null ) {
        locked=true
      }
    } catch {
      case e: Exception => println(s"failed to get lock $lockFile",e)
      System.exit(-1)
    }
    if ( locked ) {
      Daemon.start()
  
      SwingUtilities.invokeLater(new Runnable {
        def run() {
          createAndShowGUI
        }
      })
  
      
      Daemon ! drivedog.Run
    } else {
      println(s"Daemon is already running, lock file $lockFile is locked");
      System.exit(-1)
    }
  }
  // http://www.clipartpanda.com/categories/mean-dog-face-clipart
  val trayIcon =
      new TrayIcon(createImage("/dog.png", "tray icon"));
 
  def showWarning(s:String) {
    if (SystemTray.isSupported() && guiRunning == true ) {
        trayIcon.displayMessage("drivedog", s, TrayIcon.MessageType.WARNING);
    }
  }
  def showInfo(s:String) {
    if (SystemTray.isSupported() && guiRunning == true ) {
        trayIcon.displayMessage("drivedog", s, TrayIcon.MessageType.INFO);
    }
    
  }
  def showError(s:String) {
    if (SystemTray.isSupported() && guiRunning == true ) {
      val frame = new Frame("");
      frame.setVisible(true)
      frame.setResizable(false)
      val r=  JOptionPane.showMessageDialog(frame,
            s,
            "drivedog error",
            JOptionPane.ERROR_MESSAGE);    
      frame.setVisible(false)
      frame.dispose()
    }
      
  }
  def addRunData {
    if (SystemTray.isSupported() && guiRunning == true ) {
    val currentMinuteAsString = minuteFormat.format(  Calendar.getInstance().getTime() )

        for(l <- FinalRunReport.entries ) {
          
           lastUpdateFrame.tableModel.addRow(
              scala.collection.mutable.ArrayBuffer[AnyRef](currentMinuteAsString,l.what,l.file,l.text)) 
        }
        lastUpdateFrame.tableModel.fireTableDataChanged()
    }
  }
  

  def createAndShowGUI() {
    if (!SystemTray.isSupported()) {
      System.out.println("SystemTray is not supported");
      return ;
    }
    guiRunning=true
    trayIcon.setImageAutoSize(true);

    val tray = SystemTray.getSystemTray();
   
    val fullRun = new MenuItem("Full run");
    val lastUpdate = new MenuItem("Recent updates");
    val pause = new CheckboxMenuItem("Pause when safe");
    val helpItem = new MenuItem("Help");
    val exitItem = new MenuItem("Exit");

 
    val popup = new PopupMenu()
    popup.add(fullRun)
    popup.add(lastUpdate)
    popup.add(pause)
    popup.addSeparator
    popup.add(helpItem)
    popup.addSeparator
    popup.add(exitItem)
    
    trayIcon.setPopupMenu(popup)

    tray.add(trayIcon)
            
    trayIcon.addMouseListener(new MouseAdapter() {
      override def mouseClicked(e:MouseEvent) {
         trayIcon.displayMessage("drivedog", "right click for options", TrayIcon.MessageType.INFO);
      }
    })
   
    helpItem.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        HelpPage.top.open()
      }
    });

    fullRun.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        //GoogleDriveList.makeSureNextRunIsAFullOne
        Daemon ! FullRun
      }
    });
    
    pause.addItemListener( new ItemListener() {
      def itemStateChanged(e:ItemEvent) {
        val currentMinuteAsString = minuteFormat.format(  Calendar.getInstance().getTime() )
        
        if (  e.getStateChange ==  ItemEvent.SELECTED) {
          drivedog.Google.pause=true
          Daemon.stopNow
          lastUpdateFrame.tableModel.addRow(scala.collection.mutable.ArrayBuffer[AnyRef](currentMinuteAsString,"PAUSED","",""))
          lastUpdateFrame.tableModel.fireTableDataChanged()
        } else {
          drivedog.Google.pause=false
          Daemon.goNow
          lastUpdateFrame.tableModel.addRow(scala.collection.mutable.ArrayBuffer[AnyRef](currentMinuteAsString,"RESUMED","",""))
          lastUpdateFrame.tableModel.fireTableDataChanged()
        }
      }
    })
              
    lastUpdate.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        
        lastUpdateFrame.frame.centerOnScreen
        lastUpdateFrame.frame.open()
        
        var height  = (lastUpdateFrame.tableModel.getRowCount()*10).asInstanceOf[Int] + 100
        if ( height > 500 ) height = 500
         lastUpdateFrame.frame.size = new 
        Dimension(lastUpdateFrame.table.size.getWidth.asInstanceOf[Int],height)
        lastUpdateFrame.frame.peer.toFront()
        lastUpdateFrame.frame.peer.setAlwaysOnTop(true);

      }
    });

   
 

    exitItem.addActionListener(new ActionListener() {
      
       def actionPerformed(e: ActionEvent) {
        val reply = JOptionPane.showConfirmDialog(null,
          "really","drivedog",JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
          tray.remove(trayIcon);
          System.exit(0);
        }
      }

    });
    

  }

  //Obtain the image URL
  def createImage(path: String, description: String) = {
    val dev = s"/src/google/drivedog/src/main/resources${path}"
    if ( dev.unixfile.exists() ) {
      new ImageIcon(dev, description).getImage();
    } else {
      val url = getClass().getResource(path);
      new ImageIcon(path, description).getImage();
    }
  }
  
  
  val lastUpdateFrame = LastUpdateFrame()
 
}


