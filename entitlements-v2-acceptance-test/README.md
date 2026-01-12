### Running E2E Tests

You will need to have the following environment variables defined.

| name                                | value                                            | description                                                                                       | sensitive? | source | required |
|-------------------------------------|--------------------------------------------------|---------------------------------------------------------------------------------------------------|------------|--------|----------|
| `ENTITLEMENTS_URL`                  | ex `http://localhost:8080/api/entitlements/v2/`  | Entitlements service URL                                                                          | no         | -      | yes      |
| `TENANT_NAME`                       | ex `opendes`                                     | OSDU tenant used for testing                                                                      | no         | -      | yes      |
| `ENTITLEMENTS_DOMAIN`               | ex `contoso.com`                                 | Entitlements domain                                                                               | no         | -      | yes      |
| `PARTITION_URL`                     | ex `http://localhost:8080/api/partition/v1/`     | Partition service URL                                                                             | no         | -      | no       |
| `INTEGRATION_TESTER_EMAIL`          | ex `integration-tester@service.local`            | Integration tester email                                                                          | no         | -      | no       |
| `INDEXER_SERVICE_ACCOUNT_EMAIL`     | ex `workload-indexer@osdu.iam.gserviceaccount.com` | Indexer service account email with special privileges for data groups                           | no         | -      | no       |
| `DATA_ROOT_GROUP_HIERARCHY_ENABLED` | ex `true`                                        | Controls whether data.root groups get access to all data groups (depends on partition feature flag) | no         | -      | no       |
| `LOCAL_MODE`                        | ex `true`                                        | Enable local mode for testing/debugging with HEADER_X_USER_ID                                     | no         | -      | no       |
| `HEADER_X_USER_ID`                  | ex `1234`                                        | Custom HTTP header for user identification (used with LOCAL_MODE)                                 | no         | -      | no       |

Authentication can be provided as OIDC config:

| name                                            | value                                      | description                                 | sensitive? | source |
|-------------------------------------------------|--------------------------------------------|---------------------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                                 | Privileged User Client Id                   | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                                 | Privileged User Client secret               | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | ex `https://keycloak.com/auth/realms/osdu` | OpenID provider url                         | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_SCOPE`         | ex `api://my-app/.default`                 | OAuth2 scope (optional, defaults to openid) | no         | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_ID`      | `********`                                 | No-access User Client Id (optional)         | yes        | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_SECRET`  | `********`                                 | No-access User Client secret (optional)     | yes        | -      |
| `ROOT_USER_OPENID_PROVIDER_CLIENT_ID`           | `********`                                 | Root User Client Id (optional)              | yes        | -      |
| `ROOT_USER_OPENID_PROVIDER_CLIENT_SECRET`       | `********`                                 | Root User Client secret (optional)          | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | Privileged User Token | yes        | -      |
| `NO_ACCESS_USER_TOKEN`  | `********` | No-access User Token  | yes        | -      |
| `ROOT_USER_TOKEN`       | `********` | Root User Token       | yes        | -      |

Authentication configuration is optional and could be omitted if not needed.

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER         | NO_DATA_ACCESS_TESTER     |
|----------------------------|---------------------------|
| users                      | users                     |
| service.entitlements.user  | service.entitlements.user |
| service.entitlements.admin | service.storage.admin     |
| users.datalake.delegation  |                           |

Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd entitlements-v2-acceptance-test && mvn clean verify)
 ```

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
