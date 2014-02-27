package org.suffix.cluster

import java.io.File
import com.aliasi.symbol.MapSymbolTable
import com.aliasi.cluster.LatentDirichletAllocation
import com.aliasi.corpus.ObjectHandler
import java.util.Random
import com.aliasi.util.ScoredObject
import com.aliasi.stats.Statistics
import scala.util.Sorting
import java.util.Collections

object LdaTopicSimilarityDemo extends App {

  val MinTokenCount = 5
  
  val inputFile = new File("data/2007-12-01-wormbase-literature.endnote")
  val texts = LdaWormBaseDemo.readCorpus(inputFile)
  val symbolTable = new MapSymbolTable()
  val tokenizerFactory = LdaWormBaseDemo.wormbaseTokenizerFactory()
  val docTokens = LatentDirichletAllocation.tokenizeDocuments(
    texts, tokenizerFactory, symbolTable, MinTokenCount)

  val runnable1 = new LdaRunnable(docTokens, 
    new LdaReportingHandler(symbolTable), new Random())
  val runnable2 = new LdaRunnable(docTokens, 
    new LdaReportingHandler(symbolTable), new Random())
  val t1 = new Thread(runnable1)
  val t2 = new Thread(runnable2)
  t1.start()
  t2.start()
  t1.join()
  t2.join()
  val lda1 = runnable1.lda
  val lda2 = runnable2.lda
  Console.println("Computing Greedy Aligned Symmetrized KL Divergence")
  val scores = similarity(lda1, lda2)
  scores.foreach(score => {
    var i = 0
    Console.println("%4d %15.3f".format(i, scores(i)))
    i += 1
  })
  
  def similarity(lda1: LatentDirichletAllocation, 
      lda2: LatentDirichletAllocation): Array[Double] = {
    val numTopics = lda1.numTopics()
    val numPairs = numTopics * (numTopics - 1)
    var pairs = new Array[ScoredObject[Array[Int]]](numPairs)
    var pos = 0
    for (i <- 0 until numTopics) {
      for (j <- 0 until numTopics) {
        if (i != j) {
          val divergence = Statistics.symmetrizedKlDivergence(
            lda1.wordProbabilities(i), lda2.wordProbabilities(j))
          pairs(pos) = new ScoredObject[Array[Int]](
            Array[Int](i, j), divergence)
          pos += 1
        }
      }
    }
    var scores = new Array[Double](numTopics)
    for (pos <- 0 until numTopics) {
      scores(pos) = pairs(pos).score()
    }
    scores
  }
  
  def sample(docTokens: Array[Array[Int]], 
      handler: ObjectHandler[LatentDirichletAllocation.GibbsSample],
      random: Random): LatentDirichletAllocation = {
    val NumTopics: Short = 50
    val TopicPrior = 0.1
    val WordPrior = 0.01
    val BurnInEpochs = 0
    val SampleLag = 1
    val NumSamples = 2000
    val sample = LatentDirichletAllocation.gibbsSampler(
      docTokens, NumTopics, TopicPrior, WordPrior, 
      BurnInEpochs, SampleLag, NumSamples, random, handler)
    sample.lda()
  }

  class LdaRunnable(docTokens: Array[Array[Int]], 
      handler: ObjectHandler[LatentDirichletAllocation.GibbsSample],
      random: Random) extends Runnable {
    var lda: LatentDirichletAllocation = null
  
    override def run(): Unit = {
      lda = sample(docTokens, handler, random)
    }
  }
}
