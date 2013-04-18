package org.suffix.dbmining

import java.io.File
import java.sql.{DriverManager, Types}

import org.xml.sax.InputSource

import com.aliasi.lingmed.medline.parser.{MedlineCitation, MedlineHandler, MedlineParser}
import com.mysql.jdbc.Driver

object LoadMedlineDemo extends App {

  val inputFiles = Array[File](
      new File("data/medline/medsamp2010.xml"))
  val parser = new MedlineParser(false)
  val loader = new MedlineDbLoader()
  inputFiles.foreach(inputFile => {
    val url = inputFile.toURI().toURL().toString()
    Console.println("Processing URL: " + url)
    val inputSource = new InputSource(url)
    parser.setHandler(loader)
    parser.parse(inputSource)
  })
  loader.close()
}

class MedlineDbLoader() extends MedlineHandler {
  
  val InsertCitationSql = """
    insert into citation (pubmed_id, title, abstract) 
    values (?, ?, ?)
  """
    
  val connstr = "jdbc:mysql://localhost:3306/medline"
  classOf[com.mysql.jdbc.Driver].newInstance()
  val conn = DriverManager.getConnection(connstr, "root", "secret")
  
  override def delete(s: String): Unit = { /* Do Nothing */ }
  
  override def handle(citation: MedlineCitation): Unit = {
    val ps = conn.prepareStatement(InsertCitationSql)
    try {
      ps.setString(1, citation.pmid())
      ps.setString(2, citation.article().articleTitleText())
      val abs = citation.article().abstrct()
      if (abs == null) {
        ps.setNull(3, Types.VARCHAR)
      } else {
        ps.setString(3, abs.text())
      }
      ps.executeUpdate()
    } finally {
      ps.close()
    }
  }
  
  def close(): Unit = {
    conn.close()
  }
}
