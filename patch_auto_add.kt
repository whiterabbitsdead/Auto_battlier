        // auto-fill board
        var currentFighters = _uiState.value.playerFighters.map { it.copy() }
        val playerLevel = _uiState.value.playerLevel
        val boardCount = currentFighters.count { it.startY >= 0 }
        
        if (boardCount < playerLevel) {
            val benchFighters = currentFighters.filter { it.startY == -1 }.sortedByDescending { it.starLevel * 10 + it.cost }
            var added = 0
            val needed = playerLevel - boardCount
            
            // Find empty spots
            val emptySpots = mutableListOf<Pair<Int, Int>>()
            for (y in 4..7) {
                for (x in 0..7) {
                    if (currentFighters.none { it.startX == x && it.startY == y }) {
                        emptySpots.add(Pair(x, y))
                    }
                }
            }
            
            val newFighters = currentFighters.toMutableList()
            for (fighter in benchFighters) {
                if (added >= needed || emptySpots.isEmpty()) break
                val spot = emptySpots.removeAt(0)
                val idx = newFighters.indexOfFirst { it.id == fighter.id }
                if (idx != -1) {
                    newFighters[idx] = newFighters[idx].copy(x = spot.first, y = spot.second, startX = spot.first, startY = spot.second)
                    added++
                }
            }
            currentFighters = newFighters
        }

        // auto-equip items
        var currentInventory = _uiState.value.playerInventory.toMutableList()
        if (currentInventory.isNotEmpty()) {
            val sortedFighters = currentFighters.sortedByDescending { it.starLevel * 10 + it.cost }
            val newFighters = currentFighters.toMutableList()
            
            for (fighter in sortedFighters) {
                if (currentInventory.isEmpty()) break
                
                var f = fighter
                val idx = newFighters.indexOfFirst { it.id == fighter.id }
                
                while (f.equipment.size < 3 && currentInventory.isNotEmpty()) {
                    val eq = currentInventory.removeAt(0)
                    var newPAtk = f.physAttack
                    var newPDef = f.physDefense
                    var newMaxHp = f.maxHp
                    var newHp = f.hp
                    var newDodge = f.dodgeChance
                    when (eq.type) {
                        EquipmentType.ATTACK -> newPAtk += 10
                        EquipmentType.DEFENSE -> newPDef += 10
                        EquipmentType.HP -> { newMaxHp += 50; newHp += 50 }
                        EquipmentType.DODGE -> newDodge += 0.10
                    }
                    f = f.copy(
                        equipment = f.equipment + eq,
                        physAttack = newPAtk,
                        physDefense = newPDef,
                        maxHp = newMaxHp,
                        hp = newHp,
                        dodgeChance = newDodge
                    )
                }
                if (idx != -1) newFighters[idx] = f
            }
            currentFighters = newFighters
        }
