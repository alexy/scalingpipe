package org.suffix.sips

import java.io.File
import java.util.SortedSet

import scala.collection.JavaConversions.asScalaSet

import com.aliasi.lm.TokenizedLM
import com.aliasi.tokenizer.{IndoEuropeanTokenizerFactory, TokenizerFactory}
import com.aliasi.util.{Files, ScoredObject}

object InterestingPhrasesDemo extends App {

  val NGram = 3
  val MinCount = 5
  val MaxNGramReportingLength = 2
  val NGramReportingLength = 2
  val MaxCount = 100
  
  val BackgroundDir = new File("data/rec.sport.hockey/train")
  val ForegroundDir = new File("data/rec.sport.hockey/test")
  
  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  Console.println("Training background model")
  val backgroundModel = buildModel(tokenizerFactory, NGram, BackgroundDir)
  backgroundModel.sequenceCounter().prune(3)
  
  Console.println("Assembling collocations in training")
  val coll = backgroundModel.collocationSet(
    NGramReportingLength, MinCount, MaxCount)
    
  Console.println("Collocations in order of significance")
  report(coll)
  
  Console.println("Training foreground model")
  val foregroundModel = buildModel(tokenizerFactory, NGram, ForegroundDir)
  foregroundModel.sequenceCounter().prune(3)
  
  Console.println("Assembling new terms in test vs training")
  val newTerms = foregroundModel.newTermSet(NGramReportingLength, 
    MinCount, MaxCount, backgroundModel)
  
  Console.println("New terms in order of significance")
  report(newTerms)
  
  Console.println("Done")
  
  def buildModel(factory: TokenizerFactory, ngram: Int, 
      directory: File): TokenizedLM = {
    val model = new TokenizedLM(factory, ngram)
    Console.println("Training on: " + directory.getName())
    for (trainingFile <- directory.listFiles()) {
      val text = Files.readFromFile(trainingFile, "ISO-8859-1")
      model.handle(text)
    }
    model
  }
  
  def report(ngrams: SortedSet[ScoredObject[Array[String]]]): Unit = {
    ngrams.foreach(ngram => {
      val score = ngram.score()
      val toks = ngram.getObject()
      val ftoks = toks.filter(
        tok => (Character.isUpperCase(tok.charAt(0)) && 
        tok.substring(1).equals(tok.substring(1).toLowerCase())))
      if (ftoks.length > 1)
        Console.println("Score: " + score + " with: " + 
          ftoks.foldLeft("")(_ + " " + _))
    })
  }
}