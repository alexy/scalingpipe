package org.suffix.strcmp

import com.aliasi.spell.JaroWinklerDistance

object JaroWinklerDemo extends App {
  
  val inputs = Array(
      "MARTHA|MARHTA",
      "JONES|JOHNSON",
      "DUNNINGHAM|CUNNINGHAM",
      "MICHELLE|MICHAEL",
      "NICHLESON|NICHULSON",
      "MASSEY|MASSIE",
      "ABROMS|ABRAMS",
      "HARDIN|MARTINEZ",
      "ITMAN|SMITH",
      "JERALDINE|GERALDINE",
      "JULIES|JULIUS",
      "TANYA|TONYA",
      "DWAYNE|DUANE",
      "SEAN|SUSAN",
      "JON|JOHN",
      "JON|JAN",
      "MARTHA|MARHTA",
      "ABCDFGHe|WeXYZMNOPQRSTU",
      "ABCDFGHe|WeXYZMNOPQRS",
      "ABCDFGHIe|WeXYZMNOPQRS",
      "ijABCDE|jiUVWXYZ",
      "iABCDEF|TiUVWXYZ",
      "iABCDE|iUVWXYZ")

  val jaroWinkler = JaroWinklerDistance.JARO_WINKLER_DISTANCE
  Console.println("\n%18s  %18s  %5s  %5s\n".format(
    "String1", "String2", "Dist", "Prox"))
  for (s <- inputs) {
    val pair = s.split("\\|")
    val s1 = pair(0)
    val s2 = pair(1)
    Console.println("%18s  %18s  %5.3f  %5.3f\n".format(
      s1, s2, jaroWinkler.distance(s1, s2), 
      jaroWinkler.proximity(s1, s2)))
  }
}