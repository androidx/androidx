/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver

import androidx.benchmark.inMemoryTrace
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.PokemonInfo.Companion.MAX_ATTACK
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.PokemonInfo.Companion.MAX_DEFENSE
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.PokemonInfo.Companion.MAX_HP
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.PokemonInfo.Companion.MAX_SPEED
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fake data for the API response.
 *
 * IMPORTANT: Keep in sync with the corresponding classes in the pokedex-views and pokedex-compose
 * variants.
 */
@Serializable
data class PokemonInfo(
    @SerialName(value = "id") val id: Int,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "height") val height: Int,
    @SerialName(value = "weight") val weight: Int,
    @SerialName(value = "base_experience") val experience: Int,
    @SerialName(value = "types") val types: List<TypeResponse>,
    @SerialName(value = "stats") val stats: List<StatsResponse>,
    val exp: Int = Random.nextInt(MAX_EXP),
) {
    @Serializable
    data class TypeResponse(
        @SerialName(value = "slot") val slot: Int,
        @SerialName(value = "type") val type: Type,
    )

    @Serializable
    data class StatsResponse(
        @SerialName(value = "base_stat") val baseStat: Int,
        @SerialName(value = "effort") val effort: Int,
        @SerialName(value = "stat") val stat: Stat,
    )

    @Serializable data class Stat(@SerialName(value = "name") val name: String)

    @Serializable data class Type(@SerialName(value = "name") val name: String)

    companion object {
        const val MAX_HP = 300
        const val MAX_ATTACK = 300
        const val MAX_DEFENSE = 300
        const val MAX_SPEED = 300
        const val MAX_EXP = 1000
    }
}

/**
 * Fake data for the API response.
 *
 * IMPORTANT: Keep in sync with the corresponding classes in the pokedex-views and pokedex-compose
 * variants.
 */
@Serializable class PokemonNetworkModel(val name: String)

/**
 * Fake data for the API response.
 *
 * IMPORTANT: Keep in sync with the corresponding classes in the pokedex-views and pokedex-compose
 * variants.
 */
@Serializable
data class PokemonResponse(
    @SerialName(value = "count") val count: Int,
    @SerialName(value = "next") val next: String?,
    @SerialName(value = "previous") val previous: String?,
    @SerialName(value = "results") val results: List<PokemonNetworkModel>,
)

/**
 * Create a [PokemonResponse] with a list of [pokemons].
 *
 * @param pokemons The pokemons to be contained in the response, a list of generated items with fake
 *   data by default.
 */
fun fakePokemonResponse(
    pokemons: List<PokemonNetworkModel> = fakePokemonNetworkModels(AllPokemonNames)
) = PokemonResponse(count = pokemons.size, previous = null, next = null, results = pokemons)

fun fakePokemonNetworkModels(pokemonNames: List<String>) =
    pokemonNames.map { name -> PokemonNetworkModel(name) }

fun fakePokemonNames(limit: Int, offset: Int = 0): List<String> =
    inMemoryTrace("fakePokemonNames(limit=$limit, offset=$offset)") {
        val numberOfPokemon = AllPokemonNames.size
        val from = offset.coerceIn(0, numberOfPokemon)
        val max = (offset + limit).coerceAtMost(numberOfPokemon)
        return AllPokemonNames.subList(from, max)
    }

