package forex

import akka.actor.{ActorSystem, Props}


object Main extends App {
  implicit val actorSystem: ActorSystem = ActorSystem()
  actorSystem.actorOf(Props[HolderActor])
}