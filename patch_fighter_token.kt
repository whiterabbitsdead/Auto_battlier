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
            alpha = 0.5f // Dim slightly to show text
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
