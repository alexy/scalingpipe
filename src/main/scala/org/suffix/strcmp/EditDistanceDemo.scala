package org.suffix.strcmp

import com.aliasi.spell.EditDistance

object EditDistanceDemo extends App {

  val inputs = Array("hte", "htne", "thhe", 
      "the", "then", "The", "THE")

  val d1 = new EditDistance(false)
  val d2 = new EditDistance(true)
  
  Console.println("\n%12s  %12s  %5s  %5s   %5s  %5s\n".format(
    "String1", "String2",
    "Dist1", "Dist2",
    "Prox1", "Prox2"))
  for (s1 <- inputs) {
    for (s2 <- inputs) {
      Console.println(
        "%12s  %12s  %5.1f  %5.1f   %5.1f  %5.1f\n".format(
        s1, s2, 
        d1.distance(s1, s2), d2.distance(s1, s2), 
        d1.proximity(s1, s2), d2.proximity(s1, s2)))
    }
  }
}