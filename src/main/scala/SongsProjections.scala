import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceProjection, Handler, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object SongsProjections extends App with LazyLogging {
  // Define the actor system
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "AkkaProjectionApp")

  // Read the configuration for the projection from the application-old.conf
  //  val config: Config = ConfigFactory.load().getConfig("akka-projection-app")

  // Create a CqlSession (assuming you already have a running Cassandra instance)
  implicit val session: CqlSession = CqlSession.builder().build()

  // Define the MusicEvent class representing the events to be projected
  case class MusicEvent(genre: String, music: String)

  //Source Provider
  def sourceProvider: SourceProvider[Offset, EventEnvelope[MusicEvent]] = {
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
    logger.info("Event handler has started")
    override def process(envelope: EventEnvelope[MusicEvent]): Future[Done] = {
      logger.info("Event handler has started processing envelope")
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
  def projection(key: String): AtLeastOnceProjection[Offset, EventEnvelope[MusicEvent]] = {
    logger.info("Projection started building")
    CassandraProjection
      .atLeastOnce[Offset, EventEnvelope[MusicEvent]](
        projectionId = ProjectionId("music-genre-projection", key),
        sourceProvider = sourceProvider,
        handler = () => new MusicEventHandler()
      ).withSaveOffset(100, 500.millis)
  }

  val musicProjection = projection("MusicProjection")
  system.systemActorOf(ProjectionBehavior(musicProjection), musicProjection.projectionId.id)
}
