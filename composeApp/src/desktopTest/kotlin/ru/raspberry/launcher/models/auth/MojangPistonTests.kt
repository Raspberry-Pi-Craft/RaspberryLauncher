package ru.raspberry.launcher.models.auth

import kotlinx.coroutines.runBlocking
import ru.raspberry.launcher.service.getJavaList
import kotlin.test.Test

class MojangPistonTests {


    @Test
    fun `java versions`() {
       runBlocking {
           val data = getJavaList()
           println(data?.keys)

           assert(data != null) {
               "Java versions data should not be null"
           }
           assert(data!!.isNotEmpty()) {
               "Java versions data should not be empty"
           }
       }
    }
}