package com.example.data.mapper

import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

object GameStateMapper {

    fun serialize(score: Score, roundState: RoundState, arena: String = "ASCENDENCY", level: String = "LEVEL_1"): String {
        val root = JSONObject()
        root.put("arenaName", arena)
        root.put("levelName", level)
        
        // 1. Score
        val scoreObj = JSONObject().apply {
            put("playerScore", score.playerScore)
            put("aiScore", score.aiScore)
        }
        root.put("score", scoreObj)

        // 2. RoundState
        val stateObj = JSONObject().apply {
            put("roundNumber", roundState.roundNumber)
            put("phase", roundState.phase::class.java.simpleName)
            
            // Board (5x5 grid of Cells)
            val boardArr = JSONArray()
            for (row in roundState.board.cells) {
                val rowArr = JSONArray()
                for (cell in row) {
                    val cellObj = JSONObject().apply {
                        put("value", cell.value)
                        put("owner", cell.owner.name)
                        put("fortify", cell.fortify)
                        val modStr = when (cell.modifier) {
                            is CellModifier.GoldenCell -> "GoldenCell"
                            is CellModifier.Volatile -> "Volatile"
                            is CellModifier.Battleground -> "Battleground"
                            else -> "None"
                        }
                        put("modifier", modStr)
                    }
                    rowArr.put(cellObj)
                }
                boardArr.put(rowArr)
            }
            put("board", boardArr)

            // playerPlacements Map<Position, Token>
            val playerPlacementsArr = JSONArray()
            for ((pos, token) in roundState.playerPlacements) {
                val pObj = JSONObject().apply {
                    put("row", pos.row)
                    put("col", pos.col)
                    put("value", token.value)
                    put("isLocked", token.isLocked)
                }
                playerPlacementsArr.put(pObj)
            }
            put("playerPlacements", playerPlacementsArr)

            // aiPlacements Map<Position, Token>
            val aiPlacementsArr = JSONArray()
            for ((pos, token) in roundState.aiPlacements) {
                val pObj = JSONObject().apply {
                    put("row", pos.row)
                    put("col", pos.col)
                    put("value", token.value)
                    put("isLocked", token.isLocked)
                }
                aiPlacementsArr.put(pObj)
            }
            put("aiPlacements", aiPlacementsArr)

            // playerLocks List<Position>
            val playerLocksArr = JSONArray()
            for (pos in roundState.playerLocks) {
                val pObj = JSONObject().apply {
                    put("row", pos.row)
                    put("col", pos.col)
                }
                playerLocksArr.put(pObj)
            }
            put("playerLocks", playerLocksArr)

            // aiLocks List<Position>
            val aiLocksArr = JSONArray()
            for (pos in roundState.aiLocks) {
                val pObj = JSONObject().apply {
                    put("row", pos.row)
                    put("col", pos.col)
                }
                aiLocksArr.put(pObj)
            }
            put("aiLocks", aiLocksArr)

            // Scout fields
            put("scoutUsed", roundState.scoutUsed)
            
            roundState.scoutedPosition?.let { pos ->
                put("scoutedPosition", JSONObject().apply {
                    put("row", pos.row)
                    put("col", pos.col)
                })
            }
            
            roundState.scoutResult?.let { result ->
                put("scoutResult", result.name)
            }

            // Intel fields
            put("intelHint", roundState.intelHint)
            put("intelConfidence", roundState.intelConfidence)
            put("aiCapabilityName", roundState.aiCapabilityName)
            put("aiCapabilityModifier", roundState.aiCapabilityModifier)
            put("incomeMultiplier", roundState.incomeMultiplier)
            put("lateGameMessage", roundState.lateGameMessage)
            put("lateGameEventType", roundState.lateGameEventType.name)

            // intel (IntelHint?)
            roundState.intel?.let { intelHint ->
                val intelObj = JSONObject().apply {
                    put("text", intelHint.text)
                    put("isTrue", intelHint.isTrue)
                    put("reliabilityLabel", intelHint.reliabilityLabel)
                    put("type", intelHint.type.name)
                    
                    val actualDataObj = JSONObject().apply {
                        when (val data = intelHint.actualData) {
                            is IntelActualData.TokenRegion -> {
                                put("class", "TokenRegion")
                                put("tokenValue", data.tokenValue)
                                put("region", data.region)
                            }
                            is IntelActualData.Strategy -> {
                                put("class", "Strategy")
                                put("strategyName", data.strategyName)
                            }
                            is IntelActualData.CellValue -> {
                                put("class", "CellValue")
                                put("region", data.region)
                                put("cellValue", data.cellValue)
                            }
                        }
                    }
                    put("actualData", actualDataObj)
                }
                put("intel", intelObj)
            }
        }
        root.put("roundState", stateObj)

        return root.toString()
    }

