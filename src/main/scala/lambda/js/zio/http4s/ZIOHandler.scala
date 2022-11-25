package lambda.js.zio.http4s

import fs2.{Stream, text}
import net.exoego.facade.aws_lambda.{APIGatewayProxyEvent, APIGatewayProxyResult, AsyncAPIGatewayProxyHandler}
import org.http4s.Header.ToRaw.rawToRaw
import org.http4s.{EmptyBody, Header, Headers, HttpRoutes, Method, ParseFailure, Request, Response, Uri}
import org.typelevel.ci.CIString
import zio.{Runtime, Task, Unsafe, ZIO}
import zio.interop.catz._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.JSConverters._

abstract class ZIOHandler {
  def service: HttpRoutes[Task]

  val handler: AsyncAPIGatewayProxyHandler = (event, _) => {
    val json = js.JSON.stringify(event)
    js.Dynamic.global.console.log(json)

    implicit val ec = ExecutionContext.global
    handle(event).toJSPromise
  }

  def handle(event: APIGatewayProxyEvent)(implicit ec: ExecutionContext): Future[APIGatewayProxyResult] = {
    Unsafe.unsafe { implicit unsafe =>
      implicit val runtime: Runtime[Any] = zio.Runtime.default
      runtime.unsafe.runToFuture(parseRequest(event)
        .map(run).fold(parseFailure => ZIO.succeed(APIGatewayProxyResult(
          statusCode = 500,
          body = parseFailure.message
        )), identity)
      )
    }
  }

  private def run(request: Request[Task])(implicit runtime: Runtime[Any]): Task[APIGatewayProxyResult] = {
    service.run(request)
      .getOrElse(Response.notFound)
      .flatMap((response: Response[Task]) => response.as[String].map { body =>
        APIGatewayProxyResult(
          statusCode = response.status.code,
          headers = Option(response.headers.headers.map(header => (header.name.toString, header.value)).toMap.toJSDictionary.asInstanceOf[js.Dictionary[Boolean | Double | String]]).orUndefined,
          body = body
        )
      })
  }

  private def parseRequest(request: APIGatewayProxyEvent): Either[ParseFailure, Request[Task]] = {
    val headers: Seq[Header.ToRaw] = Option(request.headers.toMap).getOrElse(Map.empty).map {
      case (k, v) => Header.Raw(CIString(k), v)
    }.map(rawToRaw).toSeq
    for {
      method <- Method.fromString(Option(request.httpMethod).getOrElse("GET"))
      uri <- Uri.fromString(Option(request.path).getOrElse("/") + queryParamsToPath(Option(request.queryStringParameters).map(_.asInstanceOf[js.Dictionary[String]]).map(_.toMap).getOrElse(Map.empty)))
    } yield Request[Task](
      method = method,
      uri = uri,
      headers = Headers(headers: _*),
      body = Option(request.body).map(_.asInstanceOf[String]).map(body => Stream(body).through(text.utf8.encode))
        .getOrElse(EmptyBody)
    )
  }

  private def queryParamsToPath(params: Map[String, String]): String = {
    val path = params.map {
      case (k, v) => s"$k=$v"
    }.mkString("&")
    if (path.isEmpty) "" else "?" + path
  }
}