val AllPokemonNames =
    listOf(
        "Ablazeon",
        "Aerofer",
        "Amphibyte",
        "Anglark",
        "Aquastrike",
        "Arborix",
        "Arctiflow",
        "Armadile",
        "Astrobat",
        "Audioson",
        "Backfire",
        "Barricade",
        "Basaltix",
        "Batterfly",
        "Beambyte",
        "Berylix",
        "Biobloom",
        "Bizarroid",
        "Blazefin",
        "Boltclaw",
        "Bonetail",
        "Boulderisk",
        "Bramblepuff",
        "Bronzite",
        "Bubblepod",
        "Buggle",
        "Bulbasa",
        "Bumblefoot",
        "Burrowix",
        "Buzzardly",
        "Cactuspear",
        "Cadmiumite",
        "Canopyawn",
        "Capribble",
        "Carapaceon",
        "Caveworm",
        "Celestrike",
        "Cinderwing",
        "Citrineon",
        "Cliffhopper",
        "Cloudillo",
        "Coalabear",
        "Cobaltix",
        "Cometear",
        "Coralite",
        "Cosmite",
        "Cragrawler",
        "Creekle",
        "Crestawl",
        "Cryofrost",
        "Cubblestone",
        "Cyberspyke",
        "Dampfly",
        "Darkowl",
        "Dawnwing",
        "Deepshell",
        "Deltawing",
        "Desertail",
        "Dewdrop",
        "Diamondix",
        "Digmound",
        "Dirtdigger",
        "Diskdrive",
        "Dizzybat",
        "Doombloom",
        "Dracoat",
        "Dragofin",
        "Drakonix",
        "Dreamist",
        "Drillburrow",
        "Dronix",
        "Drowsipurr",
        "Duneviper",
        "Dustbunny",
        "Dynamite",
        "Ebonwing",
        "Echoark",
        "Eelixir",
        "Emberfox",
        "Emeraldite",
        "Energlow",
        "Equinox",
        "Eruptile",
        "Everbloom",
        "Fableaf",
        "Falconryx",
        "Fangsnap",
        "Featherfin",
        "Ferroclaw",
        "Fiercehorn",
        "Firefang",
        "Fissurion",
        "Flashfire",
        "Flatsnout",
        "Flitterby",
        "Flowertail",
        "Flutterfin",
        "Fogwhisper",
        "Forestomp",
        "Fossilite",
        "Fractalix",
        "Frostbite",
        "Fungoyle",
        "Galaxite",
        "Galeonix",
        "Gargoyle",
        "Gascloud",
        "Geminite",
        "Glacieron",
        "Glimmerbug",
        "Gloomfang",
        "Glowfin",
        "Graniteel",
        "Grassnake",
        "Gravityx",
        "Grubsqueak",
        "Gunkpile",
        "Gustwing",
        "Hailstone",
        "Hammerhead",
        "Harmonic",
        "Hazehorn",
        "Heatwave",
        "Heavyhorn",
        "Helixite",
        "Herbivore",
        "Hexagon",
        "Hillsnake",
        "Hollowawk",
        "Honeycomb",
        "Hornetail",
        "Hoverbug",
        "Hummingwing",
        "Hydrocoil",
        "Icefang",
        "Icicleon",
        "Igniteon",
        "Illumite",
        "Ironhide",
        "Jadeon",
        "Jasperite",
        "Jetstream",
        "Jungleop",
        "Juniperyn",
        "Kelpfin",
        "Kindlefly",
        "Kingfisher",
        "Knightowl",
        "Knucklehead",
        "Labyrinth",
        "Lagooner",
        "Lavashell",
        "Leafwing",
        "Leapingfrog",
        "Lightbeam",
        "Lightningbug",
        "Limestone",
        "Liquidite",
        "Lunamoth",
        "Magmite",
        "Malachite",
        "Mantisect",
        "Marbleon",
        "Marshwiggle",
        "Maskito",
        "Meadowfin",
        "Megaton",
        "Melodyte",
        "Meteoric",
        "Midnightowl",
        "Mistralyn",
        "Moltenix",
        "Moonbeam",
        "Mossback",
        "Mudskipper",
        "Mysticlaw",
        "Nectarin",
        "Netherfang",
        "Nightshade",
        "Nimbusowl",
        "Nocturne",
        "Novaflare",
        "Nuggeteer",
        "Obsidian",
        "Oceanaut",
        "Opalite",
        "Orbitron",
        "Overgrowth",
        "Oxideon",
        "Ozonefly",
        "Palestone",
        "Panthera",
        "Parallax",
        "Patchleaf",
        "Pebblepuff",
        "Pendulum",
        "Peridot",
        "Phantomist",
        "Phasewalk",
        "Pinecone",
        "Pinwheel",
        "Pixelite",
        "Plainsrunner",
        "Plasmafin",
        "Plumbob",
        "Poisonivy",
        "Polaris",
        "Pollenpuff",
        "Pondskater",
        "Prickleback",
        "Prismite",
        "Pumiceon",
        "Pyrefly",
        "Quakehorn",
        "Quartzite",
        "Quicksilver",
        "Radiant",
        "Raindrop",
        "Raptoros",
        "Razorfin",
        "Reefwalker",
        "Ripplefin",
        "Riverunner",
        "Rockhopper",
        "Rubblebug",
        "Rustmite",
        "Saberfang",
        "Saphireon",
        "Scarabite",
        "Scorchpaw",
        "Seabreeze",
        "Seaslug",
        "Shadowclaw",
        "Sharpfin",
        "Shellshock",
        "Shimmeron",
        "Shockwave",
        "Silicaon",
        "Silverwing",
        "Skitterbug",
        "Skywhale",
        "Slagpile",
        "Sleetfoot",
        "Smoketail",
        "Snaggletooth",
        "Snakeweed",
        "Snowdrift",
        "Solaris",
        "Sonicboom",
        "Sparkfly",
        "Spectrite",
        "Spikeball",
        "Springtail",
        "Stagbeetle",
        "Starblaze",
        "Stonefish",
        "Stormcloud",
        "Streamer",
        "Strikewing",
        "Sunbeam",
        "Sunstone",
        "Swampfin",
        "Swiftail",
        "Sycamore",
        "Tanglefoot",
        "Tarnish",
        "Terraform",
        "Thornback",
        "Thunderbug",
        "Tidalwave",
        "Timberwolf",
        "Tinytail",
        "Topazite",
        "Torrential",
        "Toxiclaw",
        "Tranquil",
        "Tremorix",
        "Tribyte",
        "Tricorne",
        "Twilight",
        "Twisteron",
        "Undergrowth",
        "Undertow",
        "Unicorn",
        "Valiant",
        "Vaporize",
        "Venomite",
        "Veridian",
        "Vibraharp",
        "Volcanic",
        "Voltwing",
        "Vortexon",
        "Wallowby",
        "Warpwing",
        "Waterbug",
        "Wavecrest",
        "Waxwing",
        "Wildfire",
        "Windigo",
        "Wispfire",
        "Woodsprite",
        "Wormhole",
        "Wyvernix",
        "Xenonix",
        "Zephyron",
        "Ziggurat",
        "Zincite",
    )

