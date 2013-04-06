package org.suffix.ner

import java.io.File

import com.aliasi.chunk.{AbstractCharLmRescoringChunker, BioTagChunkCodec, CharLmRescoringChunker, ChunkerEvaluator, Chunking, TagChunkCodecAdapters}
import com.aliasi.corpus.{ObjectHandler, StringParser}
import com.aliasi.tag.LineTaggingParser
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.util.AbstractExternalizable

object CoNLLSpanishNERDemo extends App {

  val NumChunkingsRescored = 64
  val MaxNGram = 12
  val NumChars = 256
  val LmInterpolation = MaxNGram
  val SmoothTags = true
  
  ///////////////// Train /////////////////
//  val trainFile = new File("data/conll2002/ner/data/esp.train")
//  val devFile = new File("data/conll2002/ner/data/esp.testa")
//  val modelFile = new File("models/ne-es-news-conll2002.AbstractCharLmRescoringChunker")
//  train(trainFile, devFile, modelFile)

  ///////////////// Test  /////////////////
  val modelFile = new File("models/ne-es-news-conll2002.AbstractCharLmRescoringChunker")
  val testFile = new File("data/conll2002/ner/data/esp.testb")
  evaluate(modelFile, testFile)

  def train(trainFile: File, devFile: File, modelFile: File): Unit = {
    val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
    val chunkerEstimator = new CharLmRescoringChunker(tokenizerFactory,
      NumChunkingsRescored, MaxNGram, NumChars, LmInterpolation,
      SmoothTags)
    val parser = new Conll2002ChunkTagParser()
    parser.setHandler(chunkerEstimator)
    Console.println("Training with data from File: " + trainFile.getName())
    parser.parse(trainFile)
    Console.println("Training with data from File: " + devFile.getName())
    parser.parse(devFile)
    Console.println("Compiling and Writing model: " + modelFile.getName())
    AbstractExternalizable.compileTo(chunkerEstimator, modelFile)
  }
  
  def evaluate(modelFile: File, testFile: File): Unit = {
    Console.println("Reading compiled model from file: " + modelFile.getName())
    val chunker = AbstractExternalizable.readObject(modelFile).
      asInstanceOf[AbstractCharLmRescoringChunker[_,_,_]]
    chunker.setNumChunkingsRescored(NumChars * 2)
    Console.println("Setting up Evaluator")
    val evaluator = new ChunkerEvaluator(chunker)
    evaluator.setVerbose(true)
    evaluator.setMaxNBest(128)
    evaluator.setMaxNBestReport(8)
    evaluator.setMaxConfidenceChunks(8)
    Console.println("Setting up Data Parser")
    val parser = new Conll2002ChunkTagParser()
    parser.setHandler(evaluator)
    Console.println("Running tests on file: " + testFile.getName())
    parser.parse(testFile)
    Console.println("Results:\n" + evaluator.toString())
  }
  
  class Conll2002ChunkTagParser extends StringParser[ObjectHandler[Chunking]] {
    
    val TokenTagLineRegex = "(\\S+)\\s(\\S+\\s)?(O|[B|I]-\\S+)" // token ?pos entity
    val TokenGroup = 1
    val TagGroup = 3
    val IgnoreLineRegex = "-DOCSTART(.*)"
    val EosRegex = "\\A\\Z"
    val BeginTagPrefix = "B-"
    val InTagPrefix = "I-"
    val OutTag = "O"
    
    val parser = new LineTaggingParser(TokenTagLineRegex, TokenGroup,
      TagGroup, IgnoreLineRegex, EosRegex)
    val codec = new BioTagChunkCodec(null, false, 
      BeginTagPrefix, InTagPrefix, OutTag)
    
    override def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
      parser.parseString(cs, start, end)  
    }
    
    override def setHandler(handler: ObjectHandler[Chunking]) {
      val taggingHandler = TagChunkCodecAdapters.chunkingToTagging(
        codec, handler)
      parser.setHandler(taggingHandler)
    }
  }
}