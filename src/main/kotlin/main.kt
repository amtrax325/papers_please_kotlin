
import java.util.HashMap


fun main(args: Array<String>) {



   val inspector = Inspector()
    inspector.receiveBulletin("Entrants require passport\nAllow citizens of Arstotzka, Obristan");


    val josef: MutableMap<String, String> = HashMap()
    josef["passport"] =
        "ID#: GC07D-FU8AR\nNATION: Arstotzka\nNAME: Costanza, Josef\nDOB: 1933.11.28\nSEX: M\nISS: East Grestin\nEXP: 1983.03.15"

    val guyovich: MutableMap<String, String> = HashMap()
    guyovich["access_permit"] =
        "NAME: Guyovich, Russian\nNATION: Obristan\nID#: TE8M1-V3N7R\nPURPOSE: TRANSIT\nDURATION: 14 DAYS\nHEIGHT: 159cm\nWEIGHT: 60kg\nEXP: 1983.07.13"

    val roman: MutableMap<String, String> = HashMap()
    roman["passport"] =
        "ID#: WK9XA-LKM0Q\nNATION: United Federation\nNAME: Dolanski, Roman\nDOB: 1933.01.01\nSEX: M\nISS: Shingleton\nEXP: 1983.05.12"
    roman["grant_of_asylum"] =
        "NAME: Dolanski, Roman\nNATION: United Federation\nID#: Y3MNC-TPWQ2\nDOB: 1933.01.01\nHEIGHT: 176cm\nWEIGHT: 71kg\nEXP: 1983.09.20"


    println(inspector.inspect(josef))
    println(inspector.inspect(guyovich))
    println(inspector.inspect(roman))
}