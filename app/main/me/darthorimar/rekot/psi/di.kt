package me.darthorimar.rekot.psi

import org.koin.dsl.module

val psiModule = module { single { CellPsiUtils() } }
