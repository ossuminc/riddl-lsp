package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.DocContentMgmt

import org.eclipse.lsp4j.{CompletionList, CompletionParams}

import java.util.concurrent.CompletableFuture

/*
- completion
- completionItemResolve
- hover
- signatureHelp
 */

object CompletionAndHover {
  def completion(position: CompletionParams): CompletableFuture[
    messages.Either[util.List[CompletionItem], CompletionList]
  ] = checkMessagesInASTAndFailOrDo[
    messages.Either[util.List[CompletionItem], CompletionList]
  ](
    position.getTextDocument.getUri,
    msgs => {
      val completionsOpt = getCompletionFromAST(
        msgs,
        position.getPosition.getLine,
        position.getPosition.getCharacter
      )
      if completionsOpt.isDefined then
        Future
          .successful(
            completionsOpt.getOrElse(messages.Either.forLeft(Seq().asJava))
          )
      else
        Future.failed(
          new Throwable(
            "Requested position in document does not have an error"
          )
        )
    }
  )

  private def getCompletionFromAST(
      msgs: List[Messages.Message],
      line: Int,
      charPosition: Int
  ): Option[messages.Either[util.List[CompletionItem], CompletionList]] = {
    var completions = CompletionList()
    val completionList = new CompletionList()
    val grouped = msgs.groupBy(msg => msg.loc.line)
    val lineItems =
      if grouped.isDefinedAt(line) then Some(grouped(line)) else None
    val selectedItem: Seq[Messages.Message] =
      lineItems
        .map { items =>
          val filtered = items.filter(_.loc.col == charPosition)
          if filtered.nonEmpty then filtered else Seq()
        }
        .getOrElse(Seq())

    if selectedItem.nonEmpty then {
      completionList.setItems(
        selectedItem
          .map(msgToCompletion)
          .asJava
      )
      Some(
        messages.Either.forRight(
          completionList
        )
      )
    } else None

  }

  private def msgToCompletion(msg: Messages.Message): CompletionItem = {
    val item = new CompletionItem()
    item.setDetail(msg.message)
    item
  }
}
