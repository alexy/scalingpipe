package org.suffix.langmodel

import java.io.File
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.lm.TokenizedLM
import com.aliasi.lingmed.medline.parser.MedlineParser
import org.xml.sax.InputSource
import com.aliasi.lingmed.medline.parser.MedlineHandler
import com.aliasi.lingmed.medline.parser.MedlineCitation

object TokenLMDemo extends App {

  val MaxNGrams = 3
  
  ///////////////// train model ///////////////////////
  val inputFile = new File("data/medline/medsamp2010.xml")
  val factory = IndoEuropeanTokenizerFactory.INSTANCE
  val lm = new TokenizedLM(factory, MaxNGrams)
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

  //////////////////// test model ///////////////////////
  val testSentences = Array[String](
    "Hearing loss is being partly or totally unable to hear sound in one or both ears.",
    "A hearing is a proceeding before a court or other decision-making body or officer, such as a government agency."    
  )
  testSentences.foreach(sentence => {
    Console.println(sentence + " (" + lm.log2Estimate(sentence) + ")")
  })
}