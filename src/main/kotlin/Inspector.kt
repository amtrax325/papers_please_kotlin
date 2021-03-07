import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern

class Inspector {


    private var values = HashMap<String, ArrayList<String>>()
    private val common_regex_patterns = HashMap<String, String>()
    private val special_regex_patterns = HashMap<String, String>()
    private val signatures = ArrayList<String>()

    private val foreigners_requirements = ArrayList<String>()
    private val citizens_requirements = ArrayList<String>()
    private val entrants_requirements = ArrayList<String>()
    private val allowed_nations = ArrayList<String>()

    private val required_vaccinations = HashMap<String, ArrayList<String>>()
    private val workers_requirements = ArrayList<String>()
    private var wanted_by_the_state: String = ""
    private val experienceDate = LocalDate.of(1982, 11, 22)

    private lateinit var matcher: Matcher
    private lateinit var pattern: Pattern

    init {
        common_regex_patterns["DURATION"] = "DURATION: (\\d+ [A-Z]+|[A-Z]+)"
        common_regex_patterns["PURPOSE"] = "PURPOSE: ([A-Z]+)"
        common_regex_patterns["WEIGHT"] = "WEIGHT: (\\d{2,3})"
        common_regex_patterns["HEIGHT"] = "HEIGHT: (\\d{2,3})"
        common_regex_patterns["nationality"] = "NATION: ([A-Z][a-z]+\\s*[A-Z]*[a-z]*)\n+"
        common_regex_patterns["ISS"] = "ISS: ([A-Z][a-z]+.?\\s*[A-Z]*[a-z]*)\n+"
        common_regex_patterns["DOB"] = "DOB: (\\d{4}\\.+\\d{2}\\.+\\d{2})"
        common_regex_patterns["SEX"] = "SEX: (F|M)"
        common_regex_patterns["ID number"] = "ID#: (\\w*-*\\w*)"
        common_regex_patterns["FIELD"] = "FIELD: ([A-Z][a-z]+\\s*[a-z]*)"
        special_regex_patterns["name"] = "NAME: ([A-Z]\\w+),+\\s([A-Z][a-z]+)"
        special_regex_patterns["EXP"] = "EXP: (\\d{4}\\.+\\d{2}\\.+\\d{2})"
        signatures.addAll(common_regex_patterns.keys)
        signatures.addAll(special_regex_patterns.keys)
        signatures.add("DOCUMENT")
        signatures.add("ACCESS")
        signatures.add("VACCINES")
    }

