package com.example.carapp.ui.addcar

import java.text.Normalizer
import java.util.Locale

/** Curated offline suggestions, including older models. Manual entry covers missing vehicles. */
object VehicleSuggestions {
    private val modelsByBrand: Map<String, List<String>> = """
        Abarth|500|595|695|124 Spider|Grande Punto|Punto Evo
        Alfa Romeo|33|75|145|146|147|155|156|159|164|166|Brera|GT|GTV|Spider|MiTo|Giulietta|Giulia|Stelvio|Tonale
        Aston Martin|DB7|DB9|DB11|DB12|DBS|Vantage|Vanquish|Rapide|DBX
        Audi|80|90|100|200|A1|A2|A3|A4|A5|A6|A7|A8|TT|R8|Q2|Q3|Q4 e-tron|Q5|Q7|Q8|e-tron|e-tron GT
        BMW|Seria 1|Seria 2|Seria 3|Seria 4|Seria 5|Seria 6|Seria 7|Seria 8|X1|X2|X3|X4|X5|X6|X7|Z3|Z4|i3|i4|i5|i7|i8|iX|iX1|iX3
        Bentley|Continental GT|Continental Flying Spur|Flying Spur|Bentayga|Arnage|Mulsanne
        BYD|Atto 3|Dolphin|Seal|Han|Tang|Seal U
        Chevrolet|Spark|Matiz|Aveo|Lacetti|Cruze|Epica|Malibu|Captiva|Orlando|Trax|Camaro|Corvette
        Chrysler|300C|300M|Voyager|Grand Voyager|Pacifica|PT Cruiser|Sebring|Crossfire|Neon
        Citroën|AX|BX|CX|ZX|Saxo|Xsara|Xsara Picasso|Xantia|C1|C2|C3|C3 Picasso|C3 Aircross|C4|C4 Picasso|Grand C4 Picasso|C4 Cactus|C4 X|C5|C5 Aircross|C5 X|C6|Berlingo|Nemo|Jumpy|Jumper
        Cupra|Ateca|Leon|Formentor|Born|Tavascan|Terramar
        Dacia|Logan|Sandero|Duster|Lodgy|Dokker|Spring|Jogger
        Daewoo|Matiz|Tico|Lanos|Nexia|Nubira|Leganza|Espero|Tacuma
        Daihatsu|Cuore|Sirion|Charade|Terios|Materia|YRV|Feroza|Rocky
        Dodge|Challenger|Charger|Caliber|Journey|Avenger|Nitro|Durango|Caravan|Grand Caravan|RAM
        DS|DS 3|DS 3 Crossback|DS 4|DS 5|DS 7|DS 7 Crossback|DS 9
        Ferrari|348|355|360|F430|458|488|California|Portofino|Roma|F8|812|296|SF90|Purosangue
        Fiat|126p|125p|127|128|Uno|Cinquecento|Seicento|Panda|Punto|Grande Punto|Punto Evo|Tipo|Tempra|Bravo|Brava|Marea|Stilo|Linea|Croma|500|500L|500X|Doblo|Fiorino|Qubo|Multipla|Freemont|Ducato
        Ford|Ka|Ka+|Fiesta|Fusion|Escort|Focus|Sierra|Scorpio|Mondeo|C-Max|Grand C-Max|S-Max|Galaxy|B-Max|EcoSport|Puma|Kuga|Edge|Explorer|Mustang|Mustang Mach-E|Ranger|Tourneo Connect|Tourneo Custom|Transit|Transit Connect|Transit Custom
        Honda|Civic|Accord|Jazz|City|CR-V|HR-V|FR-V|CR-Z|CRX|Prelude|Insight|Legend|S2000|e|ZR-V
        Hyundai|Atos|Getz|Accent|Elantra|Lantra|Sonata|i10|i20|i30|i40|ix20|ix35|Tucson|Kona|Santa Fe|Terracan|Trajet|Veloster|Ioniq|Ioniq 5|Ioniq 6|H-1
        Infiniti|G|M|EX|FX|Q30|Q50|Q60|Q70|QX30|QX50|QX70|QX80
        Isuzu|D-Max|Trooper|Gemini|Campo
        Iveco|Daily|Massif
        Jaguar|X-Type|S-Type|XJ|XK|XE|XF|F-Type|E-Pace|F-Pace|I-Pace
        Jeep|Avenger|Renegade|Compass|Cherokee|Grand Cherokee|Wrangler|Commander|Patriot|Gladiator
        Kia|Pride|Picanto|Rio|Venga|Soul|Ceed|ProCeed|XCeed|Cerato|Shuma|Sephia|Magentis|Optima|Stinger|Carens|Carnival|Sportage|Sorento|Niro|Stonic|EV3|EV6|EV9
        Lada|2101|2105|2107|Samara|Niva|Kalina|Priora|Granta|Vesta
        Lamborghini|Gallardo|Huracán|Murciélago|Aventador|Urus|Revuelto
        Lancia|Y|Ypsilon|Delta|Dedra|Lybra|Kappa|Thesis|Thema|Musa|Phedra|Voyager
        Land Rover|Defender|Freelander|Freelander 2|Discovery|Discovery Sport|Range Rover|Range Rover Sport|Range Rover Evoque|Range Rover Velar
        Lexus|CT|IS|ES|GS|LS|RC|LC|SC|UX|NX|RX|GX|LX|RZ|LBX
        Maserati|Ghibli|Quattroporte|GranTurismo|GranCabrio|Levante|Grecale|MC20
        Mazda|121|323|626|929|2|3|5|6|CX-3|CX-30|CX-5|CX-7|CX-9|CX-60|CX-80|MX-3|MX-5|MX-6|MX-30|RX-7|RX-8|Premacy|MPV|Tribute
        Mercedes-Benz|190|W123|W124|Klasa A|Klasa B|Klasa C|Klasa E|Klasa S|Klasa G|Klasa V|CLA|CLS|CLK|CLC|CL|SL|SLK|SLC|GLA|GLB|GLC|GLE|GLS|GLK|ML|GL|EQA|EQB|EQC|EQE|EQS|EQV|Vaneo|Viano|Vito|Citan|Sprinter
        MG|ZR|ZS|ZT|TF|3|4|5|6|HS|EHS|Marvel R|Cyberster
        MINI|One|Cooper|Cooper S|Clubman|Countryman|Paceman|Cabrio|Coupe|Roadster|Aceman
        Mitsubishi|Colt|Lancer|Carisma|Galant|Space Star|Space Wagon|Grandis|ASX|Outlander|Eclipse|Eclipse Cross|Pajero|Pajero Pinin|L200|i-MiEV
        Nissan|Micra|Note|Almera|Primera|Sunny|Tiida|Pulsar|Juke|Qashqai|Qashqai+2|X-Trail|Murano|Pathfinder|Patrol|Terrano|Navara|Leaf|Ariya|350Z|370Z|GT-R|NV200|Townstar
        Opel|Adam|Agila|Karl|Corsa|Astra|Vectra|Insignia|Omega|Signum|Calibra|Tigra|Meriva|Zafira|Mokka|Crossland|Crossland X|Grandland|Grandland X|Antara|Frontera|Combo|Vivaro|Movano
        Peugeot|106|107|108|205|206|207|208|306|307|308|309|405|406|407|408|508|607|806|807|1007|2008|3008|5008|RCZ|Partner|Rifter|Expert|Traveller|Boxer
        Polestar|1|2|3|4
        Pontiac|Firebird|Trans Am|Grand Am|Grand Prix|G6|Vibe|Trans Sport
        Porsche|911|924|928|944|968|Boxster|Cayman|718|Cayenne|Macan|Panamera|Taycan
        Renault|5|9|11|19|21|25|Clio|Thalia|Megane|Laguna|Safrane|Vel Satis|Talisman|Fluence|Modus|Grand Modus|Scenic|Grand Scenic|Espace|Captur|Kadjar|Koleos|Arkana|Austral|Zoe|Twingo|Kangoo|Trafic|Master
        Rover|25|45|75|100|200|400|600|800|Streetwise
        Saab|900|9000|9-3|9-5|9-7X
        Seat|Marbella|Arosa|Mii|Ibiza|Cordoba|Leon|Toledo|Altea|Altea XL|Exeo|Alhambra|Arona|Ateca|Tarraco
        Škoda|105|120|Favorit|Forman|Felicia|Fabia|Roomster|Rapid|Scala|Octavia|Superb|Yeti|Kamiq|Karoq|Kodiaq|Enyaq|Elroq|Citigo
        Smart|Fortwo|Forfour|Roadster|#1|#3
        SsangYong|Tivoli|Korando|Rexton|Kyron|Actyon|Rodius|Musso|Torres
        Subaru|Justy|Impreza|Legacy|Outback|Forester|XV|Crosstrek|Tribeca|Levorg|BRZ|WRX|Solterra
        Suzuki|Alto|Celerio|Swift|Baleno|Ignis|Splash|Wagon R+|SX4|SX4 S-Cross|S-Cross|Vitara|Grand Vitara|Jimny|Samurai|Liana|Kizashi|Swace|Across
        Tesla|Model S|Model 3|Model X|Model Y|Roadster|Cybertruck
        Toyota|Aygo|Aygo X|Yaris|Yaris Cross|Starlet|Corolla|Corolla Cross|Auris|Avensis|Carina|Camry|Prius|Verso|Corolla Verso|Urban Cruiser|C-HR|RAV4|Highlander|Land Cruiser|Hilux|4Runner|Celica|Supra|GT86|GR86|MR2|bZ4X|Proace|Proace City
        Volkswagen|Lupo|Fox|up!|Polo|Golf|Golf Plus|Sportsvan|Jetta|Vento|Bora|Passat|Arteon|Phaeton|Scirocco|Beetle|New Beetle|Eos|T-Cross|Taigo|T-Roc|Tiguan|Touareg|Touran|Sharan|Caddy|Transporter|Multivan|Caravelle|Crafter|Amarok|ID.3|ID.4|ID.5|ID.7|ID. Buzz
        Volvo|240|340|440|460|480|740|760|850|940|960|C30|C70|S40|S60|S70|S80|S90|V40|V50|V60|V70|V90|XC40|XC60|XC70|XC90|EX30|EX90
    """.trimIndent().lineSequence().associate { line ->
        val values = line.split('|')
        values.first() to values.drop(1).distinct().sorted()
    }.toSortedMap(compareBy { searchKey(it) })

    val brands: List<String> = modelsByBrand.keys.toList()
    fun models(brand: String): List<String> =
        modelsByBrand.entries.firstOrNull { searchKey(it.key) == searchKey(brand.trim()) }?.value.orEmpty()

    /** Searching without accents also finds Škoda and Citroën on a Polish keyboard. */
    fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace('ł', 'l')
}
