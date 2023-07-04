import SongsPersistence.Event
import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
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
    ActorSystem(Behaviors.empty, "music_keyspace")

  // Create a CqlSession (assuming you already have a running Cassandra instance)
  implicit val session: CqlSession = CqlSession.builder().build()

  // Define the MusicEvent class representing the events to be projected
  case class MusicEvent(genre: String, music: String)

  //Source Provider
  def sourceProvider: SourceProvider[Offset, EventEnvelope[Event]] = {
    logger.info("Source Provider has started")
    EventSourcedProvider
      .eventsByTag[Event](
        system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = "Rock"
      )
  }

  // The handler function to process the events
  class MusicEventHandler extends Handler[EventEnvelope[Event]] {
    logger.info("Event handler has started")

    override def process(envelope: EventEnvelope[Event]): Future[Done] = {
      logger.info("Event handler has started processing envelope")
      val event = envelope.event
      //      val genre = event.genre
      //      val music = event.music

      // Here you can perform any processing or logic with the event
      // For simplicity, we will just log the event
      logger.info(s"Received event: $event")

      Future.successful(Done)
    }
  }

  // The projection implementation
  def projection(key: String): AtLeastOnceProjection[Offset, EventEnvelope[Event]] = {
    logger.info("Projection started building")
    CassandraProjection
      .atLeastOnce[Offset, EventEnvelope[Event]](
        projectionId = ProjectionId("music-genre-projection", key),
        sourceProvider = sourceProvider,
        handler = () => new MusicEventHandler()
      ).withSaveOffset(100, 500.millis)
  }

//  val musicProjection = projection("MusicProjection")
//  system.systemActorOf(ProjectionBehavior(musicProjection), musicProjection.projectionId.id)

    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
      name = "music-genre-daemon",
      numberOfInstances = 1, //"Rock"
      behaviorFactory = (_) => ProjectionBehavior(projection("Rock")),
      stopMessage = ProjectionBehavior.Stop)
}