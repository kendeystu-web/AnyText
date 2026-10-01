package com.anytext.app.logic

/**
 * Лёгкий подсветчик синтаксиса: токенизирует построчно, состояние блочных
 * комментариев/строк переносится между строками. Без зависимостей от Compose.
 */
object Syntax {

    enum class Lang { PYTHON, JSON, CPP, JAVA, KOTLIN, JS, CSHARP, XML, HTML, SHELL, SQL, CSS }

    enum class Kind { KEYWORD, STRING, COMMENT, NUMBER, FUNC }

    data class Span(val s: Int, val e: Int, val kind: Kind)

    fun fromExt(ext: String): Lang? = when (ext) {
        "py", "pyw" -> Lang.PYTHON
        "json" -> Lang.JSON
        "c", "h", "cpp", "cxx", "cc", "hpp", "hh" -> Lang.CPP
        "java" -> Lang.JAVA
        "kt", "kts" -> Lang.KOTLIN
        "js", "ts", "jsx", "tsx", "mjs", "cjs" -> Lang.JS
        "cs" -> Lang.CSHARP
        "xml", "svg", "plist", "axml", "xaml" -> Lang.XML
        "html", "htm" -> Lang.HTML
        "sh", "bash", "zsh", "rc", "yaml", "yml", "toml", "ini", "cfg", "conf", "properties", "env", "gradle", "bat" -> Lang.SHELL
        "sql" -> Lang.SQL
        "css", "scss", "less" -> Lang.CSS
        else -> null
    }

    fun label(lang: Lang): String = when (lang) {
        Lang.PYTHON -> "Python"
        Lang.JSON -> "JSON"
        Lang.CPP -> "C/C++"
        Lang.JAVA -> "Java"
        Lang.KOTLIN -> "Kotlin"
        Lang.JS -> "JavaScript / TS"
        Lang.CSHARP -> "C#"
        Lang.XML -> "XML"
        Lang.HTML -> "HTML"
        Lang.SHELL -> "Shell / YAML"
        Lang.SQL -> "SQL"
        Lang.CSS -> "CSS"
    }

