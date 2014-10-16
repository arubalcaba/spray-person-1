package com.example.actors.routes

import com.example.model.Person
import com.example.model.PersonJsonProtocol._
import com.example.services.PersonService

import akka.actor.Props
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport
import akka.actor.Actor

/**
* Factory method for Props configuration files for actors
*/
object PersonRoute {
  def props: Props = Props(new PersonRoute())
}

/**
 * Actor that handles requests that begin with "person"
 */
class PersonRoute() extends Actor with PersonRouteTrait {
  def actorRefFactory = context
  def receive = runRoute(personRoute)
}

/**
 * Separate routing logic in an HttpService trait so that the
 * routing logic can be tested outside of an actor system in specs/mockito tests
 */
trait PersonRouteTrait extends HttpService with SprayJsonSupport{

  private val personService = PersonService

  val personRoute = {
    get {
      pathEnd {
        complete {
          val persons = personService.getPersons
          persons match {
            case head :: tail => persons
            case Nil => StatusCodes.NoContent
          }
        }
      } ~
      path(LongNumber) { personId =>
        val person = personService.getPersonById(personId)
        complete(person)
      }
    } ~
    (post & pathEnd) {
      entity(as[Person]) { person =>
        val newPerson = personService.addPerson(person)
        complete(StatusCodes.Created, newPerson)
      }
    } ~
    (put & path(LongNumber) & pathEnd) { personId =>
      entity(as[Person]) { person =>
        val updatedPerson = personService.updatePerson(person.copy(id = Some(personId)))
        updatedPerson match {
          case true => complete(StatusCodes.NoContent)
          case false => complete(StatusCodes.NotFound)
        }
      }
    } ~
    (delete & path(LongNumber) & pathEnd) { personId =>
      personService.deletePerson(personId)
      complete(StatusCodes.NoContent)
    }
  }

}
