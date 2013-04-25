package org.suffix.svd

import com.aliasi.matrix.SvdMatrix

object LsiDemo extends App {
  val Terms = Array[String](
    "human", "interface", "computer", "user", "system",
    "response", "time", "EPS", "survey", "trees", "graph",
    "minors") 
  val Docs = Array[String](
    "Human machine interface for Lab ABC computer applications",
    "A survey of user opinion of computer system response time",
    "The EPS user interface management system",
    "System and human system engineering testing of EPS",
    "Relation of user-perceived response time to error measurement",
    "The generation of random, binary, unordered trees",
    "The intersection graph of paths in trees",
    "Graph minors IV: Widths of trees and well-quasi-ordering",
    "Graph minors: A survey"
  )
  val TDMatrix = Array[Array[Double]](
    Array[Double]( 1, 0, 0, 1, 0, 0, 0, 0, 0 ),
    Array[Double]( 1, 0, 1, 0, 0, 0, 0, 0, 0 ),
    Array[Double]( 1, 1, 0, 0, 0, 0, 0, 0, 0 ),
    Array[Double]( 0, 1, 1, 0, 1, 0, 0, 0, 0 ),
    Array[Double]( 0, 1, 1, 2, 0, 0, 0, 0, 0 ),
    Array[Double]( 0, 1, 0, 0, 1, 0, 0, 0, 0 ),
    Array[Double]( 0, 1, 0, 0, 1, 0, 0, 0, 0 ),
    Array[Double]( 0, 0, 1, 1, 0, 0, 0, 0, 0 ),
    Array[Double]( 0, 1, 0, 0, 0, 0, 0, 0, 1 ),
    Array[Double]( 0, 0, 0, 0, 0, 1, 1, 1, 0 ),
    Array[Double]( 0, 0, 0, 0, 0, 0, 1, 1, 1 ),
    Array[Double]( 0, 0, 0, 0, 0, 0, 0, 1, 1 )
  )
  val NumFactors = 2
  val FeatureInit = 0.01
  val InitialLearningRate = 0.005
  val AnnealingRate = 1000
  val Regularization = 0.00
  val MinImprovement = 0.0000
  val MinEpochs = 10
  val MaxEpochs = 50000
  
  val matrix = SvdMatrix.svd(TDMatrix, NumFactors, FeatureInit, 
    InitialLearningRate, AnnealingRate, Regularization, null,
    MinImprovement, MinEpochs, MaxEpochs)
    
  val scales = matrix.singularValues()
  val termVectors = matrix.leftSingularVectors()
  val docVectors = matrix.rightSingularVectors()
  
  Console.println("SCALES:")
  for (k <- 0 until NumFactors) {
    Console.println("%d  %4.2f".format(k, scales(k)))
  }
  Console.println("TERM VECTORS:")
  for (i <- 0 until termVectors.length) {
    for (j <- 0 until NumFactors) {
      Console.print(" %5.2f".format(termVectors(i)(j)))
    }
    Console.println(" : %s".format(Terms(i)))
  }
  Console.println("DOC VECTORS")
  for (i <- 0 until docVectors.length) {
    for (j <- 0 until NumFactors) {
      Console.print(" %5.2f".format(docVectors(i)(j)))
    }
    Console.println(" : %s".format(Docs(i)))
  }

  val searchTerms = Array[String](
    "human computer interaction"    
  )
  
  searchTerms.foreach(searchTerm => {
    val terms = searchTerm.split(" |,")
    var queryVector = new Array[Double](NumFactors)
    terms.foreach(term => addTermVector(term, queryVector))
    Console.println("Query Vector: " + queryVector.toList)
    Console.println("Document Scores vs Query")
    for (i <- 0 until docVectors.length) {
      val score = dotProduct(queryVector, docVectors(i), scales)
      Console.println("  %d:  %5.2f  %s".format(i, score, Docs(i)))
    }
    Console.println("Term Scores vs Query")
    for (i <- 0 until termVectors.length) {
      val score = dotProduct(queryVector, termVectors(i), scales)
      Console.println("  %d:  %5.2f  %s".format(i, score, Terms(i)))
    }
  })
  
  def addTermVector(term: String, queryVector: Array[Double]): Unit = {
    for (i <- 0 until Terms.length) {
      if (Terms(i).equals(term)) {
        for (j <- 0 until NumFactors) {
          queryVector(j) += termVectors(i)(j)
        }
      }
    }
  }
  
  def dotProduct(xs: Array[Double], ys: Array[Double], 
      scales: Array[Double]): Double = {
   var sum = 0.0
   for (i <- 0 until xs.length) {
     sum += xs(i) * ys(i) * scales(i)
   }
   sum
  }
}