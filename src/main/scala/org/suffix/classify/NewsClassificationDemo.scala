package org.suffix.classify

import java.io.File
import com.aliasi.classify.DynamicLMClassifier
import com.aliasi.util.Files
import com.aliasi.classify.Classification
import com.aliasi.classify.Classified
import com.aliasi.util.AbstractExternalizable
import com.aliasi.classify.JointClassifier
import com.aliasi.classify.JointClassifierEvaluator

object NewsClassificationDemo extends App {

  val TrainingDir = new File("data/fourNewsGroups/4news-train")
  val TestingDir = new File("data/fourNewsGroups/4news-test")
  val Categories = Array[String](
    "soc.religion.christian",
    "talk.religion.misc",
    "alt.atheism",
    "misc.forsale")
  val NGramSize = 6
  
  val classifier = DynamicLMClassifier.createNGramProcess(
    Categories, NGramSize)
  for (i <- 0 until Categories.length) {
    val classdir = new File(TrainingDir, Categories(i))
    if (classdir.isDirectory()) {
      val trainingFiles = classdir.list()
      for (j <- 0 until trainingFiles.length) {
        val file = new File(classdir, trainingFiles(i))
        val text = Files.readFromFile(file, "ISO-8859-1")
        Console.println("Training on " + file.getAbsolutePath())
        val classification = new Classification(Categories(i))
        val classified = 
          new Classified[CharSequence](text, classification)
        classifier.handle(classified)
      }
    }
  }
  Console.println("Compiling")
  val compiledClassifier = AbstractExternalizable.compile(
    classifier).asInstanceOf[JointClassifier[CharSequence]]
  val evaluator = new JointClassifierEvaluator[CharSequence](
    compiledClassifier, Categories, true)
  for (i <- 0 until Categories.length) {
    val classdir = new File(TestingDir, Categories(i))
    if (classdir.isDirectory()) {
      val testingFiles = classdir.list()
      for (j <- 0 until testingFiles.length) {
        val file = new File(classdir, testingFiles(i))
        val text = Files.readFromFile(file, "ISO-8859-1")
        Console.print("Testing on " + file.getAbsolutePath())
        val classification = new Classification(Categories(i))
        val classified = 
          new Classified[CharSequence](text, classification)
        evaluator.handle(classified)
        val jointClassifier = compiledClassifier.classify(text)
        Console.println(" [expected: " +
          Categories(i) + ", got: " + 
          jointClassifier.bestCategory() + "]")
        Console.println(jointClassifier.toString())
        Console.println("---------")
      }
    }
  }
  val confMatrix = evaluator.confusionMatrix()
  Console.println("Total Accuracy: " + confMatrix.totalAccuracy())
  Console.println("Full Evaluation:\n" + evaluator)
}