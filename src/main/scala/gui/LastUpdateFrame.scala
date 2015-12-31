package gui

import scala.swing.MainFrame
import scala.swing.event.ButtonClicked
import scala.swing.BorderPanel
import java.awt.Color
import scala.swing.Button
import scala.swing.Table
import scala.swing.ScrollPane
import javax.swing.UIManager
import java.awt.Dimension
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import scala.swing.event.TableRowsSelected
import java.awt.Desktop
import javax.swing.JOptionPane
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import scala.swing.event.MouseClicked
import scala.swing.event.MousePressed
import scala.swing.Label
import javax.swing.JPopupMenu
import scala.swing.MenuItem
import scala.swing.Component
import scala.swing.PopupMenu
import scala.swing.Action


case class LastUpdateFrame() {

  val tableModel = new MyTableModel(scala.collection.mutable.ArrayBuffer[scala.collection.mutable.ArrayBuffer[Any]](),
    List("When", "Operation", "File", "Comment"))
  val table = new Table(1, 4) { model = tableModel }

  table.peer.getColumnModel().getColumn(0).setPreferredWidth(70);
  table.peer.getColumnModel().getColumn(0).setMaxWidth(130);

  table.peer.getColumnModel().getColumn(1).setPreferredWidth(70);
  table.peer.getColumnModel().getColumn(1).setMaxWidth(130);

  table.peer.getColumnModel().getColumn(2).setPreferredWidth(150);
  table.peer.getColumnModel().getColumn(3).setPreferredWidth(150);

  table.peer.getTableHeader.setReorderingAllowed(false)
  table.background = new Color(179, 198, 255)
  table.gridColor = new Color(102, 140, 255)

  table.peer.setFillsViewportHeight(true)
  table.listenTo(table.mouse.clicks)
  table.reactions += {

    case e: MouseClicked =>
      val point = e.point
      val row = table.peer.rowAtPoint(point)
      if (e.clicks == 2 && row >= 0 ) {
        copyToClipBoard(row)
      }

    case e:MousePressed => if(e.triggersPopup) {
      val popup = new PopupMenu
      val row = table.peer.rowAtPoint(e.point)

      if ( row >= 0 ) {
      popup.contents += 
        new MenuItem(Action("copy url") {
          copyToClipBoard(row,false)
        })
      }
      popup.contents +=   
        new MenuItem(Action("clear") {
           tableModel.rowData.clear()
           tableModel.fireTableDataChanged
        })    
        popup.show(table, e.point.x  , e.point.y)
      }
    case _ =>
  }
  val frame = new MainFrame {
    import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
    peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)

    override def closeOperation() {
      close
    }

    val button = new Button {
      text = "close"
      foreground = Color.WHITE
      background = new Color(77, 119, 255)
      borderPainted = true
      enabled = true
      tooltip = "close"
    }
    val scrollPane = new ScrollPane(table)
    contents = new BorderPanel {
      import BorderPanel.Position._
      add(scrollPane, Center)
      add(button, BorderPanel.Position.South)
    }
    listenTo(button)

    reactions += {
      case ButtonClicked(component) => close

    }

  }
  def copyToClipBoard(row:Int,ok:Boolean=true) = {
          val comment = tableModel.getValueAt(row, 3).asInstanceOf[String]
          if (comment.contains("see https")) {
            val split = comment.split("https")
            if (Desktop.isDesktopSupported()) {
              Desktop.getDesktop().browse(new java.net.URI("https" + split(1)))
            } else {
              if ( ok == true ) {
                val frame = new Frame("");
                frame.setVisible(true)
                frame.setResizable(false)
                val r = JOptionPane.showMessageDialog(frame,
                  s"clipboard set to url https${split(1)}",
                  "drivedog",
                  JOptionPane.PLAIN_MESSAGE);
                frame.setVisible(false)
                frame.dispose()
              }
              val clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
              val stringSelection = new StringSelection("https" + split(1));
              clipBoard.setContents(stringSelection, null)

            }
          }
        }
  
}

class MyTableModel(var rowData: scala.collection.mutable.ArrayBuffer[scala.collection.mutable.ArrayBuffer[Any]],
                   val columnNames: Seq[String]) extends AbstractTableModel {
  override def getColumnName(column: Int) = columnNames(column).toString
  def getRowCount() = rowData.length
  def getColumnCount() = columnNames.length
  def getValueAt(row: Int, col: Int): AnyRef = {
    rowData(rowData.length - row - 1)(col).asInstanceOf[AnyRef]
  }
  override def isCellEditable(row: Int, column: Int) = false
  override def setValueAt(value: Any, row: Int, col: Int) {
    rowData(row)(col) = value
  }

  val MAX = 100
  def addRow(data: scala.collection.mutable.ArrayBuffer[AnyRef]) {
    rowData ++= scala.collection.mutable.ArrayBuffer(data.asInstanceOf[scala.collection.mutable.ArrayBuffer[Any]])

    if (rowData.size > MAX) {
      rowData.remove(0, rowData.size - MAX)
    }
  }
}