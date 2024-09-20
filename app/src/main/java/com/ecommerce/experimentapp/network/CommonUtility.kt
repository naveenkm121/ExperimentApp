package com.ecommerce.experimentapp.network

import kotlin.random.Random

class CommonUtility {

    companion object {
        public fun getRandomInt(min: Int = 0, max: Int): Int {
            return Random.nextInt(min, max + 1)
        }
    }
}