package com.teammood.memoryleak

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import org.joda.time.DateTime
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class MongoDbRepository(mongoUri: String, actorSystem: ActorSystem) {

  private val parsedURI = MongoConnection.parseURI(mongoUri).get

  private lazy val driver = new MongoDriver
  private lazy val connection: MongoConnection = {

    val hosts = parsedURI.hosts.map(h ⇒ h._1 + ":" + h._2)

    println("Mongo Connection on " + hosts.mkString(", "))

    driver.connection(nodes = hosts, authentications = Seq(parsedURI.authenticate.get), options = MongoConnectionOptions(authMode = ScramSha1Authentication, sslEnabled = true, connectTimeoutMS = 10000))
  }

  private def db: Future[DefaultDB] = connection.database(parsedURI.db.get, failoverStrategy())

  implicit def materializer: akka.stream.Materializer = akka.stream.ActorMaterializer.create(actorSystem)

  def allTeamMember(active: Boolean): Source[TeamMember, _] = {

    def teamMembersBSONCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("teamMembers"))

    import reactivemongo.bson._

    implicit val passwordInfoReader: BSONDocumentReader[PasswordInfo] = Macros.reader[PasswordInfo]
    implicit val scheduleConfigurationReader: BSONDocumentReader[ScheduleConfiguration] = Macros.reader[ScheduleConfiguration]
    implicit val tagReader: BSONDocumentReader[Tag] = Macros.reader[Tag]

    implicit object TeamMemberReader extends BSONDocumentReader[TeamMember] {
      def read(bson: BSONDocument): TeamMember = {
        val optionalTeamMember: Option[TeamMember] = for {
          id ← bson.getAs[String]("id")
          providerId ← bson.getAs[String]("providerId")
          email ← bson.getAs[String]("email")
          firstname ← bson.getAs[String]("firstname")
          lastname ← bson.getAs[String]("lastname")
          created ← bson.getAs[Long]("created")
          active ← bson.getAs[Boolean]("active")
          scheduleConfiguration ← bson.getAs[ScheduleConfiguration]("scheduleConfiguration")
        } yield {

          val holidaysTo = bson.getAs[Long]("holidaysTo")

          val holidays: Option[DateTime] = holidaysTo.map(date ⇒ new DateTime(date).withTimeAtStartOfDay())

          val roles: Seq[Role] = bson.getAs[Seq[String]]("roles").map(roles ⇒ roles.map(Roles.fromString)).getOrElse(Seq(TeamMateRole))

          val tags: Seq[Tag] = bson.getAs[List[Tag]]("tags").getOrElse(Seq())

          TeamMember(id, providerId, email, bson.getAs[PasswordInfo]("passwordInfo"), firstname, lastname, new DateTime(created), active, tags, holidays, roles, scheduleConfiguration)
        }

        optionalTeamMember.get
      }

    }

    val onlyActiveMembers = BSONDocument("active" → active)

    val cursor: AkkaStreamCursor[TeamMember] = Cursor.flatten(teamMembersBSONCollection.map {
      collection ⇒
        collection.find(onlyActiveMembers).cursor[TeamMember](readPreference = ReadPreference.primary)

    })

    cursor.documentSource()
  }

  private def failoverStrategy(): FailoverStrategy =
    FailoverStrategy(
      initialDelay = 500 milliseconds,
      retries = 15,
      delayFactor =
      attemptNumber ⇒ 1 + attemptNumber * 0.5
    )

}
