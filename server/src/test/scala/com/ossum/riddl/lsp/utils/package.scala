package com.ossum.riddl.lsp

import java.io.File
import java.nio.file.{Files, Path}
import scala.io.Source
import scala.language.postfixOps

package object utils {
  def createTempFile(
      fileToCopy: String
  ): Path = {
    val (prefix, suffix) = fileToCopy.splitAt(fileToCopy.lastIndexOf('.'))
    val copyPath = Files.createTempFile(prefix, suffix)

    val p = new java.io.PrintWriter(new File(copyPath.toString))
    val source = Source.fromFile("server/src/test/resources/" ++ fileToCopy)
    val contents =
      try source.mkString
      finally source.close()
    try {
      p.println(contents)
    } finally {
      p.close()
    }
    copyPath
  }

  def resetTempFile(tempPath: Path, fileToCopy: String): Path = {
    Files.delete(tempPath)
    createTempFile(fileToCopy)
  }
}
