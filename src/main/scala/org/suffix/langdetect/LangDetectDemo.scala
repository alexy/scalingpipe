package org.suffix.langdetect

import java.io.File

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaBuffer

import com.aliasi.classify.{BaseClassifier, BaseClassifierEvaluator, Classification, Classified, DynamicLMClassifier}
import com.aliasi.io.FileLineReader
import com.aliasi.util.AbstractExternalizable

object LangDetectDemo extends App {

  val trainDir = new File("data/leipzig")
  val modelFile = new File("models/langid-leipzig.classifier")
//  val modelFile = new File("models/langid-leipzig-classifier.bin")
  
  val NGramSize = 5
  val TrainingSize = 2000000
  val TestSize = 32
  val MinTokenCount = 20
  val NumTestsPerCategory = 10
  
  ///////////////// train and build the model ///////////////////
//  train(trainDir, modelFile)
  ///////////////// evaluate the model //////////////////////////
//  evaluate(modelFile, trainDir)
  ///////////////// run the model ///////////////////////////////
  val sentences = Array[String](
    "Els temes poden ser tancats per moltes raons.",
    "But while VRE is not a threat to healthy individuals, its effect on the four HIV patients is potentially serious.",
    "Voiko sulle tulla juttelemaan keikkojen jälkeen?",
    "Cest la relation à la parole et laliénation du sujet en tant que nous y avons accès par la parole.",
    "गत वर्ष एलोरा और एलिफैंटा की गुफाओं, तथा सांची के बुद्ध स्थलों पर पर्यटकों का आवागमन बढ़ा है ।",
    "Possibile che fosse lui?",
    "六 月 十 九 日 まで 。",
    "박정아는 자연스럽게 아이 머리를 감싼 채 포즈를 취한 사진을 보며 “지금껏 찍은 사진 중 가장 예쁜 사진 같다”며 사진 속 아이를 쓰다듬었다.",
    "Ale my nic nikomu nie jesteśmy winni - tłumaczył Owsiak.",
    "Экономичный вид покрытия : наносится всего в один, максимум в два слоя.",
    "Según Juan March, esta operación se justifica porque el bajo precio de cotización.",
    "Çok gürültü ve fazla ziyaretçiden hoşlanmam."
  )
  test(modelFile, sentences)
  
  def train(trainDir: File, modelFile: File): Unit = {
    val categories = getCategories(trainDir)
    val classifier = DynamicLMClassifier.createNGramProcess(
      categories, NGramSize)
    for (category <- categories) {
      Console.println("Training Category: " + category)
      val trainFile = new File(trainDir, category + "-sentences.txt")
      val text = mergeLines(trainFile).substring(0, TrainingSize)
     val classification = new Classification(category)
      val classified = new Classified[CharSequence](
        text, classification)
      classifier.handle(classified)
    }
    for (category <- categories) 
      classifier.languageModel(category).substringCounter().prune(MinTokenCount)
    Console.println("Compiling model to file: " + modelFile.getName())
    AbstractExternalizable.compileTo(classifier, modelFile)
  }
  
  def evaluate(modelFile: File, trainDir: File): Unit = {
    val classifier = AbstractExternalizable.readObject(modelFile).
      asInstanceOf[BaseClassifier[CharSequence]]
    val categories = getCategories(trainDir)
    val evaluator = new BaseClassifierEvaluator[CharSequence](
      classifier, categories, false)
    for (category <- categories) {
      Console.println("Evaluating category: " + category)
      val trainFile = new File(trainDir, category + "-sentences.txt")
      val text = mergeLines(trainFile)
      var start = TrainingSize
      for (k <- 0 until NumTestsPerCategory) {
        var end = start + TestSize
        val testText = text.substring(start, end)
        val classification = new Classification(category)
        val classified = new Classified[CharSequence](
          testText, classification)
        evaluator.handle(classified)
        start = end
      }
    }
    Console.println("TEST RESULTS:\n" + evaluator.toString())
  }
  
  def test(modelFile: File, sentences: Array[String]): Unit = {
    val classifier = AbstractExternalizable.readObject(modelFile).
      asInstanceOf[BaseClassifier[CharSequence]]
    for (sentence <- sentences) {
      Console.print("SENTENCE: " + sentence)
      val classification = classifier.classify(sentence)
      Console.println("  [" + classification.toString() + "]")
    }
  }
  
  def getCategories(trainDir: File): Array[String] = {
    trainDir.list().map(x => x.substring(0, x.indexOf('-')))
  }
  
  def mergeLines(file: File): String = {
    FileLineReader.readLines(file, "UTF-8").
      map(x => x.split("\t")(1)).
      foldLeft("")(_ + " " + _)
  }
}