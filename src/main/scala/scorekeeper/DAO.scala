package scorekeeper

import DBSchema._
import scorekeeper.models._
import sangria.execution.deferred.{ RelationIds, SimpleRelation }
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future
import scala.language.postfixOps

class DAO(db: Database) {

  def allPlayers: Future[Seq[Player]] = db.run(Players.result)

  def allTeams: Future[Seq[Team]] = db.run(Teams.result)

  def allGames: Future[Seq[Game]] = db.run(Games.result)

  def allMatches: Future[Seq[Match]] = db.run(Matches.result)

  def getPlayers(ids: Seq[Int]): Future[Seq[Player]] = db.run(
    Players.filter(_.id inSet ids).result
  )

  def getTeams(ids: Seq[Int]): Future[Seq[Team]] = db.run(
    Teams.filter(_.id inSet ids).result
  )

  def getGames(ids: Seq[Int]): Future[Seq[Game]] = db.run(
    Games.filter(_.id inSet ids).result
  )

  def getMatches(ids: Seq[Int]): Future[Seq[Match]] = db.run(
    Matches.filter(_.id inSet ids).result
  )

  def getPlayerTeams(ids: Seq[Int]): Future[Seq[PlayerTeam]] = db.run(
    PlayerTeams.filter(_.id inSet ids).result
  )

  def getPlayerTeamsByTeamId(rel: RelationIds[PlayerTeam]): Future[Seq[PlayerTeam]] =
    db.run(
      PlayerTeams.filter{ playerTeam ⇒
        rel.rawIds.collect({
          case (SimpleRelation("byTeam"), ids:Seq[Int]) ⇒ playerTeam.teamId inSet ids
        }).foldLeft(true: Rep[Boolean])(_ || _)
      } result
    )

  def createPlayer(name: String): Future[Player] = {
    val newPlayer = Player(0, name)

    val insertAndReturnPlayerQuery = (Players returning Players.map(_.id)) into {
      (player, id) => player.copy(id = id)
    }

    db.run {
      insertAndReturnPlayerQuery += newPlayer
    }
  }

  def createGame(name: String): Future[Game] = {
    val newGame = Game(0, name)

    val insertAndReturnGameQuery = (Games returning Games.map(_.id)) into {
      (game, id) => game.copy(id = id)
    }

    db.run {
      insertAndReturnGameQuery += newGame
    }
  }

  def createTeam(name: String): Future[Team] = {
    val newTeam = Team(0, name)

    val insertAndReturnTeamQuery = (Teams returning Teams.map(_.id)) into {
      (team, id) => team.copy(id = id)
    }

    db.run {
      insertAndReturnTeamQuery += newTeam
    }
  }

  def addPlayerToTeam(teamId: Int, playerId: Int): Future[PlayerTeam] = {
    val newPlayerTeam = PlayerTeam(0, playerId, teamId)

    val insertAndReturnPlayerTeamQuery = (PlayerTeams returning PlayerTeams.map(_.id)) into {
      (playerTeam, id) => playerTeam.copy(id = id)
    }

    db.run {
      insertAndReturnPlayerTeamQuery += newPlayerTeam
    }
  }

  def createMatch(homeTeamId: Int, awayTeamId: Int, homeScore: Int, awayScore: Int, gameId: Int): Future[Match] = {
    val newMatch = Match(0, homeTeamId, awayTeamId, homeScore, awayScore, gameId)

    val insertAndReturnMatchQuery = (Matches returning Matches.map(_.id)) into {
      (match_, id) => match_.copy(id = id)
    }

    db.run {
      insertAndReturnMatchQuery += newMatch
    }
  }

}