    fun receiveBulletin(bulletin: String) {

        val req = bulletin.split("\n")
        for (s in req) {

            when {
                s.matches(Regex("Allow citizens of.+")) -> {
                    pattern = Pattern.compile("(?:Allow citizens of)? ([A-Z][a-z]+\\s*[A-Z]*[a-z]*),*")
                    matcher = pattern.matcher(s)
                    while (matcher.find()) {
                        allowed_nations.add(matcher.group(1))
                    }
                }
                s.matches(Regex("Deny citizens of.+")) -> {

                    pattern = Pattern.compile("(?: Deny citizens of)? ([A-Z][a-z]+\\s*[A-Z]*[a-z]*),*")
                    matcher = pattern.matcher(s)
                    while (matcher.find()) {
                        allowed_nations.remove(matcher.group(1))
                    }
                }
                s.matches(Regex("\\D+ no longer require (\\w+\\s?\\w*) vaccination")) -> {
                    pattern = Pattern.compile("\\D+ no longer require (\\w+\\s?\\w*) vaccination")
                    matcher = pattern.matcher(s)
                    matcher.find()

                    val vaccination = matcher.group(1)
                    val nations = ArrayList<String>()

                    pattern =
                        Pattern.compile("(?:^Citizens of|(?!^)\\G,) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)(?=[a-zA-Z, ]*?)")
                    matcher = pattern.matcher(s)
                    while (matcher.find())
                        nations.add(matcher.group(1))

                    when {
                        nations.isNotEmpty() -> {
                            while (nations.isNotEmpty()) {
                                required_vaccinations[vaccination]?.remove(nations.removeAt(0))
                            }
                        }
                        s.contains("Foreigners") -> required_vaccinations[vaccination]?.remove("FOREIGNERS")
                        else -> required_vaccinations[vaccination]?.remove("ENTRANTS")

                    }

                }
                s.matches(Regex("\\D+ require (\\w+\\s?\\w*) vaccination")) -> {

                    //thanks to: https://stackoverflow.com/users/548225/anubhava for commitment in this regex pattern
                    pattern =
                        Pattern.compile("(?:^Citizens of|(?!^)\\G,) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)(?=[a-zA-Z, ]*?)")
                    matcher = pattern.matcher(s)

                    val nations = ArrayList<String>()
                    while (matcher.find())
                        nations.add(matcher.group(1))

                    pattern = Pattern.compile("\\D+ require (\\w+\\s?\\w*) vaccination")
                    matcher = pattern.matcher(s)
                    matcher.find()


                    when {
                        nations.isNotEmpty() -> required_vaccinations[matcher.group(1)] = nations
                        s.contains("Foreigners") -> {
                            nations.add("FOREIGNERS")
                            required_vaccinations[matcher.group(1)] = nations
                        }
                        else -> {
                            nations.add("ENTRANTS")
                            required_vaccinations[matcher.group(1)] = nations
                        }
                    }
                }
                s.matches(Regex("Foreigners require \\D+")) -> {
                    change(s, "(?:Foreigners require) (\\D+)", foreigners_requirements)
                }
                s.matches(Regex("Workers require \\D+")) -> {
                    change(s, "(?:Workers require) (\\D+)", workers_requirements)
                }
                s.matches(Regex("Citizens of Arstotzka require \\D+")) -> {
                    change(s, "(?:Citizens of Arstotzka require) (\\D+)", citizens_requirements)
                }
                s.matches(Regex("Wanted by the State: \\D+")) -> {
                    pattern = Pattern.compile(("(?:Wanted by the State: )(\\D+)"));matcher = pattern.matcher(s)
                    matcher.find()
                    wanted_by_the_state = matcher.group(1)
                }
                s.matches(Regex("Entrants require \\D+")) -> {
                    change(s, "(?:Entrants require) (\\D+)", entrants_requirements)
                }
            }


        }

    }

    private fun change(word: String, regex: String, col: ArrayList<String>) {
        pattern = Pattern.compile(regex)
        matcher = pattern.matcher(word)
        matcher.find()
        col.add(matcher.group(1))
    }

    private fun check(v: ArrayList<String>, type: String): String {

        if (type == "DOCUMENT" || type == "EXP" || type == "ACCESS" || type == "VACCINES")
            return ""
        return if (v.distinct().count() > 1)
            "Detainment: $type mismatch."
        else
            ""
    }

    private fun vaccineCheck(): String {


        val vaccineSet = ArrayList<String>()
        vaccineSet.addAll(required_vaccinations.keys)

        for ((index) in vaccineSet.withIndex()) {
            val nations = required_vaccinations.get(index)

            for (nation in nations!!.withIndex()) {

                if ((nation.equals("FOREIGNERS") && values["nationality"]!![0] != "Arstotzka") || nation.equals(
                        "ENTRANTS"
                    ) || nation.equals(
                        values["nationality"]!![0]
                    )
                ) {
                    if (!values["DOCUMENT"]!!.contains("certificate of vaccination"))
                        return "missing required certificate of vaccination".entryDenied()
                    if (values["VACCINES"]!![0].contains(vaccineSet[index]))
                        continue
                    return "missing required vaccination.".entryDenied()
                }
            }
        }
        return ""
    }

