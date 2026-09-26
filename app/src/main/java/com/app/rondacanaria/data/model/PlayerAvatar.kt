package com.app.rondacanaria.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class PlayerAvatar(
    val id: String,
    val name: String,
    val emoji: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val description: String
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)
}

object AvatarCatalog {
    val DEFAULT_AVATAR_ID = "avatar_piedrero"

    val AVATARS: List<PlayerAvatar> = listOf(
        PlayerAvatar(
            id = "avatar_piedrero",
            name = "El Piedrero",
            emoji = "🪨",
            primaryColorHex = 0xFF78909CL, // Blue Grey
            secondaryColorHex = 0xFF455A64L,
            description = "El guardián infalible de las piedras y del tanteo"
        ),
        PlayerAvatar(
            id = "avatar_tahur",
            name = "El Tahúr",
            emoji = "🃏",
            primaryColorHex = 0xFFAB47BCL, // Purple
            secondaryColorHex = 0xFF6A1B9AL,
            description = "Maestro de las cartas, el caracolillo y las mañas"
        ),
        PlayerAvatar(
            id = "avatar_mago",
            name = "El Mago",
            emoji = "👒",
            primaryColorHex = 0xFFFFA726L, // Amber/Orange
            secondaryColorHex = 0xFFE65100L,
            description = "Campesino tradicional con maña campesina y buen ojo"
        ),
        PlayerAvatar(
            id = "avatar_lagarto",
            name = "El Lagarto",
            emoji = "🦎",
            primaryColorHex = 0xFF66BB6AL, // Green
            secondaryColorHex = 0xFF2E7D32L,
            description = "Ágil y atento como el lagarto tizón canario"
        ),
        PlayerAvatar(
            id = "avatar_bardino",
            name = "El Bardino",
            emoji = "🐕",
            primaryColorHex = 0xFF8D6E63L, // Brown
            secondaryColorHex = 0xFF4E342EL,
            description = "Fiel, noble y valiente como el can de presa majorero"
        ),
        PlayerAvatar(
            id = "avatar_mencey",
            name = "El Mencey",
            emoji = "👑",
            primaryColorHex = 0xFFFFD54FL, // Gold
            secondaryColorHex = 0xFFFFA000L,
            description = "Líder indiscutible con orgullo y nobleza de rey"
        ),
        PlayerAvatar(
            id = "avatar_palmero",
            name = "El Palmero",
            emoji = "🌴",
            primaryColorHex = 0xFF26A69AL, // Teal
            secondaryColorHex = 0xFF00695CL,
            description = "Tranquilo y sereno como las palmeras al viento"
        ),
        PlayerAvatar(
            id = "avatar_costero",
            name = "El Costero",
            emoji = "🎣",
            primaryColorHex = 0xFF42A5F5L, // Blue
            secondaryColorHex = 0xFF1565C0L,
            description = "Pescador paciente que espera el momento exacto para el lance"
        ),
        PlayerAvatar(
            id = "avatar_majo",
            name = "El Majo",
            emoji = "🥣",
            primaryColorHex = 0xFFFF7043L, // Terracota cálido / Mojo
            secondaryColorHex = 0xFFD84315L,
            description = "De casta majorera y brazo incansable: siempre majando en la mesa hasta rendir al rival"
        ),
        PlayerAvatar(
            id = "avatar_guayota",
            name = "El Guayota",
            emoji = "🌋",
            primaryColorHex = 0xFFEF5350L, // Red
            secondaryColorHex = 0xFFC62828L,
            description = "Espíritu del volcán, pura bravura y cantadas de fuego"
        ),
        PlayerAvatar(
            id = "avatar_cernicalo",
            name = "El Cernícalo",
            emoji = "🦅",
            primaryColorHex = 0xFF7E57C2L, // Deep Purple
            secondaryColorHex = 0xFF4527A0L,
            description = "Visión de águila para calcular cada arrastre de cartas"
        ),
        PlayerAvatar(
            id = "avatar_romera",
            name = "La Romera",
            emoji = "💃",
            primaryColorHex = 0xFFEC407AL, // Pink
            secondaryColorHex = 0xFFAD1457L,
            description = "La alegría de la parranda y el compás de la victoria"
        ),
        PlayerAvatar(
            id = "avatar_sabio",
            name = "El Sabio",
            emoji = "🧔",
            primaryColorHex = 0xFF5C6BC0L, // Indigo
            secondaryColorHex = 0xFF283593L,
            description = "Veterano de mil manos que nunca olvida qué cartas han salido"
        )
    )

    fun getAvatarById(id: String?): PlayerAvatar {
        if (id.isNullOrBlank()) return AVATARS.first()
        return AVATARS.find { it.id == id } ?: AVATARS.first()
    }

    fun getRandomAvatar(): PlayerAvatar = AVATARS.random()
}
