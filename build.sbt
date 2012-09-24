name := "ParallelCF: Example Recommender"

organization := "com.paulasmuth"

version := "0.0.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Compile <<= baseDirectory(_ / ".")

mainClass in (Compile, run) := Some("ExampleRecommender")

scalaVersion := "2.9.1"
