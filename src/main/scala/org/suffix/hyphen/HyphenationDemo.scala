package org.suffix.hyphen

import java.io.{File, FileWriter, PrintWriter}
import java.util.HashSet

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaSet
import scala.io.Source

import com.aliasi.classify.PrecisionRecallEvaluation
import com.aliasi.lm.NGramProcessLM
import com.aliasi.spell.{CompiledSpellChecker, TrainSpellChecker}
import com.aliasi.stats.Statistics
import com.aliasi.util.{AbstractExternalizable, Files, Strings}

object HyphenationDemo extends App {

  val NumFolds = 10
  val MaxNGrams = 8
  val NumChars = 64
  val InterpolationRatio = 4.0
  val HyphenationNBest = 1024
  
  val inputFile = new File("data/mhyph.txt")
  val dataOutFile = new File("data/mhyph.tmp")

  val dataOut = new PrintWriter(new FileWriter(dataOutFile))
  val lines = Source.fromFile(inputFile).getLines()
  lines.foreach(line => {
    dataOut.println(line.replaceAll("\uFFFD", " ").
      trim().replaceAll("\\s+", " ").toLowerCase())
  })
  dataOut.flush()
  dataOut.close()
  val hyphenatedWords = parseCorpus(dataOutFile)
  
  val accuracies = new Array[Double](NumFolds)
  val prEval = new PrecisionRecallEvaluation()
  for (fold <- 0 until NumFolds) {
    evaluateFold(hyphenatedWords, fold, NumFolds, accuracies, prEval)
  }
  Console.println(
    "NGRAM= %2d INTERP=%4.1f ACC=%5.3f P=%5.3f R=%5.3f F=%5.3f".
    format(MaxNGrams, InterpolationRatio, Statistics.mean(accuracies),
    prEval.precision(), prEval.recall(), prEval.fMeasure()))
  
  def parseCorpus(file: File): Array[String] = {
    Files.readFromFile(file, Strings.UTF8).
      split("\n").
      filter(line => line.length() > 9 && line.indexOf(' ') == -1).
      map(line => line.trim())
  }
  
  def evaluateFold(words: Array[String], fold: Int, 
      numFolds:Int, accuracies: Array[Double], 
      prEval: PrecisionRecallEvaluation): Unit = {
    val lm = new NGramProcessLM(MaxNGrams, NumChars, InterpolationRatio)
    val distance = CompiledSpellChecker.TOKENIZING
    val trainer = new TrainSpellChecker(lm, distance, null)
    for (i <- 0 until words.length) {
      if (i % numFolds != fold) {
        trainer.handle(words(i))
      }
    }
    Console.println("  Compiling hyphenator (" + fold + ")")
    val hyphenator = AbstractExternalizable.compile(trainer).
      asInstanceOf[CompiledSpellChecker]
    hyphenator.setAllowInsert(true)
    hyphenator.setAllowMatch(true)
    hyphenator.setAllowDelete(false)
    hyphenator.setAllowSubstitute(false)
    hyphenator.setAllowTranspose(false)
    hyphenator.setNumConsecutiveInsertionsAllowed(1)
    hyphenator.setFirstCharEditCost(0)
    hyphenator.setSecondCharEditCost(0)
    hyphenator.setNBest(1024)
    // evaluate
    var numCases = 0
    var numCorrect = 0
    for (i <- 0 until words.length) {
      val hyphenatedWord = words(i)
      val unhyphenatedWord = hyphenatedWord.replaceAll(" ", "")
      val rehyphenatedWord = hyphenator.didYouMean(unhyphenatedWord)
      if (hyphenatedWord.equals(rehyphenatedWord)) {
        Console.println("  " + hyphenatedWord + " = correct")
        updatePrEval(prEval, hyphenatedWord, rehyphenatedWord)
        numCorrect += 1
      } else {
        Console.println("  " + hyphenatedWord + " = incorrect")
      }
      numCases += 1
    }
    val accuracy = numCorrect.asInstanceOf[Double] / 
      numCases.asInstanceOf[Double]
    Console.println("  Fold: " + fold + ", accuracy=" + accuracy)
  }
  
  def updatePrEval(prEval: PrecisionRecallEvaluation, 
      reference: String, response: String): Unit = {
    val refBoundarySet = getBoundarySet(reference)
    val resBoundarySet = getBoundarySet(response)
    val universalSet = refBoundarySet ++ resBoundarySet
    for (i <- universalSet) {
      val ref = refBoundarySet.contains(i)
      val res = resBoundarySet.contains(i)
      prEval.addCase(ref, res)
    }
  }
  
  def getBoundarySet(hword: String): HashSet[Int] = {
    val boundarySet = new HashSet[Int]()
    var pos = 0
    for (i <- 0 until hword.length()) {
      if (hword.charAt(i) == ' ') boundarySet.add(pos)
      else pos += 1
    }
    boundarySet
  }
}
