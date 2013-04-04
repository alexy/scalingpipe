package org.suffix.strcmp

import com.aliasi.spell.FixedWeightEditDistance

object FixedEditDistanceDemo extends App {

  val inputs = Array("hte", "htne", "thhe", 
      "the", "then", "The", "THE")

  val matchWeight: Double = 0
  val deleteWeight: Double = -10
  val insertWeight: Double = -8
  val substituteWeight: Double = -9
  val transposeWeight: Double = -9
  
  Console.println(
    "match=%4.1f  del=%4.1f  ins=%4.1f  subst=%4.1f  trans=%4.1f\n".
    format(matchWeight, deleteWeight, insertWeight, 
    substituteWeight, transposeWeight))
  val fixedEd = new FixedWeightEditDistance(
    matchWeight, deleteWeight, insertWeight, 
    substituteWeight, transposeWeight)
  for (s1 <- inputs) {
    for (s2 <- inputs) {
      Console.println("%12s  %12s  %5.1f\n".format(
        s1, s2, fixedEd.distance(s1, s2)))
    }
  }
}