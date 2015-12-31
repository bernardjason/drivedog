package gui

import scala.io.Source
import java.io.InputStream
import scala.swing.EditorPane
import scala.swing.MainFrame
import scala.swing.BorderPanel
import scala.swing.SimpleSwingApplication
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit
import scala.swing.ScrollPane
import java.util.StringTokenizer
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import java.awt.Dimension

object HelpPage {

  val helpFileName = "/help.html"
  val dev = "src/main/resources" + helpFileName

  val stream = getClass.getResourceAsStream(helpFileName)
  val help = if ( new java.io.File(dev).exists == false ) scala.io.Source.fromInputStream(stream).mkString
  else Source.fromFile(dev).getLines.mkString

  val editorPane = new EditorPane()
  editorPane.editable = false

  val kit = new HTMLEditorKit();
  editorPane.peer.setEditorKit(kit);
  val doc = kit.createDefaultDocument();

  editorPane.peer.setDocument(doc)

  val styleSheet = kit.getStyleSheet();
  styleSheet.addRule("body {background-color : #e5ecff; font-family:times; margin: 4px; }")
  styleSheet.addRule("h1 {color: blue;}")
  styleSheet.addRule("h2 {color: #4d77ff;}")

  editorPane.text = help

  val scrollPane = new ScrollPane(editorPane);

  val top = new MainFrame {
    import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
    peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)

    override def closeOperation() {
      close
    }

    contents = new BorderPanel {
      import BorderPanel.Position._
      add(scrollPane, Center)
    }
    size = new Dimension(1200, 800)
  }

  def main(args: Array[String]) {

    SwingUtilities.invokeLater(new Runnable {
      def run() {
        top.open()
      }
    })

  }
}