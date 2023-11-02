package kr.co.telecons.mconhudsdk.util

import android.util.Log

/**
 * Logger
 *
 * Created by jjyang on 10.05.2023.
 */
class Logger {
    companion object {
        private const val TAG = "MConHudSdkLog"

        private var isLogEnable : Boolean = true

        private var LEVEL = 0
        private var VERBOSE : Boolean = LEVEL <= LogLevel.VERBOSE.level
        private var DEBUG : Boolean = LEVEL <= LogLevel.DEBUG.level
        private var INFO : Boolean = LEVEL <= LogLevel.INFO.level
        private var WARN : Boolean = LEVEL <= LogLevel.WARN.level
        private var ERROR : Boolean = LEVEL <= LogLevel.ERROR.level

        enum class LogLevel(val level : Int){
            VERBOSE(1), DEBUG(2), INFO(3), WARN(4), ERROR(5)
        }

        internal fun setTag(tag: String){
            "$TAG [$tag]"
        }

        fun setLogLevel(logLevel: LogLevel){
            LEVEL = logLevel.level

            VERBOSE = LEVEL <= LogLevel.VERBOSE.level
            DEBUG = LEVEL <= LogLevel.DEBUG.level
            INFO = LEVEL <= LogLevel.INFO.level
            WARN = LEVEL <= LogLevel.WARN.level
            ERROR = LEVEL <= LogLevel.ERROR.level
        }

        internal fun v(msg :String){
            if(isLogEnable && VERBOSE) {
                handleLog(LogLevel.VERBOSE, msg)
            }
        }

        internal fun d(msg :String){
            if(isLogEnable && DEBUG) {
                handleLog(LogLevel.DEBUG, msg)
            }
        }

        internal fun i(msg :String){
            if(isLogEnable && INFO) {
                handleLog(LogLevel.INFO, msg)
            }
        }

        internal fun w(msg : String, error : Throwable) {
            if(isLogEnable && WARN) {
                handleLog(LogLevel.WARN, msg + "${error.message}")
            }
        }

        internal fun e(msg : String) {
            if(isLogEnable && ERROR) {
                handleLog(LogLevel.ERROR, msg)
            }
        }

        internal fun e(msg : String, error : Throwable) {
            if(isLogEnable && ERROR) {
                handleLog(LogLevel.ERROR, msg + "${error.message}")
            }
        }

        // Message가 4000자 초과일 경우 Log 잘림 현상 대응
        private fun handleLog(level: LogLevel, message: String) {
            try{
                when(level) {
                    LogLevel.VERBOSE -> {
                        if (message.length > 4000) {
                            Log.v(TAG, message.substring(0, 4000))
                            handleLog(level, message.substring(4000))
                        } else {
                            Log.v(TAG, message)
                        }
                    }
                    LogLevel.DEBUG -> {
                        if (message.length > 4000) {
                            Log.d(TAG, message.substring(0, 4000))
                            handleLog(level, message.substring(4000))
                        } else {
                            Log.d(TAG, message)
                        }
                    }
                    LogLevel.INFO -> {
                        if (message.length > 4000) {
                            Log.i(TAG, message.substring(0, 4000))
                            handleLog(level, message.substring(4000))
                        } else {
                            Log.i(TAG, message)
                        }
                    }
                    LogLevel.WARN -> {
                        if (message.length > 4000) {
                            Log.w(TAG, message.substring(0, 4000))
                            handleLog(level, message.substring(4000))
                        } else {
                            Log.w(TAG, message)
                        }
                    }
                    else -> {
                        if (message.length > 4000) {
                            Log.e(TAG, message.substring(0, 4000))
                            handleLog(level, message.substring(4000))
                        } else {
                            Log.e(TAG, message)
                        }
                    }
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }
}