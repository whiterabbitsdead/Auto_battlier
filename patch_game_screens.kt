import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

fun getFighterImage(name: String): Int {
    return when (name) {
        "Grunt" -> R.drawable.ic_orc_grunt
        "Rogue" -> R.drawable.ic_dark_rogue
        "Archer" -> R.drawable.ic_elf_archer
        "Apprentice" -> R.drawable.ic_mage_apprentice
        else -> R.drawable.ic_placeholder_fighter
    }
}
