package com.app.rondacanaria.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.app.rondacanaria.data.history.LocalGamePersistence

/**
 * Servicio encargado de limpiar el estado de partidas activas cuando el usuario
 * cierra la aplicación completamente (deslizando hacia arriba en la lista de aplicaciones recientes).
 */
class AppCleanupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("AppCleanupService", "Aplicación pausada / tarea eliminada de recientes. Partida activa preservada en DataStore.")
        stopSelf()
    }
}
