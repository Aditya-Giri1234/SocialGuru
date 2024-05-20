package com.aditya.socialguru

import com.aditya.socialguru.domain_layer.helper.myDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis


suspend fun giveResult() = callbackFlow<Int> {
    (1..10).forEach {
//        delay(400)
        val isSend=trySend(it)
        println("Value $it is send $isSend")
    }
    close() // Close the flow after emissions are complete
}

 suspend fun runMe(callback:(where:String)->Unit){
    val scope=CoroutineScope(Dispatchers.Default)
     scope.launch {
         val time= measureTimeMillis {
             val list=(1..10).map {
                 async {
                     myDelay(it*20L)
                 }
             }

             list.awaitAll()
         }
         callback("in coroutine")
         println("Time taken to solve $time")
     }
   callback("outside coroutine")
}

 suspend fun main() {
//    val scope = CoroutineScope(Dispatchers.IO) // Use a specific dispatcher for clarity
//
//    scope.launch {
//        giveResult().collect { value ->
//            println("Upcoming result is $value")
//        }
//    }

 runMe {
     println("Hello, greeting from main function and this function call from $it !")
 }
}
