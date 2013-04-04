package org.suffix.strcmp

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.spell.JaccardDistance

object JaccardDistanceDemo extends App {

  val inputs = Array(
      "p53, also known as protein 53 (TP53), is a transcription factor that regulates the cell cycle and hence functions as a tumor suppressor.",
      "It is important in multicellular organisms as it helps to suppress cancer.",
      "The name p53 is in reference to its apparent molecular mass: it runs as a 53-kilodalton (kDa) protein on SDS-PAGE.",
      "The human gene that encodes for p53 is TP53.",
      "The gene is named TP53 after the protein it codes for (TP53 is another name for p53).",
      "The gene is located on the human chromosome 17 (17p13.1).",
      "Mutations that deactivate p53 in cancer usually occur in the DBD.")

  val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
  val jaccard = new JaccardDistance(tokenizerFactory)
  
  for (s1 <- inputs) {
    for (s2 <- inputs) {
      Console.println("String1=" + s1)
      Console.println("String2=" + s2)
      Console.println("distance=%4.2f  proximity=%4.2f\n".format(
        jaccard.distance(s1, s2), jaccard.proximity(s1, s2)))
    }
  }
}