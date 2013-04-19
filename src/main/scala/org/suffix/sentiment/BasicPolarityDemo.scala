package org.suffix.sentiment

import java.io.File

import com.aliasi.classify.{Classification, Classified, DynamicLMClassifier}
import com.aliasi.util.Files

object BasicPolarityDemo extends App {

  val polarityDir = new File("data/sentiment/txt_sentoken")
  val categories = polarityDir.list()
  val ngram = 8
  val classifier = DynamicLMClassifier.createNGramProcess(
    categories, ngram)
  train()
  evaluate()
  
  def isTrainingFile(file: File): Boolean = {
    // test on fold 9, so anything else is training
    file.getName().charAt(2) != '9'
  }
  
  def train(): Unit = {
    Console.println("Training...")
    var numTrainingCases = 0
    var numTrainingChars = 0
    categories.foreach(category => {
      val classification = new Classification(category)
      val trainFiles = new File(polarityDir, category).listFiles()
      trainFiles.foreach(trainFile => {
        numTrainingCases += 1
        if (isTrainingFile(trainFile)) {
          numTrainingCases += 1
          val review = Files.readFromFile(trainFile, "ISO-8859-1")
          numTrainingChars += review.length()
          val classified = 
            new Classified[CharSequence](review, classification)
          classifier.handle(classified)
        }
      })
    })
    Console.println("  # Training Cases=" + numTrainingCases)
    Console.println("  # Training Chars=" + numTrainingChars)
  }
  
  def evaluate(): Unit = {
    Console.println("Evaluating...")
    var numTests = 0
    var numCorrect = 0
    categories.foreach(category => {
      val trainFiles = new File(polarityDir, category).listFiles()
      trainFiles.foreach(trainFile => {
        if (! isTrainingFile(trainFile)) {
          val review = Files.readFromFile(trainFile, "ISO-8859-1")
          numTests += 1
          val classification = classifier.classify(review)
          if (classification.bestCategory().equals(category))
            numCorrect += 1
        }
      })
    })
    val pcCorrect = 
      numCorrect.asInstanceOf[Double] /
      numTests.asInstanceOf[Double]
    Console.println("  # Test Cases=" + numTests)
    Console.println("  # Correct   =" + numCorrect)
    Console.println("  % Correct   =" + pcCorrect)
  }
}