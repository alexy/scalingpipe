package org.suffix.sentence

import scala.io.Source

import com.aliasi.sentences.{MedlineSentenceModel, SentenceChunker}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

object SentenceChunkerDemo extends App {

  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val sentenceModel = new MedlineSentenceModel()
  val sentenceChunker = new SentenceChunker(
    tokenizerFactory, sentenceModel)
  
  val source = Source.fromFile("data/sentence_demo.txt")
  val text = source.mkString
  source.close()
  Console.println("INPUT TEXT: \n" + text)
  
  val chunking = sentenceChunker.chunk(text.toCharArray, 0, text.length)
  val sentences = chunking.chunkSet()
  if (sentences.size() < 1)
    Console.println("No sentence chunks found.")
  else {
    val slice = chunking.charSequence().toString()
    var i = 0
    val it = sentences.iterator()
    while (it.hasNext()) {
      val sentence = it.next()
      val start = sentence.start()
      val end = sentence.end()
      Console.println("SENTENCE " + i + ": ")
      Console.println(slice.substring(start, end))
      i = i + 1
    }
  }
}
