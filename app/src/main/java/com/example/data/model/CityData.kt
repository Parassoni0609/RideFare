package com.example.data.model

object CityData {
    val supportedCities = listOf(
        CityInfo(
            id = "bengaluru",
            name = "Bengaluru",
            popularLocations = listOf(
                PresetLocation("Koramangala 4th Block", "Near Sony World Junction, 80ft Road", 12.9344, 77.6253, "general"),
                PresetLocation("Indiranagar 100ft Road", "Metro Station Exit A, CMH Road", 12.9784, 77.6408, "metro"),
                PresetLocation("Kempegowda Int'l Airport (BLR)", "Terminal 1 & 2 Departure, Devanahalli", 13.1986, 77.7066, "airport"),
                PresetLocation("HSR Layout Sector 1", "Agara Lake Road, 27th Main", 12.9121, 77.6446, "general"),
                PresetLocation("Whitefield ITPL", "International Tech Park, Pattandur Agrahara", 12.9863, 77.7337, "work"),
                PresetLocation("Krantivira Sangolli Rayanna (Majestic)", "Platform 1 Exit & KSRTC Bus Stand", 12.9781, 77.5695, "transit"),
                PresetLocation("Manyata Tech Park", "Nagavara Outer Ring Road Gate 1", 13.0489, 77.6226, "work"),
                PresetLocation("Electronic City Phase 1", "Wipro Gate 5, Hosur Road", 12.8399, 77.6770, "work"),
                PresetLocation("MG Road Metro Station", "Church Street / Brigade Road Junction", 12.9754, 77.6066, "metro"),
                PresetLocation("Bellandur Central", "EcoSpace Business Park, Outer Ring Road", 12.9260, 77.6762, "work"),
                PresetLocation("Jayanagar 4th Block", "Near BDA Shopping Complex & Cool Joint", 12.9299, 77.5824, "shopping"),
                PresetLocation("Hebbal Flyover", "Outer Ring Road Junction & Lake Park", 13.0358, 77.5970, "transit")
            )
        ),
        CityInfo(
            id = "delhi",
            name = "Delhi NCR",
            popularLocations = listOf(
                PresetLocation("Connaught Place (CP)", "Inner Circle Block A / Rajiv Chowk", 28.6304, 77.2177, "general"),
                PresetLocation("Gurgaon Cyber Hub", "DLF Cyber City Phase 2 & Rapid Metro", 28.4950, 77.0895, "work"),
                PresetLocation("Indira Gandhi Airport (DEL)", "Terminal 3 Departure Gate 4", 28.5562, 77.1000, "airport"),
                PresetLocation("Noida Sector 18", "Wave Silver Tower & DLF Mall of India", 28.5708, 77.3260, "shopping"),
                PresetLocation("New Delhi Railway Station", "Paharganj Side & Ajmeri Gate", 28.6431, 77.2197, "transit"),
                PresetLocation("Hauz Khas Social", "Hauz Khas Village & Deer Park", 28.5494, 77.1932, "general"),
                PresetLocation("Saket Select Citywalk", "A-3 District Centre, Press Enclave Rd", 28.5284, 77.2195, "shopping"),
                PresetLocation("Noida Sector 62", "Logix Cyber Park & Electronic City Metro", 28.6258, 77.3687, "work")
            )
        ),
        CityInfo(
            id = "mumbai",
            name = "Mumbai",
            popularLocations = listOf(
                PresetLocation("Bandra Kurla Complex (BKC)", "MMRDA Grounds / Jio World Convention", 19.0664, 72.8687, "work"),
                PresetLocation("Chhatrapati Shivaji Maharaj Airport (BOM)", "Terminal 2 International Departure", 19.0974, 72.8745, "airport"),
                PresetLocation("Andheri West", "Lokhandwala Complex & Infinity Mall", 19.1363, 72.8277, "general"),
                PresetLocation("Marine Drive", "Nariman Point Promenade & Oberoi", 18.9272, 72.8236, "general"),
                PresetLocation("CSMT Railway Station", "Fort, Heritage Terminus Concourse", 18.9401, 72.8355, "transit"),
                PresetLocation("Powai Hiranandani", "Central Avenue Galleria & IIT Bombay", 19.1197, 72.9051, "work"),
                PresetLocation("Lower Parel High Street Phoenix", "Palladium Mall, Senapati Bapat Marg", 18.9953, 72.8252, "shopping"),
                PresetLocation("Thane Viviana Mall", "Eastern Express Highway, Majiwada", 19.2088, 72.9715, "shopping")
            )
        ),
        CityInfo(
            id = "hyderabad",
            name = "Hyderabad",
            popularLocations = listOf(
                PresetLocation("Hitec City Cyber Towers", "Mindspace Road & Metro Station", 17.4474, 78.3762, "work"),
                PresetLocation("Rajiv Gandhi Airport (HYD)", "Shamshabad Departure Ramp Gate 1", 17.2403, 78.4294, "airport"),
                PresetLocation("Gachibowli Stadium", "ORR Junction & Financial District", 17.4401, 78.3489, "work"),
                PresetLocation("Banjara Hills Road No. 12", "Care Hospital Circle & City Centre Mall", 17.4123, 78.4483, "general"),
                PresetLocation("Jubilee Hills Check Post", "Road No. 36 & Peddamma Temple Metro", 17.4325, 78.4072, "shopping"),
                PresetLocation("Secunderabad Railway Station", "Main Entrance Gate Platform 1", 17.4338, 78.5015, "transit"),
                PresetLocation("Charminar Old City", "Near Mecca Masjid & Laad Bazaar", 17.3616, 78.4747, "general")
            )
        ),
        CityInfo(
            id = "pune",
            name = "Pune",
            popularLocations = listOf(
                PresetLocation("Koregaon Park North Main Rd", "Lane 5 Osho Corner & German Bakery", 18.5362, 73.8940, "general"),
                PresetLocation("Hinjewadi Phase 1", "Wipro Circle & Infosys Campus Rd", 18.5913, 73.7389, "work"),
                PresetLocation("Pune International Airport (PNQ)", "Lohegaon Departure Terminal", 18.5822, 73.9197, "airport"),
                PresetLocation("Pune Junction Station", "Main Concourse & Sassoon Hospital Rd", 18.5284, 73.8744, "transit"),
                PresetLocation("Viman Nagar Phoenix Marketcity", "Ahmednagar Highway & Symbiosis", 18.5620, 73.9167, "shopping"),
                PresetLocation("FC Road (Fergusson College)", "Deccan Gymkhana & Goodluck Cafe", 18.5236, 73.8415, "general"),
                PresetLocation("Baner Balewadi High Street", "Cummins India Office Junction", 18.5694, 73.7749, "shopping")
            )
        ),
        CityInfo(
            id = "chennai",
            name = "Chennai",
            popularLocations = listOf(
                PresetLocation("T. Nagar Pondy Bazaar", "Near Panagal Park & Shopping St", 13.0418, 80.2341, "shopping"),
                PresetLocation("Chennai International Airport (MAA)", "T2 Domestic Departure Ramp", 12.9941, 80.1709, "airport"),
                PresetLocation("OMR IT Expressway", "Tidel Park Tharamani & Ascendas", 12.9897, 80.2483, "work"),
                PresetLocation("Chennai Central Station", "Puratchi Thalaivar Dr. MGR Central", 13.0827, 80.2755, "transit"),
                PresetLocation("Marina Beach Promenade", "Light House Circle & Kamarajar Salai", 13.0499, 80.2824, "general"),
                PresetLocation("Velachery Phoenix Marketcity", "Guru Nanak College Road", 12.9915, 80.2173, "shopping")
            )
        ),
        CityInfo(
            id = "kolkata",
            name = "Kolkata",
            popularLocations = listOf(
                PresetLocation("Park Street", "Near Flurys & Allen Park", 22.5510, 88.3526, "general"),
                PresetLocation("Netaji Subhash Airport (CCU)", "Dum Dum Departure Terminal", 22.6547, 88.4467, "airport"),
                PresetLocation("Salt Lake Sector V", "College More & Webel Bhavan IT Hub", 22.5804, 88.4378, "work"),
                PresetLocation("Howrah Railway Station", "Platform 1 Complex & Ferry Ghat", 22.5830, 88.3426, "transit"),
                PresetLocation("New Town Eco Park", "Major Arterial Road, Action Area II", 22.6074, 88.4682, "shopping"),
                PresetLocation("South City Mall", "Prince Anwar Shah Road, Jadavpur", 22.4998, 88.3629, "shopping")
            )
        ),
        CityInfo(
            id = "ahmedabad",
            name = "Ahmedabad",
            popularLocations = listOf(
                PresetLocation("SG Highway", "Prahlad Nagar Junction & Iskcon Cross", 23.0135, 72.5085, "work"),
                PresetLocation("Sardar Vallabhbhai Airport (AMD)", "Hansol Departure Gate", 23.0734, 72.6266, "airport"),
                PresetLocation("Sabarmati Riverfront", "Ashram Road Promenade Entrance", 23.0360, 72.5786, "general"),
                PresetLocation("Ahmedabad Junction (Kalupur)", "Main Railway Station Concourse", 23.0232, 72.5997, "transit"),
                PresetLocation("Vastrapur Lake / Alpha One", "Near IIM Ahmedabad New Campus", 23.0369, 72.5287, "shopping"),
                PresetLocation("GIFT City", "Gujarat International Finance Tec-City", 23.1610, 72.6840, "work")
            )
        ),
        CityInfo(
            id = "jaipur",
            name = "Jaipur",
            popularLocations = listOf(
                PresetLocation("MI Road / Panch Batti", "Near Raj Mandir Cinema & Lassiwala", 26.9157, 75.8118, "general"),
                PresetLocation("Jaipur International Airport (JAI)", "Sanganer Terminal 2 Departure", 26.8289, 75.8056, "airport"),
                PresetLocation("Malviya Nagar / WTP", "World Trade Park, JLN Marg", 26.8539, 75.8053, "shopping"),
                PresetLocation("Jaipur Junction Station", "Main Exit Station Road, Hasanpura", 26.9196, 75.7878, "transit"),
                PresetLocation("Vaishali Nagar", "Amrapali Circle & National Handloom", 26.9068, 75.7429, "general"),
                PresetLocation("Hawa Mahal Old City", "Badi Chaupar, Johari Bazar", 26.9239, 75.8267, "general"),
                PresetLocation("Mansarovar Metro Station", "Near VT Road & RIICO Industrial", 26.8660, 75.7600, "metro")
            )
        ),
        CityInfo(
            id = "chandigarh",
            name = "Chandigarh Tri-City",
            popularLocations = listOf(
                PresetLocation("Sector 17 Plaza", "Central Business District & Fountain", 30.7398, 76.7827, "shopping"),
                PresetLocation("Shaheed Bhagat Singh Airport (IXC)", "Mohali International Terminal", 30.6735, 76.7885, "airport"),
                PresetLocation("Sukhna Lake", "Promenade Entrance & Lake Club", 30.7421, 76.8188, "general"),
                PresetLocation("Chandigarh Railway Station", "Daria Industrial Area Gate", 30.7056, 76.8219, "transit"),
                PresetLocation("Elante Mall Industrial Area", "Phase 1, Purv Marg", 30.7055, 76.8013, "shopping"),
                PresetLocation("Mohali Phase 7", "Near Phase 7 Market & Fortis", 30.7046, 76.7179, "general")
            )
        ),
        CityInfo(
            id = "lucknow",
            name = "Lucknow",
            popularLocations = listOf(
                PresetLocation("Hazratganj", "Main Market & GPO Circle", 26.8467, 80.9462, "shopping"),
                PresetLocation("Chaudhary Charan Singh Airport (LKO)", "Amausi Terminal 2 Departure", 26.7606, 80.8893, "airport"),
                PresetLocation("Gomti Nagar / Patrakarpuram", "Near Riverside Mall & Wave", 26.8524, 80.9992, "general"),
                PresetLocation("Charbagh Railway Station", "Main Concourse & Metro Station", 26.8315, 80.9232, "transit"),
                PresetLocation("Phoenix Palassio Mall", "Amar Shaheed Path, Sector 7", 26.8124, 81.0118, "shopping"),
                PresetLocation("Alambagh Bus Terminal", "Near Alambagh Metro Station", 26.8170, 80.9080, "transit")
            )
        ),
        CityInfo(
            id = "indore",
            name = "Indore",
            popularLocations = listOf(
                PresetLocation("Vijay Nagar Square", "AB Road & Brilliant Convention Centre", 22.7533, 75.8937, "work"),
                PresetLocation("Devi Ahilya Bai Airport (IDR)", "Aerodrum Road Departure Gate", 22.7247, 75.8052, "airport"),
                PresetLocation("56 Dukan (Chhappan)", "New Palasia Food Street", 22.7244, 75.8839, "general"),
                PresetLocation("Indore Junction Station", "Platform 1 Main Station Road", 22.7176, 75.8682, "transit"),
                PresetLocation("Treasure Island Mall", "MG Road, South Tukoganj", 22.7214, 75.8778, "shopping"),
                PresetLocation("Bhawarkua Square", "Near DAVV Campus & AB Road", 22.6934, 75.8660, "general")
            )
        ),
        CityInfo(
            id = "surat",
            name = "Surat",
            popularLocations = listOf(
                PresetLocation("Ghod Dod Road", "Kakadiya Complex & Rangila Park", 21.1738, 72.8028, "shopping"),
                PresetLocation("Surat International Airport (STV)", "Dumas Road Departure Terminal", 21.1139, 72.7419, "airport"),
                PresetLocation("VR Surat Mall", "Dumas Road, Magdalla", 21.1444, 72.7567, "shopping"),
                PresetLocation("Surat Railway Station", "Varachha Main Concourse", 21.2049, 72.8407, "transit"),
                PresetLocation("Adajan / Anand Mahal Road", "Near LP Savani Circle", 21.1959, 72.7933, "general"),
                PresetLocation("Diamond Bourse (Khajod)", "DREAM City Surat", 21.1060, 72.7980, "work")
            )
        ),
        CityInfo(
            id = "patna",
            name = "Patna",
            popularLocations = listOf(
                PresetLocation("Dak Bungalow Crossing", "Frazer Road & Bailey Road", 25.6090, 85.1376, "general"),
                PresetLocation("Jay Prakash Airport (PAT)", "Shaheed Pir Ali Khan Marg", 25.5913, 85.0880, "airport"),
                PresetLocation("Patna Junction Station", "Main Station Roundabout", 25.6026, 85.1322, "transit"),
                PresetLocation("Boring Road Crossing", "Near Nageshwar Colony & P&M Mall", 25.6186, 85.1147, "shopping"),
                PresetLocation("Kankarbagh Main Road", "Near Tempo Stand & Tiwari Bechar", 25.5980, 85.1580, "general"),
                PresetLocation("Gandhi Maidan", "Exhibition Road Corner Gate", 25.6207, 85.1444, "general")
            )
        ),
        CityInfo(
            id = "bhopal",
            name = "Bhopal",
            popularLocations = listOf(
                PresetLocation("MP Nagar Zone 1 & 2", "DB City Mall & Board Office Square", 23.2332, 77.4343, "shopping"),
                PresetLocation("Raja Bhoj Airport (BHO)", "Gandhi Nagar Terminal", 23.2875, 77.3378, "airport"),
                PresetLocation("Bhopal Junction Station", "Platform 1 Hamidia Road", 23.2657, 77.4116, "transit"),
                PresetLocation("VIP Road / Upper Lake", "Bada Talab Boat Club", 23.2500, 77.3850, "general"),
                PresetLocation("Rani Kamlapati Station (RKMP)", "Habibganj Ultra-Modern Concourse", 23.2081, 77.4394, "transit")
            )
        ),
        CityInfo(
            id = "nagpur",
            name = "Nagpur",
            popularLocations = listOf(
                PresetLocation("Dharampeth / Coffee House", "West High Court Road", 21.1447, 79.0625, "general"),
                PresetLocation("Dr. Babasaheb Ambedkar Airport (NAG)", "Sonegaon Departure Gate", 21.0922, 79.0472, "airport"),
                PresetLocation("Nagpur Railway Station", "Sitabuldi Main Entrance", 21.1524, 79.0888, "transit"),
                PresetLocation("MIHAN SEZ / Infosys Gate", "Wardha Road Multi-Modal Hub", 21.0340, 79.0480, "work"),
                PresetLocation("VR Nagpur Mall", "Medical Square, Rambagh", 21.1350, 79.0960, "shopping")
            )
        ),
        CityInfo(
            id = "kochi",
            name = "Kochi (Cochin)",
            popularLocations = listOf(
                PresetLocation("MG Road / Marine Drive", "Rainbow Bridge Promenade", 9.9790, 76.2750, "general"),
                PresetLocation("Cochin International Airport (COK)", "Nedumbassery T3 Departure", 10.1556, 76.3917, "airport"),
                PresetLocation("Lulu International Mall", "Edappally Toll Junction", 10.0279, 76.3081, "shopping"),
                PresetLocation("Kakkanad InfoPark", "Phase 1 & 2 IT Express Highway", 10.0094, 76.3639, "work"),
                PresetLocation("Ernakulam South Station", "Platform 1 Exit, Karshaka Road", 9.9678, 76.2908, "transit"),
                PresetLocation("Fort Kochi Beach", "Chinese Fishing Nets Promenade", 9.9658, 76.2415, "general")
            )
        ),
        CityInfo(
            id = "goa",
            name = "Goa",
            popularLocations = listOf(
                PresetLocation("Panaji Church Square", "18th June Road & Fontainhas", 15.4989, 73.8278, "general"),
                PresetLocation("Manohar Int'l Airport (MOPA/GOX)", "Pernem Departure Ramp", 15.7483, 73.8647, "airport"),
                PresetLocation("Dabolim Airport (GOI)", "Vasco da Gama Departure Gate", 15.3808, 73.8314, "airport"),
                PresetLocation("Calangute / Baga Beach Road", "Tito's Lane Junction", 15.5494, 73.7535, "general"),
                PresetLocation("Candolim Main Road", "Near Delfino's Supermarket", 15.5186, 73.7667, "shopping"),
                PresetLocation("Madgaon Railway Station", "Margao Junction Main Exit", 15.2694, 73.9744, "transit")
            )
        ),
        CityInfo(
            id = "varanasi",
            name = "Varanasi",
            popularLocations = listOf(
                PresetLocation("Godowlia Chowk / Dashashwamedh", "Ghat Main Road Entrance", 25.3076, 83.0062, "general"),
                PresetLocation("Lal Bahadur Shastri Airport (VNS)", "Babatpur Departure Gate", 25.4524, 82.8594, "airport"),
                PresetLocation("Varanasi Junction (Cantonment)", "Main Railway Concourse", 25.3268, 82.9862, "transit"),
                PresetLocation("BHU Lanka Gate", "Banaras Hindu University Main Road", 25.2785, 82.9984, "general"),
                PresetLocation("Assi Ghat Promenade", "Near Pizzeria Vaatika Cafe", 25.2890, 83.0068, "general")
            )
        ),
        CityInfo(
            id = "dehradun",
            name = "Dehradun",
            popularLocations = listOf(
                PresetLocation("Clock Tower (Ghanta Ghar)", "Rajpur Road & Paltan Bazar", 30.3256, 78.0437, "general"),
                PresetLocation("Jolly Grant Airport (DED)", "Dehradun-Rishikesh Highway", 30.1897, 78.1803, "airport"),
                PresetLocation("Dehradun Railway Station", "Haridwar Road Main Concourse", 30.3150, 78.0322, "transit"),
                PresetLocation("Pacific Mall Rajpur Road", "Near Jakhan & Scholars Home", 30.3660, 78.0700, "shopping"),
                PresetLocation("ISBT Dehradun", "Saharanpur Road Transport Hub", 30.2860, 78.0060, "transit")
            )
        )
    )

