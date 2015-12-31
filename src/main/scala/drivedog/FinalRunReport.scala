package drivedog

import scala.collection.mutable.ListBuffer

object FinalRunReport {
  trait What { val compare:Int }
  case object PULL extends What { val compare = 1 }
  case object PUSH extends What { val compare = 2 }
  case object CONFLICT extends What { val compare = 3 }
  case object DELETE extends What { val compare = 4 }

  case class Entry(what:What,text:String,file:String)
  val entries = ListBuffer[Entry]()
  var done=false
  def start = {
    entries.clear()
    done=false
  }
  def complete = {
    done=true
  }
  def add(what:What,text:String,file:String) {
    entries += Entry(what,text,file)
  }
  def produce = {
    val format=ListBuffer[String]()
    for(l <- entries) {
      l.what match {
        case PULL => format += s"Pulled ${l.text} ${l.file}"
        case PUSH => format += s"Pushed ${l.text} ${l.file}"
        case CONFLICT => format += s"Conflict${l.text} ${l.file}"
        case DELETE => format += s"Deleted ${l.text} ${l.file}"

      }
    }
    format.toList
  }
}