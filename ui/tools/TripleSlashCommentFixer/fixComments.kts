import java.io.File

fun String.isDartComment() = with(trimStart()) {
    startsWith("///") || (startsWith("// /") && !startsWith("// //"))
}

fun String.getDartCommentPrefix() = with(trimStart()) {
    when {
        startsWith("///") -> {
            "///"
        }
        startsWith("// /") -> {
            "// /"
        }
        else -> { "" }
    }
}

File(args[0]).walkTopDown().filter { it.name.endsWith(".kt") }.forEach {
    var result = ""
    val comments = emptyList<String>().toMutableList()

    fun pourComments(indentation: Int = -1) {
        val indentation = if (indentation >= 0 || comments.isEmpty()) {
            indentation
        } else {
            comments[0].length - comments[0].trimStart().length
        }

        when {
            comments.size == 1 -> {
                // Single line style comment.
                val prefix = comments[0].getDartCommentPrefix()
                val newLine = " ".repeat(indentation) +
                    comments[0].trimStart().replaceFirst(prefix, "/**") + " */\n"
                result += newLine
            }
            comments.size > 1 -> {
                // Multi line style comment.
                result += " ".repeat(indentation) + "/**\n"
                comments.forEach { line ->
                    val prefix = line.getDartCommentPrefix()
                    val newLine = " ".repeat(indentation) +
                        line.trimStart().replaceFirst(prefix, " *") + "\n"
                    result += newLine
                }
                result += " ".repeat(indentation + 1) + "*/\n"
            }
            else -> {}
        }
    }

    it.forEachLine { line ->
        if (line.isDartComment()) {
            comments.add(line)
        } else {
            val indentation = line.length - line.trimStart().length
            pourComments(indentation)
            result += line + "\n"
            comments.clear()
        }
    }
    pourComments()

    // It's possible that we added an extra new line at the end. Remove it.
    with(it.readText()) {
        if (get(length - 1) != '\n') {
            result = result.substring(0, result.length - 1)
        }
    }

    // print(result)
    it.writeText(result)
}
