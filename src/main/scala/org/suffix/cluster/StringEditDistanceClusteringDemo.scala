package org.suffix.cluster

import scala.collection.JavaConversions.setAsJavaSet

import com.aliasi.cluster.{CompleteLinkClusterer, SingleLinkClusterer}
import com.aliasi.spell.EditDistance

object StringEditDistanceClusteringDemo extends App {

  val editDistance = new EditDistance(false)
  val inputSet = Set[String]("a", "aaa", "aaaaaa", "aaaaaaaaaa")
  val maxDistance = 10
  for (s1 <- inputSet; s2 <- inputSet) {
    if (s1.compareTo(s2) < 0)
      Console.println("Distance(" + s1 + "," + s2 + ")=" + 
        editDistance.distance(s1, s2))
  }
  val slClusterer = 
    new SingleLinkClusterer[String](maxDistance, editDistance)
  val clClusterer =
    new CompleteLinkClusterer[String](maxDistance, editDistance)
    
  val slDendogram =
    slClusterer.hierarchicalCluster(inputSet)
  Console.println("Single Link Dendogram")
  Console.println(slDendogram.prettyPrint())
  val clDendogram =
    clClusterer.hierarchicalCluster(inputSet)
  Console.println("Complete Link Dendogram")
  Console.println(clDendogram.prettyPrint())
  
  // dendograms to clustering
  Console.println("Single Link Clusterings")
  for (i <- 1 to slDendogram.size()) {
    val slKClustering = slDendogram.partitionK(i)
    Console.println(i + "  " + slKClustering)
  }
  Console.println("Complete Link Clusterings")
  for (i <- 1 to clDendogram.size()) {
    val clKClustering = clDendogram.partitionK(i)
    Console.println(i + "  " + clKClustering)
  }
  val slClustering = slClusterer.cluster(inputSet)
  Console.println("Single Link Clustering\n" + slClustering)
  val clClustering = clClusterer.cluster(inputSet)
  Console.println("Complete Link Clustering\n" + clClustering)
}