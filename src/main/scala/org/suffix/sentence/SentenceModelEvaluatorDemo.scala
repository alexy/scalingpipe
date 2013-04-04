package org.suffix.sentence

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.sentences.MedlineSentenceModel
import com.aliasi.sentences.SentenceChunker
import com.aliasi.sentences.SentenceEvaluator
import com.aliasi.corpus.XMLParser
import com.aliasi.corpus.ObjectHandler
import com.aliasi.chunk.Chunking
import java.io.File
import com.aliasi.xml.DelegatingHandler
import com.aliasi.chunk.ChunkingImpl
import com.aliasi.chunk.ChunkFactory
import scala.collection.mutable.ArrayBuffer
import com.aliasi.xml.TextAccumulatorHandler
import org.xml.sax.helpers.DefaultHandler
import com.aliasi.sentences.SentenceModel

object SentenceModelEvaluatorDemo extends App {
  val sentenceModel = new MedlineSentenceModel()
  val sentenceModelEvaluator = new SentenceModelEvaluator(sentenceModel)
  sentenceModelEvaluator.evaluate(true)
}

class SentenceModelEvaluator(sentenceModel: SentenceModel) {
  
  def evaluate(showStatsOnly: Boolean): Unit = {
    val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
    val sentenceChunker = new SentenceChunker(
      tokenizerFactory, sentenceModel)
    val sentenceEvaluator = new SentenceEvaluator(sentenceChunker)
    val parser = new GeniaSentenceParser(sentenceEvaluator)
  
    parser.parse(new File("data/GENIAcorpus3.02.xml"))
  
    val sentenceEvaluation = sentenceEvaluator.evaluation()
    val chunkingStats = 
      sentenceEvaluation.chunkingEvaluation.precisionRecallEvaluation()
    Console.println("Chunking Evaluation statistics\n" + 
      chunkingStats.toString)
    
    val endBoundaryStats = sentenceEvaluation.endBoundaryEvaluation()
    Console.println("Sentence Evaluation end boundary statistics\n" +
      endBoundaryStats.toString)
    
    if (! showStatsOnly) {
      Console.println("FALSE NEGATIVES")
      val falseNegatives = sentenceEvaluation.falseNegativeEndBoundaries()
      val fnit = falseNegatives.iterator()
      var i = 0
      while (fnit.hasNext()) {
        val sentence = fnit.next()
            Console.println(i + ". " + sentence.spanEndContext(34))
      }
      Console.println("FALSE POSITIVES")
      val falsePositives = sentenceEvaluation.falsePositiveEndBoundaries()
      val fpit = falsePositives.iterator()
      i = 0
      while (fpit.hasNext()) {
        val sentence = fpit.next()
            Console.println(i + ". " + sentence.spanEndContext(34))
      }
    }
  }
}

class GeniaSentenceParser(handler: ObjectHandler[Chunking]) extends 
    XMLParser[ObjectHandler[Chunking]](handler) {
  
  val GeniaSentenceElt = "sentence"
  val GeniaAbstractElt = "abstract"
  
  def getXMLHandler(): DefaultHandler = {
    new SetHandler(getChunkHandler())
  }
  
  def getChunkHandler(): ObjectHandler[Chunking] = {
    getHandler()
  }

  class SetHandler(val chunkHandler: ObjectHandler[Chunking]) 
      extends DelegatingHandler {
  
    val abstractHandler = new AbstractHandler(this)
    setDelegate(GeniaAbstractElt, abstractHandler)
    
    override def finishDelegate(qName: String, 
        delegate: DefaultHandler): Unit = {
      if (qName.equals(GeniaAbstractElt))
        handleSentenceTexts(abstractHandler.getSentenceTexts())
    }
    
    def handleSentenceTexts(texts: List[String]): Unit = {
      val lengths = texts.map(x => x.length())
      val buf = texts.foldLeft("")(_ + " " + _)
      val chunking = new ChunkingImpl(buf)
      var offset = 0
      for (i <- 0 until texts.size) {
        val chunk = ChunkFactory.createChunk(offset, 
          offset + lengths(i), SentenceChunker.SENTENCE_CHUNK_TYPE)
        Console.println("chunk=" + buf.substring(chunk.start(), chunk.end()))
        chunking.add(chunk)
        offset += lengths(i) + 1
      }
      chunkHandler.handle(chunking)
    }
  }
  
  class AbstractHandler(parent: DelegatingHandler) 
      extends DelegatingHandler {
    
    val sentTexts = ArrayBuffer[String]()
    val sentenceHandler = new TextAccumulatorHandler()
    setDelegate(GeniaSentenceElt, sentenceHandler)
    
    override def startDocument(): Unit = {
      sentTexts.clear
    }
    
    override def finishDelegate(qName: String, 
        delegate: DefaultHandler): Unit = {
      if (qName.equals(GeniaSentenceElt)) {
        val text = sentenceHandler.getText()
        if (text.length() > 0)
          sentTexts += text
      }
    }
    
    def getSentenceTexts(): List[String] = {
      return sentTexts.toList
    }
  }
}

