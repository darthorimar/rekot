package me.darthorimar.rekot.config.args

object ArgsConfigParser {
    fun parse(args: Array<String>): ArgsConfig {
        var appDir: String? = null

        val iterator = args.iterator()
        while (iterator.hasNext()) {
            when (val arg = iterator.next()) {
                "--app-dir" -> {
                    if (iterator.hasNext()) {
                        appDir = iterator.next()
                    } else {
                        error("`--app-dir` option requires a directory path.")
                    }
                }
                else -> {
                    error("Unknown argument `$arg`")
                }
            }
        }

        return ArgsConfig(appDir)
    }
}
