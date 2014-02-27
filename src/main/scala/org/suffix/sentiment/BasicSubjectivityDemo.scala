package org.suffix.sentiment

import java.io.{File, FileOutputStream, ObjectOutputStream}

import com.aliasi.classify.{Classification, Classified, DynamicLMClassifier, JointClassifierEvaluator}
import com.aliasi.util.Files

object BasicSubjectivityDemo extends App {

  val polarityDir = new File("data/sentiment/")
  val categories = Array[String]("plot", "quote")
  val ngram = 8
  val classifier = 
    DynamicLMClassifier.createNGramProcess(categories, ngram)
  val modelFile = new File("models/subjectivity.model")
  train()
  evaluate()
  
  def train(): Unit = {
    var numTrainingChars = 0
    Console.println("Training...")
    categories.foreach(category => {
      val classification = new Classification(category)
      val file = new File(polarityDir, category + ".tok.gt9.5000")
      val data = Files.readFromFile(file, "ISO-8859-1")
      val sentences = data.split("\n")
      Console.println("# Sentences (" + category + 
        ")=" + sentences.length)
      val numTraining: Int = sentences.length * 9 / 10
      for (j <- 0 until numTraining) {
        val sentence = sentences(j)
        numTrainingChars += sentence.length()
        val classified = 
          new Classified[CharSequence](sentence, classification)
        classifier.handle(classified)
      }
    })
    Console.println("Compiling model...")
    val objOut = new ObjectOutputStream(new FileOutputStream(modelFile))
    classifier.compileTo(objOut)
    objOut.close()
    Console.println("  # Training Cases=9000")
    Console.println("  # Training Chars=" + numTrainingChars)
  }
  
  def evaluate(): Unit = {
    val evaluator = new JointClassifierEvaluator[CharSequence](
      classifier, categories, false)
    Console.println("Evaluating...")
    categories.foreach(category => {
      val classification = new Classification(category)
      val file = new File(polarityDir, category + ".tok.gt9.5000")
      val data = Files.readFromFile(file, "ISO-8859-1")
      val sentences = data.split("\n")
      val numTraining: Int = sentences.length * 9 / 10
      for (j <- numTraining until sentences.length) {
        val classified = new Classified[CharSequence](
          sentences(j), classification)
        evaluator.handle(classified)
      }
    })
    Console.println(evaluator.toString())
  }
}