package com.ecommerce.experimentapp.utils

import com.ecommerce.experimentapp.constant.AppConstants


class DebugHandler {

    companion object {
        private val LOG_TAG = "E-Commerce App ::"
        private val ENABLE_LOGS = true
        private val VERBOSE = android.util.Log.VERBOSE
        private val DEBUG = android.util.Log.DEBUG
        private val INFO = android.util.Log.INFO
        private val WARN = android.util.Log.WARN
        private val ERROR = android.util.Log.ERROR
        private val WTF = android.util.Log.ASSERT

        //Default TAG
        private val DEFAULT_TAG = LOG_TAG

        private val tag = DEFAULT_TAG

        // private static int LEVEL = (BuildConfig.DEBUG ? VERBOSE : WARN);
        private val LEVEL = 1

        fun logException(e: Exception?) {
            android.util.Log.e(LOG_TAG, "Exception- ", e)
        }

        fun log(message: String?) {
            if (AppConstants.EnableLogging && message != null) {
                android.util.Log.i(LOG_TAG, message)
            }
        }

    }


}