import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior.{
  CommandHandler,
  EventHandler
}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceProjection, Handler, SourceProvider}
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

object SongsPersistence extends App with LazyLogging {

  // Command definition
  sealed trait Command
  case class AddMusic(genre: String,
                      music: String,
                      replyTo: ActorRef[Confirmation])
      extends Command

  // Event definition
  sealed trait Event
  case class MusicAdded(genre: String, music: String) extends Event

  // State definition
  case class State(musicByGenre: Map[String, List[String]] = Map.empty)

  // Confirmation message
  case class Confirmation(message: String)

  // Command handler
  val commandHandler: CommandHandler[Command, Event, State] = {
    (state, command) =>
      command match {
        case AddMusic(genre, music, replyTo) =>
          logger.info(
            s"Received Add music request for genre $genre and music $music"
          )
          val event = MusicAdded(genre, music)
          Effect
            .persist(event)
            .thenReply(replyTo)(
              _ => Confirmation(s"Added music '$music' to genre '$genre'")
            )
      }
  }

  // Event handler
  val eventHandler: EventHandler[State, Event] = { (state, event) =>
    event match {
      case MusicAdded(genre, music) =>
        val updatedMusicByGenre = state.musicByGenre.updatedWith(genre) {
          case Some(musicList) => Some(music :: musicList)
          case None            => Some(List(music))
        }
        State(updatedMusicByGenre)
    }
  }

  // Define the MusicGenre behavior
  val musicEventPersist: Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("music-genre"),
      emptyState = State(),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).withTagger(_ => Set("Rock"))

  val musicResultActor: Behavior[Confirmation] = Behaviors.receive {
    (context, message) =>
      message match {
        case Confirmation(confirmation) =>
          logger.info(confirmation)
          Behaviors.same
      }
  }

  // Define the actor system
  val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "MusicGenreApp")

  // Create the MusicGenre actor
  val musicGenreActor =
    system.systemActorOf(musicEventPersist, "music-genre-actor")
  val replyToActor = system.systemActorOf(musicResultActor, "MusicResult")

  musicGenreActor ! AddMusic("Rock", "Summer of 69", replyToActor)
  logger.info("Music Event sent")

//  ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
//    name = "music-genre-daemon",
//    numberOfInstances = 1, //"Rock"
//    behaviorFactory = (_) => ProjectionBehavior(MusicProjection.projection("Rock", system)),
//    stopMessage = ProjectionBehavior.Stop)

  val projection1 = MusicProjection.projection("Rock", system)
  ClusterSingleton(system).init(
    SingletonActor(ProjectionBehavior(projection1), projection1.projectionId.id)
      .withStopMessage(ProjectionBehavior.Stop)
  )
}









object MusicProjection extends LazyLogging {

  // Create a CqlSession (assuming you already have a running Cassandra instance)
  implicit val session: CqlSession = CqlSession.builder().build()

  // Define the MusicEvent class representing the events to be projected
  case class MusicEvent(genre: String, music: String)

  //Source Provider
  def sourceProvider(
    system: ActorSystem[Nothing]
  ): SourceProvider[Offset, EventEnvelope[MusicEvent]] = {
    logger.info("Source Provider has started")
    EventSourcedProvider
      .eventsByTag[MusicEvent](
        system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = "Rock"
      )
  }

  // The handler function to process the events
  class MusicEventHandler extends Handler[EventEnvelope[MusicEvent]] {
    logger.info("Event handler Initializing")
    override def process(envelope: EventEnvelope[MusicEvent]): Future[Done] = {
      logger.info(s"Started processing envelope: $envelope")
      val event = envelope.event
      val genre = event.genre
      val music = event.music

      // Here you can perform any processing or logic with the event
      // For simplicity, we will just log the event
      logger.info(s"Received event: genre=$genre, music=$music")

      Future.successful(Done)
    }
  }

  // The projection implementation
  def projection(
    tag: String,
    system: ActorSystem[Nothing]
  ): AtLeastOnceProjection[Offset, EventEnvelope[MusicEvent]] = {
    logger.info("Projection Initializing")
    CassandraProjection
      .atLeastOnce[Offset, EventEnvelope[MusicEvent]](
        projectionId = ProjectionId("music-genre-projection", tag),
        sourceProvider = sourceProvider(system),
        handler = () => new MusicEventHandler()
      )
  }
}
