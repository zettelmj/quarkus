package io.quarkus.it.kotser

import io.quarkus.it.kotser.model.Person
import io.quarkus.runtime.annotations.RegisterForReflection
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.Method
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlin.reflect.jvm.javaMethod

@Path("/")
@RegisterForReflection
class GreetingResource {
    @Path("flow")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun flowHello() = flowOf(Person("Jim Halpert"))

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun hello(): Person {
        return Person("Jim Halpert")
    }

    @Path("suspend")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun suspendHello(): Person {
        return Person("Jim Halpert")
    }

    @Path("suspendList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun suspendHelloList() = listOf(Person("Jim Halpert"))

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun marry(person: Person): Person {
        return Person(person.name.substringBefore(" ") + " Halpert")
    }

    @GET
    @Path("create")
    @Produces(MediaType.TEXT_PLAIN)
    fun create(): Response? {
        val javaMethod: Method = this::reflect.javaMethod!!
        return Response
            .ok()
            .entity(javaMethod.invoke(this))
            .build()
    }

    fun reflect() = "hello, world"
}

