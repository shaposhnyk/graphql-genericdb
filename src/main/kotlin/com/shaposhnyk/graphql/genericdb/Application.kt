package com.shaposhnyk.graphql.genericdb

import graphql.Scalars
import graphql.language.FieldDefinition
import graphql.language.ListType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.schema.*
import graphql.schema.idl.TypeDefinitionRegistry
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.ldap.NamingException
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.ContainerCriteria
import org.springframework.ldap.query.LdapQueryBuilder.query
import java.util.stream.Collectors
import javax.naming.directory.Attributes


@SpringBootApplication
class Application {

    /**
     * Retrieves all the persons in the ldap server
     * @return list with person names
     */
    fun groupList(ldapTemplate: LdapTemplate, userDn: String): List<Attributes> {
        val criteria: ContainerCriteria = query().where("objectclass").`is`("groupOfUniqueNames")
                .and("uniqueMember").not().`is`(userDn)
        return ldapTemplate!!.search(
                criteria
                ,
                object : AttributesMapper<Attributes> {
                    @Throws(NamingException::class)
                    override fun mapFromAttributes(attrs: Attributes): Attributes {
                        return attrs
                    }
                })
    }

    /**
     * Retrieves all the persons in the ldap server
     * @return list with person names
     */
    fun userList(ldapTemplate: LdapTemplate): List<Attributes> {
        return ldapTemplate!!.search(
                query().where("objectclass").`is`("person"),
                object : AttributesMapper<Attributes> {
                    @Throws(NamingException::class)
                    override fun mapFromAttributes(attrs: Attributes): Attributes {
                        return attrs
                    }
                })
    }

    @Bean
    fun init(ldapTemplate: LdapTemplate) = CommandLineRunner {
        println("Hello Boot")
        println("people " + userList(ldapTemplate).stream()
                .map { it -> it.get("cn").toString() }
                .collect(Collectors.toList()))
    }

    @Bean
    fun schema(ldapTemplate: LdapTemplate): GraphQLSchema {
        val queryType = ObjectTypeDefinition("Query")
        queryType.fieldDefinitions.add(FieldDefinition("applications", ListType(TypeName("application"))))

        val appType = ObjectTypeDefinition("application")
        appType.fieldDefinitions.add(FieldDefinition("id", TypeName("String")))
        appType.fieldDefinitions.add(FieldDefinition("name", TypeName("String")))

        val typeRegistry = TypeDefinitionRegistry()
        typeRegistry.add(queryType)
        typeRegistry.add(appType)

        val queryBuilder = GraphQLObjectType.newObject()
                .name("Query")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("applications")
                        .type(GraphQLList.list(GraphQLTypeReference("application")))
                        .dataFetcher { _ -> listOf("appFirst", "appSecond", "appGUI") }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("people")
                        .type(GraphQLList.list(GraphQLTypeReference("person")))
                        .dataFetcher { _ ->
                            LdapFetcher.with(ldapTemplate)
                                    .ofObjectClass("person")
                                    .fetch()
                        }
                )

        val appBuilder: GraphQLType = GraphQLObjectType.newObject()
                .name("application")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as String).toUpperCase() }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> env.getSource() as String }
                ).build()

        val groupBuilder: GraphQLType = GraphQLObjectType.newObject()
                .name("group")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as Attributes).get("dn").get() as String }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as Attributes).get("cn").get() as String }
                ).build()

        val personBuilder: GraphQLType = GraphQLObjectType.newObject()
                .name("person")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as Attributes).get("dn").get() as String }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("login")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as Attributes).get("sn").get() as String }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as Attributes).get("cn").get() as String }
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("groups")
                        .type(GraphQLList.list(GraphQLTypeReference("group")))
                        .dataFetcher { env ->
                            val login = ((env.getSource() as Attributes).get("sn").get() as String);
                            LdapFetcher.with(ldapTemplate)
                                    .ofObjectClass("groupOfUniqueNames")
                                    .filter("uniqueMember", "uid=${login},ou=people,dc=shaposhnyk,dc=com")
                                    .fetch()
                        }
                ).build()

        return GraphQLSchema.newSchema()
                .query(queryBuilder)
                .build(mutableSetOf(appBuilder, personBuilder, groupBuilder))
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

