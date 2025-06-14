## Service Configuration for Google Cloud

## Environment variables:

Define the following environment variables.

Must have:

| name                     | value                 | description                                                                                | sensitive? | source |
|--------------------------|-----------------------|--------------------------------------------------------------------------------------------|------------|--------|
| `SPRING_PROFILES_ACTIVE` | ex `gcp`              | Spring profile that activate default configuration for Google Cloud environment            | false      | -      |
| `GROUP_ID`               | `group`               | The id of the groups is created. The default (and recommended for `jdbc`) value is `group` | no         | -      |
| `PARTITION_HOST`         | ex `http://partition` | Partition service host address                                                             | no         | -      |

Defined in default application property file but possible to override:

| name                                 | value                                    | description                                                                                                                                                                                  | sensitive? | source |
|--------------------------------------|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------|
| `LOG_PREFIX`                         | `entitlements-v2`                        | Logging prefix                                                                                                                                                                               | no         | -      |
| `SERVER_SERVLET_CONTEXPATH`          | `/api/entitlements/v2`                   | Register context path                                                                                                                                                                        | no         | -      |
| `LOG_LEVEL`                          | `INFO`                                   | Logging level                                                                                                                                                                                | no         | -      |
| `server_port`                        | `8080`                                   | Port of the server                                                                                                                                                                           | no         | -      |
| `AUTHENTICATION_MODE`                | `INTERNAL`, `IAP`, `EXTERNAL` or `ISTIO` | `INTERNAL` is used by default for GCP implementation, but there are other modes - `IAP`, `EXTERNAL` and `ISTIO`. More information about each mode you can find [here](#authentication-modes) | no         | -      |
| `PARTITION_AUTH_ENABLED`             | `true`                                   | Disable or enable auth token provisioning for requests to Partition service                                                                                                                  | no         | -      |
| `SERVICE_TOKEN_PROVIDER`             | `GCP`                                    | Service account token provider, `GCP` means use Google Service Account `OPEIND` means use OpenId provider like `Keycloak`                                                                    | no         | -      |
| `OPENID_PROVIDER_URL`                | `https://accounts.google.com`            | OpenID provider                                                                                                                                                                              | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | `RS256`                                  | OpenID token algorithm                                                                                                                                                                       | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email`                               | OpenID User ID claim name                                                                                                                                                                    | no         | -      |
| `REDIS_USER_INFO_HOST`               | ex `127.0.0.1`                           | Redis host                                                                                                                                                                                   | no         | -      |
| `REDIS_USER_INFO_PORT`               | ex `6379`                                | Redis port                                                                                                                                                                                   | no         | -      |
| `REDIS_USER_INFO_PASSWORD`           | ex ``                                    | Redis password                                                                                                                                                                               | yes        | -      |
| `REDIS_USER_INFO_WITH_SSL`           | ex `true` or `false`                     | Redis host SSL config                                                                                                                                                                        | no         |        |
| `REDIS_USER_GROUPS_HOST`             | ex `127.0.0.1`                           | Redis host                                                                                                                                                                                   | no         | -      |
| `REDIS_USER_GROUPS_PORT`             | ex `6379`                                | Redis port                                                                                                                                                                                   | no         | -      |
| `REDIS_USER_GROUPS_PASSWORD`         | ex ``                                    | Redis password                                                                                                                                                                               | yes        | -      |
| `REDIS_USER_GROUPS_WITH_SSL`         | ex `true` or `false`                     | Redis host SSL config                                                                                                                                                                        | no         |        |
| `DATASTORE_SCHEMA_NAME`              | ex `entitlements`                        | DB Schema name                                                                                                                                                                               | yes        |        |
| `DATASTORE_SCHEMA_VERSION`           | ex `1`                                   | DB Schema version                                                                                                                                                                            | yes        |        |
| `SYSTEM_TENANT`                      | ex `system`                              | System tenant ID, default is `system`                                                                                                                                                        | no         |        |
| `PARTITION_PROPERTIES_PREFIX`        | ex `entitlements`                        | Prefix for Database connection properties in Partition configuration, default `entitlements`, result `entitlements.datasource.url`                                                           | no         |        |
| `MANAGEMENT_ENDPOINTS_WEB_BASE`      | ex `/`                                   | Web base for Actuator                                                                                                                                                                        | no         | -      |
| `MANAGEMENT_SERVER_PORT`             | ex `8081`                                | Port for Actuator                                                                                                                                                                            | no         | -      |

## Postgres Connection configuration

Entitlements database connection is configured via properties, provided by the Partition service:

```json
{
  "properties": {
    "entitlements.datasource.url": {
      "sensitive": true,
      "value": "ENT_PG_URL"
    },
    "entitlements.datasource.username": {
      "sensitive": true,
      "value": "ENT_PG_USER"
    },
    "entitlements.datasource.password": {
      "sensitive": true,
      "value": "ENT_PG_PASS"
    },
    "entitlements.datasource.schema": {
      "sensitive": true,
      "value": "ENT_PG_SCHEMA"
    }
  }
}
```

By default, the prefix is `entitlements` but could be overridden by PARTITION_PROPERTIES_PREFIX env
var.
If `sensitive` is set to false then `value` will be used from the property as is, if it is false
then `value` of the property will be used as the env var name,
and value should be provided in the service environment variables with that name.

## Authentication modes

**INTERNAL** Use it when authentication should be processed by entitlements, OpenID provider will be
used for token validation.

| name                        | value                            | description                                                                  | sensitive? | source |
|-----------------------------|----------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`       | ex `https://accounts.google.com` | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_ALGORITHM` | ex `RS256`                       | OpenID token algorithm                                                       | no         | -      |
| `USER_IDENTITY_HEADER_NAME` | ex `x-user-id`                   | The name of the header in which the "id of the authenticated user" is passed | no         | -      |

**IAP** Use it with enabled IAP, this mode use combined authentication with OpenID provider, which
will be used inside secured perimeter with service to service communication and IAP tokens
verification that came with users requests.

| name                                 | value                                                                                                   | description                                      | sensitive? | source |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|--------------------------------------------------|------------|--------|
| `IAP_PROVIDER_JWT_HEADER`            | ex `x-goog-iap-jwt-assertion`                                                                           | Header that will contain IAP token               | no         | -      |
| `IAP_PROVIDER_USER_ID_HEADER`        | ex `x-goog-authenticated-user-email`                                                                    | Header that will contain user email added by IAP | no         | -      |
| `IAP_PROVIDER_ISSUER_URL`            | ex `https://cloud.google.com/iap`                                                                       | IAP issuer url                                   | no         | -      |
| `IAP_PROVIDER_AUD`                   | ex `/projects/${iap.provider.project-number}/global/backendServices/${iap.provider.backend-service-id}` | IAP client id(audiences)                         | no         | -      |
| `IAP_PROVIDER_PROJECT_NUMBER`        | ex `12345`                                                                                              | Google project number                            | no         | -      |
| `IAP_PROVIDER_BACKEND_SERVICE_ID`    | ex `12345`                                                                                              | Google backend service id                        | no         | -      |
| `IAP_PROVIDER_JWK_URL`               | ex `https://www.gstatic.com/iap/verify/public_key-jwk`                                                  | IAP jwk url                                      | no         | -      |
| `IAP_PROVIDER_ALGORITHM`             | ex `ES256`                                                                                              | IAP token algorithm                              | no         | -      |
| `OPENID_PROVIDER_URL`                | ex `https://accounts.google.com`                                                                        | OpenID provider                                  | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | ex `RS256`                                                                                              | OpenID token algorithm                           | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email`                                                                                              | OpenID user id claim name                        | no         | -      |

**EXTERNAL**  Use it with enabled external authentication method, this mode use combined
authentication with OpenID provider, if the request will contain both token and user-id header

| name                                 | value                            | description                                                                  | sensitive? | source |
|--------------------------------------|----------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`                | ex `https://accounts.google.com` | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | ex `RS256`                       | OpenID token algorithm                                                       | no         | -      |
| `USER_IDENTITY_HEADER_NAME`          | ex `x-user-id`                   | The name of the header in which the "id of the authenticated user" is passed | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `preferred_username`          | OpenID user id claim name                                                    | no         | -      |

**ISTIO** Use it when authentication should not be processed by entitlements, Istio will be used for
token validation.

| name                                 | value      | description               | sensitive? | source |
|--------------------------------------|------------|---------------------------|------------|--------|
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email` | OpenID user id claim name | no         | -      |

**Required to run integration tests**

| Name                                | Value                                             | Description                                                                                                                                                      | Sensitive? | Source |
|-------------------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------|
| `ENTITLEMENT_V2_URL`                | ex `http://localhost:8080/api/entitlements/v2/`   | The host where the service is running                                                                                                                            | no         | --     |
| `GROUP_ID`                          | ex `contoso.com`                                  | Must match the value of `service_group_id` above                                                                                                                 | no         | --     |
| `TENANT_NAME`                       | ex `opendes`                                      | OSDU tenant used for testing                                                                                                                                     | no         | --     |
| `INTEGRATION_TESTER`                | `********`                                        | System identity to assume for API calls. Note: This user must have entitlements already configured                                                               | yes        | --     |
| `NO_DATA_ACCESS_TESTER`             | `********`                                        | Service account base64 encoded string without data access                                                                                                        | yes        | --     |
| `GOOGLE_APPLICATION_CREDENTIALS`    | `********`                                        | System identity to provide access for cleaning up groups created during test. Path to JSON file with keys for Google service account.                            | yes        | --     |
| `AUTH_MODE`                         | `IAP`                                             | Should be configured only if IAP enabled                                                                                                                         | no         | --     |
| `IAP_URL`                           | `https://dev.osdu.club`                           | Should be configured only if IAP enabled                                                                                                                         | no         | --     |
| `PARTITION_API`                     | ex `http://localhost:8080/api/partition/v1 `      | Partition service host                                                                                                                                           | no         | --     |
| `INDEXER_SERVICE_ACCOUNT_EMAIL`     | ex `workload-indexer@osdu.iam.gseviceaccount.com` | Indexer service account email with special privileges for data groups                                                                                            | no         | --     |
| `DATA_ROOT_GROUP_HIERARCHY_ENABLED` | ex `true`                                         | Depending on the `DISABLE_DATA_ROOT_GROUP HIERARCHY` feature flag in Partition info, this flag controls whenever data.root groups get access to all data groups. | no         | --     |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER         | NO ACCESS TESTER          |
|----------------------------|---------------------------|
| users                      | users                     |
| service.entitlements.user  | service.entitlements.user |
| service.entitlements.admin |                           |
| users.datalake.delegation  |                           |


## Google Cloud service account configuration

TBD

| Required roles |
|----------------|
| -              |

## License

Copyright © Google LLC
Copyright © EPAM Systems

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

