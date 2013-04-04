package org.suffix.strcmp

import com.aliasi.spell.WeightedEditDistance

object WeightedEditDistanceDemo extends App {

  val inputs = Array("the", "The", "THE", "T H E", "hte", "then")

  val dist = new CasePunctuationDistance()
  for (s1 <- inputs) {
    for (s2 <- inputs) {
      Console.println("%12s  %12s  %5.1f\n".format(
        s1, s2, dist.distance(s1, s2)))
    }
  }
}

class CasePunctuationDistance() extends WeightedEditDistance {
  
  def deleteWeight(c: Char): Double = {
    if (c.isLetter || c.isDigit) -1 else 0
  }
  
  def insertWeight(c: Char): Double = {
    deleteWeight(c)
  }
  
  def substituteWeight(cDel: Char, cIns: Char): Double = {
    if (cDel.toLower == cIns.toLower) 0 else -1
  }
  
  def matchWeight(c: Char): Double = {
    0
  }
  
  def transposeWeight(cFirst: Char, cSecond: Char): Double = {
    return Double.NegativeInfinity
  }
}