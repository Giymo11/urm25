//> using scala 3.7.3
//> using dep com.lihaoyi::upickle:4.4.1
//> using dep com.lihaoyi::mainargs:0.7.7
//> using dep com.lihaoyi::os-lib:0.11.6

// this script generates a schedule.json for the experiment
// it is all unique permutations of AABBB and AAABB (10 each)
// and assign them to subject nicknames in the format of 'brave-otter'

import java.time.*
import mainargs.*
import upickle.default.*
import scala.util.Random

def generate_nicknames(count: Int): Set[String] = {
  val adjectives = Seq(
    "brave",
    "curious",
    "eager",
    "fierce",
    "gentle",
    "happy",
    "jolly",
    "kind",
    "lively",
    "mighty",
    "noble",
    "proud",
    "quick",
    "sly",
    "wise"
  )
  val animals    = Seq(
    "otter",
    "fox",
    "bear",
    "wolf",
    "eagle",
    "lion",
    "tiger",
    "rabbit",
    "deer",
    "hawk",
    "panda",
    "koala",
    "dolphin",
    "whale",
    "shark"
  )

  val nicknames = for {
    adj    <- adjectives
    animal <- animals
  } yield s"$adj-$animal"

  Random.shuffle(nicknames).take(count).toSet
}

def parseTimestamp(datestr: String): Instant = {
  val dateTimeFormat = format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  val localTs        = LocalDateTime.parse(datestr, dateTimeFormat)
  localTs.atZone(ZoneId.systemDefault()).toInstant
}

val filename         = "schedule.json"
// start date, starting at 4 AM
val start_date       = "2025-11-14 04:00"
val start_timestamp  = parseTimestamp(start_date).toString
val num_subjects     = 20
val patterns         = Seq("AABBB", "AAABB")
val assignment_desc  = "All unique permutations of AABBB (10) and AAABB (10), one per subject."
val all_permutations = patterns.flatMap(_.permutations).toSet

// assert we have the right number of unique permutations
assert(
  all_permutations.size == num_subjects,
  s"Expected $num_subjects unique permutations, got ${all_permutations.size}"
)
// each day should have equal number of A and B assignments across all subjects
for day <- 0 until 5 do
  val countA = all_permutations.count(pattern => pattern(day) == 'A')
  val countB = all_permutations.count(pattern => pattern(day) == 'B')
  assert(countA == countB, s"Day ${day + 1} does not have equal A and B assignments: A=$countA, B=$countB")

// create json for server to load
case class SubjectSchedule(nickname: String, pattern: String, start_date: String) derives ReadWriter

val nicknames = generate_nicknames(num_subjects).toSeq
val schedules = nicknames.zip(all_permutations.toSeq).map { case (nickname, pattern) =>
  SubjectSchedule(nickname, pattern, start_timestamp)
}
// json includes assignment description
case class ScheduleFile(assignment_description: String, schedules: Seq[SubjectSchedule]) derives ReadWriter

val json = write(ScheduleFile(assignment_desc, schedules), indent = 4)
os.write.over(os.pwd / filename, json)
println(s"Generated schedule for $num_subjects subjects and wrote to $filename")
