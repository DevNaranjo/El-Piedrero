package com.app.rondacanaria.ui.components

object CanarianNames {
    val MASCULINOS = listOf(
        "Acentejo", "Acoirán", "Acomar", "Acorán", "Aday",
        "Adonhai", "Afur", "Agoney", "Aguamansa", "Ahitami",
        "Airam", "Ajei", "Anthea", "Ancor", "Antam",
        "Aragueme", "Aray", "Aridane", "Artemi", "Ataman",
        "Atasat", "AtGuayafanta", "Ayoze", "Balta", "Bentagay",
        "Bentor", "Belicar", "Bencomo", "Caluca", "Chedey",
        "Dacio", "Doramas", "Echedey", "Guacimara", "Guanarame",
        "Guenet", "Iriome", "Jonay", "Maday", "Mencey",
        "Nauzet", "Rayco", "Rucaden", "Sigoñe", "Tanausú",
        "Tenerife", "Tigre", "Timanfaya", "Ubay", "Yeray",
        "Yosimar"
    )

    val FEMENINOS = listOf(
        "Acerina", "Arima", "Arminda", "Cathaysa", "Chaxiraxi",
        "Dácil", "Fayna", "Gara", "Guayarmina", "Iballa",
        "Idaira", "Maday", "Naira", "Sibisse", "Taima",
        "Tenesar", "Yaiza", "Yurena", "Altaha", "Aniagua",
        "Ariadne", "Atchen", "Chijoraji", "Cho", "Dassa",
        "Guacimara", "Harimaguada", "Iraya", "Itahisa", "Mawa",
        "May", "Nisa", "Ramagua", "Siria", "Tasaigo",
        "Tibiabin", "Timaginas", "Tindaya", "Uza", "Tacoremi",
        "Tagamanent", "Tamaran", "Tare", "Tirma", "Zurita",
        "Ainhoa", "Atteneri", "Guaria", "Rayco", "Sibora"
    )

    val ALL_NAMES: List<String> = (MASCULINOS + FEMENINOS).distinct()

    fun getRandomName(): String {
        return ALL_NAMES.random()
    }
}
