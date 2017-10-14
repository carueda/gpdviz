package gpdviz.data

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import gpdviz.model._
import gpdviz.server.{GnError, JsonImplicits}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class FileDb(dataDir: String) extends JsonImplicits with DbInterface {

  val details: String = s"File-based database (dir: $dataDir)"

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = Future {
    val files = dataPath.toFile.listFiles.filter(_.getName.endsWith(".ss.json"))
    val systems: Seq[Option[SensorSystem]] = files.toSeq.map { f ⇒
      getSensorSystemByFilename(f.getName)
    }
    systems.flatten.map { sys ⇒
      SensorSystemSummary(sys.sysid, sys.name, sys.description, sys.streams.keySet)
    }
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = Future {
    getSensorSystemByFilename(sysid + ".ss.json")
  }

  override def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = {
    saveSensorSystem(ss)
  }

  override def registerDataStream(sysid: String)(ds: DataStream): Future[Either[GnError, String]] = Future {
    Right(ds.strid)
  }

  override def registerVariableDef(sysid: String, strid: String)(vd: VariableDef): Future[Either[GnError, String]] = Future {
    Right(vd.name)
  }

  override def registerObservation(sysid: String, strid: String, time: String)(obsData: ObsData): Future[Either[GnError, String]] = Future {
    Right(time)
  }

  override def saveSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = Future {
    doSave(ss)
  }

  override def deleteSensorSystem(sysid: String): Future[Either[GnError, String]] = {
    getSensorSystem(sysid) map {
      case Some(ss) ⇒
        val filename = sysid + ".ss.json"
        val ssPath = Paths.get(dataDir, filename)
        try {
          Files.delete(ssPath)
          Right(ss.sysid)
        }
        catch {
          case NonFatal(e) ⇒ Left(GnError(500, s"error deleting sensor system: ${e.getMessage}"))
        }

      case None ⇒ Left(GnError(404, "Not registered", sysid = Some(sysid)))
    }
  }

  private def getSensorSystemByFilename(filename: String): Option[SensorSystem] = {
    val ssPath = Paths.get(dataDir, filename)
    val ssFile = ssPath.toFile
    if (ssFile.exists()) try {
      val contents = scala.io.Source.fromFile(ssFile).mkString
      Option(contents.parseJson.convertTo[SensorSystem])
    }
    catch {
      case NonFatal(e) ⇒
        println(s"WARN: error trying to load $ssFile: $e")
        None
    }
    else None
  }

  private def doSave(ss: SensorSystem): Either[GnError, String] = {
    val filename = ss.sysid + ".ss.json"
    //println(s"SAVE $filename")
    val ssPath = Paths.get(dataDir, filename)
    try {
      Files.write(ssPath, ss.toJson.prettyPrint.getBytes(StandardCharsets.UTF_8))
      Right(ss.sysid)
    }
    catch {
      case NonFatal(e) ⇒ e.printStackTrace()
        Left(GnError(500, s"error saving sensor system: ${e.getMessage}"))
    }
  }

  private val dataPath = Paths.get(dataDir)
  dataPath.toFile.mkdir()
}
