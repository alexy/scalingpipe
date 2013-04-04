package org.suffix.postag

import java.io.File
import com.aliasi.util.FastCache
import com.aliasi.util.AbstractExternalizable
import com.aliasi.hmm.HiddenMarkovModel
import com.aliasi.hmm.HmmDecoder
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import collection.JavaConversions._
import com.aliasi.chunk.Chunking
import com.aliasi.tokenizer.TokenizerFactory
import com.aliasi.chunk.Chunker
import com.aliasi.util.Strings
import scala.collection.mutable.ArrayBuffer
import com.aliasi.chunk.ChunkingImpl
import com.aliasi.chunk.ChunkFactory

object PhraseChunkingDemo extends App {

  val inputs = Array[String](
    "'s not",
    "The not very tall man ' will not run '",
    "Not unheard of for peregrine falcons to live in urban settings.",
    "After months of coy hints, Prime Minister Tony Blair made the announcement today as part of a closely choreographed and protracted farewell.",
    "The attorney general appeared before the House Judiciary Committee to discuss the dismissals of U.S. attorneys.",
    "Nascar's most popular driver announced that his future would not include racing for Dale Earnhardt Inc.",
    "Purdue Pharma, its parent company, and three of its top executives today admitted to understating the risks of addiction to the painkiller.",
    "After a difficult stretch for the airline, David Neeleman will give way to David Barger, the No. 2 executive.")

  val modelFile = new File("models/pos-en-general-brown.HiddenMarkovModel")
  val cache = new FastCache[String,Array[Double]](50000)
  
  val posHmm = AbstractExternalizable.readObject(modelFile).
    asInstanceOf[HiddenMarkovModel]
  val posTagger = new HmmDecoder(posHmm, null, cache)
  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val chunker = new PhraseChunker(posTagger, tokenizerFactory)
  for (input <- inputs) {
    Console.println("SENTENCE: " + input)
    val tokens = tokenizerFactory.tokenizer(
      input.toCharArray(), 0, input.length()).tokenize()
    val tagging = posTagger.tag(tokens.toList)
    Console.println("TAGGED: " + tokens.zip(tagging.tags()).
      map(x => x._1 + "/" + x._2).
      foldLeft("")(_ + " " + _))
    val chunking: Chunking = chunker.chunk(input)
    val chunkText = chunking.charSequence()
    for (chunk <- chunking.chunkSet()) {
      val start = chunk.start()
      val end = chunk.end()
      val chunkType = chunk.`type`
      Console.println("  " + chunkType + "(" + start + "," + 
        end + "): " + chunkText.subSequence(start, end))
    }
  }
}

class PhraseChunker(posTagger: HmmDecoder, 
    tokenizerFactory: TokenizerFactory) extends Chunker {
  
  val PunctuationTags = Set[String]("'", ".", "*")
  val StartNounTags = Set[String](
    // determiner tags
    "abn", "abx", "ap", "ap$", "at", "cd", "cd$", "dt", "dt$",
    "dti", "dts", "dtx", "od",
    // adjective tags
    "jj", "jj$", "jjr", "jjs", "jjt", "*", "ql",
    // noun tags
    "nn", "nn$", "nns", "nns$", "np", "np$", "nps", "nps$",
    "nr", "nr$", "nrs",
    // pronoun tags
    "pn", "pn$", "pp$", "pp$$", "ppl", "ppls", "ppo", "pps", "ppss")
  val ContinueNounTags = Set[String](
    // adverb tags  
    "rb", "rb$", "rbr", "rbt", "rn", "ql", "*") ++ 
    StartNounTags ++ PunctuationTags
  val StartVerbTags = Set[String](
    // verb tags
    "vb", "vbd", "vbg", "vbn", "vbz",
    // auxilliary verb tags
    "to", "md", "be", "bed", "bedz", "beg", "bem", "ben", "ber", "bez",
    // adverb tags
    "rb", "rb$", "rbr", "rbt", "rn", "ql", "*"
  )
  val ContinueVerbTags = PunctuationTags ++ StartVerbTags
  
  override def chunk(cseq: CharSequence): Chunking = {
    val cs = Strings.toCharArray(cseq)
    return chunk(cs, 0, cs.length)
  }
  
  override def chunk(cs: Array[Char], start: Int, end: Int): Chunking = {
    val tokenList = ArrayBuffer[String]()
    val whiteList = ArrayBuffer[String]()
    val tokenizer = tokenizerFactory.tokenizer(cs, start, end - start)
    tokenizer.tokenize(tokenList, whiteList)
    val tagging = posTagger.tag(tokenList)
    val chunking = new ChunkingImpl(cs, start, end)
    var startChunk = 0
    var endChunk = 0
    var trimmedEndChunk = 0
    var i = 0
    while (i < tagging.size()) {
      startChunk += whiteList(i).size
      if (StartNounTags.contains(tagging.tag(i))) {
        endChunk = startChunk + tokenList(i).size
        i += 1
        while (i < tokenList.size() && 
            ContinueNounTags.contains(tagging.tag(i))) {
          endChunk += whiteList(i).size + tokenList(i).size
          i += 1
        }
        trimmedEndChunk = endChunk
        var k = i - 1
        while (k >= 0 && PunctuationTags.contains(tagging.tag(k))) {
          trimmedEndChunk -= whiteList(k).size + tokenList(k).size
          k -= 1
        }
        if (startChunk >= trimmedEndChunk) {
          startChunk = endChunk
        } else {
          val chunk = ChunkFactory.createChunk(startChunk, trimmedEndChunk, "noun")
          chunking.add(chunk)
          startChunk = endChunk
        }
      } else if (StartVerbTags.contains(tagging.tag(i))) {
        endChunk = startChunk + tokenList(i).size
        i += 1
        while (i < tokenList.size && 
            ContinueVerbTags.contains(tagging.tag(i))) {
          endChunk += whiteList(i).size + tokenList(i).size
          i += 1
        }
        trimmedEndChunk = endChunk
        var k = i - 1
        while (k >= 0 && PunctuationTags.contains(tagging.tag(k))) {
          trimmedEndChunk -= whiteList(k).size + tokenList(k).size
          k -= 1
        }
        if (startChunk >= trimmedEndChunk) {
          startChunk = endChunk
        } else {
          val chunk = ChunkFactory.createChunk(startChunk, trimmedEndChunk, "verb")
          chunking.add(chunk)
          startChunk = endChunk
        }
      } else {
        startChunk += tokenList(i).size
        i += 1
      }
    }
    return chunking
  }
}