fun fakePokemonInfo(id: Int, name: String): PokemonInfo {
    val random = Random(name.hashCode())
    return PokemonInfo(
        id = id,
        name = name,
        height = random.nextInt(10, 50),
        weight = random.nextInt(80, 300),
        experience = random.nextInt(0, 100),
        types = listOf(FakePokemonTypeResponse(random)),
        stats = listOf(fakePokemonStats(random)),
    )
}

private var FakePokemonStats = listOf("hp", "attack", "speed", "defense")

private fun fakePokemonStats(random: Random = Random): PokemonInfo.StatsResponse {
    val stat = PokemonInfo.Stat(FakePokemonStats.random())
    val statMax =
        when (stat.name) {
            "hp" -> MAX_HP
            "attack" -> MAX_ATTACK
            "speed" -> MAX_SPEED
            "defense" -> MAX_DEFENSE
            else -> 100
        }
    return PokemonInfo.StatsResponse(
        baseStat = random.nextInt(until = statMax),
        effort = random.nextInt(),
        stat = stat,
    )
}

private var FakePokemonTypes =
    listOf(
        "A slow one",
        "A fast one",
        "A big one",
        "An adorable one",
        "A tiny one",
        "A software-developing one",
    )

private fun FakePokemonTypeResponse(random: Random = Random) =
    PokemonInfo.TypeResponse(
        slot = 0,
        type = PokemonInfo.Type(name = FakePokemonTypes.random(random)),
    )
