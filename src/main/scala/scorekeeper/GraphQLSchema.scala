package scorekeeper

import akka.http.scaladsl.model.DateTime
import scorekeeper.models._
import sangria.ast.StringValue
import sangria.execution.deferred.{ DeferredResolver, Fetcher, Relation, RelationIds }
import sangria.macros.derive._
import sangria.schema.{ Argument, Field, IntType, InterfaceType, ListInputType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields }

object GraphQLSchema {

  implicit val GraphQLDateTime: ScalarType[DateTime] = ScalarType[DateTime](
    "DateTime",
    coerceOutput = (dt, _) => dt.toString,
    coerceInput = {
      case StringValue(dt, _, _, _, _) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
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


  val PlayerType: ObjectType[Unit, Player] = ObjectType[Unit, Player](
    "Player",
    fields[Unit, Player](
      Field("id", IntType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name)
    )
  )

  // 1. We kunnen gebruik maken van macros om de ObjectTypes uit te schrijven.
  //    Vervang de PlayerType de macro zoals hieronder uitgecommentarieerd staat:
  //
  //  lazy val PlayerType: ObjectType[Unit, Player] = deriveObjectType[Unit, Player](
  //    Interfaces(IdentifiableType)
  //  )
  //
  // 2. We kunnen op het moment 3 verschillende uitvragingen doen: allPlayers, player(id) en players(ids)
  //    Check dat dit voor je werkt (sbt clean run om de server te draaien en ga naar http://localhost:9000/graphiql
  //
  // 3. Voeg deze operaties ook toe voor games, teams en matches en check dat het werkt
  //    Gebruik bij match deze regel in de macro om ervoor te zorgen dat datums goed omgezet worden:
  //    ReplaceField("playedAt", Field("playedAt", GraphQLDateTime, resolve = _.value.playedAt)),
  //
  // 4. Al deze entiteiten hebben nog geen relaties. Laten we de benodigde relaties toevoegen.
  //    Gebruik ReplaceField voor een nieuw game veld in plaats van gameId:
  //    ReplaceField("gameId",
  //     Field("game", GameType, resolve = c => gamesFetcher.defer(c.value.gameId))
  //    )
  //
  //    Doe dit ook voor het thuis team en het uit team in Match.
  //
  //    Doe dit ook voor Team en Player in PlayerTeam.
  //
  //
  //    Gelaagde queries zou je nu moeten kunnen uitvragen vanaf match, dus bijvoorbeeld:
  //    query {
  //      allMatches {
  //        homeTeam{
  //          name
  //        }
  //        awayTeam {
  //          name
  //        }
  //        homeScore
  //        awayScore
  //        playedAt
  //      }
  //    }
  //
  // 5. Een team bestaat uit een aantal spelers. Vooralsnog hebben we alleen de relaties naar 1 enkele entiteit aangepast. Om deze relatie op te zetten
  //    moeten we een veld toevoegen aan het TeamType:
  //    AddFields(
  //      Field("playerTeams", ListType(PlayerTeamType), resolve = c => playerTeamsFetcher.deferRelSeq(playerTeamByTeamRel, c.value.id))
  //    )
  //
  //    Ook moeten we een relatie definieren:
  //    val playerTeamByTeamRel: Relation[PlayerTeam, PlayerTeam, Int] = Relation[PlayerTeam, Int]("byTeam", l => Seq(l.teamId))
  //
  //    En we moeten de fetcher laten weten hoe hier mee om kan gaan:
  //    (ctx: MyContext, ids: RelationIds[PlayerTeam]) => ctx.dao.getPlayerTeamsByTeamId(ids)
  //
  //  6. We kunnen nu alleen nog maar spelers toe voegen (controleer dit in graphiQL!)
  //     We willen echter ook de operaties createGame, createTeam, createMatch en addPlayerToTeam toevoegen.
  //     Doe dit, de methoden zijn al aangemaakt in de DAO.


  val playersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPlayers(ids)
  )

  val Resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(playersFetcher)

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
      )
    )
  )

  val SchemaDefinition = Schema(QueryType, Some(Mutation))

}
