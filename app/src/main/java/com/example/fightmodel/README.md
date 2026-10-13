# fightmodel

A standalone fight model (no Android/game dependencies). Copy the `com.example.fightmodel` package into any Kotlin/JVM project.

- `Combatant` – base stats, `Equipment`, timed `Buff`s, `Skill`s.
- `Modifier` – flat then percent bonus to a `Stat`; final stat = (base + flat) * (1 + percent).
- `Skill` – power multiplier, damage type (physical/magical/true), cooldown, optional buff/debuff, lifesteal.
- `FightModel(Random(seed)).duel(a, b)` – deterministic fight returning winner and an event log.

```kotlin
val hero = Combatant("Hero", mapOf(Stat.MAX_HP to 100.0, Stat.ATTACK to 20.0, Stat.SPEED to 10.0),
    equipment = listOf(Equipment("Sword", listOf(Modifier(Stat.ATTACK, flat = 10.0)))),
    skills = listOf(Skill("Slam", power = 2.0)))
val result = FightModel(Random(1)).duel(hero, Combatant("Orc", mapOf(Stat.MAX_HP to 120.0, Stat.ATTACK to 15.0)))
```
