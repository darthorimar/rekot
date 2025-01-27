package me.darthorimar.rekot.psi

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.projectStructure.ProjectStructure
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.util.suffixIfNot
import org.koin.core.component.inject

class CellPsiUtils : AppComponent {
    private val projectStructure: ProjectStructure by inject()

    fun createKtFile(cell: Cell, filename: String): KtFile {
        val factory = KtPsiFactory(projectStructure.project, eventSystemEnabled = true)
        return factory.createFile(filename.suffixIfNot(".kts"), cell.text)
    }
}
