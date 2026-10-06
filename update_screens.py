import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale"""
content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\n' + imports)

# Add getFighterImage & FighterToken at bottom
footer = """
fun getFighterImage(name: String): Int {
    return when (name) {
        "Grunt" -> R.drawable.ic_orc_grunt
        "Rogue" -> R.drawable.ic_dark_rogue
        "Archer" -> R.drawable.ic_elf_archer
        "Apprentice" -> R.drawable.ic_mage_apprentice
        else -> R.drawable.ic_placeholder_fighter
    }
}

@Composable
fun FighterToken(
    fighter: Fighter,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.Yellow else Color.Black, RoundedCornerShape(4.dp))
            .background(if (fighter.isPlayer) Color.Blue.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = getFighterImage(fighter.name)),
            contentDescription = fighter.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⭐".repeat(fighter.starLevel),
                fontSize = 6.sp,
                color = Color.Yellow
            )
            Text(
                text = fighter.name.take(3),
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "HP:${fighter.hp}",
                fontSize = 8.sp,
                color = Color.White
            )
            Text(
                text = "MP:${fighter.mana}/100",
                fontSize = 7.sp,
                color = Color.Cyan
            )
            val mainAtk = maxOf(fighter.physAttack, fighter.magAttack)
            val mainDef = maxOf(fighter.physDefense, fighter.magDefense)
            Text(
                text = "A:$mainAtk D:$mainDef",
                fontSize = 7.sp,
                color = Color.White
            )
        }
    }
}
"""
content += footer

# Replace board fighter rendering
board_fighter_regex = re.compile(r'Box\(\s*modifier = Modifier\s*\.offset\(\s*x = cellWidth \* fighter\.x,\s*y = cellHeight \* fighter\.y\s*\)\s*\.size\(cellWidth, cellHeight\)\s*\.padding\(2\.dp\)\s*\.background\(\s*if \(fighter\.isPlayer\) Color\.Blue\.copy\(alpha = 0\.7f\) else Color\.Red\.copy\(alpha = 0\.7f\),\s*RoundedCornerShape\(4\.dp\)\s*\)\s*\.border\(if \(isSelected\) 3\.dp else 1\.dp, if \(isSelected\) Color\.Yellow else Color\.Black, RoundedCornerShape\(4\.dp\)\)\s*\.clickable\(enabled = !state\.isBattleActive && state\.battleResult == null && fighter\.isPlayer\) \{\s*if \(state\.selectedEquipmentId != null\) \{\s*viewModel\.equipToFighter\(fighter\.id\)\s*\} else \{\s*viewModel\.selectFighter\(fighter\.id\)\s*\}\s*\},.*?\}\s*\}', re.DOTALL)

new_board_fighter = """FighterToken(
                        fighter = fighter,
                        isSelected = isSelected,
                        modifier = Modifier
                            .offset(
                                x = cellWidth * fighter.x,
                                y = cellHeight * fighter.y
                            )
                            .size(cellWidth, cellHeight)
                            .clickable(enabled = !state.isBattleActive && state.battleResult == null && fighter.isPlayer) {
                                if (state.selectedEquipmentId != null) {
                                    viewModel.equipToFighter(fighter.id)
                                } else {
                                    viewModel.selectFighter(fighter.id)
                                }
                            }
                    )"""

content = board_fighter_regex.sub(new_board_fighter, content, count=1)

# Replace bench fighter rendering
bench_fighter_regex = re.compile(r'Box\(\s*modifier = Modifier\s*\.offset\(\s*x = cellWidth \* fighter\.x,\s*y = 0\.dp\s*\)\s*\.size\(cellWidth, cellHeight\)\s*\.padding\(2\.dp\)\s*\.background\(\s*Color\.Blue\.copy\(alpha = 0\.7f\),\s*RoundedCornerShape\(4\.dp\)\s*\)\s*\.border\(if \(isSelected\) 3\.dp else 1\.dp, if \(isSelected\) Color\.Yellow else Color\.Black, RoundedCornerShape\(4\.dp\)\)\s*\.clickable\(enabled = !state\.isBattleActive && state\.battleResult == null\) \{\s*if \(state\.selectedEquipmentId != null\) \{\s*viewModel\.equipToFighter\(fighter\.id\)\s*\} else \{\s*viewModel\.selectFighter\(fighter\.id\)\s*\}\s*\},.*?\}\s*\}', re.DOTALL)

new_bench_fighter = """FighterToken(
                        fighter = fighter,
                        isSelected = isSelected,
                        modifier = Modifier
                            .offset(
                                x = cellWidth * fighter.x,
                                y = 0.dp
                            )
                            .size(cellWidth, cellHeight)
                            .clickable(enabled = !state.isBattleActive && state.battleResult == null) {
                                if (state.selectedEquipmentId != null) {
                                    viewModel.equipToFighter(fighter.id)
                                } else {
                                    viewModel.selectFighter(fighter.id)
                                }
                            }
                    )"""

content = bench_fighter_regex.sub(new_bench_fighter, content, count=1)

with open('app/src/main/java/com/example/game/GameScreens.kt', 'w') as f:
    f.write(content)

print("Done replacing.")
