package scorekeeper

import akka.http.scaladsl.model.DateTime
import sangria.execution.deferred.HasId
import sangria.validation.Violation

package object models {
  object Identifiable {
    implicit def hasId[T <: Identifiable]: HasId[T, Int] = HasId(_.id)
  }

  trait Identifiable {
    val id: Int
  }

  case object DateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing DateTime"
  }

  case class Player(id: Int, name: String) extends Identifiable

  case class Team(id: Int, name: String) extends Identifiable

  case class Game(id: Int, name: String) extends Identifiable

  case class PlayerTeam(id: Int, playerId: Int, teamId: Int) extends Identifiable

  case class Match(id: Int, homeTeamId: Int, awayTeamId: Int, homeScore: Int, awayScore: Int, gameId: Int, playedAt: DateTime = DateTime.now) extends Identifiable
}
