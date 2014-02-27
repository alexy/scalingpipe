package org.suffix.ner

import java.io.File
import com.aliasi.corpus.Parser
import com.aliasi.corpus.ObjectHandler
import com.aliasi.chunk.Chunking
import com.aliasi.chunk.ChunkingEvaluation
import scala.collection.mutable.ArrayBuffer
import com.aliasi.corpus.XMLParser
import org.xml.sax.helpers.DefaultHandler
import com.aliasi.xml.DelegatingHandler
import com.aliasi.chunk.Chunk
import org.xml.sax.Attributes
import com.aliasi.chunk.ChunkFactory
import com.aliasi.chunk.ChunkingImpl
import collection.JavaConversions._

object Muc6FileScoringDemo extends App {

  val referenceFile = new File("data/neFileScore/reference.muc6")
  val responseFile = new File("data/neFileScore/response.muc6")
  
  val parser = new Muc6ChunkParser()
  val scorer = new FileScorer(parser)
  scorer.score(referenceFile, responseFile)
  Console.println(scorer.evaluation.toString())
}

class FileScorer(parser: Parser[ObjectHandler[Chunking]]) {
  
  val evaluation = new ChunkingEvaluation()
  
  def score(referenceFile: File, responseFile: File): Unit = {
    val refCollector = new ChunkingCollector()
    parser.setHandler(refCollector)
    parser.parse(referenceFile)
    val refChunkings = refCollector.chunkingList
    
    val resCollector = new ChunkingCollector()
    parser.setHandler(resCollector)
    parser.parse(responseFile)
    val resChunkings = resCollector.chunkingList
    
    if (refChunkings.length != resChunkings.length) {
      throw new IllegalArgumentException("Chunkings not same size")
    }
    for (i <- 0 until refChunkings.length) {
      evaluation.addCase(refChunkings(i), resChunkings(i))
    }
  }
}

class ChunkingCollector extends ObjectHandler[Chunking] {
  
  val chunkingList = ArrayBuffer[Chunking]()
  
  override def handle(chunking: Chunking): Unit = {
    chunkingList += chunking
  }
}

class Muc6ChunkParser(handler: ObjectHandler[Chunking]) 
    extends XMLParser[ObjectHandler[Chunking]](handler) {
  
  val sentenceTag = "s"

  def this() { this(null) }
  
  override def getXMLHandler(): DefaultHandler = {
    return new MucHandler(getHandler())
  }
}

class MucHandler(chunkHandler: ObjectHandler[Chunking]) 
    extends DelegatingHandler {
  
  val sentenceHandler = new SentenceHandler()
  setDelegate("s", sentenceHandler)
  
  override def finishDelegate(qName: String, handler: DefaultHandler): Unit = {
    val chunking = sentenceHandler.getChunking()
    chunkHandler.handle(chunking)
  }
}

class SentenceHandler extends DefaultHandler {
  
  var buf: StringBuilder = null
  var chunkType: String = null
  var start: Int = 0
  var end: Int = 0
  var chunkList: ArrayBuffer[Chunk] = null
  
  override def startDocument(): Unit = {
    buf = new StringBuilder()
    chunkList = ArrayBuffer[Chunk]()
  }
  
  override def startElement(uri: String, localName: String, 
      qName: String, attributes: Attributes): Unit = {
    if ("ENAMEX".equals(qName)) {
      chunkType = attributes.getValue("TYPE")
      start = buf.length()
    }
  }
  
  override def endElement(uri: String, localName: String, 
      qName: String): Unit = {
    if ("ENAMEX".equals(qName)) {
      end = buf.length()
      val chunk = ChunkFactory.createChunk(start, end, chunkType, 0)
      chunkList += chunk
    }
  }
  
  override def characters(cs: Array[Char], start: Int, length: Int): 
      Unit = {
    buf.append(cs, start, length)
  }
  
  def getChunking(): Chunking = {
    val chunking = new ChunkingImpl(buf)
    for (chunk <- chunkList) {
      chunking.add(chunk)
    }
    chunking
  }
}