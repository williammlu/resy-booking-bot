package com.resy

import akka.actor.ActorSystem
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object ResyBookingBot {

  private val resyKeys = ResyKeys(
    // Your user profile API key which can be found via your browser web console in your headers
    // called "authorization"
    apiKey = "insert",
    // Your user profile authentication token which can be found via your browser web console in
    // your headers called "x-resy-auth-token"
    authToken = "insert"
  )

  private val resDetails = ReservationDetails(
    // Date of the reservation in YYYY-MM-DD format
    date = "2022-08-29",
    // Size of the party reservation
    partySize = 3,
    // Unique identifier of the restaurant where you want to make the reservation
    venueId = 1505, // currently pointing to Don Angie
    // Priority list of reservation times and table types. Time is in military time HH:MM:SS format.
    // If no preference on table type, then simply don't set it.
    // If you provide a list reservation times, it won't snipe if there isn't
    // any availability. Err on the side of too many options.
    resTimeTypes = Seq(ReservationTimeType("19:00:00"), ReservationTimeType("19:30:00"), ReservationTimeType("19:45:00"), ReservationTimeType("19:15:00"), ReservationTimeType("20:00:00"), ReservationTimeType("20:15:00"), ReservationTimeType("20:30:00"), ReservationTimeType("18:45:00"), ReservationTimeType("18:30:00"))
  )

  private val snipeTime = SnipeTime(
    // Hour of the day when reservations become available and when you want to snipe
    hours = 6,
    // Minute of the day when reservations become available and when you want to snipe
    minutes = 0
  )

  def main(args: Array[String]): Unit = {
    println("Starting Resy Booking Bot")

    val resyApi    = new ResyApi(resyKeys)
    val resyClient = new ResyClient(resyApi)

    val system      = ActorSystem("System")
    val dateTimeNow = DateTime.now
    val todaysSnipeTime = dateTimeNow
      .withHourOfDay(snipeTime.hours)
      .withMinuteOfHour(snipeTime.minutes)
      .withSecondOfMinute(0)
      .withMillisOfSecond(0)

    val nextSnipeTime =
      if (todaysSnipeTime.getMillis > dateTimeNow.getMillis) todaysSnipeTime
      else todaysSnipeTime.plusDays(1)

    val millisUntilTomorrow = nextSnipeTime.getMillis - DateTime.now.getMillis - 1000
    val hoursRemaining      = millisUntilTomorrow / 1000 / 60 / 60
    val minutesRemaining    = millisUntilTomorrow / 1000 / 60 - hoursRemaining * 60
    val secondsRemaining =
      millisUntilTomorrow / 1000 - hoursRemaining * 60 * 60 - minutesRemaining * 60

    println(s"Current time: ${DateTime.now}")
    println(s"Next snipe time: $nextSnipeTime")
    println(
      s"Sleeping for $hoursRemaining hours, $minutesRemaining minutes, and $secondsRemaining seconds"
    )

    var loopCounter = 0.0
    while (nextSnipeTime.getMillis > DateTime.now.getMillis) {
      println(s"Waited for $loopCounter seconds")
      Thread.sleep(500)
      loopCounter += 0.5
    }
    ResyBookingWorkflow.run(resyClient, resDetails)
    println("Shutting down Resy Booking Bot at " + DateTime.now)
    System.exit(0)
  }
}

final case class ResyKeys(apiKey: String, authToken: String)

final case class ReservationDetails(
  date: String,
  partySize: Int,
  venueId: Int,
  resTimeTypes: Seq[ReservationTimeType]
)

final case class ReservationTimeType(reservationTime: String, tableType: Option[String] = None)

object ReservationTimeType {

  def apply(reservationTime: String, tableType: String): ReservationTimeType = {
    ReservationTimeType(reservationTime, Some(tableType))
  }
}

final case class SnipeTime(hours: Int, minutes: Int)
