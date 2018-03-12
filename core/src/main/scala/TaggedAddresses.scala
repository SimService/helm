package helm

import argonaut._, Argonaut._

final case class TaggedAddresses(lan: String, wan: String)

object TaggedAddresses {
  implicit def TaggedAddressesDecoder: DecodeJson[TaggedAddresses] =
    DecodeJson(j =>
      for {
        lan <- (j --\ "lan").as[String]
        wan <- (j --\ "wan").as[String]
      } yield TaggedAddresses(lan, wan)
    )
}