    fun getCityByNameOrId(query: String): CityInfo {
        val trimmed = query.trim()
        val found = supportedCities.find {
            trimmed.isNotBlank() && (
                it.id.equals(trimmed, ignoreCase = true) ||
                it.name.equals(trimmed, ignoreCase = true) ||
                it.name.contains(trimmed, ignoreCase = true) ||
                trimmed.contains(it.name, ignoreCase = true)
            )
        }
        if (found != null) return found

        // Create dynamic CityInfo for ANY city entered
        val displayName = trimmed.ifBlank { "Current City" }
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        return CityInfo(
            id = displayName.lowercase().replace(" ", "_"),
            name = displayName,
            popularLocations = emptyList()
        )
    }

    fun searchSuggestions(query: String, currentCityId: String): List<PresetLocation> {
        val trimmed = query.trim().lowercase()
        if (trimmed.length < 1) return emptyList()

        val allLocations = supportedCities.flatMap { it.popularLocations }

        fun matches(loc: PresetLocation): Boolean {
            val name = loc.name.lowercase()
            val sub = loc.subtitle.lowercase()
            val cat = loc.category.lowercase()

            if (trimmed == "airport" && (cat == "airport" || name.contains("airport") || name.contains("terminal"))) return true
            if ((trimmed == "train" || trimmed == "station" || trimmed == "railway") && (cat == "transit" || name.contains("station") || name.contains("railway"))) return true
            if (trimmed == "metro" && (cat == "metro" || name.contains("metro"))) return true
            if ((trimmed == "mall" || trimmed == "market") && (cat == "shopping" || name.contains("mall") || name.contains("market"))) return true
            if ((trimmed == "work" || trimmed == "office" || trimmed == "tech") && (cat == "work" || name.contains("tech") || name.contains("park"))) return true

            return name.contains(trimmed) || sub.contains(trimmed)
        }

        val matchesList = allLocations.filter { matches(it) }.distinctBy { it.name }.take(8)

        if (matchesList.isEmpty() && trimmed.length >= 2) {
            val words = trimmed.split(" ").filter { it.isNotBlank() }
            return allLocations.filter { loc ->
                words.any { w -> loc.name.lowercase().contains(w) || loc.subtitle.lowercase().contains(w) }
            }.distinctBy { it.name }.take(6)
        }

        return matchesList
    }
}
