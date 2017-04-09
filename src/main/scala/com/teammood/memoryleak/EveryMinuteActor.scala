package com.teammood.memoryleak

import akka.actor.Actor
import akka.stream.scaladsl.Sink
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

class EveryMinuteActor(val repository: MongoDbRepository) extends Actor {

  implicit def materializer: akka.stream.Materializer = akka.stream.ActorMaterializer.create(this.context.system)

  def receive = {
    case _ ⇒ sendDailyEmailToTeams()
  }

  private def sendDailyEmailToTeams() = {

    val start = System.currentTimeMillis()

    val now = new DateTime()

    val sendEmailAndCount = Sink.fold[Int, TeamMember](0) {
      (count, teamMember) ⇒
        count + 1
    }

    val futureCount: Future[Int] = repository.allTeamMember(active = true).runWith(sendEmailAndCount)

    futureCount onComplete {
      case Success(count) ⇒
        if (count > 0) {
          val duration = System.currentTimeMillis() - start
          println(s"Job: $count emails sent in $duration ms")
        }
      case Failure(t) ⇒
        println("Job: An error has occured: " + t.getMessage)
        t.printStackTrace()
    }

  }
}