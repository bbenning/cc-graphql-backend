package scorekeeper

import akka.http.scaladsl.model.DateTime
import scorekeeper.models._
import sangria.ast.StringValue
import sangria.execution.deferred.{ DeferredResolver, Fetcher, Relation, RelationIds }
import sangria.macros.derive._
import sangria.schema.{ Argument, Field, IntType, InterfaceType, ListInputType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields }

object GraphQLSchema {

  implicit val GraphQLDateTime = ScalarType[DateTime](
    "DateTime",
    coerceOutput = (dt, _) => dt.toString,
    coerceInput = {
      case StringValue(dt, _, _, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = {
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  lazy val PlayerType: ObjectType[Unit, Player] = deriveObjectType[Unit, Player](
    Interfaces(IdentifiableType)
  )

  lazy val TeamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](
    Interfaces(IdentifiableType),
    AddFields(
      Field("playerTeams", ListType(PlayerTeamType), resolve = c => playerTeamsFetcher.deferRelSeq(playerTeamByTeamRel, c.value.id))
    )
  )

  lazy val GameType: ObjectType[Unit, Game] = deriveObjectType[Unit, Game](
    Interfaces(IdentifiableType)
  )

  lazy val PlayerTeamType: ObjectType[Unit, PlayerTeam] = deriveObjectType[Unit, PlayerTeam](
    Interfaces(IdentifiableType),
    ReplaceField("playerId",
      Field("player", PlayerType, resolve = c => playersFetcher.defer(c.value.playerId))
    ),
    ReplaceField("teamId",
      Field("team", TeamType, resolve = c => teamsFetcher.defer(c.value.teamId))
    )
  )

  lazy val MatchType: ObjectType[Unit, Match] = deriveObjectType[Unit, Match](
    Interfaces(IdentifiableType),
    ReplaceField("playedAt", Field("playedAt", GraphQLDateTime, resolve = _.value.playedAt)),
    ReplaceField("gameId",
      Field("game", GameType, resolve = c => gamesFetcher.defer(c.value.gameId))
    ),
    ReplaceField("homeTeamId",
      Field("homeTeam", TeamType, resolve = c => teamsFetcher.defer(c.value.homeTeamId))
    ),
    ReplaceField("awayTeamId",
      Field("awayTeam", TeamType, resolve = c => teamsFetcher.defer(c.value.awayTeamId))
    )
  )

  val playerTeamByTeamRel: Relation[PlayerTeam, PlayerTeam, Int] = Relation[PlayerTeam, Int]("byTeam", l => Seq(l.teamId))

  val playersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPlayers(ids)
  )

  val teamsFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getTeams(ids),

  )

  val gamesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getGames(ids)
  )

  val playerTeamsFetcher: Fetcher[MyContext, PlayerTeam, PlayerTeam, Int] = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPlayerTeams(ids),
    (ctx: MyContext, ids: RelationIds[PlayerTeam]) => ctx.dao.getPlayerTeamsByTeamId(ids)
  )

  val matchesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getMatches(ids)
  )

  val Resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(playersFetcher, teamsFetcher, gamesFetcher, playerTeamsFetcher, matchesFetcher)

    val Id = Argument("id", IntType)
    val Ids =  Argument("ids", ListInputType(IntType))

    val QueryType = ObjectType(
      "Query",
      fields[MyContext, Unit](
        Field("allPlayers", ListType(PlayerType), resolve = c => c.ctx.dao.allPlayers),
        Field("player",
          OptionType(PlayerType),
          arguments = Id :: Nil,
          resolve = c => playersFetcher.deferOpt(c.arg(Id))
        ),
        Field("players",
          ListType(PlayerType),
          arguments = Ids :: Nil,
          resolve = c => playersFetcher.deferSeq(c.arg(Ids))
        ),
        Field("allTeams", ListType(TeamType), resolve = c => c.ctx.dao.allTeams),
        Field("team",
          OptionType(TeamType),
          arguments = Id :: Nil,
          resolve = c => teamsFetcher.deferOpt(c.arg(Id))
        ),
        Field("teams",
          ListType(TeamType),
          arguments = Ids :: Nil,
          resolve = c => teamsFetcher.deferSeq(c.arg(Ids))
        ),
        Field("allGames", ListType(GameType), resolve = c => c.ctx.dao.allGames),
        Field("game",
          OptionType(GameType),
          arguments = Id :: Nil,
          resolve = c => gamesFetcher.deferOpt(c.arg(Id))
        ),
        Field("games",
          ListType(GameType),
          arguments = Ids :: Nil,
          resolve = c => gamesFetcher.deferSeq(c.arg(Ids))
        ),
        Field("allMatches", ListType(MatchType), resolve = c => c.ctx.dao.allMatches),
        Field("match",
          OptionType(MatchType),
          arguments = Id :: Nil,
          resolve = c => matchesFetcher.deferOpt(c.arg(Id))
        ),
        Field("matches",
          ListType(MatchType),
          arguments = Ids :: Nil,
          resolve = c => matchesFetcher.deferSeq(c.arg(Ids))
        )
      )
    )

  val NameArg = Argument("name", StringType)
  val PlayerIdArg = Argument("playerId", IntType)
  val TeamIdArg = Argument("teamId", IntType)
  val HomeTeamIdArg = Argument("homeTeamId", IntType)
  val AwayTeamIdArg = Argument("awayTeamId", IntType)
  val HomeScoreArg = Argument("homeScore", IntType)
  val AwayScoreArg = Argument("awayScore", IntType)
  val GameIdArg = Argument("gameId", IntType)

  val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("createPlayer",
        PlayerType,
        arguments = NameArg :: Nil,
        resolve = c => c.ctx.dao.createPlayer(c.arg(NameArg))
      ),
      Field("createGame",
        GameType,
        arguments = NameArg :: Nil,
        resolve = c => c.ctx.dao.createGame(c.arg(NameArg))
      ),
      Field("createTeam",
        TeamType,
        arguments = NameArg :: Nil,
        resolve = c => c.ctx.dao.createTeam(c.arg(NameArg))
      ),
      Field("addPlayerToTeam",
        PlayerTeamType,
        arguments = PlayerIdArg :: TeamIdArg :: Nil,
        resolve = c => c.ctx.dao.addPlayerToTeam(c.arg(TeamIdArg), c.arg(PlayerIdArg))
      ),
      Field("createMatch",
        MatchType,
        arguments = HomeTeamIdArg :: AwayTeamIdArg :: HomeScoreArg :: AwayScoreArg :: GameIdArg :: Nil,
        resolve = c => c.ctx.dao.createMatch(c.arg(HomeTeamIdArg), c.arg(AwayTeamIdArg), c.arg(HomeScoreArg), c.arg(AwayScoreArg), c.arg(GameIdArg))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType, Some(Mutation))

}
