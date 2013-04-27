package org.suffix.cjkseg

import java.io.{File, ObjectInput, ObjectOutput}
import java.util.HashSet

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaSet

import com.aliasi.classify.PrecisionRecallEvaluation
import com.aliasi.lm.NGramProcessLM
import com.aliasi.spell.{CompiledSpellChecker, FixedWeightEditDistance, TrainSpellChecker}
import com.aliasi.util.{AbstractExternalizable, Compilable, Files}

object ChineseTokenizationDemo extends App {

  val TrainFile = new File("data/sinica/sinica_train.txt")
  val TestFile = new File("data/sinica/sinica_test.txt")

  val CharEncoding = "CP950"
  val MaxNGram = 4
  val LambdaFactor = 4.0
  val NumChars = 5000
  val MaxNBest = 256
  val ContinueWeight = 0.0
  val BreakWeight = 0.0
  
  val trainingCharSet = new HashSet[Char]()
  val testCharSet = new HashSet[Char]()
  val trainingTokenSet = new HashSet[String]()
  val testTokenSet = new HashSet[String]()
  
  val spellChecker = train()
  test(spellChecker)
  
  def train(): CompiledSpellChecker = {
    Console.println("Training...")
    val lm = new NGramProcessLM(MaxNGram, NumChars, LambdaFactor)
    val dist = new ChineseTokenizing(ContinueWeight, BreakWeight)
    val trainer = new TrainSpellChecker(lm, dist, null)
    val lines = extractLines(TrainFile, trainingCharSet, trainingTokenSet)
    lines.foreach(trainer.handle(_))
    val spellChecker = AbstractExternalizable.compile(trainer).
      asInstanceOf[CompiledSpellChecker]
    spellChecker.setAllowInsert(true)
    spellChecker.setAllowMatch(true)
    spellChecker.setAllowDelete(false)
    spellChecker.setAllowSubstitute(false)
    spellChecker.setAllowTranspose(false)
    spellChecker.setNumConsecutiveInsertionsAllowed(1)
    spellChecker.setNBest(MaxNBest)
    spellChecker
  }
  
  def test(spellChecker: CompiledSpellChecker): Unit = {
    Console.println("Testing...")
    val lines = extractLines(TestFile, testCharSet, testTokenSet)
    val pr = new PrecisionRecallEvaluation()
    lines.foreach(line => {
      val response = spellChecker.didYouMean(line)
      val refSpaces = getSpaces(line)
      val responseSpaces = getSpaces(response)
      evaluate(refSpaces, responseSpaces, pr)
    })
    Console.println("Precision=" + pr.precision() + 
      ", Recall=" + pr.recall() + 
      ", F-Measure=" + pr.fMeasure())
  }

  def getSpaces(xs: String): HashSet[Int] = {
    val breakSet = new HashSet[Int]()
    for (i <- 0 until xs.length()) {
      if (xs.charAt(i) == ' ') breakSet.add(i)
    }
    breakSet
  }
  
  def evaluate(ref: HashSet[Int], res: HashSet[Int], 
      pr: PrecisionRecallEvaluation): Unit = {
    val pr = new PrecisionRecallEvaluation()
    for (e <- ref) pr.addCase(true, res.contains(e))
    for (e <- res) 
      if (! ref.contains(e)) pr.addCase(false, true)
  }
  
  def extractLines(file: File, charset: HashSet[Char], 
      tokset: HashSet[String]): Array[String] = {
    Files.readFromFile(file, CharEncoding).split("\n").map(line => {
      val normline = line.replaceAll("\\s+", " ")
      addTokChars(charset, tokset, normline)
      normline
    })
  }
  
  def addTokChars(charset: HashSet[Char], tokset: HashSet[String], 
      line: String): Unit = {
    if (line.indexOf("  ") == 0) { // double space not allowed\u00e5
      val toks = line.split("\\s+")
      toks.foreach(tok => {
        if (tok.length() > 0) tokset.add(tok)
        tok.toCharArray().foreach(ch => {
          charset.add(ch)
        })
      })
    }
  }

}

class ChineseTokenizing(val breakWeight: Double, 
    val continueWeight: Double) 
    extends FixedWeightEditDistance with Compilable {
  
  override def insertWeight(inserted: Char): Double = {
    if (inserted == ' ') breakWeight else Double.NegativeInfinity
  }
  
  override def matchWeight(matched: Char): Double = {
    continueWeight
  }
  
  override def compileTo(objOut: ObjectOutput): Unit = {
    objOut.writeObject(new ChineseTokenizingExternalizable(this))
  }
}

class ChineseTokenizingExternalizable(dist: ChineseTokenizing) 
    extends AbstractExternalizable {
  
  def this() { this(null) }
  
  override def writeExternal(objOut: ObjectOutput): Unit = {
    objOut.writeDouble(dist.breakWeight)
    objOut.writeDouble(dist.continueWeight)
  }
    
  override def read(objIn: ObjectInput): Object = {
    new ChineseTokenizing(objIn.readDouble(), objIn.readDouble())
  }
}
