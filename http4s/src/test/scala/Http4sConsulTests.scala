package helm
package http4s

import cats.~>
import cats.data.Kleisli
import cats.effect.IO
import fs2.{Chunk, Stream}
import org.http4s.{EntityBody, Request, Response, Status, Uri}
import org.http4s.client._
import org.scalatest._
import org.scalactic.TypeCheckedTripleEquals

class Http4sConsulTests extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  import Http4sConsulTests._

  "get" should "succeed with some when the response is 200" in {
    val response = consulResponse(Status.Ok, "yay")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.kvGet("foo")).attempt.unsafeRunSync should ===(
      Right(Some("yay")))
  }

  "get" should "succeed with none when the response is 404" in {
    val response = consulResponse(Status.NotFound, "nope")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.kvGet("foo")).attempt.unsafeRunSync should ===(
      Right(None))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "boo")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.kvGet("foo")).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "set" should "succeed when the response is 200" in {
    val response = consulResponse(Status.Ok, "yay")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.kvSet("foo", "bar")).attempt.unsafeRunSync should ===(
      Right(()))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "boo")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.kvSet("foo", "bar")).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "healthListChecksForNode" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, serviceHealthChecksReplyJson)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksForNode("localhost", None)).attempt.unsafeRunSync should ===(
      Right(healthStatusReplyJson))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "doesn't actually matter since this part is ignored")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksForNode("localhost", None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "healthListChecksInState" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, serviceHealthChecksReplyJson)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksInState(HealthStatus.Passing, None, None, None)).attempt.unsafeRunSync should ===(
      Right(healthStatusReplyJson))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "doesn't actually matter since this part is ignored")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksInState(HealthStatus.Passing, None, None, None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "healthListChecksForService" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, serviceHealthChecksReplyJson)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksForService("test", None, None, None)).attempt.unsafeRunSync should ===(
      Right(healthStatusReplyJson))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "doesn't actually matter since this part is ignored")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListChecksForService("test", None, None, None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "healthListNodesForService" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, healthNodesForServiceReplyJson)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListNodesForService("test", None, None, None, None, None)).attempt.unsafeRunSync should ===(
      Right(healthNodesForServiceReturnValue))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "aww yeah")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.healthListNodesForService("test", None, None, None, None, None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "agentRegisterService" should "succeed when the response is 200" in {
    val response = consulResponse(Status.Ok, "yay")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentRegisterService("testService", Some("testId"), None, None, None, None, None, None)).attempt.unsafeRunSync should ===(
      Right(()))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "boo")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentRegisterService("testService", Some("testId"), None, None, None, None, None, None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "agentDeregisterService" should "succeed when the response is 200" in {
    val response = consulResponse(Status.Ok, "")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentDeregisterService("testService")).attempt.unsafeRunSync should ===(
      Right(()))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentDeregisterService("testService")).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "agentEnableMaintenanceMode" should "succeed when the response is 200" in {
    val response = consulResponse(Status.Ok, "")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentEnableMaintenanceMode("testService", true, None)).attempt.unsafeRunSync should ===(
      Right(()))
  }

  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentEnableMaintenanceMode("testService", true, None)).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }

  "agentListServices" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, dummyServicesReply)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentListServices).attempt.unsafeRunSync should ===(
      Right(
        Map(
          "consul" -> ServiceResponse("consul", "consul", List.empty, "", 8300, false, 1L, 2L),
          "test"   -> ServiceResponse("testService", "test", List("testTag", "anotherTag"), "127.0.0.1", 1234, false, 123455121300L, 123455121321L)
        )
      ))
  }

  "catalogservice" should "succeed with the proper result when the response is 200" in {
    val response = consulResponse(Status.Ok, catalogServicesReplyJson)
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.catalogService("test")).attempt.unsafeRunSync should ===(
      Right(catalogServiceReturnValue))
  }



  it should "fail when the response is 500" in {
    val response = consulResponse(Status.InternalServerError, "boo")
    val csl = constantConsul(response)
    helm.run(csl, ConsulOp.agentListServices).attempt.unsafeRunSync should ===(
      Left(UnexpectedStatus(Status.InternalServerError)))
  }
}

