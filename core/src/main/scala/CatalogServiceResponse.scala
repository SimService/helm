package helm

import argonaut._, Argonaut._

/** Case class representing a service as returned from an API call to Consul */
final case class CatalogServiceResponse(
  id:                       String,
  node:                     String,
  address:                  String,
  datacenter:               String,
  taggedAddresses:          TaggedAddresses,
  nodeMeta:                 Map[String, String],
  serviceId:                String,
  serviceName:              String,
  serviceTags:              List[String],
  serviceAddress:           String,
  servicePort:              Int,
  serviceEnableTagOverride: Boolean,
  createIndex:              Long,
  modifyIndex:              Long
)

object CatalogServiceResponse {
  implicit def CatalogServiceResponseDecoder: DecodeJson[CatalogServiceResponse] =
    DecodeJson(j =>
      for {
        id                        <- (j --\ "ID").as[String]
        node                      <- (j --\ "Node").as[String]
        address                   <- (j --\ "Address").as[String]
        datacenter                <- (j --\ "Datacenter").as[String]
        taggedAddresses           <- (j --\ "TaggedAddresses").as[TaggedAddresses]
        nodeMeta                  <- (j --\ "NodeMeta").as[Map[String, String]]
        serviceId                 <- (j --\ "ServiceID").as[String]
        serviceName               <- (j --\ "ServiceName").as[String]
        serviceTags               <- (j --\ "ServiceTags").as[List[String]]
        serviceAddress            <- (j --\ "ServiceAddress").as[String]
        servicePort               <- (j --\ "ServicePort").as[Int]
        serviceEnableTagOverride  <- (j --\ "ServiceEnableTagOverride").as[Boolean]
        createIndex               <- (j --\ "CreateIndex").as[Long]
        modifyIndex               <- (j --\ "ModifyIndex").as[Long]
      } yield CatalogServiceResponse(id, node, address, datacenter, taggedAddresses, nodeMeta, serviceId, serviceName, serviceTags, serviceAddress, servicePort, serviceEnableTagOverride, createIndex, modifyIndex)
    )
}
