package com.example.data.mapper

import com.example.data.model.*
import com.example.domain.model.*

fun MatchPlayer.toMap(): Map<String, Any?> {
    return mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "isReady" to isReady,
        "lastSeen" to lastSeen
    )
}

fun Map<*, *>.toMatchPlayer(): MatchPlayer {
    return MatchPlayer(
        uid = this["uid"] as? String ?: "",
        displayName = this["displayName"] as? String ?: "",
        photoUrl = this["photoUrl"] as? String,
        isReady = this["isReady"] as? Boolean ?: false,
        lastSeen = (this["lastSeen"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}

fun Board.toMap(): Map<String, Any?> {
    val cellsList = cells.map { row ->
        row.map { cell ->
            mapOf(
                "value" to cell.value,
                "owner" to cell.owner.name,
                "fortify" to cell.fortify,
                "modifier" to when (cell.modifier) {
                    is CellModifier.GoldenCell -> "GoldenCell"
                    is CellModifier.Volatile -> "Volatile"
                    is CellModifier.Battleground -> "Battleground"
                    else -> "None"
                }
            )
        }
    }
    return mapOf("cells" to cellsList)
}

fun Map<*, *>.toBoard(): Board {
    val cellsList = (this["cells"] as? List<*>)?.map { rowObj ->
        (rowObj as? List<*>)?.map { cellObj ->
            val cellMap = cellObj as? Map<*, *>
            val value = (cellMap?.get("value") as? Number)?.toInt() ?: 1
            val ownerStr = cellMap?.get("owner") as? String ?: "NONE"
            val owner = try { CellOwner.valueOf(ownerStr) } catch (e: Exception) { CellOwner.NONE }
            val fortify = (cellMap?.get("fortify") as? Number)?.toInt() ?: 0
            val modifierStr = cellMap?.get("modifier") as? String ?: "None"
            val modifier = when (modifierStr) {
                "GoldenCell" -> CellModifier.GoldenCell
                "Volatile" -> CellModifier.Volatile
                "Battleground" -> CellModifier.Battleground
                else -> CellModifier.None
            }
            Cell(value = value, owner = owner, fortify = fortify, modifier = modifier)
        } ?: List(5) { Cell(value = 1) }
    } ?: List(5) { List(5) { Cell(value = 1) } }
    
    // Ensure 5x5
    val paddedCells = List(5) { r ->
        val row = cellsList.getOrNull(r) ?: emptyList()
        List(5) { c ->
            row.getOrNull(c) ?: Cell(value = 1)
        }
    }
    return Board(paddedCells)
}

fun RoundResult.toMap(): Map<String, Any?> {
    return mapOf(
        "roundNumber" to roundNumber,
        "player1Placements" to player1Placements,
        "player2Placements" to player2Placements,
        "clashes" to clashes.map { it.toMap() },
        "player1Income" to player1Income,
        "player2Income" to player2Income
    )
}

fun Map<*, *>.toRoundResult(): RoundResult {
    val clashesList = (this["clashes"] as? List<*>)?.mapNotNull {
        (it as? Map<*, *>)?.toClashOutcome()
    } ?: emptyList()
    
    return RoundResult(
        roundNumber = (this["roundNumber"] as? Number)?.toInt() ?: 1,
        player1Placements = (this["player1Placements"] as? Map<*, *>)?.entries?.associate {
            it.key.toString() to (it.value as? Number)?.toInt()!!
        } ?: emptyMap(),
        player2Placements = (this["player2Placements"] as? Map<*, *>)?.entries?.associate {
            it.key.toString() to (it.value as? Number)?.toInt()!!
        } ?: emptyMap(),
        clashes = clashesList,
        player1Income = (this["player1Income"] as? Number)?.toInt() ?: 0,
        player2Income = (this["player2Income"] as? Number)?.toInt() ?: 0
    )
}

fun ClashOutcome.toMap(): Map<String, Any?> {
    return mapOf(
        "position" to position,
        "player1Token" to player1Token,
        "player2Token" to player2Token,
        "winner" to winner,
        "player1Strength" to player1Strength,
        "player2Strength" to player2Strength
    )
}

fun Map<*, *>.toClashOutcome(): ClashOutcome {
    return ClashOutcome(
        position = this["position"] as? String ?: "",
        player1Token = (this["player1Token"] as? Number)?.toInt(),
        player2Token = (this["player2Token"] as? Number)?.toInt(),
        winner = this["winner"] as? String,
        player1Strength = (this["player1Strength"] as? Number)?.toInt() ?: 0,
        player2Strength = (this["player2Strength"] as? Number)?.toInt() ?: 0
    )
}

fun PvPMatch.toMap(): Map<String, Any?> {
    return mapOf(
        "matchId" to matchId,
        "player1" to player1.toMap(),
        "player2" to player2.toMap(),
        "arena" to arena.name,
        "level" to level.name,
        "board" to board.toMap(),
        "currentRound" to currentRound,
        "matchPhase" to matchPhase.name,
        "roundPhase" to roundPhase.name,
        "player1Placements" to player1Placements,
        "player2Placements" to player2Placements,
        "player1Locks" to player1Locks,
        "player2Locks" to player2Locks,
        "player1Score" to player1Score,
        "player2Score" to player2Score,
        "roundResults" to roundResults.map { it.toMap() },
        "winnerUid" to winnerUid,
        "scouts" to scouts,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun Map<*, *>.toPvPMatch(): PvPMatch {
    val player1Map = this["player1"] as? Map<*, *>
    val player2Map = this["player2"] as? Map<*, *>
    val boardMap = this["board"] as? Map<*, *>
    
    val arenaStr = this["arena"] as? String ?: Arena.ASCENDENCY.name
    val arena = try { Arena.valueOf(arenaStr) } catch (e: Exception) { Arena.ASCENDENCY }
    
    val levelStr = this["level"] as? String ?: GameLevel.LEVEL_1.name
    val level = try { GameLevel.valueOf(levelStr) } catch (e: Exception) { GameLevel.LEVEL_1 }
    
    val matchPhaseStr = this["matchPhase"] as? String ?: MatchPhase.WAITING.name
    val matchPhase = try { MatchPhase.valueOf(matchPhaseStr) } catch (e: Exception) { MatchPhase.WAITING }
    
    val roundPhaseStr = this["roundPhase"] as? String ?: RoundPhase.PLACEMENT.name
    val roundPhase = try { RoundPhase.valueOf(roundPhaseStr) } catch (e: Exception) { RoundPhase.PLACEMENT }
    
    val rResults = (this["roundResults"] as? List<*>)?.mapNotNull {
        (it as? Map<*, *>)?.toRoundResult()
    } ?: emptyList()
    
    return PvPMatch(
        matchId = this["matchId"] as? String ?: "",
        player1 = player1Map?.toMatchPlayer() ?: MatchPlayer(),
        player2 = player2Map?.toMatchPlayer() ?: MatchPlayer(),
        arena = arena,
        level = level,
        board = boardMap?.toBoard() ?: Board(),
        currentRound = (this["currentRound"] as? Number)?.toInt() ?: 1,
        matchPhase = matchPhase,
        roundPhase = roundPhase,
        player1Placements = (this["player1Placements"] as? Map<*, *>)?.entries?.associate {
            it.key.toString() to (it.value as? Number)?.toInt()!!
        } ?: emptyMap(),
        player2Placements = (this["player2Placements"] as? Map<*, *>)?.entries?.associate {
            it.key.toString() to (it.value as? Number)?.toInt()!!
        } ?: emptyMap(),
        player1Locks = (this["player1Locks"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        player2Locks = (this["player2Locks"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        player1Score = (this["player1Score"] as? Number)?.toInt() ?: 0,
        player2Score = (this["player2Score"] as? Number)?.toInt() ?: 0,
        roundResults = rResults,
        winnerUid = this["winnerUid"] as? String,
        scouts = (this["scouts"] as? Map<*, *>)?.entries?.associate { entry ->
            val uidKey = entry.key.toString()
            val subMap = (entry.value as? Map<*, *>)?.entries?.associate { subEntry ->
                subEntry.key.toString() to subEntry.value.toString()
            } ?: emptyMap()
            uidKey to subMap
        } ?: emptyMap(),
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}