object Http4sConsulTests {
  def constantConsul(response: Response[IO]): ConsulOp ~> IO = {
    new Http4sConsulClient(
      Uri.uri("http://localhost:8500/v1/kv/v1"),
      constantResponseClient(response),
      None)
  }

  def consulResponse(status: Status, s: String): Response[IO] = {
    val responseBody = body(s)
    Response(status = status, body = responseBody)
  }

  def constantResponseClient(response: Response[IO]): Client[IO] = {
    val dispResponse = DisposableResponse(response, IO.unit)
    Client(Kleisli{req => IO.pure(dispResponse)}, IO.unit)
  }

  def body(s: String): EntityBody[IO] =
    Stream.chunk(Chunk.bytes(s.getBytes("UTF-8"))) // YOLO

  val dummyRequest: Request[IO] = Request[IO]()

  val dummyServicesReply = """
  {
      "consul": {
          "Address": "",
          "CreateIndex": 1,
          "EnableTagOverride": false,
          "ID": "consul",
          "ModifyIndex": 2,
          "Port": 8300,
          "Service": "consul",
          "Tags": []
      },
      "test": {
          "Address": "127.0.0.1",
          "CreateIndex": 123455121300,
          "EnableTagOverride": false,
          "ID": "test",
          "ModifyIndex": 123455121321,
          "Port": 1234,
          "Service": "testService",
          "Tags": [
              "testTag",
              "anotherTag"
          ]
      }
  }
  """

  val serviceHealthChecksReplyJson = """
  [
      {
          "CheckID": "service:testService",
          "CreateIndex": 19008,
          "ModifyIndex": 19013,
          "Name": "Service 'testService' check",
          "Node": "localhost",
          "Notes": "test note",
          "Output": "HTTP GET https://test.test.test/: 200 OK Output: all's well",
          "ServiceID": "testServiceID",
          "ServiceName": "testServiceName",
          "ServiceTags": ["testTag"],
          "Status": "passing"
      },
      {
          "CheckID": "service:testService#2",
          "CreateIndex": 123455121300,
          "ModifyIndex": 123455121321,
          "Name": "other check",
          "Node": "localhost",
          "Notes": "a note",
          "Output": "Get https://test.test.test/: dial tcp 192.168.1.71:443: getsockopt: connection refused",
          "ServiceID": "testServiceID",
          "ServiceName": "testServiceName",
          "ServiceTags": ["testTag", "anotherTag"],
          "Status": "critical"
      }
  ]
  """


  val healthNodesForServiceReplyJson = """
  [
      {
          "Checks": [
              {
                  "CheckID": "service:testService",
                  "CreateIndex": 19008,
                  "ModifyIndex": 19013,
                  "Name": "Service 'testService' check",
                  "Node": "localhost",
                  "Notes": "test note",
                  "Output": "HTTP GET https://test.test.test/: 200 OK Output: all's well",
                  "ServiceID": "testServiceID",
                  "ServiceName": "testServiceName",
                  "ServiceTags": ["testTag"],
                  "Status": "passing"
              },
              {
                  "CheckID": "service:testService#2",
                  "CreateIndex": 123455121300,
                  "ModifyIndex": 123455121321,
                  "Name": "other check",
                  "Node": "localhost",
                  "Notes": "a note",
                  "Output": "Get https://test.test.test/: dial tcp 192.168.1.71:443: getsockopt: connection refused",
                  "ServiceID": "testServiceID",
                  "ServiceName": "testServiceName",
                  "ServiceTags": ["testTag", "anotherTag"],
                  "Status": "critical"
              }
          ],
          "Node": {
              "Address": "192.168.1.145",
              "CreateIndex": 123455121311,
              "Datacenter": "dc1",
              "ID": "cb1f6030-a220-4f92-57dc-7baaabdc3823",
              "Meta": {
                "metaTest": "test123"
              },
              "ModifyIndex": 123455121347,
              "Node": "localhost",
              "TaggedAddresses": {
                  "lan": "192.168.1.145",
                  "wan": "192.168.2.145"
              }
          },
          "Service": {
              "Address": "127.0.0.1",
              "CreateIndex": 123455121301,
              "EnableTagOverride": false,
              "ID": "test",
              "ModifyIndex": 123455121322,
              "Port": 1234,
              "Service": "testService",
              "Tags": [
                  "testTag",
                  "anotherTag"
              ]
          }
      }
  ]
  """

