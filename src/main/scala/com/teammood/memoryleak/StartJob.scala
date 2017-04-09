package com.teammood.memoryleak

import akka.actor.{ ActorSystem, Props }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object StartJob {
  def main(args: Array[String]): Unit = {

    println("Starting DailyEmailSender job...")

    val akkaSystem = ActorSystem("jobs-actor-system")

    sys.env.get("MONGODB_URI") match {
      case Some(mongoUri) ⇒
        val repository = new MongoDbRepository(mongoUri.toString, akkaSystem)

        val everyMinuteActor = akkaSystem.actorOf(Props(classOf[EveryMinuteActor], repository))

        akkaSystem.scheduler.schedule(0 milliseconds, 1 minutes, everyMinuteActor, "everyMinuteActor")

      case None ⇒ throw new RuntimeException("ERROR: Please set your mongodb connection with the MONGODB_URI env variable")
    }

  }
}
