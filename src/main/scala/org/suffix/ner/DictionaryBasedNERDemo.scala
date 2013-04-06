package org.suffix.ner

import scala.collection.JavaConversions.asScalaSet

import com.aliasi.dict.{DictionaryEntry, ExactDictionaryChunker, MapDictionary}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

object DictionaryBasedNERDemo extends App {

  val inputs = Array[String](
    "50 Cent is hard to distinguish from 50 cent and just plain cent without case",
    "The product xyz120 DVD player won't match unless it's exact like XYZ120 DVD Player."
  )
  
  val Dict = new MapDictionary[String]()
  Dict.addEntry(new DictionaryEntry[String]("50 Cent", "PERSON", 1.0))
  Dict.addEntry(new DictionaryEntry[String]("XYZ120 DVD Player", "DB_ID_1232", 1.0))
  Dict.addEntry(new DictionaryEntry[String]("cent", "MONETARY_UNIT", 1.0))
  Dict.addEntry(new DictionaryEntry[String]("dvd player", "PRODUCT", 1.0))
  
  val TokFactory = IndoEuropeanTokenizerFactory.INSTANCE
  
  val dictChunkerTT = new ExactDictionaryChunker(Dict, TokFactory, true, true)
  val dictChunkerTF = new ExactDictionaryChunker(Dict, TokFactory, true, false)
  val dictChunkerFT = new ExactDictionaryChunker(Dict, TokFactory, false, true)
  val dictChunkerFF = new ExactDictionaryChunker(Dict, TokFactory, false, false)
  
  for (input <- inputs) {
    Console.println("TEXT=" + input)
    chunk(dictChunkerTT, input)
    chunk(dictChunkerTF, input)
    chunk(dictChunkerFT, input)
    chunk(dictChunkerFF, input)
  }
  
  def chunk(chunker: ExactDictionaryChunker, input: String): Unit = {
    Console.println("  All matches: " + chunker.returnAllMatches())
    Console.println("  Case sensitive: " + chunker.caseSensitive())
    val chunking = chunker.chunk(input)
    for (chunk <- chunking.chunkSet()) {
      val start = chunk.start()
      val end = chunk.end()
      val chunkType = chunk.`type`()
      val score = chunk.score()
      val phrase = input.substring(start, end)
      Console.println("    phrase=|" + phrase + "|, start=" + 
        start + " end=" + end + " type=" + chunkType + 
        " score=" + score)
    }
  }
}