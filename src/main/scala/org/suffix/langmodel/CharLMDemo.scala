package org.suffix.langmodel

import java.io.File

import org.xml.sax.InputSource

import com.aliasi.lingmed.medline.parser.{MedlineCitation, MedlineHandler, MedlineParser}
import com.aliasi.lm.NGramProcessLM

object CharLMDemo extends App {

  val MaxNGram = 8
  val NumChars = 256

  ///////////////// train model ///////////////////////
  val inputFile = new File("data/medline/medsamp2010.xml")
  val lm = new NGramProcessLM(MaxNGram, NumChars)
  val parser = new MedlineParser(false)
  parser.setHandler(new MedlineHandler() {
    
    override def delete(id: String): Unit = { /* NOOP */ }
    
    override def handle(citation: MedlineCitation): Unit = {
      val text = citation.article().abstrct().text()
      lm.train(text.toCharArray(), 0, text.length())
    }
  })
  val url = inputFile.toURI().toURL().toString()
  parser.parse(new InputSource(url))
  
  /////////////////// count top ngrams /////////////////
  val counter = lm.substringCounter()
  val uniqueTotals = counter.uniqueTotalNGramCount()
  Console.println("N-GRAM COUNTS")
  Console.println("N, #Unique, #Total, %")
  for (i <- 0 until uniqueTotals.length) {
    val uniq = uniqueTotals(i)(0)
    val tot = uniqueTotals(i)(1)
    val avg = 1.0 - (uniq.asInstanceOf[Double] / tot.asInstanceOf[Double])
    Console.println(i + ", " + uniq + ", " + tot + ", " + avg)
  }
  
  /////////////////// print top ngrams /////////////////
  Console.println("Top N-GRAMS")
  Console.println("N, (N-GRAM, Count)*")
  for (i <- 1 until MaxNGram) {
    val buf = new StringBuilder()
    buf.append(i).append(", ")
    val topNGrams = counter.topNGrams(i, 5)
    topNGrams.keysOrderedByCount().foreach(key => {
      val ngram = key.toString()
      val count = topNGrams.getCount(ngram)
      buf.append("(\"").append(ngram).append("\", ").
        append(count).append("), ")
    })
    Console.println(buf.toString())
  }
  
  ///////////////////// test model /////////////////////
  val testSentences = Array[String](
    "Hearing loss is being partly or totally unable to hear sound in one or both ears.",
    "A hearing is a proceeding before a court or other decision-making body or officer, such as a government agency."    
  )
  testSentences.foreach(sentence => {
    Console.println(sentence + " (" + lm.log2Prob(sentence) + ")")
  })
}
