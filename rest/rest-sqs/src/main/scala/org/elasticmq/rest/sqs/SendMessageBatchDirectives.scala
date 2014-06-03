package org.elasticmq.rest.sqs

import Constants._
import org.elasticmq.rest.sqs.directives.ElasticMQDirectives

trait SendMessageBatchDirectives { this: ElasticMQDirectives with SendMessageDirectives with BatchRequestsModule =>
  val SendMessageBatchPrefix = "SendMessageBatchRequestEntry"

  val sendMessageBatch = {
    action("SendMessageBatch") {
      queueActorFromRequest { queueActor =>
        anyParamsMap { parameters =>
          verifyMessagesNotTooLong(parameters)

          val resultsFuture = batchRequest(SendMessageBatchPrefix, parameters) { (messageData, id) =>
            doSendMessage(queueActor, messageData).map { case (message, digest, messageAttributeDigest) =>
              <SendMessageBatchResultEntry>
                <Id>{id}</Id>
                <MD5OfMessageAttributes>{messageAttributeDigest}</MD5OfMessageAttributes> // TODO: Only send if message attributes included
                <MD5OfMessageBody>{digest}</MD5OfMessageBody>
                <MessageId>{message.id.id}</MessageId>
              </SendMessageBatchResultEntry>
            }
          }

          resultsFuture.map { results =>
            respondWith {
              <SendMessageBatchResponse>
                <SendMessageBatchResult>
                  {results}
                </SendMessageBatchResult>
                <ResponseMetadata>
                  <RequestId>{EmptyRequestId}</RequestId>
                </ResponseMetadata>
              </SendMessageBatchResponse>
            }
          }
        }
      }
    }
  }

  def verifyMessagesNotTooLong(parameters: Map[String, String]) {
    val messageLengths = for {
      parameterMap <-batchParametersMap(SendMessageBatchPrefix, parameters)
    } yield {
      parameterMap(MessageBodyParameter).length
    }

    verifyMessageNotTooLong(messageLengths.sum)
  }
}
