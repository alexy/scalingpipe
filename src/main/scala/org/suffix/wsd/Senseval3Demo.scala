
package org.suffix.wsd

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.util.{ArrayList, HashMap, HashSet}

import scala.collection.JavaConversions.{asScalaBuffer, asScalaSet, bufferAsJavaList}
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import com.aliasi.classify.{BaseClassifier, Classification, Classified, ConditionalClassification, DynamicLMClassifier, KnnClassifier, NaiveBayesClassifier, TfIdfClassifierTrainer}
import com.aliasi.corpus.ObjectHandler
import com.aliasi.matrix.CosineDistance
import com.aliasi.tokenizer.{LowerCaseTokenizerFactory, NGramTokenizerFactory, RegExTokenizerFactory, TokenFeatureExtractor}
import com.aliasi.util.{AbstractExternalizable, Compilable}

object Senseval3Demo extends App {

  val dictionaryFile = new File("data/senseval-en/EnglishLS.dictionary.xml")
  val trainingFile = new File("data/senseval-en/EnglishLS.train")
  val testFile = new File("data/senseval-en/EnglishLS.test")
  val responseFile = new File("Senseval3Demo.out")
  		
  val UnknownSense = "U"
  val ClassifierNumber = 1
  val NGramTokFactory = new NGramTokenizerFactory(4, 6)
  val SpaceTokFactory = new RegExTokenizerFactory("\\S+")
  val NormTokFactory = new LowerCaseTokenizerFactory(
      SpaceTokFactory)
  
  val dict = new SensevalDict(dictionaryFile)
  val trainingData = new TrainingData(trainingFile)
  val testData = new TestData(testFile)
  val output = new PrintWriter(new OutputStreamWriter(
    new FileOutputStream(responseFile)))
  for (cid <- 0 to 6) {
    val model = new SensevalModel(cid, dict, trainingData)
    for (i <- 0 until testData.wordPlusCats.size()) {
      val wordPlusCat = testData.wordPlusCats.get(i)
      val instanceId = testData.instanceIds.get(i)
      val textToClassify = testData.textsToClassify.get(i)
      val classifier = model.get(wordPlusCat)
      val classification = classifier.classify(textToClassify)
      output.print(wordPlusCat + " " + wordPlusCat + ".bnc." + instanceId)
      if (classification.isInstanceOf[ConditionalClassification]) {
        val condClassification = classification.asInstanceOf[ConditionalClassification]
        for (rank <- 0 until condClassification.size()) {
          val condProb = Math.round(1000.0 * 
            condClassification.conditionalProbability(rank)).toInt
          if (!(rank > 0 && condProb < 1)) {
            val category = condClassification.category(rank)
            output.print(" " + category + "/" + condProb)
          } 
        }
      } else {
        output.print(" " + classification.bestCategory())
      }
      output.println()
    }
    output.println("====")
  }
  output.flush()
  output.close()
  
  def extractAttribute(attr: String, line: String): String = {
    val start = line.indexOf(attr + "=") + attr.length() + 2
    val end = line.indexOf("\"", start)
    return line.substring(start, end)
  }

  def seek(lineStartStr: String, lines: Array[String], pos: Int): Int = {
    if (pos == -1) return -1
    else {
      var i = pos
      while (i < lines.length) {
        if (lines(i).startsWith(lineStartStr)) return i
        i += 1
      }
    }
    -1
  }

  def createClassifierTrainer(cid: Int, senseIds: Array[String]): 
      ObjectHandler[Classified[CharSequence]] = {
    cid match {
      case 0 => DynamicLMClassifier.createNGramProcess(senseIds, 5)
      case 1 => new NaiveBayesClassifier(senseIds, NormTokFactory)
      case 2 => DynamicLMClassifier.createTokenized(senseIds, NormTokFactory, 1)
      case 3 => DynamicLMClassifier.createTokenized(senseIds, NormTokFactory, 2)
      case 4 => {
        val featureExtractor = new TokenFeatureExtractor(SpaceTokFactory)
        new TfIdfClassifierTrainer[CharSequence](featureExtractor)
      }
      case 5 => {
        val featureExtractor = new TokenFeatureExtractor(SpaceTokFactory)
        new KnnClassifier[CharSequence](featureExtractor, 16)
      }
      case 6 => {
        val featureExtractor = new TokenFeatureExtractor(NGramTokFactory)
        new KnnClassifier[CharSequence](featureExtractor, 
          5, new CosineDistance(), true)
      }
    }
  }

