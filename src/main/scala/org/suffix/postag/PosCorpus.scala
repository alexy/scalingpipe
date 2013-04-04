package org.suffix.postag

import com.aliasi.corpus.Parser
import com.aliasi.corpus.ObjectHandler
import com.aliasi.tag.Tagging
import org.xml.sax.InputSource
import collection.JavaConversions._
import java.io.File
import com.aliasi.io.FileExtensionFilter
import com.aliasi.corpus.StringParser
import scala.collection.mutable.ArrayBuffer
import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import com.aliasi.util.Strings
import com.aliasi.util.Iterators

trait PosCorpus {
  
  def parser(): Parser[ObjectHandler[Tagging[String]]]

  def sourceIterator(): Iterator[InputSource]
}

///////////////////////// Brown ////////////////////////

class BrownPosCorpus(brownZip: File) extends PosCorpus {

  override def parser(): Parser[ObjectHandler[Tagging[String]]] = {
    new BrownPosParser()
  }
  
  override def sourceIterator(): Iterator[InputSource] = {
    val inputSources = ArrayBuffer[InputSource]()
    val zistream = new ZipInputStream(new FileInputStream(brownZip))
    try {
      var buffer = new Array[Byte](4 * 1024)
      var zipentry: ZipEntry = null
      do {
        zipentry = zistream.getNextEntry()
        if (zipentry != null && (! zipentry.isDirectory())) {
          val name = zipentry.getName()
          if (!(name.equals("brown/CONTENTS") || 
              name.equals("brown/README"))) {
            val bos = new ByteArrayOutputStream()
            var bytesRead = -1
            do {
              bytesRead = zistream.read(buffer, 0, buffer.length)
              if (bytesRead > -1) {
                bos.write(buffer, 0, bytesRead)
              }
            } while (bytesRead > -1)
            val bis = new ByteArrayInputStream(bos.toByteArray())
            inputSources += new InputSource(bis)
          }
        }
      } while (zipentry != null)
    } catch {
      case ex: Exception => {
        ex.printStackTrace()
      }
    }
    inputSources.toIterator
  }
}

class BrownPosParser extends StringParser[ObjectHandler[Tagging[String]]] {
  
  override def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
    val input = new String(cs, start, end - start)
    val sentences = input.split("\n")
    for (sentence <- sentences) {
      if (! Strings.allWhitespace(sentence)) {
        processSentence(sentence)
      }
    }
  }
  
  def processSentence(sentence: String): Unit = {
    val words = sentence.split(" ")
    val tokenList = ArrayBuffer[String]()
    val tagList = ArrayBuffer[String]()
    for (word <- words) {
      val slashPos = word.lastIndexOf('/')
      if (slashPos > -1) {
        tokenList += word.substring(0, slashPos)
        tagList += normalizeTag(word.substring(slashPos + 1))
      }
    }
    val tagging = new Tagging[String](tokenList, tagList)
    getHandler().handle(tagging)
  }
  
  def normalizeTag(rawTag: String): String = {
    var tag = rawTag
    val startTag = rawTag
    // last plus
    val lastPlus = tag.lastIndexOf('+')
    if (lastPlus >= 0) tag = tag.substring(0, lastPlus)
    // last hyphen
    val lastHyphen = tag.lastIndexOf('-')
    if (lastHyphen >= 0) {
      val suffix = tag.substring(lastHyphen + 1)
      if (suffix.equalsIgnoreCase("HL") || 
          suffix.equalsIgnoreCase("TL") || 
          suffix.equalsIgnoreCase("NC"))
        tag = tag.substring(0, lastHyphen)
    }
    // first hyphen
    val firstHyphen = tag.indexOf('-')
    if (firstHyphen > 0) {
      val prefix = tag.substring(0, firstHyphen)
      if (prefix.equalsIgnoreCase("FW") || 
          prefix.equalsIgnoreCase("NC") || 
          prefix.equalsIgnoreCase("NP"))
        tag = tag.substring(firstHyphen + 1)
    }
    // neg last, and only if not whole thing
    val negIndex = tag.indexOf('*')
    if (negIndex > 0) {
      if (negIndex == tag.length() - 1) 
        tag = tag.substring(0, negIndex)
      else
        tag = tag.substring(0, negIndex) + 
          tag.substring(negIndex + 1)
    }
    // run recursively till convergence
    if (tag.equals(startTag)) tag else normalizeTag(tag)
  }
}

///////////////////////// GENIA ////////////////////////

class GeniaPosCorpus(geniaPosFile: File) extends PosCorpus {
  
  override def parser(): Parser[ObjectHandler[Tagging[String]]] = {
    new GeniaPosParser()
  }
  
  override def sourceIterator(): Iterator[InputSource] = {
    Iterators.singleton(new InputSource(new FileInputStream(geniaPosFile)))
  }
}

class GeniaPosParser extends StringParser[ObjectHandler[Tagging[String]]] {

  def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
    val tokenList = ArrayBuffer[String]()
    val tagList = ArrayBuffer[String]()
    val input = new String(cs, start, end - start)
    val lines = input.split("\n")
    for (line <- lines) {
      if (line.startsWith("===================="))
        handle(tokenList, tagList)
      else {
        val slashPos = line.lastIndexOf('/')
        if (slashPos > -1) {
          tokenList += line.substring(0, slashPos)
          val tag = line.substring(slashPos + 1)
          val pipePos = tag.indexOf('|')
          if (pipePos > -1) {
            tagList += tag.substring(0, pipePos)
          } else {
            tagList += tag
          }
        }
      }
    }
    handle(tokenList, tagList)
  }
  
  def handle(tokenList: ArrayBuffer[String], 
      tagList: ArrayBuffer[String]): Unit = {
    val tagging = new Tagging[String](tokenList, tagList)
    getHandler().handle(tagging)
  }
}

///////////////////////// MedPost //////////////////////

class MedPostPosCorpus(medPostDir: File) extends PosCorpus {
  
  override def parser(): Parser[ObjectHandler[Tagging[String]]] = {
    return new MedPostPosParser()
  }
  
  override def sourceIterator(): Iterator[InputSource] = {
    medPostDir.listFiles(new FileExtensionFilter("ioc")).map(
      file => new InputSource(file.toURI().toURL().toString())).
      toIterator
  }
}

class MedPostPosParser 
    extends StringParser[ObjectHandler[Tagging[String]]] {
  
  override def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
    val input = new String(cs, start, end - start)
    val sentences = input.split("\n")
    for (sentence <- sentences) {
      val tokenList = ArrayBuffer[String]()
      val tagList = ArrayBuffer[String]()
      if ((! sentence.isEmpty) && (sentence.indexOf('_') >= 0)) {
        val words = sentence.split(" ")
        for (word <- words) {
          val pair = word.split("_")
          tokenList += pair(0)
          tagList += pair(1)
        }
        val tagging = new Tagging[String](tokenList, tagList)
        getHandler().handle(tagging)
      }
    }
  }
}
