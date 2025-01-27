// package me.darthorimar.repl.logging
//
// import org.apache.log4j.DefaultThrowableRenderer
// import org.apache.log4j.spi.ThrowableRenderer
//
// class AppThrowableRenderer  : ThrowableRenderer {
//    private val delegate = DefaultThrowableRenderer()
//    override fun doRender(throwable: Throwable?): Array<String> {
//        if (throwable == null) return emptyArray()
//
//        throwable.stackTraceToString()
//        IllegalStateException("AAA",  IllegalStateException("BBB")).stackTraceToString()
//    }
// }