  class SensevalDict(file: File) extends HashMap[String,Array[Sense]] {
  
    val input = Source.fromFile(file)
    var wordPlusCat = ""
    val senses = ArrayBuffer[Sense]()
    for (line <- input.getLines()) {
      if (line.startsWith("<lexelt")) {
        wordPlusCat = extractAttribute("item", line)
      } else if (line.startsWith("<sense")) {
        senses += new Sense(line)
      } else if (line.startsWith("</lexelt")) {
        put(wordPlusCat, senses.toArray)
        senses.clear()
      }
    }
  }

  class Sense(line: String) {
    val id = extractAttribute("id", line)
    val source = extractAttribute("source", line)
    val synset = extractAttribute("synset", line)
    val gloss = extractAttribute("gloss", line)
    
    override def toString(): String = {
      return "(" + id + "," + source + "," + 
        synset + "," + gloss + ")"
    }
  }
  
  class TrainingData(file: File) extends 
      HashMap[String,HashMap[String,ArrayList[String]]] {

    val source = Source.fromFile(file)
    var wordCat = ""
    var inContent = false
    var text = ""
    var idSet = new HashSet[String]()
    for (line <- source.getLines()) {
      if (line.startsWith("<lexelt")) {
        wordCat = extractAttribute("item", line)
      } else if (line.startsWith("<answer")) {
        idSet += extractAttribute("senseid", line)
      } else if (line.startsWith("<context")) {
        inContent = true
      } else if (inContent) {
        text = line
        inContent = false
      } else if (line.startsWith("</context")) {
        for (senseId <- idSet) {
          if (senseId != "U") {
            var senseToTextListMap = get(wordCat)
            if (senseToTextListMap == null) {
              senseToTextListMap = new HashMap[String,ArrayList[String]]()
              put(wordCat, senseToTextListMap) 
            }
            var trainingTextList = senseToTextListMap.get(senseId)
            if (trainingTextList == null) {
              trainingTextList = new ArrayList[String]()
              senseToTextListMap.put(senseId, trainingTextList)
            }
            trainingTextList.add(text)
            put(wordCat, senseToTextListMap)
          }
        }
        idSet.clear()
        text = ""
      } 
    }
  }
  
  class TestData(file: File) {

    val wordPlusCats = ArrayBuffer[String]()
    val instanceIds = ArrayBuffer[String]()
    val textsToClassify = ArrayBuffer[String]()
    val source = Source.fromFile(file)
    var inContent = false
    var text = ""
    for (line <- source.getLines()) {
      if (line.startsWith("<instance")) {
        val id = extractAttribute("id", line)
        val end = id.indexOf(".", id.indexOf(".") + 1)
        instanceIds += id
        wordPlusCats += id.substring(0, end)
      } else if (line.startsWith("<context")) {
        inContent = true
      } else if (inContent) {
        textsToClassify += line
        inContent = false
      }
    }
  }
  
  class SensevalModel(cid: Int, dict: SensevalDict, 
      trainingData: TrainingData) 
      extends HashMap[String,BaseClassifier[CharSequence]] {
    
    for (wordCat <- trainingData.keySet()) {
      val senseToTextList = trainingData.get(wordCat)
      val senseIds = senseToTextList.keySet().
        map(x => x.asInstanceOf[String]).toArray
      Console.println("    " + wordCat + " [" + senseIds.length + " senses]")
      val trainer = createClassifierTrainer(cid, senseIds)
      for (senseId <- senseToTextList.keySet()) {
        val classificationForSenseId = new Classification(senseId)
        val trainingTexts = senseToTextList.get(senseId)
        for (trainingText <- trainingTexts) {
          val classified = new Classified[CharSequence](
            trainingText, classificationForSenseId)
          trainer.handle(classified)
        }
      }
      val classifier = AbstractExternalizable.compile(
        trainer.asInstanceOf[Compilable])
      put(wordCat, classifier.asInstanceOf[BaseClassifier[CharSequence]])
    }
  }
}
