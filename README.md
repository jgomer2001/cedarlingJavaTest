# Benchmarking Cedarling Java performance

## Requirements

- Java 17 or higher
- Maven 3.8 or higher
- Jans Server

## What this test does

Here we measure how long it takes for Java Cedarling to evaluate token-based authorization authorization on a simple resource against a policy.

The resource (`student`) looks like: `{ "name": "John Doe", "grad_year": 2023 }`

The policy:

```
@id("alumni_restricted_access")
permit(
  principal,
  action in Jans::Action::"Search",
  resource is Jans::student
)
when {
  resource.grad_year < 2026 ||
  (
    context has tokens.jans_userinfo_token &&
    context.tokens.jans_userinfo_token.hasTag("role") &&
    context.tokens.jans_userinfo_token.getTag("role").contains("AdmissionsCounselor")
  )
};
```

No extra context data supplied.

## How it works

Once a (`UserInfo`) token is obtained, the class `co.MyCedarling` generates a number of random student resources, with 
`grad_year` between 2024 and 2027. The time taken by each execution of method `authorizeMultiIssuer` is measured. The (native) time taken by Cedarling alone is extracted from the decision logs as well.

Average times in microseconds are printed to stdout.

## How to run

```
export JAVA_HOME= ...
mvn compile
mvn exec:java -Dexec.mainClass=co.MyCedarling -Dexec.args="1000 config.json eyJraWQiOiJjb25uZWN0...."
```

Execution parameters in order:

- Number of random students (1000 in the above example)
- Path to configuration file
- UserInfo token

The contents of the config file used were:

```
{
    "CEDARLING_APPLICATION_NAME": "none",
    "CEDARLING_JWT_SIG_VALIDATION": "enabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "enabled",
    "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
        "HS256",
        "RS256"
    ],
    "CEDARLING_LOG_TYPE": "memory",
    "CEDARLING_LOG_LEVEL": "INFO",
    "CEDARLING_LOG_TTL": 60,
    "CEDARLING_POLICY_STORE_URI": "https://github.com/jgomer2001/CedarlingQuickstart/releases/download/v0.0.4/tarpDemo.cjar"
}
```

## Results

On a Basic [DO](https://slugs.do-api.dev/) VM (s-4vcpu-8gb / Ubuntu 22) with Amazon Corretto-17.0.19 and Janssen Server 2.0.0 (AS and config-api only) alongside, the following was obtained:

- Average decision time: ~2.7ms
- Average decision time (Cedarling only): ~1.8ms
- 1000 random students
