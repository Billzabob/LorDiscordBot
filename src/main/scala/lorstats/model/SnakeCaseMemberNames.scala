package lorstats.model

import io.circe.generic.extras.Configuration

trait SnakeCaseMemberNames {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}
