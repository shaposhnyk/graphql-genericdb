# Generic data graphql application

## Motivations
- documentation from graphql-java repository references GQL v3.0 (current version is 7) and targets "bean" use case
- another documentation references object builder, but never shows where it can be used.

## Observations

Non-builder-style object definition code:
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


## Pros
- minimize transferred data
- automatic schema generation

## Open questions
- how to cache effectively data. fragments?