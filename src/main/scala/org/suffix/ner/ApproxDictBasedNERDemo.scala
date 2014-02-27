package org.suffix.ner

import scala.collection.JavaConversions.asScalaSet

import com.aliasi.dict.{ApproxDictionaryChunker, DictionaryEntry, TrieDictionary}
import com.aliasi.spell.FixedWeightEditDistance
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

object ApproxDictBasedNERDemo extends App {

  val inputs = Array[String](
    "A protein called Mdm2 binds to p53 and transports it from the nucleus to the cytosol.",
    "p53, also known as protein 53 (TP53), functions as a tumor supressor."
  )
  
  val Dict = new TrieDictionary[String]()
  Dict.addEntry(new DictionaryEntry[String]("P53", "P53"))
  Dict.addEntry(new DictionaryEntry[String]("protein 53", "P53"))
  Dict.addEntry(new DictionaryEntry[String]("Mdm", "Mdm"))
  
  val TokFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN)
  val maxDistance = 2.0
  
  val chunker = new ApproxDictionaryChunker(
    Dict, TokFactory, editDistance, maxDistance)
  for (input <- inputs) {
    Console.println("TEXT: " + input)
    val chunking = chunker.chunk(input)
    Console.println("%15s  %15s   %8s".
      format("Matched Phrase", "Dict Entry", "Distance"))
    for (chunk <- chunking.chunkSet()) {
      val start = chunk.start()
      val end = chunk.end()
      val dist = chunk.score()
      val chunkType = chunk.`type`()
      val phrase = input.substring(start, end)
      Console.println("%15s  %15s   %8.1f".
        format(phrase, chunkType, dist))
    }
  }
}