    fun deserialize(jsonStr: String): Pair<Score, RoundState>? {
        return try {
            val root = JSONObject(jsonStr)
            
            // 1. Score
            val scoreObj = root.getJSONObject("score")
            val score = Score(
                playerScore = scoreObj.getInt("playerScore"),
                aiScore = scoreObj.getInt("aiScore")
            )

            // 2. RoundState
            val stateObj = root.getJSONObject("roundState")
            val roundNumber = stateObj.getInt("roundNumber")
            val phaseStr = stateObj.getString("phase")
            val phase = when (phaseStr) {
                "Intel" -> GamePhase.Intel
                "Placing" -> GamePhase.Placing
                "Revealing" -> GamePhase.Revealing
                "Resolving" -> GamePhase.Resolving
                "IncomeSummary" -> GamePhase.IncomeSummary
                "RoundTransition" -> GamePhase.RoundTransition
                "GameOver" -> GamePhase.GameOver
                else -> GamePhase.Intel
            }

            // Board
            val boardArr = stateObj.getJSONArray("board")
            val cellList = mutableListOf<List<Cell>>()
            for (r in 0 until 5) {
                val rowArr = boardArr.getJSONArray(r)
                val rowList = mutableListOf<Cell>()
                for (c in 0 until 5) {
                    val cellObj = rowArr.getJSONObject(c)
                    val modStr = cellObj.optString("modifier", "None")
                    val modifier = when (modStr) {
                        "GoldenCell" -> CellModifier.GoldenCell
                        "Volatile" -> CellModifier.Volatile
                        "Battleground" -> CellModifier.Battleground
                        else -> CellModifier.None
                    }
                    val cell = Cell(
                        value = cellObj.getInt("value"),
                        owner = CellOwner.valueOf(cellObj.getString("owner")),
                        fortify = cellObj.optInt("fortify", 0),
                        modifier = modifier
                    )
                    rowList.add(cell)
                }
                cellList.add(rowList)
            }
            val board = Board(cellList)

            // playerPlacements Map<Position, Token>
            val playerPlacements = mutableMapOf<Position, Token>()
            val playerPlacementsArr = stateObj.getJSONArray("playerPlacements")
            for (i in 0 until playerPlacementsArr.length()) {
                val obj = playerPlacementsArr.getJSONObject(i)
                val pos = Position(obj.getInt("row"), obj.getInt("col"))
                val token = Token(
                    value = obj.getInt("value"),
                    isLocked = obj.optBoolean("isLocked", false)
                )
                playerPlacements[pos] = token
            }

            // aiPlacements Map<Position, Token>
            val aiPlacements = mutableMapOf<Position, Token>()
            val aiPlacementsArr = stateObj.getJSONArray("aiPlacements")
            for (i in 0 until aiPlacementsArr.length()) {
                val obj = aiPlacementsArr.getJSONObject(i)
                val pos = Position(obj.getInt("row"), obj.getInt("col"))
                val token = Token(
                    value = obj.getInt("value"),
                    isLocked = obj.optBoolean("isLocked", false)
                )
                aiPlacements[pos] = token
            }

            // playerLocks List<Position>
            val playerLocks = mutableListOf<Position>()
            val playerLocksArr = stateObj.getJSONArray("playerLocks")
            for (i in 0 until playerLocksArr.length()) {
                val obj = playerLocksArr.getJSONObject(i)
                playerLocks.add(Position(obj.getInt("row"), obj.getInt("col")))
            }

            // aiLocks List<Position>
            val aiLocks = mutableListOf<Position>()
            val aiLocksArr = stateObj.getJSONArray("aiLocks")
            for (i in 0 until aiLocksArr.length()) {
                val obj = aiLocksArr.getJSONObject(i)
                aiLocks.add(Position(obj.getInt("row"), obj.getInt("col")))
            }

            // Scout
            val scoutUsed = stateObj.getBoolean("scoutUsed")
            val scoutedPosition = if (stateObj.has("scoutedPosition")) {
                val posObj = stateObj.getJSONObject("scoutedPosition")
                Position(posObj.getInt("row"), posObj.getInt("col"))
            } else null
            val scoutResult = if (stateObj.has("scoutResult")) {
                ScoutResult.valueOf(stateObj.getString("scoutResult"))
            } else null

            // Intel
            val intelHintText = stateObj.getString("intelHint")
            val intelConfidence = stateObj.getString("intelConfidence")
            val aiCapabilityName = stateObj.getString("aiCapabilityName")
            val aiCapabilityModifier = stateObj.getInt("aiCapabilityModifier")
            val incomeMultiplier = stateObj.optInt("incomeMultiplier", 1)
            val lateGameMessage = stateObj.optString("lateGameMessage", "")
            val lateGameEventTypeStr = stateObj.optString("lateGameEventType", "NONE")
            val lateGameEventType = try {
                LateGameEventType.valueOf(lateGameEventTypeStr)
            } catch (e: Exception) {
                LateGameEventType.NONE
            }

            // intel
            val intel = if (stateObj.has("intel") && !stateObj.isNull("intel")) {
                val intelObj = stateObj.getJSONObject("intel")
                val text = intelObj.getString("text")
                val isTrue = intelObj.getBoolean("isTrue")
                val reliabilityLabel = intelObj.getString("reliabilityLabel")
                val type = IntelType.valueOf(intelObj.getString("type"))
                
                val actualDataObj = intelObj.getJSONObject("actualData")
                val actualData = when (val className = actualDataObj.getString("class")) {
                    "TokenRegion" -> IntelActualData.TokenRegion(
                        tokenValue = actualDataObj.getInt("tokenValue"),
                        region = actualDataObj.getString("region")
                    )
                    "Strategy" -> IntelActualData.Strategy(
                        strategyName = actualDataObj.getString("strategyName")
                    )
                    "CellValue" -> IntelActualData.CellValue(
                        region = actualDataObj.getString("region"),
                        cellValue = actualDataObj.getInt("cellValue")
                    )
                    else -> throw IllegalArgumentException("Unknown actualData class: $className")
                }
                
                IntelHint(text, isTrue, reliabilityLabel, type, actualData)
            } else null

            val roundState = RoundState(
                roundNumber = roundNumber,
                phase = phase,
                board = board,
                playerPlacements = playerPlacements,
                aiPlacements = aiPlacements,
                playerLocks = playerLocks,
                aiLocks = aiLocks,
                scoutUsed = scoutUsed,
                scoutedPosition = scoutedPosition,
                scoutResult = scoutResult,
                intelHint = intelHintText,
                intelConfidence = intelConfidence,
                aiCapabilityName = aiCapabilityName,
                aiCapabilityModifier = aiCapabilityModifier,
                intel = intel,
                incomeMultiplier = incomeMultiplier,
                lateGameMessage = lateGameMessage,
                lateGameEventType = lateGameEventType
            )

            Pair(score, roundState)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deserializeArenaAndLevel(jsonStr: String): Pair<String, String> {
        return try {
            val root = JSONObject(jsonStr)
            val arena = root.optString("arenaName", "ASCENDENCY")
            val level = root.optString("levelName", "LEVEL_1")
            Pair(arena, level)
        } catch (e: Exception) {
            Pair("ASCENDENCY", "LEVEL_1")
        }
    }
}
