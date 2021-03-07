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
    private val expirienceDate = LocalDate.of(1982, 11, 22)

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
        signatures.add("VACCINESS")
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

        if (type == "DOCUMENT" || type == "EXP" || type == "ACCESS" || type == "VACCINESS")
            return ""
        if (v.distinct().count() > 1)
            return "Detainment: " + type + " mismatch."
        else
            return ""
    }

    private fun vaccineCheck(): String {


        val vacineSet = ArrayList<String>()
        vacineSet.addAll(required_vaccinations.keys)

        for ((index) in vacineSet.withIndex()) {
            val nations = required_vaccinations.get(index)

            for (nation in nations!!.withIndex()) {

                if ((nation.equals("FOREIGNERS") && values.get("nationality")!!.get(0) != "Arstotzka") || nation.equals("ENTRANTS") || nation.equals(
                        values.get("nationality")!!.get(0)
                    )
                ) {
                    if (!values.get("DOCUMENT")!!.contains("certificate of vaccination"))
                        return "Entry denied: missing required certificate of vaccination."
                    if (values.get("VACCINESS")!!.get(0).contains(vacineSet.get(index)))
                        continue
                    return "Entry denied: missing required vaccination."
                }
            }
        }
        return ""
    }

    private fun expChecking(date: String?): String {

        if (date == null)
            return ""
        val localDate = LocalDate.parse(date.substring(0, 10), DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        if (localDate.isAfter(expirienceDate) || localDate.isEqual(expirienceDate))
            return ""
        return "Entry denied: " + date.substring(11) + " expired."
    }
    private fun setDocument(docName : String, document : String){

        val doc = docName.replace("_"," ")
        values["DOCUMENT"]?.add(doc)
        getName(document)
        getDate(doc,document)
        when (docName) {
            "diplomatic_authorization" -> values["ACCESS"]?.add(document)
            "certificate_of_vaccination" -> values["VACCINESS"]?.add(document)
        }
        for (patterns in common_regex_patterns.keys){
            pattern = Pattern.compile(common_regex_patterns.get(patterns));
            matcher = pattern.matcher(document);
            if(matcher.find())
                values[patterns]?.add(matcher.group(1));
        }
    }

    private fun getName(name: String) {
        pattern = Pattern.compile(special_regex_patterns.get("name"));
        matcher = pattern.matcher(name);
        if(matcher.find());
        values["name"]?.add(matcher.group(2)+" "+matcher.group(1));
    }

    private fun getDate(docName: String, document: String) {
        pattern = Pattern.compile(special_regex_patterns.get("EXP"));
        matcher = pattern.matcher(document);
        if(matcher.find())
            values["EXP"]?.add(matcher.group(1) + " " + docName);
    }
    private fun resetValues()
    {
        values =  HashMap()
        for (s in signatures) values[s] = ArrayList()
    }

    fun inspect(a : Map<String,String>) : String{
        resetValues()

        for (ks in a.keys){
            this.setDocument(ks,a[ks]!!)

            if(values["name"]?.contains(wanted_by_the_state) == true)
                return "Detainment: Entrant is a wanted criminal."

            for(i in 0 ..values["EXP"]?.size!!) {

                var test = expChecking(values["EXP"]?.get(i))
                if (test.isNotEmpty()) return test

            }

            for (i in 0 .. entrants_requirements.size){
                if (values["DOCUMENT"]?.contains(entrants_requirements[i]) == false)
                    return "Entry denied: missing required " + entrants_requirements.get(i) + ".";
            }

            if ((values["nationality"]?.get(0) == "Arstotzka") == false) {
                for (i in 0..foreigners_requirements.size) {
                    if ((values["DOCUMENT"]?.contains(foreigners_requirements[i])) == false) {
                        if (foreigners_requirements[i] == "access permit") {
                            if (values["DOCUMENT"]?.contains("diplomatic authorization") == true) {
                                if ((values["ACCESS"]?.get(0)?.contains("Arstotzka")) == false)
                                    return "Entry denied: invalid diplomatic authorization"
                                else
                                    continue
                            } else if (values["DOCUMENT"]?.contains("grant of asylum") == true)
                                continue
                            else
                                return "Entry denied: missing required " + foreigners_requirements[i] + "."
                        }
                        return "Entry denied: missing required " + foreigners_requirements[i] + "."
                    }

                }
            }

            }
            if ((allowed_nations.contains(values["nationality"]?.get(0))) == false)
                return "Entry denied: citizen of banned nation."

            if (values["PURPOSE"]?.contains("WORK") == true && workers_requirements.contains("work pass")){
                if(values["DOCUMENT"]?.contains("work pass") == false)
                    return "Entry denied: missing required work pass."
            }

             if(vaccineCheck().isNotEmpty())
                 return vaccineCheck()

             if (values["nationality"]?.get(0) == "Arstotzka" && citizens_requirements.contains("ID card") && values["DOCUMENT"]?.contains("ID card") == false)
                 return "Entry denied: missing required ID card.";

             if(values["nationality"]?.get(0) == "Arstotzka")
                 return "Glory to Arstotzka"
            else
                return "Cause no trouble"
        }

    }






