package com.app.rondacanaria.domain.model

import com.app.rondacanaria.data.model.CantoType
import com.app.rondacanaria.data.model.TeamScore
import org.junit.Assert.*
import org.junit.Test

class TeamScoreTest {

    @Test
    fun `inicialmente el tanteo debe estar a cero malas y fuera de buenas`() {
        val score = TeamScore.calculate(0)
        assertEquals(0, score.totalPiedras)
        assertEquals(0, score.malas)
        assertEquals(0, score.buenas)
        assertFalse(score.isInBuenas)
    }

    @Test
    fun `al sumar hasta 10 piedras permanece en malas`() {
        val score = TeamScore.calculate(10)
        assertEquals(10, score.totalPiedras)
        assertEquals(10, score.malas)
        assertEquals(0, score.buenas)
        assertFalse(score.isInBuenas)
    }

    @Test
    fun `al alcanzar 11 piedras entra oficialmente en buenas`() {
        val score = TeamScore.calculate(11)
        assertEquals(11, score.totalPiedras)
        assertEquals(11, score.malas)
        assertEquals(0, score.buenas)
        assertTrue(score.isInBuenas)
    }

    @Test
    fun `las piedras adicionales a 11 se acumulan correctamente en buenas`() {
        val score = TeamScore.calculate(15)
        assertEquals(15, score.totalPiedras)
        assertEquals(11, score.malas)
        assertEquals(4, score.buenas)
        assertTrue(score.isInBuenas)
    }

    @Test
    fun `al llegar a 21 piedras se completa la victoria con 10 buenas`() {
        val score = TeamScore.calculate(21)
        assertEquals(21, score.totalPiedras)
        assertEquals(11, score.malas)
        assertEquals(10, score.buenas)
        assertTrue(score.isInBuenas)
    }

    @Test
    fun `no se puede exceder el limite de 21 piedras totales`() {
        val score = TeamScore.calculate(35)
        assertEquals(TeamScore.TOTAL_PIEDRAS_VICTORY, score.totalPiedras)
        assertEquals(10, score.buenas)
    }

    @Test
    fun `las piedras nunca pueden ser negativas`() {
        val score = TeamScore.calculate(-5)
        assertEquals(0, score.totalPiedras)
        assertEquals(0, score.malas)
        assertEquals(0, score.buenas)
        assertFalse(score.isInBuenas)
    }

    @Test
    fun `verificar piedras nominales de todos los cantos y jugadas`() {
        assertEquals(1, CantoType.RONDA.defaultPiedras)
        assertEquals(3, CantoType.PARRANDA.defaultPiedras)
        assertEquals(4, CantoType.CARACOL.defaultPiedras)
        assertEquals(5, CantoType.CARACOLILLO.defaultPiedras)
        assertEquals(1, CantoType.MAJO.defaultPiedras)
        assertEquals(1, CantoType.LIMPIAR.defaultPiedras)
        assertEquals(2, CantoType.MAJO_Y_LIMPIO.defaultPiedras)
    }
}
