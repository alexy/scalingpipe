package org.suffix.dbmining

import java.io.File
import java.sql.{Connection, DriverManager}

import scala.collection.JavaConversions.asScalaSet
import scala.collection.mutable.ArrayBuffer

import com.aliasi.chunk.Chunker
import com.aliasi.sentences.{IndoEuropeanSentenceModel, SentenceChunker}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.util.AbstractExternalizable
import com.mysql.jdbc.Driver

object AnnotateMedlineDemo extends App {

  val amd = new AnnotatedMedlineDb()
  amd.getCitationIds().foreach(citationId => {
    amd.annotateCitation(citationId)
  })
  amd.close()
}

class AnnotatedMedlineDb {

  val GetCitationIDsSql = """
    select citation_id from citation
    """
  val GetCitationTextSql = """
    select title, abstract from citation where citation_id = ?
    """
  val InsertSentenceSql = """
    insert into sentence 
    (sentence_id, citation_id, offset, length, type)
    values (NULL, ?, ?, ?, ?)
    """
  val InsertMentionSql = """
    insert into mention
    (mention_id, sentence_id, offset, length, type, text)
    values (NULL, ?, ?, ?, ?, ?)
    """

  val conn = getConnection()
  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val sentenceModel = new IndoEuropeanSentenceModel()
  val sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel)
  val genomicsModelFile = new File("models/ne-en-bio-genia.TokenShapeChunker")
  val neChunker = AbstractExternalizable.readObject(
    genomicsModelFile).asInstanceOf[Chunker]
  
  
  def getConnection(): Connection = {
    val connstr = "jdbc:mysql://localhost:3306/medline"
    classOf[com.mysql.jdbc.Driver].newInstance()
    DriverManager.getConnection(connstr, "root", "secret")
  }
  
  def close(): Unit = {
    conn.close()
  }
  
  def getCitationIds(): Array[Int] = {
    val ids = ArrayBuffer[Int]()
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(GetCitationIDsSql)
    while (rs.next()) {
      ids += rs.getInt("citation_id")
    }
    ids.toArray
  }
  
  def annotateCitation(citationId: Int): Unit = {
    val ps = conn.prepareStatement(GetCitationTextSql)
    ps.setInt(1, citationId)
    val rs = ps.executeQuery()
    val (title, abstrct) = if (rs.next()) {
      (rs.getString("title"), rs.getString("abstract"))
    } else {
      (null, null)
    }
    rs.close()
    ps.close()
    annotateSentences(citationId, "Title", title)
    annotateSentences(citationId, "Abstract", abstrct)
  }
  
  def annotateSentences(citationId: Int, name: String, 
      text: String): Unit = {
    val chunking = sentenceChunker.chunk(text.toCharArray(), 0, text.length())
    chunking.chunkSet().foreach(sentence => {
      val start = sentence.start()
      val end = sentence.end()
      val sentenceId = storeSentence(citationId, start, end - start, name)
      annotateMentions(sentenceId, text.substring(start, end))
    })
  }
  
  def storeSentence(citationId: Int, start: Int, 
      length: Int, name: String): Int = {
    val ps = conn.prepareStatement(InsertSentenceSql)
    ps.setLong(1, citationId)
    ps.setInt(2, start)
    ps.setInt(3, length)
    ps.setString(4, name)
    ps.executeUpdate()
    val rs = ps.getGeneratedKeys()
    val sentenceId = if (rs.first()) rs.getInt(1) else -1 
    rs.close()
    ps.close()
    sentenceId
  }
  
  def annotateMentions(sentenceId: Int, text: String): Unit = {
    val chunking = neChunker.chunk(text.toCharArray(), 0, text.length())
    chunking.chunkSet().foreach(mention => {
      val start = mention.start()
      val end = mention.end()
      storeMention(sentenceId, start, 
        mention.`type`, text.substring(start, end))
    })
  }
  
  def storeMention(sentenceId: Int, offset: Int, 
      name: String, text: String): Int = {
    val ps = conn.prepareStatement(InsertMentionSql)
    ps.setLong(1, sentenceId)
    ps.setInt(2, offset)
    ps.setInt(3, text.length())
    ps.setString(4, name)
    ps.setString(5, text)
    ps.executeUpdate()
    val rs = ps.getGeneratedKeys()
    val mentionId = if (rs.first()) rs.getInt(1) else -1
    rs.close()
    ps.close()
    mentionId
  }
}