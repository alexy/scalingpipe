package org.suffix.cluster

import java.io.File
import java.util.HashSet

import scala.collection.JavaConversions.{asScalaSet, collectionAsScalaIterable}

import com.aliasi.cluster.{ClusterScore, CompleteLinkClusterer, SingleLinkClusterer}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.util.{Distance, Files, ObjectToCounterMap, Strings}

object DocumentClusteringDemo extends App {

//  val inputDir = new File("data/fourNewsGroups/4news-train")
  val inputDir = new File("data/johnSmith")
  
  val CosineDistance = new Distance[Document]() {
    override def distance(doc1: Document, doc2: Document): Double = {
      val oneMinusCosine = 1.0 - doc1.cosine(doc2)
      if (oneMinusCosine > 1.0) 1.0
      else if (oneMinusCosine < 0.0) 0
      else oneMinusCosine
    }
  }
  val TokFactory = IndoEuropeanTokenizerFactory.INSTANCE
  
  val refPartition = new HashSet[HashSet[Document]]()
  inputDir.listFiles().foreach(categoryDir => {
    val docsForCat = new HashSet[Document]()
    categoryDir.listFiles().foreach(file => {
      docsForCat.add(new Document(file))
    })
    refPartition.add(docsForCat)
  })
  val docSet = new HashSet[Document]()
  refPartition.foreach(cluster => docSet.addAll(cluster))
  
  // eval clusterers
  val clClusterer = new CompleteLinkClusterer[Document](CosineDistance)
  val clDendogram = clClusterer.hierarchicalCluster(docSet)
  
  val slClusterer = new SingleLinkClusterer[Document](CosineDistance)
  val slDendogram = slClusterer.hierarchicalCluster(docSet)
  
  Console.println(" --------------------------------------------------------")
  Console.println("|  K  |  Complete      |  Single        |  Cross         ")
  Console.println("|     |  P    R    F   |  P    R    F   |  P    R   F    ")
  Console.println(" --------------------------------------------------------")
  for (i <- 1 to docSet.size()) {
    val clResPartition = clDendogram.partitionK(i)
    val clScore = new ClusterScore[Document](refPartition, clResPartition)
    val clPrEval = clScore.equivalenceEvaluation()
    val slResPartition = slDendogram.partitionK(i)
    val slScore = new ClusterScore[Document](refPartition, slResPartition)
    val slPrEval = slScore.equivalenceEvaluation()
    val xScore = new ClusterScore[Document](clResPartition, slResPartition)
    val xPrEval = xScore.equivalenceEvaluation()
    Console.println("| %3d | %3.2f %3.2f %3.2f | %3.2f %3.2f %3.2f | %3.2f %3.2f %3.2f".
      format(i, clPrEval.precision(), clPrEval.recall(), 
      clPrEval.fMeasure(), slPrEval.precision(), slPrEval.recall(),
      slPrEval.fMeasure(), xPrEval.precision(), xPrEval.recall(),
      xPrEval.fMeasure()))
  }
}

class Document(file: File) {
  val text = Files.readCharsFromFile(file, Strings.UTF8)
  val tokenizer = DocumentClusteringDemo.TokFactory.
    tokenizer(text, 0, text.length)
  val tokenCounter = new ObjectToCounterMap[String]()
  var token: String = null
  do {
    token = tokenizer.nextToken()
    if (token != null) 
      tokenCounter.increment(token.toLowerCase())
  } while (token != null)
  val length = Math.sqrt(tokenCounter.values().
    map(counter => counter.doubleValue()).toList.
    foldLeft(0.0D)(_ + _))
  
  def cosine(that: Document): Double = {
    product(that) / (length * that.length)
  }
  
  def product(that: Document): Double = {
    Math.sqrt(tokenCounter.keySet().map(token => {
      val count = that.tokenCounter.getCount(token)
      count * tokenCounter.getCount(token)
    }).foldLeft(0)(_ + _))
  }
}