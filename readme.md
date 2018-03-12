# Generic data graphql application
Generic GraphQL application with programmaticly generated schema, to be used with external third-party providers, which allows clients to specify data columns, like relational and NoSQL databases, JCR (Apache Jackrabbit) or LDAP

## Motivations
- make prof of concept of generic GraphQL application with programmatic schema, which can be used with databases and external providers, which allows clients to specify data columns to select
- overcome lack of documentation, as documentation on graphql-java and most of examples focus on bean classes use-case
-- both docs/tutorials mentioned on graphql-java focus on bean classes use-case
-- graphql-java-db-example reads schema from a file
-- graphql-java project is developing fast and documentation is quite outdated (v3 documented, while v7 released)

## Goals
- easy-to-understand high-level code
```
        val groupType = newObject("group")
                .field(ldapField("id", "sn") { it.toUpperCase() })
                .field(ldapField("name", "cn"))
```
- easy-to-write-and-maintain code, for a someone already familiar with GraphQL
- minimize transferred data
- generate GraphQL schema automatically
- easy async fetchers

## Open questions
- how to cache effectively data? server-side fragments?
- pool management to avoid deadlocks? is it handled?
- object level post processors?

## Observations

Non-builder-style object definition code is more compact, than builde-style:
```
        val appType = ObjectTypeDefinition("application")
        appType.fieldDefinitions.add(FieldDefinition("id", TypeName("String")))
        appType.fieldDefinitions.add(FieldDefinition("name", TypeName("String")))
        appType.fieldDefinitions.add(FieldDefinition("instances", ListType(TypeName("instance"))))
```

Builder-style object definition code:
```
        val appType = GraphQLObjectType.newObject()
                .name("application")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> (env.getSource() as String).toUpperCase() }
                        .build()
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher { env -> env.getSource() as String }
                        .build()
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("instances")
                        .type(GraphQLList.list(GraphQLTypeReference("instance")))
                        .dataFetcher(instanceFetcher)
                        .build()
                )
                .build()
```

## Easy Error tracing
java.lang.ClassCastException: com.shaposhnyk.graphql.genericdb.LdapFetcher cannot be cast to java.lang.Iterable
	at graphql.execution.ExecutionStrategy.toIterable(ExecutionStrategy.java:407) ~[graphql-java-6.0.jar:na]
	at graphql.execution.ExecutionStrategy.completeValue(ExecutionStrategy.java:334) ~[graphql-java-6.0.jar:na]
	at graphql.execution.ExecutionStrategy.completeField(ExecutionStrategy.java:304) ~[graphql-java-6.0.jar:na]
	at graphql.execution.ExecutionStrategy.lambda$resolveField$0(ExecutionStrategy.java:167) ~[graphql-java-6.0.jar:na]
	at java.util.concurrent.CompletableFuture.uniComposeStage(CompletableFuture.java:981) ~[na:1.8.0_91]
	at java.util.concurrent.CompletableFuture.thenCompose(CompletableFuture.java:2124) ~[na:1.8.0_91]
	at graphql.execution.ExecutionStrategy.resolveField(ExecutionStrategy.java:166) ~[graphql-java-6.0.jar:na]
	at graphql.execution.AsyncExecutionStrategy.execute(AsyncExecutionStrategy.java:55) ~[graphql-java-6.0.jar:na]
	at graphql.execution.ExecutionStrategy.completeValue(ExecutionStrategy.java:368) ~[graphql-java-6.0.jar:na]