    private fun expChecking(date: String?): String {

        if (date == null)
            return ""
        val localDate = LocalDate.parse(date.substring(0, 10), DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        if (localDate.isAfter(experienceDate) || localDate.isEqual(experienceDate))
            return ""
        return (date.substring(11) + " expired").entryDenied()
    }

    private fun setDocument(docName: String, document: String) {

        val doc = docName.replace("_", " ")
        values["DOCUMENT"]?.add(doc)
        getName(document)
        getDate(doc, document)
        when (docName) {
            "diplomatic_authorization" -> values["ACCESS"]?.add(document)
            "certificate_of_vaccination" -> values["VACCINES"]?.add(document)
        }
        for (patterns in common_regex_patterns.keys) {
            pattern = Pattern.compile(common_regex_patterns[patterns]!!)
            matcher = pattern.matcher(document)
            if (matcher.find())
                values[patterns]?.add(matcher.group(1))
        }
    }

    private fun getName(name: String) {
        pattern = Pattern.compile(special_regex_patterns["name"]!!)
        matcher = pattern.matcher(name)
        if (matcher.find())
        values["name"]?.add(matcher.group(2) + " " + matcher.group(1))
    }

    private fun getDate(docName: String, document: String) {
        pattern = Pattern.compile(special_regex_patterns["EXP"]!!)
        matcher = pattern.matcher(document)
        if (matcher.find())
            values["EXP"]?.add(matcher.group(1) + " " + docName)
    }

    private fun resetValues() {
        values = HashMap()
        for (s in signatures) values[s] = ArrayList()
    }

    fun inspect(a: Map<String, String>): String {
        resetValues()

        for (ks in a.keys) {
            this.setDocument(ks, a[ks]!!)

            if (values["name"]?.contains(wanted_by_the_state) == true)
                return "Detainment: Entrant is a wanted criminal."
for (i in 0  until values.size){
    val test = check(values[signatures[i]]!!,signatures[i])
    if(test.isNotEmpty())
        return test
}

            for (i in 0 until values["EXP"]?.size!!) {

                val test = expChecking(values["EXP"]?.get(i))
                if (test.isNotEmpty()) return test

            }

            for (i in 0 until entrants_requirements.size) {
                if (values["DOCUMENT"]?.contains(entrants_requirements[i]) == false)
                    return ("missing required " + entrants_requirements[i]).entryDenied()
            }

            if (values["nationality"]?.get(0) != "Arstotzka") {
                for (i in 0 until foreigners_requirements.size) {
                    if ((values["DOCUMENT"]?.contains(foreigners_requirements[i])) == false) {
                        if (foreigners_requirements[i] == "access permit") {
                            return if (values["DOCUMENT"]?.contains("diplomatic authorization") == true) {
                                if ((values["ACCESS"]?.get(0)?.contains("Arstotzka")) == false)
                                    "invalid diplomatic authorization".entryDenied()
                                else
                                    continue
                            } else if (values["DOCUMENT"]?.contains("grant of asylum") == true)
                                continue
                            else
                                ("missing required " + foreigners_requirements[i]).entryDenied()
                        }
                        ("missing required " + foreigners_requirements[i]).entryDenied()
                    }

                }
            }

        }

        if (!allowed_nations.contains(values["nationality"]?.get(0)))
            return "citizen of banned nation.".entryDenied()

        if (values["PURPOSE"]?.contains("WORK") == true && workers_requirements.contains("work pass")) {
            if (values["DOCUMENT"]?.contains("work pass") == false)
                return "missing required work pass".entryDenied()
        }

        if (vaccineCheck().isNotEmpty())
            return vaccineCheck()

        if (values["nationality"]?.get(0) == "Arstotzka" && citizens_requirements.contains("ID card") && values["DOCUMENT"]?.contains(
                "ID card"
            ) == false
        )
            return "missing required ID card".entryDenied()

        return if (values["nationality"]?.get(0) == "Arstotzka")
            "Glory to Arstotzka"
        else
            "Cause no trouble"
    }

}

private fun String.entryDenied(): String {

    return "Entry denied: $this ."
}







