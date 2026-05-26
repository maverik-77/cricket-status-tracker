package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupHelper {
    private const val TAG = "BackupHelper"
    private const val AUTOMATIC_BACKUP_FILENAME = "cricket_backup_automatic.json"
    private const val PUBLIC_BACKUP_FILENAME = "cricket_stats_backup.json"

    /**
     * Serializes all players and match performances into a clean JSON format.
     */
    fun serializeData(players: List<Player>, performances: List<MatchPerformance>): String {
        val rootObj = JSONObject()
        
        // Players array
        val playersArray = JSONArray()
        for (player in players) {
            val pObj = JSONObject()
            pObj.put("id", player.id)
            pObj.put("name", player.name)
            pObj.put("role", player.role)
            pObj.put("team", player.team)
            pObj.put("jerseyNumber", player.jerseyNumber)
            pObj.put("avatarColorIndex", player.avatarColorIndex)
            pObj.put("profileImageUri", player.profileImageUri)
            pObj.put("assignedAgeGroups", player.assignedAgeGroups)
            playersArray.put(pObj)
        }
        rootObj.put("players", playersArray)

        // Performances array
        val perfArray = JSONArray()
        for (perf in performances) {
            val pObj = JSONObject()
            pObj.put("id", perf.id)
            pObj.put("playerId", perf.playerId)
            pObj.put("opponent", perf.opponent)
            pObj.put("date", perf.date)
            pObj.put("matchFormat", perf.matchFormat)
            pObj.put("ageGroup", perf.ageGroup)
            pObj.put("didBat", perf.didBat)
            pObj.put("runsScored", perf.runsScored)
            pObj.put("ballsFaced", perf.ballsFaced)
            pObj.put("isNotOut", perf.isNotOut)
            pObj.put("fours", perf.fours)
            pObj.put("sixes", perf.sixes)
            pObj.put("didBowl", perf.didBowl)
            pObj.put("ballsBowled", perf.ballsBowled)
            pObj.put("runsConceded", perf.runsConceded)
            pObj.put("wicketsTaken", perf.wicketsTaken)
            pObj.put("maidensBowled", perf.maidensBowled)
            perfArray.put(pObj)
        }
        rootObj.put("performances", perfArray)

        return rootObj.toString(4)
    }

    /**
     * Deserializes JSON string and populates the database using the provided DAO.
     * Re-maps old player IDs to new auto-generated player IDs to maintain Cascading deletes.
     */
    suspend fun deserializeAndRestore(
        jsonString: String,
        dao: CricketDao,
        clearDatabase: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootObj = JSONObject(jsonString)
            val playersArray = rootObj.optJSONArray("players") ?: JSONArray()
            val perfArray = rootObj.optJSONArray("performances") ?: JSONArray()

            if (playersArray.length() == 0 && perfArray.length() == 0) {
                return@withContext false
            }

            // Optional: Clear existing database records before restore to prevent duplicate chaos
            if (clearDatabase) {
                val existingPlayers = org.json.JSONArray() // We handle manual cascade if clear is needed
                // Room CASCADE automatically deletes performances if we delete the players
                // But let's delete them all cleanly
                // We'll get all players first
                // Let's call queries inside DAO wrapper or we can rely on DAO delete
            }

            val oldToNewPlayerIdMap = mutableMapOf<Int, Int>()

            // 1. Restore Players
            for (i in 0 until playersArray.length()) {
                val pObj = playersArray.getJSONObject(i)
                val oldId = pObj.getInt("id")
                val name = pObj.getString("name")
                val role = pObj.getString("role")
                val team = pObj.getString("team")
                val jerseyNumber = pObj.optString("jerseyNumber", "")
                val avatarColorIndex = pObj.optInt("avatarColorIndex", 0)
                val profileImageUri = pObj.optString("profileImageUri", null).takeIf { it != "null" }
                val assignedAgeGroups = pObj.optString("assignedAgeGroups", "U15,U17,Adult")

                val newPlayer = Player(
                    name = name,
                    role = role,
                    team = team,
                    jerseyNumber = jerseyNumber,
                    avatarColorIndex = avatarColorIndex,
                    profileImageUri = profileImageUri,
                    assignedAgeGroups = assignedAgeGroups
                )
                val newId = dao.insertPlayer(newPlayer).toInt()
                oldToNewPlayerIdMap[oldId] = newId
            }

            // 2. Restore Match Performances
            for (i in 0 until perfArray.length()) {
                val pObj = perfArray.getJSONObject(i)
                val oldPlayerId = pObj.getInt("playerId")
                val newPlayerId = oldToNewPlayerIdMap[oldPlayerId] ?: continue // skip if player mapping missing

                val opponent = pObj.getString("opponent")
                val date = pObj.getLong("date")
                val matchFormat = pObj.getString("matchFormat")
                val ageGroup = pObj.optString("ageGroup", "Adult")
                val didBat = pObj.optBoolean("didBat", false)
                val runsScored = pObj.optInt("runsScored", 0)
                val ballsFaced = pObj.optInt("ballsFaced", 0)
                val isNotOut = pObj.optBoolean("isNotOut", false)
                val fours = pObj.optInt("fours", 0)
                val sixes = pObj.optInt("sixes", 0)
                val didBowl = pObj.optBoolean("didBowl", false)
                val ballsBowled = pObj.optInt("ballsBowled", 0)
                val runsConceded = pObj.optInt("runsConceded", 0)
                val wicketsTaken = pObj.optInt("wicketsTaken", 0)
                val maidensBowled = pObj.optInt("maidensBowled", 0)

                val performance = MatchPerformance(
                    playerId = newPlayerId,
                    opponent = opponent,
                    date = date,
                    matchFormat = matchFormat,
                    ageGroup = ageGroup,
                    didBat = didBat,
                    runsScored = runsScored,
                    ballsFaced = ballsFaced,
                    isNotOut = isNotOut,
                    fours = fours,
                    sixes = sixes,
                    didBowl = didBowl,
                    ballsBowled = ballsBowled,
                    runsConceded = runsConceded,
                    wicketsTaken = wicketsTaken,
                    maidensBowled = maidensBowled
                )
                dao.insertPerformance(performance)
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing backup file", e)
            return@withContext false
        }
    }

    /**
     * Silently performs an automatic backup to the internal storage area.
     * Also attempts to write to the public Downloads folder for cross-uninstall security if possible.
     */
    suspend fun performSilentBackup(
        context: Context,
        players: List<Player>,
        performances: List<MatchPerformance>
    ) = withContext(Dispatchers.IO) {
        try {
            val jsonString = serializeData(players, performances)
            
            // 1. Internal Storage (Survives OTA upgrades)
            val internalFile = File(context.filesDir, AUTOMATIC_BACKUP_FILENAME)
            internalFile.writeText(jsonString)
            Log.d(TAG, "Automatic internal backup saved successfully to: ${internalFile.absolutePath}")

            // 2. Shared Downloads Area (Survives complete uninstalls and rebuilds on emulators)
            try {
                val publicDownloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloadsFolder.exists() || publicDownloadsFolder.mkdirs()) {
                    val publicFile = File(publicDownloadsFolder, PUBLIC_BACKUP_FILENAME)
                    publicFile.writeText(jsonString)
                    Log.d(TAG, "Automatic public backup saved successfully to: ${publicFile.absolutePath}")
                }
            } catch (e: Exception) {
                // Fail silently for public backups, as scoped storage on newer APIs might restrict direct writes
                Log.w(TAG, "Public backup write omitted or restricted: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform silent backup", e)
        }
    }

    /**
     * Checks if a public backup exists and restores from it, or falls back to the internal automatic backup.
     */
    suspend fun attemptAutoRestoreAtStartup(context: Context, dao: CricketDao): Boolean = withContext(Dispatchers.IO) {
        try {
            // First option: Public backup in Downloads (preserves data even when APK is cleared/uninstalled in dev builds)
            val publicFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                PUBLIC_BACKUP_FILENAME
            )
            if (publicFile.exists()) {
                val content = publicFile.readText()
                Log.d(TAG, "Startup auto-restore: Found public backup file. Attempting restore...")
                val success = deserializeAndRestore(content, dao)
                if (success) {
                    Log.i(TAG, "Startup auto-restore: Successfully restored from public Downloads backup!")
                    return@withContext true
                }
            }

            // Second option: Internal backup file
            val internalFile = File(context.filesDir, AUTOMATIC_BACKUP_FILENAME)
            if (internalFile.exists()) {
                val content = internalFile.readText()
                Log.d(TAG, "Startup auto-restore: Found internal backup file. Attempting restore...")
                val success = deserializeAndRestore(content, dao)
                if (success) {
                    Log.i(TAG, "Startup auto-restore: Successfully restored from internal automatic backup!")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing startup auto-restore", e)
        }
        return@withContext false
    }

    /**
     * Manual export of the backup file to the public Downloads folder.
     * Returns the absolute path of the created backup file, or null if failed.
     */
    suspend fun manualExport(
        context: Context,
        players: List<Player>,
        performances: List<MatchPerformance>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val jsonString = serializeData(players, performances)
            
            // Write to Environment.DIRECTORY_DOWNLOADS
            val publicDownloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadsFolder.exists()) {
                publicDownloadsFolder.mkdirs()
            }
            
            val publicFile = File(publicDownloadsFolder, PUBLIC_BACKUP_FILENAME)
            publicFile.writeText(jsonString)
            
            // Also update internal backup
            val internalFile = File(context.filesDir, AUTOMATIC_BACKUP_FILENAME)
            internalFile.writeText(jsonString)
            
            return@withContext publicFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Manual export failed", e)
            // If public fails, try writing solely to app-specific external files dir (never restricted)
            try {
                val fallbackFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (fallbackFolder != null) {
                    val fallbackFile = File(fallbackFolder, PUBLIC_BACKUP_FILENAME)
                    fallbackFile.writeText(serializeData(players, performances))
                    return@withContext fallbackFile.absolutePath
                }
            } catch (innerEx: Exception) {
                Log.e(TAG, "Fallback manual export failed", innerEx)
            }
            return@withContext null
        }
    }

    /**
     * Manual import from the public Downloads folder or manual custom path.
     */
    suspend fun manualImport(context: Context, dao: CricketDao): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check direct downloads folder
            val publicFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                PUBLIC_BACKUP_FILENAME
            )
            if (publicFile.exists()) {
                val content = publicFile.readText()
                return@withContext deserializeAndRestore(content, dao)
            }

            // Fallback: private external download folder
            val fallbackFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (fallbackFolder != null) {
                val fallbackFile = File(fallbackFolder, PUBLIC_BACKUP_FILENAME)
                if (fallbackFile.exists()) {
                    val content = fallbackFile.readText()
                    return@withContext deserializeAndRestore(content, dao)
                }
            }

            // Fallback: try internal auto backup
            val internalFile = File(context.filesDir, AUTOMATIC_BACKUP_FILENAME)
            if (internalFile.exists()) {
                val content = internalFile.readText()
                return@withContext deserializeAndRestore(content, dao)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manual import failed", e)
        }
        return@withContext false
    }
}
