package com.ossuminc.riddl.lsp.utils

object implicits {
  implicit class TextProcessing(text: String) {
    def getLinesFromText: Seq[String] = text
      .replaceAll("(?<=\\S)(?= {2,})", "\n")
      .split("\\n")
      .toSeq
  }
}
