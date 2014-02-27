package org.suffix.ner

import com.aliasi.chunk.RegExChunker
import collection.JavaConversions._

object EmailRegexNERDemo extends App {

  val inputs = Array[String](
    "John's email is john@his.company.com and his friend's is foo.bar@123.foo.ca."
  )
  val EmailRegex = "[A-Za-z0-9](([_\\.\\-]?[a-zA-Z0-9]+)*)@([A-Za-z0-9]+)(([\\.\\-]?[a-zA-Z0-9]+)*)\\.([A-Za-z]{2,})"
  val ChunkType = "email"
  val ChunkScore = 0.0
  
  val chunker = new EmailRegexChunker(EmailRegex, ChunkType, ChunkScore)
  for (input <- inputs) {
    val chunking = chunker.chunk(input)
    Console.println("input=" + input)
    Console.println("chunking=" + chunking)
    for (chunk <- chunking.chunkSet()) {
      val start = chunk.start()
      val end = chunk.end()
      Console.println("  chunk=(" + start + "," + 
        end + "): " + input.substring(start, end))
    }
  } 
}

class EmailRegexChunker(
    regex: String, chunkType: String, chunkScore: Double) 
    extends RegExChunker(regex, chunkType, chunkScore) {
}