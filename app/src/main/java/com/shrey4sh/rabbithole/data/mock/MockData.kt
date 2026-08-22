package com.shrey4sh.rabbithole.data.mock

import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType
import com.shrey4sh.rabbithole.domain.model.RabbitHole

/** Phase 2 mock data — complete demo rabbit holes for offline testing. */
object MockData {

    private fun node(id: String, title: String, type: NodeType, desc: String = "") =
        Node(id = id, title = title, type = type, description = desc)

    private fun edge(from: String, to: String, rel: String) =
        Edge(id = "$from-$to-$rel", sourceNodeId = from, targetNodeId = to, relationship = rel)

    val cyberpunk: RabbitHole = RabbitHole(
        id = "cyberpunk-2077",
        rootNodeId = "cp2077",
        nodes = listOf(
            node("cp2077", "Cyberpunk 2077", NodeType.GAME, "2020 open-world action RPG by CD Projekt Red, set in Night City."),
            node("cdpr", "CD Projekt Red", NodeType.ORGANIZATION, "Polish game studio behind The Witcher series and Cyberpunk 2077."),
            node("nightcity", "Night City", NodeType.PLACE, "Fictional Californian megacity where Cyberpunk 2077 takes place."),
            node("keanu", "Keanu Reeves", NodeType.PERSON, "Played Johnny Silverhand in Cyberpunk 2077."),
            node("blade", "Blade Runner", NodeType.MOVIE, "Ridley Scott's 1982 cyberpunk classic, a major influence on the genre."),
            node("gibson", "William Gibson", NodeType.PERSON, "Author who pioneered the cyberpunk genre with Neuromancer."),
            node("witcher3", "The Witcher 3", NodeType.GAME, "CD Projekt Red's acclaimed 2015 fantasy RPG."),
            node("dystopia", "Dystopian Fiction", NodeType.CONCEPT, "Fictional societies marked by suffering and oppression."),
            node("genre", "Cyberpunk Genre", NodeType.CONCEPT, "High-tech, low-life science fiction subgenre."),
        ),
        edges = listOf(
            edge("cp2077", "cdpr", "CREATED_BY"),
            edge("cp2077", "nightcity", "LOCATED_IN"),
            edge("cp2077", "keanu", "MEMBER_OF"),
            edge("cp2077", "genre", "SAME_GENRE"),
            edge("genre", "gibson", "CREATED_BY"),
            edge("genre", "blade", "INSPIRED_BY"),
            edge("cp2077", "blade", "INSPIRED_BY"),
            edge("gibson", "blade", "INFLUENCED"),
            edge("cdpr", "witcher3", "WORKED_ON"),
            edge("cp2077", "dystopia", "BASED_ON"),
            edge("genre", "dystopia", "RELATED_TO"),
        ),
    )

    val ai: RabbitHole = RabbitHole(
        id = "artificial-intelligence",
        rootNodeId = "ai",
        nodes = listOf(
            node("ai", "Artificial Intelligence", NodeType.TECHNOLOGY, "Machines performing tasks that typically require human intelligence."),
            node("turing", "Alan Turing", NodeType.PERSON, "Father of theoretical computer science and AI."),
            node("neural", "Neural Networks", NodeType.TECHNOLOGY, "Computing systems inspired by biological brains."),
            node("gpt", "Large Language Models", NodeType.TECHNOLOGY, "Models trained on vast text to generate and understand language."),
            node("deeplearn", "Deep Learning", NodeType.CONCEPT, "Multi-layered neural network learning."),
            node("imitation", "Imitation Game", NodeType.CONCEPT, "Turing's 1950 test for machine intelligence."),
            node("mit", "MIT", NodeType.ORGANIZATION, "Pioneering AI research university."),
            node("robotics", "Robotics", NodeType.TECHNOLOGY, "Machines that act in the physical world."),
        ),
        edges = listOf(
            edge("ai", "turing", "CREATED_BY"),
            edge("turing", "imitation", "WORKED_ON"),
            edge("ai", "neural", "BASED_ON"),
            edge("neural", "deeplearn", "BASED_ON"),
            edge("ai", "gpt", "BASED_ON"),
            edge("gpt", "neural", "BASED_ON"),
            edge("ai", "mit", "LOCATED_IN"),
            edge("ai", "robotics", "RELATED_TO"),
        ),
    )

    val joji: RabbitHole = RabbitHole(
        id = "joji",
        rootNodeId = "joji",
        nodes = listOf(
            node("joji", "Joji", NodeType.MUSIC, "Japanese singer-songwriter, former YouTube comedian George Miller."),
            node("filthyfrank", "Filthy Frank", NodeType.PERSON, "Miller's absurdist YouTube persona before music."),
            node("slowdancing", "Slow Dancing in the Dark", NodeType.MUSIC, "Joji's breakout 2018 single."),
            node("88rising", "88rising", NodeType.ORGANIZATION, "Label that represents Asian artists including Joji."),
            node("lofi", "Lo-fi Music", NodeType.CONCEPT, "Mellow production style associated with Joji's sound."),
            node("osaka", "Osaka", NodeType.PLACE, "Japanese city where Joji was born."),
        ),
        edges = listOf(
            edge("joji", "filthyfrank", "RELATED_TO"),
            edge("joji", "slowdancing", "WORKED_ON"),
            edge("joji", "88rising", "MEMBER_OF"),
            edge("joji", "lofi", "SAME_GENRE"),
            edge("joji", "osaka", "LOCATED_IN"),
        ),
    )

