#!/usr/bin/env bash

set -ex

bootstrap_entitlements_gc_system_partition() {

  local DATA_PARTITION_ID=$1

  cat <<EOF >/opt/system-partition-config.json
{
  "aliasMappings":
[
{
"aliasId": "SERVICE_PRINCIPAL",
"userId": "datafier@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_AIRFLOW",
"userId": "$AIRFLOW_COMPOSER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_REGISTER",
"userId": "wi-register-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_INDEXER",
"userId": "wi-indexer-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_NOTIFICATION",
"userId": "wi-notification-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_STORAGE",
"userId": "wi-storage-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SEISMIC",
"userId": "wi-seismic-store-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_GCZ",
"userId": "wi-gcz-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SCHEMA_UPGRADE",
"userId": "wi-schema-upgrade-gc@${PROJECT_ID}.iam.gserviceaccount.com"
}
]
}
EOF

  set +x
  status_code=$(curl --location --silent --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer $(gcloud auth print-identity-token)" \
    --data @/opt/system-partition-config.json)
  set -x

  if [ "$status_code" == 200 ]; then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    cat /opt/output.txt | jq
    exit 1
  fi

}

bootstrap_entitlements_gc_non_system_partition() {

  local DATA_PARTITION_ID=$1

cat <<EOF >/opt/other-partition-config.json
{
  "aliasMappings":
[
{
"aliasId": "SERVICE_PRINCIPAL",
"userId": "$ADMIN_USER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_AIRFLOW",
"userId": "$AIRFLOW_COMPOSER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_REGISTER",
"userId": "wi-register-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_INDEXER",
"userId": "wi-indexer-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_NOTIFICATION",
"userId": "wi-notification-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_STORAGE",
"userId": "wi-storage-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SEISMIC",
"userId": "wi-seismic-store-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_GCZ",
"userId": "wi-gcz-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SCHEMA_UPGRADE",
"userId": "wi-schema-upgrade-gc@${PROJECT_ID}.iam.gserviceaccount.com"
}
]
}
EOF

  set +x
  status_code=$(curl --location --silent --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer $(gcloud auth print-identity-token)" \
    --data @/opt/other-partition-config.json)
  set -x

  if [ "$status_code" == 200 ]; then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    cat /opt/output.txt | jq
    exit 1
  fi

}

source ./validate-env.sh "DATA_PARTITION_ID"
source ./validate-env.sh "ENTITLEMENTS_HOST"
source ./validate-env.sh "ADMIN_USER_EMAIL"
source ./validate-env.sh "GROUP_ID"
source ./validate-env.sh "AIRFLOW_COMPOSER_EMAIL"
source ./validate-env.sh "PROJECT_ID"

# Specifying "system" partition for GC installation
export SYSTEM_PARTITION_ID="system"

echo "Bootstrapping entitlements for data_partition: ${SYSTEM_PARTITION_ID}"
bootstrap_entitlements_gc_system_partition "${SYSTEM_PARTITION_ID}"
echo "Finished entitlements bootstrap for data_partition: ${SYSTEM_PARTITION_ID}"

echo "Bootstrapping entitlements for data_partition: ${DATA_PARTITION_ID}"
bootstrap_entitlements_gc_non_system_partition "${DATA_PARTITION_ID}"
echo "Finished entitlements bootstrap for data_partition: ${DATA_PARTITION_ID}"

touch /tmp/bootstrap_ready

sleep 365d
