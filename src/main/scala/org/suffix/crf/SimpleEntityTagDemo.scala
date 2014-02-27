package org.suffix.crf

import com.aliasi.corpus.Corpus
import com.aliasi.corpus.ObjectHandler
import com.aliasi.chunk.Chunking
import com.aliasi.chunk.Chunk
import com.aliasi.chunk.ChunkingImpl
import com.aliasi.chunk.ChunkFactory
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.chunk.BioTagChunkCodec
import com.aliasi.stats.RegressionPrior
import com.aliasi.stats.AnnealingSchedule
import com.aliasi.io.Reporters
import com.aliasi.io.LogLevel
import com.aliasi.crf.ChainCrfChunker
import java.io.File
import com.aliasi.util.AbstractExternalizable

object SimpleEntityTagDemo extends App {

  val MinFeatureCount = 1
  val CacheFeatures = true
  val AddIntercept = true
  val PriorVariance = 4.0
  val UninformativeIntercept = true
  val PriorBlockSize = 3
  val InitialLearningRate = 0.05
  val LearningRateDecay = 0.995
  val MinImprovement = 0.00001
  val MinEpochs = 10
  val MaxEpochs = 5000
  
  val ModelFile = new File("models/SimpleEnt.ChainCrf.bin")
  
  Console.println("Training...")
  train()
  Console.println("Testing...")
  test("John Smith lives in New York.")

  def train(): Unit = {
    val corpus = new TinyEntityCorpus()
    val tokFactory = IndoEuropeanTokenizerFactory.INSTANCE
    val tagChunkCodec = new BioTagChunkCodec(tokFactory, true)
    val featureExtractor = new SimpleChainCrfFeatureExtractor()
    val prior = RegressionPrior.gaussian(
      PriorVariance, UninformativeIntercept)
    val annealingSchedule = AnnealingSchedule.exponential(
      InitialLearningRate, LearningRateDecay)
    val reporter = Reporters.stdOut().setLevel(LogLevel.DEBUG)
    Console.println("Estimating")
    val crfChunker = ChainCrfChunker.estimate(
      corpus, tagChunkCodec, tokFactory, featureExtractor, 
      AddIntercept, MinFeatureCount, CacheFeatures, prior, 
      PriorBlockSize, annealingSchedule, MinImprovement, 
      MinEpochs, MaxEpochs, reporter)
    AbstractExternalizable.serializeTo(crfChunker, ModelFile)
  }
  
  def test(sentence: String): Unit = {
    val crfChunker = AbstractExternalizable.readObject(ModelFile).
      asInstanceOf[ChainCrfChunker]
    
    Console.println("First Best")
    val chunking = crfChunker.chunk(sentence)
    Console.println(chunking)
    
    val cs = sentence.toCharArray()
    val MaxNBest = 10
    Console.println(MaxNBest + " Best Conditional")
    Console.println("Rank log p(tags|tokens)  Tagging")
    val nit = crfChunker.nBestConditional(cs, 0, cs.length, MaxNBest)
    var rank = 0
    while (nit.hasNext() && rank < MaxNBest) {
      val scoredChunking = nit.next()
      Console.println(rank + "    " + scoredChunking.score() + 
        " " + scoredChunking.getObject())
      rank += 1
    }
    
    Console.println("Marginal Chunk Probability")
    Console.println("Rank Chunk Phrase")
    val mit = crfChunker.nBestChunks(cs, 0, cs.length, MaxNBest)
    rank = 0
    while (mit.hasNext() && rank < MaxNBest) {
      val chunk = mit.next()
      Console.println(rank + " " + chunk + " " + 
        sentence.substring(chunk.start(), chunk.end()))
      rank += 1
    }
  }
}

class TinyEntityCorpus extends Corpus[ObjectHandler[Chunking]] {

  def chunking(s: String, chunks: Array[Chunk]): Chunking = {
    val chunking = new ChunkingImpl(s)
    chunks.foreach(chunk => chunking.add(chunk))
    chunking
  }
  
  def chunk(start: Int, end: Int, ctype: String): Chunk = {
    ChunkFactory.createChunk(start, end, ctype)
  }
  
  val Chunkings = Array[Chunking](
    chunking("", Array.empty),
    chunking("The", Array.empty),
    chunking("John ran.", Array(chunk(0, 4, "PER"))),
    chunking("Mary ran.", Array(chunk(0, 4, "PER"))),
    chunking("The kid ran.", Array.empty),
    chunking("John likes Mary.", Array(
      chunk(0, 4, "PER"), chunk(11, 15, "PER"))),
    chunking("Tim lives in Washington", Array(
      chunk(0, 3, "PER"), chunk(13, 23, "LOC"))),
    chunking("Mary Smith is in New York City", Array(
      chunk(0, 10, "PER"), chunk(17, 30, "LOC"))),
    chunking("New York City is fun.", Array(
      chunk(0, 13, "LOC"))),
    chunking("Chicago is not like Washington", Array(
      chunk(0, 7, "LOC"), chunk(20, 30, "LOC")))
  )
  
  override def visitTrain(handler: ObjectHandler[Chunking]): Unit = {
    Chunkings.foreach(chunking => handler.handle(chunking))  
  }
  
  override def visitTest(handler: ObjectHandler[Chunking]): Unit = {
    /* NO OP */  
  }
}