    val delhi: RabbitHole = RabbitHole(
        id = "delhi",
        rootNodeId = "delhi",
        nodes = listOf(
            node("delhi", "Delhi", NodeType.PLACE, "Capital territory of India, historic metropolis."),
            node("redfort", "Red Fort", NodeType.PLACE, "Mughal fortress built by Shah Jahan in 1648."),
            node("mughal", "Mughal Empire", NodeType.EVENT, "Empire that ruled much of India 1526–1857."),
            node("chandnichowk", "Chandni Chowk", NodeType.PLACE, "Old Delhi's historic market street."),
            node("metro", "Delhi Metro", NodeType.TECHNOLOGY, "One of the world's largest metro networks."),
            node("shahjahan", "Shah Jahan", NodeType.PERSON, "Mughal emperor who built the Red Fort and Taj Mahal."),
        ),
        edges = listOf(
            edge("delhi", "redfort", "LOCATED_IN"),
            edge("redfort", "mughal", "CREATED_BY"),
            edge("shahjahan", "mughal", "MEMBER_OF"),
            edge("shahjahan", "redfort", "WORKED_ON"),
            edge("delhi", "chandnichowk", "LOCATED_IN"),
            edge("delhi", "metro", "BASED_ON"),
        ),
    )

    val blackholes: RabbitHole = RabbitHole(
        id = "black-holes",
        rootNodeId = "bh",
        nodes = listOf(
            node("bh", "Black Holes", NodeType.CONCEPT, "Regions of spacetime with gravity so strong nothing escapes."),
            node("hawking", "Stephen Hawking", NodeType.PERSON, "Theorized Hawking radiation from black holes."),
            node("einstein", "Albert Einstein", NodeType.PERSON, "General relativity predicted black holes."),
            node("eventhorizon", "Event Horizon", NodeType.CONCEPT, "Boundary beyond which nothing can return."),
            node("singularity", "Singularity", NodeType.CONCEPT, "Point of infinite density at a black hole's center."),
            node("eht", "Event Horizon Telescope", NodeType.TECHNOLOGY, "Imaged the first black hole in 2019."),
        ),
        edges = listOf(
            edge("bh", "hawking", "RELATED_TO"),
            edge("hawking", "einstein", "INFLUENCED"),
            edge("einstein", "bh", "CREATED_BY"),
            edge("bh", "eventhorizon", "BASED_ON"),
            edge("bh", "singularity", "BASED_ON"),
            edge("eht", "bh", "WORKED_ON"),
        ),
    )

    val ww2: RabbitHole = RabbitHole(
        id = "world-war-ii",
        rootNodeId = "ww2",
        nodes = listOf(
            node("ww2", "World War II", NodeType.EVENT, "Global war fought 1939–1945."),
            node("enigma", "Enigma Machine", NodeType.TECHNOLOGY, "German cipher machine broken at Bletchley Park."),
            node("turing2", "Alan Turing", NodeType.PERSON, "Led the team that cracked Enigma."),
            node("churchill", "Winston Churchill", NodeType.PERSON, "UK Prime Minister through most of WWII."),
            node("pearlharbor", "Pearl Harbor", NodeType.EVENT, "1941 attack that brought the US into the war."),
            node("holocaust", "The Holocaust", NodeType.EVENT, "Genocide perpetrated by Nazi Germany."),
        ),
        edges = listOf(
            edge("enigma", "ww2", "OCCURRED_IN"),
            edge("turing2", "enigma", "WORKED_ON"),
            edge("churchill", "ww2", "MEMBER_OF"),
            edge("pearlharbor", "ww2", "OCCURRED_IN"),
            edge("holocaust", "ww2", "OCCURRED_IN"),
        ),
    )

    val f1: RabbitHole = RabbitHole(
        id = "formula-1",
        rootNodeId = "f1",
        nodes = listOf(
            node("f1", "Formula 1", NodeType.GAME, "FIA's premier single-seater racing championship."),
            node("hamilton", "Lewis Hamilton", NodeType.PERSON, "7-time world champion."),
            node("verstappen", "Max Verstappen", NodeType.PERSON, "Red Bull's multiple world champion."),
            node("redbull", "Red Bull Racing", NodeType.ORGANIZATION, "Austrian F1 team."),
            node("monaco", "Monaco Grand Prix", NodeType.EVENT, "The most famous street circuit in F1."),
            node("senna", "Ayrton Senna", NodeType.PERSON, "Three-time champion, considered among the greatest."),
        ),
        edges = listOf(
            edge("hamilton", "f1", "MEMBER_OF"),
            edge("verstappen", "f1", "MEMBER_OF"),
            edge("verstappen", "redbull", "MEMBER_OF"),
            edge("monaco", "f1", "OCCURRED_IN"),
            edge("senna", "f1", "MEMBER_OF"),
        ),
    )

    val all: Map<String, RabbitHole> = listOf(cyberpunk, ai, joji, delhi, blackholes, ww2, f1)
        .associateBy { it.id }

    fun search(query: String): RabbitHole? {
        val q = query.lowercase().trim()
        return all.entries.firstOrNull { (id, hole) ->
            q in id || hole.nodes.any { it.title.lowercase().contains(q) }
        }?.value
    }

    fun random(): RabbitHole = all.values.random()
}
