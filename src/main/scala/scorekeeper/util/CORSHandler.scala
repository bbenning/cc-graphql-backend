package scorekeeper.util

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive0, RejectionHandler, Route }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._


trait CORSHandler{
  lazy val allowedOrigin = {
    `Access-Control-Allow-Origin`.*
  }

  // this rejection handler adds access control headers to Authentication Required response
  implicit val unauthRejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthenticationFailedRejection(_, challenge) =>
        complete(
          HttpResponse(401)
            .withHeaders(allowedOrigin, `Access-Control-Allow-Credentials`(true), `WWW-Authenticate`(challenge))
            .withEntity("Authentication missing")
        )
    }
    .result()

  // this directive adds access control headers to normal responses
  private def addAccessControlHeaders: Directive0 = {
    mapResponseHeaders { headers =>
      allowedOrigin +:
        `Access-Control-Allow-Credentials`(true) +:
        headers
    }
  }

  // this handles preflight OPTIONS requests. TODO: see if can be done with rejection handler,
  // otherwise has to be under addAccessControlHeaders
  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    ))
  }

  def corsHandler(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }
}
