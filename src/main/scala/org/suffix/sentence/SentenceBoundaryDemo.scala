package org.suffix.sentence

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.sentences.MedlineSentenceModel
import scala.io.Source
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object SentenceBoundaryDemo extends App {
  
  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val sentenceModel = new MedlineSentenceModel()

  val source = Source.fromFile("data/sentence_demo.txt")
  val text = source.mkString
  source.close()
  Console.println("INPUT TEXT: \n" + text)
  
  val tokenList = ArrayBuffer[String]()
  val whiteList = ArrayBuffer[String]()
  val tokenizer = tokenizerFactory.tokenizer(
    text.toCharArray, 0, text.length)
  tokenizer.tokenize(tokenList, whiteList)
  Console.println(tokenList.length + " TOKENS")
  Console.println(whiteList.length + " WHITES")
  
  val sentenceBoundaries = sentenceModel.boundaryIndices(
    tokenList.toArray, whiteList.toArray)
  Console.println(sentenceBoundaries.length + 
    " SENTENCE END TOKEN OFFSETS")
  
  if (sentenceBoundaries.length < 1) {
    Console.println("No sentence boundaries found")
  } else {
    var start = 0
    var end = 0
    for (i <- 0 until sentenceBoundaries.length) {
      end = sentenceBoundaries(i)
      Console.println("SENTENCE " + (i+1) + ": ")
      for (j <- start until end) {
        Console.print(tokenList(j) + whiteList(j+1))
      }
      Console.println()
      start = end + 1
    }
  }
}