  val healthNodesForServiceReturnValue =
    List(
      HealthNodesForServiceResponse(
        NodeResponse(
          "cb1f6030-a220-4f92-57dc-7baaabdc3823",
          "localhost",
          "192.168.1.145",
          "dc1",
          Map("metaTest" -> "test123"),
          TaggedAddresses("192.168.1.145", "192.168.2.145"),
          123455121311L,
          123455121347L
        ),
        ServiceResponse(
          "testService",
          "test",
          List("testTag",
          "anotherTag"),
          "127.0.0.1",
          1234,
          false,
          123455121301L,
          123455121322L
        ),
        List(
          HealthCheckResponse(
            "localhost",
            "service:testService",
            "Service 'testService' check",
            HealthStatus.Passing,
            "test note",
            "HTTP GET https://test.test.test/: 200 OK Output: all's well",
            "testServiceID",
            "testServiceName",
            List("testTag"),
            19008L,
            19013L),
          HealthCheckResponse(
            "localhost",
            "service:testService#2",
            "other check",
            HealthStatus.Critical,
            "a note",
            "Get https://test.test.test/: dial tcp 192.168.1.71:443: getsockopt: connection refused",
            "testServiceID",
            "testServiceName",
            List("testTag", "anotherTag"),
            123455121300L,
            123455121321L)
        )
      )
    )

  val healthStatusReplyJson =
    List(
      HealthCheckResponse(
        "localhost",
        "service:testService",
        "Service 'testService' check",
        HealthStatus.Passing,
        "test note",
        "HTTP GET https://test.test.test/: 200 OK Output: all's well",
        "testServiceID",
        "testServiceName",
        List("testTag"),
        19008L,
        19013L),
      HealthCheckResponse(
        "localhost",
        "service:testService#2",
        "other check",
        HealthStatus.Critical,
        "a note",
        "Get https://test.test.test/: dial tcp 192.168.1.71:443: getsockopt: connection refused",
        "testServiceID",
        "testServiceName",
        List("testTag", "anotherTag"),
        123455121300L,
        123455121321L)
    )

  val catalogServicesReplyJson = """[
  {
    "ID": "test",
    "Node": "test.node",
    "Address": "192.168.1.145",
    "Datacenter": "test.datacenter",
    "TaggedAddresses": {
      "lan": "192.168.1.145",
      "wan": "192.168.1.145"
    },
    "NodeMeta": {
      "consul-network-segment": ""
    },
    "ServiceID": "test.serviceid",
    "ServiceName": "test.servicename",
    "ServiceTags": ["test.tag"],
    "ServiceAddress": "test.serviceaddress",
    "ServicePort": 1234,
    "ServiceEnableTagOverride": false,
    "CreateIndex": 123456789,
    "ModifyIndex": 123456789
  }
]"""

val catalogServiceReturnValue =
    List(
      CatalogServiceResponse(
        "test",
        "test.node",
        "192.168.1.145",
        "test.datacenter",
        TaggedAddresses("192.168.1.145", "192.168.1.145"),
        Map("consul-network-segment" -> ""),
        "test.serviceid",
        "test.servicename",
        List("test.tag"),
        "test.serviceaddress",
        1234,
        false,
        123456789,
        123456789
      )
    )

}
