package scorekeeper

import akka.http.scaladsl.model.DateTime
import scorekeeper.models._
import slick.jdbc.H2Profile.api._
import java.sql.Timestamp

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.Source
import scala.language.postfixOps


object DBSchema {

  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.clicks),
    ts => DateTime(ts.getTime)
  )

  class PlayersTable(tag: Tag) extends Table[Player](tag, "PLAYERS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = (id, name).mapTo[Player]
  }
  val Players = TableQuery[PlayersTable]

  class TeamsTable(tag: Tag) extends Table[Team](tag, "TEAMS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = (id, name).mapTo[Team]
  }
  val Teams = TableQuery[TeamsTable]

  class GamesTable(tag: Tag) extends Table[Game](tag, "GAMES"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = (id, name).mapTo[Game]
  }
  val Games = TableQuery[GamesTable]

  class PlayerTeamsTable(tag: Tag) extends Table[PlayerTeam](tag, "PLAYERTEAMS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def playerId = column[Int]("PLAYER_ID")
    def teamId = column[Int]("TEAM_ID")

    def * = (id, playerId, teamId).mapTo[PlayerTeam]
    def playerFK = foreignKey("ptt_player_FK", playerId, Players)(_.id)
    def teamFK = foreignKey("ptt_team_FK", teamId, Teams)(_.id)
  }
  val PlayerTeams = TableQuery[PlayerTeamsTable]

  class MatchesTable(tag: Tag) extends Table[Match](tag, "MATCHES"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def homeTeamId = column[Int]("HOME_TEAM_ID")
    def awayTeamId = column[Int]("AWAY_TEAM_ID")
    def homeScore = column[Int]("HOME_SCORE")
    def awayScore = column[Int]("AWAY_SCORE")
    def gameId = column[Int]("GAME_ID")
    def playedAt = column[DateTime]("PLAYED_AT")

    def * = (id, homeTeamId, awayTeamId, homeScore, awayScore, gameId, playedAt).mapTo[Match]
    def homeTeamFK = foreignKey("matches_team1_FK", homeTeamId, Teams)(_.id)
    def awayTeamFK = foreignKey("matches_team2_FK", awayTeamId, Teams)(_.id)
    def gameFK = foreignKey("matches_game_FK", gameId, Games)(_.id)
  }
  val Matches = TableQuery[MatchesTable]


  val databaseSetup = DBIO.seq(
    Players.schema.create, Teams.schema.create, Games.schema.create, PlayerTeams.schema.create, Matches.schema.create,

    Players forceInsertAll readAllPlayers(),
    Teams forceInsertAll readAllTeams(),
    Games forceInsertAll readAllGames(),
    PlayerTeams forceInsertAll readAllPlayerTeams(),
    Matches forceInsertAll readAllMatches(),
  )

  private def readAllPlayers() = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("model/players.csv")).getLines().map(_.split(",")).map{x⇒ Player(x(0).toInt, x(1))}.toSeq
  private def readAllTeams() = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("model/teams.csv")).getLines().map(_.split(",")).map{x⇒ Team(x(0).toInt, x(1))}.toSeq
  private def readAllGames() = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("model/games.csv")).getLines().map(_.split(",")).map{x⇒ Game(x(0).toInt, x(1))}.toSeq
  private def readAllPlayerTeams() = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("model/player_teams.csv")).getLines().map(_.split(",")).zipWithIndex.map{x⇒ PlayerTeam(x._2, x._1(1).toInt, x._1(0).toInt)}.toSeq
  private def readAllMatches() = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("model/matches.csv")).getLines().map(_.split(",")).zipWithIndex.map{x⇒ Match(x._2, x._1(2).toInt, x._1(3).toInt, x._1(4).toInt, x._1(5).toInt, x._1(0).toInt, DateTime(x._1(1).toLong))}.toSeq

  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}


