package org.suffix.crf

import java.io.File
import java.util.Collections

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ArrayBuffer

import com.aliasi.corpus.{Corpus, ObjectHandler}
import com.aliasi.crf.{ChainCrf, ChainCrfFeatureExtractor, ChainCrfFeatures}
import com.aliasi.io.{LogLevel, Reporters}
import com.aliasi.stats.{AnnealingSchedule, RegressionPrior}
import com.aliasi.tag.Tagging
import com.aliasi.util.AbstractExternalizable

object SimplePosTagDemo extends App {

  val ModelFile = new File("models/SimplePos.ChainCrf.bin")
  
  val AddIntercept = true
  val MinFeatureCount = 1
  val CacheFeatures = false
  val AllowUnseenTransitions = true
  val PriorVariance = 4.0
  val UninformativeIntercept = true
  val PriorBlockSize = 3
  val InitialLearningRate = 0.05
  val LearningRateDecay = 0.995
  val MinImprovement = 0.00001
  val MinEpochs = 2
  val MaxEpochs = 2000
  
  Console.println("Training CRF...")
  train()
  Console.println("Test CRF...")
  test("John ran .")
  
  def train(): Unit = {
    val corpus = new TinyPosCorpus()
    val featureExtractor = new SimpleChainCrfFeatureExtractor()
  
    val prior = RegressionPrior.gaussian(
      PriorVariance, UninformativeIntercept)
    val annealingSchedule = AnnealingSchedule.exponential(
      InitialLearningRate, LearningRateDecay)
    val reporter = Reporters.stdOut().setLevel(LogLevel.DEBUG)
    
    Console.println("Estimating...")
    val crf = ChainCrf.estimate(corpus, 
      featureExtractor, AddIntercept, MinFeatureCount, 
      CacheFeatures, AllowUnseenTransitions, prior, 
      PriorBlockSize, annealingSchedule, MinImprovement, 
      MinEpochs, MaxEpochs, reporter)
    Console.println("Compiling to file:" + ModelFile.getName())
    AbstractExternalizable.serializeTo(crf, ModelFile)
  }
  
  def test(sentence: String): Unit = {
    val crf = AbstractExternalizable.readObject(ModelFile).
      asInstanceOf[ChainCrf[String]]
    val tokens = sentence.split(" ").toList
    Console.println("First best")
    val tagging = crf.tag(tokens)
    Console.println(tagging)
    val MaxNBest = 5
    Console.println(MaxNBest + " Best Conditional")
    Console.println("Rank log p(tags|tokens)  Tagging")
    val it = crf.tagNBestConditional(tokens, MaxNBest)
    var rank = 0
    while (it.hasNext()) {
      val scoredTagging = it.next()
      Console.println(rank + "    " + scoredTagging)
      rank += 1
    }
    Console.println("Marginal Tag Probabilities")
    Console.println("Token .. Tag log p(tag|pos,tokens)")
    val fbLattice = crf.tagMarginal(tokens)
    for (i <- 0 until tokens.size) {
      Console.println(tokens(i))
      for (j <- 0 until fbLattice.numTags()) {
        val tag = fbLattice.tag(j)
        val prob = fbLattice.logProbability(i, j)
        Console.println("     " + tag + " " + prob)
      }
    }
  }
}

class TinyPosCorpus extends Corpus[ObjectHandler[Tagging[String]]] {
  
  val Sentences = Array[String](
    "John/PN ran/IV ./EOS",
    "Mary/PN ran/IV ./EOS",
    "John/PN jumped/IV ./EOS",
    "The/DET dog/N jumped/IV ./EOS",
    "The/PN dog/N sat/IV ./EOS",
    "Mary/PN sat/IV !/EOS",
    "Mary/PN likes/IV John/PN ./EOS",
    "The/DET dog/N likes/TV Mary/PN ./EOS",
    "John/PN likes/TV the/DET dog/N ./EOS",
    "The/DET dog/N ran/IV ./EOS",
    "The/DET dog/N ran/IV ./EOS"
  )

  override def visitTrain(handler: ObjectHandler[Tagging[String]]): 
      Unit = {
    Sentences.foreach(sentence => {
      val tokens = ArrayBuffer[String]()
      val tags = ArrayBuffer[String]()
      sentence.split(" ").foreach(wtp => {
        val ttp = wtp.split("/")
        tokens += ttp(0)
        tags += ttp(1)
        val tagging = new Tagging[String](tokens.toList, tags.toList)
        handler.handle(tagging)
      })
    })
  }
  
  override def visitTest(handler: ObjectHandler[Tagging[String]]): 
      Unit = {
    /* NO OP */
  }
}

class SimpleChainCrfFeatureExtractor 
    extends ChainCrfFeatureExtractor[String] 
    with Serializable {
  
  override def extract(tokens: java.util.List[String], 
      tags: java.util.List[String]): ChainCrfFeatures[String] = {
    new SimpleChainCrfFeatures(tokens, tags)
  }
}

class SimpleChainCrfFeatures(tokens: java.util.List[String], 
    tags: java.util.List[String]) 
    extends ChainCrfFeatures[String](tokens, tags) {
  
  override def nodeFeatures(n: Int): 
      java.util.Map[String,java.lang.Number] = {
    return Collections.singletonMap("TOK_" + token(n), 1)
  }
  
  override def edgeFeatures(n: Int, k: Int): 
      java.util.Map[String,java.lang.Number] = {
    return Collections.singletonMap("PREV_TAG_" + tag(k), 1)
  }
}
