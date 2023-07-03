import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

// Define the messages for communication between actors
sealed trait MusicMessage
case class AddMusic(genre: String,
                    music: String,
                    replyTo: ActorRef[MusicResult])
    extends MusicMessage

sealed trait MusicResult
case class MusicAdded(genre: String, music: String) extends MusicResult

// Define the music storage actor
object MusicStorageActor {
  def apply(session: CqlSession): Behavior[MusicMessage] = Behaviors.receive {
    (context, message) =>
      message match {
        case AddMusic(genre, music, replyTo) =>
          context.log.info(s"Received Add music request for Genre -> $genre, Song -> $music")
          val statement = session.prepare(
            "INSERT INTO  music_keyspace.music_by_genre (genre, music) VALUES (?, ?)"
          )
          context.log.info(s"The Prepared Statement is $statement")
          val boundStatement: BoundStatement = statement.bind(genre, music)
          context.log.info(s"The Bound statement is $boundStatement")
          executeAsync(session, boundStatement).onComplete {
            case Success(_) =>
              replyTo ! MusicAdded(genre, music)
            case Failure(ex) =>
              context.log.info(s"Failed to add music: ${ex.getMessage}")
          }
          Behaviors.same
      }
  }

  private def executeAsync(session: CqlSession, statement: BoundStatement)(
    implicit ec: ExecutionContext
  ): Future[Unit] = {
    Future(session.executeAsync(statement))(ec).flatMap(Future(_)(ec))(ec).map(_ => ())(ec)
  }
}

object MusicResultActor {
  def apply(): Behavior[MusicResult] = Behaviors.receive { (context, message) =>
    message match {
      case MusicAdded(genre, music) =>
        context.log.info(s"$music of $genre has been added")
        Behaviors.same
    }
  }
}

// Main entry point of the application
object MusicApp extends App {

  // Create a CqlSession (assuming you already have a running Cassandra instance)
  val session: CqlSession = CqlSession.builder().build()

  // Create the actor system
  val system = ActorSystem[MusicMessage](MusicStorageActor(session), "MusicApp")
  system.log.info("Cassandra session created")
  system.log.info(s"Actor System Created with name ${system.name}")
  // Examples of using the actor system
  val storageActor = system
  val replyToActor = system.systemActorOf(MusicResultActor(), "MusicResult")
  system.log.info("Storage and ReplyTo actor created")

  storageActor ! AddMusic("Rock", "Stairway to Heaven", replyToActor)
  storageActor ! AddMusic("Rock", "Bohemian Rhapsody", replyToActor)
  storageActor ! AddMusic("Jazz", "Fly me to the Moon", replyToActor)
  storageActor ! AddMusic("Country", "Take me Home! Country Roads", replyToActor)
}
