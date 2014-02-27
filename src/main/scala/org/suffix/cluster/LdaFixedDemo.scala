package org.suffix.cluster

import java.util.Random

import com.aliasi.cluster.LatentDirichletAllocation
import com.aliasi.corpus.ObjectHandler
import com.aliasi.symbol.MapSymbolTable
import com.aliasi.util.{ObjectToCounterMap, Strings}

object LdaFixedDemo extends App {

  val NumTopics: Short = 2
  val DocTopicPrior = 0.1
  val TopicWordPrior = 0.01
  val BurnInEpochs = 0
  val SampleLag = 1
  val NumSamples = 16
  val Words = Array[String](
    "river", "stream", "bank", "money", "loan")
  val DocWords = Array[Array[Int]](
    Array[Int]( 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4 ),
    Array[Int]( 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4 ),
    Array[Int]( 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4 ),
    Array[Int]( 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4 ),
    Array[Int]( 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 4 ),
    Array[Int]( 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4 ),
    Array[Int]( 0, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4 ),
    Array[Int]( 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4 ),
    Array[Int]( 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4 ),
    Array[Int]( 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 4, 4, 4, 4 ),
    Array[Int]( 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4 ),
    Array[Int]( 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3 ),
    Array[Int]( 0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 4 ),
    Array[Int]( 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2 ),
    Array[Int]( 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 ),
    Array[Int]( 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2 ))
  
  val symbolTable = new MapSymbolTable()
  Words.foreach(word => symbolTable.getOrAddSymbol(word))
  
  val random = new Random(42)
  
  val handler = new LdaReportingHandler(symbolTable)
  val sample = LatentDirichletAllocation.gibbsSampler(DocWords, 
      NumTopics, DocTopicPrior, TopicWordPrior, BurnInEpochs, 
      SampleLag, NumSamples, random, handler)
  handler.fullReport(sample, 5, 2, true)
}

class LdaReportingHandler(symbolTable: MapSymbolTable) 
    extends ObjectHandler[LatentDirichletAllocation.GibbsSample] {
  
  val startTime = System.currentTimeMillis()
  
  override def handle(sample: LatentDirichletAllocation.GibbsSample): 
      Unit = {
    Console.println("Epoch=%3d, elapsed time=%s".format(sample.epoch(), 
      Strings.msToString(System.currentTimeMillis() - startTime)))
    if (sample.epoch() % 10 == 0) {
      val corpusLog2Prob = sample.corpusLog2Probability()
      Console.println("    log2 p(corpus|phi,theta)=" + 
        corpusLog2Prob + ", token cross entropy rate=" + 
        (-corpusLog2Prob/sample.numTokens()))
    }
  }
  
  def fullReport(sample: LatentDirichletAllocation.GibbsSample,
      maxWordsPerTopic: Int, maxTopicsPerDoc: Int, 
      reportTakens: Boolean): Unit = {
    Console.println("Epoch=" + sample.epoch() + ", docs=" + 
      sample.numDocuments() + ", tokens=" + sample.numTokens() +
      ", words=", sample.numWords() + ", topics=" + sample.numTopics())
    for (topic <- 0 until sample.numTopics()) {
      val topicCount = sample.topicCount(topic)
      val counter = new ObjectToCounterMap[Int]()
      for (word <- 0 until sample.numWords()) {
        counter.set(word, sample.topicWordCount(topic, word))
      }
      val topWords = counter.keysOrderedByCountList()
      Console.println("TOPIC " + topic  + 
        "  (total count=" + topicCount + ")")
      Console.println("SYMBOL             WORD    COUNT   PROB          Z")
      Console.println("--------------------------------------------------")
      for (rank <- Iterator.iterate(0)(x => x + 1).
          takeWhile(x => x < maxWordsPerTopic && x < topWords.size())) {
        val wordId = topWords.get(rank)
        val word = symbolTable.idToSymbol(wordId)
        val wordCount = sample.wordCount(wordId)
        val topicWordCount = sample.topicWordCount(topic, wordId)
        val topicWordProb = sample.topicWordProb(topic, wordId)
        val z = binomialZ(topicWordCount, topicCount, wordCount, 
          sample.numTokens())
        Console.println("%6d  %15s  %7d   %4.3f  %8.1f".format(
          wordId, word, topicWordCount, topicWordProb, z))
      }
    }
    for (doc <- 0 until sample.numDocuments()) {
      var docCount = 0
      for (topic <- 0 until sample.numTopics()) {
        docCount += sample.documentTopicCount(doc, topic)
      }
      val counter = new ObjectToCounterMap[Int]()
      for (topic <- 0 until sample.numTopics()) {
        counter.set(topic, sample.documentTopicCount(doc, topic))
      }
      val topTopics = counter.keysOrderedByCountList()
      for (rank <- Iterator.iterate(0)(x => x + 1).
          takeWhile(x => x < topTopics.size && x < maxTopicsPerDoc)) {
        val topic = topTopics.get(rank)
        val docTopicCount = sample.documentTopicCount(doc, topic)
        val docTopicPrior = sample.documentTopicPrior()
        val docTopicProb = (docTopicCount + docTopicPrior) /
          (docCount + sample.numTopics() + docTopicPrior)
        Console.println("%5d  %7d   %4.3f".
          format(topic, docTopicCount, docTopicProb))
      }
      val numDocTokens = sample.documentLength(doc)
      for (tok <- 0 until numDocTokens) {
        val symbol = sample.word(doc, tok)
        val topic = sample.topicSample(doc, tok)
        val word = symbolTable.idToSymbol(symbol)
        Console.print(word + " (" + topic + ") ")
      }
      if (numDocTokens > 0) Console.println()
    }
  }
  
  def binomialZ(wordCountInDoc: Double, wordsInDoc: Double,
      wordCountInCorpus: Double, wordsInCorpus: Double): Double = {
    val pCorpus = wordCountInCorpus / wordsInCorpus
    (wordCountInDoc - (wordsInDoc * pCorpus)) /
      Math.sqrt(wordsInCorpus * pCorpus * (1 - pCorpus))
  }
}