    private val keywords: Map<Lang, Set<String>> = mapOf(
        Lang.PYTHON to setOf(
            "def", "class", "if", "elif", "else", "for", "while", "import", "from", "as",
            "return", "try", "except", "finally", "with", "lambda", "yield", "pass",
            "break", "continue", "and", "or", "not", "in", "is", "None", "True", "False",
            "global", "del", "raise", "assert", "async", "await", "nonlocal", "match", "case", "self"
        ),
        Lang.JSON to setOf("true", "false", "null"),
        Lang.CPP to setOf(
            "auto", "bool", "break", "case", "catch", "char", "class", "const", "constexpr",
            "continue", "default", "delete", "do", "double", "else", "enum", "explicit",
            "extern", "false", "float", "for", "friend", "goto", "if", "inline", "int",
            "long", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
            "private", "protected", "public", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "template", "this", "throw", "true", "try",
            "typedef", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "while"
        ),
        Lang.JAVA to setOf(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
            "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "var", "record"
        ),
        Lang.KOTLIN to setOf(
            "as", "break", "by", "class", "continue", "companion", "const", "crossinline",
            "data", "do", "else", "enum", "expect", "external", "false", "final", "finally",
            "for", "fun", "get", "if", "import", "in", "infix", "init", "inline", "inner",
            "interface", "internal", "is", "lateinit", "null", "object", "open", "operator",
            "out", "override", "package", "private", "protected", "public", "reified",
            "return", "sealed", "set", "super", "suspend", "this", "throw", "true", "try",
            "typealias", "val", "var", "vararg", "when", "where", "while"
        ),
        Lang.JS to setOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends", "false",
            "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new",
            "null", "of", "return", "static", "super", "switch", "this", "throw", "true",
            "try", "typeof", "undefined", "var", "void", "while", "yield"
        ),
        Lang.CSHARP to setOf(
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char",
            "checked", "class", "const", "continue", "decimal", "default", "delegate",
            "do", "double", "else", "enum", "event", "explicit", "extern", "false",
            "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit", "in",
            "int", "interface", "internal", "is", "lock", "long", "namespace", "new",
            "null", "object", "operator", "out", "override", "params", "private",
            "protected", "public", "readonly", "ref", "return", "sbyte", "sealed",
            "short", "sizeof", "stackalloc", "static", "string", "struct", "switch",
            "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked",
            "unsafe", "ushort", "using", "var", "virtual", "void", "volatile", "while"
        ),
        Lang.XML to emptySet(),
        Lang.HTML to emptySet(),
        Lang.SHELL to setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case",
            "esac", "function", "in", "export", "return", "local", "true", "false",
            "echo", "cd", "sudo", "set", "source", "alias", "exit", "unset", "read"
        ),
        Lang.SQL to setOf(
            "select", "from", "where", "insert", "into", "values", "update", "set",
            "delete", "create", "table", "drop", "alter", "add", "join", "left", "right",
            "inner", "outer", "full", "on", "group", "by", "order", "limit", "offset",
            "having", "as", "and", "or", "not", "null", "primary", "key", "foreign",
            "references", "index", "view", "distinct", "union", "all", "exists",
            "between", "like", "case", "when", "then", "else", "end", "commit",
            "rollback", "begin", "transaction", "default", "asc", "desc", "if"
        ),
        Lang.CSS to emptySet()
    )

    /** Создаёт подсветчик; состояние (строки/комментарии) переносится между строками */
    fun highlighter(lang: Lang): Highlighter = Highlighter(lang)

    class Highlighter(private val lang: Lang) {
        private var inBlock = false
        private var strDelim: Char? = null
        private var triple = false

        private val kw = keywords[lang] ?: emptySet()
        private val lineComment: String? = when (lang) {
            Lang.CPP, Lang.JAVA, Lang.KOTLIN, Lang.JS, Lang.CSHARP, Lang.CSS -> "//"
            Lang.PYTHON, Lang.SHELL -> "#"
            Lang.SQL -> "--"
            else -> null
        }
        private val blockStart: String? = when (lang) {
            Lang.CPP, Lang.JAVA, Lang.KOTLIN, Lang.JS, Lang.CSHARP, Lang.CSS -> "/*"
            Lang.XML, Lang.HTML -> "<!--"
            else -> null
        }
        private val blockEnd: String? = when (lang) {
            Lang.CPP, Lang.JAVA, Lang.KOTLIN, Lang.JS, Lang.CSHARP, Lang.CSS -> "*/"
            Lang.XML, Lang.HTML -> "-->"
            else -> null
        }

        /** Возвращает диапазоны стилей для одной строки, продолжая состояние файла */
        fun lineSpans(line: String): List<Span> {
            val spans = ArrayList<Span>()
            var i = 0
            val n = line.length
            while (i < n) {
                if (inBlock) {
                    val be = blockEnd!!
                    val end = line.indexOf(be, i)
                    if (end < 0) {
                        spans += Span(i, n, Kind.COMMENT)
                        return spans
                    }
                    spans += Span(i, end + be.length, Kind.COMMENT)
                    i = end + be.length
                    inBlock = false
                    continue
                }
                if (strDelim != null) {
                    val d = strDelim!!
                    var j = i
                    var closed = false
                    while (j < n) {
                        if (triple) {
                            if (line.startsWith("$d$d$d", j)) { closed = true; j += 3; break }
                            if (line[j] == '\\' && j + 1 < n) j++
                        } else {
                            if (line[j] == d) { closed = true; j++; break }
                            if (line[j] == '\\' && j + 1 < n) j++
                        }
                        j++
                    }
                    spans += Span(i, j, Kind.STRING)
                    if (closed) { strDelim = null; triple = false }
                    i = j
                    continue
                }

                val c = line[i]

                if (lineComment != null && line.startsWith(lineComment, i)) {
                    spans += Span(i, n, Kind.COMMENT)
                    return spans
                }
                if (blockStart != null && line.startsWith(blockStart, i)) {
                    val be = blockEnd!!
                    val end = line.indexOf(be, i + blockStart.length)
                    if (end < 0) {
                        spans += Span(i, n, Kind.COMMENT)
                        inBlock = true
                        return spans
                    }
                    spans += Span(i, end + be.length, Kind.COMMENT)
                    i = end + be.length
                    continue
                }
                if (c == '"' || c == '\'' || ((c == '`') && (lang == Lang.JS || lang == Lang.KOTLIN))) {
                    if (lang == Lang.PYTHON && line.startsWith("$c$c$c", i)) {
                        val close = line.indexOf("$c$c$c", i + 3)
                        if (close < 0) {
                            spans += Span(i, n, Kind.STRING)
                            strDelim = c; triple = true
                            return spans
                        }
                        spans += Span(i, close + 3, Kind.STRING)
                        i = close + 3
                        continue
                    }
                    val close = findStringEnd(line, i + 1, c)
                    if (close < 0) {
                        spans += Span(i, n, Kind.STRING)
                        strDelim = c
                        return spans
                    }
                    spans += Span(i, close, Kind.STRING)
                    i = close
                    continue
                }
                if (lang == Lang.CPP && c == '#') {
                    var j = i + 1
                    while (j < n && line[j].isLetter()) j++
                    spans += Span(i, j, Kind.KEYWORD)
                    i = j
                    continue
                }
                if ((lang == Lang.XML || lang == Lang.HTML) && c == '<') {
                    val end = line.indexOf('>', i)
                    if (end < 0) { spans += Span(i, n, Kind.KEYWORD); return spans }
                    spans += Span(i, end + 1, Kind.KEYWORD)
                    i = end + 1
                    continue
                }
                if (c.isDigit()) {
                    var j = i + 1
                    while (j < n && (line[j].isLetterOrDigit() || line[j] == '.' || line[j] == '_')) j++
                    spans += Span(i, j, Kind.NUMBER)
                    i = j
                    continue
                }
                if (c.isLetter() || c == '_') {
                    var j = i + 1
                    while (j < n && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                    val word = line.substring(i, j)
                    if (lang == Lang.SQL) {
                        if (word.lowercase() in kw) spans += Span(i, j, Kind.KEYWORD)
                    } else if (word in kw) {
                        spans += Span(i, j, Kind.KEYWORD)
                    } else {
                        var k = j
                        while (k < n && line[k] == ' ') k++
                        if (k < n && line[k] == '(') spans += Span(i, j, Kind.FUNC)
                    }
                    i = j
                    continue
                }
                i++
            }
            return spans
        }

        private fun findStringEnd(line: String, from: Int, d: Char): Int {
            var j = from
            val n = line.length
            while (j < n) {
                if (line[j] == d) return j + 1
                if (line[j] == '\\' && j + 1 < n) j++
                j++
            }
            return -1
        }
    }
}
