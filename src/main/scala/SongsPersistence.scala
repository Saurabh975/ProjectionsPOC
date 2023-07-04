import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior.{
  CommandHandler,
  EventHandler
}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.typesafe.scalalogging.LazyLogging

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
  musicGenreActor ! AddMusic("Rock", "Sweet Child O' Mine", replyToActor)
  musicGenreActor ! AddMusic("Jazz", "Fly Me to the Mon", replyToActor)
  musicGenreActor ! AddMusic(
    "Country",
    "Take Me Home, Country Roads",
    replyToActor
  )
  logger.info("Music Event sent")
}
