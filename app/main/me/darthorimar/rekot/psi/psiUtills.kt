package me.darthorimar.rekot.psi

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

val PsiFile.document: Document
    get() {
        return PsiDocumentManager.getInstance(project).getDocument(this) ?: error("No document found for file $name")
    }
