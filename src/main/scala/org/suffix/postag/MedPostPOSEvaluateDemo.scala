package org.suffix.postag

import java.io.File

import scala.collection.JavaConversions.mutableSetAsJavaSet
import scala.collection.mutable.HashSet

import com.aliasi.classify.ConfusionMatrix
import com.aliasi.corpus.ObjectHandler
import com.aliasi.hmm.{HmmCharLmEstimator, HmmDecoder}
import com.aliasi.tag.{MarginalTaggerEvaluator, NBestTaggerEvaluator, TaggerEvaluator, Tagging}

object MedPostPOSEvaluateDemo extends App {

  val sentEvalRate = 1
  val toksBeforeEval = 170000
  val maxNBest = 100
  val ngramSize = 8
  val numChars = 256
  val lambdaFactor = 8.0
  
  val trainFile = new File("data/medtag/medpost")
  val corpus = new MedPostPosCorpus(trainFile)

  var trainingSentenceCount = 0
  var trainingTokenCount = 0
  var estimator: HmmCharLmEstimator = null
  var taggerEvaluator: TaggerEvaluator[String] = null
  var nbestTaggerEvaluator: NBestTaggerEvaluator[String] = null
  var marginalTaggerEvaluator: MarginalTaggerEvaluator[String] = null
  val tagSet = new HashSet[String]()
  
  evaluate()
  
  def evaluate(): Unit = {
    Console.println("COMMAND PARAMETERS:" + 
      "\n    Sent eval rate=" + sentEvalRate +
      "\n    Toks before eval=" + toksBeforeEval +
      "\n    Max n-best eval=" + maxNBest +
      "\n    Max n-gram=" + ngramSize + 
      "\n    Num-Chars=" + numChars +
      "\n    Lambda-Factor=" + lambdaFactor)
    val profileHandler = new CorpusProfileHandler()
    parseCorpus(profileHandler)
    Console.println("CORPUS PROFILE:" +
      "\n    #-sentences: " + trainingSentenceCount +
      "\n    #-tokens: " + trainingTokenCount +
      "\n    #-tags: " + tagSet.size +
      "\n    Tags: " + tagSet.foldLeft("")(_ + "," + _))
    estimator = new HmmCharLmEstimator(ngramSize, numChars, lambdaFactor)
    for (tag <- tagSet) {
      estimator.addState(tag)
    }
    val decoder = new HmmDecoder(estimator)
    taggerEvaluator = new TaggerEvaluator[String](decoder, true)
    nbestTaggerEvaluator = new NBestTaggerEvaluator[String](
      decoder, maxNBest, maxNBest)
    marginalTaggerEvaluator = new MarginalTaggerEvaluator[String](
      decoder, tagSet, true)
    val evaluationHandler = new LearningCurveHandler()
    parseCorpus(evaluationHandler)
    Console.println("FINAL REPORT:" + 
      "\nFirst Best Evaluation: " + taggerEvaluator.tokenEval() + 
      "\nN Best Evaluation: " + nbestTaggerEvaluator.nBestHistogram())
  }
  
  def parseCorpus(handler: ObjectHandler[Tagging[String]]): Unit = {
    val parser = corpus.parser()
    parser.setHandler(handler)
    val sit = corpus.sourceIterator()
    while (sit.hasNext) {
      val inputSource = sit.next()
      parser.parse(inputSource)
    }
  }
  
  class CorpusProfileHandler extends ObjectHandler[Tagging[String]] {
    
    def handle(tagging: Tagging[String]): Unit = {
      trainingSentenceCount = trainingSentenceCount + 1
      trainingTokenCount += tagging.size()
      for (i <- 0 until tagging.size()) {
        tagSet.add(tagging.tag(i))
      }
    }
  }
  
  class LearningCurveHandler extends ObjectHandler[Tagging[String]] {
    
    val knownTokenSet = new HashSet[String]()
    var unknownTokensTotal = 0
    var unknownTokensCorrect = 0
    
    def handle(tagging: Tagging[String]): Unit = {
      if (estimator.numTrainingTokens() > toksBeforeEval &&
          estimator.numTrainingCases() % sentEvalRate == 0) {
        taggerEvaluator.handle(tagging)
        nbestTaggerEvaluator.handle(tagging)
        marginalTaggerEvaluator.handle(tagging)
        Console.println("Test Case: " + taggerEvaluator.numCases())
        Console.println("First Best Last Case Report: " + 
          taggerEvaluator.lastCaseToString(knownTokenSet))
        Console.println("N Best Last Case Report: " +
          nbestTaggerEvaluator.lastCaseToString(5))
        Console.println("Marginal Last Case Report: " + 
          marginalTaggerEvaluator.lastCaseToString(5))
        Console.println("Cumulative Evaluation:")
        Console.println("  Estimator: #Train Cases=" + 
          estimator.numTrainingCases() + 
          ", #Train Toks=" + 
          estimator.numTrainingTokens())
        val tokenEval: ConfusionMatrix = 
          taggerEvaluator.tokenEval().confusionMatrix()
        Console.println("  First Best Accuracy (All Tokens) = " +
          tokenEval.totalCorrect() + "/" + tokenEval.totalCount() + 
          " = " + tokenEval.totalAccuracy())
        val unkTokenEval: ConfusionMatrix =
          taggerEvaluator.unknownTokenEval(knownTokenSet).
          confusionMatrix()
        unknownTokensTotal += unkTokenEval.totalCount()
        unknownTokensCorrect += unkTokenEval.totalCorrect()
        Console.println("  First Best Accuracy (Unknown Tokens) = " +
          unknownTokensCorrect + "/" + unknownTokensTotal + 
          " = " + (unknownTokensCorrect / unknownTokensTotal))
      }
      // train after eval
      estimator.handle(tagging)
      for (i <- 0 until tagging.size()) {
        knownTokenSet.add(tagging.token(i))
      }
    }
  }
}