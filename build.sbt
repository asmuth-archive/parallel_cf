name := "ParallelCF: Example Recommender"

organization := "com.paulasmuth"

version := "0.1.0"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Compile <<= baseDirectory(_ / ".")

mainClass in (Compile, run) := Some("ExampleRecommender")

scalaVersion := "2.9.1"
